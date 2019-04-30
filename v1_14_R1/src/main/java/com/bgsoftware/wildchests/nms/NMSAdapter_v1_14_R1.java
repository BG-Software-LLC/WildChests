package com.bgsoftware.wildchests.nms;

import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.ChatComponentText;
import net.minecraft.server.v1_14_R1.Container;
import net.minecraft.server.v1_14_R1.Containers;
import net.minecraft.server.v1_14_R1.EntityPlayer;
import net.minecraft.server.v1_14_R1.IInventory;
import net.minecraft.server.v1_14_R1.ItemStack;
import net.minecraft.server.v1_14_R1.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_14_R1.TileEntityChest;
import net.minecraft.server.v1_14_R1.TileEntityHopper;
import net.minecraft.server.v1_14_R1.World;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSAdapter_v1_14_R1 implements NMSAdapter {

    @Override
    public String getVersion() {
        return "v1_14_R1";
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(blockPosition);
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
                Class minecraftInventory = optionalClass.get();
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

}
