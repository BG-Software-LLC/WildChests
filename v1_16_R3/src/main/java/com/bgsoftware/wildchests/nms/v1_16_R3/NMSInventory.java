package com.bgsoftware.wildchests.nms.v1_16_R3;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.nms.v1_16_R3.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.nms.v1_16_R3.inventory.TileEntityWildChest;
import com.bgsoftware.wildchests.nms.v1_16_R3.inventory.WildContainerChest;
import com.bgsoftware.wildchests.nms.v1_16_R3.inventory.WildContainerHopper;
import com.bgsoftware.wildchests.nms.v1_16_R3.inventory.WildContainerItemImpl;
import com.bgsoftware.wildchests.nms.v1_16_R3.inventory.WildInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.Container;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.NBTTagByte;
import net.minecraft.server.v1_16_R3.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_16_R3.PlayerInventory;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.World;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSInventory implements com.bgsoftware.wildchests.nms.NMSInventory {

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
            world.removeTileEntity(blockPosition);
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
            world.removeTileEntity(blockPosition);
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

        Container container = createContainer(entityPlayer.nextContainerCounter(), entityPlayer.inventory, inventory);

        container.setTitle(new ChatComponentText(title));

        entityPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(container.windowId, container.getType(), container.getTitle()));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }

    @Override
    public void createDesignItem(com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory, org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE) :
                itemStack.clone());

        designItem.setCount(1);
        designItem.a("DesignItem", NBTTagByte.a(true));

        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        inventory.setItem(0, designItem, false);
        inventory.setItem(1, designItem, false);
        inventory.setItem(3, designItem, false);
        inventory.setItem(4, designItem, false);
    }

    public static Container createContainer(int id, PlayerInventory playerInventory, com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory) {
        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        return inventory.getSize() == 5 ? WildContainerHopper.of(id, playerInventory, inventory) :
                WildContainerChest.of(id, playerInventory, inventory);
    }

}
