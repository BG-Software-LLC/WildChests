package com.bgsoftware.wildchests.objects.inventory;

import org.bukkit.inventory.ItemStack;

public final class InventoryHolder {

    private final String title;
    private final ItemStack[] contents;

    public InventoryHolder(int size, String title){
        this(title, new ItemStack[size]);
    }

    public InventoryHolder(String title, ItemStack[] contents){
        this.title = title == null ? "Chest" : title;
        this.contents = contents;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public int getSize(){
        return contents.length;
    }

    public String getTitle() {
        return title;
    }

    public void setItem(int slot, ItemStack itemStack){
        contents[slot] = itemStack;
    }

}
