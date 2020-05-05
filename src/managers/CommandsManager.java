package managers;

import commands.*;
import exceptions.NoCommandException;
import java.util.*;

/**
 * Менеджер команд
 */
public class CommandsManager {
    private static CommandsManager instance;

    public static CommandsManager getInstance() {
        if (instance == null) {
            instance = new CommandsManager();
        }
        return instance;
    }

    private Map<String, ACommand> commands = new HashMap<>();

    public CommandsManager(){
        addCommand(new AddCommand());
        addCommand(new HelpCommand());
        addCommand(new ExitCommand());
        addCommand(new ShowCommand());
        addCommand(new InfoCommand());
        addCommand(new UpdateIdCommand());
        addCommand(new RemoveIdCommand());
        addCommand(new ClearCommad());
        addCommand(new ShuffleCommand());
        addCommand(new AddIfMinCommand());
        addCommand(new CountLessThanGovernor());
        addCommand(new FilterContainsNameCommand());
        addCommand(new RemoveLowerCommand());
        addCommand(new PrintFieldAscendingTimezoneCommand());
        addCommand(new SaveCommand());
        addCommand(new ExecuteScriptCommand());
    }

    private void addCommand(ACommand cmd){
        commands.put(cmd.getCmdName(), cmd);
    }

    public ACommand getCommand(String s) throws NoCommandException {
        if (!commands.containsKey(s)) {
            throw new NoCommandException("Команда не найдена");
        }
        return commands.get(s);
    }

    public ACommand parseCommand(String str){
        ACommand cmd = null;
        String[] parse = str.trim().split(" ");
        if(!parse[0].equals("")) {
            cmd = getCommand(parse[0].toLowerCase());
            String[] args = Arrays.copyOfRange(parse, 1, parse.length);
            cmd.setArgs(args);
        }

        return cmd;
    }

    /**
     * Выполняет команды введенные пользователем
     * @param str
     * @param consoleManager
     * @param collectionManager
     */
    public void execute(String str, ConsoleManager consoleManager, CollectionManager collectionManager){
        parseCommand(str).execute(consoleManager, collectionManager);
    }


    public List<ACommand> getAllCommands() {
        return new ArrayList<>(commands.values());
    }
}