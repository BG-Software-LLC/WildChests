package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.common.config.CommentedConfiguration;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.DepositMethod;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.hooks.StackerProviderType;
import com.bgsoftware.wildchests.key.KeySet;
import com.bgsoftware.wildchests.objects.data.WChestData;
import com.bgsoftware.wildchests.objects.data.WInventoryData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class SettingsHandler {

    public final long notifyInterval;
    public final boolean detailedNotifier;
    public final boolean confirmGUI;
    public final String sellCommand;
    public final boolean sellFormat;
    public final String pricesProvider;
    public final StackerProviderType stackerProvider;
    public final int explodeDropChance;
    public final boolean invalidWorldDelete;
    public final boolean wildStackerHook;
    public final int maximumPickupDelay;
    public final int maxStacksOnDrop;

    public SettingsHandler(WildChestsPlugin plugin) {
        WildChestsPlugin.log("Loading configuration started...");
        long startTime = System.currentTimeMillis();
        int chestsAmount = 0;
        File file = new File(plugin.getDataFolder(), "config.yml");

        if (!file.exists())
            plugin.saveResource("config.yml", false);

        CommentedConfiguration cfg = CommentedConfiguration.loadConfiguration(file);

        try {
            cfg.syncWithConfig(file, plugin.getResource("config.yml"), "chests");
        } catch (IOException error) {
            error.printStackTrace();
        }

        notifyInterval = cfg.getLong("notifier-interval", 12000);
        detailedNotifier = cfg.getBoolean("detailed-notifier", true);
        confirmGUI = cfg.getBoolean("confirm-gui", false);
        sellCommand = cfg.getString("sell-command", "");
        sellFormat = cfg.getBoolean("sell-format", false);
        explodeDropChance = cfg.getInt("explode-drop-chance", 100);
        pricesProvider = cfg.getString("prices-provider");
        stackerProvider = StackerProviderType.fromName(cfg.getString("stacker-provider")).orElse(StackerProviderType.AUTO);
        invalidWorldDelete = cfg.getBoolean("database.invalid-world-delete", false);
        wildStackerHook = cfg.getBoolean("hooks.wildstacker", true);
        maximumPickupDelay = cfg.getInt("maximum-pickup-delay", 32767);
        maxStacksOnDrop = cfg.getInt("max-stacks-on-drop", -1);

        Map<String, Double> prices = new HashMap<>();

        if (cfg.contains("prices-list")) {
            for (String line : cfg.getStringList("prices-list")) {
                String[] split = line.split(":");
                try {
                    if (split.length == 2) {
                        prices.put(split[0], Double.valueOf(split[1]));
                    } else if (split.length == 3) {
                        prices.put(split[0] + ":" + split[1], Double.valueOf(split[2]));
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        PricesProvider_Default.prices = prices;

        Map<String, ChestData> chestsData = new HashMap<>();

        for (String name : cfg.getConfigurationSection("chests").getKeys(false)) {
            if (!cfg.isConfigurationSection("chests." + name)) {
                WildChestsPlugin.log("Not a valid section: chests." + name + " - skipping...");
                continue;
            }

            ConfigurationSection chestSection = cfg.getConfigurationSection("chests." + name);

            ChestData chestData;
            try {
                chestData = loadChestFromSection(plugin, name, chestSection);
            } catch (Throwable error) {
                WildChestsPlugin.log("An unexpected error occurred while loading chest " + name + ":");
                error.printStackTrace();
                continue;
            }

            if (chestData == null)
                continue;

            chestsData.put(name.toLowerCase(), chestData);
            chestsAmount++;
        }

        plugin.getChestsManager().loadChestsData(chestsData);

        WildChestsPlugin.log(" - Found " + chestsAmount + " chests in config.yml.");
        WildChestsPlugin.log("Loading configuration done (Took " + (System.currentTimeMillis() - startTime) + "ms)");
    }

    public static void reload() {
        WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
        plugin.setSettings(new SettingsHandler(plugin));
    }

    @Nullable
    private static ChestData loadChestFromSection(WildChestsPlugin plugin, String chestName, ConfigurationSection section) {
        ChestType chestType;

        if (!section.contains("chest-mode")) {
            WildChestsPlugin.log("Couldn't find chest-mode for " + chestName + " - skipping...");
            return null;
        }

        try {
            chestType = ChestType.valueOf(section.getString("chest-mode"));
        } catch (IllegalArgumentException ex) {
            WildChestsPlugin.log("Found an invalid chest-type for " + chestName + " - skipping...");
            return null;
        }

        if (!section.contains("item.name") && !section.contains("item.lore")) {
            WildChestsPlugin.log("Found an invalid item for " + chestName + " - skipping...");
            return null;
        }

        ItemStack itemStack = new ItemStack(Material.CHEST);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (section.contains("item.name")) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("item.name")));
        }

        if (section.contains("item.lore")) {
            List<String> lore = new LinkedList<>();

            for (String line : section.getStringList("item.lore"))
                lore.add(ChatColor.translateAlternateColorCodes('&', line));

            itemMeta.setLore(lore);
        }

        itemStack.setItemMeta(itemMeta);

        ChestData chestData = new WChestData(chestName, plugin.getNMSAdapter().setChestType(itemStack, chestType), chestType);

        if (section.contains("size")) {
            int rows = section.getInt("size");

            if (rows < 1 || rows > 6) {
                WildChestsPlugin.log("Found an invalid rows amount for " + chestName + " - setting default size to 3 rows");
                rows = 3;
            }

            chestData.setDefaultSize(rows * 9);
        }

        if (section.contains("title")) {
            chestData.setDefaultTitle(ChatColor.translateAlternateColorCodes('&', section.getString("title")));
        }

        if (section.getBoolean("sell-mode", false)) {
            chestData.setSellMode(true);
        }

        if (section.contains("deposit-method")) {
            String depositMethod = section.getString("deposit-method").toUpperCase(Locale.ENGLISH);
            try {
                chestData.setDepositMethod(DepositMethod.valueOf(depositMethod));
            } catch (IllegalArgumentException error) {
                WildChestsPlugin.log("Found an invalid deposit-method for " + chestName + " - skipping...");
            }
        }

        if (section.contains("crafter-chest")) {
            chestData.setAutoCrafter(section.getStringList("crafter-chest"));
        }

        if (section.contains("hopper-filter")) {
            chestData.setHopperFilter(section.getBoolean("hopper-filter"));
        }

        if (section.contains("pages")) {
            Map<Integer, InventoryData> pages = new HashMap<>();
            for (String index : section.getConfigurationSection("pages").getKeys(false)) {
                if (!index.equals("default")) {
                    String title = section.getString("pages." + index + ".title");
                    double price = section.getDouble("pages." + index + ".price", 0);
                    pages.put(Integer.valueOf(index), new WInventoryData(title, price));
                } else {
                    chestData.setDefaultPagesAmount(section.getInt("pages.default"));
                }
            }
            chestData.setPagesData(pages);
        }

        if (section.contains("multiplier")) {
            chestData.setMultiplier(section.getDouble("multiplier"));
        }

        if (section.contains("auto-collect")) {
            chestData.setAutoCollect(section.getBoolean("auto-collect"));
        }

        if (section.contains("auto-suction")) {
            chestData.setAutoSuctionRange(section.getInt("auto-suction.range", 1));
            chestData.setAutoSuctionChunk(section.getBoolean("auto-suction.chunk", false));
        }

        if (section.contains("blacklist")) {
            chestData.setBlacklisted(new KeySet(section.getStringList("blacklist")));
        }

        if (section.contains("whitelist")) {
            chestData.setWhitelisted(new KeySet(section.getStringList("whitelist")));
        }

        if (section.contains("max-amount") && chestType == ChestType.STORAGE_UNIT) {
            chestData.setStorageUnitMaxAmount(section.isInt("max-amount") ?
                    BigInteger.valueOf(section.getInt("max-amount")) :
                    new BigInteger(section.getString("max-amount")));
        }

        if (section.contains("particles")) {
            chestData.setParticles(section.getStringList("particles"));
        }

        return chestData;
    }

}
