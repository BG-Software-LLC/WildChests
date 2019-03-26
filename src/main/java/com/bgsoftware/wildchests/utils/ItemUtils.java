package com.bgsoftware.wildchests.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public final class ItemUtils {

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
        int amountOfIterates = itemStack.getAmount() / itemStack.getMaxStackSize();

        ItemStack cloned = itemStack.clone();
        cloned.setAmount(itemStack.getMaxStackSize());

        for(int i = 0; i < amountOfIterates; i++)
            location.getWorld().dropItemNaturally(location, cloned);

        if(itemStack.getAmount() % itemStack.getMaxStackSize() > 0){
            cloned.setAmount(itemStack.getAmount() % itemStack.getMaxStackSize());
            location.getWorld().dropItemNaturally(location, cloned);
        }
    }

}
