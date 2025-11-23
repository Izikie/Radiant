package net.minecraft.network.packet.impl.play.server;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.impl.play.INetHandlerPlayClient;
import net.minecraft.util.CombatTracker;

import java.io.IOException;

public class S42PacketCombatEvent implements Packet<INetHandlerPlayClient> {
    public Event eventType;
    public int field_179774_b;
    public int field_179775_c;
    public int field_179772_d;
    public String deathMessage;

    public S42PacketCombatEvent() {
    }


    public S42PacketCombatEvent(CombatTracker combatTrackerIn, Event combatEventType) {
        this.eventType = combatEventType;
        EntityLivingBase entitylivingbase = combatTrackerIn.func_94550_c();

        switch (combatEventType) {
            case END_COMBAT:
                this.field_179772_d = combatTrackerIn.func_180134_f();
                this.field_179775_c = entitylivingbase == null ? -1 : entitylivingbase.getEntityId();
                break;

            case ENTITY_DIED:
                this.field_179774_b = combatTrackerIn.getFighter().getEntityId();
                this.field_179775_c = entitylivingbase == null ? -1 : entitylivingbase.getEntityId();
                this.deathMessage = combatTrackerIn.getDeathMessage().getUnformattedText();
        }
    }

    @Override
    public void read(PacketBuffer buf) throws IOException {
        this.eventType = buf.readEnum(Event.class);

        if (this.eventType == Event.END_COMBAT) {
            this.field_179772_d = buf.readVarInt();
            this.field_179775_c = buf.readInt();
        } else if (this.eventType == Event.ENTITY_DIED) {
            this.field_179774_b = buf.readVarInt();
            this.field_179775_c = buf.readInt();
            this.deathMessage = buf.readString(32767);
        }
    }

    @Override
    public void write(PacketBuffer buf) throws IOException {
        buf.writeEnum(this.eventType);

        if (this.eventType == Event.END_COMBAT) {
            buf.writeVarInt(this.field_179772_d);
            buf.writeInt(this.field_179775_c);
        } else if (this.eventType == Event.ENTITY_DIED) {
            buf.writeVarInt(this.field_179774_b);
            buf.writeInt(this.field_179775_c);
            buf.writeString(this.deathMessage);
        }
    }

    @Override
    public void handle(INetHandlerPlayClient handler) {
        handler.handleCombatEvent(this);
    }

    public enum Event {
        ENTER_COMBAT,
        END_COMBAT,
        ENTITY_DIED
    }
}
