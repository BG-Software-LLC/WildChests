package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.key.KeySet;
import com.bgsoftware.wildchests.utils.ItemUtils;
import net.minecraft.server.v1_15_R1.AxisAlignedBB;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.ChatComponentText;
import net.minecraft.server.v1_15_R1.Chunk;
import net.minecraft.server.v1_15_R1.Container;
import net.minecraft.server.v1_15_R1.Containers;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityItem;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.IInventory;
import net.minecraft.server.v1_15_R1.ItemStack;
import net.minecraft.server.v1_15_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
import net.minecraft.server.v1_15_R1.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_15_R1.TileEntity;
import net.minecraft.server.v1_15_R1.TileEntityChest;
import net.minecraft.server.v1_15_R1.TileEntityHopper;
import net.minecraft.server.v1_15_R1.World;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_15_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftChatMessage;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSAdapter_v1_15_R1 implements NMSAdapter {

    @Override
    public String getVersion() {
        return "v1_15_R1";
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(blockPosition);
        if(tileChest != null)
            world.playBlockAction(blockPosition, tileChest.getBlock().getBlock(), 1, open ? 1 : 0);
    }

    @Override
    public int getHopperTransfer(org.bukkit.World world) {
        return ((CraftWorld) world).getHandle().spigotConfig.hopperTransfer;
    }

    @Override
    public int getHopperAmount(org.bukkit.World world) {
        return ((CraftWorld) world).getHandle().spigotConfig.hopperAmount;
    }

    @Override
    public void refreshHopperInventory(Player player, Inventory inventory) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        Container container = new CraftContainer(inventory, entityPlayer, entityPlayer.nextContainerCounter());
        String title = container.getBukkitView().getTitle();
        if(((CraftInventory) inventory).getInventory() instanceof TileEntityHopper)
            title = ((TileEntityHopper) ((CraftInventory) inventory).getInventory()).getCustomName().getString();
        entityPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(container.windowId, Containers.HOPPER, new ChatComponentText(title)));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }

    @Override
    public void setDesignItem(org.bukkit.inventory.ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        itemMeta.setDisplayName(ChatColor.RESET + nmsItem.getName().getString());
        itemStack.setItemMeta(itemMeta);
        itemStack.setAmount(1);
    }

    @Override
    public void setTitle(Inventory bukkitInventory, String title) {
        try {
            IInventory inventory = ((CraftInventory) bukkitInventory).getInventory();

            Optional<Class<?>> optionalClass = Arrays.stream(bukkitInventory.getClass().getDeclaredClasses())
                    .filter(clazz -> clazz.getName().contains("MinecraftInventory")).findFirst();

            if(optionalClass.isPresent()){
                Class<?> minecraftInventory = optionalClass.get();
                Field titleField = minecraftInventory.getDeclaredField("title");
                titleField.setAccessible(true);
                titleField.set(inventory, title);
                titleField.setAccessible(false);
            }else{
                TileEntityHopper tileEntityHopper = (TileEntityHopper) inventory;
                tileEntityHopper.setCustomName(CraftChatMessage.fromStringOrNull(title));
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public String serialize(org.bukkit.inventory.ItemStack itemStack) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(outputStream);

        NBTTagCompound tagCompound = new NBTTagCompound();

        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);

        if(nmsItem != null)
            nmsItem.save(tagCompound);

        try {
            NBTCompressedStreamTools.a(tagCompound, dataOutput);
        }catch(Exception ex){
            return null;
        }

        return new BigInteger(1, outputStream.toByteArray()).toString(32);
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

        return new BigInteger(1, outputStream.toByteArray()).toString(32);
    }

    @Override
    public Inventory[] deserialze(String serialized) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(serialized, 32).toByteArray());
        List<Inventory> inventories = new ArrayList<>();

        try {
            NBTTagCompound tagCompound = NBTCompressedStreamTools.a(new DataInputStream(inputStream));
            int length = tagCompound.getInt("Length");
            inventories = new ArrayList<>(length);

            for(int i = 0; i < length; i++){
                if(tagCompound.hasKey(i + "")) {
                    NBTTagCompound nbtTagCompound = tagCompound.getCompound(i + "");
                    inventories.add(i, deserialize(nbtTagCompound));
                }
            }

        }catch(Exception ignored){}

        return inventories.toArray(new Inventory[0]);
    }

    @Override
    public org.bukkit.inventory.ItemStack deserialzeItem(String serialized) {
        int itemsAmount = serialized.contains("$") ? Integer.parseInt(serialized.split("\\$")[1]) : -1;
        serialized = serialized.split("\\$")[0];
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new BigInteger(serialized, 32).toByteArray());

        try {
            NBTTagCompound nbtTagCompoundRoot = NBTCompressedStreamTools.a(new DataInputStream(inputStream));

            ItemStack nmsItem = ItemStack.a(nbtTagCompoundRoot);
            if(itemsAmount > 0)
                nmsItem.setCount(itemsAmount);

            return CraftItemStack.asBukkitCopy(nmsItem);
        }catch(Exception ex){
            return null;
        }
    }

    @Override
    public void updateTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Chunk chunk = world.getChunkAtWorldCoords(blockPosition);
        TileEntityWildChest tileEntityWildChest = new TileEntityWildChest(chest, world, blockPosition);
        chunk.tileEntities.put(blockPosition.immutableCopy(), tileEntityWildChest);
        world.capturedTileEntities.put(blockPosition.immutableCopy(), tileEntityWildChest);
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
    public org.bukkit.inventory.ItemStack setChestNBT(org.bukkit.inventory.ItemStack itemStack, ChestType chestType) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound tagCompound = nmsItem.getOrCreateTag();

        tagCompound.setString("chest-type", chestType.name());

        nmsItem.setTag(tagCompound);

        return CraftItemStack.asBukkitCopy(nmsItem);
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

    private Inventory deserialize(NBTTagCompound tagCompound){
        Inventory inventory = Bukkit.createInventory(null, tagCompound.getInt("Size"));
        NBTTagList itemsList = tagCompound.getList("Items", 10);

        for(int i = 0; i < itemsList.size(); i++){
            NBTTagCompound nbtTagCompound = itemsList.getCompound(i);
            inventory.setItem(nbtTagCompound.getByte("Slot"), CraftItemStack.asBukkitCopy(ItemStack.a(nbtTagCompound)));
        }

        return inventory;
    }

    private static class TileEntityWildChest extends TileEntityChest{

        private TileEntityChest tileEntityChest = new TileEntityChest();
        private Chest chest;

        TileEntityWildChest(Chest chest, World world, BlockPosition blockPosition){
            this.chest = chest;
            this.world = world;
            updateTile(this, world, blockPosition);
            updateTile(tileEntityChest, world, blockPosition);
        }

        @Override
        public void update() {
            List<org.bukkit.inventory.ItemStack> bukkitItems = new ArrayList<>();
            getContents().stream().filter(itemStack -> itemStack != null && !itemStack.getItem().getName().contains("air"))
                    .forEach(itemStack -> bukkitItems.add(CraftItemStack.asBukkitCopy(itemStack)));
            for(org.bukkit.inventory.ItemStack itemStack : chest.addItems(bukkitItems.toArray(new org.bukkit.inventory.ItemStack[0])).values())
                ItemUtils.dropItem(chest.getLocation(), itemStack);
            super.getContents().clear();
            super.update();
        }

        @Override
        public NBTTagCompound save(NBTTagCompound nbttagcompound) {
            return tileEntityChest.save(nbttagcompound);
        }

        @Override
        public NBTTagCompound b() {
            return save(new NBTTagCompound());
        }

        private void updateTile(TileEntity tileEntity, World world, BlockPosition blockPosition){
            tileEntity.setLocation(world, blockPosition);
        }

    }

}
