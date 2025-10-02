package com.bgsoftware.wildchests.nms.v1_17;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class NMSAdapterImpl extends com.bgsoftware.wildchests.nms.v1_17.AbstractNMSAdapter {

    @Override
    protected int getDataVersion() {
        return SharedConstants.getCurrentVersion().getWorldVersion();
    }

    @Override
    protected int getCompoundTagInt(CompoundTag compoundTag, String key, int def) {
        return compoundTag.contains(key, 3) ? compoundTag.getInt(key) : def;
    }

    @Override
    protected CompoundTag getCompoundTagChildCompound(CompoundTag compoundTag, String key) {
        return compoundTag.getCompound(key);
    }

    @Override
    protected ItemStack parseItemStack(CompoundTag compoundTag) {
        return ItemStack.of(compoundTag);
    }

    @Override
    protected String getChestNameInternal(ItemStack itemStack) {
        CompoundTag compoundTag = itemStack.getTag();
        return compoundTag == null || !compoundTag.contains("chest-name") ? null :
                compoundTag.getString("chest-name");
    }

    @Override
    protected void setItemTag(ItemStack itemStack, String key, String value) {
        CompoundTag compoundTag = itemStack.getOrCreateTag();
        compoundTag.putString(key, value);
    }

    @Override
    protected CompoundTag saveItemStack(ItemStack itemStack) {
        return itemStack.save(new CompoundTag());
    }

    @Override
    protected ListTag getCompoundTagList(CompoundTag compoundTag, String key, int type) {
        return compoundTag.getList(key, type);
    }

    @Override
    protected CompoundTag getListTagChildCompound(ListTag listTag, int i) {
        return listTag.getCompound(i);
    }

}
