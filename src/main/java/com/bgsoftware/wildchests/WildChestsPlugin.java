package com.bgsoftware.wildchests;

import com.bgsoftware.wildchests.api.WildChestsAPI;
import com.bgsoftware.wildchests.command.CommandsHandler;
import com.bgsoftware.wildchests.handlers.ChestsHandler;
import com.bgsoftware.wildchests.handlers.DataHandler;
import com.bgsoftware.wildchests.handlers.ProvidersHandler;
import com.bgsoftware.wildchests.handlers.SettingsHandler;
import com.bgsoftware.wildchests.listeners.ItemsListener;
import com.bgsoftware.wildchests.nms.NMSAdapter;
import com.bgsoftware.wildchests.task.NotifierTask;
import com.bgsoftware.wildchests.task.SaveTask;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.bgsoftware.wildchests.api.WildChests;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.listeners.BlockListener;
import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.listeners.PlayerListener;

import java.lang.reflect.Field;
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
        getServer().getPluginManager().registerEvents(new ItemsListener(this), this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        getCommand("chests").setExecutor(commandsHandler);
        getCommand("chests").setTabCompleter(commandsHandler);

        Locale.reload();
        loadAPI();
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

    private void loadAPI(){
        try{
            Field instance = WildChestsAPI.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, this);
        }catch(Exception ex){
            log("Failed to set-up API - disabling plugin...");
            setEnabled(false);
            Bukkit.getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
            ex.printStackTrace();
        }
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
            nmsAdapter = (NMSAdapter) Class.forName("com.bgsoftware.wildchests.nms.NMSAdapter_" + version).newInstance();
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
