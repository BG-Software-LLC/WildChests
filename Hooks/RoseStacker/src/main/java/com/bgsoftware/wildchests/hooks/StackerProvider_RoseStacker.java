package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.api.hooks.StackerProvider;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedItem;
import org.bukkit.entity.Item;

public final class StackerProvider_RoseStacker implements StackerProvider {

    @Override
    public int getItemAmount(Item item) {
        StackedItem stackedItem = RoseStackerAPI.getInstance().getStackedItem(item);
        return stackedItem == null ? item.getItemStack().getAmount() : stackedItem.getStackSize();
    }

    @Override
    public void setItemAmount(Item item, int amount) {
        StackedItem stackedItem = RoseStackerAPI.getInstance().getStackedItem(item);
        if (stackedItem != null)
            stackedItem.setStackSize(amount);
    }

}
