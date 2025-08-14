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
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

// TOOD: UnProject use JOML and possibly make this better
public class ActiveRenderInfo {
    private static final FloatBuffer VIEWPORT = GLAllocation.createDirectFloatBuffer(16);
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
        GL11.glGetFloatv(GL11.GL_VIEWPORT, VIEWPORT);

        float viewCenterX = (VIEWPORT.get(0) + VIEWPORT.get(2)) / 2.0f;
        float viewCenterY = (VIEWPORT.get(1) + VIEWPORT.get(3)) / 2.0f;

        // TODO: Use JOML for better performance and accuracy
//        Project.gluUnProject(viewCenterX, viewCenterY, 0.0F, MODELVIEW, PROJECTION, VIEWPORT, OBJECTCOORDS);
        unproject(viewCenterX, viewCenterY);

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

    private static void unproject(float winx, float winy) {

        Matrix4f modelMatrix = new Matrix4f(MODELVIEW.position(0));
        Matrix4f projMatrix = new Matrix4f(PROJECTION.position(0));
        Matrix4f viewport = new Matrix4f(VIEWPORT.position(0));
        Vector3f objPos = new Vector3f(OBJECTCOORDS.position(0));

        Matrix4f finalMatrix = new Matrix4f();
        modelMatrix.mul(projMatrix, finalMatrix);

        finalMatrix.invert();

        Vector4f in = new Vector4f(winx, winy, 0.F, 1.F);
        in.x = (in.x - viewport.m00()) / viewport.m02();
        in.y = (in.y - viewport.m01()) / viewport.m03();

        in.x = in.x * 2 - 1;
        in.y = in.y * 2 - 1;
        in.z = in.z * 2 - 1;

        Vector4f out = new Vector4f();
        in.mul(finalMatrix);

        if (out.w == 0.F) {
            return;
        }

        out.w = 1.F / out.w;

        objPos.x = out.x * out.w;
        objPos.y = out.y * out.w;
        objPos.z = out.z * out.w;

        objPos.get(OBJECTCOORDS);
    }
}
