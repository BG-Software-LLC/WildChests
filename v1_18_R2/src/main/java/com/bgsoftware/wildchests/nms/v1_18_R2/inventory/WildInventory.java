package com.bgsoftware.wildchests.nms.v1_18_R2.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R2.mappings.net.minecraft.world.item.ItemStack;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WildInventory implements IInventory {

    private static final WildItemStack<net.minecraft.world.item.ItemStack, CraftItemStack> AIR = new WildItemStack<>(
            net.minecraft.world.item.ItemStack.b, CraftItemStack.asCraftMirror(net.minecraft.world.item.ItemStack.b));

    public final NonNullList<WildItemStack<net.minecraft.world.item.ItemStack, CraftItemStack>> items;
    public final Chest chest;
    private final int index;

    public BiConsumer<Integer, net.minecraft.world.item.ItemStack> setItemFunction = null;
    private int maxStack = 64;
    private int nonEmptyItems = 0;
    private String title;

    public WildInventory(int size, String title, Chest chest, int index) {
        this.title = title == null ? "Chest" : title;
        this.items = NonNullList.a(size, AIR);
        this.chest = chest;
        this.index = index;
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "getContainerSize",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public int b() {
        return this.items.size();
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "getItem",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public net.minecraft.world.item.ItemStack a(int i) {
        return getWildItem(i).getItemStack();
    }

    public WildItemStack<net.minecraft.world.item.ItemStack, CraftItemStack> getWildItem(int i) {
        return this.items.get(i);
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "removeItem",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public net.minecraft.world.item.ItemStack a(int slot, int amount) {
        return splitStack(slot, amount, true);
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "removeItemNoUpdate",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public net.minecraft.world.item.ItemStack b(int slot) {
        return splitStack(slot, 1, false);
    }

    private net.minecraft.world.item.ItemStack splitStack(int slot, int amount, boolean update) {
        ItemStack stack = new ItemStack(this.a(slot));
        if (stack.getHandle() == net.minecraft.world.item.ItemStack.b) {
            return stack.getHandle();
        } else {
            net.minecraft.world.item.ItemStack result;
            if (stack.getCount() <= amount) {
                this.a(slot, net.minecraft.world.item.ItemStack.b);
                result = stack.getHandle();
            } else {
                result = CraftItemStack.copyNMSStack(stack.getHandle(), amount);
                stack.shrink(amount);
            }

            if (update)
                this.e();

            return result;
        }
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "setItem",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void a(int i, net.minecraft.world.item.ItemStack itemStack) {
        setItem(i, itemStack, true);
    }

    public void setItem(int i, net.minecraft.world.item.ItemStack itemStack, boolean setItemFunction) {
        setItem(i, new WildItemStack<>(itemStack, CraftItemStack.asCraftMirror(itemStack)), setItemFunction);
    }

    public void setItem(int i, WildItemStack<?, ?> wildItemStack, boolean setItemFunction) {
        ItemStack itemStack = new ItemStack((net.minecraft.world.item.ItemStack) wildItemStack.getItemStack());

        if (setItemFunction && this.setItemFunction != null) {
            this.setItemFunction.accept(i, itemStack.getHandle());
            return;
        }

        //noinspection unchecked
        WildItemStack<net.minecraft.world.item.ItemStack, CraftItemStack> original =
                this.items.set(i, (WildItemStack<net.minecraft.world.item.ItemStack, CraftItemStack>) wildItemStack);

        if (!ItemStack.matches(original.getItemStack(), itemStack.getHandle())) {
            if (itemStack.isEmpty())
                nonEmptyItems--;
            else
                nonEmptyItems++;
        }

        if (!itemStack.isEmpty() && this.getMaxStackSize() > 0 && itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
    }

    private int getMaxStackSize() {
        return this.N_();
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "getMaxStackSize",
            type = Remap.Type.METHOD,
            remappedName = "N_")
    @Override
    public int N_() {
        return this.maxStack;
    }

    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "setChanged",
            type = Remap.Type.METHOD,
            remappedName = "e")
    public void e() {
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "stillValid",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public boolean a(EntityHuman entityhuman) {
        return true;
    }

    public NonNullList<net.minecraft.world.item.ItemStack> getContents() {
        NonNullList<net.minecraft.world.item.ItemStack> contents =
                NonNullList.a(this.items.size(), net.minecraft.world.item.ItemStack.b);
        for (int i = 0; i < contents.size(); i++)
            contents.set(i, a(i));
        return contents;
    }

    public void onOpen(CraftHumanEntity who) {
        if (index != 0 && !((WChest) chest).getTileEntityContainer().getTransaction().contains(who))
            throw new IllegalArgumentException("Opened directly page!");
    }

    public void onClose(CraftHumanEntity who) {
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "startOpen",
            type = Remap.Type.METHOD,
            remappedName = "b_")
    @Override
    public void b_(EntityHuman entityhuman) {
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "stopOpen",
            type = Remap.Type.METHOD,
            remappedName = "c_")
    @Override
    public void c_(EntityHuman entityhuman) {

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

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "canPlaceItem",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public boolean b(int i, net.minecraft.world.item.ItemStack itemstack) {
        return true;
    }

    public boolean isFull() {
        return nonEmptyItems == b();
    }

    public boolean isNotEmpty() {
        return nonEmptyItems > 0;
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "isEmpty",
            type = Remap.Type.METHOD,
            remappedName = "c")
    @Override
    public boolean c() {
        return nonEmptyItems <= 0;
    }

    void setTitle(String title) {
        this.title = title;
    }

    String getTitle() {
        return title;
    }

    @Remap(classPath = "net.minecraft.world.CompoundContainer",
            name = "clearContent",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Override
    public void a() {
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

