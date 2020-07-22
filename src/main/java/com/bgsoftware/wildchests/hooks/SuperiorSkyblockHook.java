package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.key.CustomKeyParser;
import com.bgsoftware.superiorskyblock.api.key.Key;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.Location;

public final class SuperiorSkyblockHook {

    public static void register(WildChestsPlugin plugin){
        SuperiorSkyblockAPI.getBlockValues().registerKeyParser(new CustomKeyParser() {

            @Override
            public Key getCustomKey(Location location) {
                Chest chest = plugin.getChestsManager().getChest(location);
                return Key.of(chest == null ? "CHEST" : chest.getData().getName().toUpperCase());
            }

            @Override
            public boolean isCustomKey(Key key) {
                return plugin.getChestsManager().getChestData(key.getGlobalKey()) != null;
            }

        }, Key.of("CHEST"));
    }

}
