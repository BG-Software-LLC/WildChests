package com.bgsoftware.wildchests.nms.v1_12_R1.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import net.minecraft.server.v1_12_R1.ChatComponentText;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.IInventory;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WildInventory implements IInventory {

    private static final WildItemStack<ItemStack, CraftItemStack> AIR = new WildItemStack<>(ItemStack.a, CraftItemStack.asCraftMirror(ItemStack.a));

    public final NonNullList<WildItemStack<ItemStack, CraftItemStack>> items;
    public final Chest chest;
    private final int index;

    public BiConsumer<Integer, ItemStack> setItemFunction = null;
    private int maxStack = 64;
    private int nonEmptyItems = 0;
    private String title;

    public WildInventory(int size, String title, Chest chest, int index) {
        this.title = title == null ? "Chest" : title;
        this.items = NonNullList.a(size, AIR);
        this.chest = chest;
        this.index = index;
    }

    public int getSize() {
        return this.items.size();
    }

    public ItemStack getItem(int i) {
        return getWildItem(i).getItemStack();
    }

    public WildItemStack<ItemStack, CraftItemStack> getWildItem(int i) {
        return this.items.get(i);
    }

    public ItemStack splitStack(int slot, int amount) {
        return splitStack(slot, amount, true);
    }

    public ItemStack splitWithoutUpdate(int slot) {
        return splitStack(slot, 1, false);
    }

    private ItemStack splitStack(int slot, int amount, boolean update) {
        ItemStack stack = this.getItem(slot);
        if (stack == ItemStack.a) {
            return stack;
        } else {
            ItemStack result;
            if (stack.getCount() <= amount) {
                this.setItem(slot, ItemStack.a);
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
        setItem(i, new WildItemStack<>(itemStack, CraftItemStack.asCraftMirror(itemStack)), setItemFunction);
    }

    public void setItem(int i, WildItemStack<?, ?> wildItemStack, boolean setItemFunction) {
        ItemStack itemstack = (ItemStack) wildItemStack.getItemStack();

        if (setItemFunction && this.setItemFunction != null) {
            this.setItemFunction.accept(i, itemstack);
            return;
        }

        //noinspection unchecked
        WildItemStack<ItemStack, CraftItemStack> original = this.items.set(i, (WildItemStack<ItemStack, CraftItemStack>) wildItemStack);

        if (!ItemStack.matches(original.getItemStack(), itemstack)) {
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
        NonNullList<ItemStack> contents = NonNullList.a(this.items.size(), ItemStack.a);
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
        return nonEmptyItems == (chest.getPage(0).getSize() * chest.getPagesAmount());
    }

    public boolean x_() {
        return nonEmptyItems > 0;
    }

    @Override
    public int getProperty(int i) {
        return 0;
    }

    @Override
    public void setProperty(int i, int i1) {

    }

    @Override
    public int h() {
        return 0;
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
