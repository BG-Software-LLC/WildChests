package com.bgsoftware.wildchests.nms;

import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.EntityPlayer;
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

public final class NMSMappings_v1_19_R1 {

    private NMSMappings_v1_19_R1() {

    }

    public static boolean isEmpty(ItemStack itemStack) {
        return itemStack.b();
    }

    public static void setCount(ItemStack itemStack, int count) {
        itemStack.e(count);
    }

    public static void save(ItemStack itemStack, NBTTagCompound nbtTagCompound) {
        itemStack.b(nbtTagCompound);
    }

    public static NBTTagCompound getTag(ItemStack itemStack) {
        return itemStack.u();
    }

    public static NBTTagCompound getOrCreateTag(ItemStack itemStack) {
        return itemStack.v();
    }

    public static int getCount(ItemStack itemStack) {
        return itemStack.J();
    }

    public static void shrink(ItemStack itemStack, int amount) {
        itemStack.g(amount);
    }

    public static void putInt(NBTTagCompound nbtTagCompound, String key, int value) {
        nbtTagCompound.a(key, value);
    }

    public static void put(NBTTagCompound nbtTagCompound, String key, NBTBase nbtBase) {
        nbtTagCompound.a(key, nbtBase);
    }

    public static int getInt(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.h(key);
    }

    public static boolean contains(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.e(key);
    }

    public static NBTTagCompound getCompound(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.p(key);
    }

    public static String getString(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.l(key);
    }

    public static void putString(NBTTagCompound nbtTagCompound, String key, String value) {
        nbtTagCompound.a(key, value);
    }

    public static void putByte(NBTTagCompound nbtTagCompound, String key, byte value) {
        nbtTagCompound.a(key, value);
    }

    public static NBTTagList getList(NBTTagCompound nbtTagCompound, String key, int type) {
        return nbtTagCompound.c(key, type);
    }

    public static byte getByte(NBTTagCompound nbtTagCompound, String key) {
        return nbtTagCompound.f(key);
    }

    public static TileEntity getBlockEntity(World world, BlockPosition blockPosition) {
        return world.c_(blockPosition);
    }

    public static void blockEvent(World world, BlockPosition blockPosition, Block block, int i, int j) {
        world.a(blockPosition, block, i, j);
    }

    public static void removeBlockEntity(World world, BlockPosition blockPosition) {
        world.m(blockPosition);
    }

    public static Chunk getChunkAt(World world, BlockPosition blockPosition) {
        return world.l(blockPosition);
    }

    public static IBlockData getBlockState(World world, BlockPosition blockPosition) {
        return world.a_(blockPosition);
    }

    public static void updateNeighborsAt(World world, BlockPosition blockPosition, Block block) {
        world.b(blockPosition, block);
    }

    public static void setBlockEntity(World world, TileEntity tileEntity) {
        world.a(tileEntity);
    }

    public static IBlockData getBlockState(TileEntity tileEntity) {
        return tileEntity.q();
    }

    public static Block getBlock(IBlockData blockData) {
        return blockData.b();
    }

    public static NBTTagCompound getCompound(NBTTagList nbtTagList, int index) {
        return nbtTagList.a(index);
    }

    public static void drop(EntityHuman entityHuman, ItemStack itemStack, boolean flag) {
        entityHuman.a(itemStack, flag);
    }

    public static PlayerInventory getInventory(EntityHuman entityHuman) {
        return entityHuman.fB();
    }

    public static boolean isSpectator(EntityHuman entityHuman) {
        return entityHuman.B_();
    }

    public static void send(PlayerConnection playerConnection, Packet<?> packet) {
        playerConnection.a(packet);
    }

    public static Containers<?> getType(Container container) {
        return container.a();
    }

    public static void initMenu(EntityPlayer entityPlayer, Container container) {
        entityPlayer.a(container);
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

    public static ItemStack getItem(EntityItem entityItem) {
        return entityItem.h();
    }

    public static void discard(Entity entity) {
        entity.ah();
    }

    public static double getX(Entity entity) {
        return entity.dg();
    }

    public static double getY(Entity entity) {
        return entity.di();
    }

    public static double getZ(Entity entity) {
        return entity.dm();
    }

}
