package com.bgsoftware.wildchests.nms.v1_8_R3.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import net.minecraft.server.v1_8_R3.IInventory;
import net.minecraft.server.v1_8_R3.ItemStack;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

public class CraftWildInventory extends CraftInventory implements com.bgsoftware.wildchests.objects.inventory.CraftWildInventory {

    public CraftWildInventory(IInventory inventory) {
        super(inventory);
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
        return getInventory().items;
    }

    @Override
    public WildInventory getInventory() {
        return (WildInventory) super.getInventory();
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
    public void setContents(org.bukkit.inventory.ItemStack[] items) {
        for (int i = 0; i < items.length; i++)
            setItem(i, items[i]);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CraftWildInventory && getInventory() == ((CraftWildInventory) obj).getInventory();
    }
}

