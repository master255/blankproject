package com.blank.project.utils;

public class MemoryStoredArrayList<T> extends DiskStoredArrayList<T> {
    @Override
    public int size() {
        return super.sizeArrayList();
    }

    @Override
    public boolean add(T t) {
        return super.addArrayList(t);
    }

    @Override
    public T get(int index) {
        return super.getArrayList(index);
    }
}