package com.bgsoftware.wildchests.nms;

import net.minecraft.server.v1_8_R2.BlockPosition;
import net.minecraft.server.v1_8_R2.ChatComponentText;
import net.minecraft.server.v1_8_R2.Container;
import net.minecraft.server.v1_8_R2.EntityPlayer;
import net.minecraft.server.v1_8_R2.IInventory;
import net.minecraft.server.v1_8_R2.ItemStack;
import net.minecraft.server.v1_8_R2.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_8_R2.TileEntityChest;
import net.minecraft.server.v1_8_R2.TileEntityHopper;
import net.minecraft.server.v1_8_R2.World;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftContainer;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("unused")
public final class NMSAdapter_v1_8_R2 implements NMSAdapter {

    @Override
    public String getVersion() {
        return "v1_8_R2";
    }

    @Override
    public void playChestAction(Location location, boolean open) {
        World world = ((CraftWorld) location.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(location.getX(), location.getY(), location.getZ());
        TileEntityChest tileChest = (TileEntityChest) world.getTileEntity(blockPosition);
        world.playBlockAction(blockPosition, tileChest.w(), 1, open ? 1 : 0);
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

        Container container;

        try{
            container = new CraftContainer(inventory, player, entityPlayer.nextContainerCounter());
        }catch(Throwable th){
            try{
                container = (Container) CraftContainer.class.getConstructors()[1].newInstance(inventory, entityPlayer, entityPlayer.nextContainerCounter());
            }catch(Exception ex){
                ex.printStackTrace();
                return;
            }
        }

        String title = container.getBukkitView().getTitle();
        int size = container.getBukkitView().getTopInventory().getSize();
        entityPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(container.windowId, "minecraft:hopper", new ChatComponentText(title), size));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }

    @Override
    public void setDesignItem(org.bukkit.inventory.ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        itemMeta.setDisplayName(ChatColor.RESET + nmsItem.getName());
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
                tileEntityHopper.a(title);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }


}
