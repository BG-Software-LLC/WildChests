package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.database.DatabaseObject;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.SQLHelper;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.listeners.ChunksListener;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.BlockPosition;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class DataHandler {

    private final WildChestsPlugin plugin;

    public DataHandler(WildChestsPlugin plugin) {
        this.plugin = plugin;
        Scheduler.runTask(() -> {
            try {
                SQLHelper.createConnection(plugin);
                loadDatabase();
                loadOldDatabase();
            } catch (Exception ex) {
                ex.printStackTrace();
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }, 2L);
    }

    public void saveDatabase(Chunk chunk, boolean async) {
        List<Chest> chestList = chunk == null ? plugin.getChestsManager().getChests() : plugin.getChestsManager().getChests(chunk);

        List<Chest> regularModifiedChests = chestList.stream()
                .filter(chest -> chest.getChestType() == ChestType.CHEST)
                .collect(Collectors.toList());
        List<Chest> storageModifiedChests = chestList.stream()
                .filter(chest -> chest.getChestType() == ChestType.STORAGE_UNIT)
                .collect(Collectors.toList());
        List<Chest> linkedChestsModifiedChests = chestList.stream()
                .filter(chest -> chest.getChestType() == ChestType.LINKED_CHEST)
                .collect(Collectors.toList());

        if (!regularModifiedChests.isEmpty()) {
            StatementHolder chestsUpdateHolder = Query.REGULAR_CHEST_UPDATE_INVENTORIES.getStatementHolder(null);
            chestsUpdateHolder.prepareBatch();
            regularModifiedChests.forEach(chest -> ((DatabaseObject) chest).setUpdateStatement(chestsUpdateHolder).addBatch());
            chestsUpdateHolder.execute(async);
        }

        if (!storageModifiedChests.isEmpty()) {
            StatementHolder chestsUpdateHolder = Query.STORAGE_UNIT_UPDATE_ITEM.getStatementHolder(null);
            chestsUpdateHolder.prepareBatch();
            storageModifiedChests.forEach(chest -> ((DatabaseObject) chest).setUpdateStatement(chestsUpdateHolder).addBatch());
            chestsUpdateHolder.execute(async);
        }

        if (!linkedChestsModifiedChests.isEmpty()) {
            StatementHolder chestsUpdateHolder = Query.LINKED_CHEST_UPDATE_INVENTORIES.getStatementHolder(null);
            chestsUpdateHolder.prepareBatch();
            linkedChestsModifiedChests.forEach(chest -> ((DatabaseObject) chest).setUpdateStatement(chestsUpdateHolder).addBatch());
            chestsUpdateHolder.execute(async);
        }

    }

    public void insertChest(WChest chest) {
        chest.executeInsertStatement(true);
    }

    private void loadDatabase() {
        //Creating default tables
        SQLHelper.executeUpdate("CREATE TABLE IF NOT EXISTS chests (location VARCHAR PRIMARY KEY, placer VARCHAR, chest_data VARCHAR, inventories VARCHAR);");
        SQLHelper.executeUpdate("CREATE TABLE IF NOT EXISTS linked_chests (location VARCHAR PRIMARY KEY, placer VARCHAR, chest_data VARCHAR, inventories VARCHAR, linked_chest VARCHAR);");
        SQLHelper.executeUpdate("CREATE TABLE IF NOT EXISTS storage_units (location VARCHAR PRIMARY KEY, placer VARCHAR, chest_data VARCHAR, item VARCHAR, amount VARCHAR, max_amount VARCHAR);");
        SQLHelper.executeUpdate("CREATE TABLE IF NOT EXISTS offline_payment (uuid VARCHAR PRIMARY KEY, payment VARCHAR);");

        //Loading all tables
        SQLHelper.executeQuery("SELECT * FROM chests;", resultSet -> loadResultSet(resultSet, "chests"));
        SQLHelper.executeQuery("SELECT * FROM linked_chests;", resultSet -> loadResultSet(resultSet, "linked_chests"));
        SQLHelper.executeQuery("SELECT * FROM storage_units;", resultSet -> loadResultSet(resultSet, "storage_units"));

        Scheduler.runTask(() -> {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks())
                    ChunksListener.handleChunkLoad(plugin, chunk);
            }
        }, 1L);
    }

    private void loadResultSet(ResultSet resultSet, String tableName) throws SQLException {
        while (resultSet.next()) {
            String stringLocation = resultSet.getString("location");

            UUID placer = UUID.fromString(resultSet.getString("placer"));

            String chestDataName = resultSet.getString("chest_data");
            ChestData chestData = plugin.getChestsManager().getChestData(chestDataName);

            if (chestData == null) {
                WildChestsPlugin.log("Couldn't load the location " + stringLocation);
                WildChestsPlugin.log("The chest data `" + chestDataName + "` does not exist.");
                continue;
            }

            BlockPosition position = BlockPosition.deserialize(stringLocation);

            if (plugin.getSettings().invalidWorldDelete) {
                World world = Bukkit.getWorld(position.getWorldName());
                if (world == null) {
                    SQLHelper.executeUpdate("DELETE FROM " + tableName + " WHERE location = '" + stringLocation + "';");
                    WildChestsPlugin.log("Deleted spawner (" + stringLocation + ") from database.");
                    continue;
                }
            }

            ChestsHandler.UnloadedChest unloadedChest;

            ChestType chestType = chestData.getChestType();
            if (chestType == ChestType.STORAGE_UNIT) {
                String item = resultSet.getString("item");
                String amount = resultSet.getString("amount");
                String maxAmount = resultSet.getString("max_amount");
                unloadedChest = new ChestsHandler.UnloadedStorageUnit(placer, position, chestData,
                        plugin.getNMSAdapter().deserialzeItem(item), new BigInteger(amount), new BigInteger(maxAmount));
            } else {
                String inventories = resultSet.getString("inventories");
                Location linkedChest = null;

                if (chestType == ChestType.LINKED_CHEST) {
                    linkedChest = LocationUtils.fromString(resultSet.getString("linked_chest"), true);
                }

                boolean executeUpdate = !inventories.isEmpty() && inventories.toCharArray()[0] != '*';

                unloadedChest = new ChestsHandler.UnloadedRegularChest(placer, position, chestData,
                        plugin.getNMSAdapter().deserialze(inventories), linkedChest, executeUpdate);
            }

            plugin.getChestsManager().addUnloadedChest(unloadedChest);
        }
    }

    private void loadOldDatabase() {
        File dataFolder = new File(plugin.getDataFolder(), "data");

        if (!dataFolder.exists())
            return;

        YamlConfiguration cfg;

        for (File chestFile : dataFolder.listFiles()) {
            try {
                cfg = YamlConfiguration.loadConfiguration(chestFile);

                UUID placer = UUID.fromString(cfg.getString("placer"));
                Location location = LocationUtils.fromString(chestFile.getName().replace(".yml", ""), false);
                ChestData chestData = plugin.getChestsManager().getChestData(cfg.getString("data"));

                WChest chest = (WChest) plugin.getChestsManager().addChest(placer, location, chestData);
                chest.loadFromFile(cfg);

                chestFile.delete();
            } catch (Exception ex) {
                WildChestsPlugin.log("Looks like the file " + chestFile.getName() + " is corrupted. Creating a backup file...");
                File backupFile = new File(plugin.getDataFolder(), "data-backup/" + chestFile.getName());
                copyFiles(chestFile, backupFile);
                ex.printStackTrace();
            }
        }

        if (dataFolder.listFiles().length == 0)
            dataFolder.delete();
    }

    private void copyFiles(File src, File dst) {
        try {
            if (!src.exists())
                return;
            if (!dst.exists()) {
                dst.getParentFile().mkdirs();
                dst.createNewFile();
            }

            BufferedReader reader = new BufferedReader(new FileReader(src));
            BufferedWriter writer = new BufferedWriter(new FileWriter(dst));

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write(System.getProperty("line.separator"));
            }

            reader.close();
            writer.close();
        } catch (IOException ex) {
            WildChestsPlugin.log("Couldn't create a backup file of " + src.getName() + "...");
            ex.printStackTrace();
        }
    }

}
