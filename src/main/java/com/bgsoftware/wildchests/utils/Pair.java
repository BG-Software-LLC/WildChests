package com.bgsoftware.wildchests.utils;

public final class Pair<K, V> {

    public K key;
    public V value;

    public Pair(K key, V value){
        this.key = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Pair{" + key + ", " + value + "}";
    }
}
