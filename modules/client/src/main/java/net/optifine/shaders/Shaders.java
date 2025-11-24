package net.optifine.shaders;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.math.Vec3;
import net.minecraft.world.World;
import net.optifine.*;
import net.optifine.config.ConnectedParser;
import net.optifine.expr.IExpressionBool;
import net.optifine.render.GlAlphaState;
import net.optifine.render.GlBlendState;
import net.optifine.shaders.config.*;
import net.optifine.shaders.uniform.*;
import net.optifine.texture.InternalFormat;
import net.optifine.texture.PixelFormat;
import net.optifine.texture.PixelType;
import net.optifine.texture.TextureType;
import net.optifine.util.EntityUtils;
import net.optifine.util.collection.PropertiesOrdered;
import net.optifine.util.StrUtils;
import net.optifine.util.TimedEvent;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import net.radiant.lwjgl.opengl.ContextCapabilities;
import net.radiant.lwjgl.opengl.GLContext;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Shaders {
    public static final int ENTITY_ATTRIB = 10;
    public static final int MID_TEX_COORD_ATTRIB = 11;
    public static final int TANGENT_ATTRIB = 12;
    public static final boolean[] GBUFFERS_CLEAR = new boolean[8];
    public static final Vector4f[] GBUFFERS_CLEAR_COLOR = new Vector4f[8];
    public static final boolean[] SHADOW_HARDWARE_FILTERING_ENABLED = new boolean[2];
    public static final boolean[] SHADOW_MIPMAP_ENABLED = new boolean[2];
    public static final boolean[] SHADOW_FILTER_NEAREST = new boolean[2];
    public static final boolean[] SHADOW_COLOR_MIPMAP_ENABLED = new boolean[8];
    public static final boolean[] SHADOW_COLOR_FILTER_NEAREST = new boolean[8];
    public static final PropertyDefaultTrueFalse CONFIG_OLD_LIGHTING = new PropertyDefaultTrueFalse("oldLighting", "Classic Lighting", 0);
    public static final PropertyDefaultTrueFalse CONFIG_OLD_HAND_LIGHT = new PropertyDefaultTrueFalse("oldHandLight", "Old Hand Light", 0);
    public static final String[] TEX_MIN_FIL_DESC = new String[]{"Nearest", "Nearest-Nearest", "Nearest-Linear"};
    public static final String[] TEX_MAG_FIL_DESC = new String[]{"Nearest", "Linear"};
    public static final int[] TEX_MIN_FIL_VALUE = new int[]{9728, 9984, 9986};
    public static final int[] TEX_MAG_FIL_VALUE = new int[]{9728, 9729};
    public static final File SHADER_PACKS_DIR;
    public static final PropertyDefaultFastFancyOff SHADER_PACK_CLOUDS = new PropertyDefaultFastFancyOff("clouds", "Clouds", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_OLD_LIGHTING = new PropertyDefaultTrueFalse("oldLighting", "Classic Lighting", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_OLD_HAND_LIGHT = new PropertyDefaultTrueFalse("oldHandLight", "Old Hand Light", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_DYNAMIC_HAND_LIGHT = new PropertyDefaultTrueFalse("dynamicHandLight", "Dynamic Hand Light", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_SHADOW_TRANSLUCENT = new PropertyDefaultTrueFalse("shadowTranslucent", "Shadow Translucent", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_UNDERWATER_OVERLAY = new PropertyDefaultTrueFalse("underwaterOverlay", "Underwater Overlay", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_SUN = new PropertyDefaultTrueFalse("sun", "Sun", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_MOON = new PropertyDefaultTrueFalse("moon", "Moon", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_VIGNETTE = new PropertyDefaultTrueFalse("vignette", "Vignette", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_BACK_FACE_SOLID = new PropertyDefaultTrueFalse("backFace.solid", "Back-face Solid", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_BACK_FACE_CUTOUT = new PropertyDefaultTrueFalse("backFace.cutout", "Back-face Cutout", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_BACK_FACE_CUTOUT_MIPPED = new PropertyDefaultTrueFalse("backFace.cutoutMipped", "Back-face Cutout Mipped", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_BACK_FACE_TRANSLUCENT = new PropertyDefaultTrueFalse("backFace.translucent", "Back-face Translucent", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_RAIN_DEPTH = new PropertyDefaultTrueFalse("rain.depth", "Rain Depth", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_BEACON_BEAM_DEPTH = new PropertyDefaultTrueFalse("beacon.beam.depth", "Rain Depth", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_SEPARATE_AO = new PropertyDefaultTrueFalse("separateAo", "Separate AO", 0);
    public static final PropertyDefaultTrueFalse SHADER_PACK_FRUSTUM_CULLING = new PropertyDefaultTrueFalse("frustum.culling", "Frustum Culling", 0);
    public static final boolean SAVE_FINAL_SHADERS = System.getProperty("shaders.debug.save", "false").equals("true");
    public static final int TERRAIN_ICON_SIZE = 16;
    public static final int[] TERRAIN_TEXTURE_SIZE = new int[2];
    public static final int[] ENTITY_DATA = new int[32];
    static final float[] SUN_POSITION = new float[4];
    static final float[] MOON_POSITION = new float[4];
    static final float[] SHADOW_LIGHT_POSITION = new float[4];
    static final float[] UP_POSITION = new float[4];
    static final float[] SHADOW_LIGHT_POSITION_VECTOR = new float[4];
    static final float[] UP_POS_MODEL_VIEW = new float[]{0.0F, 100.0F, 0.0F, 0.0F};
    static final float[] SUN_POS_MODEL_VIEW = new float[]{0.0F, 100.0F, 0.0F, 0.0F};
    static final float[] MOON_POS_MODEL_VIEW = new float[]{0.0F, -100.0F, 0.0F, 0.0F};
    static final File CONFIG_FILE;
    static final int[] COLOR_TEXTURE_IMAGE_UNIT = new int[]{0, 1, 2, 3, 7, 8, 9, 10};
    static final float[] FA_PROJECTION = new float[16];
    static final float[] FA_PROJECTION_INVERSE = new float[16];
    static final float[] FA_MODEL_VIEW = new float[16];
    static final float[] FA_MODEL_VIEW_INVERSE = new float[16];
    static final float[] FA_SHADOW_PROJECTION = new float[16];
    static final float[] FA_SHADOW_PROJECTION_INVERSE = new float[16];
    static final float[] FA_SHADOW_MODEL_VIEW = new float[16];
    static final float[] FA_SHADOW_MODEL_VIEW_INVERSE = new float[16];
    private static final float[] TEMP_MAT = new float[16];
    private static final ShaderUniforms SHADER_UNIFORMS = new ShaderUniforms();
    public static final ShaderUniform4f UNIFORM_ENTITY_COLOR = SHADER_UNIFORMS.make4f("entityColor");
    public static final ShaderUniform1i UNIFORM_ENTITY_ID = SHADER_UNIFORMS.make1i("entityId");
    public static final ShaderUniform1i UNIFORM_BLOCK_ENTITY_ID = SHADER_UNIFORMS.make1i("blockEntityId");
    public static final ShaderUniform1i UNIFORM_TEXTURE = SHADER_UNIFORMS.make1i("texture");
    public static final ShaderUniform1i UNIFORM_LIGHTMAP = SHADER_UNIFORMS.make1i("lightmap");
    public static final ShaderUniform1i UNIFORM_NORMALS = SHADER_UNIFORMS.make1i("normals");
    public static final ShaderUniform1i UNIFORM_SPECULAR = SHADER_UNIFORMS.make1i("specular");
    public static final ShaderUniform1i UNIFORM_SHADOW = SHADER_UNIFORMS.make1i("shadow");
    public static final ShaderUniform1i UNIFORM_WATERSHADOW = SHADER_UNIFORMS.make1i("watershadow");
    public static final ShaderUniform1i UNIFORM_SHADOWTEX_0 = SHADER_UNIFORMS.make1i("shadowtex0");
    public static final ShaderUniform1i UNIFORM_SHADOWTEX_1 = SHADER_UNIFORMS.make1i("shadowtex1");
    public static final ShaderUniform1i UNIFORM_DEPTHTEX_0 = SHADER_UNIFORMS.make1i("depthtex0");
    public static final ShaderUniform1i UNIFORM_DEPTHTEX_1 = SHADER_UNIFORMS.make1i("depthtex1");
    public static final ShaderUniform1i UNIFORM_SHADOWCOLOR = SHADER_UNIFORMS.make1i("shadowcolor");
    public static final ShaderUniform1i UNIFORM_SHADOWCOLOR_0 = SHADER_UNIFORMS.make1i("shadowcolor0");
    public static final ShaderUniform1i UNIFORM_SHADOWCOLOR_1 = SHADER_UNIFORMS.make1i("shadowcolor1");
    public static final ShaderUniform1i UNIFORM_NOISETEX = SHADER_UNIFORMS.make1i("noisetex");
    public static final ShaderUniform1i UNIFORM_GCOLOR = SHADER_UNIFORMS.make1i("gcolor");
    public static final ShaderUniform1i UNIFORM_GDEPTH = SHADER_UNIFORMS.make1i("gdepth");
    public static final ShaderUniform1i UNIFORM_GNORMAL = SHADER_UNIFORMS.make1i("gnormal");
    public static final ShaderUniform1i UNIFORM_COMPOSITE = SHADER_UNIFORMS.make1i("composite");
    public static final ShaderUniform1i UNIFORM_GAUX_1 = SHADER_UNIFORMS.make1i("gaux1");
    public static final ShaderUniform1i UNIFORM_GAUX_2 = SHADER_UNIFORMS.make1i("gaux2");
    public static final ShaderUniform1i UNIFORM_GAUX_3 = SHADER_UNIFORMS.make1i("gaux3");
    public static final ShaderUniform1i UNIFORM_GAUX_4 = SHADER_UNIFORMS.make1i("gaux4");
    public static final ShaderUniform1i UNIFORM_COLORTEX_0 = SHADER_UNIFORMS.make1i("colortex0");
    public static final ShaderUniform1i UNIFORM_COLORTEX_1 = SHADER_UNIFORMS.make1i("colortex1");
    public static final ShaderUniform1i UNIFORM_COLORTEX_2 = SHADER_UNIFORMS.make1i("colortex2");
    public static final ShaderUniform1i UNIFORM_COLORTEX_3 = SHADER_UNIFORMS.make1i("colortex3");
    public static final ShaderUniform1i UNIFORM_COLORTEX_4 = SHADER_UNIFORMS.make1i("colortex4");
    public static final ShaderUniform1i UNIFORM_COLORTEX_5 = SHADER_UNIFORMS.make1i("colortex5");
    public static final ShaderUniform1i UNIFORM_COLORTEX_6 = SHADER_UNIFORMS.make1i("colortex6");
    public static final ShaderUniform1i UNIFORM_COLORTEX_7 = SHADER_UNIFORMS.make1i("colortex7");
    public static final ShaderUniform1i UNIFORM_GDEPTHTEX = SHADER_UNIFORMS.make1i("gdepthtex");
    public static final ShaderUniform1i UNIFORM_DEPTHTEX_2 = SHADER_UNIFORMS.make1i("depthtex2");
    public static final ShaderUniform1i UNIFORM_TEX = SHADER_UNIFORMS.make1i("tex");
    public static final ShaderUniform1i UNIFORM_HELD_ITEM_ID = SHADER_UNIFORMS.make1i("heldItemId");
    public static final ShaderUniform1i UNIFORM_HELD_BLOCK_LIGHT_VALUE = SHADER_UNIFORMS.make1i("heldBlockLightValue");
    public static final ShaderUniform1i UNIFORM_HELD_ITEM_ID_2 = SHADER_UNIFORMS.make1i("heldItemId2");
    public static final ShaderUniform1i UNIFORM_HELD_BLOCK_LIGHT_VALUE_2 = SHADER_UNIFORMS.make1i("heldBlockLightValue2");
    public static final ShaderUniform1i UNIFORM_FOG_MODE = SHADER_UNIFORMS.make1i("fogMode");
    public static final ShaderUniform1f UNIFORM_FOG_DENSITY = SHADER_UNIFORMS.make1f("fogDensity");
    public static final ShaderUniform3f UNIFORM_FOG_COLOR = SHADER_UNIFORMS.make3f("fogColor");
    public static final ShaderUniform3f UNIFORM_SKY_COLOR = SHADER_UNIFORMS.make3f("skyColor");
    public static final ShaderUniform1i UNIFORM_WORLD_TIME = SHADER_UNIFORMS.make1i("worldTime");
    public static final ShaderUniform1i UNIFORM_WORLD_DAY = SHADER_UNIFORMS.make1i("worldDay");
    public static final ShaderUniform1i UNIFORM_MOON_PHASE = SHADER_UNIFORMS.make1i("moonPhase");
    public static final ShaderUniform1i UNIFORM_FRAME_COUNTER = SHADER_UNIFORMS.make1i("frameCounter");
    public static final ShaderUniform1f UNIFORM_FRAME_TIME = SHADER_UNIFORMS.make1f("frameTime");
    public static final ShaderUniform1f UNIFORM_FRAME_TIME_COUNTER = SHADER_UNIFORMS.make1f("frameTimeCounter");
    public static final ShaderUniform1f UNIFORM_SUN_ANGLE = SHADER_UNIFORMS.make1f("sunAngle");
    public static final ShaderUniform1f UNIFORM_SHADOW_ANGLE = SHADER_UNIFORMS.make1f("shadowAngle");
    public static final ShaderUniform1f UNIFORM_RAIN_STRENGTH = SHADER_UNIFORMS.make1f("rainStrength");
    public static final ShaderUniform1f UNIFORM_ASPECT_RATIO = SHADER_UNIFORMS.make1f("aspectRatio");
    public static final ShaderUniform1f UNIFORM_VIEW_WIDTH = SHADER_UNIFORMS.make1f("viewWidth");
    public static final ShaderUniform1f UNIFORM_VIEW_HEIGHT = SHADER_UNIFORMS.make1f("viewHeight");
    public static final ShaderUniform1f UNIFORM_NEAR = SHADER_UNIFORMS.make1f("near");
    public static final ShaderUniform1f UNIFORM_FAR = SHADER_UNIFORMS.make1f("far");
    public static final ShaderUniform3f UNIFORM_SUN_POSITION = SHADER_UNIFORMS.make3f("sunPosition");
    public static final ShaderUniform3f UNIFORM_MOON_POSITION = SHADER_UNIFORMS.make3f("moonPosition");
    public static final ShaderUniform3f UNIFORM_SHADOW_LIGHT_POSITION = SHADER_UNIFORMS.make3f("shadowLightPosition");
    public static final ShaderUniform3f UNIFORM_UP_POSITION = SHADER_UNIFORMS.make3f("upPosition");
    public static final ShaderUniform3f UNIFORM_PREVIOUS_CAMERA_POSITION = SHADER_UNIFORMS.make3f("previousCameraPosition");
    public static final ShaderUniform3f UNIFORM_CAMERA_POSITION = SHADER_UNIFORMS.make3f("cameraPosition");
    public static final ShaderUniformM4 UNIFORM_GBUFFER_MODEL_VIEW = SHADER_UNIFORMS.makeM4("gbufferModelView");
    public static final ShaderUniformM4 UNIFORM_GBUFFER_MODEL_VIEW_INVERSE = SHADER_UNIFORMS.makeM4("gbufferModelViewInverse");
    public static final ShaderUniformM4 UNIFORM_GBUFFER_PREVIOUS_PROJECTION = SHADER_UNIFORMS.makeM4("gbufferPreviousProjection");
    public static final ShaderUniformM4 UNIFORM_GBUFFER_PROJECTION = SHADER_UNIFORMS.makeM4("gbufferProjection");
    public static final ShaderUniformM4 UNIFORM_GBUFFER_PROJECTION_INVERSE = SHADER_UNIFORMS.makeM4("gbufferProjectionInverse");
    public static final ShaderUniformM4 UNIFORM_GBUFFER_PREVIOUS_MODEL_VIEW = SHADER_UNIFORMS.makeM4("gbufferPreviousModelView");
    public static final ShaderUniformM4 UNIFORM_SHADOW_PROJECTION = SHADER_UNIFORMS.makeM4("shadowProjection");
    public static final ShaderUniformM4 UNIFORM_SHADOW_PROJECTION_INVERSE = SHADER_UNIFORMS.makeM4("shadowProjectionInverse");
    public static final ShaderUniformM4 UNIFORM_SHADOW_MODEL_VIEW = SHADER_UNIFORMS.makeM4("shadowModelView");
    public static final ShaderUniformM4 UNIFORM_SHADOW_MODEL_VIEW_INVERSE = SHADER_UNIFORMS.makeM4("shadowModelViewInverse");
    public static final ShaderUniform1f UNIFORM_WETNESS = SHADER_UNIFORMS.make1f("wetness");
    public static final ShaderUniform1f UNIFORM_EYE_ALTITUDE = SHADER_UNIFORMS.make1f("eyeAltitude");
    public static final ShaderUniform2i UNIFORM_EYE_BRIGHTNESS = SHADER_UNIFORMS.make2i("eyeBrightness");
    public static final ShaderUniform2i UNIFORM_EYE_BRIGHTNESS_SMOOTH = SHADER_UNIFORMS.make2i("eyeBrightnessSmooth");
    public static final ShaderUniform2i UNIFORM_TERRAIN_TEXTURE_SIZE = SHADER_UNIFORMS.make2i("terrainTextureSize");
    public static final ShaderUniform1i UNIFORM_TERRAIN_ICON_SIZE = SHADER_UNIFORMS.make1i("terrainIconSize");
    public static final ShaderUniform1i UNIFORM_IS_EYE_IN_WATER = SHADER_UNIFORMS.make1i("isEyeInWater");
    public static final ShaderUniform1f UNIFORM_NIGHT_VISION = SHADER_UNIFORMS.make1f("nightVision");
    public static final ShaderUniform1f UNIFORM_BLINDNESS = SHADER_UNIFORMS.make1f("blindness");
    public static final ShaderUniform1f UNIFORM_SCREEN_BRIGHTNESS = SHADER_UNIFORMS.make1f("screenBrightness");
    public static final ShaderUniform1i UNIFORM_HIDE_GUI = SHADER_UNIFORMS.make1i("hideGUI");
    public static final ShaderUniform1f UNIFORM_CENTER_DEPTH_SMOOTH = SHADER_UNIFORMS.make1f("centerDepthSmooth");
    public static final ShaderUniform2i UNIFORM_ATLAS_SIZE = SHADER_UNIFORMS.make2i("atlasSize");
    public static final ShaderUniform4i UNIFORM_BLEND_FUNC = SHADER_UNIFORMS.make4i("blendFunc");
    public static final ShaderUniform1i UNIFORM_INSTANCE_ID = SHADER_UNIFORMS.make1i("instanceId");
    private static final int[] GBUFFERS_FORMAT = new int[8];
    private static final Programs PROGRAMS = new Programs();
    public static final Program PROGRAM_NONE = PROGRAMS.getProgramNone();
    public static final Program PROGRAM_SHADOW = PROGRAMS.makeShadow("shadow", PROGRAM_NONE);
    public static final Program PROGRAM_SHADOW_SOLID = PROGRAMS.makeShadow("shadow_solid", PROGRAM_SHADOW);
    public static final Program PROGRAM_SHADOW_CUTOUT = PROGRAMS.makeShadow("shadow_cutout", PROGRAM_SHADOW);
    public static final Program PROGRAM_BASIC = PROGRAMS.makeGbuffers("gbuffers_basic", PROGRAM_NONE);
    public static final Program PROGRAM_TEXTURED = PROGRAMS.makeGbuffers("gbuffers_textured", PROGRAM_BASIC);
    public static final Program PROGRAM_TEXTURED_LIT = PROGRAMS.makeGbuffers("gbuffers_textured_lit", PROGRAM_TEXTURED);
    public static final Program PROGRAM_TERRAIN = PROGRAMS.makeGbuffers("gbuffers_terrain", PROGRAM_TEXTURED_LIT);
    public static final Program PROGRAM_DAMAGED_BLOCK = PROGRAMS.makeGbuffers("gbuffers_damagedblock", PROGRAM_TERRAIN);
    public static final Program PROGRAM_BLOCK = PROGRAMS.makeGbuffers("gbuffers_block", PROGRAM_TERRAIN);
    public static final Program PROGRAM_WATER = PROGRAMS.makeGbuffers("gbuffers_water", PROGRAM_TERRAIN);
    public static final Program PROGRAM_ENTITIES = PROGRAMS.makeGbuffers("gbuffers_entities", PROGRAM_TEXTURED_LIT);
    public static final Program PROGRAM_ENTITIES_GLOWING = PROGRAMS.makeGbuffers("gbuffers_entities_glowing", PROGRAM_ENTITIES);
    public static final Program PROGRAM_HAND = PROGRAMS.makeGbuffers("gbuffers_hand", PROGRAM_TEXTURED_LIT);
    public static final Program PROGRAM_HAND_WATER = PROGRAMS.makeGbuffers("gbuffers_hand_water", PROGRAM_HAND);
    public static final Program PROGRAM_WEATHER = PROGRAMS.makeGbuffers("gbuffers_weather", PROGRAM_TEXTURED_LIT);
    public static final Program PROGRAM_SKY_TEXTURED = PROGRAMS.makeGbuffers("gbuffers_skytextured", PROGRAM_TEXTURED);
    public static final Program PROGRAM_CLOUDS = PROGRAMS.makeGbuffers("gbuffers_clouds", PROGRAM_TEXTURED);
    public static final Program PROGRAM_BEACON_BEAM = PROGRAMS.makeGbuffers("gbuffers_beaconbeam", PROGRAM_TEXTURED);
    public static final Program PROGRAM_ARMOR_GLINT = PROGRAMS.makeGbuffers("gbuffers_armor_glint", PROGRAM_TEXTURED);
    public static final Program PROGRAM_SPIDER_EYES = PROGRAMS.makeGbuffers("gbuffers_spidereyes", PROGRAM_TEXTURED);
    public static final Program PROGRAM_SKY_BASIC = PROGRAMS.makeGbuffers("gbuffers_skybasic", PROGRAM_BASIC);
    public static Program activeProgram = PROGRAM_NONE;
    public static final Program PROGRAM_DEFERRED_PRE = PROGRAMS.makeVirtual("deferred_pre");
    public static final Program[] PROGRAMS_DEFERRED = PROGRAMS.makeDeferreds("deferred", 16);
    public static final Program PROGRAM_COMPOSITE_PRE = PROGRAMS.makeVirtual("composite_pre");
    public static final Program[] PROGRAMS_COMPOSITE = PROGRAMS.makeComposites("composite", 16);
    public static final Program PROGRAM_FINAL = PROGRAMS.makeComposite("final");
    public static final int PROGRAM_COUNT = PROGRAMS.getCount();
    private static final int BIG_BUFFER_SIZE = (285 + 8 * PROGRAM_COUNT) * 4;
    private static final ByteBuffer BIG_BUFFER = BufferUtils.createByteBuffer(BIG_BUFFER_SIZE).limit(0);
    static final FloatBuffer PROJECTION = nextFloatBuffer(16);
    static final FloatBuffer PROJECTION_INVERSE = nextFloatBuffer(16);
    static final FloatBuffer MODEL_VIEW = nextFloatBuffer(16);
    static final FloatBuffer MODEL_VIEW_INVERSE = nextFloatBuffer(16);
    static final FloatBuffer SHADOW_PROJECTION = nextFloatBuffer(16);
    static final FloatBuffer SHADOW_PROJECTION_INVERSE = nextFloatBuffer(16);
    static final FloatBuffer SHADOW_MODEL_VIEW = nextFloatBuffer(16);
    static final FloatBuffer SHADOW_MODEL_VIEW_INVERSE = nextFloatBuffer(16);
    static final FloatBuffer PREVIOUS_PROJECTION = nextFloatBuffer(16);
    static final FloatBuffer PREVIOUS_MODEL_VIEW = nextFloatBuffer(16);
    static final FloatBuffer TEMP_MATRIX_DIRECT_BUFFER = nextFloatBuffer(16);
    static final FloatBuffer TEMP_DIRECT_FLOAT_BUFFER = nextFloatBuffer(16);
    static final IntBuffer DFB_COLOR_TEXTURES = nextIntBuffer(16);
    static final FlipTextures DFB_COLOR_TEXTURES_FLIP = new FlipTextures(DFB_COLOR_TEXTURES, 8);
    static final IntBuffer DFB_DEPTH_TEXTURES = nextIntBuffer(3);
    static final IntBuffer SFB_COLOR_TEXTURES = nextIntBuffer(8);
    static final IntBuffer SFB_DEPTH_TEXTURES = nextIntBuffer(2);
    static final IntBuffer DFB_DRAW_BUFFERS = nextIntBuffer(8);
    static final IntBuffer SFB_DRAW_BUFFERS = nextIntBuffer(8);
    static final IntBuffer DRAW_BUFFERS_NONE = nextIntBuffer(8).limit(0);
    static final IntBuffer DRAW_BUFFERS_COLOR_ATT_0 = nextIntBuffer(8).put(36064).position(0).limit(1);
    public static final Program[] PROGRAMS_ALL = PROGRAMS.getPrograms();
    private static final ProgramStack PROGRAM_STACK = new ProgramStack();
    private static final IntList SHADER_PACK_DIMENSIONS = new IntArrayList();
    private static final String[] STAGE_NAMES = new String[]{"gbuffers", "composite", "deferred"};
    private static final String[] FORMAT_NAMES = new String[]{"R8", "RG8", "RGB8", "RGBA8", "R8_SNORM", "RG8_SNORM", "RGB8_SNORM", "RGBA8_SNORM", "R16", "RG16", "RGB16", "RGBA16", "R16_SNORM", "RG16_SNORM", "RGB16_SNORM", "RGBA16_SNORM", "R16F", "RG16F", "RGB16F", "RGBA16F", "R32F", "RG32F", "RGB32F", "RGBA32F", "R32I", "RG32I", "RGB32I", "RGBA32I", "R32UI", "RG32UI", "RGB32UI", "RGBA32UI", "R3_G3_B2", "RGB5_A1", "RGB10_A2", "R11F_G11F_B10F", "RGB9_E5"};
    private static final int[] FORMAT_IDS = new int[]{33321, 33323, 32849, 32856, 36756, 36757, 36758, 36759, 33322, 33324, 32852, 32859, 36760, 36761, 36762, 36763, 33325, 33327, 34843, 34842, 33326, 33328, 34837, 34836, 33333, 33339, 36227, 36226, 33334, 33340, 36209, 36208, 10768, 32855, 32857, 35898, 35901};
    private static final Pattern PATTERN_LOAD_ENTITY_DATA_MAP = Pattern.compile("\\s*([\\w:]+)\\s*=\\s*([-]?\\d+)\\s*");
    public static boolean isInitializedOnce = false;
    public static boolean isShaderPackInitialized = false;
    public static ContextCapabilities capabilities;
    public static String glVersionString;
    public static String glVendorString;
    public static String glRendererString;
    public static boolean hasGlGenMipmap = false;
    public static int countResetDisplayLists = 0;
    public static int renderWidth = 0;
    public static int renderHeight = 0;
    public static boolean isRenderingWorld = false;
    public static boolean isRenderingSky = false;
    public static boolean isCompositeRendered = false;
    public static boolean isRenderingDfb = false;
    public static boolean isShadowPass = false;
    public static boolean isEntitiesGlowing = false;
    public static boolean isSleeping;
    public static boolean renderItemKeepDepthMask = false;
    public static boolean itemToRenderMainTranslucent = false;
    public static boolean itemToRenderOffTranslucent = false;
    public static float wetnessHalfLife = 600.0F;
    public static float drynessHalfLife = 200.0F;
    public static float eyeBrightnessHalflife = 10.0F;
    public static boolean useEntityAttrib = false;
    public static boolean useMidTexCoordAttrib = false;
    public static boolean useTangentAttrib = false;
    public static boolean progUseEntityAttrib = false;
    public static boolean progUseMidTexCoordAttrib = false;
    public static boolean progUseTangentAttrib = false;
    public static int atlasSizeX = 0;
    public static int atlasSizeY = 0;
    public static boolean needResizeShadow = false;
    public static boolean shouldSkipDefaultShadow = false;
    public static int activeProgramID = 0;
    public static Properties shadersConfig = null;
    public static ITextureObject defaultTexture = null;
    public static boolean configTweakBlockDamage = false;
    public static boolean configCloudShadow = false;
    public static float configHandDepthMul = 0.125F;
    public static float configRenderResMul = 1.0F;
    public static float configShadowResMul = 1.0F;
    public static int configTexMinFilB = 0;
    public static int configTexMinFilN = 0;
    public static int configTexMinFilS = 0;
    public static int configTexMagFilB = 0;
    public static int configTexMagFilN = 0;
    public static int configTexMagFilS = 0;
    public static boolean configShadowClipFrustrum = true;
    public static boolean configNormalMap = true;
    public static boolean configSpecularMap = true;
    public static int configAntialiasingLevel = 0;
    public static boolean shaderPackLoaded = false;
    public static String currentShaderName;
    public static float blockLightLevel05 = 0.5F;
    public static float blockLightLevel06 = 0.6F;
    public static float blockLightLevel08 = 0.8F;
    public static float aoLevel = -1.0F;
    public static float sunPathRotation = 0.0F;
    public static int fogMode = 0;
    public static float fogDensity = 0.0F;
    public static float fogColorR;
    public static float fogColorG;
    public static float fogColorB;
    public static float shadowIntervalSize = 2.0F;
    public static int entityDataIndex = 0;
    static Minecraft mc = Minecraft.get();
    static EntityRenderer entityRenderer;
    static float clearColorR;
    static float clearColorG;
    static float clearColorB;
    static float skyColorR;
    static float skyColorG;
    static float skyColorB;
    static long worldTime = 0L;
    static long lastWorldTime = 0L;
    static long diffWorldTime = 0L;
    static float celestialAngle = 0.0F;
    static float sunAngle = 0.0F;
    static float shadowAngle = 0.0F;
    static int moonPhase = 0;
    static long systemTime = 0L;
    static long lastSystemTime = 0L;
    static long diffSystemTime = 0L;
    static int frameCounter = 0;
    static float frameTime = 0.0F;
    static float frameTimeCounter = 0.0F;
    static float rainStrength = 0.0F;
    static float wetness = 0.0F;
    static int isEyeInWater = 0;
    static int eyeBrightness = 0;
    static float eyeBrightnessFadeX = 0.0F;
    static float eyeBrightnessFadeY = 0.0F;
    static float eyePosY = 0.0F;
    static float centerDepth = 0.0F;
    static float centerDepthSmooth = 0.0F;
    static float centerDepthSmoothHalflife = 1.0F;
    static boolean centerDepthSmoothEnabled = false;
    static int superSamplingLevel = 1;
    static float nightVision = 0.0F;
    static float blindness = 0.0F;
    static boolean lightmapEnabled = false;
    static boolean fogEnabled = true;
    static double previousCameraPositionX;
    static double previousCameraPositionY;
    static double previousCameraPositionZ;
    static double cameraPositionX;
    static double cameraPositionY;
    static double cameraPositionZ;
    static int cameraOffsetX;
    static int cameraOffsetZ;
    static int shadowPassInterval = 0;
    static int shadowMapWidth = 1024;
    static int shadowMapHeight = 1024;
    static int spShadowMapWidth = 1024;
    static int spShadowMapHeight = 1024;
    static float shadowMapFOV = 90.0F;
    static float shadowMapHalfPlane = 160.0F;
    static boolean shadowMapIsOrtho = true;
    static float shadowDistanceRenderMul = -1.0F;
    static int shadowPassCounter = 0;
    static @NotNull GameSettings.Perspective preShadowPassThirdPersonView;
    static boolean waterShadowEnabled = false;
    static int usedColorBuffers = 0;
    static int usedDepthBuffers = 0;
    static int usedShadowColorBuffers = 0;
    static int usedShadowDepthBuffers = 0;
    static int usedColorAttachs = 0;
    static int usedDrawBuffers = 0;
    static int dfb = 0;
    static int sfb = 0;
    static IntBuffer activeDrawBuffers = null;
    static ShaderProfile[] shaderPackProfiles = null;
    static Map<String, ScreenShaderOptions> shaderPackGuiScreens = null;
    static Map<String, IExpressionBool> shaderPackProgramConditions = new HashMap<>();
    static Map<Block, Integer> mapBlockToEntityData;
    private static int renderDisplayWidth = 0;
    private static int renderDisplayHeight = 0;
    private static boolean isRenderingFirstPersonHand;
    private static boolean isHandRenderedMain;
    private static boolean isHandRenderedOff;
    private static boolean skipRenderHandMain;
    private static boolean skipRenderHandOff;
    private static boolean progArbGeometryShader4 = false;
    private static int progMaxVerticesOut = 3;
    private static boolean hasGeometryShaders = false;
    private static boolean hasDeferredPrograms = false;
    private static int activeCompositeMipmapSetting = 0;
    private static IShaderPack shaderPack = null;
    private static ShaderOption[] shaderPackOptions = null;
    private static Set<String> shaderPackOptionSliders = null;
    private static Map<String, String> shaderPackResources = new HashMap<>();
    private static World currentWorld = null;
    private static ICustomTexture[] customTexturesGbuffers = null;
    private static ICustomTexture[] customTexturesComposite = null;
    private static ICustomTexture[] customTexturesDeferred = null;
    private static String noiseTexturePath = null;
    private static CustomUniforms customUniforms = null;
    private static ICustomTexture noiseTexture;
    private static boolean noiseTextureEnabled = false;
    private static int noiseTextureResolution = 256;

    static {
        SHADER_PACKS_DIR = new File(Minecraft.get().mcDataDir, "shaderpacks");
        CONFIG_FILE = new File(Minecraft.get().mcDataDir, "optionsshaders.txt");
    }

    public static IntBuffer nextIntBuffer(int size) {
        ByteBuffer bytebuffer = BIG_BUFFER;
        int i = bytebuffer.limit();
        bytebuffer.position(i).limit(i + size * 4);
        return bytebuffer.asIntBuffer();
    }

    private static FloatBuffer nextFloatBuffer(int size) {
        ByteBuffer bytebuffer = BIG_BUFFER;
        int i = bytebuffer.limit();
        bytebuffer.position(i).limit(i + size * 4);
        return bytebuffer.asFloatBuffer();
    }

    public static void loadConfig() {
        Log.info("Load shaders configuration.");

        try {
            if (!SHADER_PACKS_DIR.exists()) {
                SHADER_PACKS_DIR.mkdir();
            }
        } catch (Exception exception) {
            Log.error("Failed to open the shaderpacks directory: " + SHADER_PACKS_DIR);
        }

        shadersConfig = new PropertiesOrdered();
        shadersConfig.setProperty(ShaderOptions.SHADER_PACK.getPropertyKey(), "");

        if (CONFIG_FILE.exists()) {
            try {
                FileReader filereader = new FileReader(CONFIG_FILE);
                shadersConfig.load(filereader);
                filereader.close();
            } catch (Exception _) {
            }
        }

        if (!CONFIG_FILE.exists()) {
            try {
                storeConfig();
            } catch (Exception _) {
            }
        }

        ShaderOptions[] aenumshaderoption = ShaderOptions.values();

        for (ShaderOptions enumshaderoption : aenumshaderoption) {
            String s = enumshaderoption.getPropertyKey();
            String s1 = enumshaderoption.getValueDefault();
            String s2 = shadersConfig.getProperty(s, s1);
            setEnumShaderOption(enumshaderoption, s2);
        }

        loadShaderPack();
    }

    private static void setEnumShaderOption(ShaderOptions eso, String str) {
        if (str == null) {
            str = eso.getValueDefault();
        }

        switch (eso) {
            case ANTIALIASING -> configAntialiasingLevel = Config.parseInt(str, 0);
            case NORMAL_MAP -> configNormalMap = Config.parseBoolean(str, true);
            case SPECULAR_MAP -> configSpecularMap = Config.parseBoolean(str, true);
            case RENDER_RES_MUL -> configRenderResMul = Config.parseFloat(str, 1.0F);
            case SHADOW_RES_MUL -> configShadowResMul = Config.parseFloat(str, 1.0F);
            case HAND_DEPTH_MUL -> configHandDepthMul = Config.parseFloat(str, 0.125F);
            case CLOUD_SHADOW -> configCloudShadow = Config.parseBoolean(str, true);
            case OLD_HAND_LIGHT -> CONFIG_OLD_HAND_LIGHT.setPropertyValue(str);
            case OLD_LIGHTING -> CONFIG_OLD_LIGHTING.setPropertyValue(str);
            case SHADER_PACK -> currentShaderName = str;
            case TWEAK_BLOCK_DAMAGE -> configTweakBlockDamage = Config.parseBoolean(str, true);
            case SHADOW_CLIP_FRUSTRUM -> configShadowClipFrustrum = Config.parseBoolean(str, true);
            case TEX_MIN_FIL_B -> configTexMinFilB = Config.parseInt(str, 0);
            case TEX_MIN_FIL_N -> configTexMinFilN = Config.parseInt(str, 0);
            case TEX_MIN_FIL_S -> configTexMinFilS = Config.parseInt(str, 0);
            case TEX_MAG_FIL_B -> configTexMagFilB = Config.parseInt(str, 0);
            case TEX_MAG_FIL_N -> configTexMagFilB = Config.parseInt(str, 0);
            case TEX_MAG_FIL_S -> configTexMagFilB = Config.parseInt(str, 0);
            default -> throw new IllegalArgumentException("Unknown option: " + eso);
        }
    }

    public static void storeConfig() {
        Log.info("Save shaders configuration.");

        if (shadersConfig == null) {
            shadersConfig = new PropertiesOrdered();
        }

        ShaderOptions[] aenumshaderoption = ShaderOptions.values();

        for (ShaderOptions enumshaderoption : aenumshaderoption) {
            String s = enumshaderoption.getPropertyKey();
            String s1 = getEnumShaderOption(enumshaderoption);
            shadersConfig.setProperty(s, s1);
        }

        try {
            FileWriter filewriter = new FileWriter(CONFIG_FILE);
            shadersConfig.store(filewriter, null);
            filewriter.close();
        } catch (Exception exception) {
            Log.error("Error saving configuration: " + exception.getClass().getName() + ": " + exception.getMessage());
        }
    }

    public static String getEnumShaderOption(ShaderOptions eso) {
        return switch (eso) {
            case ANTIALIASING -> Integer.toString(configAntialiasingLevel);
            case NORMAL_MAP -> Boolean.toString(configNormalMap);
            case SPECULAR_MAP -> Boolean.toString(configSpecularMap);
            case RENDER_RES_MUL -> Float.toString(configRenderResMul);
            case SHADOW_RES_MUL -> Float.toString(configShadowResMul);
            case HAND_DEPTH_MUL -> Float.toString(configHandDepthMul);
            case CLOUD_SHADOW -> Boolean.toString(configCloudShadow);
            case OLD_HAND_LIGHT -> CONFIG_OLD_HAND_LIGHT.getPropertyValue();
            case OLD_LIGHTING -> CONFIG_OLD_LIGHTING.getPropertyValue();
            case SHADER_PACK -> currentShaderName;
            case TWEAK_BLOCK_DAMAGE -> Boolean.toString(configTweakBlockDamage);
            case SHADOW_CLIP_FRUSTRUM -> Boolean.toString(configShadowClipFrustrum);
            case TEX_MIN_FIL_B -> Integer.toString(configTexMinFilB);
            case TEX_MIN_FIL_N -> Integer.toString(configTexMinFilN);
            case TEX_MIN_FIL_S -> Integer.toString(configTexMinFilS);
            case TEX_MAG_FIL_B, TEX_MAG_FIL_N, TEX_MAG_FIL_S -> Integer.toString(configTexMagFilB);
        };
    }

    public static void loadShaderPack() {
        boolean flag = shaderPackLoaded;
        boolean flag1 = isOldLighting();

        if (mc.renderGlobal != null) {
            mc.renderGlobal.pauseChunkUpdates();
        }

        shaderPackLoaded = false;

        if (shaderPack != null) {
            shaderPack.close();
            shaderPack = null;
            shaderPackResources.clear();
            SHADER_PACK_DIMENSIONS.clear();
            shaderPackOptions = null;
            shaderPackOptionSliders = null;
            shaderPackProfiles = null;
            shaderPackGuiScreens = null;
            shaderPackProgramConditions.clear();
            SHADER_PACK_CLOUDS.resetValue();
            SHADER_PACK_OLD_HAND_LIGHT.resetValue();
            SHADER_PACK_DYNAMIC_HAND_LIGHT.resetValue();
            SHADER_PACK_OLD_LIGHTING.resetValue();
            resetCustomTextures();
            noiseTexturePath = null;
        }

        boolean flag2 = false;

        if (Config.isAntialiasing()) {
            Log.info("Shaders can not be loaded, Antialiasing is enabled: " + Config.getAntialiasingLevel() + "x");
            flag2 = true;
        }

        if (Config.isAnisotropicFiltering()) {
            Log.info("Shaders can not be loaded, Anisotropic Filtering is enabled: " + Config.getAnisotropicFilterLevel() + "x");
            flag2 = true;
        }

        if (Config.isFastRender()) {
            Log.info("Shaders can not be loaded, Fast Render is enabled.");
            flag2 = true;
        }

        String s = shadersConfig.getProperty(ShaderOptions.SHADER_PACK.getPropertyKey(), "(internal)");

        if (!flag2) {
            shaderPack = getShaderPack(s);
            shaderPackLoaded = shaderPack != null;
        }

        if (shaderPackLoaded) {
            Log.info("Loaded shaderpack: " + getShaderPackName());
        } else {
            Log.info("No shaderpack loaded.");
            shaderPack = new ShaderPackNone();
        }

        if (SAVE_FINAL_SHADERS) {
            clearDirectory(new File(SHADER_PACKS_DIR, "debug"));
        }

        loadShaderPackResources();
        loadShaderPackDimensions();
        shaderPackOptions = loadShaderPackOptions();
        loadShaderPackProperties();
        boolean flag3 = shaderPackLoaded != flag;
        boolean flag4 = isOldLighting() != flag1;

        if (flag3 || flag4) {
            DefaultVertexFormats.updateVertexFormats();
            updateBlockLightLevel();
        }

        if (mc.getResourcePackRepository() != null) {
            CustomBlockLayers.update();
        }

        if (mc.renderGlobal != null) {
            mc.renderGlobal.resumeChunkUpdates();
        }

        if ((flag3 || flag4) && mc.getResourceManager() != null) {
            mc.scheduleResourcesRefresh();
        }
    }

    public static IShaderPack getShaderPack(String name) {
        if (name == null) {
            return null;
        } else {
            name = name.trim();

            if (!name.isEmpty() && !name.equals("OFF")) {
                if (name.equals("(internal)")) {
                    return new ShaderPackDefault();
                } else {
                    try {
                        File file1 = new File(SHADER_PACKS_DIR, name);
                        return file1.isDirectory() ? new ShaderPackFolder(name, file1) : (file1.isFile() && name.toLowerCase().endsWith(".zip") ? new ShaderPackZip(name, file1) : null);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        return null;
                    }
                }
            } else {
                return null;
            }
        }
    }

    public static IShaderPack getShaderPack() {
        return shaderPack;
    }

    public static void setShaderPack(String par1name) {
        currentShaderName = par1name;
        shadersConfig.setProperty(ShaderOptions.SHADER_PACK.getPropertyKey(), par1name);
        loadShaderPack();
    }

    private static void loadShaderPackDimensions() {
        SHADER_PACK_DIMENSIONS.clear();

        for (int i = -128; i <= 128; ++i) {
            String s = "/shaders/world" + i;

            if (shaderPack.hasDirectory(s)) {
                SHADER_PACK_DIMENSIONS.add(i);
            }
        }

        if (!SHADER_PACK_DIMENSIONS.isEmpty()) {
            Integer[] ainteger = SHADER_PACK_DIMENSIONS.toArray(new Integer[0]);
            Log.info("[Shaders] Worlds: " + Config.arrayToString(ainteger));
        }
    }

    private static void loadShaderPackProperties() {
        SHADER_PACK_CLOUDS.resetValue();
        SHADER_PACK_OLD_HAND_LIGHT.resetValue();
        SHADER_PACK_DYNAMIC_HAND_LIGHT.resetValue();
        SHADER_PACK_OLD_LIGHTING.resetValue();
        SHADER_PACK_SHADOW_TRANSLUCENT.resetValue();
        SHADER_PACK_UNDERWATER_OVERLAY.resetValue();
        SHADER_PACK_SUN.resetValue();
        SHADER_PACK_MOON.resetValue();
        SHADER_PACK_VIGNETTE.resetValue();
        SHADER_PACK_BACK_FACE_SOLID.resetValue();
        SHADER_PACK_BACK_FACE_CUTOUT.resetValue();
        SHADER_PACK_BACK_FACE_CUTOUT_MIPPED.resetValue();
        SHADER_PACK_BACK_FACE_TRANSLUCENT.resetValue();
        SHADER_PACK_RAIN_DEPTH.resetValue();
        SHADER_PACK_BEACON_BEAM_DEPTH.resetValue();
        SHADER_PACK_SEPARATE_AO.resetValue();
        SHADER_PACK_FRUSTUM_CULLING.resetValue();
        BlockAliases.reset();
        ItemAliases.reset();
        EntityAliases.reset();
        customUniforms = null;

        for (Program program : PROGRAMS_ALL) {
            program.resetProperties();
        }

        if (shaderPack != null) {
            BlockAliases.update(shaderPack);
            ItemAliases.update(shaderPack);
            EntityAliases.update(shaderPack);
            String s = "/shaders/shaders.properties";

            try {
                InputStream inputstream = shaderPack.getResourceAsStream(s);

                if (inputstream == null) {
                    return;
                }

                inputstream = MacroProcessor.process(inputstream, s);
                Properties properties = new PropertiesOrdered();
                properties.load(inputstream);
                inputstream.close();
                SHADER_PACK_CLOUDS.loadFrom(properties);
                SHADER_PACK_OLD_HAND_LIGHT.loadFrom(properties);
                SHADER_PACK_DYNAMIC_HAND_LIGHT.loadFrom(properties);
                SHADER_PACK_OLD_LIGHTING.loadFrom(properties);
                SHADER_PACK_SHADOW_TRANSLUCENT.loadFrom(properties);
                SHADER_PACK_UNDERWATER_OVERLAY.loadFrom(properties);
                SHADER_PACK_SUN.loadFrom(properties);
                SHADER_PACK_VIGNETTE.loadFrom(properties);
                SHADER_PACK_MOON.loadFrom(properties);
                SHADER_PACK_BACK_FACE_SOLID.loadFrom(properties);
                SHADER_PACK_BACK_FACE_CUTOUT.loadFrom(properties);
                SHADER_PACK_BACK_FACE_CUTOUT_MIPPED.loadFrom(properties);
                SHADER_PACK_BACK_FACE_TRANSLUCENT.loadFrom(properties);
                SHADER_PACK_RAIN_DEPTH.loadFrom(properties);
                SHADER_PACK_BEACON_BEAM_DEPTH.loadFrom(properties);
                SHADER_PACK_SEPARATE_AO.loadFrom(properties);
                SHADER_PACK_FRUSTUM_CULLING.loadFrom(properties);
                shaderPackOptionSliders = ShaderPackParser.parseOptionSliders(properties, shaderPackOptions);
                shaderPackProfiles = ShaderPackParser.parseProfiles(properties, shaderPackOptions);
                shaderPackGuiScreens = ShaderPackParser.parseGuiScreens(properties, shaderPackProfiles, shaderPackOptions);
                shaderPackProgramConditions = ShaderPackParser.parseProgramConditions(properties, shaderPackOptions);
                customTexturesGbuffers = loadCustomTextures(properties, 0);
                customTexturesComposite = loadCustomTextures(properties, 1);
                customTexturesDeferred = loadCustomTextures(properties, 2);
                noiseTexturePath = properties.getProperty("texture.noise");

                if (noiseTexturePath != null) {
                    noiseTextureEnabled = true;
                }

                customUniforms = ShaderPackParser.parseCustomUniforms(properties);
                ShaderPackParser.parseAlphaStates(properties);
                ShaderPackParser.parseBlendStates(properties);
                ShaderPackParser.parseRenderScales(properties);
                ShaderPackParser.parseBuffersFlip(properties);
            } catch (IOException exception) {
                Log.error("[Shaders] Error reading: " + s);
            }
        }
    }

    private static ICustomTexture[] loadCustomTextures(Properties props, int stage) {
        String s = "texture." + STAGE_NAMES[stage] + ".";
        Set<Object> set = props.keySet();
        List<ICustomTexture> list = new ArrayList<>();

        for (Object o : set) {
            String s1 = (String) o;
            if (s1.startsWith(s)) {
                String s2 = StrUtils.removePrefix(s1, s);
                s2 = StrUtils.removeSuffix(s2, new String[]{".0", ".1", ".2", ".3", ".4", ".5", ".6", ".7", ".8", ".9"});
                String s3 = props.getProperty(s1).trim();
                int i = getTextureIndex(stage, s2);

                if (i < 0) {
                    Log.warn("Invalid texture name: " + s1);
                } else {
                    ICustomTexture icustomtexture = loadCustomTexture(i, s3);

                    if (icustomtexture != null) {
                        Log.info("Custom texture: " + s1 + " = " + s3);
                        list.add(icustomtexture);
                    }
                }
            }
        }

        if (list.isEmpty()) {
            return null;
        } else {
            return list.toArray(new ICustomTexture[0]);
        }
    }

    private static ICustomTexture loadCustomTexture(int textureUnit, String path) {
        if (path == null) {
            return null;
        } else {
            path = path.trim();
            return path.indexOf(58) >= 0 ? loadCustomTextureLocation(textureUnit, path) : (path.indexOf(32) >= 0 ? loadCustomTextureRaw(textureUnit, path) : loadCustomTextureShaders(textureUnit, path));
        }
    }

    private static ICustomTexture loadCustomTextureLocation(int textureUnit, String path) {
        String s = path.trim();
        int i = 0;

        if (s.startsWith("minecraft:textures/")) {
            s = StrUtils.addSuffixCheck(s, ".png");

            if (s.endsWith("_n.png")) {
                s = StrUtils.replaceSuffix(s, "_n.png", ".png");
                i = 1;
            } else if (s.endsWith("_s.png")) {
                s = StrUtils.replaceSuffix(s, "_s.png", ".png");
                i = 2;
            }
        }

        ResourceLocation resourcelocation = new ResourceLocation(s);
        return new CustomTextureLocation(textureUnit, resourcelocation, i);
    }

    private static ICustomTexture loadCustomTextureRaw(int textureUnit, String line) {
        ConnectedParser connectedparser = new ConnectedParser("Shaders");
        String[] astring = Config.tokenize(line, " ");
        Deque<String> deque = new ArrayDeque<>(Arrays.asList(astring));
        String s = deque.poll();
        TextureType texturetype = (TextureType) connectedparser.parseEnum(deque.poll(), TextureType.values(), "texture type");

        if (texturetype == null) {
            Log.warn("Invalid raw texture type: " + line);
            return null;
        } else {
            InternalFormat internalformat = (InternalFormat) connectedparser.parseEnum(deque.poll(), InternalFormat.values(), "internal format");

            if (internalformat == null) {
                Log.warn("Invalid raw texture internal format: " + line);
                return null;
            } else {
                int i;
                int j = 0;
                int k = 0;

                switch (texturetype) {
                    case TEXTURE_1D:
                        i = connectedparser.parseInt(deque.poll(), -1);
                        break;

                    case TEXTURE_2D:
                        i = connectedparser.parseInt(deque.poll(), -1);
                        j = connectedparser.parseInt(deque.poll(), -1);
                        break;

                    case TEXTURE_3D:
                        i = connectedparser.parseInt(deque.poll(), -1);
                        j = connectedparser.parseInt(deque.poll(), -1);
                        k = connectedparser.parseInt(deque.poll(), -1);
                        break;

                    case TEXTURE_RECTANGLE:
                        i = connectedparser.parseInt(deque.poll(), -1);
                        j = connectedparser.parseInt(deque.poll(), -1);
                        break;

                    default:
                        Log.warn("Invalid raw texture type: " + texturetype);
                        return null;
                }

                if (i >= 0 && j >= 0 && k >= 0) {
                    PixelFormat pixelformat = (PixelFormat) connectedparser.parseEnum(deque.poll(), PixelFormat.values(), "pixel format");

                    if (pixelformat == null) {
                        Log.warn("Invalid raw texture pixel format: " + line);
                        return null;
                    } else {
                        PixelType pixeltype = (PixelType) connectedparser.parseEnum(deque.poll(), PixelType.values(), "pixel type");

                        if (pixeltype == null) {
                            Log.warn("Invalid raw texture pixel type: " + line);
                            return null;
                        } else if (!deque.isEmpty()) {
                            Log.warn("Invalid raw texture, too many parameters: " + line);
                            return null;
                        } else {
                            return loadCustomTextureRaw(textureUnit, line, s, texturetype, internalformat, i, j, k, pixelformat, pixeltype);
                        }
                    }
                } else {
                    Log.warn("Invalid raw texture size: " + line);
                    return null;
                }
            }
        }
    }

    private static ICustomTexture loadCustomTextureRaw(int textureUnit, String line, String path, TextureType type, InternalFormat internalFormat, int width, int height, int depth, PixelFormat pixelFormat, PixelType pixelType) {
        try {
            String s = "shaders/" + StrUtils.removePrefix(path, "/");
            InputStream inputstream = shaderPack.getResourceAsStream(s);

            if (inputstream == null) {
                Log.warn("Raw texture not found: " + path);
                return null;
            } else {
                byte[] abyte = Config.readAll(inputstream);
                IOUtils.closeQuietly(inputstream);
                ByteBuffer bytebuffer = GLAllocation.createDirectByteBuffer(abyte.length);
                bytebuffer.put(abyte);
                bytebuffer.flip();
                TextureMetadataSection texturemetadatasection = SimpleShaderTexture.loadTextureMetadataSection(s, new TextureMetadataSection(true, true, new IntArrayList()));
                return new CustomTextureRaw(type, internalFormat, width, height, depth, pixelFormat, pixelType, bytebuffer, textureUnit, texturemetadatasection.getTextureBlur(), texturemetadatasection.getTextureClamp());
            }
        } catch (IOException exception) {
            Log.warn("Error loading raw texture: " + path);
            Log.warn(exception.getClass().getName() + ": " + exception.getMessage());
            return null;
        }
    }

    private static ICustomTexture loadCustomTextureShaders(int textureUnit, String path) {
        path = path.trim();

        if (path.indexOf(46) < 0) {
            path = path + ".png";
        }

        try {
            String s = "shaders/" + StrUtils.removePrefix(path, "/");
            InputStream inputstream = shaderPack.getResourceAsStream(s);

            if (inputstream == null) {
                Log.warn("Texture not found: " + path);
                return null;
            } else {
                IOUtils.closeQuietly(inputstream);
                SimpleShaderTexture simpleshadertexture = new SimpleShaderTexture(s);
                simpleshadertexture.loadTexture(mc.getResourceManager());
                return new CustomTexture(textureUnit, s, simpleshadertexture);
            }
        } catch (IOException exception) {
            Log.warn("Error loading texture: " + path);
            Log.warn(exception.getClass().getName() + ": " + exception.getMessage());
            return null;
        }
    }

    private static int getTextureIndex(int stage, String name) {
        if (stage == 0) {
            switch (name) {
                case "texture" -> {
                    return 0;
                }
                case "lightmap" -> {
                    return 1;
                }
                case "normals" -> {
                    return 2;
                }
                case "specular" -> {
                    return 3;
                }
                case "shadowtex0", "watershadow" -> {
                    return 4;
                }
                case "shadow" -> {
                    return waterShadowEnabled ? 5 : 4;
                }
                case "shadowtex1" -> {
                    return 5;
                }
                case "depthtex0" -> {
                    return 6;
                }
                case "gaux1" -> {
                    return 7;
                }
                case "gaux2" -> {
                    return 8;
                }
                case "gaux3" -> {
                    return 9;
                }
                case "gaux4" -> {
                    return 10;
                }
                case "depthtex1" -> {
                    return 12;
                }
                case "shadowcolor0", "shadowcolor" -> {
                    return 13;
                }
                case "shadowcolor1" -> {
                    return 14;
                }
                case "noisetex" -> {
                    return 15;
                }
            }

        }

        if (stage == 1 || stage == 2) {
            if (name.equals("colortex0")) {
                return 0;
            }

            switch (name) {
                case "colortex1", "gdepth" -> {
                    return 1;
                }
                case "colortex2", "gnormal" -> {
                    return 2;
                }
                case "colortex3", "composite" -> {
                    return 3;
                }
                case "shadowtex0", "watershadow" -> {
                    return 4;
                }
                case "shadow" -> {
                    return waterShadowEnabled ? 5 : 4;
                }
                case "shadowtex1" -> {
                    return 5;
                }
                case "depthtex0", "gdepthtex" -> {
                    return 6;
                }
                case "colortex4", "gaux1" -> {
                    return 7;
                }
                case "colortex5", "gaux2" -> {
                    return 8;
                }
                case "colortex6", "gaux3" -> {
                    return 9;
                }
                case "colortex7", "gaux4" -> {
                    return 10;
                }
                case "depthtex1" -> {
                    return 11;
                }
                case "depthtex2" -> {
                    return 12;
                }
                case "shadowcolor0", "shadowcolor" -> {
                    return 13;
                }
                case "shadowcolor1" -> {
                    return 14;
                }
                case "noisetex" -> {
                    return 15;
                }
            }

        }

        return -1;
    }

    private static void bindCustomTextures(ICustomTexture[] cts) {
        if (cts != null) {
            for (ICustomTexture icustomtexture : cts) {
                GlStateManager.setActiveTexture(33984 + icustomtexture.textureUnit());
                int j = icustomtexture.getTextureId();
                int k = icustomtexture.getTarget();

                if (k == 3553) {
                    GlStateManager.bindTexture(j);
                } else {
                    GL11.glBindTexture(k, j);
                }
            }
        }
    }

    private static void resetCustomTextures() {
        deleteCustomTextures(customTexturesGbuffers);
        deleteCustomTextures(customTexturesComposite);
        deleteCustomTextures(customTexturesDeferred);
        customTexturesGbuffers = null;
        customTexturesComposite = null;
        customTexturesDeferred = null;
    }

    private static void deleteCustomTextures(ICustomTexture[] cts) {
        if (cts != null) {
            for (ICustomTexture icustomtexture : cts) {
                icustomtexture.deleteTexture();
            }
        }
    }

    public static ShaderOption[] getShaderPackOptions(String screenName) {
        ShaderOption[] ashaderoption = shaderPackOptions.clone();

        if (shaderPackGuiScreens == null) {
            if (shaderPackProfiles != null) {
                ShaderOptionProfile shaderoptionprofile = new ShaderOptionProfile(shaderPackProfiles, ashaderoption);
                ashaderoption = (ShaderOption[]) Config.addObjectToArray(ashaderoption, shaderoptionprofile, 0);
            }

            ashaderoption = getVisibleOptions(ashaderoption);
            return ashaderoption;
        } else {
            String s = screenName != null ? "screen." + screenName : "screen";
            ScreenShaderOptions screenshaderoptions = shaderPackGuiScreens.get(s);

            if (screenshaderoptions == null) {
                return new ShaderOption[0];
            } else {
                ShaderOption[] ashaderoption1 = screenshaderoptions.shaderOptions();
                List<ShaderOption> list = new ArrayList<>();

                for (ShaderOption shaderoption : ashaderoption1) {
                    if (shaderoption == null) {
                        list.add(null);
                    } else if (shaderoption instanceof ShaderOptionRest) {
                        ShaderOption[] ashaderoption2 = getShaderOptionsRest(shaderPackGuiScreens, ashaderoption);
                        list.addAll(Arrays.asList(ashaderoption2));
                    } else {
                        list.add(shaderoption);
                    }
                }

                return list.toArray(new ShaderOption[0]);
            }
        }
    }

    public static int getShaderPackColumns(String screenName, int def) {
        String s = screenName != null ? "screen." + screenName : "screen";

        if (shaderPackGuiScreens == null) {
            return def;
        } else {
            ScreenShaderOptions screenshaderoptions = shaderPackGuiScreens.get(s);
            return screenshaderoptions == null ? def : screenshaderoptions.columns();
        }
    }

    private static ShaderOption[] getShaderOptionsRest(Map<String, ScreenShaderOptions> mapScreens, ShaderOption[] ops) {
        Set<String> set = new HashSet<>();

        for (String s : mapScreens.keySet()) {
            ScreenShaderOptions screenshaderoptions = mapScreens.get(s);
            ShaderOption[] ashaderoption = screenshaderoptions.shaderOptions();

            for (ShaderOption shaderoption : ashaderoption) {
                if (shaderoption != null) {
                    set.add(shaderoption.getName());
                }
            }
        }

        List<ShaderOption> list = new ArrayList<>();

        for (ShaderOption shaderoption1 : ops) {
            if (shaderoption1.isVisible()) {
                String s1 = shaderoption1.getName();

                if (!set.contains(s1)) {
                    list.add(shaderoption1);
                }
            }
        }

        return list.toArray(new ShaderOption[0]);
    }

    public static ShaderOption getShaderOption(String name) {
        return ShaderUtils.getShaderOption(name, shaderPackOptions);
    }

    public static ShaderOption[] getShaderPackOptions() {
        return shaderPackOptions;
    }

    public static boolean isShaderPackOptionSlider(String name) {
        return shaderPackOptionSliders != null && shaderPackOptionSliders.contains(name);
    }

    private static ShaderOption[] getVisibleOptions(ShaderOption[] ops) {
        List<ShaderOption> list = new ArrayList<>();

        for (ShaderOption shaderoption : ops) {
            if (shaderoption.isVisible()) {
                list.add(shaderoption);
            }
        }

        return list.toArray(new ShaderOption[0]);
    }

    public static void saveShaderPackOptions() {
        saveShaderPackOptions(shaderPackOptions, shaderPack);
    }

    private static void saveShaderPackOptions(ShaderOption[] sos, IShaderPack sp) {
        Properties properties = new PropertiesOrdered();

        if (shaderPackOptions != null) {
            for (ShaderOption shaderoption : sos) {
                if (shaderoption.isChanged() && shaderoption.isEnabled()) {
                    properties.setProperty(shaderoption.getName(), shaderoption.getValue());
                }
            }
        }

        try {
            saveOptionProperties(sp, properties);
        } catch (IOException exception) {
            Log.error("[Shaders] Error saving configuration for " + shaderPack.getName());
            exception.printStackTrace();
        }
    }

    private static void saveOptionProperties(IShaderPack sp, Properties props) throws IOException {
        String s = "shaderpacks/" + sp.getName() + ".txt";
        File file1 = new File(Minecraft.get().mcDataDir, s);

        if (props.isEmpty()) {
            file1.delete();
        } else {
            FileOutputStream fileoutputstream = new FileOutputStream(file1);
            props.store(fileoutputstream, null);
            fileoutputstream.flush();
            fileoutputstream.close();
        }
    }

    private static ShaderOption[] loadShaderPackOptions() {
        try {
            String[] astring = PROGRAMS.getProgramNames();
            ShaderOption[] ashaderoption = ShaderPackParser.parseShaderPackOptions(shaderPack, astring, SHADER_PACK_DIMENSIONS);
            Properties properties = loadOptionProperties(shaderPack);

            for (ShaderOption shaderoption : ashaderoption) {
                String s = properties.getProperty(shaderoption.getName());

                if (s != null) {
                    shaderoption.resetValue();

                    if (!shaderoption.setValue(s)) {
                        Log.error("[Shaders] Invalid value, option: " + shaderoption.getName() + ", value: " + s);
                    }
                }
            }

            return ashaderoption;
        } catch (IOException exception) {
            Log.error("[Shaders] Error reading configuration for " + shaderPack.getName());
            exception.printStackTrace();
            return null;
        }
    }

    private static Properties loadOptionProperties(IShaderPack sp) throws IOException {
        Properties properties = new PropertiesOrdered();
        String s = "shaderpacks/" + sp.getName() + ".txt";
        File file1 = new File(Minecraft.get().mcDataDir, s);

        if (file1.exists() && file1.isFile() && file1.canRead()) {
            FileInputStream fileinputstream = new FileInputStream(file1);
            properties.load(fileinputstream);
            fileinputstream.close();
            return properties;
        } else {
            return properties;
        }
    }

    public static ShaderOption[] getChangedOptions(ShaderOption[] ops) {
        List<ShaderOption> list = new ArrayList<>();

        for (ShaderOption shaderoption : ops) {
            if (shaderoption.isEnabled() && shaderoption.isChanged()) {
                list.add(shaderoption);
            }
        }

        return list.toArray(new ShaderOption[0]);
    }

    private static String applyOptions(String line, ShaderOption[] ops) {
        if (ops != null) {
            for (ShaderOption shaderoption : ops) {
                if (shaderoption.matchesLine(line)) {
                    line = shaderoption.getSourceLine();
                    break;
                }
            }

        }
        return line;
    }

    public static ArrayList<String> listOfShaders() {
        ArrayList<String> arraylist = new ArrayList<>();
        arraylist.add("OFF");
        arraylist.add("(internal)");
        int i = arraylist.size();

        try {
            if (!SHADER_PACKS_DIR.exists()) {
                SHADER_PACKS_DIR.mkdir();
            }

            File[] afile = SHADER_PACKS_DIR.listFiles();

            for (File file1 : afile) {
                String s = file1.getName();

                if (file1.isDirectory()) {
                    if (!s.equals("debug")) {
                        File file2 = new File(file1, "shaders");

                        if (file2.exists() && file2.isDirectory()) {
                            arraylist.add(s);
                        }
                    }
                } else if (file1.isFile() && s.toLowerCase().endsWith(".zip")) {
                    arraylist.add(s);
                }
            }
        } catch (Exception _) {
        }

        List<String> list = arraylist.subList(i, arraylist.size());
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return arraylist;
    }

    public static int checkFramebufferStatus(String location) {
        int i = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);

        if (i != 36053) {
            System.err.format("FramebufferStatus 0x%04X at %s\n", i, location);
        }

        return i;
    }

    public static int checkGLError(String location) {
        int i = GlStateManager.glGetError();

        if (i != 0 && GlErrors.isEnabled(i)) {
            String s = Config.getGlErrorString(i);
            String s1 = getErrorInfo(i, location);
            String s2 = String.format("OpenGL error: %s (%s)%s, at: %s", i, s, s1, location);
            Log.error(s2);

            if (Config.isShowGlErrors() && TimedEvent.isActive("ShowGlErrorShaders", 10000L)) {
                String s3 = I18n.format("of.message.openglError", i, s);
                printChat(s3);
            }
        }

        return i;
    }

    private static String getErrorInfo(int errorCode, String location) {
        StringBuilder stringbuilder = new StringBuilder();

        if (errorCode == 1286) {
            int i = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);
            String s = getFramebufferStatusText(i);
            String s1 = ", fbStatus: " + i + " (" + s + ")";
            stringbuilder.append(s1);
        }

        String s2 = activeProgram.getName();

        if (s2.isEmpty()) {
            s2 = "none";
        }

        stringbuilder.append(", program: ").append(s2);
        Program program = getProgramById(activeProgramID);

        if (program != activeProgram) {
            String s3 = program.getName();

            if (s3.isEmpty()) {
                s3 = "none";
            }

            stringbuilder.append(" (").append(s3).append(")");
        }

        if (location.equals("setDrawBuffers")) {
            stringbuilder.append(", drawBuffers: ").append(activeProgram.getDrawBufSettings());
        }

        return stringbuilder.toString();
    }

    private static Program getProgramById(int programID) {
        for (Program program : PROGRAMS_ALL) {
            if (program.getId() == programID) {
                return program;
            }
        }

        return PROGRAM_NONE;
    }

    private static String getFramebufferStatusText(int fbStatusCode) {
        return switch (fbStatusCode) {
            case 33305 -> "Undefined";
            case 36053 -> "Complete";
            case 36054 -> "Incomplete attachment";
            case 36055 -> "Incomplete missing attachment";
            case 36059 -> "Incomplete draw buffer";
            case 36060 -> "Incomplete read buffer";
            case 36061 -> "Unsupported";
            case 36182 -> "Incomplete multisample";
            case 36264 -> "Incomplete layer targets";
            default -> "Unknown";
        };
    }

    private static void printChat(String str) {
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(str));
    }

    private static void printChatAndLogError(String str) {
        Log.error(str);
        mc.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(str));
    }

    public static void printIntBuffer(String title, IntBuffer buf) {
        StringBuilder stringbuilder = new StringBuilder(128);
        stringbuilder.append(title).append(" [pos ").append(buf.position()).append(" lim ").append(buf.limit()).append(" cap ").append(buf.capacity()).append(" :");
        int i = buf.limit();

        for (int j = 0; j < i; ++j) {
            stringbuilder.append(" ").append(buf.get(j));
        }

        stringbuilder.append("]");
        Log.info(stringbuilder.toString());
    }

    public static void startup() {
        capabilities = GLContext.getCapabilities();
        glVersionString = GL11.glGetString(GL11.GL_VERSION);
        glVendorString = GL11.glGetString(GL11.GL_VENDOR);
        glRendererString = GL11.glGetString(GL11.GL_RENDERER);
        Log.info("OpenGL Version: " + glVersionString);
        Log.info("Vendor:  " + glVendorString);
        Log.info("Renderer: " + glRendererString);
        Log.info("Capabilities: " + (capabilities.OpenGL20 ? " 2.0 " : " - ") + (capabilities.OpenGL21 ? " 2.1 " : " - ") + (capabilities.OpenGL30 ? " 3.0 " : " - ") + (capabilities.OpenGL32 ? " 3.2 " : " - ") + (capabilities.OpenGL40 ? " 4.0 " : " - "));
        Log.info("GL_MAX_DRAW_BUFFERS: " + GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS));
        Log.info("GL_MAX_COLOR_ATTACHMENTS_EXT: " + GL11.glGetInteger(36063));
        Log.info("GL_MAX_TEXTURE_IMAGE_UNITS: " + GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS));
        hasGlGenMipmap = capabilities.OpenGL30;
        loadConfig();
    }

    public static void updateBlockLightLevel() {
        if (isOldLighting()) {
            blockLightLevel05 = 0.5F;
            blockLightLevel06 = 0.6F;
            blockLightLevel08 = 0.8F;
        } else {
            blockLightLevel05 = 1.0F;
            blockLightLevel06 = 1.0F;
            blockLightLevel08 = 1.0F;
        }
    }

    public static boolean isOldHandLight() {
        return !CONFIG_OLD_HAND_LIGHT.isDefault() ? CONFIG_OLD_HAND_LIGHT.isTrue() : (SHADER_PACK_OLD_HAND_LIGHT.isDefault() || SHADER_PACK_OLD_HAND_LIGHT.isTrue());
    }

    public static boolean isDynamicHandLight() {
        return SHADER_PACK_DYNAMIC_HAND_LIGHT.isDefault() || SHADER_PACK_DYNAMIC_HAND_LIGHT.isTrue();
    }

    public static boolean isOldLighting() {
        return !CONFIG_OLD_LIGHTING.isDefault() ? CONFIG_OLD_LIGHTING.isTrue() : (SHADER_PACK_OLD_LIGHTING.isDefault() || SHADER_PACK_OLD_LIGHTING.isTrue());
    }

    public static boolean isRenderShadowTranslucent() {
        return !SHADER_PACK_SHADOW_TRANSLUCENT.isFalse();
    }

    public static boolean isUnderwaterOverlay() {
        return !SHADER_PACK_UNDERWATER_OVERLAY.isFalse();
    }

    public static boolean isSun() {
        return !SHADER_PACK_SUN.isFalse();
    }

    public static boolean isMoon() {
        return !SHADER_PACK_MOON.isFalse();
    }

    public static boolean isVignette() {
        return !SHADER_PACK_VIGNETTE.isFalse();
    }

    public static boolean isRenderBackFace(RenderLayer blockLayerIn) {
        return switch (blockLayerIn) {
            case SOLID -> SHADER_PACK_BACK_FACE_SOLID.isTrue();
            case CUTOUT -> SHADER_PACK_BACK_FACE_CUTOUT.isTrue();
            case CUTOUT_MIPPED -> SHADER_PACK_BACK_FACE_CUTOUT_MIPPED.isTrue();
            case TRANSLUCENT -> SHADER_PACK_BACK_FACE_TRANSLUCENT.isTrue();
        };
    }

    public static boolean isRainDepth() {
        return SHADER_PACK_RAIN_DEPTH.isTrue();
    }

    public static boolean isBeaconBeamDepth() {
        return SHADER_PACK_BEACON_BEAM_DEPTH.isTrue();
    }

    public static boolean isSeparateAo() {
        return SHADER_PACK_SEPARATE_AO.isTrue();
    }

    public static boolean isFrustumCulling() {
        return !SHADER_PACK_FRUSTUM_CULLING.isFalse();
    }

    public static void init() {
        boolean flag;

        if (!isInitializedOnce) {
            isInitializedOnce = true;
            flag = true;
        } else {
            flag = false;
        }

        if (!isShaderPackInitialized) {
            checkGLError("Shaders.init pre");

            if (getShaderPackName() != null) {
            }

            if (!capabilities.OpenGL20) {
                printChatAndLogError("No OpenGL 2.0");
            }

            if (!capabilities.GL_EXT_framebuffer_object) {
                printChatAndLogError("No EXT_framebuffer_object");
            }

            DFB_DRAW_BUFFERS.position(0).limit(8);
            DFB_COLOR_TEXTURES.position(0).limit(16);
            DFB_DEPTH_TEXTURES.position(0).limit(3);
            SFB_DRAW_BUFFERS.position(0).limit(8);
            SFB_DEPTH_TEXTURES.position(0).limit(2);
            SFB_COLOR_TEXTURES.position(0).limit(8);
            usedColorBuffers = 4;
            usedDepthBuffers = 1;
            usedShadowColorBuffers = 0;
            usedShadowDepthBuffers = 0;
            usedColorAttachs = 1;
            usedDrawBuffers = 1;
            Arrays.fill(GBUFFERS_FORMAT, 6408);
            Arrays.fill(GBUFFERS_CLEAR, true);
            Arrays.fill(GBUFFERS_CLEAR_COLOR, null);
            Arrays.fill(SHADOW_HARDWARE_FILTERING_ENABLED, false);
            Arrays.fill(SHADOW_MIPMAP_ENABLED, false);
            Arrays.fill(SHADOW_FILTER_NEAREST, false);
            Arrays.fill(SHADOW_COLOR_MIPMAP_ENABLED, false);
            Arrays.fill(SHADOW_COLOR_FILTER_NEAREST, false);
            centerDepthSmoothEnabled = false;
            noiseTextureEnabled = false;
            sunPathRotation = 0.0F;
            shadowIntervalSize = 2.0F;
            shadowMapWidth = 1024;
            shadowMapHeight = 1024;
            spShadowMapWidth = 1024;
            spShadowMapHeight = 1024;
            shadowMapFOV = 90.0F;
            shadowMapHalfPlane = 160.0F;
            shadowMapIsOrtho = true;
            shadowDistanceRenderMul = -1.0F;
            aoLevel = -1.0F;
            useEntityAttrib = false;
            useMidTexCoordAttrib = false;
            useTangentAttrib = false;
            waterShadowEnabled = false;
            hasGeometryShaders = false;
            updateBlockLightLevel();
            Smoother.resetValues();
            SHADER_UNIFORMS.reset();

            if (customUniforms != null) {
                customUniforms.reset();
            }

            ShaderProfile shaderprofile = ShaderUtils.detectProfile(shaderPackProfiles, shaderPackOptions, false);
            String s = "";

            if (currentWorld != null) {
                int i = currentWorld.provider.getDimensionId();

                if (SHADER_PACK_DIMENSIONS.contains(i)) {
                    s = "world" + i + "/";
                }
            }

            for (Program program : PROGRAMS_ALL) {
                program.resetId();
                program.resetConfiguration();

                if (program.getProgramStage() != ProgramStage.NONE) {
                    String s1 = program.getName();
                    String s2 = s + s1;
                    boolean flag1 = true;

                    if (shaderPackProgramConditions.containsKey(s2)) {
                        flag1 = flag1 && shaderPackProgramConditions.get(s2).eval();
                    }

                    if (shaderprofile != null) {
                        flag1 = flag1 && !shaderprofile.isProgramDisabled(s2);
                    }

                    if (!flag1) {
                        Log.info("Program disabled: " + s2);
                        s1 = "<disabled>";
                        s2 = s + s1;
                    }

                    String s3 = "/shaders/" + s2;
                    String s4 = s3 + ".vsh";
                    String s5 = s3 + ".gsh";
                    String s6 = s3 + ".fsh";
                    setupProgram(program, s4, s5, s6);
                    int j = program.getId();

                    if (j > 0) {
                        Log.info("Program loaded: " + s2);
                    }

                    initDrawBuffers(program);
                    updateToggleBuffers(program);
                }
            }

            hasDeferredPrograms = false;

            for (Program program : PROGRAMS_DEFERRED) {
                if (program.getId() != 0) {
                    hasDeferredPrograms = true;
                    break;
                }
            }

            usedColorAttachs = usedColorBuffers;
            shadowPassInterval = usedShadowDepthBuffers > 0 ? 1 : 0;
            shouldSkipDefaultShadow = usedShadowDepthBuffers > 0;
            Log.info("usedColorBuffers: " + usedColorBuffers);
            Log.info("usedDepthBuffers: " + usedDepthBuffers);
            Log.info("usedShadowColorBuffers: " + usedShadowColorBuffers);
            Log.info("usedShadowDepthBuffers: " + usedShadowDepthBuffers);
            Log.info("usedColorAttachs: " + usedColorAttachs);
            Log.info("usedDrawBuffers: " + usedDrawBuffers);
            DFB_DRAW_BUFFERS.position(0).limit(usedDrawBuffers);
            DFB_COLOR_TEXTURES.position(0).limit(usedColorBuffers * 2);
            DFB_COLOR_TEXTURES_FLIP.reset();

            for (int i1 = 0; i1 < usedDrawBuffers; ++i1) {
                DFB_DRAW_BUFFERS.put(i1, 36064 + i1);
            }

            int j1 = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);

            if (usedDrawBuffers > j1) {
                printChatAndLogError("[Shaders] Error: Not enough draw buffers, needed: " + usedDrawBuffers + ", available: " + j1);
            }

            SFB_DRAW_BUFFERS.position(0).limit(usedShadowColorBuffers);

            for (int k1 = 0; k1 < usedShadowColorBuffers; ++k1) {
                SFB_DRAW_BUFFERS.put(k1, 36064 + k1);
            }

            for (Program program1 : PROGRAMS_ALL) {
                Program program2;

                for (program2 = program1; program2.getId() == 0 && program2.getProgramBackup() != program2; program2 = program2.getProgramBackup()) {
                }

                if (program2 != program1 && program1 != PROGRAM_SHADOW) {
                    program1.copyFrom(program2);
                }
            }

            resize();
            resizeShadow();

            if (noiseTextureEnabled) {
                setupNoiseTexture();
            }

            if (defaultTexture == null) {
                defaultTexture = ShadersTex.createDefaultTexture();
            }

            GlStateManager.pushMatrix();
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            preCelestialRotate();
            postCelestialRotate();
            GlStateManager.popMatrix();
            isShaderPackInitialized = true;
            loadEntityDataMap();
            resetDisplayLists();

            if (!flag) {
            }

            checkGLError("Shaders.init");
        }
    }

    private static void initDrawBuffers(Program p) {
        int i = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);
        Arrays.fill(p.getToggleColorTextures(), false);

        if (p == PROGRAM_FINAL) {
            p.setDrawBuffers(null);
        } else if (p.getId() == 0) {
            if (p == PROGRAM_SHADOW) {
                p.setDrawBuffers(DRAW_BUFFERS_NONE);
            } else {
                p.setDrawBuffers(DRAW_BUFFERS_COLOR_ATT_0);
            }
        } else {
            String s = p.getDrawBufSettings();

            if (s == null) {
                if (p != PROGRAM_SHADOW && p != PROGRAM_SHADOW_SOLID && p != PROGRAM_SHADOW_CUTOUT) {
                    p.setDrawBuffers(DFB_DRAW_BUFFERS);
                    usedDrawBuffers = usedColorBuffers;
                    Arrays.fill(p.getToggleColorTextures(), 0, usedColorBuffers, true);
                } else {
                    p.setDrawBuffers(SFB_DRAW_BUFFERS);
                }
            } else {
                IntBuffer intbuffer = p.getDrawBuffersBuffer();
                int j = s.length();
                usedDrawBuffers = Math.max(usedDrawBuffers, j);
                j = Math.min(j, i);
                p.setDrawBuffers(intbuffer);
                intbuffer.limit(j);

                for (int k = 0; k < j; ++k) {
                    int l = getDrawBuffer(p, s, k);
                    intbuffer.put(k, l);
                }
            }
        }
    }

    private static int getDrawBuffer(Program p, String str, int ic) {
        int i = 0;

        if (ic < str.length()) {
            int j = str.charAt(ic) - 48;

            if (p == PROGRAM_SHADOW) {
                if (j >= 0 && j <= 1) {
                    i = j + 36064;
                    usedShadowColorBuffers = Math.max(usedShadowColorBuffers, j);
                }

            } else {
                if (j >= 0 && j <= 7) {
                    p.getToggleColorTextures()[j] = true;
                    i = j + 36064;
                    usedColorAttachs = Math.max(usedColorAttachs, j);
                    usedColorBuffers = Math.max(usedColorBuffers, j);
                }

            }
        }
        return i;
    }

    private static void updateToggleBuffers(Program p) {
        boolean[] aboolean = p.getToggleColorTextures();
        Boolean[] aboolean1 = p.getBuffersFlip();

        for (int i = 0; i < aboolean1.length; ++i) {
            Boolean obool = aboolean1[i];

            if (obool != null) {
                aboolean[i] = obool;
            }
        }
    }

    public static void resetDisplayLists() {
        Log.info("Reset model renderers");
        ++countResetDisplayLists;
        Log.info("Reset world renderers");
        mc.renderGlobal.loadRenderers();
    }

    private static void setupProgram(Program program, String vShaderPath, String gShaderPath, String fShaderPath) {
        checkGLError("pre setupProgram");
        int i = ARBShaderObjects.glCreateProgramObjectARB();
        checkGLError("create");

        if (i != 0) {
            progUseEntityAttrib = false;
            progUseMidTexCoordAttrib = false;
            progUseTangentAttrib = false;
            int j = createVertShader(program, vShaderPath);
            int k = createGeomShader(program, gShaderPath);
            int l = createFragShader(program, fShaderPath);
            checkGLError("create");

            if (j == 0 && k == 0 && l == 0) {
                ARBShaderObjects.glDeleteObjectARB(i);
                program.resetId();
            } else {
                if (j != 0) {
                    ARBShaderObjects.glAttachObjectARB(i, j);
                    checkGLError("attach");
                }

                if (k != 0) {
                    ARBShaderObjects.glAttachObjectARB(i, k);
                    checkGLError("attach");

                    if (progArbGeometryShader4) {
                        ARBGeometryShader4.glProgramParameteriARB(i, 36315, 4);
                        ARBGeometryShader4.glProgramParameteriARB(i, 36316, 5);
                        ARBGeometryShader4.glProgramParameteriARB(i, 36314, progMaxVerticesOut);
                        checkGLError("arbGeometryShader4");
                    }

                    hasGeometryShaders = true;
                }

                if (l != 0) {
                    ARBShaderObjects.glAttachObjectARB(i, l);
                    checkGLError("attach");
                }

                if (progUseEntityAttrib) {
                    ARBVertexShader.glBindAttribLocationARB(i, ENTITY_ATTRIB, "mc_Entity");
                    checkGLError("mc_Entity");
                }

                if (progUseMidTexCoordAttrib) {
                    ARBVertexShader.glBindAttribLocationARB(i, MID_TEX_COORD_ATTRIB, "mc_midTexCoord");
                    checkGLError("mc_midTexCoord");
                }

                if (progUseTangentAttrib) {
                    ARBVertexShader.glBindAttribLocationARB(i, TANGENT_ATTRIB, "at_tangent");
                    checkGLError("at_tangent");
                }

                ARBShaderObjects.glLinkProgramARB(i);

                if (GL20.glGetProgrami(i, 35714) != 1) {
                    Log.error("Error linking program: " + i + " (" + program.getName() + ")");
                }

                printLogInfo(i, program.getName());

                if (j != 0) {
                    ARBShaderObjects.glDetachObjectARB(i, j);
                    ARBShaderObjects.glDeleteObjectARB(j);
                }

                if (k != 0) {
                    ARBShaderObjects.glDetachObjectARB(i, k);
                    ARBShaderObjects.glDeleteObjectARB(k);
                }

                if (l != 0) {
                    ARBShaderObjects.glDetachObjectARB(i, l);
                    ARBShaderObjects.glDeleteObjectARB(l);
                }

                program.setId(i);
                program.setRef(i);
                useProgram(program);
                ARBShaderObjects.glValidateProgramARB(i);
                useProgram(PROGRAM_NONE);
                printLogInfo(i, program.getName());
                int i1 = GL20.glGetProgrami(i, 35715);

                if (i1 != 1) {
                    String s = "\"";
                    printChatAndLogError("[Shaders] Error: Invalid program " + s + program.getName() + s);
                    ARBShaderObjects.glDeleteObjectARB(i);
                    program.resetId();
                }
            }
        }
    }

    private static int createVertShader(Program program, String filename) {
        int i = ARBShaderObjects.glCreateShaderObjectARB(ARBVertexShader.GL_VERTEX_SHADER_ARB);

        if (i == 0) {
            return 0;
        } else {
            StringBuilder stringbuilder = new StringBuilder(131072);
            BufferedReader bufferedreader;

            try {
                bufferedreader = new BufferedReader(getShaderReader(filename));
            } catch (Exception exception) {
                ARBShaderObjects.glDeleteObjectARB(i);
                return 0;
            }

            ShaderOption[] ashaderoption = getChangedOptions(shaderPackOptions);
            List<String> list = new ArrayList<>();

            if (bufferedreader != null) {
                try {
                    bufferedreader = ShaderPackParser.resolveIncludes(bufferedreader, filename, shaderPack, 0, list, 0);
                    MacroState macrostate = new MacroState();

                    while (true) {
                        String s = bufferedreader.readLine();

                        if (s == null) {
                            bufferedreader.close();
                            break;
                        }

                        s = applyOptions(s, ashaderoption);
                        stringbuilder.append(s).append('\n');

                        if (macrostate.processLine(s)) {
                            ShaderLine shaderline = ShaderParser.parseLine(s);

                            if (shaderline != null) {
                                if (shaderline.isAttribute("mc_Entity")) {
                                    useEntityAttrib = true;
                                    progUseEntityAttrib = true;
                                } else if (shaderline.isAttribute("mc_midTexCoord")) {
                                    useMidTexCoordAttrib = true;
                                    progUseMidTexCoordAttrib = true;
                                } else if (shaderline.isAttribute("at_tangent")) {
                                    useTangentAttrib = true;
                                    progUseTangentAttrib = true;
                                }

                                if (shaderline.isConstInt("countInstances")) {
                                    program.setCountInstances(shaderline.getValueInt());
                                    Log.info("countInstances: " + program.getCountInstances());
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    Log.error("Couldn't read " + filename + "!");
                    exception.printStackTrace();
                    ARBShaderObjects.glDeleteObjectARB(i);
                    return 0;
                }
            }

            if (SAVE_FINAL_SHADERS) {
                saveShader(filename, stringbuilder.toString());
            }

            ARBShaderObjects.glShaderSourceARB(i, stringbuilder);
            ARBShaderObjects.glCompileShaderARB(i);

            if (GL20.glGetShaderi(i, 35713) != 1) {
                Log.error("Error compiling vertex shader: " + filename);
            }

            printShaderLogInfo(i, filename, list);
            return i;
        }
    }

    private static int createGeomShader(Program program, String filename) {
        int i = ARBShaderObjects.glCreateShaderObjectARB(36313);

        if (i == 0) {
            return 0;
        } else {
            StringBuilder stringbuilder = new StringBuilder(131072);
            BufferedReader bufferedreader;

            try {
                bufferedreader = new BufferedReader(getShaderReader(filename));
            } catch (Exception exception) {
                ARBShaderObjects.glDeleteObjectARB(i);
                return 0;
            }

            ShaderOption[] ashaderoption = getChangedOptions(shaderPackOptions);
            List<String> list = new ArrayList<>();
            progArbGeometryShader4 = false;
            progMaxVerticesOut = 3;

            if (bufferedreader != null) {
                try {
                    bufferedreader = ShaderPackParser.resolveIncludes(bufferedreader, filename, shaderPack, 0, list, 0);
                    MacroState macrostate = new MacroState();

                    while (true) {
                        String s = bufferedreader.readLine();

                        if (s == null) {
                            bufferedreader.close();
                            break;
                        }

                        s = applyOptions(s, ashaderoption);
                        stringbuilder.append(s).append('\n');

                        if (macrostate.processLine(s)) {
                            ShaderLine shaderline = ShaderParser.parseLine(s);

                            if (shaderline != null) {
                                if (shaderline.isExtension("GL_ARB_geometry_shader4")) {
                                    String s1 = Config.normalize(shaderline.getValue());

                                    if (s1.equals("enable") || s1.equals("require") || s1.equals("warn")) {
                                        progArbGeometryShader4 = true;
                                    }
                                }

                                if (shaderline.isConstInt("maxVerticesOut")) {
                                    progMaxVerticesOut = shaderline.getValueInt();
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    Log.error("Couldn't read " + filename + "!");
                    exception.printStackTrace();
                    ARBShaderObjects.glDeleteObjectARB(i);
                    return 0;
                }
            }

            if (SAVE_FINAL_SHADERS) {
                saveShader(filename, stringbuilder.toString());
            }

            ARBShaderObjects.glShaderSourceARB(i, stringbuilder);
            ARBShaderObjects.glCompileShaderARB(i);

            if (GL20.glGetShaderi(i, 35713) != 1) {
                Log.error("Error compiling geometry shader: " + filename);
            }

            printShaderLogInfo(i, filename, list);
            return i;
        }
    }

    private static int createFragShader(Program program, String filename) {
        int i = ARBShaderObjects.glCreateShaderObjectARB(ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

        if (i == 0) {
            return 0;
        } else {
            StringBuilder stringbuilder = new StringBuilder(131072);
            BufferedReader bufferedreader;

            try {
                bufferedreader = new BufferedReader(getShaderReader(filename));
            } catch (Exception exception) {
                ARBShaderObjects.glDeleteObjectARB(i);
                return 0;
            }

            ShaderOption[] ashaderoption = getChangedOptions(shaderPackOptions);
            List<String> list = new ArrayList<>();

            if (bufferedreader != null) {
                try {
                    bufferedreader = ShaderPackParser.resolveIncludes(bufferedreader, filename, shaderPack, 0, list, 0);
                    MacroState macrostate = new MacroState();

                    while (true) {
                        String s = bufferedreader.readLine();

                        if (s == null) {
                            bufferedreader.close();
                            break;
                        }

                        s = applyOptions(s, ashaderoption);
                        stringbuilder.append(s).append('\n');

                        if (macrostate.processLine(s)) {
                            ShaderLine shaderline = ShaderParser.parseLine(s);

                            if (shaderline != null) {
                                if (shaderline.isUniform()) {
                                    String s6 = shaderline.getName();
                                    int l1;

                                    if ((l1 = ShaderParser.getShadowDepthIndex(s6)) >= 0) {
                                        usedShadowDepthBuffers = Math.max(usedShadowDepthBuffers, l1 + 1);
                                    } else if ((l1 = ShaderParser.getShadowColorIndex(s6)) >= 0) {
                                        usedShadowColorBuffers = Math.max(usedShadowColorBuffers, l1 + 1);
                                    } else if ((l1 = ShaderParser.getDepthIndex(s6)) >= 0) {
                                        usedDepthBuffers = Math.max(usedDepthBuffers, l1 + 1);
                                    } else if (s6.equals("gdepth") && GBUFFERS_FORMAT[1] == 6408) {
                                        GBUFFERS_FORMAT[1] = 34836;
                                    } else if ((l1 = ShaderParser.getColorIndex(s6)) >= 0) {
                                        usedColorBuffers = Math.max(usedColorBuffers, l1 + 1);
                                    } else if (s6.equals("centerDepthSmooth")) {
                                        centerDepthSmoothEnabled = true;
                                    }
                                } else if (!shaderline.isConstInt("shadowMapResolution") && !shaderline.isProperty("SHADOWRES")) {
                                    if (!shaderline.isConstFloat("shadowMapFov") && !shaderline.isProperty("SHADOWFOV")) {
                                        if (!shaderline.isConstFloat("shadowDistance") && !shaderline.isProperty("SHADOWHPL")) {
                                            if (shaderline.isConstFloat("shadowDistanceRenderMul")) {
                                                shadowDistanceRenderMul = shaderline.getValueFloat();
                                                Log.info("Shadow distance render mul: " + shadowDistanceRenderMul);
                                            } else if (shaderline.isConstFloat("shadowIntervalSize")) {
                                                shadowIntervalSize = shaderline.getValueFloat();
                                                Log.info("Shadow map interval size: " + shadowIntervalSize);
                                            } else if (shaderline.isConstBool("generateShadowMipmap", true)) {
                                                Arrays.fill(SHADOW_MIPMAP_ENABLED, true);
                                                Log.info("Generate shadow mipmap");
                                            } else if (shaderline.isConstBool("generateShadowColorMipmap", true)) {
                                                Arrays.fill(SHADOW_COLOR_MIPMAP_ENABLED, true);
                                                Log.info("Generate shadow color mipmap");
                                            } else if (shaderline.isConstBool("shadowHardwareFiltering", true)) {
                                                Arrays.fill(SHADOW_HARDWARE_FILTERING_ENABLED, true);
                                                Log.info("Hardware shadow filtering enabled.");
                                            } else if (shaderline.isConstBool("shadowHardwareFiltering0", true)) {
                                                SHADOW_HARDWARE_FILTERING_ENABLED[0] = true;
                                                Log.info("shadowHardwareFiltering0");
                                            } else if (shaderline.isConstBool("shadowHardwareFiltering1", true)) {
                                                SHADOW_HARDWARE_FILTERING_ENABLED[1] = true;
                                                Log.info("shadowHardwareFiltering1");
                                            } else if (shaderline.isConstBool("shadowtex0Mipmap", "shadowtexMipmap", true)) {
                                                SHADOW_MIPMAP_ENABLED[0] = true;
                                                Log.info("shadowtex0Mipmap");
                                            } else if (shaderline.isConstBool("shadowtex1Mipmap", true)) {
                                                SHADOW_MIPMAP_ENABLED[1] = true;
                                                Log.info("shadowtex1Mipmap");
                                            } else if (shaderline.isConstBool("shadowcolor0Mipmap", "shadowColor0Mipmap", true)) {
                                                SHADOW_COLOR_MIPMAP_ENABLED[0] = true;
                                                Log.info("shadowcolor0Mipmap");
                                            } else if (shaderline.isConstBool("shadowcolor1Mipmap", "shadowColor1Mipmap", true)) {
                                                SHADOW_COLOR_MIPMAP_ENABLED[1] = true;
                                                Log.info("shadowcolor1Mipmap");
                                            } else if (shaderline.isConstBool("shadowtex0Nearest", "shadowtexNearest", "shadow0MinMagNearest", true)) {
                                                SHADOW_FILTER_NEAREST[0] = true;
                                                Log.info("shadowtex0Nearest");
                                            } else if (shaderline.isConstBool("shadowtex1Nearest", "shadow1MinMagNearest", true)) {
                                                SHADOW_FILTER_NEAREST[1] = true;
                                                Log.info("shadowtex1Nearest");
                                            } else if (shaderline.isConstBool("shadowcolor0Nearest", "shadowColor0Nearest", "shadowColor0MinMagNearest", true)) {
                                                SHADOW_COLOR_FILTER_NEAREST[0] = true;
                                                Log.info("shadowcolor0Nearest");
                                            } else if (shaderline.isConstBool("shadowcolor1Nearest", "shadowColor1Nearest", "shadowColor1MinMagNearest", true)) {
                                                SHADOW_COLOR_FILTER_NEAREST[1] = true;
                                                Log.info("shadowcolor1Nearest");
                                            } else if (!shaderline.isConstFloat("wetnessHalflife") && !shaderline.isProperty("WETNESSHL")) {
                                                if (!shaderline.isConstFloat("drynessHalflife") && !shaderline.isProperty("DRYNESSHL")) {
                                                    if (shaderline.isConstFloat("eyeBrightnessHalflife")) {
                                                        eyeBrightnessHalflife = shaderline.getValueFloat();
                                                        Log.info("Eye brightness halflife: " + eyeBrightnessHalflife);
                                                    } else if (shaderline.isConstFloat("centerDepthHalflife")) {
                                                        centerDepthSmoothHalflife = shaderline.getValueFloat();
                                                        Log.info("Center depth halflife: " + centerDepthSmoothHalflife);
                                                    } else if (shaderline.isConstFloat("sunPathRotation")) {
                                                        sunPathRotation = shaderline.getValueFloat();
                                                        Log.info("Sun path rotation: " + sunPathRotation);
                                                    } else if (shaderline.isConstFloat("ambientOcclusionLevel")) {
                                                        aoLevel = Config.limit(shaderline.getValueFloat(), 0.0F, 1.0F);
                                                        Log.info("AO Level: " + aoLevel);
                                                    } else if (shaderline.isConstInt("superSamplingLevel")) {
                                                        int i1 = shaderline.getValueInt();

                                                        if (i1 > 1) {
                                                            Log.info("Super sampling level: " + i1 + "x");
                                                            superSamplingLevel = i1;
                                                        } else {
                                                            superSamplingLevel = 1;
                                                        }
                                                    } else if (shaderline.isConstInt("noiseTextureResolution")) {
                                                        noiseTextureResolution = shaderline.getValueInt();
                                                        noiseTextureEnabled = true;
                                                        Log.info("Noise texture enabled");
                                                        Log.info("Noise texture resolution: " + noiseTextureResolution);
                                                    } else if (shaderline.isConstIntSuffix("Format")) {
                                                        String s5 = StrUtils.removeSuffix(shaderline.getName(), "Format");
                                                        String s7 = shaderline.getValue();
                                                        int i2 = getBufferIndexFromString(s5);
                                                        int l = getTextureFormatFromString(s7);

                                                        if (i2 >= 0 && l != 0) {
                                                            GBUFFERS_FORMAT[i2] = l;
                                                            Log.info("%s format: %s", s5, s7);
                                                        }
                                                    } else if (shaderline.isConstBoolSuffix("Clear", false)) {
                                                        if (ShaderParser.isComposite(filename) || ShaderParser.isDeferred(filename)) {
                                                            String s4 = StrUtils.removeSuffix(shaderline.getName(), "Clear");
                                                            int k1 = getBufferIndexFromString(s4);

                                                            if (k1 >= 0) {
                                                                GBUFFERS_CLEAR[k1] = false;
                                                                Log.info("%s clear disabled", s4);
                                                            }
                                                        }
                                                    } else if (shaderline.isConstVec4Suffix("ClearColor")) {
                                                        if (ShaderParser.isComposite(filename) || ShaderParser.isDeferred(filename)) {
                                                            String s3 = StrUtils.removeSuffix(shaderline.getName(), "ClearColor");
                                                            int j1 = getBufferIndexFromString(s3);

                                                            if (j1 >= 0) {
                                                                Vector4f vector4f = shaderline.getValueVec4();

                                                                if (vector4f != null) {
                                                                    GBUFFERS_CLEAR_COLOR[j1] = vector4f;
                                                                    Log.info("%s clear color: %s %s %s %s", s3, vector4f.x, vector4f.y, vector4f.z, vector4f.w);
                                                                } else {
                                                                    Log.warn("Invalid color value: " + shaderline.getValue());
                                                                }
                                                            }
                                                        }
                                                    } else if (shaderline.isProperty("GAUX4FORMAT", "RGBA32F")) {
                                                        GBUFFERS_FORMAT[7] = 34836;
                                                        Log.info("gaux4 format : RGB32AF");
                                                    } else if (shaderline.isProperty("GAUX4FORMAT", "RGB32F")) {
                                                        GBUFFERS_FORMAT[7] = 34837;
                                                        Log.info("gaux4 format : RGB32F");
                                                    } else if (shaderline.isProperty("GAUX4FORMAT", "RGB16")) {
                                                        GBUFFERS_FORMAT[7] = 32852;
                                                        Log.info("gaux4 format : RGB16");
                                                    } else if (shaderline.isConstBoolSuffix("MipmapEnabled", true)) {
                                                        if (ShaderParser.isComposite(filename) || ShaderParser.isDeferred(filename) || ShaderParser.isFinal(filename)) {
                                                            String s2 = StrUtils.removeSuffix(shaderline.getName(), "MipmapEnabled");
                                                            int j = getBufferIndexFromString(s2);

                                                            if (j >= 0) {
                                                                int k = program.getCompositeMipmapSetting();
                                                                k = k | 1 << j;
                                                                program.setCompositeMipmapSetting(k);
                                                                Log.info("%s mipmap enabled", s2);
                                                            }
                                                        }
                                                    } else if (shaderline.isProperty("DRAWBUFFERS")) {
                                                        String s1 = shaderline.getValue();

                                                        if (ShaderParser.isValidDrawBuffers(s1)) {
                                                            program.setDrawBufSettings(s1);
                                                        } else {
                                                            Log.warn("Invalid draw buffers: " + s1);
                                                        }
                                                    }
                                                } else {
                                                    drynessHalfLife = shaderline.getValueFloat();
                                                    Log.info("Dryness halflife: " + drynessHalfLife);
                                                }
                                            } else {
                                                wetnessHalfLife = shaderline.getValueFloat();
                                                Log.info("Wetness halflife: " + wetnessHalfLife);
                                            }
                                        } else {
                                            shadowMapHalfPlane = shaderline.getValueFloat();
                                            shadowMapIsOrtho = true;
                                            Log.info("Shadow map distance: " + shadowMapHalfPlane);
                                        }
                                    } else {
                                        shadowMapFOV = shaderline.getValueFloat();
                                        shadowMapIsOrtho = false;
                                        Log.info("Shadow map field of view: " + shadowMapFOV);
                                    }
                                } else {
                                    spShadowMapWidth = spShadowMapHeight = shaderline.getValueInt();
                                    shadowMapWidth = shadowMapHeight = Math.round(spShadowMapWidth * configShadowResMul);
                                    Log.info("Shadow map resolution: " + spShadowMapWidth);
                                }
                            }
                        }
                    }
                } catch (Exception exception) {
                    Log.error("Couldn't read " + filename + "!");
                    exception.printStackTrace();
                    ARBShaderObjects.glDeleteObjectARB(i);
                    return 0;
                }
            }

            if (SAVE_FINAL_SHADERS) {
                saveShader(filename, stringbuilder.toString());
            }

            ARBShaderObjects.glShaderSourceARB(i, stringbuilder);
            ARBShaderObjects.glCompileShaderARB(i);

            if (GL20.glGetShaderi(i, 35713) != 1) {
                Log.error("Error compiling fragment shader: " + filename);
            }

            printShaderLogInfo(i, filename, list);
            return i;
        }
    }

    private static Reader getShaderReader(String filename) {
        return new InputStreamReader(shaderPack.getResourceAsStream(filename));
    }

    public static void saveShader(String filename, String code) {
        try {
            File file1 = new File(SHADER_PACKS_DIR, "debug/" + filename);
            file1.getParentFile().mkdirs();
            Config.writeFile(file1, code);
        } catch (IOException exception) {
            Log.error("Error saving: " + filename);
            exception.printStackTrace();
        }
    }

    private static void clearDirectory(File dir) {
        if (dir.exists()) {
            if (dir.isDirectory()) {
                File[] afile = dir.listFiles();

                if (afile != null) {
                    for (File file1 : afile) {
                        if (file1.isDirectory()) {
                            clearDirectory(file1);
                        }

                        file1.delete();
                    }
                }
            }
        }
    }

    private static boolean printLogInfo(int obj, String name) {
        IntBuffer intbuffer = BufferUtils.createIntBuffer(1);
        ARBShaderObjects.glGetObjectParameterivARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB, intbuffer);
        int i = intbuffer.get();

        if (i > 1) {
            ByteBuffer bytebuffer = BufferUtils.createByteBuffer(i);
            intbuffer.flip();
            ARBShaderObjects.glGetInfoLogARB(obj, intbuffer, bytebuffer);
            byte[] abyte = new byte[i];
            bytebuffer.get(abyte);

            if (abyte[i - 1] == 0) {
                abyte[i - 1] = 10;
            }

            String s = new String(abyte, StandardCharsets.US_ASCII);
            s = StrUtils.trim(s, " \n\r\t");
            Log.info("Info log: " + name + "\n" + s);
            return false;
        } else {
            return true;
        }
    }

    private static boolean printShaderLogInfo(int shader, String name, List<String> listFiles) {
        IntBuffer intbuffer = BufferUtils.createIntBuffer(1);
        int i = GL20.glGetShaderi(shader, 35716);

        if (i <= 1) {
            return true;
        } else {
            for (int j = 0; j < listFiles.size(); ++j) {
                String s = listFiles.get(j);
                Log.info("File: " + (j + 1) + " = " + s);
            }

            String s1 = GL20.glGetShaderInfoLog(shader, i);
            s1 = StrUtils.trim(s1, " \n\r\t");
            Log.info("Shader info log: " + name + "\n" + s1);
            return false;
        }
    }

    public static void setDrawBuffers(IntBuffer drawBuffers) {
        if (drawBuffers == null) {
            drawBuffers = DRAW_BUFFERS_NONE;
        }

        if (activeDrawBuffers != drawBuffers) {
            activeDrawBuffers = drawBuffers;
            GL20.glDrawBuffers(drawBuffers);
            checkGLError("setDrawBuffers");
        }
    }

    public static void useProgram(Program program) {
        checkGLError("pre-useProgram");

        if (isShadowPass) {
            program = PROGRAM_SHADOW;
        } else if (isEntitiesGlowing) {
            program = PROGRAM_ENTITIES_GLOWING;
        }

        if (activeProgram != program) {
            updateAlphaBlend(activeProgram, program);
            activeProgram = program;
            int i = program.getId();
            activeProgramID = i;
            ARBShaderObjects.glUseProgramObjectARB(i);

            if (checkGLError("useProgram") != 0) {
                program.setId(0);
                i = program.getId();
                activeProgramID = i;
                ARBShaderObjects.glUseProgramObjectARB(i);
            }

            SHADER_UNIFORMS.setProgram(i);

            if (customUniforms != null) {
                customUniforms.setProgram(i);
            }

            if (i != 0) {
                IntBuffer intbuffer = program.getDrawBuffers();

                if (isRenderingDfb) {
                    setDrawBuffers(intbuffer);
                }

                activeCompositeMipmapSetting = program.getCompositeMipmapSetting();

                switch (program.getProgramStage()) {
                    case GBUFFERS:
                        setProgramUniform1i(UNIFORM_TEXTURE, 0);
                        setProgramUniform1i(UNIFORM_LIGHTMAP, 1);
                        setProgramUniform1i(UNIFORM_NORMALS, 2);
                        setProgramUniform1i(UNIFORM_SPECULAR, 3);
                        setProgramUniform1i(UNIFORM_SHADOW, waterShadowEnabled ? 5 : 4);
                        setProgramUniform1i(UNIFORM_WATERSHADOW, 4);
                        setProgramUniform1i(UNIFORM_SHADOWTEX_0, 4);
                        setProgramUniform1i(UNIFORM_SHADOWTEX_1, 5);
                        setProgramUniform1i(UNIFORM_DEPTHTEX_0, 6);

                        if (customTexturesGbuffers != null || hasDeferredPrograms) {
                            setProgramUniform1i(UNIFORM_GAUX_1, 7);
                            setProgramUniform1i(UNIFORM_GAUX_2, 8);
                            setProgramUniform1i(UNIFORM_GAUX_3, 9);
                            setProgramUniform1i(UNIFORM_GAUX_4, 10);
                        }

                        setProgramUniform1i(UNIFORM_DEPTHTEX_1, 11);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR, 13);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR_0, 13);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR_1, 14);
                        setProgramUniform1i(UNIFORM_NOISETEX, 15);
                        break;

                    case DEFERRED:
                    case COMPOSITE:
                        setProgramUniform1i(UNIFORM_GCOLOR, 0);
                        setProgramUniform1i(UNIFORM_GDEPTH, 1);
                        setProgramUniform1i(UNIFORM_GNORMAL, 2);
                        setProgramUniform1i(UNIFORM_COMPOSITE, 3);
                        setProgramUniform1i(UNIFORM_GAUX_1, 7);
                        setProgramUniform1i(UNIFORM_GAUX_2, 8);
                        setProgramUniform1i(UNIFORM_GAUX_3, 9);
                        setProgramUniform1i(UNIFORM_GAUX_4, 10);
                        setProgramUniform1i(UNIFORM_COLORTEX_0, 0);
                        setProgramUniform1i(UNIFORM_COLORTEX_1, 1);
                        setProgramUniform1i(UNIFORM_COLORTEX_2, 2);
                        setProgramUniform1i(UNIFORM_COLORTEX_3, 3);
                        setProgramUniform1i(UNIFORM_COLORTEX_4, 7);
                        setProgramUniform1i(UNIFORM_COLORTEX_5, 8);
                        setProgramUniform1i(UNIFORM_COLORTEX_6, 9);
                        setProgramUniform1i(UNIFORM_COLORTEX_7, 10);
                        setProgramUniform1i(UNIFORM_SHADOW, waterShadowEnabled ? 5 : 4);
                        setProgramUniform1i(UNIFORM_WATERSHADOW, 4);
                        setProgramUniform1i(UNIFORM_SHADOWTEX_0, 4);
                        setProgramUniform1i(UNIFORM_SHADOWTEX_1, 5);
                        setProgramUniform1i(UNIFORM_GDEPTHTEX, 6);
                        setProgramUniform1i(UNIFORM_DEPTHTEX_0, 6);
                        setProgramUniform1i(UNIFORM_DEPTHTEX_1, 11);
                        setProgramUniform1i(UNIFORM_DEPTHTEX_2, 12);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR, 13);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR_0, 13);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR_1, 14);
                        setProgramUniform1i(UNIFORM_NOISETEX, 15);
                        break;

                    case SHADOW:
                        setProgramUniform1i(UNIFORM_TEX, 0);
                        setProgramUniform1i(UNIFORM_TEXTURE, 0);
                        setProgramUniform1i(UNIFORM_LIGHTMAP, 1);
                        setProgramUniform1i(UNIFORM_NORMALS, 2);
                        setProgramUniform1i(UNIFORM_SPECULAR, 3);
                        setProgramUniform1i(UNIFORM_SHADOW, waterShadowEnabled ? 5 : 4);
                        setProgramUniform1i(UNIFORM_WATERSHADOW, 4);
                        setProgramUniform1i(UNIFORM_SHADOWTEX_0, 4);
                        setProgramUniform1i(UNIFORM_SHADOWTEX_1, 5);

                        if (customTexturesGbuffers != null) {
                            setProgramUniform1i(UNIFORM_GAUX_1, 7);
                            setProgramUniform1i(UNIFORM_GAUX_2, 8);
                            setProgramUniform1i(UNIFORM_GAUX_3, 9);
                            setProgramUniform1i(UNIFORM_GAUX_4, 10);
                        }

                        setProgramUniform1i(UNIFORM_SHADOWCOLOR, 13);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR_0, 13);
                        setProgramUniform1i(UNIFORM_SHADOWCOLOR_1, 14);
                        setProgramUniform1i(UNIFORM_NOISETEX, 15);
                }

                ItemStack itemstack = mc.player != null ? mc.player.getHeldItem() : null;
                Item item = itemstack != null ? itemstack.getItem() : null;
                int j = -1;
                Block block = null;

                if (item != null) {
                    j = Item.itemRegistry.getIDForObject(item);
                    block = Block.blockRegistry.getObjectById(j);
                    j = ItemAliases.getItemAliasId(j);
                }

                int k = block != null ? block.getLightValue() : 0;
                setProgramUniform1i(UNIFORM_HELD_ITEM_ID, j);
                setProgramUniform1i(UNIFORM_HELD_BLOCK_LIGHT_VALUE, k);
                setProgramUniform1i(UNIFORM_FOG_MODE, fogEnabled ? fogMode : 0);
                setProgramUniform1f(UNIFORM_FOG_DENSITY, fogEnabled ? fogDensity : 0.0F);
                setProgramUniform3f(UNIFORM_FOG_COLOR, fogColorR, fogColorG, fogColorB);
                setProgramUniform3f(UNIFORM_SKY_COLOR, skyColorR, skyColorG, skyColorB);
                setProgramUniform1i(UNIFORM_WORLD_TIME, (int) (worldTime % 24000L));
                setProgramUniform1i(UNIFORM_WORLD_DAY, (int) (worldTime / 24000L));
                setProgramUniform1i(UNIFORM_MOON_PHASE, moonPhase);
                setProgramUniform1i(UNIFORM_FRAME_COUNTER, frameCounter);
                setProgramUniform1f(UNIFORM_FRAME_TIME, frameTime);
                setProgramUniform1f(UNIFORM_FRAME_TIME_COUNTER, frameTimeCounter);
                setProgramUniform1f(UNIFORM_SUN_ANGLE, sunAngle);
                setProgramUniform1f(UNIFORM_SHADOW_ANGLE, shadowAngle);
                setProgramUniform1f(UNIFORM_RAIN_STRENGTH, rainStrength);
                setProgramUniform1f(UNIFORM_ASPECT_RATIO, (float) renderWidth / renderHeight);
                setProgramUniform1f(UNIFORM_VIEW_WIDTH, renderWidth);
                setProgramUniform1f(UNIFORM_VIEW_HEIGHT, renderHeight);
                setProgramUniform1f(UNIFORM_NEAR, 0.05F);
                setProgramUniform1f(UNIFORM_FAR, (mc.gameSettings.renderDistanceChunks * 16));
                setProgramUniform3f(UNIFORM_SUN_POSITION, SUN_POSITION[0], SUN_POSITION[1], SUN_POSITION[2]);
                setProgramUniform3f(UNIFORM_MOON_POSITION, MOON_POSITION[0], MOON_POSITION[1], MOON_POSITION[2]);
                setProgramUniform3f(UNIFORM_SHADOW_LIGHT_POSITION, SHADOW_LIGHT_POSITION[0], SHADOW_LIGHT_POSITION[1], SHADOW_LIGHT_POSITION[2]);
                setProgramUniform3f(UNIFORM_UP_POSITION, UP_POSITION[0], UP_POSITION[1], UP_POSITION[2]);
                setProgramUniform3f(UNIFORM_PREVIOUS_CAMERA_POSITION, (float) previousCameraPositionX, (float) previousCameraPositionY, (float) previousCameraPositionZ);
                setProgramUniform3f(UNIFORM_CAMERA_POSITION, (float) cameraPositionX, (float) cameraPositionY, (float) cameraPositionZ);
                setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_MODEL_VIEW, false, MODEL_VIEW);
                setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_MODEL_VIEW_INVERSE, false, MODEL_VIEW_INVERSE);
                setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PREVIOUS_PROJECTION, false, PREVIOUS_PROJECTION);
                setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PROJECTION, false, PROJECTION);
                setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PROJECTION_INVERSE, false, PROJECTION_INVERSE);
                setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PREVIOUS_MODEL_VIEW, false, PREVIOUS_MODEL_VIEW);

                if (usedShadowDepthBuffers > 0) {
                    setProgramUniformMatrix4ARB(UNIFORM_SHADOW_PROJECTION, false, SHADOW_PROJECTION);
                    setProgramUniformMatrix4ARB(UNIFORM_SHADOW_PROJECTION_INVERSE, false, SHADOW_PROJECTION_INVERSE);
                    setProgramUniformMatrix4ARB(UNIFORM_SHADOW_MODEL_VIEW, false, SHADOW_MODEL_VIEW);
                    setProgramUniformMatrix4ARB(UNIFORM_SHADOW_MODEL_VIEW_INVERSE, false, SHADOW_MODEL_VIEW_INVERSE);
                }

                setProgramUniform1f(UNIFORM_WETNESS, wetness);
                setProgramUniform1f(UNIFORM_EYE_ALTITUDE, eyePosY);
                setProgramUniform2i(UNIFORM_EYE_BRIGHTNESS, eyeBrightness & 65535, eyeBrightness >> 16);
                setProgramUniform2i(UNIFORM_EYE_BRIGHTNESS_SMOOTH, Math.round(eyeBrightnessFadeX), Math.round(eyeBrightnessFadeY));
                setProgramUniform2i(UNIFORM_TERRAIN_TEXTURE_SIZE, TERRAIN_TEXTURE_SIZE[0], TERRAIN_TEXTURE_SIZE[1]);
                setProgramUniform1i(UNIFORM_TERRAIN_ICON_SIZE, TERRAIN_ICON_SIZE);
                setProgramUniform1i(UNIFORM_IS_EYE_IN_WATER, isEyeInWater);
                setProgramUniform1f(UNIFORM_NIGHT_VISION, nightVision);
                setProgramUniform1f(UNIFORM_BLINDNESS, blindness);
                setProgramUniform1f(UNIFORM_SCREEN_BRIGHTNESS, mc.gameSettings.gammaSetting);
                setProgramUniform1i(UNIFORM_HIDE_GUI, mc.gameSettings.hideGUI ? 1 : 0);
                setProgramUniform1f(UNIFORM_CENTER_DEPTH_SMOOTH, centerDepthSmooth);
                setProgramUniform2i(UNIFORM_ATLAS_SIZE, atlasSizeX, atlasSizeY);

                if (customUniforms != null) {
                    customUniforms.update();
                }

                checkGLError("end useProgram");
            }
        }
    }

    private static void updateAlphaBlend(Program programOld, Program programNew) {
        if (programOld.getAlphaState() != null) {
            GlStateManager.unlockAlpha();
        }

        if (programOld.getBlendState() != null) {
            GlStateManager.unlockBlend();
        }

        GlAlphaState glalphastate = programNew.getAlphaState();

        if (glalphastate != null) {
            GlStateManager.lockAlpha(glalphastate);
        }

        GlBlendState glblendstate = programNew.getBlendState();

        if (glblendstate != null) {
            GlStateManager.lockBlend(glblendstate);
        }
    }

    private static void setProgramUniform1i(ShaderUniform1i su, int value) {
        su.setValue(value);
    }

    private static void setProgramUniform2i(ShaderUniform2i su, int i0, int i1) {
        su.setValue(i0, i1);
    }

    private static void setProgramUniform1f(ShaderUniform1f su, float value) {
        su.setValue(value);
    }

    private static void setProgramUniform3f(ShaderUniform3f su, float f0, float f1, float f2) {
        su.setValue(f0, f1, f2);
    }

    private static void setProgramUniformMatrix4ARB(ShaderUniformM4 su, boolean transpose, FloatBuffer matrix) {
        su.setValue(transpose, matrix);
    }

    public static int getBufferIndexFromString(String name) {
        return !name.equals("colortex0") && !name.equals("gcolor") ? (!name.equals("colortex1") && !name.equals("gdepth") ? (!name.equals("colortex2") && !name.equals("gnormal") ? (!name.equals("colortex3") && !name.equals("composite") ? (!name.equals("colortex4") && !name.equals("gaux1") ? (!name.equals("colortex5") && !name.equals("gaux2") ? (!name.equals("colortex6") && !name.equals("gaux3") ? (!name.equals("colortex7") && !name.equals("gaux4") ? -1 : 7) : 6) : 5) : 4) : 3) : 2) : 1) : 0;
    }

    private static int getTextureFormatFromString(String par) {
        par = par.trim();

        for (int i = 0; i < FORMAT_NAMES.length; ++i) {
            String s = FORMAT_NAMES[i];

            if (par.equals(s)) {
                return FORMAT_IDS[i];
            }
        }

        return 0;
    }

    private static void setupNoiseTexture() {
        if (noiseTexture == null && noiseTexturePath != null) {
            noiseTexture = loadCustomTexture(15, noiseTexturePath);
        }

        if (noiseTexture == null) {
            noiseTexture = new HFNoiseTexture(noiseTextureResolution, noiseTextureResolution);
        }
    }

    private static void loadEntityDataMap() {
        mapBlockToEntityData = new IdentityHashMap<>(300);

        if (mapBlockToEntityData.isEmpty()) {
            for (ResourceLocation resourcelocation : Block.blockRegistry.getKeys()) {
                Block block = Block.blockRegistry.getObject(resourcelocation);
                int i = Block.blockRegistry.getIDForObject(block);
                mapBlockToEntityData.put(block, i);
            }
        }

        BufferedReader bufferedreader = null;

        try {
            bufferedreader = new BufferedReader(new InputStreamReader(shaderPack.getResourceAsStream("/mc_Entity_x.txt")));
        } catch (Exception _) {
        }

        if (bufferedreader != null) {
            String s1;

            try {
                while ((s1 = bufferedreader.readLine()) != null) {
                    Matcher matcher = PATTERN_LOAD_ENTITY_DATA_MAP.matcher(s1);

                    if (matcher.matches()) {
                        String s2 = matcher.group(1);
                        String s = matcher.group(2);
                        int j = Integer.parseInt(s);
                        Block block1 = Block.getBlockFromName(s2);

                        if (block1 != null) {
                            mapBlockToEntityData.put(block1, j);
                        } else {
                            Log.warn("Unknown block name %s", s2);
                        }
                    } else {
                        Log.warn("unmatched %s\n", s1);
                    }
                }
            } catch (Exception exception) {
                Log.warn("Error parsing mc_Entity_x.txt");
            }
        }

        if (bufferedreader != null) {
            try {
                bufferedreader.close();
            } catch (Exception _) {
            }
        }
    }

    private static IntBuffer fillIntBufferZero(IntBuffer buf) {
        int i = buf.limit();

        for (int j = buf.position(); j < i; ++j) {
            buf.put(j, 0);
        }

        return buf;
    }

    public static void uninit() {
        if (isShaderPackInitialized) {
            checkGLError("Shaders.uninit pre");

            for (Program program : PROGRAMS_ALL) {
                if (program.getRef() != 0) {
                    ARBShaderObjects.glDeleteObjectARB(program.getRef());
                    checkGLError("del programRef");
                }

                program.setRef(0);
                program.setId(0);
                program.setDrawBufSettings(null);
                program.setDrawBuffers(null);
                program.setCompositeMipmapSetting(0);
            }

            hasDeferredPrograms = false;

            if (dfb != 0) {
                EXTFramebufferObject.glDeleteFramebuffersEXT(dfb);
                dfb = 0;
                checkGLError("del dfb");
            }

            if (sfb != 0) {
                EXTFramebufferObject.glDeleteFramebuffersEXT(sfb);
                sfb = 0;
                checkGLError("del sfb");
            }

            if (DFB_DEPTH_TEXTURES != null) {
                GlStateManager.deleteTextures(DFB_DEPTH_TEXTURES);
                fillIntBufferZero(DFB_DEPTH_TEXTURES);
                checkGLError("del dfbDepthTextures");
            }

            if (DFB_COLOR_TEXTURES != null) {
                GlStateManager.deleteTextures(DFB_COLOR_TEXTURES);
                fillIntBufferZero(DFB_COLOR_TEXTURES);
                checkGLError("del dfbTextures");
            }

            if (SFB_DEPTH_TEXTURES != null) {
                GlStateManager.deleteTextures(SFB_DEPTH_TEXTURES);
                fillIntBufferZero(SFB_DEPTH_TEXTURES);
                checkGLError("del shadow depth");
            }

            if (SFB_COLOR_TEXTURES != null) {
                GlStateManager.deleteTextures(SFB_COLOR_TEXTURES);
                fillIntBufferZero(SFB_COLOR_TEXTURES);
                checkGLError("del shadow color");
            }

            if (DFB_DRAW_BUFFERS != null) {
                fillIntBufferZero(DFB_DRAW_BUFFERS);
            }

            if (noiseTexture != null) {
                noiseTexture.deleteTexture();
                noiseTexture = null;
            }

            Log.info("Uninit");
            shadowPassInterval = 0;
            shouldSkipDefaultShadow = false;
            isShaderPackInitialized = false;
            checkGLError("Shaders.uninit");
        }
    }

    public static void scheduleResize() {
        renderDisplayHeight = 0;
    }

    public static void scheduleResizeShadow() {
        needResizeShadow = true;
    }

    private static void resize() {
        renderDisplayWidth = mc.displayWidth;
        renderDisplayHeight = mc.displayHeight;
        renderWidth = Math.round(renderDisplayWidth * configRenderResMul);
        renderHeight = Math.round(renderDisplayHeight * configRenderResMul);
        setupFrameBuffer();
    }

    private static void resizeShadow() {
        needResizeShadow = false;
        shadowMapWidth = Math.round(spShadowMapWidth * configShadowResMul);
        shadowMapHeight = Math.round(spShadowMapHeight * configShadowResMul);
        setupShadowFrameBuffer();
    }

    private static void setupFrameBuffer() {
        if (dfb != 0) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(dfb);
            GlStateManager.deleteTextures(DFB_DEPTH_TEXTURES);
            GlStateManager.deleteTextures(DFB_COLOR_TEXTURES);
        }

        dfb = EXTFramebufferObject.glGenFramebuffersEXT();
        GL11.glGenTextures(DFB_DEPTH_TEXTURES.clear().limit(usedDepthBuffers));
        GL11.glGenTextures(DFB_COLOR_TEXTURES.clear().limit(16));
        DFB_DEPTH_TEXTURES.position(0);
        DFB_COLOR_TEXTURES.position(0);
        EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
        GL20.glDrawBuffers(0);
        GL11.glReadBuffer(0);

        for (int i = 0; i < usedDepthBuffers; ++i) {
            GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(i));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_DEPTH_TEXTURE_MODE, GL11.GL_LUMINANCE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, renderWidth, renderHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (FloatBuffer) null);
        }

        EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, DFB_DEPTH_TEXTURES.get(0), 0);
        GL20.glDrawBuffers(DFB_DRAW_BUFFERS);
        GL11.glReadBuffer(0);
        checkGLError("FT d");

        for (int k = 0; k < usedColorBuffers; ++k) {
            GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getA(k));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GBUFFERS_FORMAT[k], renderWidth, renderHeight, 0, getPixelFormat(GBUFFERS_FORMAT[k]), GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + k, 3553, DFB_COLOR_TEXTURES_FLIP.getA(k), 0);
            checkGLError("FT c");
        }

        for (int l = 0; l < usedColorBuffers; ++l) {
            GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getB(l));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GBUFFERS_FORMAT[l], renderWidth, renderHeight, 0, getPixelFormat(GBUFFERS_FORMAT[l]), GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);
            checkGLError("FT ca");
        }

        int i1 = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);

        if (i1 == 36058) {
            printChatAndLogError("[Shaders] Error: Failed framebuffer incomplete formats");

            for (int j = 0; j < usedColorBuffers; ++j) {
                GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getA(j));
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, renderWidth, renderHeight, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);
                EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + j, 3553, DFB_COLOR_TEXTURES_FLIP.getA(j), 0);
                checkGLError("FT c");
            }

            i1 = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);

            if (i1 == 36053) {
                Log.info("complete");
            }
        }

        GlStateManager.bindTexture(0);

        if (i1 != 36053) {
            printChatAndLogError("[Shaders] Error: Failed creating framebuffer! (Status " + i1 + ")");
        } else {
            Log.info("Framebuffer created.");
        }
    }

    private static int getPixelFormat(int internalFormat) {
        return switch (internalFormat) {
            case 33333, 33334, 33339, 33340, 36208, 36209, 36226, 36227 -> 36251;
            default -> 32993;
        };
    }

    private static void setupShadowFrameBuffer() {
        if (usedShadowDepthBuffers != 0) {
            if (sfb != 0) {
                EXTFramebufferObject.glDeleteFramebuffersEXT(sfb);
                GlStateManager.deleteTextures(SFB_DEPTH_TEXTURES);
                GlStateManager.deleteTextures(SFB_COLOR_TEXTURES);
            }

            sfb = EXTFramebufferObject.glGenFramebuffersEXT();
            EXTFramebufferObject.glBindFramebufferEXT(36160, sfb);
            GL11.glDrawBuffer(0);
            GL11.glReadBuffer(0);
            GL11.glGenTextures(SFB_DEPTH_TEXTURES.clear().limit(usedShadowDepthBuffers));
            GL11.glGenTextures(SFB_COLOR_TEXTURES.clear().limit(usedShadowColorBuffers));
            SFB_DEPTH_TEXTURES.position(0);
            SFB_COLOR_TEXTURES.position(0);

            for (int i = 0; i < usedShadowDepthBuffers; ++i) {
                GlStateManager.bindTexture(SFB_DEPTH_TEXTURES.get(i));
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071.0F);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071.0F);
                int j = SHADOW_FILTER_NEAREST[i] ? 9728 : 9729;
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, j);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, j);

                if (SHADOW_HARDWARE_FILTERING_ENABLED[i]) {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL14.GL_COMPARE_R_TO_TEXTURE);
                }

                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, shadowMapWidth, shadowMapHeight, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (FloatBuffer) null);
            }

            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, SFB_DEPTH_TEXTURES.get(0), 0);
            checkGLError("FT sd");

            for (int k = 0; k < usedShadowColorBuffers; ++k) {
                GlStateManager.bindTexture(SFB_COLOR_TEXTURES.get(k));
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071.0F);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071.0F);
                int i1 = SHADOW_COLOR_FILTER_NEAREST[k] ? 9728 : 9729;
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, i1);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, i1);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, shadowMapWidth, shadowMapHeight, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) null);
                EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + k, 3553, SFB_COLOR_TEXTURES.get(k), 0);
                checkGLError("FT sc");
            }

            GlStateManager.bindTexture(0);

            if (usedShadowColorBuffers > 0) {
                GL20.glDrawBuffers(SFB_DRAW_BUFFERS);
            }

            int l = EXTFramebufferObject.glCheckFramebufferStatusEXT(36160);

            if (l != 36053) {
                printChatAndLogError("[Shaders] Error: Failed creating shadow framebuffer! (Status " + l + ")");
            } else {
                Log.info("Shadow framebuffer created.");
            }
        }
    }

    public static void beginRender(Minecraft minecraft, float partialTicks, long finishTimeNano) {
        checkGLError("pre beginRender");
        checkWorldChanged(mc.world);
        mc = minecraft;
        entityRenderer = mc.entityRenderer;

        if (!isShaderPackInitialized) {
            try {
                init();
            } catch (IllegalStateException exception) {
                if (Config.normalize(exception.getMessage()).equals("Function is not supported")) {
                    printChatAndLogError("[Shaders] Error: " + exception.getMessage());
                    exception.printStackTrace();
                    setShaderPack("OFF");
                    return;
                }
            }
        }

        if (mc.displayWidth != renderDisplayWidth || mc.displayHeight != renderDisplayHeight) {
            resize();
        }

        if (needResizeShadow) {
            resizeShadow();
        }

        worldTime = mc.world.getWorldTime();
        diffWorldTime = (worldTime - lastWorldTime) % 24000L;

        if (diffWorldTime < 0L) {
            diffWorldTime += 24000L;
        }

        lastWorldTime = worldTime;
        moonPhase = mc.world.getMoonPhase();
        ++frameCounter;

        if (frameCounter >= 720720) {
            frameCounter = 0;
        }

        systemTime = System.currentTimeMillis();

        if (lastSystemTime == 0L) {
            lastSystemTime = systemTime;
        }

        diffSystemTime = systemTime - lastSystemTime;
        lastSystemTime = systemTime;
        frameTime = diffSystemTime / 1000.0F;
        frameTimeCounter += frameTime;
        frameTimeCounter %= 3600.0F;
        rainStrength = minecraft.world.getRainStrength(partialTicks);
        float f = diffSystemTime * 0.01F;
        float f1 = (float) Math.exp(Math.log(0.5D) * f / (wetness < rainStrength ? drynessHalfLife : wetnessHalfLife));
        wetness = wetness * f1 + rainStrength * (1.0F - f1);
        Entity entity = mc.getRenderViewEntity();

        if (entity != null) {
            isSleeping = entity instanceof EntityLivingBase entityLivingBase && entityLivingBase.isPlayerSleeping();
            eyePosY = (float) entity.posY * partialTicks + (float) entity.lastTickPosY * (1.0F - partialTicks);
            eyeBrightness = entity.getBrightnessForRender(partialTicks);
            f1 = diffSystemTime * 0.01F;
            float f2 = (float) Math.exp(Math.log(0.5D) * f1 / eyeBrightnessHalflife);
            eyeBrightnessFadeX = eyeBrightnessFadeX * f2 + (eyeBrightness & 65535) * (1.0F - f2);
            eyeBrightnessFadeY = eyeBrightnessFadeY * f2 + (eyeBrightness >> 16) * (1.0F - f2);
            Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.world, entity, partialTicks);
            Material material = block.getMaterial();

            if (material == Material.WATER) {
                isEyeInWater = 1;
            } else if (material == Material.LAVA) {
                isEyeInWater = 2;
            } else {
                isEyeInWater = 0;
            }

            if (mc.player != null) {
                nightVision = 0.0F;

                if (mc.player.isPotionActive(Potion.NIGHT_VISION)) {
                    nightVision = Config.getMinecraft().entityRenderer.getNightVisionBrightness(mc.player, partialTicks);
                }

                blindness = 0.0F;

                if (mc.player.isPotionActive(Potion.BLINDNESS)) {
                    int i = mc.player.getActivePotionEffect(Potion.BLINDNESS).getDuration();
                    blindness = Config.limit(i / 20.0F, 0.0F, 1.0F);
                }
            }

            Vec3 vec3 = mc.world.getSkyColor(entity, partialTicks);
            vec3 = CustomColors.getWorldSkyColor(vec3, currentWorld, entity, partialTicks);
            skyColorR = (float) vec3.xCoord;
            skyColorG = (float) vec3.yCoord;
            skyColorB = (float) vec3.zCoord;
        }

        isRenderingWorld = true;
        isCompositeRendered = false;
        isShadowPass = false;
        isHandRenderedMain = false;
        isHandRenderedOff = false;
        skipRenderHandMain = false;
        skipRenderHandOff = false;
        bindGbuffersTextures();
        previousCameraPositionX = cameraPositionX;
        previousCameraPositionY = cameraPositionY;
        previousCameraPositionZ = cameraPositionZ;
        PREVIOUS_PROJECTION.position(0);
        PROJECTION.position(0);
        PREVIOUS_PROJECTION.put(PROJECTION);
        PREVIOUS_PROJECTION.position(0);
        PROJECTION.position(0);
        PREVIOUS_MODEL_VIEW.position(0);
        MODEL_VIEW.position(0);
        PREVIOUS_MODEL_VIEW.put(MODEL_VIEW);
        PREVIOUS_MODEL_VIEW.position(0);
        MODEL_VIEW.position(0);
        checkGLError("beginRender");
        ShadersRender.renderShadowMap(entityRenderer, 0, partialTicks, finishTimeNano);
        EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);

        for (int j = 0; j < usedColorBuffers; ++j) {
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + j, 3553, DFB_COLOR_TEXTURES_FLIP.getA(j), 0);
        }

        checkGLError("end beginRender");
    }

    private static void bindGbuffersTextures() {
        if (usedShadowDepthBuffers >= 1) {
            GlStateManager.setActiveTexture(33988);
            GlStateManager.bindTexture(SFB_DEPTH_TEXTURES.get(0));

            if (usedShadowDepthBuffers >= 2) {
                GlStateManager.setActiveTexture(33989);
                GlStateManager.bindTexture(SFB_DEPTH_TEXTURES.get(1));
            }
        }

        GlStateManager.setActiveTexture(33984);

        for (int i = 0; i < usedColorBuffers; ++i) {
            GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getA(i));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getB(i));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        }

        GlStateManager.bindTexture(0);

        for (int j = 0; j < 4 && 4 + j < usedColorBuffers; ++j) {
            GlStateManager.setActiveTexture(33991 + j);
            GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getA(4 + j));
        }

        GlStateManager.setActiveTexture(33990);
        GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(0));

        if (usedDepthBuffers >= 2) {
            GlStateManager.setActiveTexture(33995);
            GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(1));

            if (usedDepthBuffers >= 3) {
                GlStateManager.setActiveTexture(33996);
                GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(2));
            }
        }

        for (int k = 0; k < usedShadowColorBuffers; ++k) {
            GlStateManager.setActiveTexture(33997 + k);
            GlStateManager.bindTexture(SFB_COLOR_TEXTURES.get(k));
        }

        if (noiseTextureEnabled) {
            GlStateManager.setActiveTexture(33984 + noiseTexture.textureUnit());
            GlStateManager.bindTexture(noiseTexture.getTextureId());
        }

        bindCustomTextures(customTexturesGbuffers);
        GlStateManager.setActiveTexture(33984);
    }

    public static void checkWorldChanged(World world) {
        if (currentWorld != world) {
            World oldworld = currentWorld;
            currentWorld = world;
            setCameraOffset(mc.getRenderViewEntity());
            int i = getDimensionId(oldworld);
            int j = getDimensionId(world);

            if (j != i) {
                boolean flag = SHADER_PACK_DIMENSIONS.contains(i);
                boolean flag1 = SHADER_PACK_DIMENSIONS.contains(j);

                if (flag || flag1) {
                    uninit();
                }
            }

            Smoother.resetValues();
        }
    }

    private static int getDimensionId(World world) {
        return world == null ? Integer.MIN_VALUE : world.provider.getDimensionId();
    }

    public static void beginRenderPass(int pass, float partialTicks, long finishTimeNano) {
        if (!isShadowPass) {
            EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
            GL11.glViewport(0, 0, renderWidth, renderHeight);
            activeDrawBuffers = null;
            ShadersTex.bindNSTextures(defaultTexture.getMultiTexID());
            useProgram(PROGRAM_TEXTURED);
            checkGLError("end beginRenderPass");
        }
    }

    public static void setViewport(int vx, int vy, int vw, int vh) {
        GlStateManager.colorMask(true, true, true, true);

        if (isShadowPass) {
            GL11.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
        } else {
            GL11.glViewport(0, 0, renderWidth, renderHeight);
            EXTFramebufferObject.glBindFramebufferEXT(36160, dfb);
            isRenderingDfb = true;
            GlStateManager.enableCull();
            GlStateManager.enableDepth();
            setDrawBuffers(DRAW_BUFFERS_NONE);
            useProgram(PROGRAM_TEXTURED);
            checkGLError("beginRenderPass");
        }
    }

    public static void setFogMode(int value) {
        fogMode = value;

        if (fogEnabled) {
            setProgramUniform1i(UNIFORM_FOG_MODE, value);
        }
    }

    public static void setFogColor(float r, float g, float b) {
        fogColorR = r;
        fogColorG = g;
        fogColorB = b;
        setProgramUniform3f(UNIFORM_FOG_COLOR, fogColorR, fogColorG, fogColorB);
    }

    public static void setClearColor(float red, float green, float blue, float alpha) {
        GlStateManager.clearColor(red, green, blue, alpha);
        clearColorR = red;
        clearColorG = green;
        clearColorB = blue;
    }

    public static void clearRenderBuffer() {
        if (isShadowPass) {
            checkGLError("shadow clear pre");
            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, SFB_DEPTH_TEXTURES.get(0), 0);
            GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL20.glDrawBuffers(PROGRAM_SHADOW.getDrawBuffers());
            checkFramebufferStatus("shadow clear");
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            checkGLError("shadow clear");
        } else {
            checkGLError("clear pre");

            if (GBUFFERS_CLEAR[0]) {
                Vector4f vector4f = GBUFFERS_CLEAR_COLOR[0];

                if (vector4f != null) {
                    GL11.glClearColor(vector4f.x, vector4f.y, vector4f.z, vector4f.w);
                }

                if (DFB_COLOR_TEXTURES_FLIP.isChanged(0)) {
                    EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064, 3553, DFB_COLOR_TEXTURES_FLIP.getB(0), 0);
                    GL20.glDrawBuffers(36064);
                    GL11.glClear(16384);
                    EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064, 3553, DFB_COLOR_TEXTURES_FLIP.getA(0), 0);
                }

                GL20.glDrawBuffers(36064);
                GL11.glClear(16384);
            }

            if (GBUFFERS_CLEAR[1]) {
                GL11.glClearColor(1.0F, 1.0F, 1.0F, 1.0F);
                Vector4f vector4f2 = GBUFFERS_CLEAR_COLOR[1];

                if (vector4f2 != null) {
                    GL11.glClearColor(vector4f2.x, vector4f2.y, vector4f2.z, vector4f2.w);
                }

                if (DFB_COLOR_TEXTURES_FLIP.isChanged(1)) {
                    EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36065, 3553, DFB_COLOR_TEXTURES_FLIP.getB(1), 0);
                    GL20.glDrawBuffers(36065);
                    GL11.glClear(16384);
                    EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36065, 3553, DFB_COLOR_TEXTURES_FLIP.getA(1), 0);
                }

                GL20.glDrawBuffers(36065);
                GL11.glClear(16384);
            }

            for (int i = 2; i < usedColorBuffers; ++i) {
                if (GBUFFERS_CLEAR[i]) {
                    GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                    Vector4f vector4f1 = GBUFFERS_CLEAR_COLOR[i];

                    if (vector4f1 != null) {
                        GL11.glClearColor(vector4f1.x, vector4f1.y, vector4f1.z, vector4f1.w);
                    }

                    if (DFB_COLOR_TEXTURES_FLIP.isChanged(i)) {
                        EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, DFB_COLOR_TEXTURES_FLIP.getB(i), 0);
                        GL20.glDrawBuffers(36064 + i);
                        GL11.glClear(16384);
                        EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, DFB_COLOR_TEXTURES_FLIP.getA(i), 0);
                    }

                    GL20.glDrawBuffers(36064 + i);
                    GL11.glClear(16384);
                }
            }

            setDrawBuffers(DFB_DRAW_BUFFERS);
            checkFramebufferStatus("clear");
            checkGLError("clear");
        }
    }

    public static void setCamera(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();
        double xPos = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double yPos = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double zPos = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        updateCameraOffset(entity);
        cameraPositionX = xPos - cameraOffsetX;
        cameraPositionY = yPos;
        cameraPositionZ = zPos - cameraOffsetZ;
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, PROJECTION.position(0));
        SMath.invertMat4FBFA(PROJECTION_INVERSE.position(0), PROJECTION.position(0), FA_PROJECTION_INVERSE, FA_PROJECTION);
        PROJECTION.position(0);
        PROJECTION_INVERSE.position(0);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW.position(0));
        SMath.invertMat4FBFA(MODEL_VIEW_INVERSE.position(0), MODEL_VIEW.position(0), FA_MODEL_VIEW_INVERSE, FA_MODEL_VIEW);
        MODEL_VIEW.position(0);
        MODEL_VIEW_INVERSE.position(0);
        checkGLError("setCamera");
    }

    private static void updateCameraOffset(Entity viewEntity) {
        double d0 = Math.abs(cameraPositionX - previousCameraPositionX);
        double d1 = Math.abs(cameraPositionZ - previousCameraPositionZ);
        double d2 = Math.abs(cameraPositionX);
        double d3 = Math.abs(cameraPositionZ);

        if (d0 > 1000.0D || d1 > 1000.0D || d2 > 1000000.0D || d3 > 1000000.0D) {
            setCameraOffset(viewEntity);
        }
    }

    private static void setCameraOffset(Entity viewEntity) {
        if (viewEntity == null) {
            cameraOffsetX = 0;
            cameraOffsetZ = 0;
        } else {
            cameraOffsetX = (int) viewEntity.posX / 1000 * 1000;
            cameraOffsetZ = (int) viewEntity.posZ / 1000 * 1000;
        }
    }

    public static void setCameraShadow(float partialTicks) {
        Entity entity = mc.getRenderViewEntity();
        double xPos = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double yPos = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double zPos = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        updateCameraOffset(entity);
        cameraPositionX = xPos - cameraOffsetX;
        cameraPositionY = yPos;
        cameraPositionZ = zPos - cameraOffsetZ;
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, PROJECTION.position(0));
        SMath.invertMat4FBFA(PROJECTION_INVERSE.position(0), PROJECTION.position(0), FA_PROJECTION_INVERSE, FA_PROJECTION);
        PROJECTION.position(0);
        PROJECTION_INVERSE.position(0);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, MODEL_VIEW.position(0));
        SMath.invertMat4FBFA(MODEL_VIEW_INVERSE.position(0), MODEL_VIEW.position(0), FA_MODEL_VIEW_INVERSE, FA_MODEL_VIEW);
        MODEL_VIEW.position(0);
        MODEL_VIEW_INVERSE.position(0);
        GL11.glViewport(0, 0, shadowMapWidth, shadowMapHeight);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        if (shadowMapIsOrtho) {
            GL11.glOrtho((-shadowMapHalfPlane), shadowMapHalfPlane, (-shadowMapHalfPlane), shadowMapHalfPlane, 0.05000000074505806D, 256.0D);
        } else {
            Matrix4f projectionMatrix = new Matrix4f().perspective(
                    (float) Math.toRadians(shadowMapFOV),
                    (float) shadowMapWidth / shadowMapHeight,
                    0.05F,
                    256.0F
            );
            FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
            projectionMatrix.get(projectionBuffer);
            GlStateManager.multMatrix(projectionBuffer);
        }

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -100.0F);
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        celestialAngle = mc.world.getCelestialAngle(partialTicks);
        sunAngle = celestialAngle < 0.75F ? celestialAngle + 0.25F : celestialAngle - 0.75F;
        float f = celestialAngle * -360.0F;

        if (sunAngle <= 0.5D) {
            GL11.glRotatef(f, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(sunPathRotation, 1.0F, 0.0F, 0.0F);
            shadowAngle = sunAngle;
        } else {
            GL11.glRotatef(f + 180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(sunPathRotation, 1.0F, 0.0F, 0.0F);
            shadowAngle = sunAngle - 0.5F;
        }

        if (shadowMapIsOrtho) {
            float f2 = shadowIntervalSize;
            float f3 = f2 / 2.0F;
            GL11.glTranslatef((float) xPos % f2 - f3, (float) yPos % f2 - f3, (float) zPos % f2 - f3);
        }

        float f9 = sunAngle * ((float) Math.PI * 2.0F);
        float f10 = (float) Math.cos(f9);
        float f4 = (float) Math.sin(f9);
        float f5 = sunPathRotation * ((float) Math.PI * 2.0F);
        float f6 = f10;
        float f7 = f4 * (float) Math.cos(f5);
        float f8 = f4 * (float) Math.sin(f5);

        if (sunAngle > 0.5D) {
            f6 = -f10;
            f7 = -f7;
            f8 = -f8;
        }

        SHADOW_LIGHT_POSITION_VECTOR[0] = f6;
        SHADOW_LIGHT_POSITION_VECTOR[1] = f7;
        SHADOW_LIGHT_POSITION_VECTOR[2] = f8;
        SHADOW_LIGHT_POSITION_VECTOR[3] = 0.0F;
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, SHADOW_PROJECTION.position(0));
        SMath.invertMat4FBFA(SHADOW_PROJECTION_INVERSE.position(0), SHADOW_PROJECTION.position(0), FA_SHADOW_PROJECTION_INVERSE, FA_SHADOW_PROJECTION);
        SHADOW_PROJECTION.position(0);
        SHADOW_PROJECTION_INVERSE.position(0);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, SHADOW_MODEL_VIEW.position(0));
        SMath.invertMat4FBFA(SHADOW_MODEL_VIEW_INVERSE.position(0), SHADOW_MODEL_VIEW.position(0), FA_SHADOW_MODEL_VIEW_INVERSE, FA_SHADOW_MODEL_VIEW);
        SHADOW_MODEL_VIEW.position(0);
        SHADOW_MODEL_VIEW_INVERSE.position(0);
        setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PROJECTION, false, PROJECTION);
        setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PROJECTION_INVERSE, false, PROJECTION_INVERSE);
        setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PREVIOUS_PROJECTION, false, PREVIOUS_PROJECTION);
        setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_MODEL_VIEW, false, MODEL_VIEW);
        setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_MODEL_VIEW_INVERSE, false, MODEL_VIEW_INVERSE);
        setProgramUniformMatrix4ARB(UNIFORM_GBUFFER_PREVIOUS_MODEL_VIEW, false, PREVIOUS_MODEL_VIEW);
        setProgramUniformMatrix4ARB(UNIFORM_SHADOW_PROJECTION, false, SHADOW_PROJECTION);
        setProgramUniformMatrix4ARB(UNIFORM_SHADOW_PROJECTION_INVERSE, false, SHADOW_PROJECTION_INVERSE);
        setProgramUniformMatrix4ARB(UNIFORM_SHADOW_MODEL_VIEW, false, SHADOW_MODEL_VIEW);
        setProgramUniformMatrix4ARB(UNIFORM_SHADOW_MODEL_VIEW_INVERSE, false, SHADOW_MODEL_VIEW_INVERSE);
        mc.gameSettings.thirdPersonView = GameSettings.Perspective.THIRD_PERSON;
        checkGLError("setCamera");
    }

    public static void preCelestialRotate() {
        GL11.glRotatef(sunPathRotation, 0.0F, 0.0F, 1.0F);
        checkGLError("preCelestialRotate");
    }

    public static void postCelestialRotate() {
        FloatBuffer floatbuffer = TEMP_MATRIX_DIRECT_BUFFER;
        floatbuffer.clear();
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, floatbuffer);
        floatbuffer.get(TEMP_MAT, 0, 16);
        SMath.multiplyMat4xVec4(SUN_POSITION, TEMP_MAT, SUN_POS_MODEL_VIEW);
        SMath.multiplyMat4xVec4(MOON_POSITION, TEMP_MAT, MOON_POS_MODEL_VIEW);
        System.arraycopy(shadowAngle == sunAngle ? SUN_POSITION : MOON_POSITION, 0, SHADOW_LIGHT_POSITION, 0, 3);
        setProgramUniform3f(UNIFORM_SUN_POSITION, SUN_POSITION[0], SUN_POSITION[1], SUN_POSITION[2]);
        setProgramUniform3f(UNIFORM_MOON_POSITION, MOON_POSITION[0], MOON_POSITION[1], MOON_POSITION[2]);
        setProgramUniform3f(UNIFORM_SHADOW_LIGHT_POSITION, SHADOW_LIGHT_POSITION[0], SHADOW_LIGHT_POSITION[1], SHADOW_LIGHT_POSITION[2]);

        if (customUniforms != null) {
            customUniforms.update();
        }

        checkGLError("postCelestialRotate");
    }

    public static void setUpPosition() {
        FloatBuffer floatbuffer = TEMP_MATRIX_DIRECT_BUFFER;
        floatbuffer.clear();
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, floatbuffer);
        floatbuffer.get(TEMP_MAT, 0, 16);
        SMath.multiplyMat4xVec4(UP_POSITION, TEMP_MAT, UP_POS_MODEL_VIEW);
        setProgramUniform3f(UNIFORM_UP_POSITION, UP_POSITION[0], UP_POSITION[1], UP_POSITION[2]);

        if (customUniforms != null) {
            customUniforms.update();
        }
    }

    public static void genCompositeMipmap() {
        if (hasGlGenMipmap) {
            for (int i = 0; i < usedColorBuffers; ++i) {
                if ((activeCompositeMipmapSetting & 1 << i) != 0) {
                    GlStateManager.setActiveTexture(33984 + COLOR_TEXTURE_IMAGE_UNIT[i]);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
                    GL30.glGenerateMipmap(3553);
                }
            }

            GlStateManager.setActiveTexture(33984);
        }
    }

    public static void drawComposite() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        drawCompositeQuad();
        int i = activeProgram.getCountInstances();

        if (i > 1) {
            for (int j = 1; j < i; ++j) {
                UNIFORM_INSTANCE_ID.setValue(j);
                drawCompositeQuad();
            }

            UNIFORM_INSTANCE_ID.setValue(0);
        }
    }

    private static void drawCompositeQuad() {
        if (!canRenderQuads()) {
            GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(0.0F, 0.0F, 0.0F);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(1.0F, 0.0F, 0.0F);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(0.0F, 1.0F, 0.0F);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(1.0F, 1.0F, 0.0F);
        } else {
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex3f(0.0F, 0.0F, 0.0F);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex3f(1.0F, 0.0F, 0.0F);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex3f(1.0F, 1.0F, 0.0F);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex3f(0.0F, 1.0F, 0.0F);
        }
        GL11.glEnd();
    }

    public static void renderDeferred() {
        if (!isShadowPass) {
            boolean flag = checkBufferFlip(PROGRAM_DEFERRED_PRE);

            if (hasDeferredPrograms) {
                checkGLError("pre-render Deferred");
                renderComposites(PROGRAMS_DEFERRED, false);
                flag = true;
            }

            if (flag) {
                bindGbuffersTextures();

                for (int i = 0; i < usedColorBuffers; ++i) {
                    EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + i, 3553, DFB_COLOR_TEXTURES_FLIP.getA(i), 0);
                }

                if (PROGRAM_WATER.getDrawBuffers() != null) {
                    setDrawBuffers(PROGRAM_WATER.getDrawBuffers());
                } else {
                    setDrawBuffers(DFB_DRAW_BUFFERS);
                }

                GlStateManager.setActiveTexture(33984);
                mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            }
        }
    }

    public static void renderCompositeFinal() {
        if (!isShadowPass) {
            checkBufferFlip(PROGRAM_COMPOSITE_PRE);
            checkGLError("pre-render CompositeFinal");
            renderComposites(PROGRAMS_COMPOSITE, true);
        }
    }

    private static boolean checkBufferFlip(Program program) {
        boolean flag = false;
        Boolean[] aboolean = program.getBuffersFlip();

        for (int i = 0; i < usedColorBuffers; ++i) {
            if (Config.isTrue(aboolean[i])) {
                DFB_COLOR_TEXTURES_FLIP.flip(i);
                flag = true;
            }
        }

        return flag;
    }

    private static void renderComposites(Program[] ps, boolean renderFinal) {
        if (!isShadowPass) {
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, 0.0D, 1.0D);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_ALWAYS);
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();

            if (usedShadowDepthBuffers >= 1) {
                GlStateManager.setActiveTexture(33988);
                GlStateManager.bindTexture(SFB_DEPTH_TEXTURES.get(0));

                if (usedShadowDepthBuffers >= 2) {
                    GlStateManager.setActiveTexture(33989);
                    GlStateManager.bindTexture(SFB_DEPTH_TEXTURES.get(1));
                }
            }

            for (int i = 0; i < usedColorBuffers; ++i) {
                GlStateManager.setActiveTexture(33984 + COLOR_TEXTURE_IMAGE_UNIT[i]);
                GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getA(i));
            }

            GlStateManager.setActiveTexture(33990);
            GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(0));

            if (usedDepthBuffers >= 2) {
                GlStateManager.setActiveTexture(33995);
                GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(1));

                if (usedDepthBuffers >= 3) {
                    GlStateManager.setActiveTexture(33996);
                    GlStateManager.bindTexture(DFB_DEPTH_TEXTURES.get(2));
                }
            }

            for (int k = 0; k < usedShadowColorBuffers; ++k) {
                GlStateManager.setActiveTexture(33997 + k);
                GlStateManager.bindTexture(SFB_COLOR_TEXTURES.get(k));
            }

            if (noiseTextureEnabled) {
                GlStateManager.setActiveTexture(33984 + noiseTexture.textureUnit());
                GlStateManager.bindTexture(noiseTexture.getTextureId());
            }

            if (renderFinal) {
                bindCustomTextures(customTexturesComposite);
            } else {
                bindCustomTextures(customTexturesDeferred);
            }

            GlStateManager.setActiveTexture(33984);

            for (int l = 0; l < usedColorBuffers; ++l) {
                EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + l, 3553, DFB_COLOR_TEXTURES_FLIP.getB(l), 0);
            }

            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36096, 3553, DFB_DEPTH_TEXTURES.get(0), 0);
            GL20.glDrawBuffers(DFB_DRAW_BUFFERS);
            checkGLError("pre-composite");

            for (Program program : ps) {
                if (program.getId() != 0) {
                    useProgram(program);
                    checkGLError(program.getName());

                    if (activeCompositeMipmapSetting != 0) {
                        genCompositeMipmap();
                    }

                    preDrawComposite();
                    drawComposite();
                    postDrawComposite();

                    for (int j = 0; j < usedColorBuffers; ++j) {
                        if (program.getToggleColorTextures()[j]) {
                            DFB_COLOR_TEXTURES_FLIP.flip(j);
                            GlStateManager.setActiveTexture(33984 + COLOR_TEXTURE_IMAGE_UNIT[j]);
                            GlStateManager.bindTexture(DFB_COLOR_TEXTURES_FLIP.getA(j));
                            EXTFramebufferObject.glFramebufferTexture2DEXT(36160, 36064 + j, 3553, DFB_COLOR_TEXTURES_FLIP.getB(j), 0);
                        }
                    }

                    GlStateManager.setActiveTexture(33984);
                }
            }

            checkGLError("composite");

            if (renderFinal) {
                renderFinal();
                isCompositeRendered = true;
            }

            GlStateManager.enableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            useProgram(PROGRAM_NONE);
        }
    }

    private static void preDrawComposite() {
        RenderScale renderscale = activeProgram.getRenderScale();

        if (renderscale != null) {
            int i = (int) (renderWidth * renderscale.offsetX());
            int j = (int) (renderHeight * renderscale.offsetY());
            int k = (int) (renderWidth * renderscale.scale());
            int l = (int) (renderHeight * renderscale.scale());
            GL11.glViewport(i, j, k, l);
        }
    }

    private static void postDrawComposite() {
        RenderScale renderscale = activeProgram.getRenderScale();

        if (renderscale != null) {
            GL11.glViewport(0, 0, renderWidth, renderHeight);
        }
    }

    private static void renderFinal() {
        isRenderingDfb = false;
        mc.getFramebuffer().bindFramebuffer(true);
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, 3553, mc.getFramebuffer().framebufferTexture, 0);
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);

        GlStateManager.depthMask(true);
        GL11.glClearColor(clearColorR, clearColorG, clearColorB, 1.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_ALWAYS);
        GlStateManager.depthMask(false);
        checkGLError("pre-final");
        useProgram(PROGRAM_FINAL);
        checkGLError("final");

        if (activeCompositeMipmapSetting != 0) {
            genCompositeMipmap();
        }

        drawComposite();
        checkGLError("renderCompositeFinal");
    }

    public static void endRender() {
        if (isShadowPass) {
            checkGLError("shadow endRender");
        } else {
            if (!isCompositeRendered) {
                renderCompositeFinal();
            }

            isRenderingWorld = false;
            GlStateManager.colorMask(true, true, true, true);
            useProgram(PROGRAM_NONE);
            RenderHelper.disableStandardItemLighting();
            checkGLError("endRender end");
        }
    }

    public static void beginSky() {
        isRenderingSky = true;
        fogEnabled = true;
        setDrawBuffers(DFB_DRAW_BUFFERS);
        useProgram(PROGRAM_SKY_TEXTURED);
        pushEntity(-2, 0);
    }

    public static void setSkyColor(Vec3 v3color) {
        skyColorR = (float) v3color.xCoord;
        skyColorG = (float) v3color.yCoord;
        skyColorB = (float) v3color.zCoord;
        setProgramUniform3f(UNIFORM_SKY_COLOR, skyColorR, skyColorG, skyColorB);
    }

    public static void drawHorizon() {
        WorldRenderer worldrenderer = Tessellator.get().getWorldRenderer();
        float f = (mc.gameSettings.renderDistanceChunks * 16);
        double d0 = f * 0.9238D;
        double d1 = f * 0.3826D;
        double d2 = -d1;
        double d3 = -d0;
        double d4 = 16.0D;
        double d5 = -cameraPositionY;
        worldrenderer.begin(7, DefaultVertexFormats.POSITION);
        worldrenderer.pos(d2, d5, d3).endVertex();
        worldrenderer.pos(d2, d4, d3).endVertex();
        worldrenderer.pos(d3, d4, d2).endVertex();
        worldrenderer.pos(d3, d5, d2).endVertex();
        worldrenderer.pos(d3, d5, d2).endVertex();
        worldrenderer.pos(d3, d4, d2).endVertex();
        worldrenderer.pos(d3, d4, d1).endVertex();
        worldrenderer.pos(d3, d5, d1).endVertex();
        worldrenderer.pos(d3, d5, d1).endVertex();
        worldrenderer.pos(d3, d4, d1).endVertex();
        worldrenderer.pos(d2, d4, d0).endVertex();
        worldrenderer.pos(d2, d5, d0).endVertex();
        worldrenderer.pos(d2, d5, d0).endVertex();
        worldrenderer.pos(d2, d4, d0).endVertex();
        worldrenderer.pos(d1, d4, d0).endVertex();
        worldrenderer.pos(d1, d5, d0).endVertex();
        worldrenderer.pos(d1, d5, d0).endVertex();
        worldrenderer.pos(d1, d4, d0).endVertex();
        worldrenderer.pos(d0, d4, d1).endVertex();
        worldrenderer.pos(d0, d5, d1).endVertex();
        worldrenderer.pos(d0, d5, d1).endVertex();
        worldrenderer.pos(d0, d4, d1).endVertex();
        worldrenderer.pos(d0, d4, d2).endVertex();
        worldrenderer.pos(d0, d5, d2).endVertex();
        worldrenderer.pos(d0, d5, d2).endVertex();
        worldrenderer.pos(d0, d4, d2).endVertex();
        worldrenderer.pos(d1, d4, d3).endVertex();
        worldrenderer.pos(d1, d5, d3).endVertex();
        worldrenderer.pos(d1, d5, d3).endVertex();
        worldrenderer.pos(d1, d4, d3).endVertex();
        worldrenderer.pos(d2, d4, d3).endVertex();
        worldrenderer.pos(d2, d5, d3).endVertex();
        worldrenderer.pos(d3, d5, d3).endVertex();
        worldrenderer.pos(d3, d5, d0).endVertex();
        worldrenderer.pos(d0, d5, d0).endVertex();
        worldrenderer.pos(d0, d5, d3).endVertex();
        Tessellator.get().draw();
    }

    public static void preSkyList() {
        setUpPosition();
        GL11.glColor3f(fogColorR, fogColorG, fogColorB);
        drawHorizon();
        GL11.glColor3f(skyColorR, skyColorG, skyColorB);
    }

    public static void endSky() {
        isRenderingSky = false;
        setDrawBuffers(DFB_DRAW_BUFFERS);
        useProgram(lightmapEnabled ? PROGRAM_TEXTURED_LIT : PROGRAM_TEXTURED);
        popEntity();
    }

    public static void beginUpdateChunks() {
        checkGLError("beginUpdateChunks1");
        checkFramebufferStatus("beginUpdateChunks1");

        if (!isShadowPass) {
            useProgram(PROGRAM_TERRAIN);
        }

        checkGLError("beginUpdateChunks2");
        checkFramebufferStatus("beginUpdateChunks2");
    }

    public static void endUpdateChunks() {
        checkGLError("endUpdateChunks1");
        checkFramebufferStatus("endUpdateChunks1");

        if (!isShadowPass) {
            useProgram(PROGRAM_TERRAIN);
        }

        checkGLError("endUpdateChunks2");
        checkFramebufferStatus("endUpdateChunks2");
    }

    public static boolean shouldRenderClouds(GameSettings gs) {
        if (!shaderPackLoaded) {
            return true;
        } else {
            checkGLError("shouldRenderClouds");
            return isShadowPass ? configCloudShadow : gs.clouds > 0;
        }
    }

    public static void beginClouds() {
        fogEnabled = true;
        pushEntity(-3, 0);
        useProgram(PROGRAM_CLOUDS);
    }

    public static void endClouds() {
        disableFog();
        popEntity();
        useProgram(lightmapEnabled ? PROGRAM_TEXTURED_LIT : PROGRAM_TEXTURED);
    }

    public static void beginEntities() {
        if (isRenderingWorld) {
            useProgram(PROGRAM_ENTITIES);
        }
    }

    public static void nextEntity(Entity entity) {
        if (isRenderingWorld) {
            useProgram(PROGRAM_ENTITIES);
            setEntityId(entity);
        }
    }

    public static void setEntityId(Entity entity) {
        if (UNIFORM_ENTITY_ID.isDefined()) {
            int i = EntityUtils.getEntityIdByClass(entity);
            int j = EntityAliases.getEntityAliasId(i);

            if (j >= 0) {
                i = j;
            }

            UNIFORM_ENTITY_ID.setValue(i);
        }
    }

    public static void beginSpiderEyes() {
        if (isRenderingWorld && PROGRAM_SPIDER_EYES.getId() != PROGRAM_NONE.getId()) {
            useProgram(PROGRAM_SPIDER_EYES);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
            GlStateManager.blendFunc(770, 771);
        }
    }

    public static void endSpiderEyes() {
        if (isRenderingWorld && PROGRAM_SPIDER_EYES.getId() != PROGRAM_NONE.getId()) {
            useProgram(PROGRAM_ENTITIES);
            GlStateManager.disableAlpha();
        }
    }

    public static void endEntities() {
        if (isRenderingWorld) {
            setEntityId(null);
            useProgram(lightmapEnabled ? PROGRAM_TEXTURED_LIT : PROGRAM_TEXTURED);
        }
    }

    public static void beginEntitiesGlowing() {
        if (isRenderingWorld) {
            isEntitiesGlowing = true;
        }
    }

    public static void endEntitiesGlowing() {
        if (isRenderingWorld) {
            isEntitiesGlowing = false;
        }
    }

    public static void setEntityColor(float r, float g, float b, float a) {
        if (isRenderingWorld && !isShadowPass) {
            UNIFORM_ENTITY_COLOR.setValue(r, g, b, a);
        }
    }

    public static void beginLivingDamage() {
        if (isRenderingWorld) {
            ShadersTex.bindTexture(defaultTexture);

            if (!isShadowPass) {
                setDrawBuffers(DRAW_BUFFERS_COLOR_ATT_0);
            }
        }
    }

    public static void endLivingDamage() {
        if (isRenderingWorld && !isShadowPass) {
            setDrawBuffers(PROGRAM_ENTITIES.getDrawBuffers());
        }
    }

    public static void beginBlockEntities() {
        if (isRenderingWorld) {
            checkGLError("beginBlockEntities");
            useProgram(PROGRAM_BLOCK);
        }
    }

    public static void nextBlockEntity(TileEntity tileEntity) {
        if (isRenderingWorld) {
            checkGLError("nextBlockEntity");
            useProgram(PROGRAM_BLOCK);
            setBlockEntityId(tileEntity);
        }
    }

    public static void setBlockEntityId(TileEntity tileEntity) {
        if (UNIFORM_BLOCK_ENTITY_ID.isDefined()) {
            int i = getBlockEntityId(tileEntity);
            UNIFORM_BLOCK_ENTITY_ID.setValue(i);
        }
    }

    private static int getBlockEntityId(TileEntity tileEntity) {
        if (tileEntity == null) {
            return -1;
        } else {
            Block block = tileEntity.getBlockType();

            if (block == null) {
                return 0;
            } else {
                int i = Block.getIdFromBlock(block);
                int j = tileEntity.getBlockMetadata();
                int k = BlockAliases.getBlockAliasId(i, j);

                if (k >= 0) {
                    i = k;
                }

                return i;
            }
        }
    }

    public static void endBlockEntities() {
        if (isRenderingWorld) {
            checkGLError("endBlockEntities");
            setBlockEntityId(null);
            useProgram(lightmapEnabled ? PROGRAM_TEXTURED_LIT : PROGRAM_TEXTURED);
            ShadersTex.bindNSTextures(defaultTexture.getMultiTexID());
        }
    }

    public static void beginLitParticles() {
        useProgram(PROGRAM_TEXTURED_LIT);
    }

    public static void beginParticles() {
        useProgram(PROGRAM_TEXTURED);
    }

    public static void endParticles() {
        useProgram(PROGRAM_TEXTURED_LIT);
    }

    public static void readCenterDepth() {
        if (!isShadowPass && centerDepthSmoothEnabled) {
            TEMP_DIRECT_FLOAT_BUFFER.clear();
            GL11.glReadPixels(renderWidth / 2, renderHeight / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, TEMP_DIRECT_FLOAT_BUFFER);
            centerDepth = TEMP_DIRECT_FLOAT_BUFFER.get(0);
            float f = diffSystemTime * 0.01F;
            float f1 = (float) Math.exp(Math.log(0.5D) * f / centerDepthSmoothHalflife);
            centerDepthSmooth = centerDepthSmooth * f1 + centerDepth * (1.0F - f1);
        }
    }

    public static void beginWeather() {
        if (!isShadowPass) {
            if (usedDepthBuffers >= 3) {
                GlStateManager.setActiveTexture(33996);
                GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, renderWidth, renderHeight);
                GlStateManager.setActiveTexture(33984);
            }

            GlStateManager.enableDepth();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.enableAlpha();
            useProgram(PROGRAM_WEATHER);
        }
    }

    public static void endWeather() {
        GlStateManager.disableBlend();
        useProgram(PROGRAM_TEXTURED_LIT);
    }

    public static void preWater() {
        if (usedDepthBuffers >= 2) {
            GlStateManager.setActiveTexture(33995);
            checkGLError("pre copy depth");
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, renderWidth, renderHeight);
            checkGLError("copy depth");
            GlStateManager.setActiveTexture(33984);
        }

        ShadersTex.bindNSTextures(defaultTexture.getMultiTexID());
    }

    public static void beginWater() {
        if (isRenderingWorld) {
            if (!isShadowPass) {
                renderDeferred();
                useProgram(PROGRAM_WATER);
                GlStateManager.enableBlend();
            }
            GlStateManager.depthMask(true);
        }
    }

    public static void endWater() {
        if (isRenderingWorld) {
            if (isShadowPass) {
            }

            useProgram(lightmapEnabled ? PROGRAM_TEXTURED_LIT : PROGRAM_TEXTURED);
        }
    }

    public static void applyHandDepth() {
        if (configHandDepthMul != 1.0D) {
            GL11.glScaled(1.0D, 1.0D, configHandDepthMul);
        }
    }

    public static void beginHand(boolean translucent) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        if (translucent) {
            useProgram(PROGRAM_HAND_WATER);
        } else {
            useProgram(PROGRAM_HAND);
        }

        checkGLError("beginHand");
        checkFramebufferStatus("beginHand");
    }

    public static void endHand() {
        checkGLError("pre endHand");
        checkFramebufferStatus("pre endHand");
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GlStateManager.blendFunc(770, 771);
        checkGLError("endHand");
    }

    public static void beginFPOverlay() {
        GlStateManager.disableLighting();
        GlStateManager.disableBlend();
    }

    public static void endFPOverlay() {
    }

    public static void glEnableWrapper(int cap) {
        GL11.glEnable(cap);

        if (cap == 3553) {
            enableTexture2D();
        } else if (cap == 2912) {
            enableFog();
        }
    }

    public static void glDisableWrapper(int cap) {
        GL11.glDisable(cap);

        if (cap == 3553) {
            disableTexture2D();
        } else if (cap == 2912) {
            disableFog();
        }
    }

    public static void sglEnableT2D(int cap) {
        GL11.glEnable(cap);
        enableTexture2D();
    }

    public static void sglDisableT2D(int cap) {
        GL11.glDisable(cap);
        disableTexture2D();
    }

    public static void sglEnableFog(int cap) {
        GL11.glEnable(cap);
        enableFog();
    }

    public static void sglDisableFog(int cap) {
        GL11.glDisable(cap);
        disableFog();
    }

    public static void enableTexture2D() {
        if (isRenderingSky) {
            useProgram(PROGRAM_SKY_TEXTURED);
        } else if (activeProgram == PROGRAM_BASIC) {
            useProgram(lightmapEnabled ? PROGRAM_TEXTURED_LIT : PROGRAM_TEXTURED);
        }
    }

    public static void disableTexture2D() {
        if (isRenderingSky) {
            useProgram(PROGRAM_SKY_BASIC);
        } else if (activeProgram == PROGRAM_TEXTURED || activeProgram == PROGRAM_TEXTURED_LIT) {
            useProgram(PROGRAM_BASIC);
        }
    }

    public static void pushProgram() {
        PROGRAM_STACK.push(activeProgram);
    }

    public static void popProgram() {
        Program program = PROGRAM_STACK.pop();
        useProgram(program);
    }

    public static void beginLeash() {
        pushProgram();
        useProgram(PROGRAM_BASIC);
    }

    public static void endLeash() {
        popProgram();
    }

    public static void enableFog() {
        fogEnabled = true;
        setProgramUniform1i(UNIFORM_FOG_MODE, fogMode);
        setProgramUniform1f(UNIFORM_FOG_DENSITY, fogDensity);
    }

    public static void disableFog() {
        fogEnabled = false;
        setProgramUniform1i(UNIFORM_FOG_MODE, 0);
    }

    public static void setFogDensity(float value) {
        fogDensity = value;

        if (fogEnabled) {
            setProgramUniform1f(UNIFORM_FOG_DENSITY, value);
        }
    }

    public static void sglFogi(int pname, int param) {
        GL11.glFogi(pname, param);

        if (pname == 2917) {
            fogMode = param;

            if (fogEnabled) {
                setProgramUniform1i(UNIFORM_FOG_MODE, fogMode);
            }
        }
    }

    public static void enableLightmap() {
        lightmapEnabled = true;

        if (activeProgram == PROGRAM_TEXTURED) {
            useProgram(PROGRAM_TEXTURED_LIT);
        }
    }

    public static void disableLightmap() {
        lightmapEnabled = false;

        if (activeProgram == PROGRAM_TEXTURED_LIT) {
            useProgram(PROGRAM_TEXTURED);
        }
    }

    public static int getEntityData() {
        return ENTITY_DATA[entityDataIndex * 2];
    }

    public static int getEntityData2() {
        return ENTITY_DATA[entityDataIndex * 2 + 1];
    }

    public static int setEntityData1(int data1) {
        ENTITY_DATA[entityDataIndex * 2] = ENTITY_DATA[entityDataIndex * 2] & 65535 | data1 << 16;
        return data1;
    }

    public static int setEntityData2(int data2) {
        ENTITY_DATA[entityDataIndex * 2 + 1] = ENTITY_DATA[entityDataIndex * 2 + 1] & -65536 | data2 & 65535;
        return data2;
    }

    public static void pushEntity(int data0, int data1) {
        ++entityDataIndex;
        ENTITY_DATA[entityDataIndex * 2] = data0 & 65535 | data1 << 16;
        ENTITY_DATA[entityDataIndex * 2 + 1] = 0;
    }

    public static void pushEntity(int data0) {
        ++entityDataIndex;
        ENTITY_DATA[entityDataIndex * 2] = data0 & 65535;
        ENTITY_DATA[entityDataIndex * 2 + 1] = 0;
    }

    public static void pushEntity(Block block) {
        ++entityDataIndex;
        int i = block.getRenderType();
        ENTITY_DATA[entityDataIndex * 2] = Block.blockRegistry.getIDForObject(block) & 65535 | i << 16;
        ENTITY_DATA[entityDataIndex * 2 + 1] = 0;
    }

    public static void popEntity() {
        ENTITY_DATA[entityDataIndex * 2] = 0;
        ENTITY_DATA[entityDataIndex * 2 + 1] = 0;
        --entityDataIndex;
    }

    public static String getShaderPackName() {
        return shaderPack == null ? null : (shaderPack instanceof ShaderPackNone ? null : shaderPack.getName());
    }

    public static InputStream getShaderPackResourceStream(String path) {
        return shaderPack == null ? null : shaderPack.getResourceAsStream(path);
    }

    public static void nextAntialiasingLevel(boolean forward) {
        if (forward) {
            configAntialiasingLevel += 2;

            if (configAntialiasingLevel > 4) {
                configAntialiasingLevel = 0;
            }
        } else {
            configAntialiasingLevel -= 2;

            if (configAntialiasingLevel < 0) {
                configAntialiasingLevel = 4;
            }
        }

        configAntialiasingLevel = configAntialiasingLevel / 2 * 2;
        configAntialiasingLevel = Config.limit(configAntialiasingLevel, 0, 4);
    }

    public static void resourcesReloaded() {
        loadShaderPackResources();

        if (shaderPackLoaded) {
            BlockAliases.resourcesReloaded();
            ItemAliases.resourcesReloaded();
            EntityAliases.resourcesReloaded();
        }
    }

    private static void loadShaderPackResources() {
        shaderPackResources = new HashMap<>();

        if (shaderPackLoaded) {
            List<String> list = new ArrayList<>();
            String s = "/shaders/lang/";
            String s1 = "en_US";
            String s2 = ".lang";
            list.add(s + s1 + s2);

            if (!Config.getGameSettings().language.equals(s1)) {
                list.add(s + Config.getGameSettings().language + s2);
            }

            try {
                for (String s3 : list) {
                    InputStream inputstream = shaderPack.getResourceAsStream(s3);

                    if (inputstream != null) {
                        Properties properties = new PropertiesOrdered();
                        Lang.loadLocaleData(inputstream, (Map) properties);
                        inputstream.close();

                        for (Object s4 : properties.keySet()) {
                            String cString = (String) s4;
                            shaderPackResources.put(cString, properties.getProperty(cString));
                        }
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static String translate(String key, String def) {
        String s = shaderPackResources.get(key);
        return s == null ? def : s;
    }

    public static boolean isProgramPath(String path) {
        if (path == null) {
            return false;
        } else if (path.isEmpty()) {
            return false;
        } else {
            int i = path.lastIndexOf("/");

            if (i >= 0) {
                path = path.substring(i + 1);
            }

            Program program = getProgram(path);
            return program != null;
        }
    }

    public static Program getProgram(String name) {
        return PROGRAMS.getProgram(name);
    }

    public static void setItemToRenderMain(ItemStack itemToRenderMain) {
        itemToRenderMainTranslucent = isTranslucentBlock(itemToRenderMain);
    }

    public static void setItemToRenderOff(ItemStack itemToRenderOff) {
        itemToRenderOffTranslucent = isTranslucentBlock(itemToRenderOff);
    }

    public static boolean isItemToRenderMainTranslucent() {
        return itemToRenderMainTranslucent;
    }

    public static boolean isItemToRenderOffTranslucent() {
        return itemToRenderOffTranslucent;
    }

    public static boolean isBothHandsRendered() {
        return isHandRenderedMain && isHandRenderedOff;
    }

    private static boolean isTranslucentBlock(ItemStack stack) {
        if (stack == null) {
            return false;
        } else {
            Item item = stack.getItem();

            if (item == null) {
                return false;
            } else if (!(item instanceof ItemBlock itemblock)) {
                return false;
            } else {
                Block block = itemblock.getBlock();

                if (block == null) {
                    return false;
                } else {
                    RenderLayer enumworldblocklayer = block.getBlockLayer();
                    return enumworldblocklayer == RenderLayer.TRANSLUCENT;
                }
            }
        }
    }

    public static boolean isSkipRenderHand() {
        return skipRenderHandMain;
    }

    public static boolean isRenderBothHands() {
        return !skipRenderHandMain && !skipRenderHandOff;
    }

    public static void setSkipRenderHands(boolean skipMain, boolean skipOff) {
        skipRenderHandMain = skipMain;
        skipRenderHandOff = skipOff;
    }

    public static void setHandsRendered(boolean handMain, boolean handOff) {
        isHandRenderedMain = handMain;
        isHandRenderedOff = handOff;
    }

    public static boolean isHandRenderedMain() {
        return isHandRenderedMain;
    }

    public static boolean isHandRenderedOff() {
        return isHandRenderedOff;
    }

    public static float getShadowRenderDistance() {
        return shadowDistanceRenderMul < 0.0F ? -1.0F : shadowMapHalfPlane * shadowDistanceRenderMul;
    }

    public static boolean isRenderingFirstPersonHand() {
        return isRenderingFirstPersonHand;
    }

    public static void setRenderingFirstPersonHand(boolean flag) {
        isRenderingFirstPersonHand = flag;
    }

    public static void beginBeacon() {
        if (isRenderingWorld) {
            useProgram(PROGRAM_BEACON_BEAM);
        }
    }

    public static void endBeacon() {
        if (isRenderingWorld) {
            useProgram(PROGRAM_BLOCK);
        }
    }

    public static World getCurrentWorld() {
        return currentWorld;
    }

    public static BlockPos getCameraPosition() {
        return new BlockPos(cameraPositionX, cameraPositionY, cameraPositionZ);
    }

    public static boolean isCustomUniforms() {
        return customUniforms != null;
    }

    public static boolean canRenderQuads() {
        return !hasGeometryShaders || capabilities.GL_NV_geometry_shader4;
    }
}
