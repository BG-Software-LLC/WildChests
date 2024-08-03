package com.bgsoftware.wildchests.nms.v1_19;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.nms.NMSAdapter;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftMagicNumbers;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Base64;

public final class NMSAdapterImpl implements NMSAdapter {

    @Override
    public String serialize(org.bukkit.inventory.ItemStack bukkitItem) {
        byte[] data = serializeItemAsBytes(bukkitItem);
        return "*" + new String(Base64.getEncoder().encode(data));
    }

    @Override
    public String serialize(Inventory[] inventories) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("Length", inventories.length);

        for (int slot = 0; slot < inventories.length; slot++) {
            CompoundTag inventoryCompound = new CompoundTag();
            serializeInventory(inventories[slot], inventoryCompound);
            compoundTag.put(slot + "", inventoryCompound);
        }

        try {
            NbtIo.write(compoundTag, dataOutput);
        } catch (Exception ex) {
            return null;
        }

        return "*" + new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    @Override
    public InventoryHolder[] deserialze(String serialized) {
        byte[] buff;

        if (serialized.toCharArray()[0] == '*') {
            buff = Base64.getDecoder().decode(serialized.substring(1));
        } else {
            buff = new BigInteger(serialized, 32).toByteArray();
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buff);
        InventoryHolder[] inventories = new InventoryHolder[0];

        try {
            CompoundTag compoundTag = NbtIo.read(new DataInputStream(inputStream));
            int length = compoundTag.getInt("Length");
            inventories = new InventoryHolder[length];

            for (int i = 0; i < length; i++) {
                if (compoundTag.contains(i + "")) {
                    CompoundTag itemCompound = compoundTag.getCompound(i + "");
                    inventories[i] = deserializeInventory(itemCompound);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return inventories;
    }

    @Override
    public org.bukkit.inventory.ItemStack deserialzeItem(String serialized) {
        if (serialized.isEmpty())
            return new org.bukkit.inventory.ItemStack(Material.AIR);

        byte[] buff;

        if (serialized.toCharArray()[0] == '*') {
            buff = Base64.getDecoder().decode(serialized.substring(1));
        } else {
            buff = new BigInteger(serialized, 32).toByteArray();
        }

        ItemStack itemStack = tryDeserializeNoDataVersionItem(new DataInputStream(new ByteArrayInputStream(buff)));
        if (itemStack != null)
            return CraftItemStack.asBukkitCopy(itemStack);

        return deserializeItemFromBytes(buff);
    }

    @Nullable
    private static ItemStack tryDeserializeNoDataVersionItem(DataInputStream stream) {
        try {
            CompoundTag compoundTag = NbtIo.read(new DataInputStream(stream));
            if (compoundTag.contains("DataVersion"))
                return null;
            return ItemStack.of(compoundTag);
        } catch (Exception ignored) {
        }

        return null;
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World bukkitWorld = location.getWorld();

        if (bukkitWorld == null)
            return;

        ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();
        BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        if (blockEntity instanceof ChestBlockEntity)
            serverLevel.blockEvent(blockPos, blockEntity.getBlockState().getBlock(), 1, open ? 1 : 0);
    }

    @Override
    public org.bukkit.inventory.ItemStack setChestType(org.bukkit.inventory.ItemStack itemStack, ChestType chestType) {
        return setItemTag(itemStack, "chest-type", chestType.name());
    }

    @Override
    public org.bukkit.inventory.ItemStack setChestName(org.bukkit.inventory.ItemStack itemStack, String chestName) {
        return setItemTag(itemStack, "chest-name", chestName);
    }

    @Override
    public String getChestName(org.bukkit.inventory.ItemStack bukkitItem) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        CompoundTag compoundTag = itemStack.getTag();
        return compoundTag == null || !compoundTag.contains("chest-name") ? null :
                compoundTag.getString("chest-name");
    }

    @Override
    public void dropItemAsPlayer(HumanEntity humanEntity, org.bukkit.inventory.ItemStack bukkitItem) {
        Player player = ((CraftHumanEntity) humanEntity).getHandle();
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        player.drop(itemStack, false);
    }

    private org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack bukkitItem, String key, String value) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        CompoundTag compoundTag = itemStack.getOrCreateTag();
        compoundTag.putString(key, value);
        return CraftItemStack.asCraftMirror(itemStack);
    }

    private static byte[] serializeItemAsBytes(org.bukkit.inventory.ItemStack bukkitItem) {
        try {
            return bukkitItem.serializeAsBytes();
        } catch (Throwable ignored) {
        }

        CompoundTag compoundTag = CraftItemStack.asNMSCopy(bukkitItem).save(new CompoundTag());

        compoundTag.putInt("DataVersion", CraftMagicNumbers.INSTANCE.getDataVersion());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(compoundTag, outputStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return outputStream.toByteArray();
    }

    private static org.bukkit.inventory.ItemStack deserializeItemFromBytes(byte[] data) {
        try {
            return org.bukkit.inventory.ItemStack.deserializeBytes(data);
        } catch (Throwable ignored) {
        }

        CompoundTag compoundTag;
        try {
            compoundTag = NbtIo.readCompressed(new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        int itemVersion = compoundTag.getInt("DataVersion");
        int currVersion = CraftMagicNumbers.INSTANCE.getDataVersion();
        if (itemVersion != currVersion) {
            compoundTag = (CompoundTag) DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, compoundTag), itemVersion, currVersion).getValue();
        }

        return CraftItemStack.asCraftMirror(ItemStack.of(compoundTag));
    }

    private static void serializeInventory(Inventory inventory, CompoundTag compoundTag) {
        ListTag itemsList = new ListTag();
        org.bukkit.inventory.ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; ++i) {
            org.bukkit.inventory.ItemStack curr = items[i];
            if (curr != null && curr.getType() != Material.AIR) {
                CompoundTag itemTag = serializeItemAsCompoundTag(curr);
                itemTag.putByte("Slot", (byte) i);
                itemsList.add(itemTag);
            }
        }

        compoundTag.putInt("Size", inventory.getSize());
        compoundTag.put("Items", itemsList);
    }

    private static CompoundTag serializeItemAsCompoundTag(org.bukkit.inventory.ItemStack bukkitItem) {
        byte[] data = serializeItemAsBytes(bukkitItem);

        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(data));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static InventoryHolder deserializeInventory(CompoundTag compoundTag) {
        InventoryHolder inventory = new InventoryHolder(compoundTag.getInt("Size"), "Chest");
        ListTag itemsList = compoundTag.getList("Items", 10);

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            inventory.setItem(itemTag.getByte("Slot"), deserializeItemFromCompoundTag(itemTag));
        }

        return inventory;
    }

    private static org.bukkit.inventory.ItemStack deserializeItemFromCompoundTag(CompoundTag tag) {
        if (!tag.contains("DataVersion"))
            return deserializeItemFromCompoundTagNoVersion(tag);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag, stream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return deserializeItemFromBytes(stream.toByteArray());
    }

    private static org.bukkit.inventory.ItemStack deserializeItemFromCompoundTagNoVersion(CompoundTag tag) {
        return CraftItemStack.asBukkitCopy(ItemStack.of(tag));
    }

}
