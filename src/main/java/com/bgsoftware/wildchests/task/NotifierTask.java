package com.bgsoftware.wildchests.task;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.scheduler.ScheduledTask;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NotifierTask {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static final Map<UUID, Set<TransactionDetails>> transactions = new HashMap<>();
    private static final Map<UUID, Set<CraftingDetails>> craftings = new HashMap<>();

    private static ScheduledTask task = null;

    private NotifierTask() {
        if (plugin.getSettings().notifyInterval > 0) {
            task = Scheduler.runRepeatingTaskAsync(this::run, plugin.getSettings().notifyInterval);
        } else {
            task = null;
        }
    }

    public static void start() {
        if (task != null) {
            task.cancel();
        }

        new NotifierTask();
    }

    private void run() {
        synchronized (transactions) {
            transactions.forEach((uuid, transactions) -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline()) {
                    Locale.SOLD_CHEST_HEADER.send(offlinePlayer.getPlayer());
                    BigDecimal totalEarned = BigDecimal.ZERO;

                    for (TransactionDetails transaction : transactions) {
                        if (plugin.getSettings().detailedNotifier) {
                            String soldItemType = StringUtils.format(transaction.getItemStack().getType().name());
                            String soldItemEarnings = plugin.getSettings().sellFormat ?
                                    StringUtils.fancyFormat(transaction.getEarnings()) :
                                    StringUtils.format(transaction.getEarnings());

                            Locale.SOLD_CHEST_LINE.send(offlinePlayer.getPlayer(), transaction.getAmount(),
                                    soldItemType, soldItemEarnings);
                        }
                        totalEarned = totalEarned.add(transaction.getEarnings());
                    }

                    Locale.SOLD_CHEST_FOOTER.send(offlinePlayer.getPlayer(), plugin.getSettings().sellFormat ?
                            StringUtils.fancyFormat(totalEarned) : StringUtils.format(totalEarned));
                }
            });
            transactions.clear();
        }

        synchronized (craftings) {
            craftings.forEach((uuid, transactions) -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (offlinePlayer.isOnline()) {
                    Set<CraftingDetails> itemsCrafted = craftings.get(uuid);
                    Locale.CRAFTED_ITEMS_HEADER.send(offlinePlayer.getPlayer());
                    int totalCrafted = 0;

                    for (CraftingDetails item : itemsCrafted) {
                        if (plugin.getSettings().detailedNotifier) {
                            String craftedItemType = StringUtils.format(item.getItemStack().getType().name());
                            Locale.CRAFTED_ITEMS_LINE.send(offlinePlayer.getPlayer(), item.getAmount(), craftedItemType);
                        }
                        totalCrafted += item.getAmount();
                    }

                    Locale.CRAFTED_ITEMS_FOOTER.send(offlinePlayer.getPlayer(), totalCrafted);
                }
            });
            craftings.clear();
        }
    }

    public static synchronized void addTransaction(UUID player, ItemStack itemStack, int amount, double amountEarned) {
        Set<TransactionDetails> transactionDetails;

        synchronized (transactions) {
            transactionDetails = transactions.computeIfAbsent(player, p -> new HashSet<>());
        }

        for (TransactionDetails transaction : transactionDetails) {
            if (transaction.getItemStack().isSimilar(itemStack)) {
                transaction.increaseAmount(amount);
                transaction.increaseEarnings(BigDecimal.valueOf(amountEarned));
                return;
            }
        }

        transactionDetails.add(new TransactionDetails(itemStack, amount, BigDecimal.valueOf(amountEarned)));
    }

    public static synchronized void addCrafting(UUID player, ItemStack itemStack, int amount) {
        Set<CraftingDetails> craftingDetails;

        synchronized (craftings) {
            craftingDetails = craftings.computeIfAbsent(player, p -> new HashSet<>());
        }

        for (CraftingDetails crafting : craftingDetails) {
            if (crafting.getItemStack().isSimilar(itemStack)) {
                crafting.increaseAmount(amount);
                return;
            }
        }

        craftingDetails.add(new CraftingDetails(itemStack, amount));
    }

}
