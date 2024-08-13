package com.blank.project.models;

import java.io.Serializable;

public class MapEntry implements Serializable {
    private static final long serialVersionUID = 1;
    private long startByte;
    private int length;

    public MapEntry(long startByte, int length) {
        this.startByte = startByte;
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public long getStartByte() {
        return startByte;
    }

    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    public void setLength(int length) {
        this.length = length;
    }
}