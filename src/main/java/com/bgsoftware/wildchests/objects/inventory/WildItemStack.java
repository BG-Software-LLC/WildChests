package com.bgsoftware.wildchests.objects.inventory;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class WildItemStack<T, U extends ItemStack> {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public static WildItemStack<?, ?> AIR = of(new ItemStack(Material.AIR));

    private final T itemStack;
    private final U craftItemStack;

    public WildItemStack(T itemStack, U craftItemStack){
        this.itemStack = itemStack;
        this.craftItemStack = craftItemStack;
    }

    public T getItemStack() {
        return itemStack;
    }

    public U getCraftItemStack() {
        return craftItemStack;
    }

    public WildItemStack<?, ?> cloneItemStack(){
        return of(craftItemStack);
    }

    public static WildItemStack<?, ?> of(ItemStack itemStack){
        return plugin.getNMSInventory().createItemStack(itemStack);
    }

}
