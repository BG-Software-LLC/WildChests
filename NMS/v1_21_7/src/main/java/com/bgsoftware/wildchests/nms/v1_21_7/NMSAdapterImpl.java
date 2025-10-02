package com.bgsoftware.wildchests.nms.v1_21_7;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.slf4j.Logger;

public class NMSAdapterImpl extends com.bgsoftware.wildchests.nms.v1_21_7.AbstractNMSAdapter {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    protected int getDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    @Override
    protected int getCompoundTagInt(CompoundTag compoundTag, String key, int def) {
        return compoundTag.getIntOr(key, def);
    }

    @Override
    protected CompoundTag getCompoundTagChildCompound(CompoundTag compoundTag, String key) {
        return compoundTag.getCompoundOrEmpty(key);
    }

    @Override
    protected ItemStack parseItemStack(CompoundTag compoundTag) {
        DynamicOps<Tag> context = MinecraftServer.getServer().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return ItemStack.CODEC.parse(context, compoundTag)
                .resultOrPartial((itemId) -> LOGGER.error("Tried to load invalid item: '{}'", itemId))
                .orElseThrow();
    }

    @Override
    protected String getChestNameInternal(ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag compoundTag = customData.getUnsafe();
            return compoundTag.getStringOr("chest-name", null);
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
        DynamicOps<Tag> context = MinecraftServer.getServer().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        return (CompoundTag) ItemStack.CODEC.encodeStart(context, itemStack).getOrThrow();
    }

    @Override
    protected ListTag getCompoundTagList(CompoundTag compoundTag, String key, int type) {
        return compoundTag.getListOrEmpty(key);
    }

    @Override
    protected CompoundTag getListTagChildCompound(ListTag listTag, int i) {
        return listTag.getCompoundOrEmpty(i);
    }

}
