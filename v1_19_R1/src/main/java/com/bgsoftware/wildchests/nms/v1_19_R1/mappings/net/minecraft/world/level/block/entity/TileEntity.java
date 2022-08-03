package com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.block.entity;

import com.bgsoftware.wildchests.nms.mapping.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.MappedObject;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.block.state.IBlockData;

public class TileEntity extends MappedObject<net.minecraft.world.level.block.entity.TileEntity> {

    public TileEntity(net.minecraft.world.level.block.entity.TileEntity handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.BlockEntity",
            name = "getBlockState",
            type = Remap.Type.METHOD,
            remappedName = "q")
    public net.minecraft.world.level.block.state.IBlockData getBlockStateNoMappings() {
        return handle.q();
    }

    public IBlockData getBlockState() {
        return new IBlockData(getBlockStateNoMappings());
    }

}
