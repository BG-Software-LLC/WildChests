package com.bgsoftware.wildchests.api.hooks;

import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

public interface BankProvider {

    /**
     * Deposit money to a player.
     * @param offlinePlayer The player to deposit money to.
     * @param money The amount of money to deposit.
     * @return Whether the transaction was successful or not.
     */
    boolean depositMoney(OfflinePlayer offlinePlayer, BigDecimal money);

    /**
     * Withdraw money from a player.
     * @param offlinePlayer The player to withdraw money from.
     * @param money The amount of money to withdraw.
     * @return Whether the transaction was successful or not.
     */
    boolean withdrawPlayer(OfflinePlayer offlinePlayer, double money);

}
