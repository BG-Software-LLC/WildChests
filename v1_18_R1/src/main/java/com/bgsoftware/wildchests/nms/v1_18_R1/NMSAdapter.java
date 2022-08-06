package com.bgsoftware.wildchests.nms.v1_18_R1;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.nbt.NBTTagCompound;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.nbt.NBTTagList;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.entity.Entity;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.item.ItemStack;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.level.World;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.level.block.entity.TileEntity;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTReadLimiter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.Base64;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSAdapter implements com.bgsoftware.wildchests.nms.NMSAdapter {

    @Override
    public String getMappingsHash() {
        return ((CraftMagicNumbers) CraftMagicNumbers.INSTANCE).getMappingsVersion();
    }

    @Override
    public String serialize(org.bukkit.inventory.ItemStack itemStack) {
        ItemStack nmsItem = new ItemStack(CraftItemStack.asNMSCopy(itemStack));

        if (nmsItem.isEmpty())
            return "";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();

        if (nmsItem != null) {
            nmsItem.setCount(1);
            nmsItem.save(tagCompound.getHandle());
        }

        try {
            NBTCompressedStreamTools.a(tagCompound.getHandle(), dataOutput);
        } catch (Exception ex) {
            return null;
        }

        return "*" + new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    @Override
    public String serialize(Inventory[] inventories) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.putInt("Length", inventories.length);

        for (int slot = 0; slot < inventories.length; slot++) {
            NBTTagCompound inventoryCompound = new NBTTagCompound();
            serialize(inventories[slot], inventoryCompound);
            tagCompound.put(slot + "", inventoryCompound.getHandle());
        }

        try {
            NBTCompressedStreamTools.a(tagCompound.getHandle(), dataOutput);
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
            NBTTagCompound tagCompound = new NBTTagCompound(NBTCompressedStreamTools.a(new DataInputStream(inputStream), NBTReadLimiter.a));
            int length = tagCompound.getInt("Length");
            inventories = new InventoryHolder[length];

            for (int i = 0; i < length; i++) {
                if (tagCompound.contains(i + "")) {
                    NBTTagCompound nbtTagCompound = tagCompound.getCompound(i + "");
                    inventories[i] = deserialize(nbtTagCompound);
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
            net.minecraft.nbt.NBTTagCompound nbtTagCompoundRoot = NBTCompressedStreamTools.a(new DataInputStream(inputStream), NBTReadLimiter.a);

            net.minecraft.world.item.ItemStack nmsItem = ItemStack.of(nbtTagCompoundRoot);

            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = new World(((CraftWorld) location.getWorld()).getHandle());
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntity tileChest = world.getBlockEntity(blockPosition);
        if (tileChest != null)
            world.blockEvent(blockPosition, tileChest.getBlockState().getBlock(), 1, open ? 1 : 0);
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
    public String getChestName(org.bukkit.inventory.ItemStack itemStack) {
        ItemStack nmsItem = new ItemStack(CraftItemStack.asNMSCopy(itemStack));
        NBTTagCompound tagCompound = nmsItem.getTag();
        return tagCompound == null || !tagCompound.contains("chest-name") ? null :
                tagCompound.getString("chest-name");
    }

    @Override
    public void dropItemAsPlayer(HumanEntity humanEntity, org.bukkit.inventory.ItemStack bukkitItem) {
        Entity entityHuman = new Entity(((CraftHumanEntity) humanEntity).getHandle());
        net.minecraft.world.item.ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        entityHuman.drop(itemStack, false);
    }

    private org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
        ItemStack nmsItem = new ItemStack(CraftItemStack.asNMSCopy(itemStack));
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();
        tagCompound.putString(key, value);
        return CraftItemStack.asCraftMirror(nmsItem.getHandle());
    }

    private static void serialize(Inventory inventory, NBTTagCompound tagCompound) {
        NBTTagList itemsList = new NBTTagList();
        org.bukkit.inventory.ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; ++i) {
            if (items[i] != null) {
                ItemStack itemStack = new ItemStack(CraftItemStack.asNMSCopy(items[i]));
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                nbtTagCompound.putByte("Slot", (byte) i);
                itemStack.save(nbtTagCompound.getHandle());
                itemsList.add(nbtTagCompound.getHandle());
            }
        }

        tagCompound.putInt("Size", inventory.getSize());
        tagCompound.put("Items", itemsList.getHandle());
    }

    private static InventoryHolder deserialize(NBTTagCompound tagCompound) {
        InventoryHolder inventory = new InventoryHolder(tagCompound.getInt("Size"), "Chest");
        NBTTagList itemsList = tagCompound.getList("Items", 10);

        for (int i = 0; i < itemsList.size(); i++) {
            NBTTagCompound nbtTagCompound = itemsList.getCompound(i);
            inventory.setItem(nbtTagCompound.getByte("Slot"),
                    CraftItemStack.asBukkitCopy(ItemStack.of(nbtTagCompound.getHandle())));
        }

        return inventory;
    }

}
