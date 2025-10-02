package com.bgsoftware.wildchests.nms.v1_17;

import net.minecraft.nbt.ByteTag;
import net.minecraft.world.item.ItemStack;

public class NMSInventoryImpl extends com.bgsoftware.wildchests.nms.v1_17.AbstractNMSInventory {

    @Override
    protected void setDesignItemTag(ItemStack itemStack) {
        itemStack.addTagElement("DesignItem", ByteTag.valueOf(true));
    }

}
