package com.blank.project.utils;

public class IntegerSync {
    private int value;
    private final Object object = new Object();

    public void inc() {
        synchronized (object) {
            value++;
        }
    }

    public int incGet() {
        synchronized (object) {
            value++;
        }
        return value;
    }

    public void dec() {
        synchronized (object) {
            value--;
        }
    }

    public int decGet() {
        synchronized (object) {
            value--;
        }
        return value;
    }

    public int getSync() {
        synchronized (object) {
            return value;
        }
    }

    public void setSync(int value) {
        synchronized (object) {
            this.value = value;
        }
    }

    public int get() {
        return value;
    }

    public void set(int value) {
        this.value = value;
    }
}