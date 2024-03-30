package com.bgsoftware.wildchests.nms.v1_8_R3.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IInventory;
import net.minecraft.server.v1_8_R3.ItemStack;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class WildInventory implements IInventory {

    public final List<WildContainerItem> items;
    public final Chest chest;
    private final int index;

    public BiConsumer<Integer, ItemStack> setItemFunction = null;
    private int maxStack = 64;
    private int nonEmptyItems = 0;
    private String title;

    public WildInventory(int size, String title, Chest chest, int index) {
        this.title = title == null ? "Chest" : title;
        this.chest = chest;
        this.index = index;

        this.items = new ArrayList<>(size);
        for (int i = 0; i < size; ++i)
            this.items.add(WildContainerItem.AIR);
    }

    public int getSize() {
        return this.items.size();
    }

    public ItemStack getItem(int i) {
        return getWildItem(i).getHandle();
    }

    public WildContainerItemImpl getWildItem(int i) {
        return (WildContainerItemImpl) Optional.ofNullable(this.items.get(i)).orElse(WildContainerItem.AIR);
    }

    public ItemStack splitStack(int slot, int amount) {
        return splitStack(slot, amount, true);
    }

    public ItemStack splitWithoutUpdate(int slot) {
        return splitStack(slot, 1, false);
    }

    private ItemStack splitStack(int slot, int amount, boolean update) {
        ItemStack stack = this.getItem(slot);
        if (stack == null) {
            return null;
        } else {
            ItemStack result;
            if (stack.count <= amount) {
                this.setItem(slot, null);
                result = stack;
            } else {
                result = CraftItemStack.copyNMSStack(stack, amount);
                stack.count -= amount;
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

        WildContainerItemImpl original = getWildItem(i);
        this.items.set(i, new WildContainerItemImpl(itemstack));

        if (!ItemStack.matches(original.getHandle(), itemstack)) {
            if (itemstack == null)
                nonEmptyItems--;
            else
                nonEmptyItems++;
        }

        if (itemstack != null && this.getMaxStackSize() > 0 && itemstack.count > this.getMaxStackSize()) {
            itemstack.count = this.getMaxStackSize();
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

    public ItemStack[] getContents() {
        ItemStack[] contents = new ItemStack[this.items.size()];
        for (int i = 0; i < contents.length; i++)
            contents[i] = getItem(i);
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
            return ((WChest) chest).getTileEntityContainer().getTransaction();
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    public org.bukkit.inventory.InventoryHolder getOwner() {
        return null;
    }

    public boolean b(int i, ItemStack itemstack) {
        return true;
    }

    public boolean isFull() {
        return nonEmptyItems == (chest.getPage(0).getSize() * chest.getPagesAmount());
    }

    @Override
    public int getProperty(int i) {
        return 0;
    }

    @Override
    public void b(int i, int i1) {

    }

    @Override
    public int g() {
        return 0;
    }

    @Override
    public void l() {

    }

    @Override
    public boolean hasCustomName() {
        return title != null;
    }

    @Override
    public String getName() {
        return getTitle();
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatComponentText(getTitle());
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "WildInventory{" +
                "title='" + title + '\'' +
                '}';
    }

}
