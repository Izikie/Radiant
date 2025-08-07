package net.minecraft.world.gen.structure;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapGenStructureIO {
    private static final Logger LOGGER = LoggerFactory.getLogger(MapGenStructureIO.class);
    private static final Map<String, Class<? extends StructureStart>> startNameToClassMap = new HashMap<>();
    private static final Map<Class<? extends StructureStart>, String> startClassToNameMap = new HashMap<>();
    private static final Map<String, Class<? extends StructureComponent>> componentNameToClassMap = new HashMap<>();
    private static final Map<Class<? extends StructureComponent>, String> componentClassToNameMap = new HashMap<>();

    private static void registerStructure(Class<? extends StructureStart> startClass, String structureName) {
        startNameToClassMap.put(structureName, startClass);
        startClassToNameMap.put(startClass, structureName);
    }

    static void registerStructureComponent(Class<? extends StructureComponent> componentClass, String componentName) {
        componentNameToClassMap.put(componentName, componentClass);
        componentClassToNameMap.put(componentClass, componentName);
    }

    public static String getStructureStartName(StructureStart start) {
        return startClassToNameMap.get(start.getClass());
    }

    public static String getStructureComponentName(StructureComponent component) {
        return componentClassToNameMap.get(component.getClass());
    }

    public static StructureStart getStructureStart(NBTTagCompound tagCompound, World worldIn) {
        StructureStart structurestart = null;

        try {
            Class<? extends StructureStart> oclass = startNameToClassMap.get(tagCompound.getString("id"));

            if (oclass != null) {
                structurestart = oclass.getConstructor().newInstance();
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed Start with id {}", tagCompound.getString("id"));
            exception.printStackTrace();
        }

        if (structurestart != null) {
            structurestart.readStructureComponentsFromNBT(worldIn, tagCompound);
        } else {
            LOGGER.warn("Skipping Structure with id {}", tagCompound.getString("id"));
        }

        return structurestart;
    }

    public static StructureComponent getStructureComponent(NBTTagCompound tagCompound, World worldIn) {
        StructureComponent structurecomponent = null;

        try {
            Class<? extends StructureComponent> oclass = componentNameToClassMap.get(tagCompound.getString("id"));

            if (oclass != null) {
                structurecomponent = oclass.getConstructor().newInstance();
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed Piece with id {}", tagCompound.getString("id"));
            exception.printStackTrace();
        }

        if (structurecomponent != null) {
            structurecomponent.readStructureBaseNBT(worldIn, tagCompound);
        } else {
            LOGGER.warn("Skipping Piece with id {}", tagCompound.getString("id"));
        }

        return structurecomponent;
    }

    public static List<Class<? extends StructureStart>> getStartClasses() {
        return new ArrayList<>(startNameToClassMap.values());
    }

    public static List<Class<? extends StructureComponent>> getComponentClasses() {
        return new ArrayList<>(componentNameToClassMap.values());
    }

    static {
        registerStructure(StructureMineshaftStart.class, "Mineshaft");
        registerStructure(MapGenVillage.Start.class, "Village");
        registerStructure(MapGenNetherBridge.Start.class, "Fortress");
        registerStructure(MapGenStronghold.Start.class, "Stronghold");
        registerStructure(MapGenScatteredFeature.Start.class, "Temple");
        registerStructure(StructureOceanMonument.StartMonument.class, "Monument");
        StructureMineshaftPieces.registerStructurePieces();
        StructureVillagePieces.registerVillagePieces();
        StructureNetherBridgePieces.registerNetherFortressPieces();
        StructureStrongholdPieces.registerStrongholdPieces();
        ComponentScatteredFeaturePieces.registerScatteredFeaturePieces();
        StructureOceanMonumentPieces.registerOceanMonumentPieces();
    }
}
