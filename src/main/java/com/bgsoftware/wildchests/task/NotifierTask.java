package com.bgsoftware.wildchests.task;

import com.bgsoftware.wildchests.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NotifierTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static Map<UUID, Set<TransactionDetails>> amountEarned = new HashMap<>();
    private static Map<UUID, Set<CraftingDetails>> craftings = new HashMap<>();

    private static int taskID = -1;

    private NotifierTask(){
        if(plugin.getSettings().notifyInterval > 0)
            taskID = runTaskTimerAsynchronously(plugin, plugin.getSettings().notifyInterval, plugin.getSettings().notifyInterval).getTaskId();
    }

    public static void start(){
        if(Bukkit.getScheduler().isCurrentlyRunning(taskID) || Bukkit.getScheduler().isQueued(taskID))
            Bukkit.getScheduler().cancelTask(taskID);
        new NotifierTask();
    }

    @Override
    public void run() {
        for(UUID uuid : amountEarned.keySet()){
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if(offlinePlayer.isOnline()){
                Set<TransactionDetails> itemsSold = amountEarned.get(uuid);
                Locale.SOLD_CHEST_HEADER.send(offlinePlayer.getPlayer());
                BigDecimal totalEarned = BigDecimal.ZERO;

                for(TransactionDetails item : itemsSold){
                    Locale.SOLD_CHEST_LINE.send(offlinePlayer.getPlayer(), item.amount, item.itemStack.getType(), StringUtils.format(item.amountEarned));
                    totalEarned  = totalEarned.add(item.amountEarned);
                }

                Locale.SOLD_CHEST_FOOTER.send(offlinePlayer.getPlayer(), StringUtils.format(totalEarned));
            }
        }
        for(UUID uuid : craftings.keySet()){
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if(offlinePlayer.isOnline()){
                Set<CraftingDetails> itemsCrafted = craftings.get(uuid);
                Locale.CRAFTED_ITEMS_HEADER.send(offlinePlayer.getPlayer());
                int totalCrafted = 0;

                for(CraftingDetails item : itemsCrafted){
                    Locale.CRAFTED_ITEMS_LINE.send(offlinePlayer.getPlayer(), item.amount, item.itemStack.getType());
                    totalCrafted += item.amount;
                }

                Locale.CRAFTED_ITEMS_FOOTER.send(offlinePlayer.getPlayer(), totalCrafted);
            }
        }
        amountEarned.clear();
        craftings.clear();
    }

    public static void addTransaction(UUID player, ItemStack itemStack, int amount, double _amountEarned){
        if(!amountEarned.containsKey(player)) {
            amountEarned.put(player, new HashSet<>());
        }

        Set<TransactionDetails> transectionDetails = amountEarned.get(player);
        TransactionDetails details = new TransactionDetails(itemStack, 0, BigDecimal.ZERO);

        for(TransactionDetails _transectionDetails : transectionDetails){
            if(_transectionDetails.itemStack.isSimilar(itemStack))
                details = _transectionDetails;
        }

        details.amount += amount;
        details.amountEarned = details.amountEarned.add(BigDecimal.valueOf(_amountEarned));
        transectionDetails.add(details);
    }

    public static void addCrafting(UUID player, ItemStack itemStack, int amount){
        if(!craftings.containsKey(player)) {
            craftings.put(player, new HashSet<>());
        }

        Set<CraftingDetails> craftingDetails = craftings.get(player);
        CraftingDetails details = new CraftingDetails(itemStack, 0);

        for(CraftingDetails _craftingDetails : craftingDetails){
            if(_craftingDetails.itemStack.isSimilar(itemStack))
                details = _craftingDetails;
        }

        details.amount += amount;
        craftingDetails.add(details);
    }

    private static class TransactionDetails{
        private ItemStack itemStack;
        private int amount;
        private BigDecimal amountEarned;

        TransactionDetails(ItemStack itemStack, int amount, BigDecimal amountEarned){
            this.itemStack = itemStack;
            this.amount = amount;
            this.amountEarned = amountEarned;
        }

    }

    private static class CraftingDetails{

        private ItemStack itemStack;
        private int amount;

        CraftingDetails(ItemStack itemStack, int amount){
            this.itemStack = itemStack;
            this.amount = amount;
        }

    }

}
