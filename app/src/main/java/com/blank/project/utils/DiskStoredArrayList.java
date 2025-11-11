package com.blank.project.utils;

import androidx.annotation.NonNull;

import com.blank.project.Constants;
import com.blank.project.models.MapEntry;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;

public class DiskStoredArrayList<T> extends ArrayList<T> {

    private final int bufferSize;
    private String cacheFilePath, cacheIndexFilePath;
    private RandomAccessFile cacheFile;
    private final ArrayList<MapEntry> mapEntries = new ArrayList<>();
    private final EntryCaches entryCaches = new EntryCaches();

    public DiskStoredArrayList() {
        bufferSize = 30;
    }

    public DiskStoredArrayList(int bufferSize, String cacheFolderPath, String cacheFilePath, String cacheIndexFilePath) {
        this.bufferSize = bufferSize;
        this.cacheFilePath = cacheFilePath;
        this.cacheIndexFilePath = cacheIndexFilePath;
        final File cacheFolderFile = new File(cacheFolderPath);
        if (!cacheFolderFile.exists()) cacheFolderFile.mkdirs();
        try {
            cacheFile = new RandomAccessFile(cacheFilePath, "rwd");
            final RandomAccessFile cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            if (cacheIndexFile.length() > 0) {
                final byte[] buf = new byte[(int) cacheIndexFile.length()];
                cacheIndexFile.read(buf);
                final ArrayList<MapEntry> mapEntryArrayList = (ArrayList<MapEntry>) ObjectHelper.convertFromBytes(buf, null);
                if (mapEntryArrayList != null)
                    mapEntries.addAll(mapEntryArrayList);
            }
            cacheIndexFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadList(String cacheFolderPath, String cacheFilePath, String cacheIndexFilePath) {
        if (cacheFilePath != null && cacheFile != null) freeResourcesClose();
        this.cacheFilePath = cacheFilePath;
        this.cacheIndexFilePath = cacheIndexFilePath;
        final File cacheFolderFile = new File(cacheFolderPath);
        if (!cacheFolderFile.exists()) cacheFolderFile.mkdirs();
        try {
            cacheFile = new RandomAccessFile(cacheFilePath, "rwd");
            final RandomAccessFile cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            if (cacheIndexFile.length() > 0) {
                final byte[] buf = new byte[(int) cacheIndexFile.length()];
                cacheIndexFile.read(buf);
                final ArrayList<MapEntry> mapEntryArrayList = (ArrayList<MapEntry>) ObjectHelper.convertFromBytes(buf, null);
                if (mapEntryArrayList != null)
                    mapEntries.addAll(mapEntryArrayList);
            }
            cacheIndexFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int size() {
        return mapEntries.size();
    }

    @Override
    public boolean isEmpty() {
        return mapEntries.isEmpty();
    }

    @Override
    public boolean add(T t) {
        synchronized (this) {
            try {
                final long startByte;
                if (mapEntries.isEmpty())
                    startByte = 0;
                else {
                    final MapEntry lastMapEntry = mapEntries.get(mapEntries.size() - 1);
                    startByte = lastMapEntry.getStartByte() + lastMapEntry.getLength();
                }
                final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
                cacheFile.seek(startByte);
                cacheFile.write(bytes);
                entryCaches.add(new EntryCache(mapEntries.size(), t));
                if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                mapEntries.add(new MapEntry(startByte, bytes.length));
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public boolean addAllElements(@NonNull ArrayList<? extends T> c) {
        synchronized (this) {
            for (int i = 0; i < c.size(); i++) {
                final T element = c.get(i);
                try {
                    final long startByte;
                    if (mapEntries.isEmpty())
                        startByte = 0;
                    else {
                        final MapEntry lastMapEntry = mapEntries.get(mapEntries.size() - 1);
                        startByte = lastMapEntry.getStartByte() + lastMapEntry.getLength();
                    }
                    final byte[] bytes = ObjectHelper.convertToBytes((Serializable) element);
                    cacheFile.seek(startByte);
                    cacheFile.write(bytes);
                    entryCaches.add(new EntryCache(mapEntries.size(), element));
                    if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                    mapEntries.add(new MapEntry(startByte, bytes.length));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public T get(int index) {
        synchronized (this) {
            final T object = entryCaches.search(index);
            if (object != null) return object;
            try {
                final MapEntry mapEntry = mapEntries.get(index);
                cacheFile.seek(mapEntry.getStartByte());
                final byte[] buf = new byte[mapEntry.getLength()];
                cacheFile.read(buf);
                final T entry = (T) ObjectHelper.convertFromBytes(buf, null);
                if (entry != null) {
                    entryCaches.add(new EntryCache(index, entry));
                    if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                } else {
                    for (int i = index; i < mapEntries.size(); i++) {
                        mapEntries.remove(i);
                        i--;
                    }
                    saveIndex();
                }
                return entry;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    public ArrayList<T> subList(int fromIndex, int toIndex) {
        final ArrayList<T> result = new ArrayList<>();
        synchronized (this) {
            if (toIndex > mapEntries.size()) toIndex = mapEntries.size();
            if (fromIndex < 0) fromIndex = 0;
            for (int i = fromIndex; i < toIndex; i++) {
                final T object = entryCaches.search(i);
                if (object != null) {
                    result.add(object);
                    continue;
                }
                final MapEntry mapEntry = mapEntries.get(i);
                try {
                    cacheFile.seek(mapEntry.getStartByte());
                    final byte[] buf = new byte[mapEntry.getLength()];
                    cacheFile.read(buf);
                    final T entry = (T) ObjectHelper.convertFromBytes(buf, null);
                    if (entry != null)
                        result.add(entry);
                    else return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    return result;
                }
            }
        }
        return result;
    }

    @Override
    public boolean remove(Object element) {
        synchronized (this) {
            for (int i = 0; i < mapEntries.size(); i++) {
                final T elementLocal = get(i);
                if (element.equals(elementLocal)) {
                    remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public T remove(int index) {
        synchronized (this) {
            entryCaches.removeUntil(index);
            final MapEntry mapEntry = mapEntries.remove(index);
            if (mapEntry == null) return null;
            for (int i = index; i < mapEntries.size(); i++) {
                final MapEntry mapEntryLocal = mapEntries.get(i);
                mapEntryLocal.setStartByte(mapEntryLocal.getStartByte() - mapEntry.getLength());
            }
            try {
                cacheFile.close();
                final File fileCache = new File(cacheFilePath);
                final File fileTmp = new File(cacheFilePath + "t");
                if (fileCache.renameTo(fileTmp)) {
                    cacheFile = new RandomAccessFile(fileCache, "rwd");
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
                            fileTmpData.seek(mapEntry.getStartByte() + mapEntry.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            break;
                        } else {
                            cacheFile.write(buf, 0, (int) (bytesRead + allBytes));
                            fileTmpData.seek(mapEntry.getStartByte() + mapEntry.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            break;
                        }
                    }
                    fileTmpData.close();
                    fileTmp.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean replaceElement(int index, T t) {
        synchronized (this) {
            entryCaches.replaceElement(index, t);
            final MapEntry mapEntry = mapEntries.get(index);
            if (mapEntry == null) return false;
            try {
                cacheFile.close();
                final File fileCache = new File(cacheFilePath);
                final File fileTmp = new File(cacheFilePath + "t");
                if (fileCache.renameTo(fileTmp)) {
                    cacheFile = new RandomAccessFile(fileCache, "rwd");
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
                            final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
                            cacheFile.write(bytes);
                            fileTmpData.seek(mapEntry.getStartByte() + mapEntry.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            mapEntry.setLength(bytes.length);
                            long filePosition = mapEntry.getStartByte() + mapEntry.getLength();
                            for (int i = index + 1; i < mapEntries.size(); i++) {
                                final MapEntry mapEntryLocal = mapEntries.get(i);
                                mapEntryLocal.setStartByte(filePosition);
                                filePosition = filePosition + mapEntryLocal.getLength();
                            }
                            break;
                        } else {
                            cacheFile.write(buf, 0, (int) (bytesRead + allBytes));
                            final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
                            cacheFile.write(bytes);
                            fileTmpData.seek(mapEntry.getStartByte() + mapEntry.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            mapEntry.setLength(bytes.length);
                            long filePosition = mapEntry.getStartByte() + mapEntry.getLength();
                            for (int i = index + 1; i < mapEntries.size(); i++) {
                                final MapEntry mapEntryLocal = mapEntries.get(i);
                                mapEntryLocal.setStartByte(filePosition);
                                filePosition = filePosition + mapEntryLocal.getLength();
                            }
                            break;
                        }
                    }
                    fileTmpData.close();
                    fileTmp.delete();
                    entryCaches.add(new EntryCache(index, t));
                    if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public void saveIndex() {
        try {
            final RandomAccessFile cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            cacheIndexFile.setLength(0);
            cacheIndexFile.write(ObjectHelper.convertToBytes(mapEntries));
            cacheIndexFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clear() {
        synchronized (this) {
            mapEntries.clear();
            entryCaches.clear();
            try {
                cacheFile.setLength(0);
                final RandomAccessFile cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
                cacheIndexFile.setLength(0);
                cacheIndexFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearClose() {
        mapEntries.clear();
        entryCaches.clear();
        try {
            cacheFile.setLength(0);
            cacheFile.close();
            final RandomAccessFile cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            cacheIndexFile.setLength(0);
            cacheIndexFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        entryCaches.clear();
        try {
            cacheFile.close();
            final RandomAccessFile cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            cacheIndexFile.setLength(0);
            cacheIndexFile.write(ObjectHelper.convertToBytes(mapEntries));
            cacheIndexFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void freeResourcesClose() {
        synchronized (this) {
            mapEntries.clear();
            entryCaches.clear();
            try {
                cacheFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isEmptyArrayList() {
        return super.isEmpty();
    }

    public int sizeArrayList() {
        return super.size();
    }

    public boolean addArrayList(T element) {
        return super.add(element);
    }

    public T getArrayList(int index) {
        return super.get(index);
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

        public void removeUntil(int index) {
            for (int i = size() - 1; i > -1; i--) {
                if (get(i).getIndex() >= index)
                    remove(i);
            }
        }

        public void replaceElement(int index, T t) {
            for (int i = size() - 1; i > -1; i--) {
                final EntryCache entryCache = get(i);
                if (entryCache.getIndex() == index) {
                    entryCache.setEntry(t);
                    return;
                }
            }
        }
    }

    class EntryCache {
        private final int index;
        private T entry;

        public EntryCache(int index, T entry) {
            this.index = index;
            this.entry = entry;
        }

        public int getIndex() {
            return index;
        }

        public void setEntry(T entry) {
            this.entry = entry;
        }

        public T getEntry() {
            return entry;
        }
    }
}