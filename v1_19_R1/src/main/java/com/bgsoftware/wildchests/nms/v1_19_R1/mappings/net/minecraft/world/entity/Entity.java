package com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.entity;

import com.bgsoftware.wildchests.nms.mapping.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.MappedObject;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.server.network.PlayerConnection;
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
            remappedName = "fA")
    public PlayerInventory getInventory() {
        return ((EntityHuman) handle).fA();
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
            remappedName = "bU")
    public void setContainerMenu(Container container) {
        ((EntityHuman) handle).bU = container;
    }

    @Remap(classPath = "net.minecraft.world.entity.item.ItemEntity",
            name = "getItem",
            type = Remap.Type.METHOD,
            remappedName = "i")
    public ItemStack getItem() {
        return ((EntityItem) handle).i();
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
            remappedName = "df")
    public double getX() {
        return handle.df();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getY",
            type = Remap.Type.METHOD,
            remappedName = "dh")
    public double getY() {
        return handle.dh();
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getZ",
            type = Remap.Type.METHOD,
            remappedName = "dl")
    public double getZ() {
        return handle.dl();
    }

}
