package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.common.shopsbridge.ShopsProvider;
import com.bgsoftware.common.shopsbridge.Transaction;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.handlers.ProvidersManager;
import com.bgsoftware.wildchests.api.hooks.BankProvider;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import com.bgsoftware.wildchests.api.hooks.StackerProvider;
import com.bgsoftware.wildchests.api.objects.DepositMethod;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.hooks.PricesProvider_ShopsBridgeWrapper;
import com.bgsoftware.wildchests.hooks.StackerProviderType;
import com.bgsoftware.wildchests.hooks.StackerProvider_Default;
import com.bgsoftware.wildchests.hooks.listener.IChestBreakListener;
import com.bgsoftware.wildchests.hooks.listener.IChestPlaceListener;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler implements ProvidersManager {

    private final WildChestsPlugin plugin;

    private final Map<DepositMethod, BankProvider> bankProviderMap = new EnumMap<>(DepositMethod.class);
    private final Map<UUID, PendingTransaction> pendingTransactions = new HashMap<>();

    private PricesProvider pricesProvider = new PricesProvider_Default();
    private StackerProvider stackerProvider = new StackerProvider_Default();

    private final List<IChestPlaceListener> chestPlaceListeners = new LinkedList<>();
    private final List<IChestBreakListener> chestBreakListeners = new LinkedList<>();

    private boolean isShopsBridge = false;
    private long lastBulkTransactionStart = -1;

    public ProvidersHandler(WildChestsPlugin plugin) {
        this.plugin = plugin;

        Scheduler.runTask(() -> {
            registerPricesProvider(plugin);
            registerStackersProvider();
            registerBanksProvider();
            registerGeneralHooks();

            if (bankProviderMap.isEmpty()) {
                WildChestsPlugin.log("");
                WildChestsPlugin.log("If you want sell-chests to be enabled, please install Vault & Economy plugin.");
                WildChestsPlugin.log("");
            }
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

    @Override
    public void setBanksProvider(BankProvider banksProvider) {
        Preconditions.checkNotNull(banksProvider, "bankProvider parameter cannot be null.");
        bankProviderMap.put(DepositMethod.CUSTOM, banksProvider);
    }

    /*
     * Hooks' methods
     */

    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
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

    public TransactionResult<Double> canSellItem(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        Transaction transaction;
        double price;

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            transaction = null;
            price = 0;
        } else if (pricesProvider instanceof PricesProvider_ShopsBridgeWrapper) {
            transaction = ((PricesProvider_ShopsBridgeWrapper) pricesProvider).getTransaction(offlinePlayer, itemStack);
            price = transaction.getPrice().doubleValue();
        } else {
            transaction = null;
            price = pricesProvider.getPrice(offlinePlayer, itemStack);
        }

        return TransactionResult.of(transaction, price, _price -> _price > 0);
    }

    public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double money) {
        BankProvider vaultProvider = bankProviderMap.get(DepositMethod.VAULT);

        if (vaultProvider == null)
            return false;

        return vaultProvider.withdrawPlayer(offlinePlayer, money);
    }

    public void startSellingTask(OfflinePlayer offlinePlayer) {
        if (!pendingTransactions.containsKey(offlinePlayer.getUniqueId()))
            pendingTransactions.put(offlinePlayer.getUniqueId(), new PendingTransaction());
        if (this.lastBulkTransactionStart == -1 && this.pricesProvider instanceof PricesProvider_ShopsBridgeWrapper) {
            ((PricesProvider_ShopsBridgeWrapper) this.pricesProvider).startBulkTransaction();
            this.lastBulkTransactionStart = System.currentTimeMillis();
        }
    }

    public void stopSellingTask(OfflinePlayer offlinePlayer) {
        PendingTransaction pendingTransaction = pendingTransactions.remove(offlinePlayer.getUniqueId());
        if (pendingTransaction != null)
            pendingTransaction.forEach(((depositMethod, value) -> depositPlayer(offlinePlayer, depositMethod, value)));
        if (this.pricesProvider instanceof PricesProvider_ShopsBridgeWrapper) {
            long currentTime = System.currentTimeMillis();
            if (TimeUnit.MILLISECONDS.toSeconds(currentTime - this.lastBulkTransactionStart) > 10) {
                ((PricesProvider_ShopsBridgeWrapper) this.pricesProvider).stopBulkTransaction();
                this.lastBulkTransactionStart = -1;
            }
        }
    }

    public boolean depositPlayer(OfflinePlayer offlinePlayer, DepositMethod depositMethod, double money) {
        BankProvider bankProvider = bankProviderMap.get(depositMethod);

        if (bankProvider == null)
            return false;

        PendingTransaction pendingTransaction = pendingTransactions.get(offlinePlayer.getUniqueId());

        if (pendingTransaction != null) {
            pendingTransaction.depositMoney(depositMethod, money);
            return true;
        }

        return bankProvider.depositMoney(offlinePlayer, BigDecimal.valueOf(money));
    }

    public void depositAllPending() {
        pendingTransactions.forEach((uuid, pendingTransaction) -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            pendingTransaction.forEach((depositMethod, value) -> depositPlayer(offlinePlayer, depositMethod, value));
        });
        pendingTransactions.clear();
    }

    public void registerChestPlaceListener(IChestPlaceListener chestPlaceListener) {
        this.chestPlaceListeners.add(chestPlaceListener);
    }

    public void notifyChestPlaceListeners(Chest chest) {
        this.chestPlaceListeners.forEach(chestPlaceListener -> chestPlaceListener.placeChest(chest));
    }

    public void registerChestBreakListener(IChestBreakListener chestBreakListener) {
        this.chestBreakListeners.add(chestBreakListener);
    }

    public void notifyChestBreakListeners(@Nullable OfflinePlayer offlinePlayer, Chest chest) {
        this.chestBreakListeners.forEach(chestBreakListener -> chestBreakListener.breakChest(offlinePlayer, chest));
    }

    private void registerPricesProvider(WildChestsPlugin plugin) {
        if (!(pricesProvider instanceof PricesProvider_Default))
            return;

        String pricesProviderPluginName = plugin.getSettings().pricesProvider;

        Optional<PricesProvider> pricesProvider = (pricesProviderPluginName.equalsIgnoreCase("AUTO") ?
                ShopsProvider.findAvailableProvider() : ShopsProvider.getShopsProvider(pricesProviderPluginName))
                .flatMap(shopsProvider -> shopsProvider.createInstance(plugin).map(shopsBridge ->
                        new PricesProvider_ShopsBridgeWrapper(shopsProvider, shopsBridge)));

        if (!pricesProvider.isPresent()) {
            WildChestsPlugin.log("- Couldn't find any prices providers, using default one");
            return;
        }

        this.pricesProvider = pricesProvider.get();
    }

    private void registerStackersProvider() {
        if (!(stackerProvider instanceof StackerProvider_Default))
            return;

        Optional<StackerProvider> stackerProvider = Optional.empty();

        StackerProviderType stackerProviderType = plugin.getSettings().stackerProvider;
        boolean autoDetection = stackerProviderType == StackerProviderType.AUTO;

        if ((autoDetection || stackerProviderType == StackerProviderType.WILDSTACKER) &&
                Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
            stackerProvider = createInstance("StackerProvider_WildStacker");
        } else if ((autoDetection || stackerProviderType == StackerProviderType.ROSESTACKER) &&
                Bukkit.getPluginManager().isPluginEnabled("RoseStacker")) {
            stackerProvider = createInstance("StackerProvider_RoseStacker");
        }

        stackerProvider.ifPresent(this::setStackerProvider);
    }

    private void registerBanksProvider() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Optional<BankProvider> bankProvider = createInstance("BankProvider_Vault");
            bankProvider.ifPresent(provider -> bankProviderMap.put(DepositMethod.VAULT, provider));
        }

        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            Optional<BankProvider> bankProvider = createInstance("BankProvider_SuperiorSkyblock");
            bankProvider.ifPresent(provider -> bankProviderMap.put(DepositMethod.SUPERIORSKYBLOCK2, provider));
        }
    }

    private void registerGeneralHooks() {
        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2"))
            registerHook("SuperiorSkyblockHook");

        if (Bukkit.getPluginManager().isPluginEnabled("ChestShop"))
            registerHook("ChestShopHook");

        if (Bukkit.getPluginManager().isPluginEnabled("TransportPipes"))
            registerHook("TransportPipesHook");

        if (Bukkit.getPluginManager().isPluginEnabled("CoreProtect"))
            registerHook("CoreProtectHook");
    }

    private void registerHook(String className) {
        try {
            Class<?> clazz = Class.forName("com.bgsoftware.wildchests.hooks." + className);
            Method registerMethod = clazz.getMethod("register", WildChestsPlugin.class);
            registerMethod.invoke(null, plugin);
        } catch (Exception ignored) {
        }
    }

    private <T> Optional<T> createInstance(String className) {
        try {
            Class<?> clazz = Class.forName("com.bgsoftware.wildchests.hooks." + className);
            try {
                Method compatibleMethod = clazz.getDeclaredMethod("isCompatible");
                if (!(boolean) compatibleMethod.invoke(null))
                    return Optional.empty();
            } catch (Exception ignored) {
            }

            try {
                Constructor<?> constructor = clazz.getConstructor(WildChestsPlugin.class);
                // noinspection unchecked
                return Optional.of((T) constructor.newInstance(plugin));
            } catch (Exception error) {
                // noinspection unchecked
                return Optional.of((T) clazz.newInstance());
            }
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        } catch (Exception error) {
            error.printStackTrace();
            return Optional.empty();
        }
    }

    public static final class TransactionResult<T> {

        @Nullable
        private final Transaction transaction;
        private final T data;
        private final Predicate<T> success;

        private TransactionResult(@Nullable Transaction transaction, T data, Predicate<T> success) {
            this.transaction = transaction;
            this.data = data;
            this.success = success;
        }

        public boolean isSuccess() {
            return success == null || success.test(data);
        }

        @Nullable
        public Transaction getTransaction() {
            return transaction;
        }

        public T getData() {
            return data;
        }

        public static <T> TransactionResult<T> of(@Nullable Transaction transaction, T data, Predicate<T> success) {
            return new TransactionResult<>(transaction, data, success);
        }

    }

    private static final class PendingTransaction {

        private final Map<DepositMethod, MutableDouble> pendingDeposits = new EnumMap<>(DepositMethod.class);

        void depositMoney(DepositMethod depositMethod, double money) {
            pendingDeposits.computeIfAbsent(depositMethod, d -> new MutableDouble()).value += money;
        }

        void forEach(BiConsumer<DepositMethod, Double> consumer) {
            for (Map.Entry<DepositMethod, MutableDouble> entry : pendingDeposits.entrySet())
                consumer.accept(entry.getKey(), entry.getValue().value);
        }

        private static final class MutableDouble {

            private double value = 0;

        }

    }

}
