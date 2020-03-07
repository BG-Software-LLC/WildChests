package com.bgsoftware.wildchests.utils;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class ItemUtils {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    public static void addItem(ItemStack itemStack, Inventory inventory, Location location){
        HashMap<Integer, ItemStack> additionalItems = inventory.addItem(itemStack);
        if(location != null && !additionalItems.isEmpty()){
            for(ItemStack additional : additionalItems.values())
                dropItem(location, additional);
        }
    }

    public static String getFormattedType(String type){
        StringBuilder name = new StringBuilder();
        String[] split = type.split("_");

        for(int i = 0; i < split.length; i++) {
            name.append(split[i].substring(0, 1).toUpperCase()).append(split[i].substring(1).toLowerCase());
            if(i != split.length - 1)
                name.append(" ");
        }

        return name.toString();
    }

    public static int countItems(ItemStack itemStack, Inventory inventory){
        int amount = 0;

        for(ItemStack _itemStack : inventory.getContents()){
            if(_itemStack != null && _itemStack.isSimilar(itemStack))
                amount += _itemStack.getAmount();
        }

        return amount;
    }

    public static int getSpaceLeft(Inventory inventory, ItemStack itemStack){
        int spaceLeft = 0, counter = 0;

        for(ItemStack _itemStack : inventory.getContents()){
            if(counter >= 5)
                break;
            else if(_itemStack == null || _itemStack.getType() == Material.AIR) {
                spaceLeft += itemStack.getMaxStackSize();
            }
            else if(_itemStack.isSimilar(itemStack)){
                spaceLeft += Math.max(0, _itemStack.getMaxStackSize() - _itemStack.getAmount());
            }
            counter++;
        }
        return spaceLeft;
    }

    public static void dropItem(Location location, ItemStack itemStack){
        dropItem(location, itemStack, false);
    }

    public static void dropItem(Location location, ItemStack itemStack, boolean tag){
        if(itemStack.getMaxStackSize() <= 0)
            return;

        int amountOfIterates = itemStack.getAmount() / itemStack.getMaxStackSize();

        ItemStack cloned;

        if(tag){
            cloned = plugin.getNMSAdapter().addNBTTag(itemStack);
        }
        else{
            cloned = itemStack.clone();
        }

        cloned.setAmount(itemStack.getMaxStackSize());

        for(int i = 0; i < amountOfIterates; i++)
            location.getWorld().dropItemNaturally(location, cloned);

        if(itemStack.getAmount() % itemStack.getMaxStackSize() > 0){
            cloned.setAmount(itemStack.getAmount() % itemStack.getMaxStackSize());
            location.getWorld().dropItemNaturally(location, cloned);
        }
    }

    public static void dropOrCollect(Player player, ItemStack itemStack, boolean collect, Location location){
        if(collect && player != null){
            Map<Integer, ItemStack> additionalItems = player.getInventory().addItem(itemStack);
            if(additionalItems.isEmpty())
                return;
        }

        dropItem(location, itemStack, false);
    }

    public static Map<ItemStack, Integer> getSortedItems(ItemStack[] itemStacks){
        // <ItemStack, TotalAmount>
        Map<ItemStack, Integer> map = new HashMap<>();

        for(ItemStack itemStack : itemStacks){
            if(itemStack != null && itemStack.getType() != Material.AIR){
                ItemStack cloned = itemStack.clone();
                cloned.setAmount(1);
                map.put(cloned, map.getOrDefault(cloned, 0) + itemStack.getAmount());
            }
        }

        return map;
    }

}
