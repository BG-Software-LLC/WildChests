package com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.nbt;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.MappedObject;
import net.minecraft.nbt.NBTBase;

public class NBTTagList extends MappedObject<net.minecraft.nbt.NBTTagList> {

    public NBTTagList() {
        this(new net.minecraft.nbt.NBTTagList());
    }

    public NBTTagList(net.minecraft.nbt.NBTTagList handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.nbt.ListTag",
            name = "getCompound",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public NBTTagCompound getCompound(int index) {
        return new NBTTagCompound(handle.a(index));
    }

    @Remap(classPath = "net.minecraft.nbt.ListTag",
            name = "size",
            type = Remap.Type.METHOD,
            remappedName = "size")
    public int size() {
        return handle.size();
    }

    @Remap(classPath = "net.minecraft.nbt.ListTag",
            name = "add",
            type = Remap.Type.METHOD,
            remappedName = "add")
    public void add(NBTBase nbtBase) {
        handle.add(nbtBase);
    }

}
