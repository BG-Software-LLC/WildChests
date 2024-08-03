package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.objects.Materials;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.LinkedChestInteractEvent;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public final class InventoryListener implements Listener {

    private final WildChestsPlugin plugin;

    public static final Map<UUID, InventoryData> buyNewPage = Maps.newHashMap();

    public InventoryListener(WildChestsPlugin plugin){
        this.plugin = plugin;
        initGUIConfirm();
    }

    /**
     * The following two events are here for patching a dupe glitch caused
     * by shift clicking and closing the inventory in the same time.
     */

    private final Map<UUID, ItemStack> latestClickedItem = new HashMap<>();
    private final String[] inventoryTitles = new String[] {"Expand Confirmation", };

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClickMonitor(InventoryClickEvent e){
        if(e.getCurrentItem() != null && e.isCancelled() && Arrays.stream(inventoryTitles).anyMatch(title -> e.getView().getTitle().contains(title))) {
            latestClickedItem.put(e.getWhoClicked().getUniqueId(), e.getCurrentItem());
            Scheduler.runTask(() -> latestClickedItem.remove(e.getWhoClicked().getUniqueId()), 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryCloseMonitor(InventoryCloseEvent e){
        if(latestClickedItem.containsKey(e.getPlayer().getUniqueId())){
            ItemStack clickedItem = latestClickedItem.get(e.getPlayer().getUniqueId());
            Scheduler.runTask(e.getPlayer(), () -> {
                e.getPlayer().getInventory().removeItem(clickedItem);
                ((Player) e.getPlayer()).updateInventory();
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestOpen(PlayerInteractEvent e){
        if(e instanceof LinkedChestInteractEvent || e.getAction() != Action.RIGHT_CLICK_BLOCK ||
                e.getClickedBlock().getType() != Material.CHEST)
            return;

        if(buyNewPage.containsKey(e.getPlayer().getUniqueId())){
            e.setCancelled(true);
            return;
        }

        Chest chest = plugin.getChestsManager().getChest(e.getClickedBlock().getLocation());

        if(chest == null)
            return;

        plugin.getNMSInventory().updateTileEntity(chest);

        chest.onOpen(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestClose(InventoryCloseEvent e){
        Chest chest = WChest.viewers.get(e.getPlayer().getUniqueId());

        //Making sure it's still a valid chest
        if(chest == null) {
            WChest.viewers.remove(e.getPlayer().getUniqueId());
            return;
        }

        chest.onClose(e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestInteract(InventoryClickEvent e){
        Inventory clickedInventory = e.getView().getTopInventory();

        if(!(clickedInventory instanceof CraftWildInventory))
            return;

        Chest chest = ((CraftWildInventory) clickedInventory).getOwner();

        chest.onInteract(e);
    }

    /*
     *  Upgrade Events
     */

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBuyConfirm(AsyncPlayerChatEvent e){
        if(!buyNewPage.containsKey(e.getPlayer().getUniqueId()))
            return;

        e.setCancelled(true);

        handleUpgrade(e.getPlayer(), e.getMessage().equalsIgnoreCase("confirm"));
    }

    @EventHandler
    public void onPlayerBuyConfirm(InventoryClickEvent e){
        if(!buyNewPage.containsKey(e.getWhoClicked().getUniqueId()))
            return;

        e.setCancelled(true);

        handleUpgrade((Player) e.getWhoClicked(), e.getRawSlot() == 4);
    }

    private void handleUpgrade(Player player, boolean confirm){
        try {
            Chest chest = WChest.viewers.get(player.getUniqueId());
            ChestData chestData = chest.getData();
            InventoryData inventoryData = buyNewPage.get(player.getUniqueId());
            int pageIndex = chest.getPagesAmount() - 1;

            if(!chestData.getPagesData().containsKey(pageIndex + 2)){
                Locale.EXPAND_FAILED.send(player);
            }
            else {
                if (confirm) {
                    if (plugin.getProviders().withdrawPlayer(player, inventoryData.getPrice())) {
                        Locale.EXPAND_PURCHASED.send(player);
                        chest.setPage(++pageIndex, chestData.getDefaultSize(), inventoryData.getTitle());
                    } else {
                        Locale.EXPAND_FAILED.send(player);
                    }
                } else {
                    Locale.EXPAND_FAILED.send(player);
                }
            }

            final int PAGE = pageIndex;
            Scheduler.runTask(player, () -> chest.openPage(player, PAGE));
        }catch(Exception ex){
            Locale.EXPAND_FAILED_CHEST_BROKEN.send(player);
        }

        buyNewPage.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerBuyConfirm(InventoryCloseEvent e) {
        if (e.getView().getTitle().equals(WChest.guiConfirmTitle)) {
            Scheduler.runTask(() -> {
                if (buyNewPage.containsKey(e.getPlayer().getUniqueId())) {
                    if(Scheduler.isRegionScheduler()) {
                        Scheduler.runTask(e.getPlayer(), () -> e.getPlayer().openInventory(e.getInventory()));
                    } else {
                        e.getPlayer().openInventory(e.getInventory());
                    }
                }
            }, 1L);
        }
    }

    private void initGUIConfirm(){
        WChest.guiConfirm = Bukkit.createInventory(null, InventoryType.HOPPER, ChatColor.BOLD + "    Expand Confirmation");
        WChest.guiConfirmTitle = ChatColor.BOLD + "    Expand Confirmation";

        ItemStack denyButton = Materials.RED_STAINED_GLASS_PANE.toBukkitItem();
        ItemMeta denyMeta = denyButton.getItemMeta();
        denyMeta.setDisplayName("" + ChatColor.RED + ChatColor.BOLD + "DENY");
        denyButton.setItemMeta(denyMeta);

        WChest.guiConfirm.setItem(0, denyButton);

        ItemStack confirmButton = Materials.GREEN_STAINED_GLASS_PANE.toBukkitItem();
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        confirmMeta.setDisplayName("" + ChatColor.GREEN + ChatColor.BOLD + "CONFIRM");
        confirmButton.setItemMeta(confirmMeta);

        WChest.guiConfirm.setItem(4, confirmButton);

        ItemStack blankButton = Materials.BLACK_STAINED_GLASS_PANE.toBukkitItem();
        ItemMeta blankMeta = blankButton.getItemMeta();
        blankMeta.setDisplayName("" + ChatColor.WHITE);
        blankButton.setItemMeta(blankMeta);

        WChest.guiConfirm.setItem(1, blankButton);
        WChest.guiConfirm.setItem(2, blankButton);
        WChest.guiConfirm.setItem(3, blankButton);
    }

}
