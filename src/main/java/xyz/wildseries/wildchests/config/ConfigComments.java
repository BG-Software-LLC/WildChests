package xyz.wildseries.wildchests.config;

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
    @Comment("How much time should be passed between saves? (in ticks)")
    @Comment("Set 0 to disable (not recommended. saving is done async, and will not lag your server)")
    public static String SAVE_INTERVAL = "save-interval";

    @Comment("")
    @Comment("How much time should be passed between sell notifies? (in ticks)")
    @Comment("Set 0 to disable this feature.")
    public static String NOTIFIER_INTERVAL = "notifier-interval";

    @Comment("")
    @Comment("Should the chest-task be enabled?")
    @Comment("Please note: The task runs every second and checks for items in the original chest (May cause lags)")
    public static String CHEST_TASK = "chest-task";

    @Comment("")
    @Comment("The plugin checks if ShopGUIPlus or Essentials is installed.")
    @Comment("If both are not installed, it will use this list for the sell-chest")
    @Comment("prices list. If both are installed, ShopGUIPlus's prices will be used.")
    public static String PRICES_LIST = "prices-list";

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
    @Comment("There are sections that can be applied to all of the chests:")
    @Comment("size: Amount of rows the chest will have (INTEGER) (Will be overridden by pages' size)")
    @Comment("title: Global title for all pages. Will be overridden by page's title (STRING)")
    @Comment("sell-chest: Should items in this chest will be sold to the chest placer? (BOOLEAN)")
    @Comment("crafter-chest: List of recipes that will take action in this chest (LIST)")
    @Comment("     Please follow \"TYPE\" and \"TYPE:DATA\" formats")
    @Comment("pages: Section that handles all settings for pages")
    @Comment("pages.default: Default amount of pages (INTEGER) [REQUIRED FOR PAGES]")
    @Comment("pages.<#>: Section that handles all settings for a specific page")
    @Comment("pages.<#>.price: Price for upgrading to that page (DOUBLE) [REQUIRED]")
    @Comment("pages.<#>.title: Custom title for that page.")
    @Comment("item.name: Custom name for the chest (STRING)")
    @Comment("item.lore: Custom lore for the chest (LIST)")
    @Comment("")
    public static String CHESTS = "chests";

}
