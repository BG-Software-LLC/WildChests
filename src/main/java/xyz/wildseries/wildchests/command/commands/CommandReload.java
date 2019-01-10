package xyz.wildseries.wildchests.command.commands;

import org.bukkit.command.CommandSender;
import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.command.ICommand;
import xyz.wildseries.wildchests.handlers.SettingsHandler;
import xyz.wildseries.wildchests.task.SaveTask;

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
        new Thread(() -> {
            SettingsHandler.reload();
            Locale.reload();
            SaveTask.start();
            Locale.RELOAD_SUCCESS.send(sender);
        }).start();
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
