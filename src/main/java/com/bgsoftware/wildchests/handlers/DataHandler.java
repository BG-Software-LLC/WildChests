package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.common.databasebridge.sql.query.Column;
import com.bgsoftware.common.databasebridge.sql.query.QueryResult;
import com.bgsoftware.common.databasebridge.sql.transaction.DeleteSQLDatabaseTransaction;
import com.bgsoftware.common.databasebridge.sql.transaction.InsertSQLDatabaseTransaction;
import com.bgsoftware.common.databasebridge.sql.transaction.SQLDatabaseTransaction;
import com.bgsoftware.common.databasebridge.sql.transaction.UpdateSQLDatabaseTransaction;
import com.bgsoftware.common.databasebridge.transaction.IDatabaseTransaction;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.database.DBSession;
import com.bgsoftware.wildchests.listeners.ChunksListener;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WLinkedChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.BlockPosition;
import com.bgsoftware.wildchests.utils.ItemUtils;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
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
                if (!DBSession.createConnection(plugin)) {
                    WildChestsPlugin.log("Could not connect to database, closing server...");
                    Bukkit.getPluginManager().disablePlugin(plugin);
                }

                loadDatabase();
                loadOldDatabase();
            } catch (Exception ex) {
                ex.printStackTrace();
                Bukkit.getPluginManager().disablePlugin(plugin);
            }
        }, 2L);
    }

    public SQLDatabaseTransaction<?> saveChestInventory(Chest chest, @Nullable SQLDatabaseTransaction<?> transaction) {
        if (transaction == null) {
            String tableName = chest instanceof LinkedChest ? "linked_chests" : "chests";
            transaction = new UpdateSQLDatabaseTransaction(tableName,
                    Arrays.asList("inventories"), Arrays.asList("location"));
        }

        Inventory[] pages = chest instanceof LinkedChest && ((LinkedChest) chest).isLinkedIntoChest() ?
                null : chest.getPages();

        transaction
                .bindObject(serializeInventoriesInternal(pages))
                .bindObject(serializeLocationInternal(chest.getLocation()))
                .newBatch();

        return transaction;
    }

    public void saveChestInventory(Chest chest) {
        DBSession.execute(saveChestInventory(chest, null));
    }

    public SQLDatabaseTransaction<?> saveStorageUnitItem(WStorageChest chest, @Nullable SQLDatabaseTransaction<?> transaction) {
        if (transaction == null) {
            transaction = new UpdateSQLDatabaseTransaction("storage_units",
                    Arrays.asList("item", "amount"), Arrays.asList("location"));
        }

        transaction
                .bindObject(serializeItemInternal(chest.getItemStackUnsafe()))
                .bindObject(chest.getAmount().toString())
                .bindObject(serializeLocationInternal(chest.getLocation()))
                .newBatch();

        return transaction;
    }

    public SQLDatabaseTransaction<?> saveLinkedChest(WLinkedChest linkedChest, @Nullable SQLDatabaseTransaction<?> transaction) {
        if (transaction == null) {
            // UPDATE  SET linked_chest = ? WHERE location = ?
            transaction = new UpdateSQLDatabaseTransaction("linked_chests",
                    Arrays.asList("linked_chest"), Arrays.asList("location"));
        }

        transaction
                .bindObject(serializeLocationInternal(linkedChest.isLinkedIntoChest() ? linkedChest.getLinkedChest().getLocation() : null))
                .bindObject(serializeLocationInternal(linkedChest.getLocation()))
                .newBatch();

        return transaction;
    }

    public void saveLinkedChest(WLinkedChest linkedChest) {
        DBSession.execute(saveLinkedChest(linkedChest, null));
    }

    public void saveDatabase(Chunk chunk) {
        List<Chest> chestList = chunk == null ? plugin.getChestsManager().getChests() : plugin.getChestsManager().getChests(chunk);

        List<IDatabaseTransaction> transactionsToExecute = new LinkedList<>();

        saveRegularChestsInternal(chestList, transactionsToExecute);
        saveStorageUnitsInternal(chestList, transactionsToExecute);
        saveLinkedChestsInternal(chestList, transactionsToExecute);

        if (!transactionsToExecute.isEmpty())
            DBSession.execute(transactionsToExecute);
    }

    public void insertChest(Chest chest) {
        if (chest instanceof StorageChest) {
            WStorageChest storageChest = (WStorageChest) chest;
            DBSession.execute(new InsertSQLDatabaseTransaction("storage_units",
                    Arrays.asList("location", "placer", "chest_data", "item", "amount", "max_amount"))
                    .bindObject(serializeLocationInternal(chest.getLocation()))
                    .bindObject(chest.getPlacer().toString())
                    .bindObject(chest.getData().getName())
                    .bindObject(serializeItemInternal(storageChest.getItemStackUnsafe()))
                    .bindObject(storageChest.getAmount().toString())
                    .bindObject(storageChest.getMaxAmount().toString())
            );
        } else if (chest instanceof LinkedChest) {
            WLinkedChest linkedChest = (WLinkedChest) chest;
            boolean isLinkedIntoChest = linkedChest.isLinkedIntoChest();
            DBSession.execute(new InsertSQLDatabaseTransaction("linked_chests",
                    Arrays.asList("location", "placer", "chest_data", "inventories", "linked_chest"))
                    .bindObject(serializeLocationInternal(chest.getLocation()))
                    .bindObject(chest.getPlacer().toString())
                    .bindObject(chest.getData().getName())
                    .bindObject(serializeInventoriesInternal(isLinkedIntoChest ? null : linkedChest.getPages()))
                    .bindObject(serializeLocationInternal(isLinkedIntoChest ? linkedChest.getLinkedChest().getLocation() : null))
            );
        } else {
            DBSession.execute(new InsertSQLDatabaseTransaction("chests",
                    Arrays.asList("location", "placer", "chest_data", "inventories"))
                    .bindObject(serializeLocationInternal(chest.getLocation()))
                    .bindObject(chest.getPlacer().toString())
                    .bindObject(chest.getData().getName())
                    .bindObject(serializeInventoriesInternal(chest.getPages()))
            );
        }
    }

    public void deleteChest(Chest chest) {
        String tableName = chest instanceof StorageChest ? "storage_units" :
                chest instanceof LinkedChest ? "linked_chests" : "chests";

        DBSession.execute(new DeleteSQLDatabaseTransaction(tableName,
                Arrays.asList("location"))
                .bindObject(serializeLocationInternal(chest.getLocation()))
        );
    }

    private void saveRegularChestsInternal(List<Chest> chestList, List<IDatabaseTransaction> transactionsToExecute) {
        List<Chest> regularModifiedChests = chestList.stream()
                .filter(chest -> chest.getChestType() == ChestType.CHEST)
                .collect(Collectors.toList());
        if (!regularModifiedChests.isEmpty()) {
            SQLDatabaseTransaction<?> updateTransaction = null;
            for (Chest chest : regularModifiedChests) {
                updateTransaction = saveChestInventory(chest, updateTransaction);
            }
            transactionsToExecute.add(updateTransaction);
        }
    }

    private void saveStorageUnitsInternal(List<Chest> chestList, List<IDatabaseTransaction> transactionsToExecute) {
        List<Chest> storageModifiedChests = chestList.stream()
                .filter(chest -> chest.getChestType() == ChestType.STORAGE_UNIT)
                .collect(Collectors.toList());
        if (!storageModifiedChests.isEmpty()) {
            SQLDatabaseTransaction<?> updateTransaction = null;
            for (Chest chest : storageModifiedChests) {
                updateTransaction = saveStorageUnitItem((WStorageChest) chest, updateTransaction);
            }
            transactionsToExecute.add(updateTransaction);
        }
    }

    private void saveLinkedChestsInternal(List<Chest> chestList, List<IDatabaseTransaction> transactionsToExecute) {
        List<Chest> linkedChestsModifiedChests = chestList.stream()
                .filter(chest -> chest.getChestType() == ChestType.LINKED_CHEST)
                .collect(Collectors.toList());
        if (!linkedChestsModifiedChests.isEmpty()) {
            SQLDatabaseTransaction<?> updateTransaction = null;
            for (Chest chest : linkedChestsModifiedChests) {
                updateTransaction = saveChestInventory(chest, updateTransaction);
            }
            transactionsToExecute.add(updateTransaction);
        }
    }

    private void loadDatabase() {
        //Creating default tables
        DBSession.createTable("chests",
                new Column("location", "LONG_UNIQUE_TEXT PRIMARY KEY"),
                new Column("placer", "UUID"),
                new Column("chest_data", "TEXT"),
                new Column("inventories", "LONGTEXT"));
        DBSession.createTable("linked_chests",
                new Column("location", "LONG_UNIQUE_TEXT PRIMARY KEY"),
                new Column("placer", "UUID"),
                new Column("chest_data", "TEXT"),
                new Column("inventories", "LONGTEXT"),
                new Column("linked_chest", "LONG_UNIQUE_TEXT"));
        DBSession.createTable("storage_units",
                new Column("location", "LONG_UNIQUE_TEXT PRIMARY KEY"),
                new Column("placer", "UUID"),
                new Column("chest_data", "TEXT"),
                new Column("item", "TEXT"),
                new Column("amount", "TEXT"),
                new Column("max_amount", "TEXT"));
        DBSession.createTable("offline_payment",
                new Column("uuid", "UUID PRIMARY KEY"),
                new Column("payment", "TEXT"));

        DBSession.modifyColumnType("chests", "inventories", "LONGTEXT");
        DBSession.modifyColumnType("linked_chests", "inventories", "LONGTEXT");

        List<IDatabaseTransaction> transactionsToExecute = new LinkedList<>();

        //Loading all tables
        DBSession.select("chests", "", new QueryResult<ResultSet>()
                .onSuccess(resultSet -> loadResultSet(resultSet, "chests", transactionsToExecute)));
        DBSession.select("linked_chests", "", new QueryResult<ResultSet>()
                .onSuccess(resultSet -> loadResultSet(resultSet, "linked_chests", transactionsToExecute)));
        DBSession.select("storage_units", "", new QueryResult<ResultSet>()
                .onSuccess(resultSet -> loadResultSet(resultSet, "storage_units", transactionsToExecute)));

        if (!transactionsToExecute.isEmpty())
            DBSession.execute(transactionsToExecute);

        Scheduler.runTask(() -> {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks())
                    ChunksListener.handleChunkLoad(plugin, chunk);
            }
        }, 1L);
    }

    private void loadResultSet(ResultSet resultSet, String tableName, List<IDatabaseTransaction> transactionsToExecute) throws SQLException {
        DeleteSQLDatabaseTransaction deleteNullWorldTransaction = new DeleteSQLDatabaseTransaction(
                tableName, Arrays.asList("location"));

        boolean calledDeleteTransaction = false;

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
                    deleteNullWorldTransaction.bindObject(stringLocation).newBatch();
                    WildChestsPlugin.log("Deleted spawner (" + stringLocation + ") from database.");
                    calledDeleteTransaction = true;
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

        if (calledDeleteTransaction)
            transactionsToExecute.add(deleteNullWorldTransaction);
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

    private String serializeItemInternal(@Nullable ItemStack itemStack) {
        return ItemUtils.isEmpty(itemStack) ? "" : plugin.getNMSAdapter().serialize(itemStack);
    }

    private String serializeInventoriesInternal(@Nullable Inventory[] inventories) {
        return inventories == null || inventories.length == 0 ? "" : plugin.getNMSAdapter().serialize(inventories);
    }

    private static String serializeLocationInternal(@Nullable Location location) {
        return location == null ? "" : location.getWorld().getName() + ", " + location.getBlockX() + ", " +
                location.getBlockY() + ", " + location.getBlockZ();
    }

}
