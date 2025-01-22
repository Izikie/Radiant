package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.util.Direction;

public abstract class BlockDirectional extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing", Direction.Plane.HORIZONTAL);

    protected BlockDirectional(Material materialIn) {
        super(materialIn);
    }

    protected BlockDirectional(Material p_i46398_1_, MapColor p_i46398_2_) {
        super(p_i46398_1_, p_i46398_2_);
    }
}
