package com.bgsoftware.wildchests.nms.v1_20_4.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagTypes;
import net.minecraft.util.FastBufferedInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * This class reimplements {@link net.minecraft.nbt.NbtIo} without the CrashReport part.
 * For more information, see https://github.com/BG-Software-LLC/WildChests/issues/258
 */
public class NbtUtils {

    private NbtUtils() {

    }

    public static CompoundTag read(DataInput input) throws IOException {
        return read(input, NbtAccounter.unlimitedHeap());
    }

    private static CompoundTag read(DataInput input, NbtAccounter tracker) throws IOException {
        Tag nbtBase = readUnnamedTag(input, tracker);

        if (nbtBase instanceof CompoundTag) {
            return (CompoundTag) nbtBase;
        }

        throw new IOException("Root tag must be a named compound tag");
    }

    private static Tag readUnnamedTag(DataInput input, NbtAccounter tracker) throws IOException {
        byte firstByte = input.readByte();

        if (firstByte == 0) {
            return EndTag.INSTANCE;
        }

        StringTag.skipString(input);
        return readTag(input, tracker, firstByte);
    }

    private static Tag readTag(DataInput input, NbtAccounter tracker, byte typeId) throws IOException {
        return TagTypes.getType(typeId).load(input, tracker);
    }

    public static CompoundTag readCompressed(InputStream stream, NbtAccounter tagSizeTracker) throws IOException {
        try (DataInputStream dataInputStream = createDecompressorStream(stream)) {
            return read(dataInputStream, tagSizeTracker);
        }
    }

    private static DataInputStream createDecompressorStream(InputStream stream) throws IOException {
        return new DataInputStream(new FastBufferedInputStream(new GZIPInputStream(stream)));
    }

}
