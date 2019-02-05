package xyz.wildseries.wildchests.handlers;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.ChestType;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.api.objects.data.InventoryData;
import xyz.wildseries.wildchests.config.CommentedConfiguration;
import xyz.wildseries.wildchests.config.ConfigComments;
import xyz.wildseries.wildchests.hooks.PricesProvider_Default;
import xyz.wildseries.wildchests.objects.data.WChestData;
import xyz.wildseries.wildchests.objects.data.WInventoryData;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SettingsHandler {

    public final long saveInterval;
    public final long notifyInterval;
    public final boolean chestTask;
    public final boolean confirmGUI;
    public final String sellCommand;

    public SettingsHandler(WildChestsPlugin plugin){
        WildChestsPlugin.log("Loading configuration started...");
        long startTime = System.currentTimeMillis();
        int chestsAmount = 0;
        File file = new File(plugin.getDataFolder(), "config.yml");

        if(!file.exists())
            plugin.saveResource("config.yml", false);

        CommentedConfiguration cfg = new CommentedConfiguration(ConfigComments.class);
        cfg.load(file);

        cfg.resetYamlFile(plugin, "config.yml");

        saveInterval = cfg.getLong("save-interval", 6000);
        notifyInterval = cfg.getLong("notifier-interval", 12000);
        chestTask = cfg.getBoolean("chest-task", false);
        confirmGUI = cfg.getBoolean("confirm-gui", false);
        sellCommand = cfg.getString("sell-command", "");

        Map<String, Double> prices = new HashMap<>();

        if(cfg.contains("prices-list")){
            for(String line : cfg.getStringList("prices-list")){
                String[] split = line.split(":");
                try {
                    if (split.length == 2) {
                        prices.put(split[0], Double.valueOf(split[1]));
                    } else if (split.length == 3) {
                        prices.put(split[0] + ":" + split[1], Double.valueOf(split[2]));
                    }
                } catch(IllegalArgumentException ignored){}
            }
        }

        try{
            Field pricesMap = PricesProvider_Default.class.getDeclaredField("prices");
            pricesMap.setAccessible(true);
            pricesMap.set(null, prices);
        } catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
            return;
        }

        Set<ChestData> chestsData = new HashSet<>();

        for(String name : cfg.getConfigurationSection("chests").getKeys(false)){
            ChestType chestType;

            if(!cfg.contains("chests." + name + ".chest-mode")){
                WildChestsPlugin.log("Couldn''t find chest-mode for " + name + " - skipping...");
                continue;
            }

            try{
                chestType = ChestType.valueOf(cfg.getString("chests." + name + ".chest-mode"));
            }catch(IllegalArgumentException ex){
                WildChestsPlugin.log("Found an invalid chest-type for " + name + " - skipping...");
                continue;
            }

            if(!cfg.contains("chests." + name + ".item.name") && !cfg.contains("chests." + name + ".item.lore")){
                WildChestsPlugin.log("Found an invalid item for " + name + " - skipping...");
                continue;
            }

            ItemStack itemStack = new ItemStack(Material.CHEST);
            ItemMeta itemMeta = itemStack.getItemMeta();

            if(cfg.contains("chests." + name + ".item.name")){
                itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', cfg.getString("chests." + name + ".item.name")));
            }

            if(cfg.contains("chests." + name + ".item.lore")){
                List<String> lore = new ArrayList<>();

                for(String line : cfg.getStringList("chests." + name + ".item.lore"))
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));

                itemMeta.setLore(lore);
            }

            itemStack.setItemMeta(itemMeta);

            ChestData chestData = new WChestData(name, itemStack, chestType);

            if(cfg.contains("chests." + name + ".size")){
                int rows = cfg.getInt("chests." + name + ".size");

                if(rows < 1 || rows > 6){
                    WildChestsPlugin.log("Found an invalid rows amount for " + name + " - setting default size to 3 rows");
                    rows = 3;
                }

                chestData.setDefaultSize(rows * 9);
            }

            if(cfg.contains("chests." + name + ".title")){
                chestData.setDefaultTitle(ChatColor.translateAlternateColorCodes('&', cfg.getString("chests." + name + ".title")));
            }

            if(cfg.getBoolean("chests." + name + ".sell-mode", false)){
                chestData.setSellMode(true);
            }

            if(cfg.contains("chests." + name + ".crafter-chest")){
                chestData.setAutoCrafter(cfg.getStringList("chests." + name + ".crafter-chest"));
            }

            if(cfg.contains("chests." + name +".hopper-filter")){
                chestData.setHopperFilter(cfg.getBoolean("chests." + name +".hopper-filter"));
            }

            if(cfg.contains("chests." + name + ".pages")){
                Map<Integer, InventoryData> pages = new HashMap<>();
                for(String index : cfg.getConfigurationSection("chests." + name + ".pages").getKeys(false)){
                    if(!index.equals("default")){
                        String title = cfg.getString("chests." + name + ".pages." + index + ".title");
                        double price = cfg.getDouble("chests." + name + ".pages." + index + ".price", 0);
                        pages.put(Integer.valueOf(index), new WInventoryData(title, price));
                    }else{
                        chestData.setDefaultPagesAmount(cfg.getInt("chests." + name + ".pages.default"));
                    }
                }
                chestData.setPagesData(pages);
            }

            chestsData.add(chestData);
            chestsAmount++;
        }

        try{
            Field dataMap = ChestsHandler.class.getDeclaredField("chestsData");
            dataMap.setAccessible(true);
            dataMap.set(plugin.getChestsManager(), chestsData);
        } catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
        }

        WildChestsPlugin.log(" - Found " + chestsAmount + " chests in config.yml.");
        WildChestsPlugin.log("Loading configuration done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public static void reload(){
        try{
            WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
            Field field = WildChestsPlugin.class.getDeclaredField("settingsHandler");
            field.setAccessible(true);
            field.set(plugin, new SettingsHandler(plugin));
            field.setAccessible(false);
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

}
