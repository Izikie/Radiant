package net.radiant.graalvm;

import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketDirection;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

@SuppressWarnings("unused")
public class PacketReflectRegister implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (NetworkState state : NetworkState.values()) {
            for (PacketDirection direction : PacketDirection.values()) {
                for (int id = 0;; id++) {
                    Class<? extends Packet<?>> packetClass = state.getPacketClass(direction, id);
                    if (packetClass == null) {
                        break;
                    }
                    RuntimeReflection.register(packetClass);
                    RuntimeReflection.registerAllConstructors(packetClass);
                }
            }
        }
    }
}
