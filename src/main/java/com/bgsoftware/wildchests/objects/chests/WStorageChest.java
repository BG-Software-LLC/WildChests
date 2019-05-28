package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.objects.Materials;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.objects.WInventory;
import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.utils.ItemUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WStorageChest extends WChest implements StorageChest {

    private static ItemStack[] defaultInventory;

    static {
        initDefaultInventory();
    }

    private ItemStack itemStack = new ItemStack(Material.AIR);
    private BigInteger amount = BigInteger.ZERO, maxAmount;

    public WStorageChest(UUID placer, WLocation location, ChestData chestData) {
        super(placer, location, chestData);
        Inventory defaultInventory = Bukkit.createInventory(null, InventoryType.HOPPER);
        defaultInventory.setContents(WStorageChest.defaultInventory);
        setPage(0, defaultInventory);
        maxAmount = chestData.getStorageUnitMaxAmount();
    }

    @Override
    public ItemStack getItemStack() {
        if(amount.compareTo(BigInteger.ZERO) < 1) setItemStack(null);
        return itemStack.clone();
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack == null ? new ItemStack(Material.AIR) : itemStack.clone();
        ItemStack designItem = this.itemStack.getType() == Material.AIR ? Materials.BLACK_STAINED_GLASS_PANE.toBukkitItem() : this.itemStack.clone();
        plugin.getNMSAdapter().setDesignItem(designItem);
        Inventory page = getPage(0);
        if(page != null) {
            page.setItem(0, designItem);
            page.setItem(1, designItem);
            page.setItem(3, designItem);
            page.setItem(4, designItem);
        }
    }

    @Override
    @Deprecated
    public int getAmount() {
        return amount.intValue();
    }

    @Override
    public BigInteger getExactAmount() {
        return amount;
    }

    @Override
    public void setAmount(int amount) {
        setAmount(BigInteger.valueOf(amount));
    }

    @Override
    public void setAmount(BigInteger amount) {
        this.amount = amount.max(BigInteger.ZERO);
        if(amount.compareTo(BigInteger.ZERO) == 0) setItemStack(new ItemStack(Material.AIR));
    }

    @Override
    public BigInteger getMaxAmount() {
        return maxAmount;
    }

    @Override
    public Inventory getPage(int page) {
        if(page != 0) return null;
        WInventory pageInv = pages.get(0);
        pageInv.setTitle(getData().getTitle( 1).replace("{0}", this.amount + ""));
        return pageInv.getInventory();
    }

    @Override
    public void remove() {
        plugin.getChestsManager().removeChest(this);
    }

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        Location loc = getLocation();

        ItemStack itemStack = getItemStack();

        BigInteger[] divideAndRemainder = amount.divideAndRemainder(BigInteger.valueOf(Integer.MAX_VALUE));
        int amountOfMaximums = divideAndRemainder[0].intValue();
        int reminder = divideAndRemainder[1].intValue();

        for(int i = 0; i < amountOfMaximums; i++) {
            itemStack.setAmount(Integer.MAX_VALUE);
            ItemUtils.dropItem(loc, itemStack);
        }

        if(reminder > 0){
            itemStack.setAmount(reminder);
            ItemUtils.dropItem(loc, itemStack);
        }

        Inventory page = getPage(0);
        if(page != null) page.clear();
        WChest.viewers.keySet().removeIf(uuid -> WChest.viewers.get(uuid).equals(this));

        return true;
    }

    private Set<UUID> recentlyClicked = new HashSet<>();

    @Override
    public boolean onInteract(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();

        if(event.getRawSlot() >= 5 && !event.getClick().name().contains("SHIFT"))
            return false;

        if(event.getRawSlot() < 5 && event.getRawSlot() != 2){
            event.setCancelled(true);
            return false;
        }

        if(recentlyClicked.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            return false;
        }

        recentlyClicked.add(event.getWhoClicked().getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> recentlyClicked.remove(event.getWhoClicked().getUniqueId()));

        if(event.getClick().name().contains("SHIFT")){
            if(event.getCurrentItem() == null)
                return false;
            cursor = event.getCurrentItem();
            event.setCurrentItem(new ItemStack(Material.AIR));
            event.getInventory().setItem(2, cursor);
            event.setCancelled(true);
        }

        ItemStack chestItem = getItemStack();

        if(cursor != null && cursor.getType() != Material.AIR) {
            if (chestItem.getType() == Material.AIR) {
                setItemStack(cursor);
            }else if (!cursor.isSimilar(chestItem)) {
                event.setCancelled(true);
                return false;
            }

            //Add items into storage unit
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if(event.getInventory().getItem(2) != null) {
                    boolean hasMaxAmount = maxAmount.compareTo(BigInteger.ZERO) >= 0;
                    BigInteger newAmount = amount.add(BigInteger.valueOf(event.getInventory().getItem(2).getAmount()));
                    BigInteger reminder = newAmount.subtract(maxAmount);
                    if(hasMaxAmount)
                        newAmount = newAmount.min(maxAmount);
                    setAmount(newAmount);

                    //Less than 0
                    if(hasMaxAmount && reminder.compareTo(BigInteger.ZERO) > 0) {
                        ItemStack itemStack = event.getInventory().getItem(2).clone();
                        itemStack.setAmount(reminder.intValue());
                        event.getWhoClicked().setItemOnCursor(itemStack);
                    }

                    event.getInventory().setItem(2, new ItemStack(Material.AIR));
                    updateInventory(event.getInventory());
                }
            }, 1L);
        }
        else{
            if(event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR)
                return false;

            //Take items out of storage unit
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                //int itemAmount = Math.min(getItemStack().getMaxStackSize(), getAmount());
                BigInteger itemAmount = amount.min(BigInteger.valueOf(getItemStack().getMaxStackSize()));
                setAmount(amount.subtract(itemAmount));
                chestItem.setAmount(itemAmount.intValue());
                updateInventory(event.getInventory());
                event.getWhoClicked().setItemOnCursor(chestItem);
            }, 1L);
        }

        return true;
    }

    @Override
    public boolean onHopperMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem().clone();
        ItemStack itemStack = getItemStack();

        Inventory page = getPage(0);
        if(page == null)
            return false;

        if(itemStack.getType() == Material.AIR){
            event.getSource().removeItem(item);
            setItemStack(item);
            setAmount(item.getAmount());
            updateInventory(page);
            return true;
        }

        else if(event.getItem().isSimilar(itemStack)){
            event.getSource().removeItem(event.getItem());
            setAmount(amount.add(BigInteger.valueOf(event.getItem().getAmount())));
            updateInventory(page);
            return true;
        }

        return false;
    }

    @Override
    public boolean onHopperItemTake(Inventory hopperInventory) {
        ItemStack itemStack = getItemStack();
        Inventory page = getPage(0);

        if(page == null)
            return false;

        int hopperAmount = plugin.getNMSAdapter().getHopperAmount(location.getWorld());

        int amount = Math.min(ItemUtils.getSpaceLeft(hopperInventory, itemStack), hopperAmount);

        if(amount == 0)
            return false;

        int itemAmount = this.amount.min(BigInteger.valueOf(itemStack.getMaxStackSize())).intValue();
        amount = Math.min(amount, itemAmount);

        itemStack.setAmount(amount);

        HashMap<Integer, ItemStack> additionalItems = hopperInventory.addItem(itemStack);

        if(additionalItems.isEmpty()) {
            setAmount(this.amount.subtract(BigInteger.valueOf(amount)));
            updateInventory(page);
        }

        return true;
    }

    @Override
    public void saveIntoFile(YamlConfiguration cfg) {
        cfg.set("placer", placer.toString());
        cfg.set("data", getData().getName());
        cfg.set("item", itemStack);
        cfg.set("amount", amount.toString());
        if(maxAmount.compareTo(BigInteger.ZERO) >= 0)
            cfg.set("max-amount", maxAmount.toString());
    }

    @Override
    public void loadFromFile(YamlConfiguration cfg) {
        if(cfg.contains("item"))
            setItemStack(cfg.getItemStack("item"));
        if(cfg.contains("amount")) {
            if(cfg.isInt("amount"))
                setAmount(cfg.getInt("amount"));
            else if(cfg.isString("amount"))
                setAmount(new BigInteger(cfg.getString("amount")));
        }
        if(cfg.contains("max-amount"))
            maxAmount = new BigInteger(cfg.getString("max-amount"));
    }

    private void updateInventory(Inventory inventory){
        inventory.getViewers().stream()
                .filter(viewer -> viewer instanceof Player)
                .forEach(viewer -> plugin.getNMSAdapter().refreshHopperInventory((Player) viewer, getPage(0)));
    }

    private static void initDefaultInventory(){
        Inventory defaultInventory = Bukkit.createInventory(null, InventoryType.HOPPER);
        defaultInventory.setItem(0, Materials.BLACK_STAINED_GLASS_PANE.toBukkitItem());
        defaultInventory.setItem(1, Materials.BLACK_STAINED_GLASS_PANE.toBukkitItem());
        defaultInventory.setItem(3, Materials.BLACK_STAINED_GLASS_PANE.toBukkitItem());
        defaultInventory.setItem(4, Materials.BLACK_STAINED_GLASS_PANE.toBukkitItem());
        WStorageChest.defaultInventory = defaultInventory.getContents();
    }

}
