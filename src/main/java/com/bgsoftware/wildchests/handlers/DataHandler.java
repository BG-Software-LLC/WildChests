package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.database.Database;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.data.ChestData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ResultOfMethodCallIgnored")
public final class DataHandler {

    private final WildChestsPlugin plugin;

    public DataHandler(WildChestsPlugin plugin){
        this.plugin = plugin;
        Executor.sync(() -> {
            try {
                Database.start(new File(plugin.getDataFolder(), "database.db"));
                loadDatabase();
                loadOldDatabase();
            }catch(Exception ex){
                ex.printStackTrace();
                Executor.sync(() -> Bukkit.getPluginManager().disablePlugin(plugin));
            }
        }, 2L);
    }

    public void saveDatabase(Chunk chunk){
        List<Chest> chestList = chunk == null ? plugin.getChestsManager().getChests() : plugin.getChestsManager().getChests(chunk);

        chestList.forEach(chest -> {
            if(chest instanceof LinkedChest) {
                LinkedChest linkedChest = ((LinkedChest) chest).getLinkedChest();

                Query.LINKED_CHEST_UPDATE_INVENTORY.insertParameters()
                        .setInventories(((LinkedChest) chest).isLinkedIntoChest() ? null : chest.getPages())
                        .setLocation(chest.getLocation())
                        .queue(chest);
                Query.LINKED_CHEST_UPDATE_TARGET.insertParameters()
                        .setLocation(linkedChest == null ? null : linkedChest.getLocation())
                        .setLocation(chest.getLocation())
                        .queue(chest);
            }
            else if(chest instanceof StorageChest){
                StorageChest storageChest = (StorageChest) chest;
                Query.STORAGE_UNIT_UPDATE_INVENTORY.insertParameters()
                        .setItemStack(storageChest.getItemStack())
                        .setObject(storageChest.getAmount().toString())
                        .setLocation(storageChest.getLocation())
                        .queue(chest);
            }
            else{
                Query.REGULAR_CHEST_UPDATE_INVENTORY.insertParameters()
                        .setInventories(chest.getPages())
                        .setLocation(chest.getLocation())
                        .queue(chest);
            }
        });

        chestList.forEach(plugin.getNMSInventory()::removeTileEntity);
    }

    public void insertChest(WChest chest){
        chest.executeInsertQuery();
    }

    private void loadDatabase(){
        //Creating default tables
        Database.executeUpdate("CREATE TABLE IF NOT EXISTS chests (location VARCHAR PRIMARY KEY, placer VARCHAR, chest_data VARCHAR, inventories VARCHAR);");
        Database.executeUpdate("CREATE TABLE IF NOT EXISTS linked_chests (location VARCHAR PRIMARY KEY, placer VARCHAR, chest_data VARCHAR, inventories VARCHAR, linked_chest VARCHAR);");
        Database.executeUpdate("CREATE TABLE IF NOT EXISTS storage_units (location VARCHAR PRIMARY KEY, placer VARCHAR, chest_data VARCHAR, item VARCHAR, amount VARCHAR, max_amount VARCHAR);");
        Database.executeUpdate("CREATE TABLE IF NOT EXISTS offline_payment (uuid VARCHAR PRIMARY KEY, payment VARCHAR);");

        //Loading all tables
        Database.executeQuery("SELECT * FROM chests;", resultSet -> loadResultSet(resultSet, "chests"));
        Database.executeQuery("SELECT * FROM linked_chests;", resultSet -> loadResultSet(resultSet, "linked_chests"));
        Database.executeQuery("SELECT * FROM storage_units;", resultSet -> loadResultSet(resultSet, "storage_units"));

        //Load offline payments
        Database.executeQuery("SELECT * FROM offline_payment;", resultSet -> {
            while(resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                String payment = resultSet.getString("payment");
                plugin.getOfflinePayments().depositItems(uuid, payment);
            }
        });

        Database.executeUpdate("DELETE FROM offline_payment;");
    }

    private void loadResultSet(ResultSet resultSet, String tableName) throws SQLException{
        while (resultSet.next()) {
            UUID placer = UUID.fromString(resultSet.getString("placer"));
            String stringLocation = resultSet.getString("location");
            String errorMessage = null;

            try {
                if (Bukkit.getWorld(stringLocation.split(", ")[0]) == null) {
                    errorMessage = "Null world.";
                } else {
                    Location location = LocationUtils.fromString(stringLocation);
                    ChestData chestData = plugin.getChestsManager().getChestData(resultSet.getString("chest_data"));
                    WChest chest = plugin.getChestsManager().loadChest(placer, location, chestData);
                    chest.loadFromData(resultSet);
                }
            }catch(Exception ex){
                errorMessage = ex.getMessage();
            }

            if(errorMessage != null) {
                WildChestsPlugin.log("Couldn't load the location " + stringLocation);
                WildChestsPlugin.log(errorMessage);
                if(errorMessage.contains("Null") && plugin.getSettings().invalidWorldDelete){
                    Database.executeUpdate("DELETE FROM " + tableName + " WHERE location = '" + stringLocation + "';");
                    WildChestsPlugin.log("Deleted spawner (" + stringLocation + ") from database.");
                }
            }
        }
    }

    private void loadOldDatabase(){
        File dataFolder = new File(plugin.getDataFolder(), "data");

        if(!dataFolder.exists())
            return;

        YamlConfiguration cfg;

        for(File chestFile : dataFolder.listFiles()){
            try {
                cfg = YamlConfiguration.loadConfiguration(chestFile);

                UUID placer = UUID.fromString(cfg.getString("placer"));
                Location location = LocationUtils.fromString(chestFile.getName().replace(".yml", ""));
                ChestData chestData = plugin.getChestsManager().getChestData(cfg.getString("data"));

                WChest chest = (WChest) plugin.getChestsManager().addChest(placer, location, chestData);
                chest.loadFromFile(cfg);

                chestFile.delete();
            }catch(Exception ex){
                WildChestsPlugin.log("Looks like the file " + chestFile.getName() + " is corrupted. Creating a backup file...");
                File backupFile = new File(plugin.getDataFolder(), "data-backup/" + chestFile.getName());
                copyFiles(chestFile, backupFile);
                ex.printStackTrace();
            }
        }

        if(dataFolder.listFiles().length == 0)
            dataFolder.delete();
    }

    private void copyFiles(File src, File dst){
        try {
            if(!src.exists())
                return;
            if(!dst.exists()){
                dst.getParentFile().mkdirs();
                dst.createNewFile();
            }

            BufferedReader reader = new BufferedReader(new FileReader(src));
            BufferedWriter writer = new BufferedWriter(new FileWriter(dst));

            String line;
            while((line = reader.readLine()) != null){
                writer.write(line);
                writer.write(System.getProperty("line.separator"));
            }

            reader.close();
            writer.close();
        }catch(IOException ex){
            WildChestsPlugin.log("Couldn't create a backup file of " + src.getName() + "...");
            ex.printStackTrace();
        }
    }

}
