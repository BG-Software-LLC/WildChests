package com.bgsoftware.wildchests.utils;

import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ItemStackMap<V> {

    private final Map<ItemStack, V> backendData = new HashMap<>();

    @Nullable
    public V put(ItemStack key, V value) {
        return this.backendData.put(getKeyFromItemStack(key), value);
    }

    @Nullable
    public V get(ItemStack key) {
        return this.backendData.get(getKeyFromItemStack(key));
    }

    public V computeIfAbsent(ItemStack key, Function<ItemStack, V> mapper) {
        return this.backendData.computeIfAbsent(getKeyFromItemStack(key), mapper);
    }

    public void forEach(BiConsumer<ItemStack, V> consumer) {
        this.backendData.forEach(consumer);
    }

    private static ItemStack getKeyFromItemStack(ItemStack itemStack) {
        ItemStack key = itemStack.clone();
        key.setAmount(1);
        return key;
    }

}
