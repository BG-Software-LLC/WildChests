package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.database.StatementHolder;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import com.google.common.collect.Maps;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.utils.ItemUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

public abstract class WChest implements Chest {

    protected static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    public static final Map<UUID, Chest> viewers = Maps.newHashMap();
    public static Inventory guiConfirm;
    public static String guiConfirmTitle;

    protected final UUID placer;
    protected final Location location;
    protected final ChestData chestData;

    protected TileEntityContainer tileEntityContainer;

    public WChest(UUID placer, Location location, ChestData chestData) {
        this.placer = placer;
        this.location = location;
        this.chestData = chestData;
    }

    /* CHEST RELATED METHODS */

    @Override
    public UUID getPlacer() {
        return placer;
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public ChestData getData() {
        return chestData;
    }

    @Override
    public ChestType getChestType() {
        return getData().getChestType();
    }

    @Override
    public void remove(){
        plugin.getChestsManager().removeChest(this);
    }

    /* INVENTORIES / PAGES RELATED METHODS */

    @Override
    public ItemStack[] getContents(){
        WildItemStack<?, ?>[] originalContents = getWildContents();
        ItemStack[] contents = new ItemStack[originalContents.length];

        for(int i = 0; i < contents.length; i++)
            contents[i] = originalContents[i].getCraftItemStack();

        return contents;
    }

    public abstract WildItemStack<?, ?>[] getWildContents();

    @Override
    public Map<Integer, ItemStack> addItems(ItemStack... itemStacks) {
        Map<Integer, ItemStack> additionalItems = new HashMap<>();
        Map<Integer, ItemStack> itemAdditionalItems = new HashMap<>();

        for(ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                int currentInventory = 0;

                do {
                    Inventory inventory = getPage(currentInventory);
                    if(inventory != null) {
                        itemAdditionalItems = inventory.addItem(itemStack);
                    }
                    currentInventory++;
                } while (!itemAdditionalItems.isEmpty() && currentInventory < getPagesAmount());

                additionalItems.putAll(itemAdditionalItems);
            }
        }

        return additionalItems;
    }

    @Override
    public Map<Integer, ItemStack> addRawItems(ItemStack... itemStacks) {
        return addItems(itemStacks);
    }

    @Override
    public void removeItem(int amountToRemove, ItemStack itemStack) {
        Inventory[] pages = getPages();

        int itemsRemoved = 0;

        for(int i = 0; i < pages.length && itemsRemoved < amountToRemove; i++){
            Inventory page = pages[i];
            int toRemove = Math.min(amountToRemove - itemsRemoved, ItemUtils.countItems(itemStack, page));
            ItemStack cloned = itemStack.clone();
            cloned.setAmount(toRemove);
            page.removeItem(cloned);
        }
    }

    @Override
    public ItemStack getItem(int i){
        return getWildItem(i).getCraftItemStack();
    }

    public abstract WildItemStack<?, ?> getWildItem(int i);

    @Override
    public void setItem(int i, ItemStack itemStack){
        setItem(i, WildItemStack.of(itemStack));
    }

    public abstract void setItem(int i, WildItemStack<?, ?> itemStack);

    @Override
    public abstract Inventory getPage(int page);

    @Override
    public abstract Inventory[] getPages();

    @Override
    public void setPage(int page, Inventory inventory) {
        setPage(page, new InventoryHolder(null, inventory.getContents()));
    }

    @Override
    public abstract Inventory setPage(int page, int size, String title);

    public void setPage(int page, InventoryHolder inventoryHolder){
        Inventory inventory = setPage(page, inventoryHolder.getSize(), inventoryHolder.getTitle());
        inventory.setContents(inventoryHolder.getContents());
    }

    @Override
    public void openPage(Player player, int page) {
        viewers.put(player.getUniqueId(), this);
        plugin.getNMSInventory().openPage(player, (CraftWildInventory) getPage(page));
    }

    @Override
    public void closePage(Player player) {
        viewers.remove(player.getUniqueId());
    }

    @Override
    public abstract int getPagesAmount();

    @Override
    public abstract int getPageIndex(Inventory inventory);

    /* BLOCK-ACTIONS RELATED METHODS */

    @Override
    public boolean onBreak(BlockBreakEvent event){
        Location loc = getLocation();
        for(int page = 0; page < getPagesAmount(); page++){
            Inventory inventory = getPage(page);
            for(ItemStack itemStack : inventory.getContents())
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    ItemUtils.dropOrCollect(event.getPlayer(), itemStack, getData().isAutoCollect(), loc);
                }
            inventory.clear();
        }

        viewers.keySet().removeIf(uuid -> viewers.get(uuid).equals(this));

