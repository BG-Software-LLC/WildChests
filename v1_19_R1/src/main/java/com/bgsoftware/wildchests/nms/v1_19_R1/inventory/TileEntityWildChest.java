package com.bgsoftware.wildchests.nms.v1_19_R1.inventory;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.common.remaps.Remap;
import com.bgsoftware.wildchests.nms.v1_19_R1.NMSInventory;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.core.BlockPosition;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.entity.Entity;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.inventory.Container;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.World;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.block.entity.TileEntity;
import com.bgsoftware.wildchests.nms.v1_19_R1.mappings.net.minecraft.world.level.block.state.IBlockData;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import com.bgsoftware.wildchests.utils.ChestUtils;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_19_R1.CraftParticle;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;

import java.util.List;

public class TileEntityWildChest extends TileEntityChest implements IWorldInventory, TileEntityContainer,
        BlockEntityTicker<TileEntityWildChest> {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    @Remap(classPath = "net.minecraft.world.level.block.entity.BlockEntity", name = "saveAdditional", type = Remap.Type.METHOD)
    private static final ReflectMethod<Void> TILE_ENTITY_SAVE = new ReflectMethod<>(
            net.minecraft.world.level.block.entity.TileEntity.class, "b", NBTTagCompound.class);
    @Remap(classPath = "net.minecraft.world.level.block.Blocks", name = "TRAPPED_CHEST", type = Remap.Type.FIELD, remappedName = "fW")
    private static final Block TRAPPED_CHEST_BLOCK = Blocks.fW;
    @Remap(classPath = "net.minecraft.world.level.block.Blocks", name = "CHEST", type = Remap.Type.FIELD, remappedName = "cg")
    private static final Block CHEST_BLOCK = Blocks.cg;
    @Remap(classPath = "net.minecraft.sounds.SoundEvents", name = "CHEST_CLOSE", type = Remap.Type.FIELD, remappedName = "cX")
    private static final SoundEffect CHEST_CLOSE_SOUND = SoundEffects.cX;
    @Remap(classPath = "net.minecraft.sounds.SoundEvents", name = "CHEST_OPEN", type = Remap.Type.FIELD, remappedName = "cZ")
    private static final SoundEffect CHEST_OPEN_SOUND = SoundEffects.cZ;

    private final TileEntityChest tileEntityChest;
    private final Chest chest;
    private final boolean isTrappedChest;

    private final World world;
    private final BlockPosition tilePosition;
    private final TileEntity tileEntity;

    private short currentCooldown = ChestUtils.DEFAULT_COOLDOWN;
    private int viewingCount = 0;

    private AxisAlignedBB suctionItems = null;
    private boolean autoCraftMode = false;
    private boolean autoSellMode = false;

    @Remap(classPath = "net.minecraft.world.level.block.entity.BlockEntity",
            name = "level",
            type = Remap.Type.FIELD,
            remappedName = "n")
    public TileEntityWildChest(Chest chest, World world, BlockPosition blockPosition) {
        super(blockPosition.getHandle(), world.getBlockStateNoMappings(blockPosition.getHandle()));
        this.chest = chest;
        this.world = world;
        this.tilePosition = blockPosition;
        this.tileEntity = new TileEntity(this);
        this.n = world.getHandle();
        IBlockData blockData = tileEntity.getBlockState();
        this.tileEntityChest = new TileEntityChest(blockPosition.getHandle(), blockData.getHandle());
        isTrappedChest = blockData.getBlock() == TRAPPED_CHEST_BLOCK;
        ((WChest) chest).setTileEntityContainer(this);
        updateData();
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity",
            name = "getItems",
            type = Remap.Type.METHOD,
            remappedName = "f")
    @Override
    protected NonNullList<ItemStack> f() {
        return getContents();
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity",
            name = "setItem",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Override
    public void a(int i, net.minecraft.world.item.ItemStack itemStack) {
        ((WChest) chest).setItem(i, new WildItemStack<>(itemStack, CraftItemStack.asCraftMirror(itemStack)));
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity",
            name = "getItem",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Override
    public net.minecraft.world.item.ItemStack a(int i) {
        return (net.minecraft.world.item.ItemStack) ((WChest) chest).getWildItem(i).getItemStack();
    }


    @Override
    public NonNullList<net.minecraft.world.item.ItemStack> getContents() {
        WildItemStack<?, ?>[] contents = ((WChest) chest).getWildContents();
        NonNullList<net.minecraft.world.item.ItemStack> nonNullList = NonNullList.a(contents.length,
                net.minecraft.world.item.ItemStack.b);

        for (int i = 0; i < contents.length; i++)
            nonNullList.set(i, (net.minecraft.world.item.ItemStack) contents[i].getItemStack());

        return nonNullList;
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.BlockEntity",
            name = "saveAdditional",
            type = Remap.Type.METHOD,
            remappedName = "b")
    @Override
    protected void b(NBTTagCompound nbtTagCompound) {
        TILE_ENTITY_SAVE.invoke(tileEntityChest, nbtTagCompound);
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.BaseContainerBlockEntity",
            name = "createMenu",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Remap(classPath = "net.minecraft.world.entity.player.Inventory",
            name = "player",
            type = Remap.Type.FIELD,
            remappedName = "l")
    @Override
    public net.minecraft.world.inventory.Container a(int id, PlayerInventory playerInventory) {
        Container container = NMSInventory.createContainer(id, playerInventory,
                (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
        startOpen(playerInventory.l);
        return container.getHandle();
    }

    @Remap(classPath = "net.minecraft.world.Nameable",
            name = "getDisplayName",
            type = Remap.Type.METHOD,
            remappedName = "C_")
    @Override
    public IChatBaseComponent C_() {
        return CraftChatMessage.fromStringOrNull(((com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0)).getTitle());
    }

    public void closeContainer(EntityHuman entityHuman) {
        this.c_(entityHuman);
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.ChestBlockEntity",
            name = "stopOpen",
            type = Remap.Type.METHOD,
            remappedName = "c_")
    @Override
    public final void c_(EntityHuman nmsEntityHuman) {
        CraftHumanEntity craftHumanEntity = nmsEntityHuman.getBukkitEntity();

        this.transaction.remove(craftHumanEntity);

        Entity entityHuman = new Entity(nmsEntityHuman);

        if (!entityHuman.isSpectator()) {
            int oldPower = Math.max(0, Math.min(15, this.viewingCount));
            this.viewingCount--;

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(world.getHandle(), tilePosition.getHandle(), oldPower, newPower);
                }
            }

            onOpen();
            if (viewingCount <= 0)
                playOpenSound(CHEST_CLOSE_SOUND);
        }
    }

    private void startOpen(EntityHuman entityHuman) {
        this.d_(entityHuman);
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.ChestBlockEntity",
            name = "startOpen",
            type = Remap.Type.METHOD,
            remappedName = "d_")
    @Override
    public final void d_(EntityHuman nmsEntityHuman) {
        CraftHumanEntity craftHumanEntity = nmsEntityHuman.getBukkitEntity();

        this.transaction.add(craftHumanEntity);

        Entity entityHuman = new Entity(nmsEntityHuman);

        if (!entityHuman.isSpectator()) {
            if (this.viewingCount < 0) {
                this.viewingCount = 0;
            }

            int oldPower = Math.max(0, Math.min(15, this.viewingCount));
            this.viewingCount++;

            if (isTrappedChest) {
                int newPower = Math.max(0, Math.min(15, this.viewingCount));
                if (oldPower != newPower) {
                    CraftEventFactory.callRedstoneChange(world.getHandle(), tilePosition.getHandle(), oldPower, newPower);
                }
            }

            onOpen();
            if (viewingCount == 1)
                playOpenSound(CHEST_OPEN_SOUND);
        }
    }

    @Override
    public final void onOpen(CraftHumanEntity who) {

    }

    @Override
    public final void onClose(CraftHumanEntity who) {

    }

    @Override
    public void tick(net.minecraft.world.level.World world,
                     net.minecraft.core.BlockPosition blockPosition,
                     net.minecraft.world.level.block.state.IBlockData iBlockData,
                     TileEntityWildChest tileEntity) {
        ChestData chestData = chest.getData();

        {
            double x = tilePosition.getX() + this.world.getRandom().nextFloat(); // RandomSource#nextFloat
            double y = tilePosition.getY() + this.world.getRandom().nextFloat(); // RandomSource#nextFloat
            double z = tilePosition.getZ() + this.world.getRandom().nextFloat(); // RandomSource#nextFloat
            for (String particle : chestData.getChestParticles()) {
                try {
                    this.world.sendParticles(null, CraftParticle.toNMS(Particle.valueOf(particle)),
                            x, y, z, 0, 0.0, 0.0, 0.0, 1.0, false);
                } catch (Exception ignored) {
                }
            }
        }

        if (--currentCooldown >= 0)
            return;

        Block currentBlock = this.world.getBlockState(tilePosition.getHandle()).getBlock();

        if (((WChest) chest).isRemoved() || (currentBlock != CHEST_BLOCK && currentBlock != TRAPPED_CHEST_BLOCK)) {
            this.world.removeBlockEntity(tilePosition.getHandle());
            return;
        }

        currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

        if (suctionItems != null) {
            for (EntityItem nmsEntityItem : this.world.getEntitiesOfClass(EntityItem.class, suctionItems, entityItem ->
                    ChestUtils.SUCTION_PREDICATE.test((CraftItem) entityItem.getBukkitEntity(), chestData))) {
                Entity entity = new Entity(nmsEntityItem);
                org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(entity.getItem());
                Item item = (Item) nmsEntityItem.getBukkitEntity();

                itemStack.setAmount(plugin.getProviders().getItemAmount(item));

                org.bukkit.inventory.ItemStack remainingItem = ChestUtils.getRemainingItem(chest.addItems(itemStack));

                if (remainingItem == null) {
                    this.world.sendParticles(null, CraftParticle.toNMS(Particle.CLOUD),
                            entity.getX(), entity.getY(), entity.getZ(),
                            0, 0.0, 0.0, 0.0, 1.0, false);
                    entity.discard();
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

    @Remap(classPath = "net.minecraft.world.level.block.entity.ChestBlockEntity",
            name = "getContainerSize",
            type = Remap.Type.METHOD,
            remappedName = "b")
    @Override
    public int b() {
        return chest.getPage(0).getSize() * chest.getPagesAmount();
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.ChestBlockEntity",
            name = "openersCounter",
            type = Remap.Type.FIELD,
            remappedName = "f")
    @Override
    public int getViewingCount() {
        this.f.a();
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
                chestData.isAutoSuctionChunk() ? tilePosition.getX() >> 4 << 4 : tilePosition.getX() - chestData.getAutoSuctionRange(),
                tilePosition.getY() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? tilePosition.getZ() >> 4 << 4 : tilePosition.getZ() - chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (tilePosition.getX() >> 4 << 4) + 16 : tilePosition.getX() + chestData.getAutoSuctionRange(),
                tilePosition.getY() + chestData.getAutoSuctionRange(),
                chestData.isAutoSuctionChunk() ? (tilePosition.getZ() >> 4 << 4) + 16 : tilePosition.getZ() + chestData.getAutoSuctionRange()
        );
        autoCraftMode = chestData.isAutoCrafter();
        autoSellMode = chestData.isSellMode();
    }

    @Remap(classPath = "net.minecraft.world.WorldlyContainer",
            name = "getSlotsForFace",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Override
    public int[] a(EnumDirection enumDirection) {
        return chest.getSlotsForFace();
    }

    @Remap(classPath = "net.minecraft.world.WorldlyContainer",
            name = "canPlaceItemThroughFace",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Override
    public boolean a(int i, net.minecraft.world.item.ItemStack itemStack, EnumDirection enumDirection) {
        return chest.canPlaceItemThroughFace(CraftItemStack.asCraftMirror(itemStack));
    }

    @Remap(classPath = "net.minecraft.world.WorldlyContainer",
            name = "canTakeItemThroughFace",
            type = Remap.Type.METHOD,
            remappedName = "b")
    @Override
    public boolean b(int i, net.minecraft.world.item.ItemStack itemStack, EnumDirection enumDirection) {
        return chest.canTakeItemThroughFace(i, CraftItemStack.asCraftMirror(itemStack));
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity",
            name = "removeItem",
            type = Remap.Type.METHOD,
            remappedName = "a")
    @Override
    public net.minecraft.world.item.ItemStack a(int slot, int amount) {
        return slot != -2 || !(chest instanceof StorageChest) ? super.a(slot, amount) :
                (net.minecraft.world.item.ItemStack) ((WStorageChest) chest).splitItem(amount).getItemStack();
    }

    @Remap(classPath = "net.minecraft.world.level.block.entity.BlockEntity",
            name = "setChanged",
            type = Remap.Type.METHOD,
            remappedName = "e")
    @Override
    public void e() {
        super.e();
        if (chest instanceof StorageChest)
            ((StorageChest) chest).update();
    }

    private void playOpenSound(SoundEffect soundEffect) {
        a(world.getHandle(), tilePosition.getHandle(), tileEntity.getBlockStateNoMappings(), soundEffect);
    }

    private void onOpen() {
        Block block = tileEntity.getBlockState().getBlock();
        if (block instanceof BlockChest) {
            if (!this.f.opened) {
                world.blockEvent(tilePosition.getHandle(), block, 1, this.viewingCount);
            }

            world.updateNeighborsAt(tilePosition.getHandle(), block);
        }
    }

}