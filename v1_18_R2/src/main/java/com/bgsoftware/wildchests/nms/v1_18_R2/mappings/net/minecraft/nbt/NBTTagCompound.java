package com.bgsoftware.wildchests.nms.v1_18_R2.mappings.net.minecraft.nbt;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R2.mappings.MappedObject;
import net.minecraft.nbt.NBTBase;

public class NBTTagCompound extends MappedObject<net.minecraft.nbt.NBTTagCompound> {

    public static NBTTagCompound ofNullable(net.minecraft.nbt.NBTTagCompound handle) {
        return handle == null ? null : new NBTTagCompound(handle);
    }

    public NBTTagCompound() {
        this(new net.minecraft.nbt.NBTTagCompound());
    }

    public NBTTagCompound(net.minecraft.nbt.NBTTagCompound handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "putInt",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void putInt(String key, int value) {
        handle.a(key, value);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "put",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void put(String key, NBTBase nbtBase) {
        handle.a(key, nbtBase);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "getInt",
            type = Remap.Type.METHOD,
            remappedName = "h")
    public int getInt(String key) {
        return handle.h(key);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "contains",
            type = Remap.Type.METHOD,
            remappedName = "e")
    public boolean contains(String key) {
        return handle.e(key);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "getCompound",
            type = Remap.Type.METHOD,
            remappedName = "p")
    public NBTTagCompound getCompound(String key) {
        return ofNullable(handle.p(key));
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "getString",
            type = Remap.Type.METHOD,
            remappedName = "l")
    public String getString(String key) {
        return handle.l(key);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "putString",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void putString(String key, String value) {
        handle.a(key, value);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "putByte",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void putByte(String key, byte value) {
        handle.a(key, value);
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "getList",
            type = Remap.Type.METHOD,
            remappedName = "c")
    public NBTTagList getList(String key, int type) {
        return new NBTTagList(handle.c(key, type));
    }

    @Remap(classPath = "net.minecraft.nbt.CompoundTag",
            name = "getByte",
            type = Remap.Type.METHOD,
            remappedName = "f")
    public byte getByte(String key) {
        return handle.f(key);
    }

}
