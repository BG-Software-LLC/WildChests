package com.bgsoftware.wildchests.nms;

import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.phys.Vec3D;

public final class NMSMappings_v1_18_R2 {

    private NMSMappings_v1_18_R2() {

    }

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack.b();
    }

    public static void setCount(ItemStack itemStack, int count) {
        itemStack.e(count);
    }

    public static NBTTagCompound save(ItemStack itemStack, NBTTagCompound nbtTagCompound) {
        return itemStack.b(nbtTagCompound);
    }

    public static void setInt(NBTTagCompound nbtTagCompound, String key, int value) {
        nbtTagCompound.a(key, value);
    }

    public static void set(NBTTagCompound nbtTagCompound, String key, NBTBase nbtBase) {
        nbtTagCompound.a(key, nbtBase);
    }

    public static int getInt(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.h(key);
    }

    public static boolean hasKey(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.e(key);
    }

    public static NBTTagCompound getCompound(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.p(key);
    }

    public static TileEntity getTileEntity(World world, BlockPosition blockPosition) {
        return world.c_(blockPosition);
    }

    public static void playBlockAction(World world, BlockPosition blockPosition, Block block, int i, int j) {
        world.a(blockPosition, block, i, j);
    }

    public static IBlockData getBlock(TileEntity tileEntity) {
        return tileEntity.q();
    }

    public static Block getBlock(IBlockData blockData) {
        return blockData.b();
    }

    public static NBTTagCompound getTag(ItemStack itemStack) {
        return itemStack.t();
    }

    public static String getString(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.l(key);
    }

    public static NBTTagCompound getOrCreateTag(ItemStack itemStack) {
        return itemStack.t();
    }

    public static void setString(NBTTagCompound nbtTagCompound, String key, String value) {
        nbtTagCompound.a(key, value);
    }

    public static void setByte(NBTTagCompound nbtTagCompound, String key, byte value) {
        nbtTagCompound.a(key, value);
    }

    public static NBTTagList getList(NBTTagCompound nbtTagCompound, String key, int type) {
        return nbtTagCompound.c(key, type);
    }

    public static NBTTagCompound getCompound(NBTTagList nbtTagList, int index) {
        return nbtTagList.a(index);
    }

    public static byte getByte(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.f(key);
    }

    public static void drop(EntityHuman entityHuman, ItemStack itemStack, boolean flag) {
        entityHuman.a(itemStack, flag);
    }

    public static void removeTileEntity(World world, BlockPosition blockPosition) {
        world.m(blockPosition);
    }

    public static void setTileEntity(WorldServer worldServer, TileEntity tileEntity) {
        worldServer.a(tileEntity);
    }

    public static Chunk getChunkAtWorldCoords(World world, BlockPosition blockPosition) {
        return world.l(blockPosition);
    }

    public static void sendPacket(PlayerConnection playerConnection, Packet<?> packet) {
        playerConnection.a(packet);
    }

    public static PlayerInventory getInventory(EntityHuman entityHuman) {
        return entityHuman.fr();
    }

    public static Containers<?> getType(Container container) {
        return container.a();
    }

    public static void initMenu(EntityPlayer entityPlayer, Container container) {
        entityPlayer.a(container);
    }

    public static IBlockData getType(World world, BlockPosition blockPosition) {
        return world.a_(blockPosition);
    }

    public static boolean isSpectator(EntityHuman entityHuman) {
        return entityHuman.B_();
    }

    public static int getX(BaseBlockPosition baseBlockPosition) {
        return baseBlockPosition.u();
    }

    public static int getY(BaseBlockPosition baseBlockPosition) {
        return baseBlockPosition.v();
    }

    public static int getZ(BaseBlockPosition baseBlockPosition) {
        return baseBlockPosition.w();
    }

    public static ItemStack getItemStack(EntityItem entityItem) {
        return entityItem.h();
    }

    public static void die(Entity entity) {
        entity.ah();
    }

    public static Vec3D getPositionVector(Entity entity) {
        return entity.ac();
    }

    public static double locX(Entity entity) {
        return getPositionVector(entity).b;
    }

    public static double locY(Entity entity) {
        return getPositionVector(entity).c;
    }

    public static double locZ(Entity entity) {
        return getPositionVector(entity).d;
    }

    public static void applyPhysics(World world, BlockPosition blockPosition, Block block) {
        world.b(blockPosition, block);
    }

    public static int getCount(ItemStack itemStack) {
        return itemStack.I();
    }

    public static void subtract(ItemStack itemStack, int amount) {
        itemStack.g(amount);
    }

}
