package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.OptionalInt;

public final class ChestLimitUtils {

    private ChestLimitUtils() {}

    /**
     * Result class to hold both limit existence and value information
     */
    public static class ChestLimitResult {
        private final boolean hasLimit;
        private final int limit;

        private ChestLimitResult(boolean hasLimit, int limit) {
            this.hasLimit = hasLimit;
            this.limit = limit;
        }

        public boolean hasLimit() {
            return hasLimit;
        }

        public int getLimit() {
            return limit;
        }

        public static ChestLimitResult noLimit() {
            return new ChestLimitResult(false, Integer.MAX_VALUE);
        }

        public static ChestLimitResult withLimit(int limit) {
            return new ChestLimitResult(true, limit);
        }
    }

    public static ChestLimitResult getPlayerChestLimitResult(Player player, String chestDataName) {
        String permissionPrefix = "wildchests.limit." + chestDataName + ".";

        OptionalInt maxPermissionLimit = player.getEffectivePermissions().stream()
                .filter(PermissionAttachmentInfo::getValue)
                .map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith(permissionPrefix))
                .map(permission -> permission.substring(permissionPrefix.length()))
                .mapToInt(limitStr -> {
                    try {
                        return Integer.parseInt(limitStr);
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                })
                .filter(limit -> limit >= 0)
                .max();

        if (maxPermissionLimit.isPresent()) {
            return ChestLimitResult.withLimit(maxPermissionLimit.getAsInt());
        }

        Integer defaultLimit = WildChestsPlugin.getPlugin().getSettings().defaultChestLimits.get(chestDataName);
        if (defaultLimit != null && defaultLimit != -1) {
            int actualLimit = defaultLimit == 0 ? Integer.MAX_VALUE : defaultLimit;
            return ChestLimitResult.withLimit(actualLimit);
        }

        return ChestLimitResult.noLimit();
    }
}