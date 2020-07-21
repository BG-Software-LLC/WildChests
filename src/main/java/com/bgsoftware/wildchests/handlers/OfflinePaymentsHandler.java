package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Executor;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.UUID;

public final class OfflinePaymentsHandler {

    private final WildChestsPlugin plugin;

    public OfflinePaymentsHandler(WildChestsPlugin plugin){
        this.plugin = plugin;

        Executor.sync(() -> {
            File file = new File(plugin.getDataFolder(), "offline_payments");

            if(file.exists()){
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

                for(String uuidKey : cfg.getConfigurationSection("").getKeys(false)) {
                    depositItems(UUID.fromString(uuidKey), cfg.getString(uuidKey));
                }
            }

            file.renameTo(new File(plugin.getDataFolder(), "offline_payments_backup"));

        }, 2L);
    }

    public void depositItems(UUID uuid, String payment){
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        for(String _payment : payment.split(";")){
            String[] paymentSections = _payment.split("=");

            String itemSerialized = paymentSections[0];
            double multiplier = Double.parseDouble(paymentSections[1]);

            if(itemSerialized.contains("$")){
                String[] serializeSections = itemSerialized.split("\\$");
                itemSerialized = serializeSections[0];
                multiplier *= Double.parseDouble(serializeSections[1]);
            }

            ItemStack itemStack = plugin.getNMSAdapter().deserialzeItem(itemSerialized);
            multiplier *= itemStack.getAmount();
            double totalAmount = plugin.getProviders().getPrice(offlinePlayer, itemStack, multiplier);

            if(plugin.getSettings().sellCommand.isEmpty()) {
                if(!plugin.getProviders().depositPlayer(offlinePlayer, totalAmount)){
                    WildChestsPlugin.log("&cCouldn't deposit offline payment for " + offlinePlayer.getName() + "...");
                }
            }
            else{
                Executor.sync(() ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                                .replace("{player-name}", offlinePlayer.getName())
                                .replace("{price}", String.valueOf(totalAmount))));
            }

        }
    }

}
