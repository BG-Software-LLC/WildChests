package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.utils.ItemUtils;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WStorageChest extends WChest implements StorageChest {

    private final CraftWildInventory inventory;

    private BigInteger amount = BigInteger.ZERO, maxAmount;
    private final WildItemStack<?, ?>[] contents = new WildItemStack[5];

    private boolean broken = false, lastItemAddedFromInventory = false;

    public WStorageChest(UUID placer, Location location, ChestData chestData) {
        super(placer, location, chestData);
        maxAmount = chestData.getStorageUnitMaxAmount();
        inventory = plugin.getNMSInventory().createInventory(this, 5, chestData.getTitle(1), 0);
        Arrays.fill(contents, WildItemStack.AIR);
        setItemStack(null);
    }

    @Override
    public WildItemStack<?, ?>[] getWildContents() {
        return contents;
    }

    @Override
    public Inventory getPage(int page) {
        inventory.setTitle(getData().getTitle(1).replace("{0}", amount + ""));
        return inventory;
    }

    @Override
    public Inventory[] getPages() {
        return new Inventory[] { getPage(0) };
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
        if(amount.compareTo(BigInteger.ZERO) < 1)
            setItemStack(null);

        return contents[2].getCraftItemStack();
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        contents[2] = itemStack == null ? WildItemStack.AIR : WildItemStack.of(itemStack);
        plugin.getNMSInventory().createDesignItem(inventory, contents[2].getCraftItemStack());
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
        if(amount.compareTo(BigInteger.ZERO) == 0) {
            setItemStack(null);
        }
        else{
            // We must clone the item, otherwise a dupe will occur
            contents[2] = contents[2].cloneItemStack();
            ItemStack storageItem = contents[2].getCraftItemStack();
            storageItem.setAmount(Math.min(storageItem.getMaxStackSize(), amount.intValue()));
        }
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
           The slots that I want the hoppers to check are -1, -2, and 2.
                Slot -1: Will always return air, which is used when hoppers moving items into the chest.
                (so they add the item to the chest instead of changing the item's count)
                Slot -2: Will return the chest's item, but with a count of one.
                (so hoppers that pulling items will set the item in the chest to air instead of changing count)
                Slot 2: Will return the chest's item, but with the correct amount.
                (Default getItem method, but using slot 2 so it will count as a valid slot in canTakeItemThroughFace)
        */
        return new int[] { -1, -2, 2 };
    }

    @Override
    public boolean canPlaceItemThroughFace(ItemStack itemStack) {
        ItemStack storageItem = contents[2].getCraftItemStack();
        return storageItem.getType() == Material.AIR || itemStack.isSimilar(storageItem);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack) {
        return slot < 0 || slot == 2;
    }

    @Override
    public WildItemStack<?, ?> getWildItem(int i) {
        return i == -1 || broken ? contents[0] : i == -2 ? contents[2] : contents[i];
    }

    @Override
    public void setItem(int i, WildItemStack<?, ?> itemStack) {
        ItemStack storageItem = contents[2].getCraftItemStack();

        if(itemStack == null || itemStack.getCraftItemStack().getType() == Material.AIR){
            if(amount.compareTo(BigInteger.ONE) == 0){
                setItemStack(null);
                setAmount(BigInteger.ZERO);
            }
            else {
                setAmount(amount.subtract(BigInteger.valueOf(storageItem.getMaxStackSize())));
            }
        }
        else{
            if(storageItem.getType() == Material.AIR) {
                setItemStack(itemStack.getCraftItemStack().clone());
            }

            else if(!canPlaceItemThroughFace(itemStack.getCraftItemStack())){
                ItemUtils.dropItem(getLocation(), itemStack.getCraftItemStack());
                return;
            }

            int itemAmount = itemStack.getCraftItemStack().getAmount();
            int originalAmount = amount.min(BigInteger.valueOf(storageItem.getMaxStackSize())).intValue();

            // The slot -1 is used for hoppers to push items into the storage units.
            // Therefore, if the slot is -1 we must add the item amount
            if(!lastItemAddedFromInventory && (i == 2 || (i != -1 && itemAmount < originalAmount))){
                setAmount(amount.subtract(BigInteger.valueOf(originalAmount - itemAmount)));
            }
            else{
                setAmount(amount.add(BigInteger.valueOf(itemAmount)));
            }
        }

        lastItemAddedFromInventory = false;

        update();
    }

    public WildItemStack<?, ?> splitItem(int amount){
        WildItemStack<?, ?> itemStack = WildItemStack.of(contents[2].getCraftItemStack());
        itemStack.getCraftItemStack().setAmount(this.amount.min(BigInteger.valueOf(amount)).intValue());
        setAmount(this.amount.subtract(BigInteger.valueOf(amount)));
        return itemStack;
    }

    @Override
    public void update() {
        tileEntityContainer.getTransaction().stream()
                .filter(viewer -> viewer instanceof Player)
                .forEach(viewer -> openPage((Player) viewer, 0));
    }

    @Override
    public void remove() {
        super.remove();
        Query.STORAGE_UNIT_DELETE.getStatementHolder()
                .setLocation(getLocation())
                .execute(true);
    }

    @Override
    public Map<Integer, ItemStack> addItems(ItemStack... itemStacks) {
        Map<Integer, ItemStack> additionalItems = new HashMap<>();

        ItemStack storageItem = getItemStack();

        for(int i = 0; i < itemStacks.length; i++){
            ItemStack itemStack = itemStacks[i];

            if(storageItem.getType() == Material.AIR) {
                setItemStack(itemStack);
                storageItem = itemStack.clone();
            }

            if(storageItem.isSimilar(itemStack)) {
                setAmount(getAmount().add(BigInteger.valueOf(itemStack.getAmount())));
            }
            else {
                additionalItems.put(i, itemStack);
            }
        }

        updateInventory(getPage(0));

        return additionalItems;
    }

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        broken = true;

        Location loc = getLocation();

        ItemStack itemStack = getItemStack();

        BigInteger[] divideAndRemainder = getAmount().divideAndRemainder(BigInteger.valueOf(Integer.MAX_VALUE));
        int amountOfMaximums = divideAndRemainder[0].intValue();
        int reminder = divideAndRemainder[1].intValue();

        for(int i = 0; i < amountOfMaximums; i++) {
            itemStack.setAmount(Integer.MAX_VALUE);
            ItemUtils.dropOrCollect(event.getPlayer(), itemStack, getData().isAutoCollect(), loc);
        }

        if(reminder > 0){
            itemStack.setAmount(reminder);
            ItemUtils.dropOrCollect(event.getPlayer(), itemStack, getData().isAutoCollect(), loc);
        }

        Inventory page = getPage(0);
        if(page != null) page.clear();
        WChest.viewers.keySet().removeIf(uuid -> WChest.viewers.get(uuid).equals(this));

        return true;
    }

    @Override
    public boolean onInteract(InventoryClickEvent event) {
        Player clickedPlayer = (Player) event.getWhoClicked();
        ItemStack cursor = event.getCursor() == null ? new ItemStack(Material.AIR) : event.getCursor();
        ItemStack clickedItem = event.getCurrentItem() == null ? new ItemStack(Material.AIR) : event.getCurrentItem();

        if(event.getRawSlot() != 2 && event.getRawSlot() < 5){
            event.setCancelled(true);
            return false;
        }

        if(clickedItem.getType() != Material.AIR && event.getClick().name().contains("SHIFT") && !canPlaceItemThroughFace(clickedItem)){
            event.setCancelled(true);
            return false;
        }

        if(event.getRawSlot() == 2 || (clickedItem.getType() != Material.AIR && event.getClick().name().contains("SHIFT"))) {
            lastItemAddedFromInventory = true;
            Executor.sync(() -> lastItemAddedFromInventory = false, 1L);
        }

        ItemStack storageItem = contents[2].getCraftItemStack();

        if(event.getRawSlot() == 2){
            if(cursor.getType() != Material.AIR){
                if(!canPlaceItemThroughFace(cursor)) {
                    event.setCancelled(true);
                    return false;
                }

                BigInteger currentAmount = getAmount();
                BigInteger newAmount = currentAmount.add(BigInteger.valueOf(cursor.getAmount()));
                BigInteger maxAmount = getMaxAmount();
                int toAdd = cursor.getAmount();

                if(maxAmount.compareTo(BigInteger.ZERO) > 0 && newAmount.compareTo(maxAmount) > 0){
                    toAdd = maxAmount.subtract(currentAmount).intValue();
                }

                if(toAdd != cursor.getAmount()){
                    setAmount(maxAmount);
                    ItemStack cursorItem = storageItem.clone();
                    cursorItem.setAmount(toAdd);
                    clickedPlayer.setItemOnCursor(cursorItem);
                    updateInventory(getPage(0));
                    event.setCancelled(true);
                }
            }

            else{
                ItemStack itemToAdd = storageItem.clone();

                if(itemToAdd.getType() == Material.AIR)
                    return false;

                int newAmount = getAmount().min(BigInteger.valueOf(storageItem.getMaxStackSize())).intValue();
                itemToAdd.setAmount(newAmount);

                if(event.getClick().name().contains("SHIFT")){
                    Map<Integer, ItemStack> leftOvers = clickedPlayer.getInventory().addItem(itemToAdd);
                    if(!leftOvers.isEmpty()){
                        ItemStack leftOver = leftOvers.get(0);
                        if(leftOver.getAmount() == newAmount) {
                            return false;
                        }
                        else{
                            newAmount -= leftOver.getAmount();
                        }
                    }
                }
                else{
                    clickedPlayer.setItemOnCursor(itemToAdd);
                }

                setAmount(getAmount().subtract(BigInteger.valueOf(newAmount)));

                updateInventory(getPage(0));
            }

        }

        return true;
    }

    @Override
    public void loadFromData(ResultSet resultSet) throws SQLException {
        setItemStack(plugin.getNMSAdapter().deserialzeItem(resultSet.getString("item")));
        setAmount(new BigInteger(resultSet.getString("amount")));
        setMaxAmount(new BigInteger(resultSet.getString("max_amount")));
    }

    @Override
    public void loadFromFile(YamlConfiguration cfg) {
        if(cfg.contains("item"))
            setItemStack(cfg.getItemStack("item"));
        if(cfg.contains("amount")) {
            if(cfg.isInt("amount"))
                setAmount(BigInteger.valueOf(cfg.getInt("amount")));
            else if(cfg.isString("amount"))
                setAmount(new BigInteger(cfg.getString("amount")));
        }
        if(cfg.contains("max-amount"))
            setMaxAmount(new BigInteger(cfg.getString("max-amount")));
    }

    @Override
    public void executeInsertQuery(boolean async) {
        Query.STORAGE_UNIT_INSERT.getStatementHolder()
                .setLocation(location)
                .setString(placer.toString())
                .setString(getData().getName())
                .setItemStack(getItemStack())
                .setString(getAmount().toString())
                .setString(getMaxAmount().toString())
                .execute(async);
    }

    @Override
    public void executeUpdateQuery(boolean async) {
        Query.STORAGE_UNIT_UPDATE.getStatementHolder()
                .setString(placer.toString())
                .setString(getData().getName())
                .setItemStack(getItemStack())
                .setString(getAmount().toString())
                .setString(getMaxAmount().toString())
                .setLocation(location)
                .execute(async);
    }

    @Override
    public StatementHolder getSelectQuery() {
        return Query.STORAGE_UNIT_SELECT.getStatementHolder().setLocation(location);
    }

    private void updateInventory(Inventory inventory){
        inventory.getViewers().stream()
                .filter(viewer -> viewer instanceof Player)
                .forEach(viewer -> openPage((Player) viewer, 0));
    }

}
