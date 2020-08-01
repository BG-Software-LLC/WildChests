package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.Query;
import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
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

    private ItemStack itemStack = new ItemStack(Material.AIR);
    private BigInteger amount = BigInteger.ZERO, maxAmount;
    private final WildItemStack<?, ?>[] contents = new WildItemStack[5];

    private boolean broken = false;

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

        return itemStack;
    }

    @Override
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack == null ? new ItemStack(Material.AIR) : itemStack;
        plugin.getNMSInventory().createDesignItem(inventory, this.itemStack);
    }

    @Override
    public BigInteger getAmount() {
        return amount;
    }

    @Override
    public void setAmount(BigInteger amount) {
        this.amount = amount.max(BigInteger.ZERO);
        if(amount.compareTo(BigInteger.ZERO) == 0)
            setItemStack(null);
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
        return this.itemStack.getType() == Material.AIR || itemStack.isSimilar(this.itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack) {
        return slot < 0 || slot == 2;
    }

    @Override
    public WildItemStack<?, ?> getWildItem(int i) {
        if(i == -1 || broken){
            return WildItemStack.AIR.cloneItemStack();
        }
        else {
            ItemStack clonedItem = itemStack.clone();
            clonedItem.setAmount(1);
            return WildItemStack.of(clonedItem);
        }
    }

    @Override
    public void setItem(int i, WildItemStack<?, ?> itemStack) {
        if(itemStack == null || itemStack.getCraftItemStack().getType() == Material.AIR){
            if(amount.compareTo(BigInteger.ONE) == 0){
                setItemStack(null);
                setAmount(BigInteger.ZERO);
            }
            else {
                setAmount(amount.subtract(BigInteger.ONE));
            }
        }
        else{
            if(this.itemStack.getType() == Material.AIR) {
                setItemStack(itemStack.getCraftItemStack().clone());
            }

            else if(!itemStack.getCraftItemStack().isSimilar(this.itemStack)){
                ItemUtils.dropItem(getLocation(), itemStack.getCraftItemStack());
                return;
            }

            setAmount(amount.add(BigInteger.valueOf(itemStack.getCraftItemStack().getAmount())));
        }

        update();
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

        if(clickedItem.getType() != Material.AIR && event.getClick().name().contains("SHIFT") && !clickedItem.isSimilar(this.itemStack)){
            event.setCancelled(true);
            return false;
        }

        if(event.getRawSlot() == 2){
            if(cursor.getType() != Material.AIR){
                if(this.itemStack.getType() != Material.AIR && !cursor.isSimilar(this.itemStack)) {
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
                    ItemStack cursorItem = this.itemStack.clone();
                    cursorItem.setAmount(toAdd);
                    clickedPlayer.setItemOnCursor(cursorItem);
                    updateInventory(getPage(0));
                    event.setCancelled(true);
                }
            }

            else{
                ItemStack newCursor = this.itemStack.clone();

                if(newCursor.getType() == Material.AIR)
                    return false;

                int newAmount = getAmount().min(BigInteger.valueOf(this.itemStack.getMaxStackSize())).intValue();

                newCursor.setAmount(newAmount);

                setAmount(getAmount().subtract(BigInteger.valueOf(newAmount)));

                clickedPlayer.setItemOnCursor(newCursor);
                updateInventory(getPage(0));
            }

        }

        return true;
    }

    //    @Override
//    public boolean onInteract(InventoryClickEvent event) {
//        ItemStack cursor = event.getCursor();
//
//        if(event.getRawSlot() >= 5 && !event.getClick().name().contains("SHIFT"))
//            return false;
//
//        if(event.getRawSlot() < 5 && event.getRawSlot() != 2){
//            event.setCancelled(true);
//            return false;
//        }
//
//        if(recentlyClicked.contains(event.getWhoClicked().getUniqueId())) {
//            event.setCancelled(true);
//            return false;
//        }
//
//        recentlyClicked.add(event.getWhoClicked().getUniqueId());
//        Executor.sync(() -> recentlyClicked.remove(event.getWhoClicked().getUniqueId()));
//
//        if(event.getClick().name().contains("SHIFT")){
//            if(event.getCurrentItem() == null)
//                return false;
//            cursor = event.getCurrentItem();
//            event.setCurrentItem(new ItemStack(Material.AIR));
//            event.getInventory().setItem(2, cursor);
//            event.setCancelled(true);
//        }
//
//        ItemStack chestItem = getItemStack();
//
//        if(cursor != null && cursor.getType() != Material.AIR) {
//            if (chestItem.getType() == Material.AIR) {
//                setItemStack(cursor);
//            }else if (!cursor.isSimilar(chestItem)) {
//                event.setCancelled(true);
//                return false;
//            }
//
//            //Add items into storage unit
//            Executor.sync(() -> {
//                if(event.getInventory().getItem(2) != null) {
//                    boolean hasMaxAmount = getMaxAmount().compareTo(BigInteger.ZERO) >= 0;
//                    BigInteger newAmount = getAmount().add(BigInteger.valueOf(event.getInventory().getItem(2).getAmount()));
//                    BigInteger reminder = newAmount.subtract(getMaxAmount());
//                    if(hasMaxAmount)
//                        newAmount = newAmount.min(getMaxAmount());
//                    setAmount(newAmount);
//
//                    //Less than 0
//                    if(hasMaxAmount && reminder.compareTo(BigInteger.ZERO) > 0) {
//                        ItemStack itemStack = event.getInventory().getItem(2).clone();
//                        itemStack.setAmount(reminder.intValue());
//                        event.getWhoClicked().setItemOnCursor(itemStack);
//                    }
//
//                    event.getInventory().setItem(2, new ItemStack(Material.AIR));
//                    updateInventory(event.getInventory());
//                }
//            }, 1L);
//        }
//        else{
//            if(event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR)
//                return false;
//
//            //Take items out of storage unit
//            Executor.sync(() -> {
//                //int itemAmount = Math.min(getItemStack().getMaxStackSize(), getAmount());
//                BigInteger itemAmount = getAmount().min(BigInteger.valueOf(getItemStack().getMaxStackSize()));
//                setAmount(getAmount().subtract(itemAmount));
//                chestItem.setAmount(itemAmount.intValue());
//                updateInventory(event.getInventory());
//                event.getWhoClicked().setItemOnCursor(chestItem);
//            }, 1L);
//        }
//
//        return true;
//    }

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
