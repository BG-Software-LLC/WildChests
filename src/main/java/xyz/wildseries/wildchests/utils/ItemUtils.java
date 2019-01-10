package xyz.wildseries.wildchests.utils;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.api.objects.chests.Chest;

import java.util.HashMap;

public final class ItemUtils {

    public static void addItem(ItemStack itemStack, Inventory inventory, Location location){
        HashMap<Integer, ItemStack> additionalItems = inventory.addItem(itemStack);
        if(location != null && !additionalItems.isEmpty()){
            for(ItemStack additional : additionalItems.values())
                location.getWorld().dropItemNaturally(location, additional);
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

    public static boolean addToChest(Chest chest, ItemStack itemStack){
        HashMap<Integer, ItemStack> additionalItems;

        int currentInventory = 0;

        do{
            Inventory inventory = chest.getPage(currentInventory);
            additionalItems = inventory.addItem(itemStack);
            currentInventory++;
        }while(!additionalItems.isEmpty() && currentInventory < chest.getPagesAmount());

        return additionalItems.isEmpty();
    }

}
