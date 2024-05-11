package com.bgsoftware.wildchests.command;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.command.commands.CommandGive;
import com.bgsoftware.wildchests.command.commands.CommandInfo;
import com.bgsoftware.wildchests.command.commands.CommandLink;
import com.bgsoftware.wildchests.command.commands.CommandReload;
import com.bgsoftware.wildchests.command.commands.CommandSave;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class CommandsHandler implements CommandExecutor, TabCompleter {


    private final Map<String, ICommand> subCommands = new LinkedHashMap<>();

    private final WildChestsPlugin plugin;

    public CommandsHandler(WildChestsPlugin plugin) {
        this.plugin = plugin;
        registerCommand(new CommandGive());
        registerCommand(new CommandInfo());
        registerCommand(new CommandLink());
        registerCommand(new CommandReload());
        registerCommand(new CommandSave());
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length > 0) {
            ICommand subCommand = this.subCommands.get(args[0].toLowerCase(java.util.Locale.ENGLISH));
            if (subCommand != null) {
                if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
                    Locale.NO_PERMISSION.send(sender);
                    return false;
                }
                if (args.length < subCommand.getMinArgs() || args.length > subCommand.getMaxArgs()) {
                    Locale.COMMAND_USAGE.send(sender, subCommand.getUsage());
                    return false;
                }
                subCommand.perform(plugin, sender, args);
                return true;
            }
        }

        // Showing help

        //Checking that the player has permission to use at least one of the commands.
        for (ICommand subCommand : subCommands.values()) {
            if (sender.hasPermission(subCommand.getPermission())) {
                //Player has permission, send help message
                Locale.HELP_COMMAND_HEADER.send(sender);

                for (ICommand cmd : subCommands.values())
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
        if (args.length > 0) {
            ICommand subCommand = this.subCommands.get(args[0].toLowerCase(java.util.Locale.ENGLISH));
            if (subCommand != null) {
                if (subCommand.getPermission() != null && !sender.hasPermission(subCommand.getPermission())) {
                    return Collections.emptyList();
                }

                return subCommand.tabComplete(plugin, sender, args);
            }
        }

        List<String> list = new LinkedList<>();

        for (ICommand subCommand : subCommands.values())
            if (subCommand.getPermission() == null || sender.hasPermission(subCommand.getPermission()))
                if (subCommand.getLabel().startsWith(args[0]))
                    list.add(subCommand.getLabel());

        return list;
    }

    private void registerCommand(ICommand command) {
        this.subCommands.put(command.getLabel().toLowerCase(java.util.Locale.ENGLISH), command);
    }

}
