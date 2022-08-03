package com.bgsoftware.wildchests.nms.v1_17_R1.inventory;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.nms.v1_17_R1.NMSInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import com.bgsoftware.wildchests.utils.ChestUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_17_R1.CraftParticle;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_17_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;

import java.util.List;

public class TileEntityWildChest extends TileEntityChest implements IWorldInventory, TileEntityContainer, BlockEntityTicker<TileEntityWildChest> {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private final TileEntityChest tileEntityChest;
    private final Chest chest;
    private final boolean isTrappedChest;

    private short currentCooldown = ChestUtils.DEFAULT_COOLDOWN;
    private int viewingCount = 0;

    private AxisAlignedBB suctionItems = null;
    private boolean autoCraftMode = false;
    private boolean autoSellMode = false;

    public TileEntityWildChest(Chest chest, World world, BlockPosition blockPosition) {
        super(blockPosition, world.getType(blockPosition));
        this.chest = chest;
        this.n = world;
        this.tileEntityChest = new TileEntityChest(blockPosition, world.getType(blockPosition));
        isTrappedChest = world.getType(blockPosition).getBlock() == Blocks.fE;
        ((WChest) chest).setTileEntityContainer(this);
        updateData();
    }

    @Override
    protected NonNullList<ItemStack> f() {
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
        NonNullList<ItemStack> nonNullList = NonNullList.a(contents.length, ItemStack.b);

        for (int i = 0; i < contents.length; i++)
            nonNullList.set(i, (ItemStack) contents[i].getItemStack());

        return nonNullList;
    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbttagcompound) {
        return tileEntityChest.save(nbttagcompound);
    }

    @Override
    public Container createContainer(int id, PlayerInventory playerinventory) {
        Container container = NMSInventory.createContainer(id, playerinventory, (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
        startOpen(playerinventory.l);
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
                    CraftEventFactory.callRedstoneChange(this.n, this.o, oldPower, newPower);
                }
            }

            onOpen();
            if (viewingCount <= 0)
                playOpenSound(SoundEffects.cL);
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

            if (this.n == null) {
                return;
            }

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(this.n, this.o, oldPower, newPower);
                }
            }

            onOpen();
            if (viewingCount == 1)
                playOpenSound(SoundEffects.cN);
        }
    }

    @Override
    public final void onOpen(CraftHumanEntity who) {

    }

    @Override
    public final void onClose(CraftHumanEntity who) {

    }

    @Override
    public void tick(World world, BlockPosition blockPosition, IBlockData iBlockData, TileEntityWildChest tileEntity) {
        ChestData chestData = chest.getData();
        assert this.n != null;

        {
            double x = this.o.getX() + this.n.w.nextFloat();
            double y = this.o.getY() + this.n.w.nextFloat();
            double z = this.o.getZ() + this.n.w.nextFloat();
            for (String particle : chestData.getChestParticles()) {
                try {
                    ((WorldServer) this.n).sendParticles(null, CraftParticle.toNMS(Particle.valueOf(particle)),
                            x, y, z, 0, 0.0, 0.0, 0.0, 1.0, false);
                } catch (Exception ignored) {
                }
            }
        }

        if (--currentCooldown >= 0)
            return;

        Block currentBlock = this.n.getType(this.o).getBlock();

        if (((WChest) chest).isRemoved() || (currentBlock != Blocks.bX && currentBlock != Blocks.fE)) {
            this.n.removeTileEntity(this.o);
            return;
        }

        currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

        if (suctionItems != null) {
            for (EntityItem entityItem : this.n.a(EntityItem.class, suctionItems, entityItem ->
                    ChestUtils.SUCTION_PREDICATE.test((CraftItem) entityItem.getBukkitEntity(), chestData))) {
                org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(entityItem.getItemStack());
                Item item = (Item) entityItem.getBukkitEntity();

                itemStack.setAmount(plugin.getProviders().getItemAmount(item));

                org.bukkit.inventory.ItemStack remainingItem = ChestUtils.getRemainingItem(chest.addItems(itemStack));

                if (remainingItem == null) {
                    ((WorldServer) this.n).sendParticles(null, CraftParticle.toNMS(Particle.CLOUD), entityItem.locX(), entityItem.locY(),
                            entityItem.locZ(), 0, 0.0, 0.0, 0.0, 1.0, false);
                    entityItem.die();
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
    public int getSize() {
        return chest.getPage(0).getSize() * chest.getPagesAmount();
    }

    @Override
    public int getViewingCount() {
        this.f.getOpenerCount();
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
                chestData.isAutoSuctionChunk() ? this.o.getX() >> 4 << 4 : this.o.getX() - chestData.getAutoSuctionRange(),
                this.o.getY() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? this.o.getZ() >> 4 << 4 : this.o.getZ() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (this.o.getX() >> 4 << 4) + 16 : this.o.getX() + chestData.getAutoSuctionRange(),
                this.o.getY() + chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (this.o.getZ() >> 4 << 4) + 16 : this.o.getZ() + chestData.getAutoSuctionRange()
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
                (ItemStack) ((WStorageChest) chest).splitItem(amount).getItemStack();
    }

    @Override
    public void update() {
        super.update();
        if (chest instanceof StorageChest)
            ((StorageChest) chest).update();
    }

    private void playOpenSound(SoundEffect soundEffect) {
        playOpenSound(this.getWorld(), this.getPosition(), this.getBlock(), soundEffect);
    }

    private void onOpen() {
        Block block = this.getBlock().getBlock();
        if (block instanceof BlockChest) {
            if (!this.f.opened) {
                this.n.playBlockAction(this.o, block, 1, this.viewingCount);
            }

            this.n.applyPhysics(this.o, block);
        }
    }

}
