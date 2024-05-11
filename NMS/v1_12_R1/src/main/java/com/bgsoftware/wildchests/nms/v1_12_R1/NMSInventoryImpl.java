package com.bgsoftware.wildchests.nms.v1_12_R1;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.nms.NMSInventory;
import com.bgsoftware.wildchests.nms.v1_12_R1.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.nms.v1_12_R1.inventory.TileEntityWildChest;
import com.bgsoftware.wildchests.nms.v1_12_R1.inventory.WildContainerChest;
import com.bgsoftware.wildchests.nms.v1_12_R1.inventory.WildContainerHopper;
import com.bgsoftware.wildchests.nms.v1_12_R1.inventory.WildContainerItemImpl;
import com.bgsoftware.wildchests.nms.v1_12_R1.inventory.WildInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.ChatComponentText;
import net.minecraft.server.v1_12_R1.Container;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTTagByte;
import net.minecraft.server.v1_12_R1.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_12_R1.PlayerInventory;
import net.minecraft.server.v1_12_R1.TileEntity;
import net.minecraft.server.v1_12_R1.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public final class NMSInventoryImpl implements NMSInventory {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    @Override
    public void updateTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        TileEntity tileEntity = world.getTileEntity(blockPosition);

        TileEntityWildChest tileEntityWildChest;

        if (tileEntity instanceof TileEntityWildChest) {
            tileEntityWildChest = (TileEntityWildChest) tileEntity;
            ((WChest) chest).setTileEntityContainer(tileEntityWildChest);
        } else {
            tileEntityWildChest = new TileEntityWildChest(chest, world, blockPosition);
            world.s(blockPosition);
            world.setTileEntity(blockPosition, tileEntityWildChest);
        }
    }

    @Override
    public void removeTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        TileEntity currentTileEntity = world.getTileEntity(blockPosition);
        if (currentTileEntity instanceof TileEntityWildChest)
            world.s(blockPosition);
    }

    @Override
    public WildContainerItemImpl createItemStack(org.bukkit.inventory.ItemStack itemStack) {
        return new WildContainerItemImpl(CraftItemStack.asNMSCopy(itemStack));
    }

    @Override
    public com.bgsoftware.wildchests.objects.inventory.CraftWildInventory createInventory(Chest chest, int size, String title, int index) {
        WildInventory wildInventory = new WildInventory(size, title, chest, index);

        if (chest instanceof StorageChest)
            wildInventory.setItemFunction = (slot, itemStack) -> chest.setItem(slot, CraftItemStack.asCraftMirror(itemStack));

        return new CraftWildInventory(wildInventory);
    }

    @Override
    public void openPage(Player player, com.bgsoftware.wildchests.objects.inventory.CraftWildInventory inventory) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        String title = inventory.getTitle();

        Container container = createContainer(entityPlayer.inventory, entityPlayer, inventory);
        container.windowId = entityPlayer.nextContainerCounter();
        TileEntityWildChest tileEntityWildChest = getTileEntity(inventory.getOwner());

        entityPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(container.windowId, tileEntityWildChest.getContainerName(), new ChatComponentText(title), inventory.getSize()));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }

    @Override
    public void createDesignItem(com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory, org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15) :
                itemStack.clone());

        designItem.setCount(1);
        designItem.a("DesignItem", new NBTTagByte((byte) 1));

        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        inventory.setItem(0, designItem, false);
        inventory.setItem(1, designItem, false);
        inventory.setItem(3, designItem, false);
        inventory.setItem(4, designItem, false);
    }

    public static Container createContainer(PlayerInventory playerInventory, EntityHuman entityHuman, com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory) {
        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        return inventory.getSize() == 5 ? WildContainerHopper.of(playerInventory, entityHuman, inventory) :
                WildContainerChest.of(playerInventory, entityHuman, inventory);
    }

    private static TileEntityWildChest getTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return (TileEntityWildChest) world.getTileEntity(blockPosition);
    }

}
