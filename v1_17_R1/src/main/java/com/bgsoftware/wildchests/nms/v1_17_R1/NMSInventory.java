package com.bgsoftware.wildchests.nms.v1_17_R1;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.nms.v1_17_R1.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.nms.v1_17_R1.inventory.TileEntityWildChest;
import com.bgsoftware.wildchests.nms.v1_17_R1.inventory.WildContainerChest;
import com.bgsoftware.wildchests.nms.v1_17_R1.inventory.WildContainerHopper;
import com.bgsoftware.wildchests.nms.v1_17_R1.inventory.WildInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.chunk.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSInventory implements com.bgsoftware.wildchests.nms.NMSInventory {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static final ReflectMethod<TickingBlockEntity> CREATE_TICKING_BLOCK = new ReflectMethod<>(
            Chunk.class, "a", TileEntity.class, BlockEntityTicker.class);

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
            world.setTileEntity(tileEntityWildChest);
            Chunk chunk = world.getChunkAtWorldCoords(blockPosition);
            world.a(CREATE_TICKING_BLOCK.invoke(chunk, tileEntityWildChest, tileEntityWildChest));
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
    public WildItemStack<?, ?> createItemStack(org.bukkit.inventory.ItemStack itemStack) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        return new WildItemStack<>(nmsItem, CraftItemStack.asCraftMirror(nmsItem));
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

        Container container = createContainer(entityPlayer.nextContainerCounter(), entityPlayer.getInventory(), inventory);

        container.setTitle(new ChatComponentText(title));

        // Cursor item is not updated, so we need to update it manually
        org.bukkit.inventory.ItemStack cursorItem = player.getItemOnCursor();

        entityPlayer.b.sendPacket(new PacketPlayOutOpenWindow(container.j, container.getType(), container.getTitle()));
        entityPlayer.bV = container;
        entityPlayer.initMenu(container);

        player.setItemOnCursor(cursorItem);
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
