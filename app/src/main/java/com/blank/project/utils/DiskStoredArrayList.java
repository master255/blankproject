package com.blank.project.utils;

import com.blank.project.Constants;
import com.blank.project.models.MapEntry;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DiskStoredArrayList<T> extends ArrayList<T> {

    private final int bufferSize;
    private final String cacheFilePath;
    private RandomAccessFile cacheFile, cacheIndexFile;
    private final ArrayList<MapEntry> mapEntries = new ArrayList<>();
    private final EntryCaches entryCaches = new EntryCaches();

    public DiskStoredArrayList(int bufferSize, String cacheFolderPath, String cacheFilePath, String cacheIndexFilePath) {
        this.bufferSize = bufferSize;
        this.cacheFilePath = cacheFilePath;
        final File cacheFolderFile = new File(cacheFolderPath);
        if (!cacheFolderFile.exists()) cacheFolderFile.mkdirs();
        try {
            this.cacheFile = new RandomAccessFile(cacheFilePath, "rwd");
            this.cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            if (cacheIndexFile.length() > 0) {
                final byte[] buf = new byte[Constants.FILE_BUFFER_LENGTH];
                int bytesRead;
                final CustomByteArrayOutputStream byteArrayOutputStream = new CustomByteArrayOutputStream(0);
                try {
                    while ((bytesRead = cacheIndexFile.read(buf)) != -1)
                        byteArrayOutputStream.write(buf, 0, bytesRead);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final ArrayList<MapEntry> mapEntryArrayList = (ArrayList<MapEntry>) ObjectHelper.convertFromBytes(byteArrayOutputStream.getData(), null);
                if (mapEntryArrayList != null)
                    mapEntries.addAll(mapEntryArrayList);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int size() {
        return mapEntries.size();
    }

    @Override
    public boolean add(T t) {
        try {
            final long length = cacheFile.length();
            final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
            cacheFile.seek(length);
            cacheFile.write(bytes);
            entryCaches.add(new EntryCache(mapEntries.size(), t));
            if (entryCaches.size() > bufferSize) entryCaches.remove(0);
            mapEntries.add(new MapEntry(length, bytes.length));
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public T get(int index) {
        final T object = entryCaches.search(index);
        if (object != null) return object;
        final MapEntry mapEntry = mapEntries.get(index);
        final byte[] buf = new byte[10240];
        int bytesRead;
        final CustomByteArrayOutputStream byteArrayOutputStream = new CustomByteArrayOutputStream(0);
        try {
            cacheFile.seek(mapEntry.getStartByte());
            int allBytes = mapEntry.getLength();
            while ((bytesRead = cacheFile.read(buf)) != -1) {
                allBytes -= bytesRead;
                if (allBytes > 0)
                    byteArrayOutputStream.write(buf, 0, bytesRead);
                else if (allBytes == 0) {
                    byteArrayOutputStream.write(buf, 0, bytesRead);
                    break;
                } else {
                    byteArrayOutputStream.write(buf, 0, (bytesRead + allBytes));
                    break;
                }
            }
            final T entry = (T) ObjectHelper.convertFromBytes(byteArrayOutputStream.getData(), null);
            if (entry != null) {
                entryCaches.add(new EntryCache(index, entry));
                if (entryCaches.size() > bufferSize) entryCaches.remove(0);
            }
            return entry;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Sublist not supported");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Removing is not supported");
    }

    @Override
    public T remove(int index) {//not tested
        final MapEntry mapEntry = mapEntries.remove(index);
        if (mapEntry == null) return null;
        try {
            cacheFile.close();
            final File fileTmp = new File(cacheFilePath);
            if (fileTmp.renameTo(new File(cacheFilePath + "t"))) {
                cacheFile = new RandomAccessFile(cacheFilePath, "rwd");
                final RandomAccessFile fileTmpData = new RandomAccessFile(fileTmp, "rwd");
                final byte[] buf = new byte[Constants.FILE_BUFFER_LENGTH];
                int bytesRead;
                long allBytes = mapEntry.getStartByte();
                while ((bytesRead = fileTmpData.read(buf)) != -1) {
                    allBytes -= bytesRead;
                    if (allBytes > 0)
                        cacheFile.write(buf, 0, bytesRead);
                    else if (allBytes == 0) {
                        cacheFile.write(buf, 0, bytesRead);
                        fileTmpData.skipBytes(mapEntry.getLength());
                        while ((bytesRead = fileTmpData.read(buf)) != -1)
                            cacheFile.write(buf, 0, bytesRead);
                        break;
                    } else {
                        cacheFile.write(buf, 0, (int) (bytesRead + allBytes));
                        fileTmpData.skipBytes(mapEntry.getLength());
                        while ((bytesRead = fileTmpData.read(buf)) != -1)
                            cacheFile.write(buf, 0, bytesRead);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void clear() {
        super.clear();
        mapEntries.clear();
        try {
            cacheFile.setLength(0);
            cacheIndexFile.setLength(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            cacheFile.close();
            cacheIndexFile.setLength(0);
            cacheIndexFile.write(ObjectHelper.convertToBytes(mapEntries));
            cacheIndexFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class EntryCaches extends ArrayList<EntryCache> {
        public T search(int index) {
            for (int i = size() - 1; i > -1; i--) {
                final EntryCache entryCache = get(i);
                if (index == entryCache.getIndex())
                    return entryCache.getEntry();
            }
            return null;
        }
    }

    class EntryCache {
        private final int index;
        private final T entry;

        public EntryCache(int index, T entry) {
            this.index = index;
            this.entry = entry;
        }

        public int getIndex() {
            return index;
        }

        public T getEntry() {
            return entry;
        }
    }
}