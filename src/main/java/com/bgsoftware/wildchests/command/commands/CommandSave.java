package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
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
        Executor.async(() -> {
            plugin.getDataHandler().saveDatabase(null, false);
            sender.sendMessage(ChatColor.YELLOW + "Successfully saved all cached data.");
        });
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
