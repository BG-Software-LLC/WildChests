package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;


public final class DepositMethod {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public void Deposit(Chest chest, OfflinePlayer player, double finalPrice) {
        switch (chest.getData().getDepositMethod()) {
            case Vault:
                plugin.getProviders().depositPlayer(player, finalPrice);
                break;
            case SuperiorSkyblock2:
                SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
                superiorPlayer.getIsland().getIslandBank().depositAdminMoney(Bukkit.getConsoleSender(), BigDecimal.valueOf(finalPrice));
                break;
        }
    }
}
