package com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.entity;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.MappedObject;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;

public class Entity extends MappedObject<net.minecraft.world.entity.Entity> {

    public Entity(net.minecraft.world.entity.Entity handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.world.entity.player.Player",
            name = "drop",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void drop(ItemStack itemStack, boolean flag) {
        ((EntityHuman) handle).a(itemStack, flag);
    }

    @Remap(classPath = "net.minecraft.world.entity.player.Player",
            name = "getInventory",
            type = Remap.Type.METHOD,
            remappedName = "fq")
    public PlayerInventory getInventory() {
        return ((EntityHuman) handle).fq();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "isSpectator",
            type = Remap.Type.METHOD,
            remappedName = "B_")
    public boolean isSpectator() {
        return handle.B_();
    }

    @Remap(classPath = "net.minecraft.server.level.ServerPlayer",
            name = "initMenu",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void initMenu(Container container) {
        ((EntityPlayer) handle).a(container);
    }

    public int nextContainerCounter() {
        return ((EntityPlayer) handle).nextContainerCounter();
    }

    @Remap(classPath = "net.minecraft.server.level.ServerPlayer",
            name = "connection",
            type = Remap.Type.FIELD,
            remappedName = "b")
    public PlayerConnection getPlayerConnection() {
        return new PlayerConnection(((EntityPlayer) handle).b);
    }

    @Remap(classPath = "net.minecraft.world.entity.player.Player",
            name = "containerMenu",
            type = Remap.Type.FIELD,
            remappedName = "bW")
    public void setContainerMenu(Container container) {
        ((EntityHuman) handle).bW = container;
    }

    @Remap(classPath = "net.minecraft.world.entity.item.ItemEntity",
            name = "getItem",
            type = Remap.Type.METHOD,
            remappedName = "h")
    public ItemStack getItem() {
        return ((EntityItem) handle).h();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "discard",
            type = Remap.Type.METHOD,
            remappedName = "ah")
    public void discard() {
        handle.ah();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getX",
            type = Remap.Type.METHOD,
            remappedName = "dc")
    public double getX() {
        return handle.dc();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getY",
            type = Remap.Type.METHOD,
            remappedName = "de")
    public double getY() {
        return handle.de();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getZ",
            type = Remap.Type.METHOD,
            remappedName = "di")
    public double getZ() {
        return handle.di();
    }

}
