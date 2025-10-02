package com.bgsoftware.wildchests.nms.v1_20_4;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class NMSAdapterImpl extends com.bgsoftware.wildchests.nms.v1_20_4.AbstractNMSAdapter {

    @Override
    protected int getDataVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
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
        return ItemStack.parse(MinecraftServer.getServer().registryAccess(), compoundTag)
                .orElseThrow();
    }

    @Override
    protected String getChestNameInternal(ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag compoundTag = customData.getUnsafe();
            if (compoundTag.contains("chest-name", 8))
                return compoundTag.getString("chest-name");
        }

        return null;
    }

    @Override
    protected void setItemTag(ItemStack itemStack, String key, String value) {
        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(compoundTag -> compoundTag.putString(key, value));
        itemStack.set(DataComponents.CUSTOM_DATA, customData);
    }

    @Override
    protected CompoundTag saveItemStack(ItemStack itemStack) {
        return (CompoundTag) itemStack.save(MinecraftServer.getServer().registryAccess());
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
