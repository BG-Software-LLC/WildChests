package com.bgsoftware.wildchests.nms.v117.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;

public class CraftWildInventoryImpl extends CraftInventory implements CraftWildInventory {

    public CraftWildInventoryImpl(Container container) {
        super(container);
    }

    @Override
    public Chest getOwner() {
        return getInventory().chest;
    }

    @Override
    public WildItemStack<ItemStack, CraftItemStack> getWildItem(int slot) {
        return getInventory().getWildItem(slot);
    }

    @Override
    public void setItem(int i, WildItemStack<?, ?> itemStack) {
        getInventory().setItem(i, itemStack, true);
    }

    @Override
    public WildItemStack<?, ?>[] getWildContents() {
        return getInventory().items.toArray(new WildItemStack[0]);
    }

    @Override
    public WildContainer getInventory() {
        return (WildContainer) super.getInventory();
    }

    @Override
    public void setTitle(String title) {
        getInventory().setTitle(title);
    }

    @Override
    public String getTitle() {
        return getInventory().getTitle();
    }

    @Override
    public boolean isFull() {
        return getInventory().isFull();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CraftWildInventoryImpl && getInventory() == ((CraftWildInventoryImpl) obj).getInventory();
    }

}

