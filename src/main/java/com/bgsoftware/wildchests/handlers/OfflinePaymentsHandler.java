package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Executor;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class OfflinePaymentsHandler {

    private final WildChestsPlugin plugin;
    private final Map<UUID, Map<String, Double>> awaitingItems = new ConcurrentHashMap<>();

    public OfflinePaymentsHandler(WildChestsPlugin plugin){
        this.plugin = plugin;

        File file = new File(plugin.getDataFolder(), "offline_payments");

        if(file.exists()){
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            for(String uuidKey : cfg.getConfigurationSection("").getKeys(false)) {
                Map<String, Double> paymentMap = new HashMap<>();
                UUID uuid = UUID.fromString(uuidKey);

                for(String payment : cfg.getString(uuidKey).split(";")){
                    String[] paymentSections = payment.split("=");
                    paymentMap.put(paymentSections[0], Double.parseDouble(paymentSections[1]));
                }

                this.awaitingItems.put(uuid, paymentMap);
            }
        }

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> saveItems(false), 6000, 6000);
    }

    public void tryDepositItems(Player player, Consumer<Double> moneyEarned){
        if(!awaitingItems.containsKey(player.getUniqueId())) {
            moneyEarned.accept(0D);
            return;
        }

        if(!plugin.getProviders().isVaultEnabled()){
            moneyEarned.accept(0D);
            return;
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        Map<String, Double> paymentMap = this.awaitingItems.remove(player.getUniqueId());

        for (Map.Entry<String, Double> entry : paymentMap.entrySet()) {
            try {
                ItemStack itemStack = plugin.getNMSAdapter().deserialzeItem(entry.getKey());
                totalPrice = totalPrice.add(BigDecimal.valueOf(plugin.getProviders().getPrice(player, itemStack, entry.getValue())));
            } catch (Exception ex) {
                ex.printStackTrace();
                this.awaitingItems.computeIfAbsent(player.getUniqueId(), s -> new HashMap<>()).put(entry.getKey(), entry.getValue());
            }
        }

        final BigDecimal TOTAL_PRICE = totalPrice;

        Executor.sync(() -> {
            boolean removeFromMap = true;

            if(plugin.getSettings().sellCommand.isEmpty()) {
                if(!plugin.getProviders().depositPlayer(player, TOTAL_PRICE.doubleValue())){
                    WildChestsPlugin.log("&cCouldn't deposit offline payment for " + player.getName() + "...");
                    removeFromMap = false;
                }
            }
            else{
                Executor.sync(() ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                                .replace("{player-name}", player.getName())
                                .replace("{price}", String.valueOf(TOTAL_PRICE))));
            }

            if(removeFromMap) {
                this.awaitingItems.remove(player.getUniqueId());
                moneyEarned.accept(TOTAL_PRICE.doubleValue());
            }

            else{
                moneyEarned.accept(0D);
            }
        });
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public void addItem(UUID uuid, ItemStack itemStack, double multiplier){
        String itemSerialized = plugin.getNMSAdapter().serialize(itemStack.clone());
        Map<String, Double> paymentMap = awaitingItems.computeIfAbsent(uuid, m -> new HashMap<>());
        synchronized (paymentMap){
            paymentMap.put(itemSerialized, paymentMap.getOrDefault(itemSerialized, 0D) + (itemStack.getAmount() * multiplier));
        }
    }

    public void loadItems(UUID uuid, String payment){
        Map<String, Double> paymentMap = new HashMap<>();

        for(String _payment : payment.split(";")){
            String[] paymentSections = _payment.split("=");
            paymentMap.put(paymentSections[0], Double.parseDouble(paymentSections[1]));
        }

        this.awaitingItems.put(uuid, paymentMap);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveItems(boolean async){
        if(async && Bukkit.isPrimaryThread()){
            Executor.async(() -> saveItems(false));
            return;
        }

        File file = new File(plugin.getDataFolder(), "offline_payments");
        if(file.exists())
            file.delete();

        if(!awaitingItems.isEmpty()) {
            YamlConfiguration cfg = new YamlConfiguration();

            for(Map.Entry<UUID, Map<String, Double>> entry : awaitingItems.entrySet()){
                StringBuilder payment = new StringBuilder();

                for(Map.Entry<String, Double> itemEntry : entry.getValue().entrySet()){
                    payment.append(";").append(itemEntry.getKey()).append("=").append(itemEntry.getValue());
                }

                cfg.set(entry.getKey() + "", payment.substring(1));
            }

            try{
                file.createNewFile();
                cfg.save(file);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

}
