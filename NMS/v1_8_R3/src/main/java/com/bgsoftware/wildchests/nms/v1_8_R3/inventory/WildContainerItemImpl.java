package com.bgsoftware.wildchests.nms.v1_8_R3.inventory;

import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import net.minecraft.server.v1_8_R3.ItemStack;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

public class WildContainerItemImpl implements WildContainerItem {

    private final ItemStack handle;
    private final CraftItemStack craftItemStack;

    public WildContainerItemImpl(ItemStack nmsItemStack) {
        this(nmsItemStack, CraftItemStack.asCraftMirror(nmsItemStack));
    }

    public WildContainerItemImpl(ItemStack handle, CraftItemStack craftItemStack) {
        this.handle = handle;
        this.craftItemStack = craftItemStack;
    }

    @Override
    public CraftItemStack getBukkitItem() {
        return craftItemStack;
    }

    public ItemStack getHandle() {
        return handle;
    }

    @Override
    public WildContainerItem copy() {
        return new WildContainerItemImpl(handle.cloneItemStack());
    }

}
