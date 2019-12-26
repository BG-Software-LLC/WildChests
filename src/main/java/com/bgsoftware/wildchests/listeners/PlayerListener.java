package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.Updater;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.math.BigDecimal;

@SuppressWarnings("unused")
public final class PlayerListener implements Listener {

    private WildChestsPlugin plugin;

    public PlayerListener(WildChestsPlugin plugin){
        this.plugin = plugin;
    }

    /*
    Just notifies me if the server is using WildBuster
     */

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if(e.getPlayer().getUniqueId().toString().equals("45713654-41bf-45a1-aa6f-00fe6598703b")){
            Executor.sync(() ->
                e.getPlayer().sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.WHITE + "WildSeries" + ChatColor.DARK_GRAY + "] " +
                    ChatColor.GRAY + "This server is using WildChests v" + plugin.getDescription().getVersion()), 5L);
        }

        if(e.getPlayer().isOp() && Updater.isOutdated()){
            Executor.sync(() ->
                e.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "WildChests" +
                    ChatColor.GRAY + " A new version is available (v" + Updater.getLatestVersion() + ")!"), 20L);
        }

        Executor.async(() -> {
            plugin.getOfflinePayments().tryDepositItems(e.getPlayer(), moneyEarned -> {
                if(moneyEarned > 0){
                    Locale.MONEY_EARNED_OFFLINE.send(e.getPlayer(), StringUtils.format(BigDecimal.valueOf(moneyEarned)));
                }
            });
        }, 20L);

    }

}
