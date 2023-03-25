package com.bgsoftware.wildchests.nms.v1_16_R3.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.IInventory;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WildInventory implements IInventory {

    public final NonNullList<WildContainerItem> items;
    public final Chest chest;
    private final int index;

    public BiConsumer<Integer, ItemStack> setItemFunction = null;
    private int maxStack = 64;
    private int nonEmptyItems = 0;
    private String title;

    public WildInventory(int size, String title, Chest chest, int index) {
        this.title = title == null ? "Chest" : title;
        this.items = NonNullList.a(size, WildContainerItem.AIR);
        this.chest = chest;
        this.index = index;
    }

    public int getSize() {
        return this.items.size();
    }

    public ItemStack getItem(int i) {
        return getWildItem(i).getHandle();
    }

    public WildContainerItemImpl getWildItem(int i) {
        return (WildContainerItemImpl) this.items.get(i);
    }

    public ItemStack splitStack(int slot, int amount) {
        return splitStack(slot, amount, true);
    }

    public ItemStack splitWithoutUpdate(int slot) {
        return splitStack(slot, 1, false);
    }

    private ItemStack splitStack(int slot, int amount, boolean update) {
        ItemStack stack = this.getItem(slot);
        if (stack == ItemStack.b) {
            return stack;
        } else {
            ItemStack result;
            if (stack.getCount() <= amount) {
                this.setItem(slot, ItemStack.b);
                result = stack;
            } else {
                result = CraftItemStack.copyNMSStack(stack, amount);
                stack.subtract(amount);
            }

            if (update)
                this.update();

            return result;
        }
    }

    public void setItem(int i, ItemStack itemStack) {
        setItem(i, itemStack, true);
    }

    public void setItem(int i, ItemStack itemStack, boolean setItemFunction) {
        setItem(i, new WildContainerItemImpl(itemStack), setItemFunction);
    }

    public void setItem(int i, WildContainerItemImpl wildContainerItem, boolean setItemFunction) {
        ItemStack itemstack = wildContainerItem.getHandle();

        if (setItemFunction && this.setItemFunction != null) {
            this.setItemFunction.accept(i, itemstack);
            return;
        }

        WildContainerItemImpl original = (WildContainerItemImpl) this.items.set(i, wildContainerItem);

        if (!ItemStack.matches(original.getHandle(), itemstack)) {
            if (itemstack.isEmpty())
                nonEmptyItems--;
            else
                nonEmptyItems++;
        }

        if (!itemstack.isEmpty() && this.getMaxStackSize() > 0 && itemstack.getCount() > this.getMaxStackSize()) {
            itemstack.setCount(this.getMaxStackSize());
        }
    }

    public int getMaxStackSize() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    public void update() {
    }

    public boolean a(EntityHuman entityhuman) {
        return true;
    }

    public NonNullList<ItemStack> getContents() {
        NonNullList<ItemStack> contents = NonNullList.a(this.items.size(), ItemStack.b);
        for (int i = 0; i < contents.size(); i++)
            contents.set(i, getItem(i));
        return contents;
    }

    public void onOpen(CraftHumanEntity who) {
        if (index != 0 && !((WChest) chest).getTileEntityContainer().getTransaction().contains(who))
            throw new IllegalArgumentException("Opened directly page!");
    }

    public void onClose(CraftHumanEntity who) {
    }

    @Override
    public void startOpen(EntityHuman entityhuman) {
    }

    @Override
    public void closeContainer(EntityHuman entityhuman) {

    }

    public List<HumanEntity> getViewers() {
        try {
            return new ArrayList<>(((WChest) chest).getTileEntityContainer().getTransaction());
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    public org.bukkit.inventory.InventoryHolder getOwner() {
        return null;
    }

    public boolean b(int i, ItemStack itemstack) {
        return true;
    }

    public boolean isFull() {
        return nonEmptyItems == getSize();
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
    public void clear() {
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
