package xyz.wildseries.wildchests.utils;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.api.objects.chests.Chest;

import java.util.HashMap;

@SuppressWarnings("WeakerAccess")
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

    public static void removeFromChest(Chest chest, ItemStack itemStack, int amount){
        Inventory[] pages = chest.getPages();

        int itemsRemoved = 0;

        for(int i = 0; i < pages.length && itemsRemoved < amount; i++){
            Inventory page = pages[i];
            int toRemove = Math.min(amount - itemsRemoved, countItems(itemStack, page));
            ItemStack cloned = itemStack.clone();
            cloned.setAmount(toRemove);
            page.removeItem(cloned);
        }
    }

    public static int countItems(ItemStack itemStack, Inventory inventory){
        int amount = 0;

        for(ItemStack _itemStack : inventory.getContents()){
            if(_itemStack != null && _itemStack.isSimilar(itemStack))
                amount += _itemStack.getAmount();
        }

        return amount;
    }

}
