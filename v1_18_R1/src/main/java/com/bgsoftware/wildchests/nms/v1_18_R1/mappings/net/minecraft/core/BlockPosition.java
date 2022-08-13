package com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.core;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.MappedObject;

public class BlockPosition extends MappedObject<net.minecraft.core.BlockPosition> {

    public BlockPosition(int x, int y, int z) {
        this(new net.minecraft.core.BlockPosition(x, y, z));
    }

    public BlockPosition(net.minecraft.core.BlockPosition handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.core.Vec3i",
            name = "getX",
            type = Remap.Type.METHOD,
            remappedName = "u")
    public int getX() {
        return handle.u();
    }

    @Remap(classPath = "net.minecraft.core.Vec3i",
            name = "getY",
            type = Remap.Type.METHOD,
            remappedName = "v")
    public int getY() {
        return handle.v();
    }

    @Remap(classPath = "net.minecraft.core.Vec3i",
            name = "getZ",
            type = Remap.Type.METHOD,
            remappedName = "w")
    public int getZ() {
        return handle.w();
    }

}
