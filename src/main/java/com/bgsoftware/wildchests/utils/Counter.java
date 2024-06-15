package com.bgsoftware.wildchests.utils;

public class Counter {

    private long value = 0;

    public Counter() {

    }

    public void increase(int delta) {
        this.value += delta;
    }

    public long get() {
        return value;
    }

}
