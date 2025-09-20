package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class ChestLimitUtils {

    private ChestLimitUtils() {}

    public static int getPlayerChestLimit(Player player, String chestType) {
        String permissionPrefix = "wildchests.limit." + chestType + ".";
        
        int maxLimit = 0;
        boolean hasPermission = false;
        
        for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions()) {
            String permission = permissionInfo.getPermission();
            if (permission.startsWith(permissionPrefix) && permissionInfo.getValue()) {
                hasPermission = true;
                try {
                    String limitStr = permission.substring(permissionPrefix.length());
                    int limit = Integer.parseInt(limitStr);
                    if (limit > maxLimit) {
                        maxLimit = limit;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        if (!hasPermission) {
            Integer defaultLimit = WildChestsPlugin.getPlugin().getSettings().defaultChestLimits.get(chestType);
            if (defaultLimit != null) {
                return defaultLimit == 0 ? Integer.MAX_VALUE : defaultLimit;
            }
            return Integer.MAX_VALUE;
        }
        
        return maxLimit;
    }

    public static boolean hasChestLimit(Player player, String chestType) {
        String permissionPrefix = "wildchests.limit." + chestType + ".";
        
        boolean hasPermission = player.getEffectivePermissions().stream()
                .anyMatch(permissionInfo -> permissionInfo.getPermission().startsWith(permissionPrefix) && permissionInfo.getValue());
        
        if (!hasPermission) {
            Integer defaultLimit = WildChestsPlugin.getPlugin().getSettings().defaultChestLimits.get(chestType);
            return defaultLimit != null && defaultLimit != -1;
        }
        
        return true;
    }
}