package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.objects.chests.WChest;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.objects.WLocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions", "WeakerAccess"})
public final class DataHandler {

    private WildChestsPlugin plugin;

    public DataHandler(WildChestsPlugin plugin){
        this.plugin = plugin;
        Bukkit.getScheduler().runTask(plugin, this::loadDatabase);
    }

    public void saveDatabase(){
        long startTime = System.currentTimeMillis();
        List<Chest> chests = plugin.getChestsManager().getChests();

        File dataFolder = new File(plugin.getDataFolder(), "data");
        if(dataFolder.exists()){
            for(File file : dataFolder.listFiles())
                file.delete();
        }

        for(Chest chest : chests){
            try {
                File file = new File(plugin.getDataFolder(), "data/" + WLocation.of(chest.getLocation()).toString() + ".yml");

                if (file.exists())
                    file.delete();

                file.getParentFile().mkdirs();
                file.createNewFile();

                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                ((WChest) chest).saveIntoFile(cfg);

                cfg.save(file);
            }catch(IOException ex){
                WildChestsPlugin.log("Couldn't save chest at " + WLocation.of(chest.getLocation()).toString());
            }
        }

        WildChestsPlugin.log("Successfully saved database (" + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public void loadDatabase(){
        WildChestsPlugin.log("Loading database started...");
        long startTime = System.currentTimeMillis();
        int chestsAmount = 0;
        File dataFolder = new File(plugin.getDataFolder(), "data");

        if(!dataFolder.exists())
            return;

        YamlConfiguration cfg;

        for(File chestFile : dataFolder.listFiles()){
            try {
                cfg = YamlConfiguration.loadConfiguration(chestFile);

                UUID placer = UUID.fromString(cfg.getString("placer"));
                Location location = WLocation.of(chestFile.getName().replace(".yml", "")).getLocation();
                ChestData chestData = plugin.getChestsManager().getChestData(cfg.getString("data"));

                WChest chest = (WChest) plugin.getChestsManager().addChest(placer, location, chestData);

                chest.loadFromFile(cfg);

                chestsAmount++;
            }catch(Exception ex){
                WildChestsPlugin.log("Looks like the file " + chestFile.getName() + " is corrupted. Creating a backup file...");
                File backupFile = new File(plugin.getDataFolder(), "data-backup/" + chestFile.getName());
                copyFiles(chestFile, backupFile);
                ex.printStackTrace();
            }
        }

        WildChestsPlugin.log(" - Found " + chestsAmount + " chests in files.");
        WildChestsPlugin.log("Loading database done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
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
