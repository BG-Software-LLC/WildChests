package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.handlers.SettingsHandler;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.command.CommandSender;
import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.task.NotifierTask;

import java.util.ArrayList;
import java.util.List;

public final class CommandReload implements ICommand {

    @Override
    public String getLabel() {
        return "reload";
    }

    @Override
    public String getUsage() {
        return "chests reload";
    }

    @Override
    public String getPermission() {
        return "wildchests.reload";
    }

    @Override
    public String getDescription() {
        return "Reload the settings and the language files.";
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
            SettingsHandler.reload();
            Locale.reload(plugin);
            NotifierTask.start();
            Locale.RELOAD_SUCCESS.send(sender);
        });
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
