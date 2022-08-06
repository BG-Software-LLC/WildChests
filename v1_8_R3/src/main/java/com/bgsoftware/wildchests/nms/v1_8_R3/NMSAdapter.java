package com.bgsoftware.wildchests.nms.v1_8_R3;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;
import net.minecraft.server.v1_8_R3.TileEntityChest;
import net.minecraft.server.v1_8_R3.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.Base64;

@SuppressWarnings("unused")
public final class NMSAdapter implements com.bgsoftware.wildchests.nms.NMSAdapter {

    @Override
    public String getMappingsHash() {
        return null;
    }

    @Override
    public String serialize(org.bukkit.inventory.ItemStack itemStack) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();

        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        if (nmsItem != null) {
            nmsItem.count = 1;
            nmsItem.save(tagCompound);
        }

        try {
            NBTCompressedStreamTools.a(tagCompound, dataOutput);
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
        tagCompound.setInt("Length", inventories.length);

        for (int slot = 0; slot < inventories.length; slot++) {
            NBTTagCompound inventoryCompound = new NBTTagCompound();
            serialize(inventories[slot], inventoryCompound);
            tagCompound.set(slot + "", inventoryCompound);
        }

        try {
            NBTCompressedStreamTools.a(tagCompound, dataOutput);
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
            NBTTagCompound tagCompound = NBTCompressedStreamTools.a(new DataInputStream(inputStream));
            int length = tagCompound.getInt("Length");
            inventories = new InventoryHolder[length];

            for (int i = 0; i < length; i++) {
                if (tagCompound.hasKey(i + "")) {
                    NBTTagCompound nbtTagCompound = tagCompound.getCompound(i + "");
                    inventories[i] = deserialize(nbtTagCompound);
                }
            }

        } catch (Exception ignored) {
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
            NBTTagCompound nbtTagCompoundRoot = NBTCompressedStreamTools.a(new DataInputStream(inputStream));

            ItemStack nmsItem = ItemStack.createStack(nbtTagCompoundRoot);

            return CraftItemStack.asBukkitCopy(nmsItem);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(blockPosition);
        if (tileChest != null)
            world.playBlockAction(blockPosition, world.getType(blockPosition).getBlock(), 1, open ? 1 : 0);
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
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getTag();
        return tagCompound == null || !tagCompound.hasKey("chest-name") ? null : tagCompound.getString("chest-name");
    }

    @Override
    public void dropItemAsPlayer(HumanEntity humanEntity, org.bukkit.inventory.ItemStack bukkitItem) {
        EntityHuman entityHuman = ((CraftHumanEntity) humanEntity).getHandle();
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        entityHuman.drop(itemStack, false);
    }

    private org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack itemStack, String key, String value) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getTag();

        if (tagCompound == null)
            nmsItem.setTag((tagCompound = new NBTTagCompound()));

        tagCompound.setString(key, value);

        return CraftItemStack.asCraftMirror(nmsItem);
    }

    private void serialize(Inventory inventory, NBTTagCompound tagCompound) {
        NBTTagList itemsList = new NBTTagList();
        org.bukkit.inventory.ItemStack[] items = inventory.getContents();

        for (int i = 0; i < items.length; ++i) {
            if (items[i] != null) {
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                nbtTagCompound.setByte("Slot", (byte) i);
                CraftItemStack.asNMSCopy(items[i]).save(nbtTagCompound);
                itemsList.add(nbtTagCompound);
            }
        }

        tagCompound.setInt("Size", inventory.getSize());
        tagCompound.set("Items", itemsList);
    }

    private InventoryHolder deserialize(NBTTagCompound tagCompound) {
        InventoryHolder inventory = new InventoryHolder(tagCompound.getInt("Size"), "Chest");
        NBTTagList itemsList = tagCompound.getList("Items", 10);

        for (int i = 0; i < itemsList.size(); i++) {
            NBTTagCompound nbtTagCompound = itemsList.get(i);
            inventory.setItem(nbtTagCompound.getByte("Slot"), CraftItemStack.asBukkitCopy(ItemStack.createStack(nbtTagCompound)));
        }

        return inventory;
    }

}
