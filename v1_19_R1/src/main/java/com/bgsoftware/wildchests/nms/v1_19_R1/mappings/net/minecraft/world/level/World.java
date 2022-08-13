package com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.MappedObject;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.util.RandomSource;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.block.entity.TileEntity;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.phys.AxisAlignedBB;

import java.util.List;
import java.util.function.Predicate;

public class World extends MappedObject<net.minecraft.world.level.World> {

    public World(net.minecraft.world.level.World handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "getBlockEntity",
            type = Remap.Type.METHOD,
            remappedName = "c_")
    public net.minecraft.world.level.block.entity.TileEntity getBlockEntityNoMappings(BlockPosition blockPosition) {
        return handle.c_(blockPosition);
    }

    public TileEntity getBlockEntity(BlockPosition blockPosition) {
        return new TileEntity(getBlockEntityNoMappings(blockPosition));
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "blockEvent",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void blockEvent(BlockPosition blockPosition, Block block, int i, int j) {
        handle.a(blockPosition, block, i, j);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "removeBlockEntity",
            type = Remap.Type.METHOD,
            remappedName = "n")
    public void removeBlockEntity(BlockPosition blockPosition) {
        handle.n(blockPosition);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "getChunkAt",
            type = Remap.Type.METHOD,
            remappedName = "l")
    public Chunk getChunkAt(BlockPosition blockPosition) {
        return handle.l(blockPosition);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "getBlockState",
            type = Remap.Type.METHOD,
            remappedName = "a_")
    public net.minecraft.world.level.block.state.IBlockData getBlockStateNoMappings(BlockPosition blockPosition) {
        return handle.a_(blockPosition);
    }

    public IBlockData getBlockState(BlockPosition blockPosition) {
        return new IBlockData(getBlockStateNoMappings(blockPosition));
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "updateNeighborsAt",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void updateNeighborsAt(BlockPosition blockPosition, Block block) {
        handle.a(blockPosition, block);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "setBlockEntity",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void setBlockEntity(net.minecraft.world.level.block.entity.TileEntity tileEntity) {
        handle.a(tileEntity);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "addBlockEntityTicker",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void addBlockEntityTicker(TickingBlockEntity tickingBlockEntity) {
        handle.a(tickingBlockEntity);
    }

    @Remap(classPath = "net.minecraft.world.level.Level",
            name = "getRandom",
            type = Remap.Type.METHOD,
            remappedName = "r_")
    public RandomSource getRandom() {
        return new RandomSource(handle.r_());
    }

    public <T extends ParticleParam> void sendParticles(EntityPlayer sender, T t0, double d0, double d1, double d2,
                                                        int i, double d3, double d4, double d5, double d6, boolean force) {
        ((WorldServer) handle).sendParticles(sender, t0, d0, d1, d2, i, d3, d4, d5, d6, force);
    }

    @Remap(classPath = "net.minecraft.world.level.EntityGetter",
            name = "getEntitiesOfClass",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> entityClass, AxisAlignedBB boundingBox, Predicate<? super T> predicate) {
        return handle.a(entityClass, boundingBox, predicate);
    }

}
