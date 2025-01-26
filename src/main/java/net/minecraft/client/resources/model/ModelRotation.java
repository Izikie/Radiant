package net.minecraft.client.resources.model;

import com.google.common.collect.Maps;

import java.util.Map;

import net.FastMath;
import net.minecraft.util.Direction;
import net.minecraft.util.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public enum ModelRotation {
    X0_Y0(0, 0),
    X0_Y90(0, 90),
    X0_Y180(0, 180),
    X0_Y270(0, 270),
    X90_Y0(90, 0),
    X90_Y90(90, 90),
    X90_Y180(90, 180),
    X90_Y270(90, 270),
    X180_Y0(180, 0),
    X180_Y90(180, 90),
    X180_Y180(180, 180),
    X180_Y270(180, 270),
    X270_Y0(270, 0),
    X270_Y90(270, 90),
    X270_Y180(270, 180),
    X270_Y270(270, 270);

    private static final Map<Integer, ModelRotation> mapRotations = Maps.newHashMap();
    private final int combinedXY;
    private final Matrix4f matrix4d;
    private final int quartersX;
    private final int quartersY;

    private static int combineXY(int p_177521_0_, int p_177521_1_) {
        return p_177521_0_ * 360 + p_177521_1_;
    }

    ModelRotation(int p_i46087_3_, int p_i46087_4_) {
        this.combinedXY = combineXY(p_i46087_3_, p_i46087_4_);
        this.quartersX = MathHelper.abs_int(p_i46087_3_ / 90);
        this.quartersY = MathHelper.abs_int(p_i46087_4_ / 90);
        this.matrix4d = new Matrix4f()
                .identity()
                .rotate(FastMath.toRadians(-p_i46087_3_), new Vector3f(1.0F, 0.0F, 0.0F))
                .rotate(FastMath.toRadians(-p_i46087_4_), new Vector3f(0.0F, 1.0F, 0.0F));
    }

    public Matrix4f getMatrix4d() {
        return this.matrix4d;
    }

    public Direction rotateFace(Direction facing) {
        Direction enumfacing = facing;

        for (int i = 0; i < this.quartersX; ++i) {
            enumfacing = enumfacing.rotateAround(Direction.Axis.X);
        }

        if (enumfacing.getAxis() != Direction.Axis.Y) {
            for (int j = 0; j < this.quartersY; ++j) {
                enumfacing = enumfacing.rotateAround(Direction.Axis.Y);
            }
        }

        return enumfacing;
    }

    public int rotateVertex(Direction facing, int vertexIndex) {
        int i = vertexIndex;

        if (facing.getAxis() == Direction.Axis.X) {
            i = (vertexIndex + this.quartersX) % 4;
        }

        Direction enumfacing = facing;

        for (int j = 0; j < this.quartersX; ++j) {
            enumfacing = enumfacing.rotateAround(Direction.Axis.X);
        }

        if (enumfacing.getAxis() == Direction.Axis.Y) {
            i = (i + this.quartersY) % 4;
        }

        return i;
    }

    public static ModelRotation getModelRotation(int p_177524_0_, int p_177524_1_) {
        return mapRotations.get(combineXY(MathHelper.normalizeAngle(p_177524_0_, 360), MathHelper.normalizeAngle(p_177524_1_, 360)));
    }

    static {
        for (ModelRotation modelrotation : values()) {
            mapRotations.put(modelrotation.combinedXY, modelrotation);
        }
    }
}
