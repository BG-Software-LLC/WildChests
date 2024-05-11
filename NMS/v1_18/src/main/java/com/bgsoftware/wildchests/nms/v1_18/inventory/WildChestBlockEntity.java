package com.bgsoftware.wildchests.nms.v1_18.inventory;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.nms.v1_18.NMSInventoryImpl;
import com.bgsoftware.wildchests.nms.v1_18.utils.TransformingNonNullList;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.utils.ChestUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_18_R2.CraftParticle;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_18_R2.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R2.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;

import java.util.AbstractSequentialList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class WildChestBlockEntity extends ChestBlockEntity implements WorldlyContainer, TileEntityContainer,
        BlockEntityTicker<WildChestBlockEntity> {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static final ReflectMethod<Void> TILE_ENTITY_SAVE = new ReflectMethod<>(
            BlockEntity.class, "b", CompoundTag.class);

    private final ChestBlockEntity chestBlockEntity;
    private final Chest chest;
    private final ServerLevel serverLevel;
    private final BlockPos blockPos;
    private final boolean isTrappedChest;

    private short currentCooldown = ChestUtils.DEFAULT_COOLDOWN;
    private int viewingCount = 0;

    private AABB suctionItems = null;
    private boolean autoCraftMode = false;
    private boolean autoSellMode = false;

    public WildChestBlockEntity(Chest chest, ServerLevel serverLevel, BlockPos blockPos) {
        super(blockPos, serverLevel.getBlockState(blockPos));
        this.chest = chest;
        this.serverLevel = serverLevel;
        this.blockPos = blockPos;
        this.level = serverLevel;
        BlockState blockState = getBlockState();
        this.chestBlockEntity = new ChestBlockEntity(blockPos, blockState);
        isTrappedChest = blockState.getBlock() == Blocks.TRAPPED_CHEST;
        ((WChest) chest).setTileEntityContainer(this);
        updateData();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
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
        return TransformingNonNullList.transform(((WChest) chest).getWildContents(), ItemStack.EMPTY, WildContainerItemImpl::transform);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        TILE_ENTITY_SAVE.invoke(chestBlockEntity, compoundTag);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        AbstractContainerMenu containerMenu = NMSInventoryImpl.createMenu(id, playerInventory,
                (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
        startOpen(playerInventory.player);
        return containerMenu;
    }

    @Override
    public Component getDisplayName() {
        return CraftChatMessage.fromStringOrNull(((com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0)).getTitle());
    }

    @Override
    public final void stopOpen(Player player) {
        CraftHumanEntity craftHumanEntity = player.getBukkitEntity();

        this.transaction.remove(craftHumanEntity);

        if (!player.isSpectator()) {
            int oldPower = Math.max(0, Math.min(15, this.viewingCount));
            this.viewingCount--;

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.serverLevel, this.blockPos, oldPower, newPower);
                }
            }

            onOpen();
            if (viewingCount <= 0)
                playOpenSound(SoundEvents.CHEST_CLOSE);
        }
    }

    @Override
    public void startOpen(Player player) {
        CraftHumanEntity craftHumanEntity = player.getBukkitEntity();

        this.transaction.add(craftHumanEntity);

        if (!player.isSpectator()) {
            if (this.viewingCount < 0) {
                this.viewingCount = 0;
            }

            int oldPower = Math.max(0, Math.min(15, this.viewingCount));
            this.viewingCount++;

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.serverLevel, this.blockPos, oldPower, newPower);
                }
            }

            onOpen();
            if (viewingCount == 1)
                playOpenSound(SoundEvents.CHEST_OPEN);
        }
    }

    @Override
    public final void onOpen(CraftHumanEntity who) {
    }

    @Override
    public final void onClose(CraftHumanEntity who) {
    }

    @Override
    public void tick(Level level, BlockPos blockPos, BlockState blockState, WildChestBlockEntity blockEntity) {
        ChestData chestData = chest.getData();

        {
            double x = blockPos.getX() + level.getRandom().nextFloat();
            double y = blockPos.getY() + level.getRandom().nextFloat();
            double z = blockPos.getZ() + level.getRandom().nextFloat();
            for (String particle : chestData.getChestParticles()) {
                try {
                    this.serverLevel.sendParticles(null, CraftParticle.toNMS(Particle.valueOf(particle)),
                            x, y, z, 0, 0.0, 0.0, 0.0, 1.0, false);
                } catch (Exception ignored) {
                }
            }
        }

        if (--currentCooldown >= 0)
            return;

        Block currentBlock = level.getBlockState(blockPos).getBlock();

        if (((WChest) chest).isRemoved() || (currentBlock != Blocks.CHEST && currentBlock != Blocks.TRAPPED_CHEST)) {
            level.removeBlockEntity(blockPos);
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
    public int getContainerSize() {
        return chest.getPage(0).getSize() * chest.getPagesAmount();
    }

    @Override
    public int getViewingCount() {
        this.openersCounter.getOpenerCount();
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
        suctionItems = !chestData.isAutoSuction() ? null : new AABB(
                chestData.isAutoSuctionChunk() ? blockPos.getX() >> 4 << 4 : blockPos.getX() - chestData.getAutoSuctionRange(),
                blockPos.getY() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? blockPos.getZ() >> 4 << 4 : blockPos.getZ() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (blockPos.getX() >> 4 << 4) + 16 : blockPos.getX() + chestData.getAutoSuctionRange(),
                blockPos.getY() + chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (blockPos.getZ() >> 4 << 4) + 16 : blockPos.getZ() + chestData.getAutoSuctionRange()
        );
        autoCraftMode = chestData.isAutoCrafter();
        autoSellMode = chestData.isSellMode();
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return chest.getSlotsForFace();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return chest.canPlaceItemThroughFace(CraftItemStack.asCraftMirror(itemStack));
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return chest.canTakeItemThroughFace(slot, CraftItemStack.asCraftMirror(itemStack));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return slot != -2 || !(chest instanceof StorageChest) ? super.removeItem(slot, amount) :
                ((WildContainerItemImpl) ((WStorageChest) chest).splitItem(amount)).getHandle();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (chest instanceof StorageChest)
            ((StorageChest) chest).update();
    }

    private void playOpenSound(SoundEvent soundEvent) {
        ChestBlockEntity.playSound(this.serverLevel, this.blockPos, getBlockState(), soundEvent);
    }

    private void onOpen() {
        Block block = getBlockState().getBlock();
        if (block instanceof ChestBlock) {
            if (!this.openersCounter.opened) {
                this.serverLevel.blockEvent(this.blockPos, block, 1, this.viewingCount);
            }

            this.serverLevel.updateNeighborsAt(this.blockPos, block);
        }
    }

    private void handleSuctionItems(ChestData chestData) {
        List<ItemEntity> nearbyItems = level.getEntitiesOfClass(ItemEntity.class, suctionItems, entityItem ->
                ChestUtils.SUCTION_PREDICATE.test((CraftItem) entityItem.getBukkitEntity(), chestData));

        if (nearbyItems.isEmpty())
            return;

        if (!(nearbyItems instanceof AbstractSequentialList))
            nearbyItems = new LinkedList<>(nearbyItems);

        org.bukkit.inventory.ItemStack[] suctionItems = new org.bukkit.inventory.ItemStack[nearbyItems.size()];

        int itemIndex = 0;
        for (ItemEntity itemEntity : nearbyItems) {
            org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(itemEntity.getItem());
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
        for (ItemEntity nearbyItem : nearbyItems) {
            if (nearbyItem.isRemoved()) {
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

    private void handleItemSuctionRemoval(ItemEntity itemEntity) {
        this.serverLevel.sendParticles(null, CraftParticle.toNMS(Particle.CLOUD),
                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                0, 0.0, 0.0, 0.0, 1.0, false);
        itemEntity.discard();
    }

}