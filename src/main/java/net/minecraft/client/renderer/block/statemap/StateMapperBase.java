package net.minecraft.client.renderer.block.statemap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.model.ModelResourceLocation;

public abstract class StateMapperBase implements IStateMapper {
    protected final Map<IBlockState, ModelResourceLocation> mapStateModelLocations = new LinkedHashMap<>();

    public String getPropertyString(Map<IProperty, Comparable> p_178131_1_) {
        StringBuilder stringbuilder = new StringBuilder();

        for (Entry<IProperty, Comparable> entry : p_178131_1_.entrySet()) {
            if (!stringbuilder.isEmpty()) {
                stringbuilder.append(",");
            }

            IProperty iproperty = entry.getKey();
            Comparable comparable = entry.getValue();
            stringbuilder.append(iproperty.getName());
            stringbuilder.append("=");
            stringbuilder.append(iproperty.getName(comparable));
        }

        if (stringbuilder.isEmpty()) {
            stringbuilder.append("normal");
        }

        return stringbuilder.toString();
    }

    public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block blockIn) {
        for (IBlockState iblockstate : blockIn.getBlockState().getValidStates()) {
            this.mapStateModelLocations.put(iblockstate, this.getModelResourceLocation(iblockstate));
        }

        return this.mapStateModelLocations;
    }

    protected abstract ModelResourceLocation getModelResourceLocation(IBlockState state);
}
