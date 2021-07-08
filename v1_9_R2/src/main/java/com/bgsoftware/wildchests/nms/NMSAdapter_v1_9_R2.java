package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.key.KeySet;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import net.minecraft.server.v1_9_R2.AxisAlignedBB;
import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.Chunk;
import net.minecraft.server.v1_9_R2.Entity;
import net.minecraft.server.v1_9_R2.EntityHuman;
import net.minecraft.server.v1_9_R2.EntityItem;
import net.minecraft.server.v1_9_R2.ItemStack;
import net.minecraft.server.v1_9_R2.NBTCompressedStreamTools;
import net.minecraft.server.v1_9_R2.NBTTagCompound;
import net.minecraft.server.v1_9_R2.NBTTagList;
import net.minecraft.server.v1_9_R2.TileEntityChest;
import net.minecraft.server.v1_9_R2.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_9_R2.CraftChunk;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_9_R2.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public final class NMSAdapter_v1_9_R2 implements NMSAdapter {

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

        if(nmsItem != null) {
            nmsItem.count = 1;
            nmsItem.save(tagCompound);
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
        tagCompound.setInt("Length", inventories.length);

        for(int slot = 0; slot < inventories.length; slot++) {
            NBTTagCompound inventoryCompound = new NBTTagCompound();
            serialize(inventories[slot], inventoryCompound);
            tagCompound.set(slot + "", inventoryCompound);
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
            NBTTagCompound tagCompound = NBTCompressedStreamTools.a(new DataInputStream(inputStream));
            int length = tagCompound.getInt("Length");
            inventories = new InventoryHolder[length];

            for(int i = 0; i < length; i++){
                if(tagCompound.hasKey(i + "")) {
                    NBTTagCompound nbtTagCompound = tagCompound.getCompound(i + "");
                    inventories[i] = deserialize(nbtTagCompound);
                }
            }

        }catch(Exception ignored){}

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
            NBTTagCompound nbtTagCompoundRoot = NBTCompressedStreamTools.a(new DataInputStream(inputStream));

            ItemStack nmsItem = ItemStack.createStack(nbtTagCompoundRoot);

            return CraftItemStack.asBukkitCopy(nmsItem);
        }catch(Exception ex){
            return null;
        }
    }

    @Override
    public Stream<Item> getNearbyItems(Location location, int range, boolean onlyChunk, KeySet blacklisted, KeySet whitelisted) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        List<Entity> entityList = new ArrayList<>();

        if(onlyChunk){
            Chunk chunk = ((CraftChunk) location.getChunk()).getHandle();
            for(int i = 0; i < chunk.entitySlices.length; i++)
                entityList.addAll(chunk.entitySlices[i]);
            entityList = entityList.stream().filter(entity -> entity instanceof EntityItem).collect(Collectors.toList());
        }
        else {
            AxisAlignedBB boundingBox = new AxisAlignedBB(location.getX() + range, location.getY() + range, location.getZ() + range,
                    location.getX() - range, location.getY() - range, location.getZ() - range);
            entityList = world.getEntities(null, boundingBox, entity -> entity instanceof EntityItem);
        }

        return entityList.stream().map(entity -> (Item) entity.getBukkitEntity())
                .filter(item -> !blacklisted.contains(item.getItemStack()) && (whitelisted.isEmpty() || whitelisted.contains(item.getItemStack())));
    }

    @Override
    public void spawnSuctionParticle(Location location) {
        location.getWorld().spawnParticle(Particle.CLOUD, location, 0);
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(blockPosition);
        if(tileChest != null)
            world.playBlockAction(blockPosition, tileChest.getBlock(), 1, open ? 1 : 0);
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

    private org.bukkit.inventory.ItemStack setItemTag(org.bukkit.inventory.ItemStack itemStack, String key, String value){
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getTag();

        if(tagCompound == null)
            nmsItem.setTag((tagCompound = new NBTTagCompound()));

        tagCompound.setString(key, value);

        return CraftItemStack.asCraftMirror(nmsItem);
    }

    private void serialize(Inventory inventory, NBTTagCompound tagCompound){
        NBTTagList itemsList = new NBTTagList();
        org.bukkit.inventory.ItemStack[] items = inventory.getContents();

        for(int i = 0; i < items.length; ++i) {
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

    private InventoryHolder deserialize(NBTTagCompound tagCompound){
        InventoryHolder inventory = new InventoryHolder(tagCompound.getInt("Size"), "Chest");
        NBTTagList itemsList = tagCompound.getList("Items", 10);

        for(int i = 0; i < itemsList.size(); i++){
            NBTTagCompound nbtTagCompound = itemsList.get(i);
            inventory.setItem(nbtTagCompound.getByte("Slot"), CraftItemStack.asBukkitCopy(ItemStack.createStack(nbtTagCompound)));
        }

        return inventory;
    }

}
