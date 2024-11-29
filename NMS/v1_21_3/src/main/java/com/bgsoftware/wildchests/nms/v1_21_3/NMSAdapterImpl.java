package com.bgsoftware.wildchests.nms.v1_21_3;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.nms.NMSAdapter;
import com.bgsoftware.wildchests.nms.v1_21_3.utils.NbtUtils;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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

    private static final int DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();

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
        } catch (Exception error) {
            error.printStackTrace();
            return null;
        }

        return "*" + new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    @Override
    public InventoryHolder[] deserialze(String serialized) {
        InventoryHolder[] inventories = new InventoryHolder[0];

        if (serialized.isEmpty())
            return inventories;

        byte[] buff;

        if (serialized.toCharArray()[0] == '*') {
            buff = Base64.getDecoder().decode(serialized.substring(1));
        } else {
            buff = new BigInteger(serialized, 32).toByteArray();
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buff);

        try {
            CompoundTag compoundTag = NbtUtils.read(new DataInputStream(inputStream));
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
            CompoundTag compoundTag = NbtUtils.read(new DataInputStream(stream));
            if (compoundTag.contains("DataVersion"))
                return null;
            return ItemStack.parse(MinecraftServer.getServer().registryAccess(), compoundTag).orElseThrow();
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
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag compoundTag = customData.getUnsafe();
            if (compoundTag.contains("chest-name", 8))
                return compoundTag.getString("chest-name");
        }

        return null;
    }

    @Override
    public void dropItemAsPlayer(HumanEntity humanEntity, org.bukkit.inventory.ItemStack bukkitItem) {
        Player player = ((CraftHumanEntity) humanEntity).getHandle();
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        player.drop(itemStack, false);
    }

    private org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack bukkitItem, String key, String value) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);

        CustomData customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(compoundTag -> compoundTag.putString(key, value));
        itemStack.set(DataComponents.CUSTOM_DATA, customData);

        return CraftItemStack.asCraftMirror(itemStack);
    }

    private static byte[] serializeItemAsBytes(org.bukkit.inventory.ItemStack bukkitItem) {
        try {
            return bukkitItem.serializeAsBytes();
        } catch (Throwable ignored) {
        }

        CompoundTag compoundTag = (CompoundTag) CraftItemStack.asNMSCopy(bukkitItem)
                .save(MinecraftServer.getServer().registryAccess());

        compoundTag.putInt("DataVersion", DATA_VERSION);
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
            compoundTag = NbtUtils.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        int itemVersion = compoundTag.getInt("DataVersion");
        if (itemVersion != DATA_VERSION) {
            compoundTag = (CompoundTag) DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, compoundTag), itemVersion, DATA_VERSION).getValue();
        }

        return CraftItemStack.asCraftMirror(ItemStack.parse(MinecraftServer.getServer().registryAccess(), compoundTag).orElseThrow());
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
            return NbtUtils.readCompressed(new ByteArrayInputStream(data), NbtAccounter.unlimitedHeap());
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
        ItemStack itemStack = ItemStack.parse(MinecraftServer.getServer().registryAccess(), tag).orElseThrow();
        return CraftItemStack.asBukkitCopy(itemStack);
    }

}
