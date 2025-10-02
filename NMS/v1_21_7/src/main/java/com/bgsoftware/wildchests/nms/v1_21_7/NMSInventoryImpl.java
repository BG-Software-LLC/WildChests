package com.bgsoftware.wildchests.nms.v1_21_7;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class NMSInventoryImpl extends com.bgsoftware.wildchests.nms.v1_21_7.AbstractNMSInventory {

    @Override
    protected void setDesignItemTag(ItemStack itemStack) {
        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(compoundTag -> compoundTag.putBoolean("DesignItem", true));
        itemStack.set(DataComponents.CUSTOM_DATA, customData);
    }

}
