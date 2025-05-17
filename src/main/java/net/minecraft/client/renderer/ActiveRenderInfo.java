package net.minecraft.client.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

// TOOD: UnProject use JOML and possibly make this better
public class ActiveRenderInfo {
    private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer OBJECTCOORDS = GLAllocation.createDirectFloatBuffer(3);
    private static Vec3 position = new Vec3(0.0D, 0.0D, 0.0D);
    private static float rotationX;
    private static float rotationXZ;
    private static float rotationZ;
    private static float rotationYZ;
    private static float rotationXY;

    public static void updateRenderInfo(EntityPlayer player, boolean invertRotation) {
        GlStateManager.getFloat(2982, MODELVIEW);
        GlStateManager.getFloat(2983, PROJECTION);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);

        float viewCenterX = (VIEWPORT.get(0) + VIEWPORT.get(2)) / 2.0f;
        float viewCenterY = (VIEWPORT.get(1) + VIEWPORT.get(3)) / 2.0f;

        Project.gluUnProject(viewCenterX, viewCenterY, 0.0F, MODELVIEW, PROJECTION, VIEWPORT, OBJECTCOORDS);

        position = new Vec3(OBJECTCOORDS.get(0), OBJECTCOORDS.get(1), OBJECTCOORDS.get(2));

        float rotationFactor = invertRotation ? -1.0f : 1.0f;
        float pitchRadians = (float) (player.rotationPitch * Math.PI / 180.0F);
        float yawRadians = (float) (player.rotationYaw * Math.PI / 180.0F);

        rotationX = MathHelper.cos(yawRadians) * rotationFactor;
        rotationZ = MathHelper.sin(yawRadians) * rotationFactor;
        rotationYZ = -rotationZ * MathHelper.sin(pitchRadians) * rotationFactor;
        rotationXY = rotationX * MathHelper.sin(pitchRadians) * rotationFactor;
        rotationXZ = MathHelper.cos(pitchRadians);
    }

    public static Vec3 projectViewFromEntity(Entity entity, double partialTicks) {
        double entityX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double entityY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double entityZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

        double viewpointX = entityX + position.xCoord;
        double viewpointY = entityY + position.yCoord;
        double viewpointZ = entityZ + position.zCoord;

        return new Vec3(viewpointX, viewpointY, viewpointZ);
    }

    public static Block getBlockAtEntityViewpoint(World world, Entity entity, float partialTicks) {
        Vec3 vec3 = projectViewFromEntity(entity, partialTicks);
        BlockPos blockPos = new BlockPos(vec3);
        IBlockState blockState = world.getBlockState(blockPos);
        Block block = blockState.getBlock();

        if (block.getMaterial().isLiquid()) {
            float f = 0.0F;

            if (blockState.getBlock() instanceof BlockLiquid) {
                f = BlockLiquid.getLiquidHeightPercent(blockState.getValue(BlockLiquid.LEVEL)) - 0.11111111F;
            }

            float f1 = (blockPos.getY() + 1) - f;

            if (vec3.yCoord >= f1) {
                block = world.getBlockState(blockPos.up()).getBlock();
            }
        }

        return block;
    }

    public static Vec3 getPosition() {
        return position;
    }

    public static float getRotationX() {
        return rotationX;
    }

    public static float getRotationXZ() {
        return rotationXZ;
    }

    public static float getRotationZ() {
        return rotationZ;
    }

    public static float getRotationYZ() {
        return rotationYZ;
    }

    public static float getRotationXY() {
        return rotationXY;
    }
}
