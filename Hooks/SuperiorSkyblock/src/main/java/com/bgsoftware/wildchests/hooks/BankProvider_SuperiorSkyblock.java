package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.bank.BankTransaction;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.wildchests.api.hooks.BankProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public final class BankProvider_SuperiorSkyblock implements BankProvider {

    @Override
    public boolean depositMoney(OfflinePlayer offlinePlayer, BigDecimal money) {
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(offlinePlayer.getUniqueId());
        Island island = superiorPlayer.getIsland();

        if (island == null)
            return false;

        BankTransaction bankTransaction = island.getIslandBank().depositAdminMoney(Bukkit.getConsoleSender(), money);
        return bankTransaction.getFailureReason().isEmpty();
    }

    @Override
    public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double money) {
        throw new UnsupportedOperationException("This method is not supported for SuperiorSkyblock bank.");
    }

}
