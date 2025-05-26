package net.minecraft.network;

import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.src.Config;
import net.minecraft.util.IThreadListener;

public class PacketThreadUtil {
    public static int lastDimensionId = Integer.MIN_VALUE;

    public static <T extends INetHandler> void checkThreadAndEnqueue(Packet<T> packet, T handler, IThreadListener listener) throws ThreadQuickExitException {
        if (!listener.isCallingFromMinecraftThread()) {
            listener.addScheduledTask(() -> {
                PacketThreadUtil.clientPreProcessPacket(packet);
                packet.processPacket(handler);
            });
            throw ThreadQuickExitException.INSTANCE;
        } else {
            clientPreProcessPacket(packet);
        }
    }

    protected static void clientPreProcessPacket(Packet<?> packet) {
        if (packet instanceof S08PacketPlayerPosLook) {
            Config.getRenderGlobal().onPlayerPositionSet();
        }

        if (packet instanceof S07PacketRespawn s07) {
            lastDimensionId = s07.getDimensionID();
        } else if (packet instanceof S01PacketJoinGame s01) {
            lastDimensionId = s01.getDimension();
        } else {
            lastDimensionId = Integer.MIN_VALUE;
        }
    }
}
