package com.blank.project.utils;

import java.io.OutputStream;

public class CustomByteArrayOutputStream extends OutputStream {

    protected byte[] buf;
    protected int count;
    protected int readed;
    protected final boolean increase2x;

    public CustomByteArrayOutputStream(int size) {
        if (size >= 0)
            buf = new byte[size];
        else
            throw new IllegalArgumentException("size < 0");
        increase2x = false;
    }

    public CustomByteArrayOutputStream(int size, boolean increase2x) {
        if (size >= 0)
            buf = new byte[size];
        else
            throw new IllegalArgumentException("size < 0");
        this.increase2x = true;
    }

    public byte[] getData() {
        return buf;
    }

    private void expand(int i) {
        if (count + i <= buf.length)
            return;
        byte[] newbuf = new byte[increase2x ? (count + i) * 2 : count + i];
        if (buf.length > 0)
            System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
    }

    public synchronized void reset() {
        count = 0;
        readed = 0;
    }

    public int size() {
        return count;
    }

    public synchronized void read(byte[] buffer) {
        int countAll = Math.min(buffer.length, count - readed);
        System.arraycopy(buf, readed, buffer, 0, countAll);
        readed += countAll;
    }

    @Override
    public synchronized void write(int oneByte) {
        if (count == buf.length)
            expand(1);
        buf[count++] = (byte) oneByte;
    }

    @Override
    public synchronized void write(byte[] buffer, int offset, int len) {
        if (len == 0)
            return;
        expand(len);
        System.arraycopy(buffer, offset, buf, count, len);
        count += len;
    }

    public void setSize(int size) {
        buf = new byte[size];
        reset();
    }
}
