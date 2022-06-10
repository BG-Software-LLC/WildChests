package com.bgsoftware.wildchests.nms;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import com.bgsoftware.wildchests.utils.ChestUtils;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.IInventory;
import net.minecraft.world.IWorldInventory;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerChest;
import net.minecraft.world.inventory.ContainerHopper;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockChest;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityChest;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.chunk.Chunk;
import net.minecraft.world.phys.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_19_R1.CraftParticle;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftChatMessage;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static com.bgsoftware.wildchests.nms.NMSMappings_v1_19_R1.*;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class NMSInventory_v1_19_R1 implements NMSInventory {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static final ReflectMethod<Void> TILE_ENTITY_SAVE = new ReflectMethod<>(TileEntity.class, "b", NBTTagCompound.class);
    private static final ReflectMethod<TickingBlockEntity> CREATE_TICKING_BLOCK = new ReflectMethod<>(
            Chunk.class, "a", TileEntity.class, BlockEntityTicker.class);

    @Override
    public void updateTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        TileEntity tileEntity = getBlockEntity(world, blockPosition);

        TileEntityWildChest tileEntityWildChest;

        if(tileEntity instanceof TileEntityWildChest) {
            tileEntityWildChest = (TileEntityWildChest) tileEntity;
            ((WChest) chest).setTileEntityContainer(tileEntityWildChest);
        }
        else {
            tileEntityWildChest = new TileEntityWildChest(chest, world, blockPosition);
            NMSMappings_v1_19_R1.removeBlockEntity(world, blockPosition);
            setBlockEntity(world, tileEntityWildChest);
            Chunk chunk = getChunkAt(world, blockPosition);
            world.a(CREATE_TICKING_BLOCK.invoke(chunk, tileEntityWildChest, tileEntityWildChest));
        }
    }

    @Override
    public void removeTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        TileEntity currentTileEntity = getBlockEntity(world, blockPosition);
        if(currentTileEntity instanceof TileEntityWildChest)
            NMSMappings_v1_19_R1.removeBlockEntity(world, blockPosition);
    }

    @Override
    public WildItemStack<?, ?> createItemStack(org.bukkit.inventory.ItemStack itemStack) {
        ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        return new WildItemStack<>(nmsItem, CraftItemStack.asCraftMirror(nmsItem));
    }

    @Override
    public com.bgsoftware.wildchests.objects.inventory.CraftWildInventory createInventory(Chest chest, int size, String title, int index) {
        WildInventory wildInventory = new WildInventory(size, title, chest, index);

        if(chest instanceof StorageChest)
            wildInventory.setItemFunction = (slot, itemStack) -> chest.setItem(slot, CraftItemStack.asCraftMirror(itemStack));

        return new CraftWildInventory(wildInventory);
    }

    @Override
    public void openPage(Player player, com.bgsoftware.wildchests.objects.inventory.CraftWildInventory inventory) {
        EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();

        String title = inventory.getTitle();

        Container container = createContainer(entityPlayer.nextContainerCounter(), getInventory(entityPlayer), inventory);

        container.setTitle(CraftChatMessage.fromStringOrNull(title));

        // Cursor item is not updated, so we need to update it manually
        org.bukkit.inventory.ItemStack cursorItem = player.getItemOnCursor();

        send(entityPlayer.b, new PacketPlayOutOpenWindow(container.j, getType(container), container.getTitle()));
        entityPlayer.bU = container;
        initMenu(entityPlayer, container);

        player.setItemOnCursor(cursorItem);
    }

    @Override
    public void createDesignItem(com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory, org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE) :
                itemStack.clone());

        setCount(designItem, 1);
        designItem.a("DesignItem", NBTTagByte.a(true));

        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        inventory.setItem(0, designItem, false);
        inventory.setItem(1, designItem, false);
        inventory.setItem(3, designItem, false);
        inventory.setItem(4, designItem, false);
    }

    private static Container createContainer(int id, PlayerInventory playerInventory, com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory){
        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        return inventory.b() == 5 ? WildContainerHopper.of(id, playerInventory, inventory) :
                WildContainerChest.of(id, playerInventory, inventory);
    }

    private static class TileEntityWildChest extends TileEntityChest implements IWorldInventory, TileEntityContainer, BlockEntityTicker<TileEntityWildChest> {

        private final TileEntityChest tileEntityChest;
        private final Chest chest;
        private final boolean isTrappedChest;

        private short currentCooldown = ChestUtils.DEFAULT_COOLDOWN;
        private int viewingCount = 0;

        private AxisAlignedBB suctionItems = null;
        private boolean autoCraftMode = false;
        private boolean autoSellMode = false;

        private TileEntityWildChest(Chest chest, World world, BlockPosition blockPosition){
            super(blockPosition, getBlockState(world, blockPosition));
            this.chest = chest;
            this.n = world;
            this.tileEntityChest = new TileEntityChest(blockPosition, NMSMappings_v1_19_R1.getBlockState(this));
            isTrappedChest = getBlock(NMSMappings_v1_19_R1.getBlockState(this)) == Blocks.fE;
            ((WChest) chest).setTileEntityContainer(this);
            updateData();
        }

        @Override
        protected NonNullList<ItemStack> f() {
            return getContents();
        }

        @Override
        public void a(int i, ItemStack itemStack) {
            ((WChest) chest).setItem(i, new WildItemStack<>(itemStack, CraftItemStack.asCraftMirror(itemStack)));
        }

        @Override
        public ItemStack a(int i) {
            return (ItemStack) ((WChest) chest).getWildItem(i).getItemStack();
        }

        @Override
        public NonNullList<ItemStack> getContents() {
            WildItemStack<?, ?>[] contents = ((WChest) chest).getWildContents();
            NonNullList<ItemStack> nonNullList = NonNullList.a(contents.length, ItemStack.b);

            for(int i = 0; i < contents.length; i++)
                nonNullList.set(i, (ItemStack) contents[i].getItemStack());

            return nonNullList;
        }

        @Override
        protected void b(NBTTagCompound nbtTagCompound) {
            TILE_ENTITY_SAVE.invoke(tileEntityChest, nbtTagCompound);
        }

        @Override
        public Container a(int id, PlayerInventory playerinventory) {
            Container container = NMSInventory_v1_19_R1.createContainer(id, playerinventory,
                    (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
            startOpen(playerinventory.l);
            return container;
        }

        @Override
        public IChatBaseComponent C_() {
            return CraftChatMessage.fromStringOrNull(((com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0)).getTitle());
        }

        private void closeContainer(EntityHuman entityHuman) {
            this.c_(entityHuman);
        }

        @Override
        public final void c_(EntityHuman entityHuman) {
            CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

            this.transaction.remove(craftHumanEntity);

            if (!isSpectator(entityHuman)) {
                int oldPower = Math.max(0, Math.min(15, this.viewingCount));
                this.viewingCount--;

                if (isTrappedChest) {
                    int newPower = Math.max(0, Math.min(15, this.viewingCount));
                    if (oldPower != newPower) {
                        CraftEventFactory.callRedstoneChange(this.n, this.o, oldPower, newPower);
                    }
                }

                onOpen();
                if(viewingCount <= 0)
                    playOpenSound(SoundEffects.cO);
            }
        }

        private void startOpen(EntityHuman entityHuman) {
            this.d_(entityHuman);
        }

        @Override
        public final void d_(EntityHuman entityHuman) {
            CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

            this.transaction.add(craftHumanEntity);

            if (!isSpectator(entityHuman)) {
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
                if(viewingCount == 1)
                    playOpenSound(SoundEffects.cQ);
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
                double x = getX(this.o) + this.n.r_().i(); // RandomSource#nextFloat
                double y = getY(this.o) + this.n.r_().i(); // RandomSource#nextFloat
                double z = getZ(this.o) + this.n.r_().i(); // RandomSource#nextFloat
                for(String particle : chestData.getChestParticles()) {
                    try {
                        ((WorldServer) this.n).sendParticles(null, CraftParticle.toNMS(Particle.valueOf(particle)),
                                x, y, z, 0, 0.0, 0.0, 0.0, 1.0, false);
                    }catch (Exception ignored){}
                }
            }

            if(--currentCooldown >= 0)
                return;

            Block currentBlock = getBlock(getBlockState(this.n, this.o));

            if(((WChest) chest).isRemoved() || (currentBlock != Blocks.bX && currentBlock != Blocks.fE)){
                NMSMappings_v1_19_R1.removeBlockEntity(this.n, this.o);
                return;
            }

            currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

            if(suctionItems != null) {
                for (EntityItem entityItem : this.n.a(EntityItem.class, suctionItems, entityItem ->
                        ChestUtils.SUCTION_PREDICATE.test((CraftItem) entityItem.getBukkitEntity(), chestData))) {
                    org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(getItem(entityItem));
                    Item item = (Item) entityItem.getBukkitEntity();

                    itemStack.setAmount(plugin.getProviders().getItemAmount(item));

                    org.bukkit.inventory.ItemStack remainingItem = ChestUtils.getRemainingItem(chest.addItems(itemStack));

                    if (remainingItem == null) {
                        ((WorldServer) this.n).sendParticles(null, CraftParticle.toNMS(Particle.CLOUD),
                                NMSMappings_v1_19_R1.getX(entityItem), NMSMappings_v1_19_R1.getY(entityItem), NMSMappings_v1_19_R1.getZ(entityItem),
                                0, 0.0, 0.0, 0.0, 1.0, false);
                        discard(entityItem);
                    } else {
                        plugin.getProviders().setItemAmount(item, remainingItem.getAmount());
                    }
                }
            }

            if(autoCraftMode){
                ChestUtils.tryCraftChest(chest);
            }

            if (autoSellMode){
                ChestUtils.trySellChest(chest);
            }
        }

        @Override
        public int b() {
            return chest.getPage(0).getSize() * chest.getPagesAmount();
        }

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
        public void updateData(){
            ChestData chestData = chest.getData();
            suctionItems = !chestData.isAutoSuction() ? null :  new AxisAlignedBB(
                    chestData.isAutoSuctionChunk() ? getX(this.o) >> 4 << 4 : getX(this.o) - chestData.getAutoSuctionRange(),
                    getY(this.o) - chestData.getAutoSuctionRange(),
                    chestData.isAutoSuctionChunk() ? getZ(this.o) >> 4 << 4 : getZ(this.o) - chestData.getAutoSuctionRange(),
                    chestData.isAutoSuctionChunk() ? (getX(this.o) >> 4 << 4) + 16 : getX(this.o) + chestData.getAutoSuctionRange(),
                    getY(this.o) + chestData.getAutoSuctionRange(),
                    chestData.isAutoSuctionChunk() ? (getZ(this.o) >> 4 << 4) + 16 : getZ(this.o) + chestData.getAutoSuctionRange()
            );
            autoCraftMode = chestData.isAutoCrafter();
            autoSellMode = chestData.isSellMode();
        }

        @Override
        public int[] a(EnumDirection enumDirection) {
            return chest.getSlotsForFace();
        }

        @Override
        public boolean a(int i, ItemStack itemStack, EnumDirection enumDirection) {
            return chest.canPlaceItemThroughFace(CraftItemStack.asCraftMirror(itemStack));
        }

        @Override
        public boolean b(int i, ItemStack itemStack, EnumDirection enumDirection) {
            return chest.canTakeItemThroughFace(i, CraftItemStack.asCraftMirror(itemStack));
        }

        @Override
        public ItemStack a(int slot, int amount) {
            return slot != -2 || !(chest instanceof StorageChest) ? super.a(slot, amount) :
                    (ItemStack) ((WStorageChest) chest).splitItem(amount).getItemStack();
        }

        @Override
        public void e() {
            super.e();
            if(chest instanceof StorageChest)
                ((StorageChest) chest).update();
        }

        private void playOpenSound(SoundEffect soundEffect){
            a(this.n, this.o, NMSMappings_v1_19_R1.getBlockState(this), soundEffect);
        }

        private void onOpen() {
            Block block = getBlock(NMSMappings_v1_19_R1.getBlockState(this));
            if (block instanceof BlockChest) {
                if (!this.f.opened) {
                    blockEvent(this.n, this.o, block, 1, this.viewingCount);
                }

                updateNeighborsAt(this.n, this.o, block);
            }
        }

    }

    public static class WildInventory implements IInventory {

        private static final WildItemStack<ItemStack, CraftItemStack> AIR = new WildItemStack<>(ItemStack.b, CraftItemStack.asCraftMirror(ItemStack.b));

        private final NonNullList<WildItemStack<ItemStack, CraftItemStack>> items;
        private final Chest chest;
        private final int index;

        private BiConsumer<Integer, ItemStack> setItemFunction = null;
        private int maxStack = 64;
        private int nonEmptyItems = 0;
        private String title;

        WildInventory(int size, String title, Chest chest, int index) {
            this.title = title == null ? "Chest" : title;
            this.items = NonNullList.a(size, AIR);
            this.chest = chest;
            this.index = index;
        }

        public int b() {
            return this.items.size();
        }

        public ItemStack a(int i) {
            return getWildItem(i).getItemStack();
        }

        public WildItemStack<ItemStack, CraftItemStack> getWildItem(int i) {
            return this.items.get(i);
        }

        public ItemStack a(int slot, int amount) {
            return splitStack(slot, amount, true);
        }

        public ItemStack b(int slot) {
            return splitStack(slot, 1, false);
        }

        private ItemStack splitStack(int slot, int amount, boolean update){
            ItemStack stack = this.a(slot);
            if (stack == ItemStack.b) {
                return stack;
            } else {
                ItemStack result;
                if (getCount(stack) <= amount) {
                    this.a(slot, ItemStack.b);
                    result = stack;
                } else {
                    result = CraftItemStack.copyNMSStack(stack, amount);
                    shrink(stack, amount);
                }

                if(update)
                    this.e();

                return result;
            }
        }

        public void a(int i, ItemStack itemStack) {
            setItem(i, itemStack, true);
        }

        public void setItem(int i, ItemStack itemStack, boolean setItemFunction){
            setItem(i, new WildItemStack<>(itemStack, CraftItemStack.asCraftMirror(itemStack)), setItemFunction);
        }

        public void setItem(int i, WildItemStack<?, ?> wildItemStack, boolean setItemFunction){
            ItemStack itemstack = (ItemStack) wildItemStack.getItemStack();

            if(setItemFunction && this.setItemFunction != null){
                this.setItemFunction.accept(i, itemstack);
                return;
            }

            //noinspection unchecked
            WildItemStack<ItemStack, CraftItemStack> original = this.items.set(i, (WildItemStack<ItemStack, CraftItemStack>) wildItemStack);

            if(!ItemStack.a(original.getItemStack(), itemstack)) {
                if (NMSMappings_v1_19_R1.isEmpty(itemstack))
                    nonEmptyItems--;
                else
                    nonEmptyItems++;
            }

            if (!NMSMappings_v1_19_R1.isEmpty(itemstack) && this.getMaxStackSize() > 0 && getCount(itemstack) > this.getMaxStackSize()) {
                setCount(itemstack, this.getMaxStackSize());
            }
        }

        private int getMaxStackSize() {
            return this.P_();
        }

        public int P_() {
            return this.maxStack;
        }

        public void setMaxStackSize(int size) {
            this.maxStack = size;
        }

        public void e() {
        }

        public boolean a(EntityHuman entityhuman) {
            return true;
        }

        public NonNullList<ItemStack> getContents() {
            NonNullList<ItemStack> contents = NonNullList.a(this.items.size(), ItemStack.b);
            for(int i = 0; i < contents.size(); i++)
                contents.set(i, a(i));
            return contents;
        }

        public void onOpen(CraftHumanEntity who) {
            if(index != 0 && !((WChest) chest).getTileEntityContainer().getTransaction().contains(who))
                throw new IllegalArgumentException("Opened directly page!");
        }

        public void onClose(CraftHumanEntity who) {
        }

        @Override
        public void d_(EntityHuman entityhuman) {
        }

        @Override
        public void c_(EntityHuman entityhuman) {

        }

        public List<HumanEntity> getViewers() {
            try {
                return new ArrayList<>(((WChest) chest).getTileEntityContainer().getTransaction());
            }catch (Exception ex){
                return new ArrayList<>();
            }
        }

        public org.bukkit.inventory.InventoryHolder getOwner() {
            return null;
        }

        public boolean b(int i, ItemStack itemstack) {
            return true;
        }

        public boolean isFull(){
            return nonEmptyItems == b();
        }

        public boolean isNotEmpty() {
            return nonEmptyItems > 0;
        }

        @Override
        public boolean c() {
            return nonEmptyItems <= 0;
        }

        void setTitle(String title) {
            this.title = title;
        }

        String getTitle() {
            return title;
        }

        @Override
        public void a() {
            this.items.clear();
        }

        @Override
        public Location getLocation() {
            return chest.getLocation();
        }

        @Override
        public String toString() {
            return "WildInventory{" +
                    "title='" + title + '\'' +
                    '}';
        }

    }

    private static class CraftWildInventory extends CraftInventory implements com.bgsoftware.wildchests.objects.inventory.CraftWildInventory {

        CraftWildInventory(IInventory inventory){
            super(inventory);
        }

        @Override
        public Chest getOwner() {
            return getInventory().chest;
        }

        @Override
        public WildItemStack<ItemStack, CraftItemStack> getWildItem(int slot) {
            return getInventory().getWildItem(slot);
        }

        @Override
        public void setItem(int i, WildItemStack<?, ?> itemStack) {
            getInventory().setItem(i, itemStack, true);
        }

        @Override
        public WildItemStack<?, ?>[] getWildContents() {
            return getInventory().items.toArray(new WildItemStack[0]);
        }

        @Override
        public WildInventory getInventory() {
            return (WildInventory) super.getInventory();
        }

        @Override
        public void setTitle(String title) {
            getInventory().setTitle(title);
        }

        @Override
        public String getTitle() {
            return getInventory().getTitle();
        }

        @Override
        public boolean isFull() {
            return getInventory().isFull();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CraftWildInventory && getInventory() == ((CraftWildInventory) obj).getInventory();
        }

    }

    private static class WildContainerChest extends ContainerChest {

        private final PlayerInventory playerInventory;
        private final WildInventory inventory;
        private CraftInventoryView bukkitEntity;

        private WildContainerChest(Containers<?> containers, int id, PlayerInventory playerInventory, WildInventory inventory, int rows){
            super(containers, id, playerInventory, inventory, rows);
            this.playerInventory = playerInventory;
            this.inventory = inventory;
        }

        @Override
        public CraftInventoryView getBukkitView() {
            if(bukkitEntity == null) {
                CraftWildInventory inventory = new CraftWildInventory(this.inventory);
                bukkitEntity = new CraftInventoryView(playerInventory.l.getBukkitEntity(), inventory, this);
            }

            return bukkitEntity;
        }

        @Override
        public void b(EntityHuman entityhuman) {
            if(!InventoryListener.buyNewPage.containsKey(entityhuman.cm()))
                ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
        }

        static Container of(int id, PlayerInventory playerInventory, WildInventory inventory){
            Containers<?> containers = Containers.c;
            int rows = 3;

            switch (inventory.b()) {
                case 9 -> {
                    rows = 1;
                    containers = Containers.a;
                }
                case 18 -> {
                    rows = 2;
                    containers = Containers.b;
                }
                case 36 -> {
                    rows = 4;
                    containers = Containers.d;
                }
                case 45 -> {
                    rows = 5;
                    containers = Containers.e;
                }
                case 54 -> {
                    rows = 6;
                    containers = Containers.f;
                }
            }

            return new WildContainerChest(containers, id, playerInventory, inventory, rows);
        }

    }

    private static class WildContainerHopper extends ContainerHopper {

        private final PlayerInventory playerInventory;
        private final WildInventory inventory;
        private CraftInventoryView bukkitEntity;

        private WildContainerHopper(int id, PlayerInventory playerInventory, WildInventory inventory){
            super(id, playerInventory, inventory);
            this.playerInventory = playerInventory;
            this.inventory = inventory;
        }

        @Override
        public CraftInventoryView getBukkitView() {
            if(bukkitEntity == null) {
                CraftWildInventory inventory = new CraftWildInventory(this.inventory);
                bukkitEntity = new CraftInventoryView(playerInventory.l.getBukkitEntity(), inventory, this);
            }

            return bukkitEntity;
        }

        @Override
        public void b(EntityHuman entityhuman) {
            ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
        }

        static WildContainerHopper of(int id, PlayerInventory playerInventory, WildInventory inventory){
            return new WildContainerHopper(id, playerInventory, inventory);
        }

    }

}
