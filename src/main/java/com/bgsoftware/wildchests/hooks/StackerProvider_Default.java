package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.api.hooks.StackerProvider;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class StackerProvider_Default implements StackerProvider {

    @Override
    public int getItemAmount(Item item) {
        return item.getItemStack().getAmount();
    }

    @Override
    public void setItemAmount(Item item, int amount) {
        ItemStack itemStack = item.getItemStack().clone();
        itemStack.setAmount(amount);
        item.setItemStack(itemStack);
    }

}
