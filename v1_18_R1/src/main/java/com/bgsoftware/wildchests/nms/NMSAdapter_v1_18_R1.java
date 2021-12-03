package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.key.KeySet;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTCompressedStreamTools;
import net.minecraft.nbt.NBTReadLimiter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntityChest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.stream.Stream;

import static com.bgsoftware.wildchests.nms.NMSMappings_v1_18_R1.*;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSAdapter_v1_18_R1 implements NMSAdapter {

    @Override
    public int getHopperTransfer(org.bukkit.World world) {
        return ((CraftWorld) world).getHandle().spigotConfig.hopperTransfer;
    }

    @Override
    public int getHopperAmount(org.bukkit.World world) {
        return ((CraftWorld) world).getHandle().spigotConfig.hopperAmount;
    }

    @Override
    public String serialize(org.bukkit.inventory.ItemStack itemStack) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();

        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        if(isEmpty(nmsItem))
            return "";

        if(nmsItem != null) {
            setCount(nmsItem, 1);
            save(nmsItem, tagCompound);
        }

        try {
            NBTCompressedStreamTools.a(tagCompound, dataOutput);
        }catch(Exception ex){
            return null;
        }

        return "*" + new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    @Override
    public String serialize(Inventory[] inventories) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();
        setInt(tagCompound, "Length", inventories.length);

        for(int slot = 0; slot < inventories.length; slot++) {
            NBTTagCompound inventoryCompound = new NBTTagCompound();
            serialize(inventories[slot], inventoryCompound);
            set(tagCompound, slot + "", inventoryCompound);
        }

        try {
            NBTCompressedStreamTools.a(tagCompound, dataOutput);
        }catch(Exception ex){
            return null;
        }

        return "*" + new String(Base64.getEncoder().encode(outputStream.toByteArray()));
    }

    @Override
    public InventoryHolder[] deserialze(String serialized) {
        byte[] buff;

        if(serialized.toCharArray()[0] == '*'){
            buff = Base64.getDecoder().decode(serialized.substring(1));
        }
        else{
            buff = new BigInteger(serialized, 32).toByteArray();
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buff);
        InventoryHolder[] inventories = new InventoryHolder[0];

        try {
            NBTTagCompound tagCompound = NBTCompressedStreamTools.a(new DataInputStream(inputStream), NBTReadLimiter.a);
            int length = getInt(tagCompound, "Length");
            inventories = new InventoryHolder[length];

            for(int i = 0; i < length; i++){
                if(hasKey(tagCompound, i + "")) {
                    NBTTagCompound nbtTagCompound = getCompound(tagCompound, i + "");
                    inventories[i] = deserialize(nbtTagCompound);
                }
            }

        }catch(Exception ex){
            ex.printStackTrace();
        }

        return inventories;
    }

    @Override
    public org.bukkit.inventory.ItemStack deserialzeItem(String serialized) {
        if(serialized.isEmpty())
            return new org.bukkit.inventory.ItemStack(Material.AIR);

        byte[] buff;

        if(serialized.toCharArray()[0] == '*'){
            buff = Base64.getDecoder().decode(serialized.substring(1));
        }
        else{
            buff = new BigInteger(serialized, 32).toByteArray();
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(buff);

        try {
            NBTTagCompound nbtTagCompoundRoot = NBTCompressedStreamTools.a(new DataInputStream(inputStream), NBTReadLimiter.a);

            ItemStack nmsItem = ItemStack.a(nbtTagCompoundRoot);

            return CraftItemStack.asBukkitCopy(nmsItem);
        }catch(Exception ex){
            return null;
        }
    }

    @Override
    public Stream<Item> getNearbyItems(Location location, int range, boolean onlyChunk, KeySet blacklisted, KeySet whitelisted) {
        return null;
    }

    @Override
    public void spawnSuctionParticle(Location location) {
        location.getWorld().spawnParticle(Particle.CLOUD, location, 0);
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) getTileEntity(world, blockPosition);
        if(tileChest != null)
            playBlockAction(world, blockPosition, getBlock(getBlock(tileChest)), 1, open ? 1 : 0);
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
        NBTTagCompound tagCompound = getTag(nmsItem);
        return tagCompound == null || !hasKey(tagCompound, "chest-name") ? null :
                getString(tagCompound, "chest-name");
    }

    @Override
    public void dropItemAsPlayer(HumanEntity humanEntity, org.bukkit.inventory.ItemStack bukkitItem) {
        EntityHuman entityHuman = ((CraftHumanEntity) humanEntity).getHandle();
        ItemStack itemStack = CraftItemStack.asNMSCopy(bukkitItem);
        drop(entityHuman, itemStack, false);
    }

    private org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack itemStack, String key, String value){
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = getOrCreateTag(nmsItem);
        setString(tagCompound, key, value);
        return CraftItemStack.asCraftMirror(nmsItem);
    }

    private static void serialize(Inventory inventory, NBTTagCompound tagCompound){
        NBTTagList itemsList = new NBTTagList();
        org.bukkit.inventory.ItemStack[] items = inventory.getContents();

        for(int i = 0; i < items.length; ++i) {
            if (items[i] != null) {
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                setByte(nbtTagCompound, "Slot", (byte) i);
                save(CraftItemStack.asNMSCopy(items[i]), nbtTagCompound);
                itemsList.add(nbtTagCompound);
            }
        }

        setInt(tagCompound, "Size", inventory.getSize());
        set(tagCompound, "Items", itemsList);
    }

    private static InventoryHolder deserialize(NBTTagCompound tagCompound){
        InventoryHolder inventory = new InventoryHolder(getInt(tagCompound, "Size"), "Chest");
        NBTTagList itemsList = getList(tagCompound, "Items", 10);

        for(int i = 0; i < itemsList.size(); i++){
            NBTTagCompound nbtTagCompound = getCompound(itemsList, i);
            inventory.setItem(getByte(nbtTagCompound, "Slot"), CraftItemStack.asBukkitCopy(ItemStack.a(nbtTagCompound)));
        }

        return inventory;
    }

}
