package net.minecraft.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ActiveRenderInfo {
    private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
    private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
    private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);

    private static Vector3f position = new Vector3f(0.F, 0.F, 0.F);
    private static float rotationX;
    private static float rotationXZ;
    private static float rotationZ;
    private static float rotationYZ;
    private static float rotationXY;

    public static void updateRenderInfo(EntityPlayer player, boolean invertRotation) {
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, VIEWPORT);

        Matrix4f modelViewMat = new Matrix4f().set(MODELVIEW);
        Matrix4f projectionMat = new Matrix4f().set(PROJECTION);

        int[] vp = new int[4];
        VIEWPORT.get(vp).rewind();

        float viewCenterX = (vp[0] + vp[2]) / 2.0f;
        float viewCenterY = (vp[1] + vp[3]) / 2.0f;

        Vector3f worldCoords = new Vector3f();
        projectionMat.mul(modelViewMat, new Matrix4f()).unproject(viewCenterX, viewCenterY, 0f, vp, worldCoords);
        position.set(worldCoords);

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

        double viewpointX = entityX + position.x;
        double viewpointY = entityY + position.y;
        double viewpointZ = entityZ + position.z;

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
        return new Vec3(position.x, position.y, position.z);
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
