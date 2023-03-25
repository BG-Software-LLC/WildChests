package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.api.hooks.BankProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;

public final class BankProvider_Vault implements BankProvider {

    private final Economy economy;

    public BankProvider_Vault() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = rsp == null ? null : rsp.getProvider();
    }

    @Override
    public boolean depositMoney(OfflinePlayer offlinePlayer, BigDecimal money) {
        if(economy == null)
            return false;

        if (!economy.hasAccount(offlinePlayer))
            economy.createPlayerAccount(offlinePlayer);

        try {
            return economy.depositPlayer(offlinePlayer, money.doubleValue()).transactionSuccess();
        } catch (Throwable error) {
            return false;
        }
    }

    @Override
    public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double money) {
        if (!economy.hasAccount(offlinePlayer))
            economy.createPlayerAccount(offlinePlayer);

        try {
            return economy.withdrawPlayer(offlinePlayer, money).transactionSuccess();
        } catch (Throwable error) {
            return false;
        }
    }

}
