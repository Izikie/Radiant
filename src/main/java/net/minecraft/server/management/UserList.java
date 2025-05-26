package net.minecraft.server.management;

import com.google.common.io.Files;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UserList<K, V extends UserListEntry<K>> {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected final Gson gson;
    private final File saveFile;
    private final Map<String, V> values = new HashMap<>();
    private boolean lanServer = true;
    private static final ParameterizedType SAVE_FILE_FORMAT = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[]{UserListEntry.class};
        }

        public Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };

    public UserList(File saveFile) {
        this.saveFile = saveFile;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeHierarchyAdapter(UserListEntry.class, new Serializer())
                .create();
    }

    public boolean isLanServer() {
        return this.lanServer;
    }

    public void setLanServer(boolean state) {
        this.lanServer = state;
    }

    public void addEntry(V entry) {
        this.values.put(this.getObjectKey(entry.getValue()), entry);

        try {
            this.writeChanges();
        } catch (IOException exception) {
            LOGGER.warn("Could not save the list after adding a user.", exception);
        }
    }

    public V getEntry(K obj) {
        this.removeExpired();
        return this.values.get(this.getObjectKey(obj));
    }

    public void removeEntry(K entry) {
        this.values.remove(this.getObjectKey(entry));

        try {
            this.writeChanges();
        } catch (IOException exception) {
            LOGGER.warn("Could not save the list after removing a user.", exception);
        }
    }

    public String[] getKeys() {
        return this.values.keySet().toArray(new String[0]);
    }

    protected String getObjectKey(K obj) {
        return obj.toString();
    }

    protected boolean hasEntry(K entry) {
        return this.values.containsKey(this.getObjectKey(entry));
    }

    private void removeExpired() {
        List<K> list = new ArrayList<>();

        for (V v : this.values.values()) {
            if (v.hasBanExpired()) {
                list.add(v.getValue());
            }
        }

        for (K k : list) {
            this.values.remove(k);
        }
    }

    protected UserListEntry<K> createEntry(JsonObject entryData) {
        return new UserListEntry<>(null, entryData);
    }

    protected Map<String, V> getValues() {
        return this.values;
    }

    public void writeChanges() throws IOException {
        Collection<V> collection = this.values.values();
        String s = this.gson.toJson(collection);
        BufferedWriter bufferedwriter = null;

        try {
            bufferedwriter = Files.newWriter(this.saveFile, StandardCharsets.UTF_8);
            bufferedwriter.write(s);
        } finally {
            IOUtils.closeQuietly(bufferedwriter);
        }
    }

    class Serializer implements JsonDeserializer<UserListEntry<K>>, JsonSerializer<UserListEntry<K>> {
        private Serializer() {
        }

        public JsonElement serialize(UserListEntry<K> p_serialize_1_, Type type, JsonSerializationContext ctx) {
            JsonObject jsonobject = new JsonObject();
            p_serialize_1_.onSerialization(jsonobject);
            return jsonobject;
        }

        public UserListEntry<K> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            if (jsonElement.isJsonObject()) {
                JsonObject jsonobject = jsonElement.getAsJsonObject();
                return UserList.this.createEntry(jsonobject);
            } else {
                return null;
            }
        }
    }
}
