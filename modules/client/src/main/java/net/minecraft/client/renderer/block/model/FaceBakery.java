package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.CubeFace;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.optifine.Config;
import net.optifine.model.BlockModelUtils;
import net.optifine.shaders.Shaders;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;

public class FaceBakery {
    private static final float SCALE_ROTATION_22_5 = 1.0F / (float) Math.cos(0.39269909262657166D) - 1.0F;
    private static final float SCALE_ROTATION_GENERAL = 1.0F / (float) Math.cos((Math.PI / 4.0D)) - 1.0F;

    public BakedQuad makeBakedQuad(Vector3f posFrom, Vector3f posTo, BlockPartFace face, TextureAtlasSprite sprite, Direction facing, ModelRotation modelRotationIn, BlockPartRotation partRotation, boolean uvLocked, boolean shade) {
        int[] aint = this.makeQuadVertexData(face, sprite, facing, this.getPositionsDiv16(posFrom, posTo), modelRotationIn, partRotation, uvLocked, shade);
        Direction enumfacing = getFacingFromVertexData(aint);

        if (uvLocked) {
            this.lockUv(aint, enumfacing, face.blockFaceUV, sprite);
        }

        if (partRotation == null) {
            this.applyFacing(aint, enumfacing);
        }

        return new BakedQuad(aint, face.tintIndex, enumfacing);
    }

    private int[] makeQuadVertexData(BlockPartFace face, TextureAtlasSprite sprite, Direction facing, float[] p_makeQuadVertexData_4_, ModelRotation modelRotationIn, BlockPartRotation partRotation, boolean uvLocked, boolean shade) {
        int i = 28;

        if (Config.isShaders()) {
            i = 56;
        }

        int[] aint = new int[i];

        for (int j = 0; j < 4; ++j) {
            this.fillVertexData(aint, j, facing, face, p_makeQuadVertexData_4_, sprite, modelRotationIn, partRotation, uvLocked, shade);
        }

        return aint;
    }

    private int getFaceShadeColor(Direction facing) {
        float f = getFaceBrightness(facing);
        int i = MathHelper.clamp((int) (f * 255.0F), 0, 255);
        return -16777216 | i << 16 | i << 8 | i;
    }

    public static float getFaceBrightness(Direction p_178412_0_) {
        return switch (p_178412_0_) {
            case DOWN -> {
                if (Config.isShaders()) {
                    yield Shaders.blockLightLevel05;
                }

                yield 0.5F;
            }
            case UP -> 1.0F;
            case NORTH, SOUTH -> {
                if (Config.isShaders()) {
                    yield Shaders.blockLightLevel08;
                }

                yield 0.8F;
            }
            case WEST, EAST -> {
                if (Config.isShaders()) {
                    yield Shaders.blockLightLevel06;
                }

                yield 0.6F;
            }
            default -> 1.0F;
        };
    }

    private float[] getPositionsDiv16(Vector3f pos1, Vector3f pos2) {
        float[] afloat = new float[Direction.values().length];
        afloat[CubeFace.Constants.WEST_INDEX] = pos1.x / 16.0F;
        afloat[CubeFace.Constants.DOWN_INDEX] = pos1.y / 16.0F;
        afloat[CubeFace.Constants.NORTH_INDEX] = pos1.z / 16.0F;
        afloat[CubeFace.Constants.EAST_INDEX] = pos2.x / 16.0F;
        afloat[CubeFace.Constants.UP_INDEX] = pos2.y / 16.0F;
        afloat[CubeFace.Constants.SOUTH_INDEX] = pos2.z / 16.0F;
        return afloat;
    }

