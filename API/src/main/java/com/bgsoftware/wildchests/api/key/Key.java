package com.bgsoftware.wildchests.api.key;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Class to convert item-stacks into a comparable object.
 * This doesn't support ItemMeta, used for detection of blacklists & whitelists.
 */
public final class Key {

    private final String key;

    private Key(String key){
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }

    public static Key of(ItemStack itemStack){
        return of(itemStack.getType(), itemStack.getDurability());
    }

    public static Key of(Material material, short data){
        return of(material + ":" + data);
    }

    public static Key of(String key){
        return new Key(key);
    }

}
