package com.bgsoftware.wildchests.utils;

public class Counter {

    private int value = 0;

    public void increase(int delta) {
        this.value += delta;
    }

    public int get() {
        return value;
    }

}