    private void fillVertexData(int[] p_fillVertexData_1_, int p_fillVertexData_2_, Direction face, BlockPartFace p_fillVertexData_4_, float[] p_fillVertexData_5_, TextureAtlasSprite sprite, ModelRotation modelRotationIn, BlockPartRotation partRotation, boolean uvLocked, boolean shade) {
        Direction enumfacing = modelRotationIn.rotateFace(face);
        int i = shade ? this.getFaceShadeColor(enumfacing) : -1;
        CubeFace.VertexInformation enumfacedirection$vertexinformation = CubeFace.getFacing(face).getVertexInformation(p_fillVertexData_2_);
        Vector3f vector3f = new Vector3f(p_fillVertexData_5_[enumfacedirection$vertexinformation.xIndex], p_fillVertexData_5_[enumfacedirection$vertexinformation.yIndex], p_fillVertexData_5_[enumfacedirection$vertexinformation.zIndex]);
        this.rotatePart(vector3f, partRotation);
        int j = this.rotateVertex(vector3f, face, p_fillVertexData_2_, modelRotationIn, uvLocked);
        BlockModelUtils.snapVertexPosition(vector3f);
        this.storeVertexData(p_fillVertexData_1_, j, p_fillVertexData_2_, vector3f, i, sprite, p_fillVertexData_4_.blockFaceUV);
    }

    private void storeVertexData(int[] faceData, int storeIndex, int vertexIndex, Vector3f position, int shadeColor, TextureAtlasSprite sprite, BlockFaceUV faceUV) {
        int i = faceData.length / 4;
        int j = storeIndex * i;
        faceData[j] = Float.floatToRawIntBits(position.x);
        faceData[j + 1] = Float.floatToRawIntBits(position.y);
        faceData[j + 2] = Float.floatToRawIntBits(position.z);
        faceData[j + 3] = shadeColor;
        faceData[j + 4] = Float.floatToRawIntBits(sprite.getInterpolatedU(faceUV.func_178348_a(vertexIndex) * 0.999D + faceUV.func_178348_a((vertexIndex + 2) % 4) * 0.001D));
        faceData[j + 4 + 1] = Float.floatToRawIntBits(sprite.getInterpolatedV(faceUV.func_178346_b(vertexIndex) * 0.999D + faceUV.func_178346_b((vertexIndex + 2) % 4) * 0.001D));
    }

    private void rotatePart(Vector3f point, BlockPartRotation partRotation) {
        if (partRotation == null) {
            return;
        }

        Matrix4f matrix = new Matrix4f();
        Vector3f scaleVector = new Vector3f(0.0F, 0.0F, 0.0F);
        float angle = partRotation.angle * 0.017453292F;

        switch (partRotation.axis) {
            case X -> {
                matrix.rotateX(angle);
                scaleVector.set(0.0F, 1.0F, 1.0F);
            }
            case Y -> {
                matrix.rotateY(angle);
                scaleVector.set(1.0F, 0.0F, 1.0F);
            }
            case Z -> {
                matrix.rotateZ(angle);
                scaleVector.set(1.0F, 1.0F, 0.0F);
            }
        }

        if (partRotation.rescale) {
            if (Math.abs(partRotation.angle) == 22.5F) {
                scaleVector.mul(SCALE_ROTATION_22_5);
            } else {
                scaleVector.mul(SCALE_ROTATION_GENERAL);
            }

            scaleVector.add(1.0F, 1.0F, 1.0F);
        } else {
            scaleVector.set(1.0F, 1.0F, 1.0F);
        }

        this.rotateScale(point, new Vector3f(partRotation.origin), matrix, scaleVector);
    }

    public int rotateVertex(Vector3f position, Direction facing, int vertexIndex, ModelRotation modelRotationIn, boolean uvLocked) {
        if (modelRotationIn == ModelRotation.X0_Y0) {
            return vertexIndex;
        } else {
            this.rotateScale(position, new Vector3f(0.5F, 0.5F, 0.5F), modelRotationIn.getMatrix4d(), new Vector3f(1.0F, 1.0F, 1.0F));
            return modelRotationIn.rotateVertex(facing, vertexIndex);
        }
    }

