package com.bgsoftware.wildchests.nms.v1_18_R2.mappings.net.minecraft.world.level.block.state;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R2.mappings.MappedObject;
import net.minecraft.world.level.block.Block;

public class IBlockData extends MappedObject<net.minecraft.world.level.block.state.IBlockData> {

    public IBlockData(net.minecraft.world.level.block.state.IBlockData handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase",
            name = "getBlock",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public Block getBlock() {
        return handle.b();
    }

}
