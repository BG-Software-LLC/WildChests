package com.bgsoftware.wildchests.nms.v1_16_R3.utils;

import net.minecraft.server.v1_16_R3.NBTBase;
import net.minecraft.server.v1_16_R3.NBTReadLimiter;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagEnd;
import net.minecraft.server.v1_16_R3.NBTTagTypes;

import java.io.DataInput;
import java.io.IOException;

/**
 * This class reimplements {@link net.minecraft.server.v1_16_R3.NBTCompressedStreamTools} without the CrashReport part.
 * For more information, see https://github.com/BG-Software-LLC/WildChests/issues/258
 */
public class NbtUtils {

    private NbtUtils() {

    }

    public static NBTTagCompound read(DataInput input, NBTReadLimiter tracker) throws IOException {
        NBTBase nbtBase = readUnnamedTag(input, tracker);

        if (nbtBase instanceof NBTTagCompound) {
            return (NBTTagCompound) nbtBase;
        }

        throw new IOException("Root tag must be a named compound tag");
    }

    private static NBTBase readUnnamedTag(DataInput input, NBTReadLimiter tracker) throws IOException {
        byte firstByte = input.readByte();

        if (firstByte == 0) {
            return NBTTagEnd.b;
        }

        input.readUTF();
        return readTag(input, tracker, firstByte);
    }

    private static NBTBase readTag(DataInput input, NBTReadLimiter tracker, byte typeId) throws IOException {
        return NBTTagTypes.a(typeId).b(input, 0, tracker);
    }

}
