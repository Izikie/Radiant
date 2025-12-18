package net.minecraft.network.packet.impl.play.server;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class S37PacketStatistics implements Packet<INetHandlerPlayClient> {
    private Object2IntMap<StatBase> field_148976_a;

    public S37PacketStatistics() {
    }

    public S37PacketStatistics(Object2IntMap<StatBase> p_i45173_1_) {
        this.field_148976_a = p_i45173_1_;
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleStatistics(this);
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        int i = buf.readVarInt();
        this.field_148976_a = new Object2IntOpenHashMap<>();

        for (int j = 0; j < i; ++j) {
            StatBase statbase = StatList.getOneShotStat(buf.readString(32767));
            int k = buf.readVarInt();

            if (statbase != null) {
                this.field_148976_a.put(statbase, k);
            }
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeVarInt(this.field_148976_a.size());

        for (Entry<StatBase, Integer> entry : this.field_148976_a.entrySet()) {
            buf.writeString(entry.getKey().statId);
            buf.writeVarInt(entry.getValue());
        }
    }

    public Object2IntMap<StatBase> func_148974_c() {
        return this.field_148976_a;
    }
}