    private void rotateScale(Vector3f position, Vector3f rotationOrigin, Matrix4f rotationMatrix, Vector3f scale) {
        Vector4f vector4f = new Vector4f(position.x - rotationOrigin.x, position.y - rotationOrigin.y, position.z - rotationOrigin.z, 1.0F);
        vector4f.mul(rotationMatrix);
        vector4f.x *= scale.x;
        vector4f.y *= scale.y;
        vector4f.z *= scale.z;
        position.set(vector4f.x + rotationOrigin.x, vector4f.y + rotationOrigin.y, vector4f.z + rotationOrigin.z);
    }

    public static Direction getFacingFromVertexData(int[] faceData) {
        int i = faceData.length / 4;
        int j = i * 2;
        int k = i * 3;
        Vector3f vector3f = new Vector3f(Float.intBitsToFloat(faceData[0]), Float.intBitsToFloat(faceData[1]), Float.intBitsToFloat(faceData[2]));
        Vector3f vector3f1 = new Vector3f(Float.intBitsToFloat(faceData[i]), Float.intBitsToFloat(faceData[i + 1]), Float.intBitsToFloat(faceData[i + 2]));
        Vector3f vector3f2 = new Vector3f(Float.intBitsToFloat(faceData[j]), Float.intBitsToFloat(faceData[j + 1]), Float.intBitsToFloat(faceData[j + 2]));
        Vector3f vector3f3 = new Vector3f();
        Vector3f vector3f4 = new Vector3f();
        Vector3f vector3f5 = new Vector3f();
        vector3f.sub(vector3f1, vector3f3);
        vector3f2.sub(vector3f1, vector3f4);
        vector3f4.cross(vector3f3, vector3f5);
        float f = (float) Math.sqrt((vector3f5.x * vector3f5.x + vector3f5.y * vector3f5.y + vector3f5.z * vector3f5.z));
        vector3f5.x /= f;
        vector3f5.y /= f;
        vector3f5.z /= f;
        Direction enumfacing = null;
        float f1 = 0.0F;

        for (Direction enumfacing1 : Direction.values()) {
            Vec3i vec3i = enumfacing1.getDirectionVec();
            Vector3f vector3f6 = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            float f2 = vector3f5.dot(vector3f6);

            if (f2 >= 0.0F && f2 > f1) {
                f1 = f2;
                enumfacing = enumfacing1;
            }
        }

        return Objects.requireNonNullElse(enumfacing, Direction.UP);
    }

    public void lockUv(int[] p_178409_1_, Direction facing, BlockFaceUV p_178409_3_, TextureAtlasSprite p_178409_4_) {
        for (int i = 0; i < 4; ++i) {
            this.lockVertexUv(i, p_178409_1_, facing, p_178409_3_, p_178409_4_);
        }
    }

