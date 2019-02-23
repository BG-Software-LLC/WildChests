package com.bgsoftware.wildchests.command;

import org.bukkit.command.CommandSender;
import com.bgsoftware.wildchests.WildChestsPlugin;

import java.util.List;

public interface ICommand {

    String getLabel();

    String getUsage();

    String getPermission();

    String getDescription();

    int getMinArgs();

    int getMaxArgs();

    void perform(WildChestsPlugin plugin, CommandSender sender, String[] args);

    List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args);

}
