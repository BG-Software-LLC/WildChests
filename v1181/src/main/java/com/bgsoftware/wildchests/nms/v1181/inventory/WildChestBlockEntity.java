package com.bgsoftware.wildchests.nms.v1181.inventory;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.nms.v1181.NMSInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
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
import org.bukkit.craftbukkit.v1_18_R1.CraftParticle;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_18_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;

import java.util.List;

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
        ((WChest) chest).setItem(i, new WildItemStack<>(itemStack, CraftItemStack.asCraftMirror(itemStack)));
    }

    @Override
    public ItemStack getItem(int i) {
        return (ItemStack) ((WChest) chest).getWildItem(i).getItemStack();
    }


    @Override
    public NonNullList<ItemStack> getContents() {
        WildItemStack<?, ?>[] contents = ((WChest) chest).getWildContents();
        NonNullList<ItemStack> nonNullList = NonNullList.createWithCapacity(contents.length);

        for (int i = 0; i < contents.length; i++)
            nonNullList.set(i, (ItemStack) contents[i].getItemStack());

        return nonNullList;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        TILE_ENTITY_SAVE.invoke(chestBlockEntity, compoundTag);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        AbstractContainerMenu containerMenu = NMSInventory.createMenu(id, playerInventory,
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
            for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, suctionItems, entityItem ->
                    ChestUtils.SUCTION_PREDICATE.test((CraftItem) entityItem.getBukkitEntity(), chestData))) {
                org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(itemEntity.getItem());
                Item item = (Item) itemEntity.getBukkitEntity();

                itemStack.setAmount(plugin.getProviders().getItemAmount(item));

                org.bukkit.inventory.ItemStack remainingItem = ChestUtils.getRemainingItem(chest.addItems(itemStack));

                if (remainingItem == null) {
                    this.serverLevel.sendParticles(null, CraftParticle.toNMS(Particle.CLOUD),
                            itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                            0, 0.0, 0.0, 0.0, 1.0, false);
                    itemEntity.discard();
                } else {
                    plugin.getProviders().setItemAmount(item, remainingItem.getAmount());
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
                (ItemStack) ((WStorageChest) chest).splitItem(amount).getItemStack();
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

}