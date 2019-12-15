package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.SQLHelper;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.Pair;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OfflinePaymentsHandler {

    private final WildChestsPlugin plugin;
    private final Map<UUID, Set<Pair<ItemStack, Double>>> awaitingItems = new ConcurrentHashMap<>();

    public OfflinePaymentsHandler(WildChestsPlugin plugin){
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> saveItems(false), 6000, 6000);
    }

    public double tryDepositItems(Player player){
        if(!awaitingItems.containsKey(player.getUniqueId()))
            return 0;

        boolean removeFromMap = true;

        Set<Pair<ItemStack, Double>> awaitingItems = this.awaitingItems.get(player.getUniqueId());

        if(!plugin.getProviders().isVaultEnabled())
            return 0;

        BigDecimal totalPrice = BigDecimal.ZERO;

        for(Pair<ItemStack, Double> pair : awaitingItems) {
            totalPrice = totalPrice.add(BigDecimal.valueOf(plugin.getProviders().getPrice(player, pair.getKey(), pair.getValue())));
        }

        if(plugin.getSettings().sellCommand.isEmpty()) {
            if(!plugin.getProviders().depositPlayer(player, totalPrice.doubleValue())){
                WildChestsPlugin.log("&cCouldn't deposit offline payment for " + player.getName() + "...");
                removeFromMap = false;
            }
        }
        else{
            final BigDecimal TOTAL_PRICE = totalPrice;
            Executor.sync(() ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                            .replace("{player-name}", player.getName())
                            .replace("{price}", String.valueOf(TOTAL_PRICE))));
        }

        if(removeFromMap)
            this.awaitingItems.remove(player.getUniqueId());

        return totalPrice.doubleValue();
    }

    public void addItem(UUID uuid, ItemStack itemStack, double multiplier){
        awaitingItems.computeIfAbsent(uuid, set -> new HashSet<>()).add(new Pair<>(itemStack, multiplier));
    }

    public void loadItems(UUID uuid, String payment){
        Set<Pair<ItemStack, Double>> awaitingItems = Sets.newConcurrentHashSet();

        for(String pair : payment.split(";")) {
            String[] pairSections = pair.split("=");
            awaitingItems.add(new Pair<>(plugin.getNMSAdapter().deserialzeItem(pairSections[0]), Double.valueOf(pairSections[1])));
        }

        this.awaitingItems.put(uuid, awaitingItems);
    }

    public void saveItems(boolean async){
        if(async && Bukkit.isPrimaryThread()){
            Executor.async(() -> saveItems(false));
            return;
        }

        SQLHelper.executeUpdate("DELETE FROM offline_payment;");

        for(Map.Entry<UUID, Set<Pair<ItemStack, Double>>> entry : awaitingItems.entrySet()){
            StringBuilder payment = new StringBuilder();

            for(Pair<ItemStack, Double> pair : entry.getValue()){
                payment.append(";").append(plugin.getNMSAdapter().serialize(pair.getKey())).append("=").append(pair.getValue());
            }

            Query.OFFLINE_PAYMENT_INSERT.getStatementHolder()
                    .setString(entry.getKey() + "")
                    .setString(payment.substring(1))
                    .execute(false);
        }
    }

}
