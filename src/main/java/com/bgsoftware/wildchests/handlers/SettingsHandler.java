package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.key.KeySet;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.config.CommentedConfiguration;
import com.bgsoftware.wildchests.config.ConfigComments;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.objects.data.WChestData;
import com.bgsoftware.wildchests.objects.data.WInventoryData;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class SettingsHandler {

    public final long notifyInterval;
    public final boolean confirmGUI;
    public final String sellCommand;
    public final String pricesProvider;
    public final int explodeDropChance;
    public final boolean invalidWorldDelete;
    public final boolean wildStackerHook;

    public SettingsHandler(WildChestsPlugin plugin){
        WildChestsPlugin.log("Loading configuration started...");
        long startTime = System.currentTimeMillis();
        int chestsAmount = 0;
        File file = new File(plugin.getDataFolder(), "config.yml");

        if(!file.exists())
            plugin.saveResource("config.yml", false);

        CommentedConfiguration cfg = new CommentedConfiguration(ConfigComments.class, file);

        cfg.resetYamlFile(plugin, "config.yml");

        notifyInterval = cfg.getLong("notifier-interval", 12000);
        confirmGUI = cfg.getBoolean("confirm-gui", false);
        sellCommand = cfg.getString("sell-command", "");
        explodeDropChance = cfg.getInt("explode-drop-chance", 100);
        pricesProvider = cfg.getString("prices-provider", "ShopGUIPlus");
        invalidWorldDelete = cfg.getBoolean("database.invalid-world-delete", false);
        wildStackerHook = cfg.getBoolean("hooks.wildstacker", true);

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

        PricesProvider_Default.prices = prices;

        Map<String, ChestData> chestsData = new HashMap<>();

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

            ChestData chestData = new WChestData(name, plugin.getNMSAdapter().setChestNBT(itemStack, chestType), chestType);

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

            if(cfg.contains("chests." + name + ".multiplier")){
                chestData.setMultiplier(cfg.getDouble("chests." + name + ".multiplier"));
            }

            if(cfg.contains("chests." + name + ".auto-collect")){
                chestData.setAutoCollect(cfg.getBoolean("chests." + name + ".auto-collect"));
            }

            if(cfg.contains("chests." + name + ".auto-suction")){
                chestData.setAutoSuctionRange(cfg.getInt("chests." + name + ".auto-suction.range", 1));
                chestData.setAutoSuctionChunk(cfg.getBoolean("chests." + name + ".auto-suction.chunk", false));
            }

            if(cfg.contains("chests." + name + ".blacklist")){
                chestData.setBlacklisted(new KeySet(cfg.getStringList("chests." + name + ".blacklist")));
            }

            if(cfg.contains("chests." + name + ".whitelist")){
                chestData.setWhitelisted(new KeySet(cfg.getStringList("chests." + name + ".whitelist")));
            }

            if(cfg.contains("chests." + name + ".max-amount") && chestType == ChestType.STORAGE_UNIT){
                chestData.setStorageUnitMaxAmount(cfg.isInt("chests." + name + ".max-amount") ?
                        BigInteger.valueOf(cfg.getInt("chests." + name + ".max-amount")) :
                        new BigInteger(cfg.getString("chests." + name + ".max-amount")));
            }

            if(cfg.contains("chests." + name + ".particles")){
                chestData.setParticles(cfg.getStringList("chests." + name + ".particles"));
            }

            chestsData.put(name.toLowerCase(), chestData);
            chestsAmount++;
        }

        plugin.getChestsManager().loadChestsData(chestsData);

        WildChestsPlugin.log(" - Found " + chestsAmount + " chests in config.yml.");
        WildChestsPlugin.log("Loading configuration done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public static void reload(){
        WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
        plugin.setSettings(new SettingsHandler(plugin));
    }

}
