package net.minecraft.client.renderer.culling;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class ClippingHelperImpl extends ClippingHelper {
    private static final ClippingHelperImpl INSTANCE = new ClippingHelperImpl();
    private final FloatBuffer projectionMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final FloatBuffer modelviewMatrixBuffer = GLAllocation.createDirectFloatBuffer(16);
    private final Matrix4f legacyProjection = new Matrix4f();
    private final Matrix4f legacyView = new Matrix4f();
    private final Matrix4f legacyClip = new Matrix4f();

    public static ClippingHelper get() {
        INSTANCE.init();
        return INSTANCE;
    }

    public static ClippingHelper get(Matrix4fc projection, Matrix4fc view, Matrix4fc clip) {
        INSTANCE.init(projection, view, clip);
        return INSTANCE;
    }

    public void init() {
        GlStateManager.getFloat(GL11.GL_PROJECTION_MATRIX, this.projectionMatrixBuffer);
        GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, this.modelviewMatrixBuffer);
        this.legacyProjection.set(this.projectionMatrixBuffer);
        this.legacyView.set(this.modelviewMatrixBuffer);
        this.legacyProjection.mul(this.legacyView, this.legacyClip);
        this.init(this.legacyProjection, this.legacyView, this.legacyClip);
    }

    private void init(Matrix4fc projection, Matrix4fc view, Matrix4fc clip) {
        projection.get(this.projectionMatrix);
        view.get(this.modelviewMatrix);
        clip.get(this.clippingMatrix);
        // The fully-inside check treats the first four planes as the sides.
        this.setPlane(0, 0, -1.0F); // Right
        this.setPlane(1, 0, 1.0F);  // Left
        this.setPlane(2, 1, 1.0F);  // Bottom
        this.setPlane(3, 1, -1.0F); // Top
        this.setPlane(4, 2, -1.0F); // Far
        this.setPlane(5, 2, 1.0F);  // Near
    }

    private void setPlane(int index, int axis, float sign) {
        float[] plane = this.frustum[index];
        float[] clip = this.clippingMatrix;
        float x = clip[3] + sign * clip[axis];
        float y = clip[7] + sign * clip[axis + 4];
        float z = clip[11] + sign * clip[axis + 8];
        float length = MathHelper.sqrt(x * x + y * y + z * z);
        plane[0] = x / length;
        plane[1] = y / length;
        plane[2] = z / length;
        plane[3] = (clip[15] + sign * clip[axis + 12]) / length;
    }
}
