package com.bgsoftware.wildchests.config;

@SuppressWarnings("unused")
public final class ConfigComments {

    @Comment("#########################################################")
    @Comment("##                                                     ##")
    @Comment("##              WildChests Configuration               ##")
    @Comment("##                 Developed by Ome_R                  ##")
    @Comment("##                                                     ##")
    @Comment("#########################################################")
    public static String HEADER = "";

    @Comment("")
    @Comment("How much time should be passed between sell and crafting notifies? (in ticks)")
    @Comment("Set 0 to disable this feature.")
    public static String NOTIFIER_INTERVAL = "notifier-interval";

    @Comment("")
    @Comment("When enabled, a gui will be popped to confirm chest expand purchase. Otherwise, it will be done in chat.")
    public static String CONFIRM_GUI = "confirm-gui";

    @Comment("")
    @Comment("Specify a command to be executed when a sell is running.")
    @Comment("{player-name} - use as the player's name")
    @Comment("{price} - use as the price of item")
    @Comment("Set to '' in order to disable the feature.")
    public static String SELL_COMMAND = "sell-command";

    @Comment("")
    @Comment("Should the chest item be dropped on explosions?")
    @Comment("Set a chance of it to get dropped between 0 and 100.")
    public static String EXPLODE_DROP_CHANCE = "explode-drop-chance";

    @Comment("")
    @Comment("The plugin checks if ShopGUIPlus or Essentials is installed.")
    @Comment("If both are not installed, it will use this list for the sell-chest")
    @Comment("prices list. If both are installed, ShopGUIPlus's prices will be used.")
    public static String PRICES_LIST = "prices-list";

    @Comment("")
    @Comment("What prices provider should the plugin use? (ShopGUIPlus, Essentials, WildChests)")
    public static String PRICES_PROVIDER = "prices-provider";

    @Comment("")
    @Comment("The plugin brings tons of new custom and unique chests to your server. All the chests are configurable, and")
    @Comment("you can create and mix between them. You can create chests that stores infinite amount of items, chests that")
    @Comment("are connected to player vaults or factions, linked chests and many more!")
    @Comment(" ")
    @Comment("*** Chest Mode ***")
    @Comment("Chests in this mode react like regular chests. This is the base mode, and used for creating")
    @Comment("sell chests and larger chests.")
    @Comment(" ")
    @Comment("*** Linked Mode ***")
    @Comment("Chests in this mode are all linked together, and work like ender chests, but globally.")
    @Comment(" ")
    @Comment("*** Storage Units Mode ***")
    @Comment("Chests in this mode will have unlimited amount of storage, but only for one specific item.")
    @Comment("Note: Auto sell & auto craft modes doesn't work for this mode!")
    @Comment(" ")
    @Comment("There are sections that can be applied to all of the chests:")
    @Comment("size: Amount of rows the chest will have (INTEGER) (Will be overridden by pages' size)")
    @Comment("title: Global title for all pages. Will be overridden by page's title (STRING)")
    @Comment("sell-chest: Should items in this chest will be sold to the chest placer? (BOOLEAN)")
    @Comment("crafter-chest: List of recipes that will take action in this chest (LIST)")
    @Comment("     Please follow \"TYPE\" and \"TYPE:DATA\" formats")
    @Comment("hopper-filter: When enabled, only craftable items will go into hoppers below (BOOLEAN)")
    @Comment("pages: Section that handles all settings for pages")
    @Comment("pages.default: Default amount of pages (INTEGER) [REQUIRED FOR PAGES]")
    @Comment("pages.<#>: Section that handles all settings for a specific page")
    @Comment("pages.<#>.price: Price for upgrading to that page (DOUBLE) [REQUIRED]")
    @Comment("pages.<#>.title: Custom title for that page.")
    @Comment("item.name: Custom name for the chest (STRING)")
    @Comment("item.lore: Custom lore for the chest (LIST)")
    @Comment("multiplier: Money multiplier for sell chests. (BOOLEAN)")
    @Comment("auto-collect: Should items get directly into inventory after breaking? (BOOLEAN)")
    @Comment("auto-suction.range: Should items inside the specified range will get suctioned into the chest? (INTEGER)")
    @Comment("auto-suction.chunk: Should items inside the chunk will get suctioned into the chest? (BOOLEAN)")
    @Comment("     If you set it to true, the range will be checked as y-range only.")
    @Comment("")
    public static String CHESTS = "chests";

}
