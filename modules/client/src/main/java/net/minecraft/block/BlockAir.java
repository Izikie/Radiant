package net.minecraft.block;

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.IdentityHashMap;
import java.util.Map;

public class BlockAir extends Block {
    private static final Reference2IntMap<Block> MAP_ORIGINAL_OPACITY = new Reference2IntOpenHashMap<>();

    protected BlockAir() {
        super(Material.AIR);
    }

    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return false;
    }

    @Override
    public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
    }

    @Override
    public boolean isReplaceable(World worldIn, BlockPos pos) {
        return true;
    }

    public static void setLightOpacity(Block p_setLightOpacity_0_, int p_setLightOpacity_1_) {
        if (!MAP_ORIGINAL_OPACITY.containsKey(p_setLightOpacity_0_)) {
            MAP_ORIGINAL_OPACITY.put(p_setLightOpacity_0_, p_setLightOpacity_0_.lightOpacity);
        }

        p_setLightOpacity_0_.lightOpacity = p_setLightOpacity_1_;
    }

    public static void restoreLightOpacity(Block p_restoreLightOpacity_0_) {
        if (MAP_ORIGINAL_OPACITY.containsKey(p_restoreLightOpacity_0_)) {
            int i = MAP_ORIGINAL_OPACITY.get(p_restoreLightOpacity_0_);
            setLightOpacity(p_restoreLightOpacity_0_, i);
        }
    }
}
