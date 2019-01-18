package xyz.wildseries.wildchests.task;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class NotifierTask extends BukkitRunnable {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static Map<UUID, Set<TransectionDetails>> amountEarned = new HashMap<>();

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
                Set<TransectionDetails> itemsSold = amountEarned.get(uuid);
                Locale.SOLD_CHEST_HEADER.send(offlinePlayer.getPlayer());

                for(TransectionDetails item : itemsSold){
                    Locale.SOLD_CHEST_LINE.send(offlinePlayer.getPlayer(), item.amount, item.itemStack.getType(), item.amountEarned);
                }

                Locale.SOLD_CHEST_FOOTER.send(offlinePlayer.getPlayer());
            }
        }
        amountEarned.clear();
    }

    private class TransectionDetails{
        private ItemStack itemStack;
        private int amount;
        private double amountEarned;

        TransectionDetails(ItemStack itemStack, int amount, double amountEarned){
            this.itemStack = itemStack;
            this.amount = amount;
            this.amountEarned = amountEarned;
        }

    }

}
