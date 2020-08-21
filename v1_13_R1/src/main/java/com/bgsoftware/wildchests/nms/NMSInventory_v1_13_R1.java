package com.bgsoftware.wildchests.nms;

import com.bgsoftware.wildchests.api.key.Key;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.hooks.WildStackerHook;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.chests.WStorageChest;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.WildItemStack;
import com.bgsoftware.wildchests.utils.ChestUtils;
import net.minecraft.server.v1_13_R1.AxisAlignedBB;
import net.minecraft.server.v1_13_R1.Block;
import net.minecraft.server.v1_13_R1.BlockPosition;
import net.minecraft.server.v1_13_R1.Blocks;
import net.minecraft.server.v1_13_R1.ChatComponentText;
import net.minecraft.server.v1_13_R1.Chunk;
import net.minecraft.server.v1_13_R1.Container;
import net.minecraft.server.v1_13_R1.ContainerChest;
import net.minecraft.server.v1_13_R1.ContainerHopper;
import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityHuman;
import net.minecraft.server.v1_13_R1.EntityItem;
import net.minecraft.server.v1_13_R1.EntityPlayer;
import net.minecraft.server.v1_13_R1.EnumDirection;
import net.minecraft.server.v1_13_R1.IChatBaseComponent;
import net.minecraft.server.v1_13_R1.IInventory;
import net.minecraft.server.v1_13_R1.ITickable;
import net.minecraft.server.v1_13_R1.IWorldInventory;
import net.minecraft.server.v1_13_R1.ItemStack;
import net.minecraft.server.v1_13_R1.NBTTagByte;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import net.minecraft.server.v1_13_R1.NonNullList;
import net.minecraft.server.v1_13_R1.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_13_R1.PlayerInventory;
import net.minecraft.server.v1_13_R1.TileEntity;
import net.minecraft.server.v1_13_R1.TileEntityChest;
import net.minecraft.server.v1_13_R1.World;
import net.minecraft.server.v1_13_R1.WorldServer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_13_R1.CraftParticle;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R1.event.CraftEventFactory;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftInventory;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public final class NMSInventory_v1_13_R1 implements NMSInventory {

    @Override
    public void updateTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        Chunk chunk = world.getChunkAtWorldCoords(blockPosition);

        TileEntityWildChest tileEntityWildChest = new TileEntityWildChest(chest, world, blockPosition);

        chunk.tileEntities.put(blockPosition, tileEntityWildChest);
        world.capturedTileEntities.put(blockPosition, tileEntityWildChest);
        world.tileEntityListTick.add(tileEntityWildChest);
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

        Container container = createContainer(entityPlayer.inventory, entityPlayer, inventory);
        container.windowId = entityPlayer.nextContainerCounter();
        TileEntityWildChest tileEntityWildChest = getTileEntity(inventory.getOwner());

        entityPlayer.playerConnection.sendPacket(new PacketPlayOutOpenWindow(container.windowId, tileEntityWildChest.getContainerName(), new ChatComponentText(title), inventory.getSize()));
        entityPlayer.activeContainer = container;
        entityPlayer.activeContainer.addSlotListener(entityPlayer);
    }

    @Override
    public void createDesignItem(com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory, org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE) :
                itemStack.clone());

        designItem.setCount(1);
        designItem.a("DesignItem", new NBTTagByte((byte) 1));

        WildInventory inventory = ((CraftWildInventory) craftWildInventory).getInventory();
        inventory.setItem(0, designItem, false);
        inventory.setItem(1, designItem, false);
        inventory.setItem(3, designItem, false);
        inventory.setItem(4, designItem, false);
    }

    private static Container createContainer(PlayerInventory playerInventory, EntityHuman entityHuman, com.bgsoftware.wildchests.objects.inventory.CraftWildInventory craftWildInventory){
        WildInventory inventory = ((NMSInventory_v1_13_R1.CraftWildInventory) craftWildInventory).getInventory();
        return inventory.getSize() == 5 ? WildContainerHopper.of(playerInventory, entityHuman, inventory) :
                WildContainerChest.of(playerInventory, entityHuman, inventory);
    }

    private static TileEntityWildChest getTileEntity(Chest chest) {
        Location loc = chest.getLocation();
        World world = ((CraftWorld) loc.getWorld()).getHandle();
        BlockPosition blockPosition = new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return (TileEntityWildChest) world.getTileEntity(blockPosition);
    }

    private static class TileEntityWildChest extends TileEntityChest implements IWorldInventory, TileEntityContainer, ITickable {

        private static final BiPredicate<EntityItem, ChestData> suctionPredicate = (entityItem, chestData) -> {
            Key itemKey = Key.of(CraftItemStack.asCraftMirror(entityItem.getItemStack()));
            return entityItem.isAlive() && !entityItem.getItemStack().isEmpty() &&
                    (chestData.getWhitelisted().isEmpty() || chestData.getWhitelisted().contains(itemKey)) &&
                    !chestData.getBlacklisted().contains(itemKey);
        };

        private final TileEntityChest tileEntityChest = new TileEntityChest();
        private final Chest chest;
        private final boolean isTrappedChest;

        private short currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

        private AxisAlignedBB suctionItems = null;
        private boolean autoCraftMode = false;
        private boolean autoSellMode = false;

        private TileEntityWildChest(Chest chest, World world, BlockPosition blockPosition){
            this.chest = chest;
            this.world = world;
            updateTile(this, world, blockPosition);
            updateTile(tileEntityChest, world, blockPosition);
            isTrappedChest = world.getType(blockPosition).getBlock() == Blocks.TRAPPED_CHEST;
            ((WChest) chest).setTileEntityContainer(this);
            updateData();
        }

        @Override
        protected NonNullList<ItemStack> q() {
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
            NonNullList<ItemStack> nonNullList = NonNullList.a(contents.length, ItemStack.a);

            for(int i = 0; i < contents.length; i++)
                nonNullList.set(i, (ItemStack) contents[i].getItemStack());

            return nonNullList;
        }

        @Override
        public NBTTagCompound save(NBTTagCompound nbttagcompound) {
            return tileEntityChest.save(nbttagcompound);
        }

        @Override
        public NBTTagCompound aa_() {
            return save(new NBTTagCompound());
        }

        @Override
        public Container createContainer(PlayerInventory playerinventory, EntityHuman entityHuman) {
            return NMSInventory_v1_13_R1.createContainer(playerinventory, entityHuman,
                    (com.bgsoftware.wildchests.objects.inventory.CraftWildInventory) chest.getPage(0));
        }

        @Override
        public IChatBaseComponent getScoreboardDisplayName() {
            return new ChatComponentText(chest.getPage(0).getTitle());
        }

        @Override
        public final void closeContainer(EntityHuman entityHuman) {
            CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

            this.f = (int) this.transaction.stream().filter(human -> human.getGameMode() != GameMode.SPECTATOR).count();
            this.transaction.remove(craftHumanEntity);

            if (!craftHumanEntity.getHandle().isSpectator()) {
                int oldPower = Math.max(0, Math.min(15, this.f));

                this.f--;

                if (isTrappedChest) {
                    int newPower = Math.max(0, Math.min(15, this.f));
                    if (oldPower != newPower) {
                        CraftEventFactory.callRedstoneChange(this.world, this.position, oldPower, newPower);
                    }
                }

                Block block = this.getBlock().getBlock();
                this.world.playBlockAction(this.position, block, 1, this.f);
                this.world.applyPhysics(this.position, block);
            }
        }

        @Override
        public final void startOpen(EntityHuman entityHuman) {
            CraftHumanEntity craftHumanEntity = entityHuman.getBukkitEntity();

            this.transaction.add(craftHumanEntity);

            if (!craftHumanEntity.getHandle().isSpectator()) {
                if (this.f < 0) {
                    this.f = 0;
                }

                int oldPower = Math.max(0, Math.min(15, this.f));
                this.f++;

                if (this.world == null) {
                    return;
                }

                if (isTrappedChest) {
                    int newPower = Math.max(0, Math.min(15, this.f));
                    if (oldPower != newPower) {
                        CraftEventFactory.callRedstoneChange(this.world, this.position, oldPower, newPower);
                    }
                }

                Block block = this.getBlock().getBlock();
                this.world.playBlockAction(this.position, block, 1, this.f);
                this.world.applyPhysics(this.position, block);
            }
        }

        @Override
        public final void onOpen(CraftHumanEntity who) {

        }

        @Override
        public final void onClose(CraftHumanEntity who) {

        }

        @Override
        public void Y_() {
            super.Y_();

            ChestData chestData = chest.getData();

            {
                double x = position.getX() + world.random.nextFloat();
                double y = position.getY() + world.random.nextFloat();
                double z = position.getZ() + world.random.nextFloat();
                for(String particle : chestData.getChestParticles()) {
                    try {
                        ((WorldServer) world).sendParticles(null, CraftParticle.toNMS(Particle.valueOf(particle)),
                                x, y, z, 0, 0.0, 0.0, 0.0, 1.0);
                    }catch (Exception ignored){}
                }
            }

            if(--currentCooldown >= 0)
                return;

            Block currentBlock = world.getType(position).getBlock();

            if(currentBlock != Blocks.CHEST && currentBlock != Blocks.TRAPPED_CHEST){
                world.getChunkAtWorldCoords(position).tileEntities.remove(position);
                world.capturedTileEntities.remove(position);
                world.tileEntityListTick.remove(this);
                return;
            }

            currentCooldown = ChestUtils.DEFAULT_COOLDOWN;

            if(suctionItems != null) {
                for (Entity entity : world.a(EntityItem.class, suctionItems, (Predicate<? super EntityItem>)
                        entity -> suctionPredicate.test((EntityItem) entity, chestData))) {
                    EntityItem entityItem = (EntityItem) entity;
                    org.bukkit.inventory.ItemStack itemStack = CraftItemStack.asCraftMirror(entityItem.getItemStack());
                    Item item = (Item) entityItem.getBukkitEntity();

                    if (WildStackerHook.isEnabled())
                        itemStack.setAmount(WildStackerHook.getItemAmount(item));

                    org.bukkit.inventory.ItemStack remainingItem = ChestUtils.getRemainingItem(chest.addItems(itemStack));

                    if (remainingItem == null) {
                        ((WorldServer) world).sendParticles(null, CraftParticle.toNMS(Particle.CLOUD), entityItem.locX, entityItem.locY,
                                entityItem.locZ, 0, 0.0, 0.0, 0.0, 1.0);
                        entityItem.die();
                    } else if (WildStackerHook.isEnabled()) {
                        WildStackerHook.setRemainings(item, remainingItem.getAmount());
                    } else {
                        item.setItemStack(remainingItem);
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
        public int getSize() {
            return chest.getPage(0).getSize() * chest.getPagesAmount();
        }

        @Override
        public int getViewingCount() {
            if (this.f < 0)
                this.f = 0;

            return f;
        }

        @Override
        public List<HumanEntity> getTransaction() {
            return transaction;
        }

        @Override
        public void setTransaction(List<HumanEntity> transaction) {
            this.transaction = transaction;
        }

        @Override
        public void openContainer(HumanEntity humanEntity) {
            startOpen(((CraftHumanEntity) humanEntity).getHandle());
        }

        @Override
        public void closeContainer(HumanEntity humanEntity) {
            closeContainer(((CraftHumanEntity) humanEntity).getHandle());
        }

        @Override
        public void updateData(){
            ChestData chestData = chest.getData();
            suctionItems = !chestData.isAutoSuction() ? null :  new AxisAlignedBB(
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
            if (slot == -2 && chest instanceof StorageChest) {
                ItemStack itemStack = this.getItem(slot);
                itemStack.setCount(((WStorageChest) chest).getAmount().min(BigInteger.valueOf(amount)).intValue());
                ((WStorageChest) chest).setAmount(((WStorageChest) chest).getAmount().subtract(BigInteger.valueOf(amount)));
                return itemStack;
            }

            return super.splitStack(slot, amount);
        }

        @Override
        public void update() {
            super.update();
            if(chest instanceof StorageChest)
                ((StorageChest) chest).update();
        }

        private void updateTile(TileEntity tileEntity, World world, BlockPosition blockPosition){
            tileEntity.setWorld(world);
            tileEntity.setPosition(blockPosition);
        }

    }

    public static class WildInventory implements IInventory {

        private static final WildItemStack<ItemStack, CraftItemStack> AIR = new WildItemStack<>(ItemStack.a, CraftItemStack.asCraftMirror(ItemStack.a));

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

        public int getSize() {
            return this.items.size();
        }

        public ItemStack getItem(int i) {
            return getWildItem(i).getItemStack();
        }

        public WildItemStack<ItemStack, CraftItemStack> getWildItem(int i) {
            return this.items.get(i);
        }

        public ItemStack splitStack(int slot, int amount) {
            return splitStack(slot, amount, true);
        }

        public ItemStack splitWithoutUpdate(int slot) {
            return splitStack(slot, 1, false);
        }

        private ItemStack splitStack(int slot, int amount, boolean update){
            ItemStack stack = this.getItem(slot);
            if (stack == ItemStack.a) {
                return stack;
            } else {
                ItemStack result;
                if (stack.getCount() <= amount) {
                    this.setItem(slot, ItemStack.a);
                    result = stack;
                } else {
                    result = CraftItemStack.copyNMSStack(stack, amount);
                    stack.subtract(amount);
                }

                if(update)
                    this.update();

                return result;
            }
        }

        public void setItem(int i, ItemStack itemStack) {
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

            if(!ItemStack.matches(original.getItemStack(), itemstack)) {
                if (itemstack.isEmpty())
                    nonEmptyItems--;
                else
                    nonEmptyItems++;
            }

            if (!itemstack.isEmpty() && this.getMaxStackSize() > 0 && itemstack.getCount() > this.getMaxStackSize()) {
                itemstack.setCount(this.getMaxStackSize());
            }
        }

        public int getMaxStackSize() {
            return this.maxStack;
        }

        public void setMaxStackSize(int size) {
            this.maxStack = size;
        }

        public void update() {
        }

        public boolean a(EntityHuman entityhuman) {
            return true;
        }

        public NonNullList<ItemStack> getContents() {
            NonNullList<ItemStack> contents = NonNullList.a(this.items.size(), ItemStack.a);
            for(int i = 0; i < contents.size(); i++)
                contents.set(i, getItem(i));
            return contents;
        }

        public void onOpen(CraftHumanEntity who) {
            if(index != 0 && !((WChest) chest).getTileEntityContainer().getTransaction().contains(who))
                throw new IllegalArgumentException("Opened directly page!");
        }

        public void onClose(CraftHumanEntity who) {
        }

        @Override
        public void startOpen(EntityHuman entityhuman) {
        }

        @Override
        public void closeContainer(EntityHuman entityhuman) {

        }

        public List<HumanEntity> getViewers() {
            return new ArrayList<>(((WChest) chest).getTileEntityContainer().getTransaction());
        }

        public org.bukkit.inventory.InventoryHolder getOwner() {
            return null;
        }

        public boolean b(int i, ItemStack itemstack) {
            return true;
        }

        public boolean isFull(){
            return nonEmptyItems == getSize();
        }

        public boolean P_() {
            return nonEmptyItems > 0;
        }

        @Override
        public int getProperty(int i) {
            return 0;
        }

        @Override
        public void setProperty(int i, int i1) {

        }

        @Override
        public int h() {
            return 0;
        }

        @Override
        public IChatBaseComponent getDisplayName() {
            return new ChatComponentText(getTitle());
        }

        @Override
        public IChatBaseComponent getCustomName() {
            return getDisplayName();
        }

        @Override
        public boolean hasCustomName() {
            return title != null;
        }

        void setTitle(String title) {
            this.title = title;
        }

        String getTitle() {
            return title;
        }

        @Override
        public void clear() {
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

    private static class WildContainerChest extends ContainerChest{

        private final PlayerInventory playerInventory;
        private final WildInventory inventory;
        private CraftInventoryView bukkitEntity;

        private WildContainerChest(PlayerInventory playerInventory, EntityHuman entityHuman,  WildInventory inventory){
            super(playerInventory, inventory, entityHuman);
            this.playerInventory = playerInventory;
            this.inventory = inventory;
        }

        @Override
        public CraftInventoryView getBukkitView() {
            if(bukkitEntity == null) {
                NMSInventory_v1_13_R1.CraftWildInventory inventory = new NMSInventory_v1_13_R1.CraftWildInventory(this.inventory);
                bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
            }

            return bukkitEntity;
        }

        static Container of(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory){
            return new WildContainerChest(playerInventory, entityHuman, inventory);
        }

    }

    private static class WildContainerHopper extends ContainerHopper {

        private final PlayerInventory playerInventory;
        private final WildInventory inventory;
        private CraftInventoryView bukkitEntity;

        private WildContainerHopper(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory){
            super(playerInventory, inventory, entityHuman);
            this.playerInventory = playerInventory;
            this.inventory = inventory;
        }

        @Override
        public CraftInventoryView getBukkitView() {
            if(bukkitEntity == null) {
                NMSInventory_v1_13_R1.CraftWildInventory inventory = new NMSInventory_v1_13_R1.CraftWildInventory(this.inventory);
                bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
            }

            return bukkitEntity;
        }

        static WildContainerHopper of(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory){
            return new WildContainerHopper(playerInventory, entityHuman, inventory);
        }

    }

}
