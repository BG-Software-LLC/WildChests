package com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.inventory;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.MappedObject;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.inventory.Containers;

public class Container extends MappedObject<net.minecraft.world.inventory.Container> {

    public Container(net.minecraft.world.inventory.Container handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.world.inventory.AbstractContainerMenu",
            name = "getType",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public Containers<?> getType() {
        return handle.a();
    }

    public void setTitle(IChatBaseComponent title) {
        handle.setTitle(title);
    }

    public IChatBaseComponent getTitle() {
        return handle.getTitle();
    }

    @Remap(classPath = "net.minecraft.world.inventory.AbstractContainerMenu",
            name = "containerId",
            type = Remap.Type.FIELD,
            remappedName = "j")
    public int getSyncId() {
        return handle.j;
    }

}
