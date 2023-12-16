package com.bgsoftware.wildchests;

import com.bgsoftware.common.dependencies.DependenciesManager;
import com.bgsoftware.common.reflection.ReflectMethod;
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
import com.bgsoftware.wildchests.task.NotifierTask;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.Pair;
import com.bgsoftware.wildchests.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.UnsafeValues;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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

        Bukkit.getScheduler().cancelTasks(this);

        //Closing all inventories & closing chests
        for (Chest chest : chestsManager.getChests()) {
            boolean needClose = false;
            for (Inventory inventory : chest.getPages()) {
                List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
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
        Executor.stop();
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
            Executor.sync(() -> getServer().getPluginManager().disablePlugin(this));
            ex.printStackTrace();
        }
    }

    private boolean loadNMSAdapter() {
        String version = null;

        if (ServerVersion.isLessThan(ServerVersion.v1_17)) {
            version = getServer().getClass().getPackage().getName().split("\\.")[3];
        } else {
            ReflectMethod<Integer> getDataVersion = new ReflectMethod<>(UnsafeValues.class, "getDataVersion");
            int dataVersion = getDataVersion.invoke(Bukkit.getUnsafe());

            List<Pair<Integer, String>> versions = Arrays.asList(
                    new Pair<>(2729, null),
                    new Pair<>(2730, "v1_17"),
                    new Pair<>(2974, null),
                    new Pair<>(2975, "v1_18"),
                    new Pair<>(3336, null),
                    new Pair<>(3337, "v1_19"),
                    new Pair<>(3465, "v1_20_1"),
                    new Pair<>(3578, "v1_20_2"),
                    new Pair<>(3700, "v1_20_3")
            );

            for (Pair<Integer, String> versionData : versions) {
                if (dataVersion <= versionData.key) {
                    version = versionData.value;
                    break;
                }
            }

            if (version == null) {
                log("Data version: " + dataVersion);
            }
        }

        if (version != null) {
            try {
                nmsAdapter = (NMSAdapter) Class.forName(String.format("com.bgsoftware.wildchests.nms.%s.NMSAdapter", version)).newInstance();
                nmsInventory = (NMSInventory) Class.forName(String.format("com.bgsoftware.wildchests.nms.%s.NMSInventory", version)).newInstance();

                return true;
            } catch (Exception error) {
                error.printStackTrace();
            }
        }

        log("&cThe plugin doesn't support your minecraft version.");
        log("&cPlease try a different version.");

        return false;
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
