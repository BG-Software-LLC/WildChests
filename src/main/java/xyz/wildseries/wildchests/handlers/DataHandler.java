package xyz.wildseries.wildchests.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.chests.LinkedChest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.objects.WLocation;

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
        loadDatabase();
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

                cfg.set("placer", chest.getPlacer().toString());
                cfg.set("data", chest.getData().getName());

                int index = 0;
                Inventory inventory;

                while((inventory = chest.getPage(index)) != null){
                    cfg.set("inventory." + index, "empty");
                    for(int slot = 0; slot < inventory.getSize(); slot++){
                        ItemStack itemStack = inventory.getItem(slot);

                        if(itemStack == null)
                            continue;

                        cfg.set("inventory." + index + "." + slot, itemStack);
                    }
                    index++;
                }

                if(chest instanceof LinkedChest){
                    if(((LinkedChest) chest).isLinkedIntoChest())
                        cfg.set("linked-chest", WLocation.of(((LinkedChest) chest).getLinkedChest().getLocation()).toString());
                }

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

                Chest chest = plugin.getChestsManager().addChest(placer, location, chestData);

                if (cfg.contains("inventory")) {
                    for (String inventoryIndex : cfg.getConfigurationSection("inventory").getKeys(false)) {
                        Inventory inventory = Bukkit.createInventory(null, chestData.getDefaultSize(), chestData.getTitle(Integer.valueOf(inventoryIndex) + 1));
                        if(cfg.isConfigurationSection("inventory." + inventoryIndex)){
                            for (String slot : cfg.getConfigurationSection("inventory." + inventoryIndex).getKeys(false)) {
                                try {
                                    inventory.setItem(Integer.valueOf(slot), cfg.getItemStack("inventory." + inventoryIndex + "." + slot));
                                } catch (Exception ex) {
                                    break;
                                }
                            }
                        }
                        chest.setPage(Integer.valueOf(inventoryIndex), inventory);
                    }
                }

                if (cfg.contains("linked-chest")) {
                    //We want to run it on the first tick, after all chests were loaded.
                    Location linkedChest = WLocation.of(cfg.getString("linked-chest")).getLocation();
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            ((LinkedChest) chest).linkIntoChest(plugin.getChestsManager().getLinkedChest(linkedChest)), 1L);
                }

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
