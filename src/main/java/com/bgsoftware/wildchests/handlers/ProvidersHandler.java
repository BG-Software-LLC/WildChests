package com.bgsoftware.wildchests.handlers;
import com.bgsoftware.wildchests.api.handlers.ProvidersManager;
import com.bgsoftware.wildchests.api.hooks.StackerProvider;
import com.bgsoftware.wildchests.hooks.ChestShopHook;
import com.bgsoftware.wildchests.hooks.PricesProvider_QuantumShop;
import com.bgsoftware.wildchests.hooks.PricesProvider_ShopGUIPlus;
import com.bgsoftware.wildchests.hooks.PricesProvider_zShop;
import com.bgsoftware.wildchests.hooks.StackerProvider_Default;
import com.bgsoftware.wildchests.hooks.StackerProvider_WildStacker;
import com.bgsoftware.wildchests.hooks.SuperiorSkyblockHook;
import com.bgsoftware.wildchests.utils.Executor;
import net.brcdev.shopgui.player.PlayerData;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.hooks.PricesProvider_Essentials;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler implements ProvidersManager {

    private boolean isVaultEnabled;
    private Economy economy;

    private final Map<UUID, MutableDouble> pendingTransactions = new HashMap<>();
    private PricesProvider pricesProvider = new PricesProvider_Default();
    private StackerProvider stackerProvider = new StackerProvider_Default();

    public ProvidersHandler(WildChestsPlugin plugin){
        Executor.sync(() -> {
            if(pricesProvider instanceof PricesProvider_Default) {
                switch (plugin.getSettings().pricesProvider.toUpperCase()) {
                    case "SHOPGUIPLUS":
                        if (Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")) {
                            try {
                                //noinspection JavaReflectionMemberAccess
                                ShopItem.class.getMethod("getSellPriceForAmount", Shop.class, Player.class, PlayerData.class, int.class);
                                pricesProvider = (PricesProvider) Class.forName("com.bgsoftware.wildchests.hooks.PricesProvider_ShopGUIPlusOld").newInstance();
                            } catch (Throwable ex) {
                                pricesProvider = new PricesProvider_ShopGUIPlus();
                            }
                            break;
                        }
                    case "QUANTUMSHOP":
                        if (Bukkit.getPluginManager().isPluginEnabled("QuantumShop")) {
                            pricesProvider = new PricesProvider_QuantumShop();
                            break;
                        }
                    case "ESSENTIALS":
                        if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
                            pricesProvider = new PricesProvider_Essentials();
                            break;
                        }
                    case "ZSHOP":
                        if (Bukkit.getPluginManager().isPluginEnabled("zShop")) {
                            pricesProvider = new PricesProvider_zShop();
                            break;
                        }
                    default:
                        WildChestsPlugin.log("- Couldn''t find any prices providers, using default one");
                }
            }

            if(stackerProvider instanceof StackerProvider_Default){
                if(Bukkit.getPluginManager().isPluginEnabled("WildStacker"))
                    setStackerProvider(new StackerProvider_WildStacker());
            }

            if(!initVault()){
                WildChestsPlugin.log("");
                WildChestsPlugin.log("If you want sell-chests to be enabled, please install Vault & Economy plugin.");
                WildChestsPlugin.log("");
            }

            if(Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2"))
                SuperiorSkyblockHook.register(plugin);

            if(Bukkit.getPluginManager().isPluginEnabled("ChestShop"))
                ChestShopHook.register(plugin);
        });
    }

    @Override
    public void setPricesProvider(PricesProvider pricesProvider) {
        this.pricesProvider = pricesProvider;
    }

    @Override
    public void setStackerProvider(StackerProvider stackerProvider) {
        this.stackerProvider = stackerProvider;
    }

    /*
     * Hooks' methods
     */

    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack){
        return pricesProvider.getPrice(offlinePlayer, itemStack);
    }

    public int getItemAmount(Item item) {
        return stackerProvider.getItemAmount(item);
    }

    public void setItemAmount(Item item, int amount) {
        stackerProvider.setItemAmount(item, amount);
    }

    public boolean dropItem(Location location, ItemStack itemStack, int amount) {
        return stackerProvider.dropItem(location, itemStack, amount);
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

    public TransactionResult<Double> canSellItem(OfflinePlayer offlinePlayer, ItemStack itemStack){
        double price = itemStack == null ? 0 : getPrice(offlinePlayer, itemStack);
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

    public void startSellingTask(OfflinePlayer offlinePlayer){
        if(!pendingTransactions.containsKey(offlinePlayer.getUniqueId()))
            pendingTransactions.put(offlinePlayer.getUniqueId(), new MutableDouble());
    }

    public void stopSellingTask(OfflinePlayer offlinePlayer){
        MutableDouble sellTaskValue = pendingTransactions.remove(offlinePlayer.getUniqueId());
        if(sellTaskValue != null)
            depositPlayer(offlinePlayer, sellTaskValue.value);
    }

    public boolean depositPlayer(OfflinePlayer offlinePlayer, double money){
        try {
            MutableDouble sellTaskValue = pendingTransactions.get(offlinePlayer.getUniqueId());

            if(sellTaskValue != null){
                sellTaskValue.value += money;
                return true;
            }

            if (!economy.hasAccount(offlinePlayer))
                economy.createPlayerAccount(offlinePlayer);

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
        pendingTransactions.forEach((uuid, sellTaskValue) ->
                depositPlayer(Bukkit.getOfflinePlayer(uuid), sellTaskValue.value));
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

    private static final class MutableDouble{

        private double value = 0;

    }

}
