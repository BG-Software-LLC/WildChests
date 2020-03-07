package com.bgsoftware.wildchests.handlers;
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

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private boolean isVaultEnabled;
    private Economy economy;

    private final PricesProvider pricesProvider;

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

    public CompletableFuture<Double> getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack, double multiplier){
        return pricesProvider.getPrice(offlinePlayer, itemStack).thenApply(new Function<Double, Double>() {
            @Override
            public Double apply(Double d) {
                return d * multiplier;
            }
        });
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

    public CompletableFuture<TransactionResult<Double>> canSellItem(OfflinePlayer offlinePlayer, ItemStack itemStack, double multiplier){
        CompletableFuture<TransactionResult<Double>> completableFuture = new CompletableFuture<>();
        CompletableFuture<Double> price = CompletableFuture.completedFuture(0D);

        if(itemStack != null){
            price = getPrice(offlinePlayer, itemStack, multiplier);
        }

        price.whenComplete((d, e) -> completableFuture.complete(TransactionResult.of(d, _price -> isVaultEnabled && d > 0)));

        return completableFuture;
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
            if (!economy.hasAccount(offlinePlayer))
                economy.createPlayerAccount(offlinePlayer);

            return economy.depositPlayer(offlinePlayer, money).transactionSuccess();
        }catch(Throwable ex){
            return false;
        }
    }

    public boolean isVaultEnabled(){
        return isVaultEnabled;
    }

    public static final class TransactionResult<T>{

        private T data;
        private Predicate<T> success;

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
