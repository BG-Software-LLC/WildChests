package com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.entity;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.MappedObject;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.server.network.PlayerConnection;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;

public class Entity extends MappedObject<net.minecraft.world.entity.Entity> {

    private static final ReflectMethod<ItemStack> ENTITY_ITEM_GET_ITEM;
    private static final ReflectMethod<Double> ENTITY_GET_X;
    private static final ReflectMethod<Double> ENTITY_GET_Y;
    private static final ReflectMethod<Double> ENTITY_GET_Z;
    private static final ReflectMethod<PlayerInventory> ENTITY_HUMAN_GET_INVENTORY;

    static {
        ReflectMethod<?> method119 = new ReflectMethod<>(EntityHuman.class, PlayerInventory.class, "fB");
        boolean is119Mappings = method119.isValid();

        if (is119Mappings) {
            ENTITY_ITEM_GET_ITEM = new ReflectMethod<>(EntityItem.class, "h");
            ENTITY_GET_X = new ReflectMethod<>(net.minecraft.world.entity.Entity.class, "dg");
            ENTITY_GET_Y = new ReflectMethod<>(net.minecraft.world.entity.Entity.class, "di");
            ENTITY_GET_Z = new ReflectMethod<>(net.minecraft.world.entity.Entity.class, "dm");
            ENTITY_HUMAN_GET_INVENTORY = new ReflectMethod<>(EntityHuman.class, "fB");
        } else {
            ENTITY_ITEM_GET_ITEM = null;
            ENTITY_GET_X = null;
            ENTITY_GET_Y = null;
            ENTITY_GET_Z = null;
            ENTITY_HUMAN_GET_INVENTORY = null;
        }

    }

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
        return ENTITY_HUMAN_GET_INVENTORY == null ? ((EntityHuman) handle).fA() :
                ENTITY_HUMAN_GET_INVENTORY.invoke(handle);
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
        return ENTITY_ITEM_GET_ITEM == null ? ((EntityItem) handle).i() : ENTITY_ITEM_GET_ITEM.invoke(handle);
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
        return ENTITY_GET_X == null ? handle.df() : ENTITY_GET_X.invoke(handle);
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getY",
            type = Remap.Type.METHOD,
            remappedName = "dh")
    public double getY() {
        return ENTITY_GET_Y == null ? handle.dh() : ENTITY_GET_Y.invoke(handle);
    }

    @Remap(classPath = "net.minecraft.world.entity.Entity",
            name = "getZ",
            type = Remap.Type.METHOD,
            remappedName = "dl")
    public double getZ() {
        return ENTITY_GET_Z == null ? handle.dl() : ENTITY_GET_Z.invoke(handle);
    }

}
