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
            cacheFile = new RandomAccessFile(cacheFilePath, "rwd");
            cacheIndexFile = new RandomAccessFile(cacheIndexFilePath, "rwd");
            if (cacheIndexFile.length() > 0) {
                final byte[] buf = new byte[(int) cacheIndexFile.length()];
                cacheIndexFile.read(buf);
                final ArrayList<MapEntry> mapEntryArrayList = (ArrayList<MapEntry>) ObjectHelper.convertFromBytes(buf, null);
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
        synchronized (this) {
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
    }

    public boolean addAllElements(@NonNull ArrayList<? extends T> c) {
        synchronized (this) {
            for (int i = 0; i < c.size(); i++) {
                final T element = c.get(i);
                try {
                    final long length = cacheFile.length();
                    final byte[] bytes = ObjectHelper.convertToBytes((Serializable) element);
                    cacheFile.seek(length);
                    cacheFile.write(bytes);
                    entryCaches.add(new EntryCache(mapEntries.size(), element));
                    if (entryCaches.size() > bufferSize) entryCaches.remove(0);
                    mapEntries.add(new MapEntry(length, bytes.length));
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
        final T object = entryCaches.search(index);
        if (object != null) return object;
        synchronized (this) {
            final MapEntry mapEntry = mapEntries.get(index);
            try {
                cacheFile.seek(mapEntry.getStartByte());
                final byte[] buf = new byte[mapEntry.getLength()];
                cacheFile.read(buf);
                final T entry = (T) ObjectHelper.convertFromBytes(buf, null);
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
    }


    public ArrayList<T> subList(int fromIndex, int toIndex) {
        final ArrayList<T> result = new ArrayList<>();
        if (toIndex > mapEntries.size()) toIndex = mapEntries.size();
        for (int i = fromIndex; i < toIndex; i++) {
            final T object = entryCaches.search(i);
            if (object != null) {
                result.add(object);
                continue;
            }
            synchronized (this) {
                final MapEntry mapEntry = mapEntries.get(i);
                try {
                    cacheFile.seek(mapEntry.getStartByte());
                    final byte[] buf = new byte[mapEntry.getLength()];
                    cacheFile.read(buf);
                    final T entry = (T) ObjectHelper.convertFromBytes(buf, null);
                    if (entry != null)
                        result.add(entry);
                    else return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Removing is not supported");
    }

    @Override
    public T remove(int index) {
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
        synchronized (this) {
            mapEntries.clear();
            try {
                cacheFile.setLength(0);
                cacheIndexFile.setLength(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    public void clearClose() {
        synchronized (this) {
            mapEntries.clear();
            entryCaches.clear();
            try {
                cacheFile.close();
                cacheIndexFile.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
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