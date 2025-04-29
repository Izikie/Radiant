package net.minecraft.network;

import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.src.Config;
import net.minecraft.util.IThreadListener;

public class PacketThreadUtil {
    public static int lastDimensionId = Integer.MIN_VALUE;

    public static <T extends INetHandler> void checkThreadAndEnqueue(final Packet<T> packet, final T p_180031_1_, IThreadListener p_180031_2_) throws ThreadQuickExitException {
        if (!p_180031_2_.isCallingFromMinecraftThread()) {
            p_180031_2_.addScheduledTask(() -> {
                PacketThreadUtil.clientPreProcessPacket(packet);
                packet.processPacket(p_180031_1_);
            });
            throw ThreadQuickExitException.INSTANCE;
        } else {
            clientPreProcessPacket(packet);
        }
    }

    protected static void clientPreProcessPacket(Packet packet) {
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
