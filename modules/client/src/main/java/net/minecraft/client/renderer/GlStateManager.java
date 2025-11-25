package net.minecraft.client.renderer;

import net.optifine.Config;
import net.optifine.SmartAnimations;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.Shaders;
import net.optifine.util.LockCounter;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GlStateManager {
    private static final AlphaState alphaState = new AlphaState();
    private static final BooleanState lightingState = new BooleanState(2896);
    private static final BooleanState[] lightState = new BooleanState[8];
    private static final ColorMaterialState colorMaterialState = new ColorMaterialState();
    private static final BlendState blendState = new BlendState();
    private static final DepthState depthState = new DepthState();
    private static final FogState fogState = new FogState();
    private static final CullState cullState = new CullState();
    private static final PolygonOffsetState polygonOffsetState = new PolygonOffsetState();
    private static final ColorLogicState colorLogicState = new ColorLogicState();
    private static final TexGenState texGenState = new TexGenState();
    private static final ClearState clearState = new ClearState();
    private static final BooleanState normalizeState = new BooleanState(2977);
    private static int activeTextureUnit = 0;
    private static final TextureState[] textureState = new TextureState[32];
    private static int activeShadeModel = 7425;
    private static final BooleanState rescaleNormalState = new BooleanState(32826);
    private static final ColorMask colorMaskState = new ColorMask();
    private static final Color colorState = new Color();
    public static final boolean CLEAR_ENABLED = true;
    private static final LockCounter ALPHA_LOCK = new LockCounter();
    private static final GlAlphaState ALPHA_LOCK_STATE = new GlAlphaState();
    private static final LockCounter BLEND_LOCK = new LockCounter();
    private static final GlBlendState BLEND_LOCK_STATE = new GlBlendState();

    public static void pushAttrib() {
        GL11.glPushAttrib(8256);
    }

    public static void popAttrib() {
        GL11.glPopAttrib();
    }

    public static void disableAlpha() {
        if (ALPHA_LOCK.isLocked()) {
            ALPHA_LOCK_STATE.setDisabled();
        } else {
            alphaState.alphaTest.setDisabled();
        }
    }

    public static void enableAlpha() {
        if (ALPHA_LOCK.isLocked()) {
            ALPHA_LOCK_STATE.setEnabled();
        } else {
            alphaState.alphaTest.setEnabled();
        }
    }

    public static void alphaFunc(int func, float ref) {
        if (ALPHA_LOCK.isLocked()) {
            ALPHA_LOCK_STATE.setFuncRef(func, ref);
        } else {
            if (func != alphaState.func || ref != alphaState.ref) {
                alphaState.func = func;
                alphaState.ref = ref;
                GL11.glAlphaFunc(func, ref);
            }
        }
    }

    public static void enableLighting() {
        lightingState.setEnabled();
    }

    public static void disableLighting() {
        lightingState.setDisabled();
    }

    public static void enableLight(int light) {
        lightState[light].setEnabled();
    }

    public static void disableLight(int light) {
        lightState[light].setDisabled();
    }

    public static void enableColorMaterial() {
        colorMaterialState.colorMaterial.setEnabled();
    }

    public static void disableColorMaterial() {
        colorMaterialState.colorMaterial.setDisabled();
    }

    public static void colorMaterial(int face, int mode) {
        if (face != colorMaterialState.face || mode != colorMaterialState.mode) {
            colorMaterialState.face = face;
            colorMaterialState.mode = mode;
            GL11.glColorMaterial(face, mode);
        }
    }

    public static void disableDepth() {
        depthState.depthTest.setDisabled();
    }

    public static void enableDepth() {
        depthState.depthTest.setEnabled();
    }

    public static void depthFunc(int func) {
        if (func != depthState.depthFunc) {
            depthState.depthFunc = func;
            GL11.glDepthFunc(func);
        }
    }

    public static void depthMask(boolean flag) {
        if (flag != depthState.maskEnabled) {
            depthState.maskEnabled = flag;
            GL11.glDepthMask(flag);
        }
    }

    public static void disableBlend() {
        if (BLEND_LOCK.isLocked()) {
            BLEND_LOCK_STATE.setDisabled();
        } else {
            blendState.blend.setDisabled();
        }
    }

    public static void enableBlend() {
        if (BLEND_LOCK.isLocked()) {
            BLEND_LOCK_STATE.setEnabled();
        } else {
            blendState.blend.setEnabled();
        }
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        if (BLEND_LOCK.isLocked()) {
            BLEND_LOCK_STATE.setFactors(srcFactor, dstFactor);
        } else {
            if (srcFactor != blendState.srcFactor || dstFactor != blendState.dstFactor || srcFactor != blendState.srcFactorAlpha || dstFactor != blendState.dstFactorAlpha) {
                blendState.srcFactor = srcFactor;
                blendState.dstFactor = dstFactor;
                blendState.srcFactorAlpha = srcFactor;
                blendState.dstFactorAlpha = dstFactor;

                if (Config.isShaders()) {
                    Shaders.UNIFORM_BLEND_FUNC.setValue(srcFactor, dstFactor, srcFactor, dstFactor);
                }

                GL11.glBlendFunc(srcFactor, dstFactor);
            }
        }
    }

    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
        if (BLEND_LOCK.isLocked()) {
            BLEND_LOCK_STATE.setFactors(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
        } else {
            if (srcFactor != blendState.srcFactor || dstFactor != blendState.dstFactor || srcFactorAlpha != blendState.srcFactorAlpha || dstFactorAlpha != blendState.dstFactorAlpha) {
                blendState.srcFactor = srcFactor;
                blendState.dstFactor = dstFactor;
                blendState.srcFactorAlpha = srcFactorAlpha;
                blendState.dstFactorAlpha = dstFactorAlpha;

                if (Config.isShaders()) {
                    Shaders.UNIFORM_BLEND_FUNC.setValue(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
                }

                OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
            }
        }
    }

    public static void enableFog() {
        fogState.fog.setEnabled();
    }

    public static void disableFog() {
        fogState.fog.setDisabled();
    }

    public static void setFog(int param) {
        if (param != fogState.mode) {
            fogState.mode = param;
            GL11.glFogi(GL11.GL_FOG_MODE, param);

            if (Config.isShaders()) {
                Shaders.setFogMode(param);
            }
        }
    }

    public static void setFogDensity(float param) {
        if (param < 0.0F) {
            param = 0.0F;
        }

        if (param != fogState.density) {
            fogState.density = param;
            GL11.glFogf(GL11.GL_FOG_DENSITY, param);

            if (Config.isShaders()) {
                Shaders.setFogDensity(param);
            }
        }
    }

    public static void setFogStart(float param) {
        if (param != fogState.start) {
            fogState.start = param;
            GL11.glFogf(GL11.GL_FOG_START, param);
        }
    }

    public static void setFogEnd(float param) {
        if (param != fogState.end) {
            fogState.end = param;
            GL11.glFogf(GL11.GL_FOG_END, param);
        }
    }

    public static void enableCull() {
        cullState.cullFace.setEnabled();
    }

    public static void disableCull() {
        cullState.cullFace.setDisabled();
    }

    public static void cullFace(int mode) {
        if (mode != cullState.mode) {
            cullState.mode = mode;
            GL11.glCullFace(mode);
        }
    }

    public static void enablePolygonOffset() {
        polygonOffsetState.polygonOffsetFill.setEnabled();
    }

    public static void disablePolygonOffset() {
        polygonOffsetState.polygonOffsetFill.setDisabled();
    }

    public static void doPolygonOffset(float factor, float units) {
        if (factor != polygonOffsetState.factor || units != polygonOffsetState.units) {
            polygonOffsetState.factor = factor;
            polygonOffsetState.units = units;
            GL11.glPolygonOffset(factor, units);
        }
    }

    public static void enableColorLogic() {
        colorLogicState.colorLogicOp.setEnabled();
    }

    public static void disableColorLogic() {
        colorLogicState.colorLogicOp.setDisabled();
    }

    public static void colorLogicOp(int opcode) {
        if (opcode != colorLogicState.opcode) {
            colorLogicState.opcode = opcode;
            GL11.glLogicOp(opcode);
        }
    }

    public static void enableTexGenCoord(TexGen state) {
        texGenCoord(state).textureGen.setEnabled();
    }

    public static void disableTexGenCoord(TexGen state) {
        texGenCoord(state).textureGen.setDisabled();
    }

    public static void texGen(TexGen state, int param) {
        TexGenCoord glstatemanager$texgencoord = texGenCoord(state);

        if (param != glstatemanager$texgencoord.param) {
            glstatemanager$texgencoord.param = param;
            GL11.glTexGeni(glstatemanager$texgencoord.coord, GL11.GL_TEXTURE_GEN_MODE, param);
        }
    }

    public static void texGen(TexGen state, int pname, FloatBuffer params) {
        GL11.glTexGenfv(texGenCoord(state).coord, pname, params);
    }

    private static TexGenCoord texGenCoord(TexGen state) {
        return switch (state) {
            case S -> texGenState.s;
            case T -> texGenState.t;
            case R -> texGenState.r;
            case Q -> texGenState.q;
        };
    }

    public static void setActiveTexture(int texture) {
        if (activeTextureUnit != texture - OpenGlHelper.defaultTexUnit) {
            activeTextureUnit = texture - OpenGlHelper.defaultTexUnit;
            OpenGlHelper.setActiveTexture(texture);
        }
    }

    public static void enableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setEnabled();
    }

    public static void disableTexture2D() {
        textureState[activeTextureUnit].texture2DState.setDisabled();
    }

    public static int generateTexture() {
        return GL11.glGenTextures();
    }

    public static void deleteTexture(int texture) {
        if (texture != 0) {
            GL11.glDeleteTextures(texture);

            for (TextureState glstatemanager$texturestate : textureState) {
                if (glstatemanager$texturestate.textureName == texture) {
                    glstatemanager$texturestate.textureName = 0;
                }
            }
        }
    }

    public static void bindTexture(int texture) {
        if (texture != textureState[activeTextureUnit].textureName) {
            textureState[activeTextureUnit].textureName = texture;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

            if (SmartAnimations.isActive()) {
                SmartAnimations.textureRendered(texture);
            }
        }
    }

    public static void enableNormalize() {
        normalizeState.setEnabled();
    }

    public static void disableNormalize() {
        normalizeState.setDisabled();
    }

    public static void shadeModel(int mode) {
        if (mode != activeShadeModel) {
            activeShadeModel = mode;
            GL11.glShadeModel(mode);
        }
    }

    public static void enableRescaleNormal() {
        rescaleNormalState.setEnabled();
    }

    public static void disableRescaleNormal() {
        rescaleNormalState.setDisabled();
    }

    public static void viewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        if (red != colorMaskState.red || green != colorMaskState.green || blue != colorMaskState.blue || alpha != colorMaskState.alpha) {
            colorMaskState.red = red;
            colorMaskState.green = green;
            colorMaskState.blue = blue;
            colorMaskState.alpha = alpha;
            GL11.glColorMask(red, green, blue, alpha);
        }
    }

    public static void clearDepth(double depth) {
        if (depth != clearState.depth) {
            clearState.depth = depth;
            GL11.glClearDepth(depth);
        }
    }

    public static void clearColor(float red, float green, float blue, float alpha) {
        if (red != clearState.color.red || green != clearState.color.green || blue != clearState.color.blue || alpha != clearState.color.alpha) {
            clearState.color.red = red;
            clearState.color.green = green;
            clearState.color.blue = blue;
            clearState.color.alpha = alpha;
            GL11.glClearColor(red, green, blue, alpha);
        }
    }

    public static void clear(int mask) {
        if (CLEAR_ENABLED) {
            GL11.glClear(mask);
        }
    }

    public static void matrixMode(int mode) {
        GL11.glMatrixMode(mode);
    }

    public static void loadIdentity() {
        GL11.glLoadIdentity();
    }

    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    public static void getFloat(int pname, FloatBuffer params) {
        GL11.glGetFloatv(pname, params);
    }

    public static void ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    public static void rotate(float angle, float x, float y, float z) {
        GL11.glRotatef(angle, x, y, z);
    }

    public static void scale(float x, float y, float z) {
        GL11.glScalef(x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    public static void translate(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    public static void multMatrix(FloatBuffer matrix) {
        GL11.glMultMatrixf(matrix);
    }

    public static void color(float colorRed, float colorGreen, float colorBlue, float colorAlpha) {
        if (colorRed != colorState.red || colorGreen != colorState.green || colorBlue != colorState.blue || colorAlpha != colorState.alpha) {
            colorState.red = colorRed;
            colorState.green = colorGreen;
            colorState.blue = colorBlue;
            colorState.alpha = colorAlpha;
            GL11.glColor4f(colorRed, colorGreen, colorBlue, colorAlpha);
        }
    }

    public static void color(float colorRed, float colorGreen, float colorBlue) {
        color(colorRed, colorGreen, colorBlue, 1.0F);
    }

    public static void resetColor() {
        colorState.red = colorState.green = colorState.blue = colorState.alpha = -1.0F;
    }

    public static void glVertexPointer(int p_glVertexPointer_0_, int p_glVertexPointer_1_, int p_glVertexPointer_2_, int p_glVertexPointer_3_) {
        GL11.glVertexPointer(p_glVertexPointer_0_, p_glVertexPointer_1_, p_glVertexPointer_2_, p_glVertexPointer_3_);
    }

    public static void glDisableClientState(int p_glDisableClientState_0_) {
        GL11.glDisableClientState(p_glDisableClientState_0_);
    }

    public static void glEnableClientState(int p_glEnableClientState_0_) {
        GL11.glEnableClientState(p_glEnableClientState_0_);
    }

    public static void glDrawArrays(int p_glDrawArrays_0_, int p_glDrawArrays_1_, int p_glDrawArrays_2_) {
        GL11.glDrawArrays(p_glDrawArrays_0_, p_glDrawArrays_1_, p_glDrawArrays_2_);

        if (Config.isShaders()) {
            int i = Shaders.activeProgram.getCountInstances();

            if (i > 1) {
                for (int j = 1; j < i; ++j) {
                    Shaders.UNIFORM_INSTANCE_ID.setValue(j);
                    GL11.glDrawArrays(p_glDrawArrays_0_, p_glDrawArrays_1_, p_glDrawArrays_2_);
                }

                Shaders.UNIFORM_INSTANCE_ID.setValue(0);
            }
        }
    }

    public static void callList(int list) {
        GL11.glCallList(list);

        if (Config.isShaders()) {
            int i = Shaders.activeProgram.getCountInstances();

            if (i > 1) {
                for (int j = 1; j < i; ++j) {
                    Shaders.UNIFORM_INSTANCE_ID.setValue(j);
                    GL11.glCallList(list);
                }

                Shaders.UNIFORM_INSTANCE_ID.setValue(0);
            }
        }
    }

    public static void callLists(IntBuffer p_callLists_0_) {
        GL11.glCallLists(p_callLists_0_);

        if (Config.isShaders()) {
            int i = Shaders.activeProgram.getCountInstances();

            if (i > 1) {
                for (int j = 1; j < i; ++j) {
                    Shaders.UNIFORM_INSTANCE_ID.setValue(j);
                    GL11.glCallLists(p_callLists_0_);
                }

                Shaders.UNIFORM_INSTANCE_ID.setValue(0);
            }
        }
    }

    public static int glGetError() {
        return GL11.glGetError();
    }

    public static void glTexImage2D(int p_glTexImage2D_0_, int p_glTexImage2D_1_, int p_glTexImage2D_2_, int p_glTexImage2D_3_, int p_glTexImage2D_4_, int p_glTexImage2D_5_, int p_glTexImage2D_6_, int p_glTexImage2D_7_, IntBuffer p_glTexImage2D_8_) {
        GL11.glTexImage2D(p_glTexImage2D_0_, p_glTexImage2D_1_, p_glTexImage2D_2_, p_glTexImage2D_3_, p_glTexImage2D_4_, p_glTexImage2D_5_, p_glTexImage2D_6_, p_glTexImage2D_7_, p_glTexImage2D_8_);
    }

    public static int glGetTexLevelParameteri(int p_glGetTexLevelParameteri_0_, int p_glGetTexLevelParameteri_1_, int p_glGetTexLevelParameteri_2_) {
        return GL11.glGetTexLevelParameteri(p_glGetTexLevelParameteri_0_, p_glGetTexLevelParameteri_1_, p_glGetTexLevelParameteri_2_);
    }

    public static int getActiveTextureUnit() {
        return OpenGlHelper.defaultTexUnit + activeTextureUnit;
    }

    public static void bindCurrentTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureState[activeTextureUnit].textureName);
    }

    public static int getBoundTexture() {
        return textureState[activeTextureUnit].textureName;
    }

    public static void deleteTextures(IntBuffer p_deleteTextures_0_) {
        p_deleteTextures_0_.rewind();

        while (p_deleteTextures_0_.position() < p_deleteTextures_0_.limit()) {
            int i = p_deleteTextures_0_.get();
            deleteTexture(i);
        }

        p_deleteTextures_0_.rewind();
    }

    public static void lockAlpha(GlAlphaState p_lockAlpha_0_) {
        if (!ALPHA_LOCK.isLocked()) {
            getAlphaState(ALPHA_LOCK_STATE);
            setAlphaState(p_lockAlpha_0_);
            ALPHA_LOCK.lock();
        }
    }

    public static void unlockAlpha() {
        if (ALPHA_LOCK.unlock()) {
            setAlphaState(ALPHA_LOCK_STATE);
        }
    }

    public static void getAlphaState(GlAlphaState p_getAlphaState_0_) {
        if (ALPHA_LOCK.isLocked()) {
            p_getAlphaState_0_.setState(ALPHA_LOCK_STATE);
        } else {
            p_getAlphaState_0_.setState(alphaState.alphaTest.currentState, alphaState.func, alphaState.ref);
        }
    }

    public static void setAlphaState(GlAlphaState p_setAlphaState_0_) {
        if (ALPHA_LOCK.isLocked()) {
            ALPHA_LOCK_STATE.setState(p_setAlphaState_0_);
        } else {
            alphaState.alphaTest.setState(p_setAlphaState_0_.isEnabled());
            alphaFunc(p_setAlphaState_0_.getFunc(), p_setAlphaState_0_.getRef());
        }
    }

    public static void lockBlend(GlBlendState p_lockBlend_0_) {
        if (!BLEND_LOCK.isLocked()) {
            getBlendState(BLEND_LOCK_STATE);
            setBlendState(p_lockBlend_0_);
            BLEND_LOCK.lock();
        }
    }

    public static void unlockBlend() {
        if (BLEND_LOCK.unlock()) {
            setBlendState(BLEND_LOCK_STATE);
        }
    }

    public static void getBlendState(GlBlendState p_getBlendState_0_) {
        if (BLEND_LOCK.isLocked()) {
            p_getBlendState_0_.setState(BLEND_LOCK_STATE);
        } else {
            p_getBlendState_0_.setState(blendState.blend.currentState, blendState.srcFactor, blendState.dstFactor, blendState.srcFactorAlpha, blendState.dstFactorAlpha);
        }
    }

    public static void setBlendState(GlBlendState p_setBlendState_0_) {
        if (BLEND_LOCK.isLocked()) {
            BLEND_LOCK_STATE.setState(p_setBlendState_0_);
        } else {
            blendState.blend.setState(p_setBlendState_0_.isEnabled());

            if (!p_setBlendState_0_.isSeparate()) {
                blendFunc(p_setBlendState_0_.getSrcFactor(), p_setBlendState_0_.getDstFactor());
            } else {
                tryBlendFuncSeparate(p_setBlendState_0_.getSrcFactor(), p_setBlendState_0_.getDstFactor(), p_setBlendState_0_.getSrcFactorAlpha(), p_setBlendState_0_.getDstFactorAlpha());
            }
        }
    }

    public static void glMultiDrawArrays(int mode, IntBuffer first, IntBuffer count) {
        GL14.glMultiDrawArrays(mode, first, count);

        if (Config.isShaders()) {
            int i = Shaders.activeProgram.getCountInstances();

            if (i > 1) {
                for (int j = 1; j < i; ++j) {
                    Shaders.UNIFORM_INSTANCE_ID.setValue(j);
                    GL14.glMultiDrawArrays(mode, first, count);
                }

                Shaders.UNIFORM_INSTANCE_ID.setValue(0);
            }
        }
    }

    static {
        for (int i = 0; i < 8; ++i) {
            lightState[i] = new BooleanState(16384 + i);
        }

        for (int j = 0; j < textureState.length; ++j) {
            textureState[j] = new TextureState();
        }
    }

    static class AlphaState {
        public final BooleanState alphaTest;
        public int func;
        public float ref;

        private AlphaState() {
            this.alphaTest = new BooleanState(3008);
            this.func = 519;
            this.ref = -1.0F;
        }
    }

    static class BlendState {
        public final BooleanState blend;
        public int srcFactor;
        public int dstFactor;
        public int srcFactorAlpha;
        public int dstFactorAlpha;

        private BlendState() {
            this.blend = new BooleanState(3042);
            this.srcFactor = 1;
            this.dstFactor = 0;
            this.srcFactorAlpha = 1;
            this.dstFactorAlpha = 0;
        }
    }

    static class BooleanState {
        private final int capability;
        private boolean currentState = false;

        public BooleanState(int capabilityIn) {
            this.capability = capabilityIn;
        }

        public void setDisabled() {
            this.setState(false);
        }

        public void setEnabled() {
            this.setState(true);
        }

        public void setState(boolean state) {
            if (state != this.currentState) {
                this.currentState = state;

                if (state) {
                    GL11.glEnable(this.capability);
                } else {
                    GL11.glDisable(this.capability);
                }
            }
        }
    }

    static class ClearState {
        public double depth;
        public final Color color;
        public final int field_179204_c;

        private ClearState() {
            this.depth = 1.0D;
            this.color = new Color(0.0F, 0.0F, 0.0F, 0.0F);
            this.field_179204_c = 0;
        }
    }

    static class Color {
        public float red = 1.0F;
        public float green = 1.0F;
        public float blue = 1.0F;
        public float alpha = 1.0F;

        public Color() {
        }

        public Color(float redIn, float greenIn, float blueIn, float alphaIn) {
            this.red = redIn;
            this.green = greenIn;
            this.blue = blueIn;
            this.alpha = alphaIn;
        }
    }

    static class ColorLogicState {
        public final BooleanState colorLogicOp;
        public int opcode;

        private ColorLogicState() {
            this.colorLogicOp = new BooleanState(3058);
            this.opcode = 5379;
        }
    }

    static class ColorMask {
        public boolean red;
        public boolean green;
        public boolean blue;
        public boolean alpha;

        private ColorMask() {
            this.red = true;
            this.green = true;
            this.blue = true;
            this.alpha = true;
        }
    }

    static class ColorMaterialState {
        public final BooleanState colorMaterial;
        public int face;
        public int mode;

        private ColorMaterialState() {
            this.colorMaterial = new BooleanState(2903);
            this.face = 1032;
            this.mode = 5634;
        }
    }

    static class CullState {
        public final BooleanState cullFace;
        public int mode;

        private CullState() {
            this.cullFace = new BooleanState(2884);
            this.mode = 1029;
        }
    }

    static class DepthState {
        public final BooleanState depthTest;
        public boolean maskEnabled;
        public int depthFunc;

        private DepthState() {
            this.depthTest = new BooleanState(2929);
            this.maskEnabled = true;
            this.depthFunc = 513;
        }
    }

    static class FogState {
        public final BooleanState fog;
        public int mode;
        public float density;
        public float start;
        public float end;

        private FogState() {
            this.fog = new BooleanState(2912);
            this.mode = 2048;
            this.density = 1.0F;
            this.start = 0.0F;
            this.end = 1.0F;
        }
    }

    static class PolygonOffsetState {
        public final BooleanState polygonOffsetFill;
        public final BooleanState polygonOffsetLine;
        public float factor;
        public float units;

        private PolygonOffsetState() {
            this.polygonOffsetFill = new BooleanState(32823);
            this.polygonOffsetLine = new BooleanState(10754);
            this.factor = 0.0F;
            this.units = 0.0F;
        }
    }

    public enum TexGen {
        S,
        T,
        R,
        Q
    }

    static class TexGenCoord {
        public final BooleanState textureGen;
        public final int coord;
        public int param = -1;

        public TexGenCoord(int p_i46254_1_, int p_i46254_2_) {
            this.coord = p_i46254_1_;
            this.textureGen = new BooleanState(p_i46254_2_);
        }
    }

    static class TexGenState {
        public final TexGenCoord s;
        public final TexGenCoord t;
        public final TexGenCoord r;
        public final TexGenCoord q;

        private TexGenState() {
            this.s = new TexGenCoord(8192, 3168);
            this.t = new TexGenCoord(8193, 3169);
            this.r = new TexGenCoord(8194, 3170);
            this.q = new TexGenCoord(8195, 3171);
        }
    }

    static class TextureState {
        public final BooleanState texture2DState;
        public int textureName;

        private TextureState() {
            this.texture2DState = new BooleanState(3553);
            this.textureName = 0;
        }
    }
}
