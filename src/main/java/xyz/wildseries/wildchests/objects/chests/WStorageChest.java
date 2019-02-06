package xyz.wildseries.wildchests.objects.chests;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.api.objects.chests.StorageChest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.objects.WInventory;
import xyz.wildseries.wildchests.objects.WLocation;
import xyz.wildseries.wildchests.utils.Materials;

import java.util.UUID;

public final class WStorageChest extends WChest implements StorageChest {

    private static ItemStack[] defaultInventory;

    static {
        initDefaultInventory();
    }

    private ItemStack itemStack = new ItemStack(Material.AIR);
    private int amount = 0;

    public WStorageChest(UUID placer, WLocation location, ChestData chestData) {
        super(placer, location, chestData);
        Inventory defaultInventory = Bukkit.createInventory(null, InventoryType.HOPPER);
        defaultInventory.setContents(WStorageChest.defaultInventory);
        setPage(0, defaultInventory);
    }

    @Override
    public ItemStack getItemStack() {
        if(amount <= 0) setItemStack(null);
        return itemStack.clone();
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack == null ? new ItemStack(Material.AIR) : itemStack.clone();
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    @Override
    public Inventory getPage(int page) {
        WInventory pageInv = pages.get(0);
        pageInv.setTitle(getData().getTitle( 1).replace("{0}", this.amount + ""));
        return pageInv.getInventory();
    }

    @Override
    public void remove() {
        plugin.getChestsManager().removeChest(this);
    }

    @Override
    public boolean onInteract(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();

        if(event.getRawSlot() >= 5) {
            if(!event.getClick().name().contains("SHIFT"))
                return false;
            else
                cursor = event.getCurrentItem();
        }

        if(event.getRawSlot() < 5 && event.getRawSlot() != 2){
            event.setCancelled(true);
            return false;
        }

        ItemStack chestItem = getItemStack();

        if(cursor != null && cursor.getType() != Material.AIR) {
            if (chestItem.getType() == Material.AIR) {
                setItemStack(cursor);
            }else if (!cursor.isSimilar(chestItem)) {
                event.setCancelled(true);
                return false;
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if(event.getInventory().getItem(2) != null) {
                    setAmount(getAmount() + event.getInventory().getItem(2).getAmount());
                    event.getInventory().setItem(2, new ItemStack(Material.AIR));
                    updateInventory(event.getInventory());
                }
            }, 1L);
        }
        else{
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                int itemAmount = Math.min(64, getAmount());
                setAmount(getAmount() - itemAmount);
                chestItem.setAmount(itemAmount);
                updateInventory(event.getInventory());
                event.getWhoClicked().setItemOnCursor(chestItem);
            }, 1L);
        }

        return true;
    }

    private void updateInventory(Inventory inventory){
        for (HumanEntity viewer : inventory.getViewers()) {
            if (viewer instanceof Player) {
                WChest.viewers.remove(viewer.getUniqueId());
                viewer.closeInventory();
                WChest.viewers.put(viewer.getUniqueId(), this);
                viewer.openInventory(getPage(0));
            }
        }
    }

    private static void initDefaultInventory(){
        Inventory defaultInventory = Bukkit.createInventory(null, InventoryType.HOPPER);
        defaultInventory.setItem(0, Materials.BLACK_STAINED_GLASS_PANE.toItemStack(1));
        defaultInventory.setItem(1, Materials.BLACK_STAINED_GLASS_PANE.toItemStack(1));
        defaultInventory.setItem(3, Materials.BLACK_STAINED_GLASS_PANE.toItemStack(1));
        defaultInventory.setItem(4, Materials.BLACK_STAINED_GLASS_PANE.toItemStack(1));
        WStorageChest.defaultInventory = defaultInventory.getContents();
    }

}