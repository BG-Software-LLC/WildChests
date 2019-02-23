package xyz.wildseries.wildchests;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.wildseries.wildchests.api.WildChests;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.command.CommandsHandler;
import xyz.wildseries.wildchests.handlers.ChestsHandler;
import xyz.wildseries.wildchests.handlers.DataHandler;
import xyz.wildseries.wildchests.handlers.ProvidersHandler;
import xyz.wildseries.wildchests.handlers.SettingsHandler;
import xyz.wildseries.wildchests.listeners.BlockListener;
import xyz.wildseries.wildchests.listeners.InventoryListener;
import xyz.wildseries.wildchests.listeners.PlayerListener;
import xyz.wildseries.wildchests.metrics.Metrics;
import xyz.wildseries.wildchests.nms.NMSAdapter;
import xyz.wildseries.wildchests.task.NotifierTask;
import xyz.wildseries.wildchests.task.SaveTask;

import java.util.ArrayList;
import java.util.List;

public final class WildChestsPlugin extends JavaPlugin implements WildChests {

    private static WildChestsPlugin plugin;

    private ChestsHandler chestsManager;
    private SettingsHandler settingsHandler;
    private DataHandler dataHandler;
    private ProvidersHandler providersHandler;

    private NMSAdapter nmsAdapter;

    @Override
    public void onEnable() {
        plugin = this;
        new Metrics(this);

        log("******** ENABLE START ********");

        chestsManager = new ChestsHandler();
        settingsHandler = new SettingsHandler(this);
        dataHandler = new DataHandler(this);

        if(!loadNMSAdapter()){
            setEnabled(false);
            return;
        }

        providersHandler = new ProvidersHandler();

        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        getCommand("chests").setExecutor(commandsHandler);
        getCommand("chests").setTabCompleter(commandsHandler);

        Locale.reload();
        SaveTask.start();
        NotifierTask.start();

        if(!isVaultEnabled()){
            log("");
            log("If you want sell-chests to be enabled, please install Vault & Economy plugin.");
            log("");
        }

        if(Updater.isOutdated()) {
            log("");
            log("A new version is available (v" + Updater.getLatestVersion() + ")!");
            log("Version's description: \"" + Updater.getVersionDescription() + "\"");
            log("");
        }

        log("******** ENABLE DONE ********");
    }

    @Override
    public void onDisable() {
        //Closing all inventories & closing chests
        for(Chest chest : chestsManager.getChests()){
            int index = 0;
            Inventory inventory;
            boolean needClose = false;
            while((inventory = chest.getPage(index)) != null){
                List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
                for(HumanEntity humanEntity : viewers){
                    humanEntity.closeInventory();
                    needClose = true;
                }
                index++;
            }
            if(needClose)
                nmsAdapter.playChestAction(chest.getLocation(), false);
        }
        for(Player player : Bukkit.getOnlinePlayers())
            player.closeInventory();
        Bukkit.getScheduler().cancelTasks(this);
        dataHandler.saveDatabase();
    }

    private boolean isVaultEnabled() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null)
            return false;

        providersHandler.enableVault();

        return true;
    }

    private boolean loadNMSAdapter(){
        String version = getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            nmsAdapter = (NMSAdapter) Class.forName("xyz.wildseries.wildchests.nms.NMSAdapter_" + version).newInstance();
            return true;
        } catch (Exception ex){
            log("Error while loading adapter - unknown adapter " + version + "... Please contact @Ome_R");
            return false;
        }
    }

    public NMSAdapter getNMSAdapter() {
        return nmsAdapter;
    }

    @Override
    public ChestsHandler getChestsManager() {
        return chestsManager;
    }

    @SuppressWarnings("unused")
    public SettingsHandler getSettings() {
        return settingsHandler;
    }

    @SuppressWarnings("unused")
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public ProvidersHandler getProviders() {
        return providersHandler;
    }

    public static String getVersion(){
        return plugin.getNMSAdapter().getVersion();
    }

    public static void log(String message){
        plugin.getLogger().info(message);
    }

    public static WildChestsPlugin getPlugin() {
        return plugin;
    }
}
