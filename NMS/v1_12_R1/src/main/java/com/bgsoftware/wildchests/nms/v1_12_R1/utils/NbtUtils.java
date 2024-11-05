package com.bgsoftware.wildchests.nms.v1_12_R1.utils;

import com.bgsoftware.common.reflection.ReflectConstructor;
import com.bgsoftware.common.reflection.ReflectMethod;
import net.minecraft.server.v1_12_R1.NBTBase;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagEnd;

import java.io.DataInput;
import java.io.IOException;

/**
 * This class reimplements {@link net.minecraft.server.v1_12_R1.NBTCompressedStreamTools} without the CrashReport part.
 * For more information, see https://github.com/BG-Software-LLC/WildChests/issues/258
 */
public class NbtUtils {

    private static final ReflectMethod<NBTBase> NBT_BASE_CREATE_TAG = new ReflectMethod<>(
            NBTBase.class, "createTag", byte.class);
    private static final ReflectMethod<Void> NBT_BASE_LOAD = new ReflectMethod<>(
            NBTBase.class, "load", DataInput.class, int.class, NBTReadLimiter.class);

    private static final NBTTagEnd TAG_END = (NBTTagEnd) new ReflectConstructor<>(NBTTagEnd.class).newInstance();

    private NbtUtils() {

    }

    public static NBTTagCompound read(DataInput input) throws IOException {
        return read(input, NBTReadLimiter.a);
    }

    private static NBTTagCompound read(DataInput input, NBTReadLimiter tracker) throws IOException {
        NBTBase nbtBase = readUnnamedTag(input, tracker);

        if (nbtBase instanceof NBTTagCompound) {
            return (NBTTagCompound) nbtBase;
        }

        throw new IOException("Root tag must be a named compound tag");
    }

    private static NBTBase readUnnamedTag(DataInput input, NBTReadLimiter tracker) throws IOException {
        byte firstByte = input.readByte();

        if (firstByte == 0) {
            return TAG_END;
        }

        input.readUTF();
        return readTag(input, tracker, firstByte);
    }

    private static NBTBase readTag(DataInput input, NBTReadLimiter tracker, byte typeId) throws IOException {
        NBTBase nbtBase = NBT_BASE_CREATE_TAG.invoke(null, typeId);
        NBT_BASE_LOAD.invoke(nbtBase, input, 0, tracker);
        return nbtBase;
    }

}
