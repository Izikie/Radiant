package net.radiant.json.adapter;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.writer.ObjectWriter;

import java.lang.reflect.Type;

public abstract class AbstractJsonAdapter<T> implements ObjectReader<T>, ObjectWriter<T> {

    @Override
    public abstract T readObject(JSONReader reader, Type fieldType, Object fieldName, long features);

    public abstract void writeObject(JSONWriter writer, T object, Object fieldName, Type fieldType, long features);

    @Override
    public void write(JSONWriter writer, Object object, Object fieldName, Type fieldType, long features) {
        this.writeObject(writer, (T) object, fieldName, fieldType, features);
    }

    @Override
    public long getFeatures() {
        return ObjectReader.super.getFeatures() | ObjectWriter.super.getFeatures();
    }
}
