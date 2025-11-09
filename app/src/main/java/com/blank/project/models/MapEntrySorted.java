package com.blank.project.models;

import java.io.Serializable;

public class MapEntrySorted implements Serializable {
    private static final long serialVersionUID = 1;
    private long startByte;
    private int length;
    private int id;
    private long lastModified;

    public MapEntrySorted(long startByte, int length) {
        this.startByte = startByte;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
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