package com.bgsoftware.wildchests.nms.v1182;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.Base64;

public final class NMSAdapter implements com.bgsoftware.wildchests.nms.NMSAdapter {

    @Override
    public String serialize(org.bukkit.inventory.ItemStack bukkitItem) {
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);

        if (itemStack.isEmpty())
            return "";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        CompoundTag compoundTag = new CompoundTag();

        itemStack.setCount(1);
        itemStack.save(compoundTag);

        try {
            NbtIo.write(compoundTag, dataOutput);
        } catch (Exception ex) {
            return null;
        }

        return "*" + new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    @Override
    public String serialize(Inventory[] inventories) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("Length", inventories.length);

        for (int slot = 0; slot < inventories.length; slot++) {
            CompoundTag inventoryCompound = new CompoundTag();
            serialize(inventories[slot], inventoryCompound);
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
            CompoundTag compoundTag = NbtIo.read(new DataInputStream(inputStream), NbtAccounter.UNLIMITED);
            int length = compoundTag.getInt("Length");
            inventories = new InventoryHolder[length];

            for (int i = 0; i < length; i++) {
                if (compoundTag.contains(i + "")) {
                    CompoundTag itemCompound = compoundTag.getCompound(i + "");
                    inventories[i] = deserialize(itemCompound);
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

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buff);

        try {
            CompoundTag compoundTag = NbtIo.read(new DataInputStream(inputStream), NbtAccounter.UNLIMITED);
            ItemStack itemStack = ItemStack.of(compoundTag);
            return CraftItemStack.asBukkitCopy(itemStack);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World bukkitWorld = location.getWorld();

        if (bukkitWorld == null)
            return;

        ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();
        BlockPos blockPos = new BlockPos(location.getX(), location.getY(), location.getZ());
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

    private static void serialize(Inventory inventory, CompoundTag compoundTag) {
        ListTag itemsList = new ListTag();
        org.bukkit.inventory.ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; ++i) {
            if (items[i] != null) {
                ItemStack itemStack = CraftItemStack.asNMSCopy(items[i]);
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                itemStack.save(itemTag);
                itemsList.add(itemTag);
            }
        }

        compoundTag.putInt("Size", inventory.getSize());
        compoundTag.put("Items", itemsList);
    }

    private static InventoryHolder deserialize(CompoundTag compoundTag) {
        InventoryHolder inventory = new InventoryHolder(compoundTag.getInt("Size"), "Chest");
        ListTag itemsList = compoundTag.getList("Items", 10);

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            inventory.setItem(itemTag.getByte("Slot"), CraftItemStack.asBukkitCopy(ItemStack.of(itemTag)));
        }

        return inventory;
    }

}
