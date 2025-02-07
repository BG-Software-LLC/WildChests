package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.key.CustomKeyParser;
import com.bgsoftware.superiorskyblock.api.key.Key;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class SuperiorSkyblockHook {

    public static void register(WildChestsPlugin plugin) {
        registerCustomKeys(plugin);
        checkMissingInteractables(plugin);
    }

    private static void registerCustomKeys(WildChestsPlugin plugin) {
        SuperiorSkyblockAPI.getBlockValues().registerKeyParser(new CustomKeyParser() {

            @Override
            public Key getCustomKey(Location location) {
                Chest chest = plugin.getChestsManager().getChest(location);
                return Key.of(chest == null ? "CHEST" : chest.getData().getName().toUpperCase());
            }

            public Key getCustomKey(ItemStack itemStack, Key def) {
                ChestData chestData = plugin.getChestsManager().getChestData(itemStack);
                return chestData == null ? def : Key.of(chestData.getName().toUpperCase());
            }

            @Override
            public boolean isCustomKey(Key key) {
                return plugin.getChestsManager().getChestData(key.getGlobalKey()) != null;
            }

        }, Key.of("CHEST"));
    }

    private static void checkMissingInteractables(WildChestsPlugin plugin) {
        Set<String> interactables = new HashSet<>();
        SuperiorSkyblockAPI.getSuperiorSkyblock().getSettings().getInteractables()
                .forEach(block -> interactables.add(block.toUpperCase(Locale.ENGLISH)));

        List<String> missingChests = new LinkedList<>();

        for (ChestData chestData : plugin.getChestsManager().getAllChestData()) {
            String chestName = chestData.getName().toUpperCase(Locale.ENGLISH);
            if (!interactables.contains(chestName)) {
                missingChests.add(chestName);
            }
        }

        if(!missingChests.isEmpty()) {
            WildChestsPlugin.log("&c[WARNING] The following chests are missing from SuperiorSkyblock's interactables list:");
            missingChests.forEach(missingChestName -> WildChestsPlugin.log("&c\t- " + missingChestName));
            WildChestsPlugin.log("&cThis means players are able to open these chests on other islands, even without permissions.");
        }

    }

}