    private void applyFacing(int[] p_178408_1_, Direction p_178408_2_) {
        int[] aint = new int[p_178408_1_.length];
        System.arraycopy(p_178408_1_, 0, aint, 0, p_178408_1_.length);
        float[] afloat = new float[Direction.values().length];
        afloat[CubeFace.Constants.WEST_INDEX] = 999.0F;
        afloat[CubeFace.Constants.DOWN_INDEX] = 999.0F;
        afloat[CubeFace.Constants.NORTH_INDEX] = 999.0F;
        afloat[CubeFace.Constants.EAST_INDEX] = -999.0F;
        afloat[CubeFace.Constants.UP_INDEX] = -999.0F;
        afloat[CubeFace.Constants.SOUTH_INDEX] = -999.0F;
        int i = p_178408_1_.length / 4;

        for (int j = 0; j < 4; ++j) {
            int k = i * j;
            float f = Float.intBitsToFloat(aint[k]);
            float f1 = Float.intBitsToFloat(aint[k + 1]);
            float f2 = Float.intBitsToFloat(aint[k + 2]);

            if (f < afloat[CubeFace.Constants.WEST_INDEX]) {
                afloat[CubeFace.Constants.WEST_INDEX] = f;
            }

            if (f1 < afloat[CubeFace.Constants.DOWN_INDEX]) {
                afloat[CubeFace.Constants.DOWN_INDEX] = f1;
            }

            if (f2 < afloat[CubeFace.Constants.NORTH_INDEX]) {
                afloat[CubeFace.Constants.NORTH_INDEX] = f2;
            }

            if (f > afloat[CubeFace.Constants.EAST_INDEX]) {
                afloat[CubeFace.Constants.EAST_INDEX] = f;
            }

            if (f1 > afloat[CubeFace.Constants.UP_INDEX]) {
                afloat[CubeFace.Constants.UP_INDEX] = f1;
            }

            if (f2 > afloat[CubeFace.Constants.SOUTH_INDEX]) {
                afloat[CubeFace.Constants.SOUTH_INDEX] = f2;
            }
        }

        CubeFace enumfacedirection = CubeFace.getFacing(p_178408_2_);

        for (int j1 = 0; j1 < 4; ++j1) {
            int k1 = i * j1;
            CubeFace.VertexInformation enumfacedirection$vertexinformation = enumfacedirection.getVertexInformation(j1);
            float f8 = afloat[enumfacedirection$vertexinformation.xIndex];
            float f3 = afloat[enumfacedirection$vertexinformation.yIndex];
            float f4 = afloat[enumfacedirection$vertexinformation.zIndex];
            p_178408_1_[k1] = Float.floatToRawIntBits(f8);
            p_178408_1_[k1 + 1] = Float.floatToRawIntBits(f3);
            p_178408_1_[k1 + 2] = Float.floatToRawIntBits(f4);

            for (int l = 0; l < 4; ++l) {
                int i1 = i * l;
                float f5 = Float.intBitsToFloat(aint[i1]);
                float f6 = Float.intBitsToFloat(aint[i1 + 1]);
                float f7 = Float.intBitsToFloat(aint[i1 + 2]);

                if (MathHelper.epsilonEquals(f8, f5) && MathHelper.epsilonEquals(f3, f6) && MathHelper.epsilonEquals(f4, f7)) {
                    p_178408_1_[k1 + 4] = aint[i1 + 4];
                    p_178408_1_[k1 + 4 + 1] = aint[i1 + 4 + 1];
                }
            }
        }
    }

    private void lockVertexUv(int p_178401_1_, int[] p_178401_2_, Direction facing, BlockFaceUV p_178401_4_, TextureAtlasSprite p_178401_5_) {
        int i = p_178401_2_.length / 4;
        int j = i * p_178401_1_;
        float f = Float.intBitsToFloat(p_178401_2_[j]);
        float f1 = Float.intBitsToFloat(p_178401_2_[j + 1]);
        float f2 = Float.intBitsToFloat(p_178401_2_[j + 2]);

        if (f < -0.1F || f >= 1.1F) {
            f -= MathHelper.floor(f);
        }

        if (f1 < -0.1F || f1 >= 1.1F) {
            f1 -= MathHelper.floor(f1);
        }

        if (f2 < -0.1F || f2 >= 1.1F) {
            f2 -= MathHelper.floor(f2);
        }

        float f3;
        float f4 = switch (facing) {
            case DOWN -> {
                f3 = f * 16.0F;
                yield (1.0F - f2) * 16.0F;
            }
            case UP -> {
                f3 = f * 16.0F;
                yield f2 * 16.0F;
            }
            case NORTH -> {
                f3 = (1.0F - f) * 16.0F;
                yield (1.0F - f1) * 16.0F;
            }
            case SOUTH -> {
                f3 = f * 16.0F;
                yield (1.0F - f1) * 16.0F;
            }
            case WEST -> {
                f3 = f2 * 16.0F;
                yield (1.0F - f1) * 16.0F;
            }
            case EAST -> {
                f3 = (1.0F - f2) * 16.0F;
                yield (1.0F - f1) * 16.0F;
            }
        };

        int k = p_178401_4_.func_178345_c(p_178401_1_) * i;
        p_178401_2_[k + 4] = Float.floatToRawIntBits(p_178401_5_.getInterpolatedU(f3));
        p_178401_2_[k + 4 + 1] = Float.floatToRawIntBits(p_178401_5_.getInterpolatedV(f4));
    }
}
