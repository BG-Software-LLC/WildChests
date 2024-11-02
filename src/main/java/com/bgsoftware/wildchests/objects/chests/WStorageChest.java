package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.handlers.ChestsHandler;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.ItemUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WStorageChest extends WChest implements StorageChest {

    private static final int INVENTORY_SIZE = 5;

    private final CraftWildInventory inventory;

    private BigInteger amount = BigInteger.ZERO, maxAmount;
    private final List<WildContainerItem> contents = new ArrayList<>(INVENTORY_SIZE);
    private int maxStackSize = 64;

    private boolean broken = false;
    private Integer takeItemCheckSlot = null;

    public WStorageChest(UUID placer, Location location, ChestData chestData) {
        super(placer, location, chestData);
        maxAmount = chestData.getStorageUnitMaxAmount();
        inventory = plugin.getNMSInventory().createInventory(this, INVENTORY_SIZE,
                chestData.getTitle(1).replace("{0}", amount + ""), 0);

        for (int i = 0; i < INVENTORY_SIZE; ++i)
            contents.add(WildContainerItem.AIR);

        setItemStack(null);
    }

    @Override
    public List<WildContainerItem> getWildContents() {
        return contents;
    }

    @Override
    public Inventory getPage(int page) {
        return inventory;
    }

    @Override
    public Inventory[] getPages() {
        return new Inventory[]{getPage(0)};
    }

    @Override
    public Inventory setPage(int page, int size, String title) {
        throw new UnsupportedOperationException("You cannot change StorageUnit's page.");
    }

    @Override
    public int getPagesAmount() {
        return 1;
    }

    @Override
    public int getPageIndex(Inventory inventory) {
        return 0;
    }

    @Override
    public ItemStack getItemStack() {
        if (amount.compareTo(BigInteger.ZERO) < 1)
            setItemStack(null);

        return contents.get(1).getBukkitItem();
    }

    @Override
    public void setItemStack(@Nullable ItemStack itemStack) {
        WildContainerItem containerItem = itemStack == null ? WildContainerItem.AIR : plugin.getNMSInventory().createItemStack(itemStack);
        contents.set(1, containerItem);
        maxStackSize = itemStack == null ? 64 : itemStack.getMaxStackSize();
        plugin.getNMSInventory().createDesignItem(inventory, containerItem.getBukkitItem());
    }

    @Override
    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public BigInteger getExactAmount() {
        return getAmount();
    }

    @Override
    public void setAmount(BigInteger amount) {
        this.amount = amount.max(BigInteger.ZERO);
        if (amount.compareTo(BigInteger.ZERO) == 0) {
            setItemStack(null);
        } else {
            // We must clone the item, otherwise a dupe will occur
            WildContainerItem newItemStack = contents.get(1).copy();
            contents.set(1, newItemStack);
            ItemStack storageItem = newItemStack.getBukkitItem();
            storageItem.setAmount(Math.min(maxStackSize, amount.intValue()));
        }

        inventory.setTitle(getData().getTitle(1).replace("{0}", amount + ""));
    }

    @Override
    public void setAmount(int amount) {
        setAmount(BigInteger.valueOf(amount));
    }

    @Override
    public BigInteger getMaxAmount() {
        return maxAmount;
    }

    @Override
    public void setMaxAmount(BigInteger maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public int[] getSlotsForFace() {
        /* I am using IWorldInventory so I can force hoppers to check specific slots.
           The slots that I want the hoppers to check are -1, -2, and 1.
                Slot -1: Will always return air, which is used when hoppers moving items into the chest.
                (so they add the item to the chest instead of changing the item's count)
                Slot -2: Will return the chest's item, but with a count of one.
                (so hoppers that pulling items will set the item in the chest to air instead of changing count)
                Slot 1: Will return the chest's item, but with the correct amount.
                (Default getItem method, but using slot 1 so it will count as a valid slot in canTakeItemThroughFace)
        */
        return new int[]{-1, -2, 1};
    }

    @Override
    public boolean canPlaceItemThroughFace(ItemStack itemStack) {
        ItemStack storageItem = contents.get(1).getBukkitItem();
        BigInteger maxAmount = getMaxAmount();
        return (storageItem.getType() == Material.AIR || itemStack.isSimilar(storageItem)) &&
                (maxAmount.compareTo(BigInteger.ZERO) <= 0 || maxAmount.compareTo(amount.add(BigInteger.valueOf(itemStack.getAmount()))) >= 0);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack) {
        takeItemCheckSlot = slot;
        return slot < 0 || slot == 1;
    }

    @Override
    public WildContainerItem getWildItem(int i) {
        return contents.get(i == -1 || broken ? 0 : i == -2 ? 1 : i);
    }

    @Override
    public void setItem(int i, @Nullable WildContainerItem containerItem) {
        ItemStack itemStack = containerItem == null ? null : containerItem.getBukkitItem();

        ItemStack storageItem = contents.get(1).getBukkitItem();
        boolean takeItemCheckSlot = this.takeItemCheckSlot != null && this.takeItemCheckSlot == i;
        this.takeItemCheckSlot = null;

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            // If other plugins set the #1 slot to AIR, then they want to subtract the amount that they received before.
            if (i < 0 || i == 1) {
                if (amount.compareTo(BigInteger.ONE) == 0) {
                    setItemStack(null);
                    setAmount(BigInteger.ZERO);
                } else {
                    BigInteger itemAmount = amount.min(BigInteger.valueOf(maxStackSize));
                    setAmount(amount.subtract(itemAmount));
                }
            }
        } else {
            if (storageItem.getType() == Material.AIR) {
                setItemStack(itemStack.clone());
            } else if (!takeItemCheckSlot && !canPlaceItemThroughFace(itemStack)) {
                ItemUtils.dropItem(getLocation(), itemStack, true);
                return;
            }

            int itemAmount = itemStack.getAmount();
            int originalAmount = amount.min(BigInteger.valueOf(maxStackSize)).intValue();

            /* The slot -2 is used to pull items from the chest with hoppers.
               The slot -1 is used to push items into the chest with hoppers.
               The slot 0 is used to push items into the chest by other plugins.
               The slot 1 is used to pull items from the chest by other plugins.
             */
            if (i == 1 || i == -2 || (i != -1 && i != 2 && i != 0 && itemAmount < originalAmount && amount.intValue() < maxStackSize)) {
                setAmount(amount.subtract(BigInteger.valueOf(originalAmount - itemAmount)));
            } else {
                setAmount(amount.add(BigInteger.valueOf(itemAmount)));
            }
        }

        update();
    }

    public WildContainerItem splitItem(int amount) {
        WildContainerItem itemStack = contents.get(1).copy();
        itemStack.getBukkitItem().setAmount(this.amount.min(BigInteger.valueOf(amount)).intValue());
        setAmount(this.amount.subtract(BigInteger.valueOf(amount)));
        return itemStack;
    }

    @Override
    public void update() {
        Scheduler.runTask(() -> updateInventory(inventory), 1L);
    }

    @Override
    public Map<Integer, ItemStack> addItems(ItemStack... itemStacks) {
        Map<Integer, ItemStack> additionalItems = new HashMap<>();

        BigInteger amountToAdd = null;

        for (int i = 0; i < itemStacks.length; i++) {
            if (itemStacks[i] == null)
                continue;

            if (!canPlaceItemThroughFace(itemStacks[i])) {
                additionalItems.put(i, itemStacks[i]);
            } else {
                if (getItemStack().getType() == Material.AIR)
                    setItemStack(itemStacks[i]);

                amountToAdd = amountToAdd == null ? BigInteger.valueOf(itemStacks[i].getAmount()) :
                        amountToAdd.add(BigInteger.valueOf(itemStacks[i].getAmount()));
            }
        }

        if (amountToAdd != null) {
            setAmount(getAmount().add(amountToAdd));
            updateInventory(getPage(0));
        }

        return additionalItems;
    }

    @Override
    public void removeItem(int amountToRemove, ItemStack itemStack) {
        ItemStack storageItem = getItemStack();

        if (storageItem.getType() == Material.AIR || !storageItem.isSimilar(itemStack))
            return; // Storage unit is empty

        setAmount(getAmount().subtract(BigInteger.valueOf(amountToRemove)));

        updateInventory(getPage(0));
    }

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        broken = true;

        Location loc = getLocation();

        ItemStack itemStack = getItemStack();

        BigInteger[] divideAndRemainder = getAmount().divideAndRemainder(BigInteger.valueOf(Integer.MAX_VALUE));
        int amountOfMaximums = divideAndRemainder[0].intValue();
        int remainder = divideAndRemainder[1].intValue();

        for (int i = 0; i < amountOfMaximums; i++) {
            itemStack.setAmount(Integer.MAX_VALUE);
            ItemUtils.dropOrCollect(event.getPlayer(), itemStack, getData().isAutoCollect(), loc, true);
        }

        if (remainder > 0) {
            itemStack.setAmount(remainder);
            ItemUtils.dropOrCollect(event.getPlayer(), itemStack, getData().isAutoCollect(), loc, true);
        }

        return true;
    }

    @Override
    public boolean onInteract(InventoryClickEvent event) {
        Player clickedPlayer = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor() == null ? new ItemStack(Material.AIR) : event.getCursor();
        ItemStack clickedItem = event.getCurrentItem() == null ? new ItemStack(Material.AIR) : event.getCurrentItem();

        if (event.getRawSlot() != 2 && event.getRawSlot() < INVENTORY_SIZE) {
            event.setCancelled(true);
            return false;
        }

        if (clickedItem.getType() != Material.AIR && event.getClick().name().contains("SHIFT") && !canPlaceItemThroughFace(clickedItem)) {
            event.setCancelled(true);
            return false;
        }

        ItemStack storageItem = contents.get(1).getBukkitItem();

        if (event.getRawSlot() == 2) {
            if (cursor.getType() != Material.AIR) {
                if (!canPlaceItemThroughFace(cursor)) {
                    event.setCancelled(true);
                    return false;
                }

                BigInteger currentAmount = getAmount();
                BigInteger newAmount = currentAmount.add(BigInteger.valueOf(cursor.getAmount()));
                BigInteger maxAmount = getMaxAmount();
                int toAdd = cursor.getAmount();

                if (maxAmount.compareTo(BigInteger.ZERO) > 0 && newAmount.compareTo(maxAmount) > 0) {
                    toAdd = maxAmount.subtract(currentAmount).intValue();
                }

                if (toAdd != cursor.getAmount()) {
                    setAmount(maxAmount);
                    ItemStack cursorItem = storageItem.clone();
                    cursorItem.setAmount(toAdd);
                    event.setCancelled(true);
                }
            } else {
                ItemStack itemToAdd = storageItem.clone();

                if (itemToAdd.getType() == Material.AIR)
                    return false;

                int newAmount = getAmount().min(BigInteger.valueOf(maxStackSize)).intValue();
                itemToAdd.setAmount(newAmount);

                if (event.getClick().name().contains("SHIFT")) {
                    Map<Integer, ItemStack> leftOvers = clickedPlayer.getInventory().addItem(itemToAdd);
                    if (!leftOvers.isEmpty()) {
                        ItemStack leftOver = leftOvers.get(0);
                        if (leftOver.getAmount() == newAmount) {
                            return false;
                        } else {
                            newAmount -= leftOver.getAmount();
                        }
                    }
                } else {
                    Scheduler.runTask(clickedPlayer, () -> clickedPlayer.setItemOnCursor(itemToAdd), 1L);
                }

                setAmount(getAmount().subtract(BigInteger.valueOf(newAmount)));

                updateInventory(getPage(0));
            }

        }

        return true;
    }

    @Override
    public void loadFromData(ChestsHandler.UnloadedChest unloadedChest) {
        if(!(unloadedChest instanceof ChestsHandler.UnloadedStorageUnit)) {
            WildChestsPlugin.log("&cCannot load data to chest " + getLocation() + " from " + unloadedChest);
            return;
        }

        ChestsHandler.UnloadedStorageUnit unloadedStorageUnit =
                (ChestsHandler.UnloadedStorageUnit) unloadedChest;

        setItemStack(unloadedStorageUnit.itemStack);
        setAmount(unloadedStorageUnit.amount);
        setMaxAmount(unloadedStorageUnit.maxAmount);
    }

    @Override
    public void loadFromFile(YamlConfiguration cfg) {
        if (cfg.contains("item"))
            setItemStack(cfg.getItemStack("item"));
        if (cfg.contains("amount")) {
            if (cfg.isInt("amount"))
                setAmount(BigInteger.valueOf(cfg.getInt("amount")));
            else if (cfg.isString("amount"))
                setAmount(new BigInteger(cfg.getString("amount")));
        }
        if (cfg.contains("max-amount"))
            setMaxAmount(new BigInteger(cfg.getString("max-amount")));
    }

    @Override
    public StatementHolder setUpdateStatement(StatementHolder statementHolder) {
        return statementHolder.setItemStack(getItemStack()).setString(getAmount().toString()).setLocation(getLocation());
    }

    @Override
    public void executeUpdateStatement(boolean async) {
        setUpdateStatement(Query.STORAGE_UNIT_UPDATE_ITEM.getStatementHolder(this)).execute(async);
    }

    @Override
    public void executeInsertStatement(boolean async) {
        Query.STORAGE_UNIT_INSERT.getStatementHolder(this)
                .setLocation(getLocation())
                .setString(placer.toString())
                .setString(getData().getName())
                .setItemStack(getItemStack())
                .setString(getAmount().toString())
                .setString(getMaxAmount().toString())
                .execute(async);
    }

    @Override
    public void executeDeleteStatement(boolean async) {
        Query.STORAGE_UNIT_DELETE.getStatementHolder(this)
                .setLocation(getLocation())
                .execute(async);
    }

    private void updateInventory(Inventory inventory) {
        if (Scheduler.isRegionScheduler()) {
            inventory.getViewers().forEach(viewer -> {
                if (viewer instanceof Player)
                    Scheduler.runTask(viewer, () -> openPage((Player) viewer, 0));
            });
        } else {
            inventory.getViewers().forEach(viewer -> {
                if (viewer instanceof Player)
                    openPage((Player) viewer, 0);
            });
        }
    }

}
