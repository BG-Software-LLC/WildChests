package com.bgsoftware.wildchests;

import com.bgsoftware.common.dependencies.DependenciesManager;
import com.bgsoftware.common.nmsloader.INMSLoader;
import com.bgsoftware.common.nmsloader.NMSHandlersFactory;
import com.bgsoftware.common.nmsloader.NMSLoadException;
import com.bgsoftware.common.nmsloader.config.NMSConfiguration;
import com.bgsoftware.common.updater.Updater;
import com.bgsoftware.wildchests.api.WildChests;
import com.bgsoftware.wildchests.api.WildChestsAPI;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.command.CommandsHandler;
import com.bgsoftware.wildchests.database.SQLHelper;
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
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.task.NotifierTask;
import com.bgsoftware.wildchests.utils.DatabaseThread;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

public final class WildChestsPlugin extends JavaPlugin implements WildChests {

    private static WildChestsPlugin plugin;

    private final Updater updater = new Updater(this, "wildchests");

    private ChestsHandler chestsManager;
    private SettingsHandler settingsHandler;
    private DataHandler dataHandler;
    private ProvidersHandler providersHandler;

    private NMSAdapter nmsAdapter;
    private NMSInventory nmsInventory;

    private boolean shouldEnable = true;

    @Override
    public void onLoad() {
        plugin = this;

        DependenciesManager.inject(this);

        shouldEnable = loadNMSAdapter();

        new Metrics(this, 4102);
    }

    @Override
    public void onEnable() {
        if (!shouldEnable) {
            setEnabled(false);
            return;
        }

        log("******** ENABLE START ********");

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

        if (updater.isOutdated()) {
            log("");
            log("A new version is available (v" + updater.getLatestVersion() + ")!");
            log("Version's description: \"" + updater.getVersionDescription() + "\"");
            log("");
        }

        log("******** ENABLE DONE ********");
    }

    @Override
    public void onDisable() {
        if (!shouldEnable)
            return;

        Scheduler.cancelTasks();

        //Closing all inventories & closing chests
        for (Chest chest : chestsManager.getChests()) {
            boolean needClose = false;
            for (Inventory inventory : chest.getPages()) {
                List<HumanEntity> viewers = new LinkedList<>(inventory.getViewers());
                for (HumanEntity humanEntity : viewers) {
                    humanEntity.closeInventory();
                    needClose = true;
                }
            }
            if (needClose)
                nmsAdapter.playChestAction(chest.getLocation(), false);
        }

        for (Player player : Bukkit.getOnlinePlayers())
            player.closeInventory();

        int loadedChunks = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                dataHandler.saveDatabase(chunk, false);
                loadedChunks++;
            }
        }

        log("Chunks to save: " + loadedChunks);

        log("Terminating executor...");
        DatabaseThread.stop();
        log("Terminating database...");
        SQLHelper.close();
    }

    private void loadAPI() {
        try {
            Field instance = WildChestsAPI.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, this);
        } catch (Exception ex) {
            log("Failed to set-up API - disabling plugin...");
            setEnabled(false);
            Scheduler.runTask(() -> getServer().getPluginManager().disablePlugin(this));
            ex.printStackTrace();
        }
    }

    private boolean loadNMSAdapter() {
        try {
            INMSLoader nmsLoader = NMSHandlersFactory.createNMSLoader(this, NMSConfiguration.forPlugin(this));
            this.nmsAdapter = nmsLoader.loadNMSHandler(NMSAdapter.class);
            this.nmsInventory = nmsLoader.loadNMSHandler(NMSInventory.class);

            return true;
        } catch (NMSLoadException error) {
            log("&cThe plugin doesn't support your minecraft version.");
            log("&cPlease try a different version.");
            error.printStackTrace();

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

    public void setSettings(SettingsHandler settingsHandler) {
        this.settingsHandler = settingsHandler;
    }

    @SuppressWarnings("unused")
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public Updater getUpdater() {
        return updater;
    }

    public static void log(String message) {
        message = ChatColor.translateAlternateColorCodes('&', message);
        boolean colored = message.contains(ChatColor.COLOR_CHAR + "");
        String lastColor = colored ? ChatColor.getLastColors(message.substring(0, 2)) : "";
        for (String line : message.split("\n")) {
            if (colored)
                Bukkit.getConsoleSender().sendMessage(lastColor + "[" + plugin.getDescription().getName() + "] " + line);
            else
                plugin.getLogger().info(line);
        }
    }

    public static WildChestsPlugin getPlugin() {
        return plugin;
    }
}
