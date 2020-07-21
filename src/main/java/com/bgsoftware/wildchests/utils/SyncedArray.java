package com.bgsoftware.wildchests.utils;

import java.util.Arrays;
import java.util.stream.Stream;

public final class SyncedArray<E> {

    private Object[] arr;

    public SyncedArray(int length){
        arr = new Object[length];
    }

    public void set(int index, E element){
        arr[index] = element;
    }

    public E get(int index){
        //noinspection all
        return (E) arr[index];
    }

    public int length(){
        return arr.length;
    }

    public Stream<E> stream(){
        //noinspection all
        return Arrays.stream(arr).map(e -> (E) e);
    }

    public void increaseCapacity(int newSize){
        if(newSize > length())
            arr = Arrays.copyOf(arr, newSize);
    }

}
