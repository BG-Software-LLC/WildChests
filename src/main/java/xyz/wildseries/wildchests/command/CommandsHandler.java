package xyz.wildseries.wildchests.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.command.commands.CommandGive;
import xyz.wildseries.wildchests.command.commands.CommandInfo;
import xyz.wildseries.wildchests.command.commands.CommandLink;
import xyz.wildseries.wildchests.command.commands.CommandReload;
import xyz.wildseries.wildchests.command.commands.CommandSave;
import xyz.wildseries.wildchests.command.commands.CommandSettings;

import java.util.ArrayList;
import java.util.List;

public final class CommandsHandler implements CommandExecutor, TabCompleter {

    private WildChestsPlugin plugin;

    private List<ICommand> subCommands = new ArrayList<>();

    public CommandsHandler(WildChestsPlugin plugin){
        this.plugin = plugin;
        subCommands.add(new CommandGive());
        subCommands.add(new CommandInfo());
        subCommands.add(new CommandLink());
        subCommands.add(new CommandReload());
        subCommands.add(new CommandSave());
        //subCommands.add(new CommandSettings());
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if(args.length > 0){
            for(ICommand subCommand : subCommands) {
                if (subCommand.getLabel().equalsIgnoreCase(args[0])){
                    if(subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())){
                        Locale.NO_PERMISSION.send(sender);
                        return false;
                    }
                    if(args.length < subCommand.getMinArgs() || args.length > subCommand.getMaxArgs()){
                        Locale.COMMAND_USAGE.send(sender, subCommand.getUsage());
                        return false;
                    }
                    subCommand.perform(plugin, sender, args);
                    return true;
                }
            }
        }

        //Checking that the player has permission to use at least one of the commands.
        for(ICommand subCommand : subCommands){
            if(sender.hasPermission(subCommand.getPermission())){
                //Player has permission
                Locale.HELP_COMMAND_HEADER.send(sender);

                for(ICommand cmd : subCommands)
                    Locale.HELP_COMMAND_LINE.send(sender, cmd.getUsage(), cmd.getDescription());

                Locale.HELP_COMMAND_FOOTER.send(sender);
                return false;
            }
        }

        Locale.NO_PERMISSION.send(sender);

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if(args.length > 0){
            for(ICommand subCommand : subCommands) {
                if (subCommand.getLabel().equalsIgnoreCase(args[0])){
                    if(subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())){
                        return new ArrayList<>();
                    }
                    return subCommand.tabComplete(plugin, sender, args);
                }
            }
        }

        List<String> list = new ArrayList<>();

        for(ICommand subCommand : subCommands)
            if(subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission()))
                if(subCommand.getLabel().startsWith(args[0]))
                    list.add(subCommand.getLabel());

        return list;
    }
}
