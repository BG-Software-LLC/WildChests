package xyz.wildseries.wildchests.config;

@SuppressWarnings("unused")
public final class LangComments {

    @Comment("#########################################################")
    @Comment("##                                                     ##")
    @Comment("##                 WildChests Messages                 ##")
    @Comment("##                 Developed by Ome_R                  ##")
    @Comment("##                                                     ##")
    @Comment("#########################################################")
    public static String HEADER = "";

    @Comment("")
    @Comment("Only sent to operators")
    public static String AUTO_SAVE = "AUTO_SAVE";

    @Comment("")
    @Comment("Called when a player runs a command not in the required usage")
    public static String COMMAND_USAGE = "COMMAND_USAGE";

    @Comment("")
    @Comment("Called when using /chests give command.")
    public static String CHEST_GIVE_PLAYER = "CHEST_GIVE_PLAYER";

    @Comment("")
    @Comment("Called when using /chests info command.")
    public static String CHEST_INFO_HEADER = "CHEST_INFO_HEADER";

    @Comment("")
    @Comment("Called when a chest is placed down.")
    public static String CHEST_PLACED = "CHEST_PLACED";

    @Comment("")
    @Comment("Called when crafting items in auto-crafter")
    public static String CRAFTED_ITEMS_HEADER = "CRAFTED_ITEMS_HEADER";

    @Comment("")
    @Comment("Called when upgrading a chest")
    public static String EXPAND_CHEST = "EXPAND_CHEST";

    @Comment("")
    @Comment("Called when a player runs an invalid chests sub-command.")
    public static String HELP_COMMAND_HEADER = "HELP_COMMAND_HEADER";

    @Comment("")
    @Comment("Called when a player runs give command with an invalid argument.")
    public static String INVALID_PLAYER = "INVALID_PLAYER";

    @Comment("")
    @Comment("Called when using /chests link command")
    public static String INVALID_BLOCK_CHEST = "INVALID_BLOCK_CHEST";

    @Comment("")
    @Comment("Called when logging on and received money while being offline")
    public static String MONEY_EARNED_OFFLINE = "MONEY_EARNED_OFFLINE";

    @Comment("")
    @Comment("Called when running a chests command without permission.")
    public static String NO_PERMISSION = "NO_PERMISSION";

    @Comment("")
    @Comment("Called when a player successfully reloaded all configuration files.")
    public static String RELOAD_SUCCESS = "RELOAD_SUCCESS";

    @Comment("")
    @Comment("Called when selling items into sell-chest")
    public static String SOLD_CHEST_HEADER = "SOLD_CHEST_HEADER";

}
