package net.radiant.networkingRewrite;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.radiant.networkingRewrite.packet.api.C2SPacket;
import net.radiant.networkingRewrite.packet.api.S2CPacket;

import java.util.function.Supplier;

public enum NetworkState {
    HANDSHAKE,
    PLAY,
    STATUS,
    LOGIN;

    private static final int STATE_ID_MIN = -1;
    private static final int STATE_ID_MAX = 2;

    private static final Object2IntMap<Class<? extends C2SPacket>> c2sMap = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<Supplier<S2CPacket>> s2cMap = new Int2ObjectOpenHashMap<>();

    public void registerC2S(Class<? extends C2SPacket> cls) {
        if (c2sMap.containsKey(cls)) {
            throw new IllegalArgumentException("C2S already registered: " + cls);
        }

        c2sMap.put(cls, c2sMap.size());
    }

    public void registerS2C(Supplier<S2CPacket> supplier) {
        s2cMap.put(s2cMap.size(), supplier);
    }

    public static int getPacketId(C2SPacket packet) {
        return c2sMap.getOrDefault(packet.getClass(), -1);
    }

    public S2CPacket getPacket(int id) {
        Supplier<S2CPacket> supplier = s2cMap.get(id);

        if (supplier == null) {
            throw new IllegalArgumentException("Unknown S2C packet ID: " + id);
        }

        return supplier.get();
    }

    public int getStateId() {
        return this.ordinal() - 1;
    }

    public static NetworkState getById(int id) {
        if (id < STATE_ID_MIN || id > STATE_ID_MAX) {
            return null;
        }

        return values()[id - STATE_ID_MIN];
    }
}
