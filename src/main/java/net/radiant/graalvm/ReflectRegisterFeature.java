package net.radiant.graalvm;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketDirection;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

@SuppressWarnings("unused")
public class ReflectRegisterFeature implements Feature {

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

        for (int i = 0; i < 200; i++) {
            Class<? extends Entity> entityClass = EntityList.getClassFromID(i);
            if (entityClass != null) {
                RuntimeReflection.register(entityClass);
                RuntimeReflection.registerAllConstructors(entityClass);
            }
        }

        for (Class<? extends StructureStart> clazz : MapGenStructureIO.getStartClasses()) {
            RuntimeReflection.register(clazz);
            RuntimeReflection.registerAllConstructors(clazz);
        }

        for (Class<? extends StructureComponent> clazz : MapGenStructureIO.getComponentClasses()) {
            RuntimeReflection.register(clazz);
            RuntimeReflection.registerAllConstructors(clazz);
        }

    }
}
