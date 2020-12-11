package com.bgsoftware.wildchests;

import com.bgsoftware.wildchests.api.WildChests;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.WildChestsAPI;
import com.bgsoftware.wildchests.command.CommandsHandler;
import com.bgsoftware.wildchests.database.Database;
import com.bgsoftware.wildchests.handlers.ChestsHandler;
import com.bgsoftware.wildchests.handlers.DataHandler;
import com.bgsoftware.wildchests.handlers.ProvidersHandler;
import com.bgsoftware.wildchests.handlers.SettingsHandler;
import com.bgsoftware.wildchests.listeners.BlockListener;
import com.bgsoftware.wildchests.listeners.ChunksListener;
import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.listeners.PlayerListener;
import com.bgsoftware.wildchests.nms.NMSAdapter;
import com.bgsoftware.wildchests.nms.NMSInventory;
import com.bgsoftware.wildchests.task.NotifierTask;
import com.bgsoftware.wildchests.utils.Executor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

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
    private NMSInventory nmsInventory;

    @Override
    public void onEnable() {
        plugin = this;
        log("******** ENABLE START ********");

        if(!loadNMSAdapter()){
            setEnabled(false);
            return;
        }

        chestsManager = new ChestsHandler();
        settingsHandler = new SettingsHandler(this);
        dataHandler = new DataHandler(this);
        providersHandler = new ProvidersHandler(this);

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunksListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        CommandsHandler commandsHandler = new CommandsHandler(this);
        getCommand("chests").setExecutor(commandsHandler);
        getCommand("chests").setTabCompleter(commandsHandler);

        Locale.reload(this);
        loadAPI();
        NotifierTask.start();

        if(Updater.isOutdated()) {
            log("");
            log("A new version is available (v" + Updater.getLatestVersion() + ")!");
            log("Version's description: \"" + Updater.getVersionDescription() + "\"");
            log("");
        }

        log("******** ENABLE DONE ********");

        Executor.sync(() -> {
            for(World world : Bukkit.getWorlds()){
                for(Chunk chunk : world.getLoadedChunks())
                    ChunksListener.handleChunkLoad(this, chunk);
            }
        }, 20L);

    }

    public boolean debug = false;

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);

        //Closing all inventories & closing chests
        for(Chest chest : chestsManager.getChests()){
            boolean needClose = false;
            for(Inventory inventory : chest.getPages()){
                List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
                for(HumanEntity humanEntity : viewers){
                    humanEntity.closeInventory();
                    needClose = true;
                }
            }
            if(needClose)
                nmsAdapter.playChestAction(chest.getLocation(), false);
        }

        for(Player player : Bukkit.getOnlinePlayers())
            player.closeInventory();

        int loadedChunks = 0;

        for(World world : Bukkit.getWorlds()){
            for(Chunk chunk : world.getLoadedChunks()) {
                dataHandler.saveDatabase(chunk);
                loadedChunks++;
            }
        }

        log("Chunks to save: " + loadedChunks);

        log("Terminating executor...");
        Executor.stop();
        log("Terminating database...");
        Database.stop();
    }

    private void loadAPI(){
        try{
            Field instance = WildChestsAPI.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, this);
        }catch(Exception ex){
            log("Failed to set-up API - disabling plugin...");
            setEnabled(false);
            Executor.sync(() -> getServer().getPluginManager().disablePlugin(this));
            ex.printStackTrace();
        }
    }

    private boolean loadNMSAdapter(){
        String version = getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            nmsAdapter = (NMSAdapter) Class.forName("com.bgsoftware.wildchests.nms.NMSAdapter_" + version).newInstance();
            nmsInventory = (NMSInventory) Class.forName("com.bgsoftware.wildchests.nms.NMSInventory_" + version).newInstance();
            return true;
        } catch (Exception ex){
            log("Error while loading adapter - unknown adapter " + version + "... Please contact @Ome_R");
            return false;
        }
    }

    public NMSAdapter getNMSAdapter() {
        return nmsAdapter;
    }

    public NMSInventory getNMSInventory() {
        return nmsInventory;
    }

    @Override
    public ChestsHandler getChestsManager() {
        return chestsManager;
    }

    @Override
    public ProvidersHandler getProviders() {
        return providersHandler;
    }

    @SuppressWarnings("unused")
    public SettingsHandler getSettings() {
        return settingsHandler;
    }

    public void setSettings(SettingsHandler settingsHandler){
        this.settingsHandler = settingsHandler;
    }

    @SuppressWarnings("unused")
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public static void log(String message){
        message = ChatColor.translateAlternateColorCodes('&', message);
        boolean colored = message.contains(ChatColor.COLOR_CHAR + "");
        String lastColor = colored ? ChatColor.getLastColors(message.substring(0, 2)) : "";
        for(String line : message.split("\n")){
            if(colored)
                Bukkit.getConsoleSender().sendMessage(lastColor + "[" + plugin.getDescription().getName() + "] " + line);
            else
                plugin.getLogger().info(line);
        }
    }

    public static WildChestsPlugin getPlugin() {
        return plugin;
    }
}
