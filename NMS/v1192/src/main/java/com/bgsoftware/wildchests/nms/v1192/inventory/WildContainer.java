package com.bgsoftware.wildchests.nms.v1192.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WildContainer implements Container {

    public final NonNullList<WildContainerItem> items;
    public final Chest chest;
    private final int index;

    public BiConsumer<Integer, ItemStack> setItemFunction = null;
    private int maxStack = 64;
    private int nonEmptyItems = 0;
    private String title;

    public WildContainer(int size, String title, Chest chest, int index) {
        this.title = title == null ? "Chest" : title;
        this.items = NonNullList.withSize(size, WildContainerItem.AIR);
        this.chest = chest;
        this.index = index;
    }


    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public ItemStack getItem(int i) {
        return getWildItem(i).getHandle();
    }

    public WildContainerItemImpl getWildItem(int i) {
        return (WildContainerItemImpl) this.items.get(i);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return removeItem(slot, amount, true);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1, false);
    }

    private ItemStack removeItem(int slot, int amount, boolean update) {
        ItemStack stack = this.getItem(slot);
        if (stack == ItemStack.EMPTY) {
            return stack;
        } else {
            ItemStack result;
            if (stack.getCount() <= amount) {
                this.setItem(slot, ItemStack.EMPTY);
                result = stack;
            } else {
                result = CraftItemStack.copyNMSStack(stack, amount);
                stack.shrink(amount);
            }

            if (update)
                this.setChanged();

            return result;
        }
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        setItem(slot, itemStack, true);
    }

    public void setItem(int slot, ItemStack itemStack, boolean setItemFunction) {
        setItem(slot, new WildContainerItemImpl(itemStack), setItemFunction);
    }

    public void setItem(int slot, WildContainerItemImpl wildContainerItem, boolean setItemFunction) {
        ItemStack itemStack = wildContainerItem.getHandle();

        if (setItemFunction && this.setItemFunction != null) {
            this.setItemFunction.accept(slot, itemStack);
            return;
        }

        WildContainerItemImpl original = (WildContainerItemImpl) this.items.set(slot, wildContainerItem);

        if (!ItemStack.matches(original.getHandle(), itemStack)) {
            if (itemStack.isEmpty())
                nonEmptyItems--;
            else
                nonEmptyItems++;
        }

        if (!itemStack.isEmpty() && this.getMaxStackSize() > 0 && itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public NonNullList<ItemStack> getContents() {
        NonNullList<ItemStack> contents = NonNullList.createWithCapacity(this.items.size());
        this.items.forEach(wildContainerItem ->
                contents.add(((WildContainerItemImpl) wildContainerItem).getHandle()));
        return contents;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        if (index != 0 && !((WChest) chest).getTileEntityContainer().getTransaction().contains(who))
            throw new IllegalArgumentException("Opened directly page!");
    }

    @Override
    public void onClose(CraftHumanEntity who) {
    }

    @Override
    public void startOpen(Player player) {
    }

    @Override
    public void stopOpen(Player player) {

    }

    @Override
    public List<HumanEntity> getViewers() {
        try {
            return new ArrayList<>(((WChest) chest).getTileEntityContainer().getTransaction());
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    @Override
    public InventoryHolder getOwner() {
        return null;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemstack) {
        return true;
    }

    public boolean isFull() {
        return nonEmptyItems == getContainerSize();
    }

    public boolean isNotEmpty() {
        return nonEmptyItems > 0;
    }

    @Override
    public boolean isEmpty() {
        return nonEmptyItems <= 0;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getTitle() {
        return title;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public Location getLocation() {
        return chest.getLocation();
    }

    @Override
    public String toString() {
        return "WildInventory{" +
                "title='" + title + '\'' +
                '}';
    }

}

