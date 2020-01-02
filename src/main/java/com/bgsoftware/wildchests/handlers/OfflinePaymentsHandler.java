package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Executor;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class OfflinePaymentsHandler {

    private final WildChestsPlugin plugin;
    private final Map<UUID, StringBuilder> awaitingItems = new ConcurrentHashMap<>();

    public OfflinePaymentsHandler(WildChestsPlugin plugin){
        this.plugin = plugin;

        File file = new File(plugin.getDataFolder(), "offline_payments");

        if(file.exists()){
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            for(String uuidKey : cfg.getConfigurationSection("").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidKey);
                String payment = cfg.getString(uuidKey);
                this.awaitingItems.put(uuid, new StringBuilder(payment));
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

        String paymentString = this.awaitingItems.remove(player.getUniqueId()).toString();

        for(String payment : paymentString.split(";")){
            String[] paymentSections = payment.split("=");
            try {
                totalPrice = totalPrice.add(BigDecimal.valueOf(plugin.getProviders().getPrice(player,
                        plugin.getNMSAdapter().deserialzeItem(paymentSections[0]),
                        Double.parseDouble(paymentSections[1])
                )));
            }catch(Exception ex){
                ex.printStackTrace();
                this.awaitingItems.computeIfAbsent(player.getUniqueId(), s -> new StringBuilder()).append(";").append(payment);
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

    public void addItem(UUID uuid, ItemStack itemStack, double multiplier){
        awaitingItems.computeIfAbsent(uuid, s -> new StringBuilder()).append(plugin.getNMSAdapter().serialize(itemStack)).append("=").append(multiplier);
    }

    public void loadItems(UUID uuid, String payment){
        this.awaitingItems.computeIfAbsent(uuid, s -> new StringBuilder()).append(payment);
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

            for(Map.Entry<UUID, StringBuilder> entry : awaitingItems.entrySet()){
                String payment = entry.getValue().toString();
                cfg.set(entry.getKey() + "", payment.startsWith(";") ? payment.substring(1) : payment);
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