        return true;
    }

    @Override
    public boolean onOpen(PlayerInteractEvent event){
        viewers.put(event.getPlayer().getUniqueId(), this);
        tileEntityContainer.openContainer(event.getPlayer());
        return true;
    }

    @Override
    public boolean onClose(InventoryCloseEvent event) {
        //Checking if player is buying new page
        if(InventoryListener.buyNewPage.containsKey(event.getPlayer().getUniqueId()))
            return false;

        viewers.remove(event.getPlayer().getUniqueId());
        tileEntityContainer.closeContainer(event.getPlayer());

        //Checking that it's the last player that views the inventory
        return getViewersAmount(event.getViewers()) <= 1;
    }

    @Override
    public boolean onInteract(InventoryClickEvent event) {
        if(event.getSlotType() != InventoryType.SlotType.OUTSIDE)
            return false;

        ChestData chestData = getData();
        int index = getPageIndex(event.getWhoClicked().getOpenInventory().getTopInventory());

        if(event.getClick() == ClickType.LEFT){
            //Making sure he's not in the first page
            if(index != 0){
                //movingBetweenPages.add(event.getWhoClicked().getUniqueId());
                openPage((Player) event.getWhoClicked(), index - 1);
                //movingBetweenPages.remove(event.getWhoClicked().getUniqueId());
            }
        }

        else if(event.getClick() == ClickType.RIGHT){
            //Making sure it's not the last page
            if(index + 1 < getPagesAmount()){
                //movingBetweenPages.add(event.getWhoClicked().getUniqueId());
                openPage((Player) event.getWhoClicked(), index + 1);
            }

            //Making sure next page is purchasble
            else if(chestData.getPagesData().containsKey(++index + 1)){
                InventoryData inventoryData = chestData.getPagesData().get(index + 1);
                InventoryListener.buyNewPage.put(event.getWhoClicked().getUniqueId(), inventoryData);

                if(plugin.getSettings().confirmGUI){
                    event.getWhoClicked().openInventory(guiConfirm);
                }else {
                    Locale.EXPAND_CHEST.send(event.getWhoClicked(), inventoryData.getPrice());
                    event.getWhoClicked().closeInventory();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onPlace(BlockPlaceEvent event) {
        throw new UnsupportedOperationException("onPlace for chests is not supported anymore.");
    }

    @Override
    public boolean onHopperMove(InventoryMoveItemEvent event) {
        throw new UnsupportedOperationException("onHopperMove for chests is not supported anymore.");
    }

    @Override
    public boolean onHopperMove(ItemStack itemStack, Hopper hopper) {
        throw new UnsupportedOperationException("onHopperMove for chests is not supported anymore.");
    }

    @Override
    public boolean onHopperItemTake(Inventory hopperInventory) {
        throw new UnsupportedOperationException("onHopperItemTake for chests is not supported anymore.");
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack) {
        ChestData chestData = getData();
        return !chestData.isAutoCrafter() || !chestData.isHopperFilter() ||
                chestData.containsRecipe(itemStack);
    }

    @Override
    public boolean canPlaceItemThroughFace(ItemStack itemStack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace() {
        return IntStream.range(0, tileEntityContainer.getSize()).toArray();
    }

    /* CONTAINER RELATED METHODS */

    public TileEntityContainer getTileEntityContainer() {
        return tileEntityContainer;
    }

    public void setTileEntityContainer(TileEntityContainer tileEntityContainer) {
        this.tileEntityContainer = tileEntityContainer;
    }

    /* DATA RELATED METHODS */

    public abstract void executeInsertQuery(boolean async);

    public abstract void executeUpdateQuery(boolean async);

    public abstract StatementHolder getSelectQuery();

    public void loadFromData(ResultSet resultSet) throws SQLException {
        String serialized = resultSet.getString("inventories");
        if(!serialized.isEmpty()) {
            InventoryHolder[] inventories = plugin.getNMSAdapter().deserialze(serialized);
            for (int i = 0; i < inventories.length; i++)
                setPage(i, inventories[i]);
        }
    }

    public void loadFromFile(YamlConfiguration cfg){
        if (cfg.contains("inventory")) {
            ChestData chestData = getData();
            for (String inventoryIndex : cfg.getConfigurationSection("inventory").getKeys(false)) {
                Inventory inventory = setPage(Integer.parseInt(inventoryIndex), chestData.getDefaultSize(), chestData.getTitle(Integer.parseInt(inventoryIndex) + 1));
                if(cfg.isConfigurationSection("inventory." + inventoryIndex)){
                    for (String slot : cfg.getConfigurationSection("inventory." + inventoryIndex).getKeys(false)) {
                        try {
                            inventory.setItem(Integer.parseInt(slot), cfg.getItemStack("inventory." + inventoryIndex + "." + slot));
                        } catch (Exception ex) {
                            break;
                        }
                    }
                }
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WChest wChest = (WChest) o;
        return location.equals(wChest.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    private int getViewersAmount(List<HumanEntity> viewersList){
        int viewers = 0;

        for(HumanEntity viewer : viewersList){
            if(viewer.getGameMode() != GameMode.SPECTATOR)
                viewers++;
        }

        return viewers;
    }

}
