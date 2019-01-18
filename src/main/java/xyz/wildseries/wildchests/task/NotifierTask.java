package xyz.wildseries.wildchests.task;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NotifierTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static Map<UUID, Set<TransactionDetails>> amountEarned = new HashMap<>();

    private static int taskID = -1;

    private NotifierTask(){
        if(plugin.getSettings().notifyInterval > 0)
            taskID = runTaskTimerAsynchronously(plugin, plugin.getSettings().notifyInterval, plugin.getSettings().notifyInterval).getTaskId();
    }

    public static void start(){
        if(Bukkit.getScheduler().isCurrentlyRunning(taskID))
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
                double totalEarned = 0;

                for(TransactionDetails item : itemsSold){
                    Locale.SOLD_CHEST_LINE.send(offlinePlayer.getPlayer(), item.amount, item.itemStack.getType(), item.amountEarned);
                    totalEarned += item.amountEarned;
                }

                Locale.SOLD_CHEST_FOOTER.send(offlinePlayer.getPlayer(), totalEarned);
            }
        }
        amountEarned.clear();
    }

    private static class TransactionDetails{
        private ItemStack itemStack;
        private int amount;
        private double amountEarned;

        TransactionDetails(ItemStack itemStack, int amount, double amountEarned){
            this.itemStack = itemStack;
            this.amount = amount;
            this.amountEarned = amountEarned;
        }

    }

    public static void addTransaction(UUID player, ItemStack itemStack, int amount, double _amountEarned){
        if(!amountEarned.containsKey(player)) {
            amountEarned.put(player, new HashSet<>());
        }

        Set<TransactionDetails> transectionDetails = amountEarned.get(player);
        TransactionDetails details = new TransactionDetails(itemStack, 0, 0);

        for(TransactionDetails _transectionDetails : transectionDetails){
            if(_transectionDetails.itemStack.isSimilar(itemStack))
                details = _transectionDetails;
        }

        details.amount += amount;
        details.amountEarned += _amountEarned;
        transectionDetails.add(details);
    }

}
