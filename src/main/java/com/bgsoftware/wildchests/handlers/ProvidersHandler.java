package com.bgsoftware.wildchests.handlers;
import com.bgsoftware.wildchests.utils.Pair;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.hooks.PricesProvider;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.hooks.PricesProvider_Essentials;
import com.bgsoftware.wildchests.hooks.PricesProvider_ShopGUIPlus;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private boolean isVaultEnabled;
    private Economy economy;

    private final PricesProvider pricesProvider;
    private final Map<UUID, Pair<Long, Double>> pendingTransactions = new HashMap<>();

    public ProvidersHandler(){
        switch (plugin.getSettings().pricesProvider.toUpperCase()){
            case "SHOPGUIPLUS":
                if(Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")) {
                    pricesProvider = new PricesProvider_ShopGUIPlus();
                    break;
                }
            case "ESSENTIALS":
                if(Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
                    pricesProvider = new PricesProvider_Essentials();
                    break;
                }
            default:
                pricesProvider = new PricesProvider_Default();
        }

        if(!initVault()){
            WildChestsPlugin.log("");
            WildChestsPlugin.log("If you want sell-chests to be enabled, please install Vault & Economy plugin.");
            WildChestsPlugin.log("");
        }
    }

    /*
     * Hooks' methods
     */

    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack, double multiplier){
        return pricesProvider.getPrice(offlinePlayer, itemStack) * multiplier;
    }

    /*
     * Handler's methods
     */

    public void enableVault(){
        isVaultEnabled = true;
        economy = Bukkit.getServicesManager().getRegistration(Economy.class).getProvider();
    }

    private boolean initVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null)
            return false;

        enableVault();

        return true;
    }

    public TransactionResult<Double> canSellItem(OfflinePlayer offlinePlayer, ItemStack itemStack, double multiplier){
        double price = itemStack == null ? 0 : getPrice(offlinePlayer, itemStack, multiplier);
        return TransactionResult.of(price, _price -> isVaultEnabled && price > 0);
    }

    public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double money){
        try {
            if (!economy.hasAccount(offlinePlayer))
                economy.createPlayerAccount(offlinePlayer);

            return economy.withdrawPlayer(offlinePlayer, money).transactionSuccess();
        }catch(Throwable ex){
            return false;
        }
    }

    public boolean depositPlayer(OfflinePlayer offlinePlayer, double money){
        try {
            Pair<Long, Double> pendingTransaction = pendingTransactions.computeIfAbsent(offlinePlayer.getUniqueId(), p -> new Pair<>(0L, 0D));
            long currentTime = System.currentTimeMillis();
            if(currentTime - pendingTransaction.key <= 5000){
                pendingTransaction.value += money;
                return true;
            }

            if (!economy.hasAccount(offlinePlayer))
                economy.createPlayerAccount(offlinePlayer);

            money += pendingTransaction.value;

            pendingTransaction.key = currentTime;
            pendingTransaction.value = 0D;

            economy.depositPlayer(offlinePlayer, money);

            return true;
        }catch(Throwable ex){
            return false;
        }
    }

    public boolean isVaultEnabled(){
        return isVaultEnabled;
    }

    public void depositAllPending(){
        for(Map.Entry<UUID, Pair<Long, Double>> entry : pendingTransactions.entrySet()){
            entry.getValue().key = 0L;
            depositPlayer(Bukkit.getOfflinePlayer(entry.getKey()), 0);
        }

        pendingTransactions.clear();
    }

    public static final class TransactionResult<T>{

        private final T data;
        private final Predicate<T> success;

        private TransactionResult(T data, Predicate<T> success){
            this.data = data;
            this.success = success;
        }

        public boolean isSuccess(){
            return success == null || success.test(data);
        }

        public T getData(){
            return data;
        }

        public static <T> TransactionResult<T> of(T data, Predicate<T> success){
            return new TransactionResult<>(data, success);
        }

    }

}
