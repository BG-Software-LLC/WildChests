package com.bgsoftware.wildchests;

import com.bgsoftware.common.config.CommentedConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Locale {

    private static Map<String, Locale> localeMap = new HashMap<>();

    public static Locale SOLD_CHEST_HEADER = new Locale("SOLD_CHEST_HEADER");
    public static Locale SOLD_CHEST_LINE = new Locale("SOLD_CHEST_LINE");
    public static Locale SOLD_CHEST_FOOTER = new Locale("SOLD_CHEST_FOOTER");
    public static Locale CRAFTED_ITEMS_HEADER = new Locale("CRAFTED_ITEMS_HEADER");
    public static Locale CRAFTED_ITEMS_LINE = new Locale("CRAFTED_ITEMS_LINE");
    public static Locale CRAFTED_ITEMS_FOOTER = new Locale("CRAFTED_ITEMS_FOOTER");
    public static Locale NO_PERMISSION = new Locale("NO_PERMISSION");
    public static Locale COMMAND_USAGE = new Locale("COMMAND_USAGE");
    public static Locale HELP_COMMAND_HEADER = new Locale("HELP_COMMAND_HEADER");
    public static Locale HELP_COMMAND_LINE = new Locale("HELP_COMMAND_LINE");
    public static Locale HELP_COMMAND_FOOTER = new Locale("HELP_COMMAND_FOOTER");
    public static Locale RELOAD_SUCCESS = new Locale("RELOAD_SUCCESS");
    public static Locale INVALID_PLAYER = new Locale("INVALID_PLAYER");
    public static Locale INVALID_CHEST = new Locale("INVALID_CHEST");
    public static Locale INVALID_AMOUNT = new Locale("INVALID_AMOUNT");
    public static Locale CHEST_GIVE_PLAYER = new Locale("CHEST_GIVE_PLAYER");
    public static Locale CHEST_RECIEVE = new Locale("CHEST_RECIEVE");
    public static Locale CHEST_INFO_HEADER = new Locale("CHEST_INFO_HEADER");
    public static Locale CHEST_INFO_NAME = new Locale("CHEST_INFO_NAME");
    public static Locale CHEST_INFO_TYPE = new Locale("CHEST_INFO_TYPE");
    public static Locale CHEST_INFO_SIZE = new Locale("CHEST_INFO_SIZE");
    public static Locale CHEST_INFO_DEFAULT_TITLE = new Locale("CHEST_INFO_DEFAULT_TITLE");
    public static Locale CHEST_INFO_SELL_MODE = new Locale("CHEST_INFO_SELL_MODE");
    public static Locale CHEST_INFO_RECIPES = new Locale("CHEST_INFO_RECIPES");
    public static Locale CHEST_INFO_FOOTER = new Locale("CHEST_INFO_FOOTER");
    public static Locale CHEST_PLACED = new Locale("CHEST_PLACED");
    public static Locale AUTO_SAVE = new Locale("AUTO_SAVE");
    public static Locale INVALID_BLOCK_CHEST = new Locale("INVALID_BLOCK_CHEST");
    public static Locale NOT_LINKED_CHEST = new Locale("NOT_LINKED_CHEST");
    public static Locale SELECT_ANOTHER_CHEST = new Locale("SELECT_ANOTHER_CHEST");
    public static Locale LINKED_SUCCEED = new Locale("LINKED_SUCCEED");
    public static Locale EXPAND_CHEST = new Locale("EXPAND_CHEST");
    public static Locale EXPAND_PURCHASED = new Locale("EXPAND_PURCHASED");
    public static Locale EXPAND_FAILED = new Locale("EXPAND_FAILED");
    public static Locale EXPAND_FAILED_CHEST_BROKEN = new Locale("EXPAND_FAILED_CHEST_BROKEN");
    public static Locale FORMAT_QUAD = new Locale("FORMAT_QUAD");
    public static Locale FORMAT_TRILLION = new Locale("FORMAT_TRILLION");
    public static Locale FORMAT_BILLION = new Locale("FORMAT_BILLION");
    public static Locale FORMAT_MILLION = new Locale("FORMAT_MILLION");
    public static Locale FORMAT_THOUSANDS = new Locale("FORMAT_THOUSANDS");
    public static Locale MONEY_EARNED_OFFLINE = new Locale("MONEY_EARNED_OFFLINE");
    public static Locale LEFTOVERS_ITEMS_WARNING = new Locale("LEFTOVERS_ITEMS_WARNING");


    private Locale(String identifier) {
        localeMap.put(identifier, this);
    }

    private String message;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEmpty() {
        return message == null || message.isEmpty();
    }

    public String getMessage(Object... objects) {
        if (message != null && !message.isEmpty()) {
            String msg = message;

            for (int i = 0; i < objects.length; i++)
                msg = msg.replace("{" + i + "}", objects[i].toString());

            return msg;
        }

        return null;
    }

    public void send(CommandSender sender, Object... objects) {
        String message = getMessage(objects);
        if (message != null && sender != null)
            sender.sendMessage(message);
    }

    private void setMessage(String message) {
        this.message = message;
    }

    public static void reload(WildChestsPlugin plugin) {
        WildChestsPlugin.log("Loading messages started...");
        long startTime = System.currentTimeMillis();
        int messagesAmount = 0;
        File file = new File(plugin.getDataFolder(), "lang.yml");

        if (!file.exists())
            WildChestsPlugin.getPlugin().saveResource("lang.yml", false);

        CommentedConfiguration cfg = CommentedConfiguration.loadConfiguration(file);

        try {
            cfg.syncWithConfig(file, plugin.getResource("lang.yml"));
        } catch (IOException error) {
            error.printStackTrace();
        }

        for (String identifier : localeMap.keySet()) {
            localeMap.get(identifier).setMessage(ChatColor.translateAlternateColorCodes('&', cfg.getString(identifier, "")));
            messagesAmount++;
        }

        WildChestsPlugin.log(" - Found " + messagesAmount + " messages in lang.yml.");
        WildChestsPlugin.log("Loading messages done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

}
