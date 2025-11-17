package net.minecraft.network.packet.api;

import net.minecraft.network.INetHandler;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.packet.impl.play.server.S01PacketJoinGame;
import net.minecraft.network.packet.impl.play.server.S07PacketRespawn;
import net.minecraft.network.packet.impl.play.server.S08PacketPlayerPosLook;
import net.optifine.Config;
import net.minecraft.util.IThreadListener;

public class PacketThreadUtil {
    public static int lastDimensionId = Integer.MIN_VALUE;

    public static <T extends INetHandler> void checkThreadAndEnqueue(Packet<T> packet, T handler, IThreadListener listener) throws ThreadQuickExitException {
        if (!listener.isCallingFromMinecraftThread()) {
            listener.addScheduledTask(() -> {
                PacketThreadUtil.clientPreProcessPacket(packet);
                packet.handle(handler);
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

        lastDimensionId = switch (packet) {
            case S07PacketRespawn s07 -> s07.getDimensionID();
            case S01PacketJoinGame s01 -> s01.getDimension();
            default -> Integer.MIN_VALUE;
        };
    }
}
