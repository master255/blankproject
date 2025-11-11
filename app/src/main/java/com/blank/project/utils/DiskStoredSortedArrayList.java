package com.blank.project.utils;


import androidx.annotation.NonNull;

import com.blank.project.Constants;
import com.blank.project.models.MapEntrySorted;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;

public class DiskStoredSortedArrayList<T> extends ArrayList<T> {

    private final int bufferSize;
    private String cacheFilePath, cacheIndexFilePath;
    private RandomAccessFile cacheFile;
    private final MapEntries mapEntries = new MapEntries();
    private final EntryCaches entryCaches = new EntryCaches();

    public DiskStoredSortedArrayList() {
        bufferSize = 30;
    }

    public DiskStoredSortedArrayList(int bufferSize, String cacheFolderPath, String cacheFilePath, String cacheIndexFilePath) {
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
//                final Object object = ObjectHelper.convertFromBytes(buf, null);
//                if (object != null) {
//                    if (!(object instanceof MapEntries) &&  object instanceof ArrayList) {
//                        if (((ArrayList<?>) object).get(0) instanceof MapEntry) {
//                            final ArrayList<MapEntry> oldMapEntryes = (ArrayList<MapEntry>) object;
//                            for (int i = 0; i < oldMapEntryes.size(); i++) {
//                                final MapEntry mapEntry = oldMapEntryes.get(i);
//                                final MapEntrySorted mapEntrySorted = new MapEntrySorted(mapEntry.getStartByte(), mapEntry.getLength());
//                                mapEntrySorted.setId(i);
//                                mapEntries.add(mapEntrySorted);
//                            }
//                            saveIndex();
//                        }
//                    } else
//                        mapEntries.addAll((MapEntries) object);
//                }
                final MapEntries mapEntryArrayList = (MapEntries) ObjectHelper.convertFromBytes(buf, null);
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
//                final Object object = ObjectHelper.convertFromBytes(buf, null);
//                if (object != null) {
//                    if (!(object instanceof MapEntries) &&  object instanceof ArrayList) {
//                        if (((ArrayList<?>) object).get(0) instanceof MapEntry) {
//                            final ArrayList<MapEntry> oldMapEntryes = (ArrayList<MapEntry>) object;
//                            for (int i = 0; i < oldMapEntryes.size(); i++) {
//                                final MapEntry mapEntry = oldMapEntryes.get(i);
//                                final MapEntrySorted mapEntrySorted = new MapEntrySorted(mapEntry.getStartByte(), mapEntry.getLength());
//                                mapEntrySorted.setId(i);
//                                mapEntries.add(mapEntrySorted);
//                            }
//                            saveIndex();
//                        }
//                    } else
//                        mapEntries.addAll((MapEntries) object);
//                }
                final MapEntries mapEntryArrayList = (MapEntries) ObjectHelper.convertFromBytes(buf, null);
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
                    final MapEntrySorted lastMapEntrySorted = mapEntries.get(mapEntries.size() - 1);
                    startByte = lastMapEntrySorted.getStartByte() + lastMapEntrySorted.getLength();
                }
                final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
                cacheFile.seek(startByte);
                cacheFile.write(bytes);
                for (int i = 0; i < entryCaches.size(); i++) {
                    final EntryCache entryCache = entryCaches.get(i);
                    entryCache.setId(entryCache.getId() + 1);
                }
                entryCaches.add(new EntryCache(0, t));
                if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                for (int i = 0; i < mapEntries.size(); i++) {
                    final MapEntrySorted mapEntrySorted = mapEntries.get(i);
                    mapEntrySorted.setId(mapEntrySorted.getId() + 1);
                }
                mapEntries.add(new MapEntrySorted(startByte, bytes.length));
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
                        final MapEntrySorted lastMapEntrySorted = mapEntries.get(mapEntries.size() - 1);
                        startByte = lastMapEntrySorted.getStartByte() + lastMapEntrySorted.getLength();
                    }
                    final byte[] bytes = ObjectHelper.convertToBytes((Serializable) element);
                    cacheFile.seek(startByte);
                    cacheFile.write(bytes);
                    for (int u = 0; u < entryCaches.size(); u++) {
                        final EntryCache entryCache = entryCaches.get(u);
                        entryCache.setId(entryCache.getId() + 1);
                    }
                    entryCaches.add(new EntryCache(0, element));
                    if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                    for (int u = 0; u < mapEntries.size(); u++) {
                        final MapEntrySorted mapEntrySorted = mapEntries.get(u);
                        mapEntrySorted.setId(mapEntrySorted.getId() + 1);
                    }
                    mapEntries.add(new MapEntrySorted(startByte, bytes.length));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public T get(int id) {
        synchronized (this) {
            final T object = entryCaches.search(id);
            if (object != null) return object;
            try {
                final MapEntrySorted mapEntrySorted = mapEntries.getById(id);
                if (mapEntrySorted != null) {
                    cacheFile.seek(mapEntrySorted.getStartByte());
                    final byte[] buf = new byte[mapEntrySorted.getLength()];
                    cacheFile.read(buf);
                    final T entry = (T) ObjectHelper.convertFromBytes(buf, null);
                    if (entry != null) {
                        entryCaches.add(new EntryCache(id, entry));
                        if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                    } else {
                        for (int i = id; i < mapEntries.size(); i++) {
                            mapEntries.remove(i);
                            i--;
                        }
                        saveIndex();
                    }
                    return entry;
                } else {
                    for (int i = id; i < mapEntries.size(); i++) {
                        mapEntries.remove(i);
                        i--;
                    }
                    saveIndex();
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    public ArrayList<T> subList(int fromId, int toId) {
        final ArrayList<T> result = new ArrayList<>();
        if (toId > mapEntries.size()) toId = mapEntries.size();
        if (fromId < 0) fromId = 0;
        for (int i = fromId; i < toId; i++) {
            final T object = entryCaches.search(i);
            if (object != null) {
                result.add(object);
                continue;
            }
            synchronized (this) {
                final MapEntrySorted mapEntrySorted = mapEntries.getById(i);
                try {
                    cacheFile.seek(mapEntrySorted.getStartByte());
                    final byte[] buf = new byte[mapEntrySorted.getLength()];
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
    public T remove(int id) {
        synchronized (this) {
            entryCaches.removeUntil(id);
            final MapEntrySorted mapEntrySorted = mapEntries.getById(id);
            if (mapEntrySorted == null) return null;
            final int index = mapEntries.indexOf(mapEntrySorted);
            mapEntries.removeById(id);
            for (int i = id + 1; i <= mapEntries.size(); i++) {
                final MapEntrySorted mapEntrySortedLocal = mapEntries.getById(i);
                mapEntrySortedLocal.setId(mapEntrySortedLocal.getId() - 1);
            }
            for (int i = index; i < mapEntries.size(); i++) {
                final MapEntrySorted mapEntrySortedLocal = mapEntries.get(i);
                mapEntrySortedLocal.setStartByte(mapEntrySortedLocal.getStartByte() - mapEntrySorted.getLength());
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
                    long allBytes = mapEntrySorted.getStartByte();
                    while ((bytesRead = fileTmpData.read(buf)) != -1) {
                        allBytes -= bytesRead;
                        if (allBytes > 0)
                            cacheFile.write(buf, 0, bytesRead);
                        else if (allBytes == 0) {
                            cacheFile.write(buf, 0, bytesRead);
                            fileTmpData.seek(mapEntrySorted.getStartByte() + mapEntrySorted.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            break;
                        } else {
                            cacheFile.write(buf, 0, (int) (bytesRead + allBytes));
                            fileTmpData.seek(mapEntrySorted.getStartByte() + mapEntrySorted.getLength());
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

    public boolean replaceElement(int id, T t) {
        synchronized (this) {
            entryCaches.removeUntil(id);
            final MapEntrySorted mapEntrySorted = mapEntries.getById(id);
            if (mapEntrySorted == null) return false;
            final int index = mapEntries.indexOf(mapEntrySorted);
            try {
                cacheFile.close();
                final File fileCache = new File(cacheFilePath);
                final File fileTmp = new File(cacheFilePath + "t");
                if (fileCache.renameTo(fileTmp)) {
                    cacheFile = new RandomAccessFile(fileCache, "rwd");
                    final RandomAccessFile fileTmpData = new RandomAccessFile(fileTmp, "rwd");
                    final byte[] buf = new byte[Constants.FILE_BUFFER_LENGTH];
                    int bytesRead;
                    long allBytes = mapEntrySorted.getStartByte();
                    while ((bytesRead = fileTmpData.read(buf)) != -1) {
                        allBytes -= bytesRead;
                        if (allBytes > 0)
                            cacheFile.write(buf, 0, bytesRead);
                        else if (allBytes == 0) {
                            cacheFile.write(buf, 0, bytesRead);
                            final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
                            cacheFile.write(bytes);
                            fileTmpData.seek(mapEntrySorted.getStartByte() + mapEntrySorted.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            mapEntrySorted.setLength(bytes.length);
                            long filePosition = mapEntrySorted.getStartByte() + mapEntrySorted.getLength();
                            for (int i = index + 1; i < mapEntries.size(); i++) {
                                final MapEntrySorted mapEntrySortedLocal = mapEntries.get(i);
                                mapEntrySortedLocal.setStartByte(filePosition);
                                filePosition = filePosition + mapEntrySortedLocal.getLength();
                            }
                            break;
                        } else {
                            cacheFile.write(buf, 0, (int) (bytesRead + allBytes));
                            final byte[] bytes = ObjectHelper.convertToBytes((Serializable) t);
                            cacheFile.write(bytes);
                            fileTmpData.seek(mapEntrySorted.getStartByte() + mapEntrySorted.getLength());
                            while ((bytesRead = fileTmpData.read(buf)) != -1)
                                cacheFile.write(buf, 0, bytesRead);
                            mapEntrySorted.setLength(bytes.length);
                            long filePosition = mapEntrySorted.getStartByte() + mapEntrySorted.getLength();
                            for (int i = index + 1; i < mapEntries.size(); i++) {
                                final MapEntrySorted mapEntrySortedLocal = mapEntries.get(i);
                                mapEntrySortedLocal.setStartByte(filePosition);
                                filePosition = filePosition + mapEntrySortedLocal.getLength();
                            }
                            break;
                        }
                    }
                    fileTmpData.close();
                    fileTmp.delete();
                    entryCaches.add(new EntryCache(id, t));
                    if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
            return true;
        }
    }

    public void setFirst(int id) {
        synchronized (this) {
            final EntryCache entryCache = entryCaches.getById(id);
            entryCache.setId(-1);
            for (int i = id - 1; i > -1; i--) {
                final EntryCache entryCacheLocal = entryCaches.getById(i);
                if (entryCacheLocal != null)
                    entryCacheLocal.setId(entryCacheLocal.getId() + 1);
            }
            entryCache.setId(0);
            final MapEntrySorted mapEntrySorted = mapEntries.getById(id);
            mapEntrySorted.setId(-1);
            for (int i = id - 1; i > -1; i--) {
                final MapEntrySorted mapEntrySortedLocal = mapEntries.getById(i);
                mapEntrySortedLocal.setId(mapEntrySortedLocal.getId() + 1);
            }
            mapEntrySorted.setId(0);
        }
    }

    public int getId(T element) {
        synchronized (this) {
            for (int i = 0; i < entryCaches.size(); i++) {
                final EntryCache entryCache = entryCaches.get(i);
                if (entryCache.getEntry().equals(element)) return entryCache.getId();
            }
            for (int i = 0; i < mapEntries.size(); i++) {
                final MapEntrySorted mapEntrySorted = mapEntries.get(i);
                try {
                    cacheFile.seek(mapEntrySorted.getStartByte());
                    final byte[] buf = new byte[mapEntrySorted.getLength()];
                    cacheFile.read(buf);
                    final T entry = (T) ObjectHelper.convertFromBytes(buf, null);
                    if (entry == null) break;
                    else if (element.equals(entry)) return mapEntrySorted.getId();
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
            return -1;
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

    private class EntryCaches extends ArrayList<EntryCache> {
        public T search(int id) {
            for (int i = size() - 1; i > -1; i--) {
                final EntryCache entryCache = get(i);
                if (id == entryCache.getId())
                    return entryCache.getEntry();
            }
            return null;
        }

        public void removeUntil(int id) {
            for (int i = size() - 1; i > -1; i--) {
                if (get(i).getId() >= id)
                    remove(i);
            }
        }

        public void removeById(int id) {
            for (int i = 0; i < size(); i++) {
                final EntryCache entryCache = get(i);
                if (entryCache.getId() == id) {
                    entryCaches.remove(i);
                    return;
                }
            }
        }

        public EntryCache getById(int id) {
            for (int i = size() - 1; i > -1; i--) {
                final EntryCache entryCache = get(i);
                if (id == entryCache.getId())
                    return entryCache;
            }
            return null;
        }
    }

    private class EntryCache {
        private int id;
        private final T entry;

        public EntryCache(int id, T entry) {
            this.id = id;
            this.entry = entry;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public T getEntry() {
            return entry;
        }
    }

    private static class MapEntries extends ArrayList<MapEntrySorted> {
        private static final long serialVersionUID = 1;

        public MapEntrySorted getById(int id) {
            for (int i = size() - 1; i > -1; i--) {
                final MapEntrySorted mapEntrySorted = get(i);
                if (id == mapEntrySorted.getId())
                    return mapEntrySorted;
            }
            return null;
        }

        public MapEntrySorted removeById(int id) {
            for (int i = size() - 1; i > -1; i--) {
                final MapEntrySorted mapEntrySorted = get(i);
                if (id == mapEntrySorted.getId()) {
                    remove(i);
                    return mapEntrySorted;
                }
            }
            return null;
        }
    }
}