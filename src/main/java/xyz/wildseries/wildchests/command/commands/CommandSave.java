package xyz.wildseries.wildchests.command.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.command.ICommand;

import java.util.ArrayList;
import java.util.List;

public final class CommandSave implements ICommand {

    @Override
    public String getLabel() {
        return "save";
    }

    @Override
    public String getUsage() {
        return "chests save";
    }

    @Override
    public String getPermission() {
        return "wildchests.save";
    }

    @Override
    public String getDescription() {
        return "Save all cached data into files.";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void perform(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        new Thread(() -> {
            plugin.getDataHandler().saveDatabase();
            sender.sendMessage(ChatColor.YELLOW + "Successfully saved all cached data.");
        }).start();
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
