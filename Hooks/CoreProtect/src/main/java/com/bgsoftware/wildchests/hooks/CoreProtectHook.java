package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.utils.Executor;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

@SuppressWarnings({"deprecation", "unused"})
public final class CoreProtectHook {

    private static WildChestsPlugin plugin;

    private static Plugin coreProtect;
    private static boolean warningDisplayed = false;

    public static void register(WildChestsPlugin plugin) {
        CoreProtectHook.plugin = plugin;
        coreProtect = Bukkit.getPluginManager().getPlugin("CoreProtect");
        plugin.getProviders().registerChestBreakListener(CoreProtectHook::recordBlockBreak);
    }

    public static void recordBlockBreak(@Nullable OfflinePlayer offlinePlayer, Chest chest) {
        if(offlinePlayer == null) // We don't want entities to record block breaks
            return;

        if (!Bukkit.isPrimaryThread()) {
            Executor.sync(() -> recordBlockBreak(offlinePlayer, chest));
            return;
        }

        CoreProtectAPI coreProtectAPI = ((CoreProtect) coreProtect).getAPI();

        Location location = chest.getLocation();

        if (coreProtectAPI.APIVersion() == 5) {
            coreProtectAPI.logRemoval(offlinePlayer.getName(), location, Material.CHEST, (byte) 0);
        } else if (coreProtectAPI.APIVersion() <= 10) {
            coreProtectAPI.logRemoval(offlinePlayer.getName(), location, Material.CHEST,
                    Material.CHEST.createBlockData());
        } else if (!warningDisplayed) {
            warningDisplayed = true;
            WildChestsPlugin.log("&cDetected an API version of CoreProtect that is not supported: " + coreProtectAPI.APIVersion());
            WildChestsPlugin.log("&cOpen an issue on github regarding this!");
        }
    }

}
