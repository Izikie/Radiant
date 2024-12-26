package net.optifine.reflect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ReflectorRaw {
    public static Field getField(Class cls, Class fieldType) {
        try {
            Field[] afield = cls.getDeclaredFields();

            for (Field field : afield) {
                if (field.getType() == fieldType) {
                    field.setAccessible(true);
                    return field;
                }
            }

            return null;
        } catch (Exception var5) {
            return null;
        }
    }

    public static Field[] getFields(Class cls, Class fieldType) {
        try {
            Field[] afield = cls.getDeclaredFields();
            return getFields(afield, fieldType);
        } catch (Exception var3) {
            return null;
        }
    }

    public static Field[] getFields(Field[] fields, Class fieldType) {
        try {
            List list = new ArrayList();

            for (Field field : fields) {
                if (field.getType() == fieldType) {
                    field.setAccessible(true);
                    list.add(field);
                }
            }

            return (Field[]) list.toArray(new Field[list.size()]);
        } catch (Exception var5) {
            return null;
        }
    }

    public static Field getField(Class cls, Class fieldType, int index) {
        Field[] afield = getFields(cls, fieldType);
        return index >= 0 && index < afield.length ? afield[index] : null;
    }

    public static Object getFieldValue(Object obj, Class cls, Class fieldType) {
        ReflectorField reflectorfield = getReflectorField(cls, fieldType);
        return reflectorfield == null ? null : (!reflectorfield.exists() ? null : Reflector.getFieldValue(obj, reflectorfield));
    }

    public static Object getFieldValue(Object obj, Class cls, Class fieldType, int index) {
        ReflectorField reflectorfield = getReflectorField(cls, fieldType, index);
        return reflectorfield == null ? null : (!reflectorfield.exists() ? null : Reflector.getFieldValue(obj, reflectorfield));
    }

    public static boolean setFieldValue(Object obj, Class cls, Class fieldType, Object value) {
        ReflectorField reflectorfield = getReflectorField(cls, fieldType);
        return reflectorfield == null ? false : (!reflectorfield.exists() ? false : Reflector.setFieldValue(obj, reflectorfield, value));
    }

    public static boolean setFieldValue(Object obj, Class cls, Class fieldType, int index, Object value) {
        ReflectorField reflectorfield = getReflectorField(cls, fieldType, index);
        return reflectorfield == null ? false : (!reflectorfield.exists() ? false : Reflector.setFieldValue(obj, reflectorfield, value));
    }

    public static ReflectorField getReflectorField(Class cls, Class fieldType) {
        Field field = getField(cls, fieldType);

        if (field == null) {
            return null;
        } else {
            ReflectorClass reflectorclass = new ReflectorClass(cls);
            return new ReflectorField(reflectorclass, field.getName());
        }
    }

    public static ReflectorField getReflectorField(Class cls, Class fieldType, int index) {
        Field field = getField(cls, fieldType, index);

        if (field == null) {
            return null;
        } else {
            ReflectorClass reflectorclass = new ReflectorClass(cls);
            return new ReflectorField(reflectorclass, field.getName());
        }
    }
}
