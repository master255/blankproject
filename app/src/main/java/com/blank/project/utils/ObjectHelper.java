package com.blank.project.utils;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectHelper {

    public static String convertToString(final Serializable object) {
        CustomByteArrayOutputStream bo = new CustomByteArrayOutputStream(0, true);
        try {
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(object);
            so.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(Base64.encode(bo.getData(), 0, bo.size(), Base64.DEFAULT));
    }

    public static byte[] convertToBytes(final Serializable object) {
        CustomByteArrayOutputStream bo = new CustomByteArrayOutputStream(0, true);
        try {
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(object);
            so.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bo.getData();
    }

    public static Object convertFromString(final String objectAsString, Object defaultValue) {
        if (objectAsString != null) {
            try {
                return new ObjectInputStream(new ByteArrayInputStream(Base64.decode(objectAsString.getBytes(), Base64.DEFAULT))).readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }

    public static Object convertFromBytes(final byte[] objectAsBytes, Object defaultValue) {
        if (objectAsBytes != null) {
            try {
                return new ObjectInputStream(new ByteArrayInputStream(objectAsBytes)).readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return defaultValue;
    }
}
