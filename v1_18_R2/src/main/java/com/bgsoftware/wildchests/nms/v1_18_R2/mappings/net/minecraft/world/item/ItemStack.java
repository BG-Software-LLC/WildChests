package com.bgsoftware.wildchests.nms.v1_18_R2.mappings.net.minecraft.world.item;

import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R2.mappings.MappedObject;
import com.bgsoftware.wildchests.nms.v1_18_R2.mappings.net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;

public class ItemStack extends MappedObject<net.minecraft.world.item.ItemStack> {

    public ItemStack(net.minecraft.world.item.ItemStack handle) {
        super(handle);
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "isEmpty",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public boolean isEmpty() {
        return handle.b();
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "setCount",
            type = Remap.Type.METHOD,
            remappedName = "e")
    public void setCount(int count) {
        handle.e(count);
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "save",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public void save(net.minecraft.nbt.NBTTagCompound nbtTagCompound) {
        handle.b(nbtTagCompound);
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "getTag",
            type = Remap.Type.METHOD,
            remappedName = "t")
    public NBTTagCompound getTag() {
        return NBTTagCompound.ofNullable(handle.t());
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "getOrCreateTag",
            type = Remap.Type.METHOD,
            remappedName = "u")
    public NBTTagCompound getOrCreateTag() {
        return new NBTTagCompound(handle.u());
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "getCount",
            type = Remap.Type.METHOD,
            remappedName = "J")
    public int getCount() {
        return handle.J();
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "shrink",
            type = Remap.Type.METHOD,
            remappedName = "g")
    public void shrink(int amount) {
        handle.g(amount);
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "addTagElement",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public void addTagElement(String key, NBTBase value) {
        handle.a(key, value);
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "of",
            type = Remap.Type.METHOD,
            remappedName = "a")
    public static net.minecraft.world.item.ItemStack of(net.minecraft.nbt.NBTTagCompound tagCompound) {
        return net.minecraft.world.item.ItemStack.a(tagCompound);
    }

    @Remap(classPath = "net.minecraft.world.item.ItemStack",
            name = "matches",
            type = Remap.Type.METHOD,
            remappedName = "b")
    public static boolean matches(net.minecraft.world.item.ItemStack left, net.minecraft.world.item.ItemStack right) {
        return net.minecraft.world.item.ItemStack.b(left, right);
    }

}
