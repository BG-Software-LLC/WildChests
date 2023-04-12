package com.bgsoftware.wildchests.nms.v1_16_R3.inventory;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.nms.v1_16_R3.NMSInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import com.bgsoftware.wildchests.utils.ChestUtils;
import com.bgsoftware.wildchests.utils.Counter;
import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.Container;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityItem;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.IChatBaseComponent;
import net.minecraft.server.v1_16_R3.ITickable;
import net.minecraft.server.v1_16_R3.IWorldInventory;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NonNullList;
import net.minecraft.server.v1_16_R3.PlayerInventory;
import net.minecraft.server.v1_16_R3.SoundEffects;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.TileEntityChest;
import net.minecraft.server.v1_16_R3.World;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_16_R3.CraftParticle;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_16_R3.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;

import java.util.List;
import java.util.Map;

public class TileEntityWildChest extends TileEntityChest implements IWorldInventory, TileEntityContainer, ITickable {

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
    protected NonNullList<ItemStack> f() {
        return getContents();
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
    public NonNullList<ItemStack> getContents() {
        List<WildContainerItem> contents = ((WChest) chest).getWildContents();
        NonNullList<ItemStack> nonNullList = NonNullList.a(contents.size(), ItemStack.b);
        int index = 0;
        for (WildContainerItem wildContainerItem : contents)
            nonNullList.set(index++, ((WildContainerItemImpl) wildContainerItem).getHandle());
        return nonNullList;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbttagcompound) {
        return tileEntityChest.save(nbttagcompound);
    }

    @Override
    public NBTTagCompound b() {
        return save(new NBTTagCompound());
    }

    @Override
    public Container createContainer(int id, PlayerInventory playerinventory) {
        Container container = NMSInventory.createContainer(id, playerinventory, (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
        startOpen(playerinventory.player);
        return container;
    }

    @Override
    public IChatBaseComponent getScoreboardDisplayName() {
        return new ChatComponentText(((com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0)).getTitle());
    }

    @Override
    public final void closeContainer(EntityHuman entityHuman) {
        CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

        this.transaction.remove(craftHumanEntity);

        if (!craftHumanEntity.getHandle().isSpectator()) {
            int oldPower = Math.max(0, Math.min(15, this.viewingCount));
            this.viewingCount--;

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.world, this.position, oldPower, newPower);
                }
            }

            super.onOpen();
            if (viewingCount <= 0)
                playOpenSound(SoundEffects.BLOCK_CHEST_CLOSE);
        }
    }

    @Override
    public final void startOpen(EntityHuman entityHuman) {
        CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

        this.transaction.add(craftHumanEntity);

        if (!craftHumanEntity.getHandle().isSpectator()) {
            if (this.viewingCount < 0) {
                this.viewingCount = 0;
            }

            int oldPower = Math.max(0, Math.min(15, this.viewingCount));
            this.viewingCount++;

            if (this.world == null) {
                return;
            }

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.world, this.position, oldPower, newPower);
                }
            }

            super.onOpen();
            if (viewingCount == 1)
                playOpenSound(SoundEffects.BLOCK_CHEST_OPEN);
        }
    }

    @Override
    public final void onOpen(CraftHumanEntity who) {

    }

    @Override
    public final void onClose(CraftHumanEntity who) {

    }

    @Override
    public void tick() {
        super.tick();
        ChestData chestData = chest.getData();
        assert world != null;

        {
            double x = position.getX() + world.random.nextFloat();
            double y = position.getY() + world.random.nextFloat();
            double z = position.getZ() + world.random.nextFloat();
            for (String particle : chestData.getChestParticles()) {
                try {
                    ((WorldServer) world).sendParticles(null, CraftParticle.toNMS(Particle.valueOf(particle)),
                            x, y, z, 0, 0.0, 0.0, 0.0, 1.0, false);
                } catch (Exception ignored) {
                }
            }
        }

        if (--currentCooldown >= 0)
            return;

        Block currentBlock = world.getType(position).getBlock();

        if (((WChest) chest).isRemoved() || (currentBlock != Blocks.CHEST && currentBlock != Blocks.TRAPPED_CHEST)) {
            world.removeTileEntity(position);
            return;
        }

        currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

        if (suctionItems != null) {
            for (EntityItem entityItem : world.a(EntityItem.class, suctionItems, entityItem ->
                    ChestUtils.SUCTION_PREDICATE.test((CraftItem) entityItem.getBukkitEntity(), chestData))) {
                org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(entityItem.getItemStack());
                Item item = (Item) entityItem.getBukkitEntity();

                org.bukkit.inventory.ItemStack[] itemsToAdd = ChestUtils.fixItemStackAmount(
                        itemStack, plugin.getProviders().getItemAmount(item));

                Map<Integer, org.bukkit.inventory.ItemStack> leftOvers = chest.addItems(itemsToAdd);

                if (leftOvers.isEmpty()) {
                    ((WorldServer) world).sendParticles(null, CraftParticle.toNMS(Particle.CLOUD), entityItem.locX(), entityItem.locY(),
                            entityItem.locZ(), 0, 0.0, 0.0, 0.0, 1.0, false);
                    entityItem.die();
                } else if (leftOvers.size() != itemsToAdd.length) {
                    Counter leftOverCount = new Counter();
                    leftOvers.values().forEach(leftOver -> leftOverCount.increase(leftOver.getAmount()));
                    plugin.getProviders().setItemAmount(item, leftOverCount.get());
                }
            }
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
        return chest.getPage(0).getSize() * chest.getPagesAmount();
    }

    @Override
    public int getViewingCount() {
        if (this.viewingCount < 0)
            this.viewingCount = 0;

        return viewingCount;
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
        return slot != -2 || !(chest instanceof StorageChest) ? super.splitStack(slot, amount) :
                ((WildContainerItemImpl) ((WStorageChest) chest).splitItem(amount)).getHandle();
    }

    @Override
    public void update() {
        super.update();
        if (chest instanceof StorageChest)
            ((StorageChest) chest).update();
    }

    private void updateTile(TileEntity tileEntity, World world, BlockPosition blockPosition) {
        tileEntity.setLocation(world, blockPosition);
    }

}

