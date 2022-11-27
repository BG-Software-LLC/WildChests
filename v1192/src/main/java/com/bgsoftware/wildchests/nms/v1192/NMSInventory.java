package com.bgsoftware.wildchests.nms.v1192;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.nms.v1192.inventory.CraftWildInventoryImpl;
import com.bgsoftware.wildchests.nms.v1192.inventory.WildChestBlockEntity;
import com.bgsoftware.wildchests.nms.v1192.inventory.WildChestMenu;
import com.bgsoftware.wildchests.nms.v1192.inventory.WildContainer;
import com.bgsoftware.wildchests.nms.v1192.inventory.WildContainerItemImpl;
import com.bgsoftware.wildchests.nms.v1192.inventory.WildHopperMenu;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ByteTag;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;

public final class NMSInventory implements com.bgsoftware.wildchests.nms.NMSInventory {

    private static final ReflectMethod<TickingBlockEntity> CREATE_TICKING_BLOCK = new ReflectMethod<>(
            LevelChunk.class, "a", BlockEntity.class, BlockEntityTicker.class);

    @Override
    public void updateTileEntity(Chest chest) {
        Location location = chest.getLocation();
        World bukkitWorld = location.getWorld();

        if (bukkitWorld == null)
            throw new IllegalArgumentException("Cannot update tile entity of chests in null world.");

        ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();
        BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);

        if (blockEntity instanceof WildChestBlockEntity wildChestBlockEntity) {
            ((WChest) chest).setTileEntityContainer(wildChestBlockEntity);
        } else {
            WildChestBlockEntity wildChestBlockEntity = new WildChestBlockEntity(chest, serverLevel, blockPos);
            serverLevel.removeBlockEntity(blockPos);
            serverLevel.setBlockEntity(wildChestBlockEntity);
            LevelChunk levelChunk = serverLevel.getChunkAt(blockPos);
            serverLevel.addBlockEntityTicker(CREATE_TICKING_BLOCK.invoke(levelChunk, wildChestBlockEntity, wildChestBlockEntity));
        }
    }

    @Override
    public void removeTileEntity(Chest chest) {
        Location location = chest.getLocation();
        World bukkitWorld = location.getWorld();

        if (bukkitWorld == null)
            throw new IllegalArgumentException("Cannot update tile entity of chests in null world.");

        ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();
        BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        if (blockEntity instanceof WildChestBlockEntity)
            serverLevel.removeBlockEntity(blockPos);
    }

    @Override
    public WildContainerItemImpl createItemStack(org.bukkit.inventory.ItemStack bukkitItem) {
        return new WildContainerItemImpl(CraftItemStack.asNMSCopy(bukkitItem));
    }

    @Override
    public CraftWildInventory createInventory(Chest chest, int size, String title, int index) {
        WildContainer wildContainer = new WildContainer(size, title, chest, index);

        if (chest instanceof StorageChest)
            wildContainer.setItemFunction = (slot, itemStack) -> chest.setItem(slot, CraftItemStack.asCraftMirror(itemStack));

        return new CraftWildInventoryImpl(wildContainer);
    }

    @Override
    public void openPage(Player player, CraftWildInventory inventory) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        String title = inventory.getTitle();

        AbstractContainerMenu containerMenu = createMenu(serverPlayer.nextContainerCounter(), serverPlayer.getInventory(), inventory);
        containerMenu.setTitle(CraftChatMessage.fromStringOrNull(title));

        // Cursor item is not updated, so we need to update it manually
        org.bukkit.inventory.ItemStack cursorItem = player.getItemOnCursor();

        ClientboundOpenScreenPacket openScreenPacket = new ClientboundOpenScreenPacket(containerMenu.containerId,
                containerMenu.getType(), containerMenu.getTitle());

        serverPlayer.connection.send(openScreenPacket);
        serverPlayer.containerMenu = containerMenu;
        serverPlayer.initMenu(containerMenu);

        player.setItemOnCursor(cursorItem);
    }

    @Override
    public void createDesignItem(CraftWildInventory craftWildInventory,
                                 org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE) : itemStack.clone());

        designItem.setCount(1);
        designItem.addTagElement("DesignItem", ByteTag.valueOf(true));

        WildContainer container = ((CraftWildInventoryImpl) craftWildInventory).getInventory();
        container.setItem(0, designItem, false);
        container.setItem(1, designItem, false);
        container.setItem(3, designItem, false);
        container.setItem(4, designItem, false);
    }

    public static AbstractContainerMenu createMenu(int id, Inventory playerInventory,
                                                   CraftWildInventory craftWildInventory) {
        WildContainer container = ((CraftWildInventoryImpl) craftWildInventory).getInventory();
        return container.getContainerSize() == 5 ? WildHopperMenu.of(id, playerInventory, container) :
                WildChestMenu.of(id, playerInventory, container);
    }

}
