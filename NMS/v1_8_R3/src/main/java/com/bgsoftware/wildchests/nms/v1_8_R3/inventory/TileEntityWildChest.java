package com.bgsoftware.wildchests.nms.v1_8_R3.inventory;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.nms.v1_8_R3.NMSInventoryImpl;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import com.bgsoftware.wildchests.utils.ChestUtils;
import com.google.common.base.Predicate;
import net.minecraft.server.v1_8_R3.AxisAlignedBB;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.Container;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityItem;
import net.minecraft.server.v1_8_R3.EnumDirection;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IUpdatePlayerListBox;
import net.minecraft.server.v1_8_R3.IWorldInventory;
import net.minecraft.server.v1_8_R3.ItemStack;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PlayerInventory;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.TileEntityChest;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;

import java.util.AbstractSequentialList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TileEntityWildChest extends TileEntityChest implements IWorldInventory, TileEntityContainer, IUpdatePlayerListBox {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private final TileEntityChest tileEntityChest = new TileEntityChest();
    private final Chest chest;
    private final boolean isTrappedChest;

    private short currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

    private AxisAlignedBB suctionItems = null;
    private boolean autoCraftMode = false;
    private boolean autoSellMode = false;

    public TileEntityWildChest(Chest chest, World world, BlockPosition blockPosition) {
        this.chest = chest;
        this.world = world;
        updateTile(this, world, blockPosition);
        updateTile(tileEntityChest, world, blockPosition);
        isTrappedChest = world.getType(blockPosition).getBlock() == Blocks.TRAPPED_CHEST;
        ((WChest) chest).setTileEntityContainer(this);
        updateData();
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        ((WChest) chest).setItem(i, new WildContainerItemImpl(itemStack));
    }

    @Override
    public ItemStack getItem(int i) {
        return ((WildContainerItemImpl) ((WChest) chest).getWildItem(i)).getHandle();
    }

    @Override
    public ItemStack[] getContents() {
        List<WildContainerItem> contents = ((WChest) chest).getWildContents();
        ItemStack[] newContents = new ItemStack[contents.size()];

        for (int i = 0; i < newContents.length; i++)
            newContents[i] = ((WildContainerItemImpl) contents.get(i)).getHandle();

        return newContents;
    }

    @Override
    public void b(NBTTagCompound nbttagcompound) {
        tileEntityChest.b(nbttagcompound);
    }

    @Override
    public String getContainerName() {
        return chest instanceof StorageChest ? "minecraft:hopper" : super.getContainerName();
    }

    @Override
    public Container createContainer(PlayerInventory playerinventory, EntityHuman entityHuman) {
        Container container = NMSInventoryImpl.createContainer(playerinventory, entityHuman, (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
        startOpen(playerinventory.player);
        return container;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatComponentText(chest.getPage(0).getTitle());
    }

    @Override
    public final void closeContainer(EntityHuman entityHuman) {
        CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

        this.l = (int) this.transaction.stream().filter(human -> human.getGameMode() != GameMode.SPECTATOR).count();
        this.transaction.remove(craftHumanEntity);

        if (!craftHumanEntity.getHandle().isSpectator()) {
            int oldPower = Math.max(0, Math.min(15, this.l));
            this.l--;

            this.world.playBlockAction(this.position, this.getBlock(), 1, this.l);

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.l));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.world, this.position.getX(), this.position.getY(), this.position.getZ(), oldPower, newPower);
                }
            }

            this.world.applyPhysics(this.position, this.getBlock());
            this.world.applyPhysics(this.position.down(), this.getBlock());

            if (l <= 0)
                playOpenSound("random.chestclosed");
        }
    }

    @Override
    public final void startOpen(EntityHuman entityHuman) {
        CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

        this.transaction.add(craftHumanEntity);

        if (!craftHumanEntity.getHandle().isSpectator()) {
            if (this.l < 0) {
                this.l = 0;
            }

            int oldPower = Math.max(0, Math.min(15, this.l));
            this.l++;
            if (this.world == null) {
                return;
            }

            this.world.playBlockAction(this.position, this.getBlock(), 1, this.l);

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.l));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.world, this.position.getX(), this.position.getY(), this.position.getZ(), oldPower, newPower);
                }
            }

            this.world.applyPhysics(this.position, this.getBlock());
            this.world.applyPhysics(this.position.down(), this.getBlock());

            if (l == 1)
                playOpenSound("random.chestopen");
        }
    }

    @Override
    public final void onOpen(CraftHumanEntity who) {

    }

    @Override
    public final void onClose(CraftHumanEntity who) {

    }

    @Override
    public void c() {
        super.c();

        ChestData chestData = chest.getData();

        {
            double x = position.getX() + world.random.nextFloat();
            double y = position.getY() + world.random.nextFloat();
            double z = position.getZ() + world.random.nextFloat();
            for (String particle : chestData.getChestParticles()) {
                try {
                    ((WorldServer) world).sendParticles(null, EnumParticle.valueOf(particle),
                            false, x, y, z, 0, 0.0, 0.0, 0.0, 1.0);
                } catch (Exception ignored) {
                }
            }
        }

        if (--currentCooldown >= 0)
            return;

        Block currentBlock = world.getType(position).getBlock();

        if (((WChest) chest).isRemoved() || (currentBlock != Blocks.CHEST && currentBlock != Blocks.TRAPPED_CHEST)) {
            world.t(position);
            return;
        }

        currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

        if (suctionItems != null) {
            handleSuctionItems(chestData);
        }

        if (autoCraftMode) {
            ChestUtils.tryCraftChest(chest);
        }

        if (autoSellMode) {
            ChestUtils.trySellChest(chest);
        }
    }

    @Override
    public int getSize() {
        return chest.getPage(0).getSize();
    }

    @Override
    public int getViewingCount() {
        if (this.l < 0)
            this.l = 0;

        return l;
    }

    @Override
    public List<HumanEntity> getTransaction() {
        return transaction;
    }

    @Override
    public void updateData() {
        ChestData chestData = chest.getData();
        suctionItems = !chestData.isAutoSuction() ? null : new AxisAlignedBB(
                chestData.isAutoSuctionChunk() ? position.getX() >> 4 << 4 : position.getX() - chestData.getAutoSuctionRange(),
                position.getY() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? position.getZ() >> 4 << 4 : position.getZ() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (position.getX() >> 4 << 4) + 16 : position.getX() + chestData.getAutoSuctionRange(),
                position.getY() + chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (position.getZ() >> 4 << 4) + 16 : position.getZ() + chestData.getAutoSuctionRange()
        );
        autoCraftMode = chestData.isAutoCrafter();
        autoSellMode = chestData.isSellMode();
    }

    @Override
    public int[] getSlotsForFace(EnumDirection enumDirection) {
        return chest.getSlotsForFace();
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, EnumDirection enumDirection) {
        return chest.canPlaceItemThroughFace(CraftItemStack.asCraftMirror(itemStack));
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, EnumDirection enumDirection) {
        return chest.canTakeItemThroughFace(i, CraftItemStack.asCraftMirror(itemStack));
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        return slot != -2 || !(chest instanceof StorageChest) ? splitItem(slot, amount) :
                ((WildContainerItemImpl) ((WStorageChest) chest).splitItem(amount)).getHandle();
    }

    @Override
    public void update() {
        super.update();
        if (chest instanceof StorageChest)
            ((StorageChest) chest).update();
    }

    private void updateTile(TileEntity tileEntity, World world, BlockPosition blockPosition) {
        tileEntity.a(world);
        tileEntity.a(blockPosition);
    }

    private Block getBlock() {
        return world.getType(position).getBlock();
    }

    private ItemStack splitItem(int i, int amount) {
        Inventory firstPage = chest.getPage(0);

        if (firstPage == null)
            return null;

        int pageSize = firstPage.getSize();
        int page = i / pageSize;
        int slot = i % pageSize;

        CraftWildInventory actualPage = (CraftWildInventory) chest.getPage(page);

        if (actualPage == null)
            return null;

        return actualPage.getInventory().splitStack(slot, amount);
    }

    private void playOpenSound(String sound) {
        double d0 = (double) this.position.getX() + 0.5D;
        double d1 = (double) this.position.getY() + 0.5D;
        double d2 = (double) this.position.getZ() + 0.5D;
        this.world.makeSound(d0, d1, d2, sound, 0.5F, this.world.random.nextFloat() * 0.1F + 0.9F);
    }

    private void handleSuctionItems(ChestData chestData) {
        List<EntityItem> nearbyItems = world.a(EntityItem.class, suctionItems, (Predicate<? super EntityItem>) entity ->
                entity != null && ChestUtils.SUCTION_PREDICATE.test((CraftItem) entity.getBukkitEntity(), chestData));

        if (nearbyItems.isEmpty())
            return;

        if (!(nearbyItems instanceof AbstractSequentialList))
            nearbyItems = new LinkedList<>(nearbyItems);

        org.bukkit.inventory.ItemStack[] suctionItems = new org.bukkit.inventory.ItemStack[nearbyItems.size()];

        int itemIndex = 0;
        for (EntityItem itemEntity : nearbyItems) {
            org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(itemEntity.getItemStack());
            Item item = (Item) itemEntity.getBukkitEntity();

            int actualItemCount = plugin.getProviders().getItemAmount(item);
            if (actualItemCount != itemStack.getAmount()) {
                itemStack = itemStack.clone();
                itemStack.setAmount(actualItemCount);
            }

            suctionItems[itemIndex++] = itemStack;
        }

        Map<Integer, org.bukkit.inventory.ItemStack> leftOvers = chest.addItems(suctionItems);

        if (leftOvers.isEmpty()) {
            // We want to remove all entities.
            nearbyItems.forEach(this::handleItemSuctionRemoval);
            return;
        }

        itemIndex = 0;
        for (EntityItem nearbyItem : nearbyItems) {
            if (nearbyItem.dead) {
                continue;
            }

            org.bukkit.inventory.ItemStack leftOverItem = leftOvers.get(itemIndex++);

            if (leftOverItem == null) {
                handleItemSuctionRemoval(nearbyItem);
            } else {
                Item item = (Item) nearbyItem.getBukkitEntity();
                plugin.getProviders().setItemAmount(item, leftOverItem.getAmount());
            }
        }
    }

    private void handleItemSuctionRemoval(EntityItem entityItem) {
        ((WorldServer) world).sendParticles(null, EnumParticle.CLOUD, false,
                entityItem.locX, entityItem.locY, entityItem.locZ, 0, 0.0, 0.0, 0.0, 1.0);
        entityItem.die();
    }

}

