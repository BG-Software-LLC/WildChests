package com.bgsoftware.wildchests.nms.v1_18_R1;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_18_R1.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.nms.v1_18_R1.inventory.TileEntityWildChest;
import com.bgsoftware.wildchests.nms.v1_18_R1.inventory.WildContainerChest;
import com.bgsoftware.wildchests.nms.v1_18_R1.inventory.WildContainerHopper;
import com.bgsoftware.wildchests.nms.v1_18_R1.inventory.WildInventory;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.core.BlockPosition;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.entity.Entity;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.inventory.Container;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.item.ItemStack;
import com.bgsoftware.wildchests.nms.v1_18_R1.mappings.net.minecraft.world.level.World;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;

@SuppressWarnings({"unused"})
public final class NMSInventory implements com.bgsoftware.wildchests.nms.NMSInventory {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    @Remap(classPath = "net.minecraft.world.level.chunk.LevelChunk", name = "createTicker", type = Remap.Type.METHOD)
    private static final ReflectMethod<TickingBlockEntity> CREATE_TICKING_BLOCK = new ReflectMethod<>(
            Chunk.class, "a", net.minecraft.world.level.block.entity.TileEntity.class, BlockEntityTicker.class);

    @Override
    public void updateTileEntity(Chest chest) {
        Location location = chest.getLocation();
        World world = new World(((CraftWorld) location.getWorld()).getHandle());
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        net.minecraft.world.level.block.entity.TileEntity tileEntity = world.getBlockEntityNoMappings(blockPosition.getHandle());

        TileEntityWildChest tileEntityWildChest;

        if (tileEntity instanceof TileEntityWildChest) {
            tileEntityWildChest = (TileEntityWildChest) tileEntity;
            ((WChest) chest).setTileEntityContainer(tileEntityWildChest);
        } else {
            tileEntityWildChest = new TileEntityWildChest(chest, world, blockPosition);
            world.removeBlockEntity(blockPosition.getHandle());
            world.setBlockEntity(tileEntityWildChest);
            Chunk chunk = world.getChunkAt(blockPosition.getHandle());
            world.addBlockEntityTicker(CREATE_TICKING_BLOCK.invoke(chunk, tileEntityWildChest, tileEntityWildChest));
        }
    }

    @Override
    public void removeTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = new World(((CraftWorld) loc.getWorld()).getHandle());
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        net.minecraft.world.level.block.entity.TileEntity currentTileEntity = world.getBlockEntityNoMappings(blockPosition.getHandle());
        if (currentTileEntity instanceof TileEntityWildChest)
            world.removeBlockEntity(blockPosition.getHandle());
    }

    @Override
    public WildItemStack<?, ?> createItemStack(org.bukkit.inventory.ItemStack itemStack) {
        net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
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
        Entity entityPlayer = new Entity(((CraftPlayer) player).getHandle());

        String title = inventory.getTitle();

        Container container = createContainer(entityPlayer.nextContainerCounter(), entityPlayer.getInventory(), inventory);

        container.setTitle(CraftChatMessage.fromStringOrNull(title));

        // Cursor item is not updated, so we need to update it manually
        org.bukkit.inventory.ItemStack cursorItem = player.getItemOnCursor();

        entityPlayer.getPlayerConnection().send(new PacketPlayOutOpenWindow(container.getSyncId(), container.getType(), container.getTitle()));
        entityPlayer.setContainerMenu(container.getHandle());
        entityPlayer.initMenu(container.getHandle());

        player.setItemOnCursor(cursorItem);
    }

    @Override
    public void createDesignItem(com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory,
                                 org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = new ItemStack(CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE) :
                itemStack.clone()));

        designItem.setCount(1);
        designItem.addTagElement("DesignItem", NBTTagByte.a(true));

        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        inventory.setItem(0, designItem.getHandle(), false);
        inventory.setItem(1, designItem.getHandle(), false);
        inventory.setItem(3, designItem.getHandle(), false);
        inventory.setItem(4, designItem.getHandle(), false);
    }

    public static Container createContainer(int id, PlayerInventory playerInventory,
                                                                          com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory) {
        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        return new Container(inventory.b() == 5 ? WildContainerHopper.of(id, playerInventory, inventory) :
                WildContainerChest.of(id, playerInventory, inventory));
    }

}
