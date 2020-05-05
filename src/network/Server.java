package network;

import commands.ACommand;
import exceptions.InvalidValueException;
import lombok.extern.slf4j.Slf4j;
import managers.CollectionManager;
import managers.ConsoleManager;
import network.packets.*;
import utils.AppConstant;
import utils.Serializer;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class Server {

    class Con {
        ByteBuffer req;
        ByteBuffer resp;
        SocketAddress sa;

        public Con() {
            req = ByteBuffer.allocate(AppConstant.MESSAGE_BUFFER);
        }
    }


    private boolean isRunning = false;
    private DatagramChannel channel;
    private Map<String, SocketAddress> clients =  new HashMap<String, SocketAddress>();
    private ByteBuffer inBuff = ByteBuffer.allocate(AppConstant.MESSAGE_BUFFER);
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private Selector selector = Selector.open();

    private CollectionManager collectionManager;
    private ConsoleManager consoleManager;




    public Server(String[] args) throws IOException {
        try {
            if (args.length == 1) {
                startServer(Integer.parseInt(args[0]));
            }else{
                startServer(AppConstant.DEFAULT_PORT);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void startServer(int port) throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(port));

        collectionManager = new CollectionManager(AppConstant.FILE_PATH);
        consoleManager = new ConsoleManager(new InputStreamReader(System.in), new OutputStreamWriter(outputStream), false);

        log.info("Server started. Listening port {}", port);
    }

    private void stopServer(){
        isRunning = false;
    }

    public void run() throws IOException, ClassNotFoundException {
        isRunning = true;

        log.info("Ready to receive...");

        SelectionKey clientKey = channel.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new Con());
        while(isRunning){
            try {
                selector.select();
                Iterator selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        read(key);
                        key.interestOps(SelectionKey.OP_WRITE);
                    }

                    if (key.isWritable()) {
                        write(key);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }

            } catch (IOException e) {
                log.error("IO Exception... " +(e.getMessage()!=null?e.getMessage():""));
            }
        }
    }

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {
        DatagramChannel chan = (DatagramChannel)key.channel();
        Con con = (Con)key.attachment();
        con.sa = chan.receive(con.req);
        con.req.flip();

        int limits = con.req.limit();
        byte bytes[] = new byte[limits];
        con.req.get(bytes, 0, limits);

        Object obj  = Serializer.Deserialize(bytes);
        con.req.clear();

        con.resp = ByteBuffer.wrap(Serializer.Serialize(objectHandler(obj, con.sa)));
    }

    private void write(SelectionKey key) throws IOException {
        DatagramChannel chan = (DatagramChannel)key.channel();
        Con con = (Con)key.attachment();
        chan.send(con.resp, con.sa);
    }


    private Object objectHandler(Object obj, SocketAddress client) throws IOException {
        Object outObj = null;
        if(obj instanceof LoginPacket) {
            if (!clients.containsKey(((LoginPacket) obj).getNick())) {
                clients.put(((LoginPacket) obj).getNick(), client);
                outObj = new LoginSuccessPacket("Connected");
                log.info("User connected " + ((LoginPacket) obj).getNick() + ": " + client);
            } else {
                outObj = new LoginFailedPacket("User with this nick already exists");
            }

        } else if(obj instanceof LogoutPacket){
            if (clients.containsKey(((LogoutPacket) obj).getNick())) {
                clients.remove(((LogoutPacket) obj).getNick());
                log.info("User disconnected " + ((LogoutPacket) obj).getNick() + ": " + client);
            }

        }else if(obj instanceof ACommand){
            outputStream.reset();
            try {
                ((ACommand) obj).execute(consoleManager, collectionManager);
                outObj = new CommandExecutionPacket(new String(outputStream.toByteArray()));
            }catch (InvalidValueException ex){
                outObj = new CommandExecutionPacket(ex.getMessage());
            }
            log.info(obj.toString());
            log.warn(client.toString());
        }

        return outObj;
    }
}
