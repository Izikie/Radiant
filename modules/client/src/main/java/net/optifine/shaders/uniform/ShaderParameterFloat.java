package net.optifine.shaders.uniform;

import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.BiomeGenBase;
import net.optifine.Log;
import net.optifine.shaders.Shaders;

public enum ShaderParameterFloat {
    BIOME("biome"),
    TEMPERATURE("temperature"),
    RAINFALL("rainfall"),
    HELD_ITEM_ID(Shaders.UNIFORM_HELD_ITEM_ID),
    HELD_BLOCK_LIGHT_VALUE(Shaders.UNIFORM_HELD_BLOCK_LIGHT_VALUE),
    HELD_ITEM_ID2(Shaders.UNIFORM_HELD_ITEM_ID_2),
    HELD_BLOCK_LIGHT_VALUE2(Shaders.UNIFORM_HELD_BLOCK_LIGHT_VALUE_2),
    WORLD_TIME(Shaders.UNIFORM_WORLD_TIME),
    WORLD_DAY(Shaders.UNIFORM_WORLD_DAY),
    MOON_PHASE(Shaders.UNIFORM_MOON_PHASE),
    FRAME_COUNTER(Shaders.UNIFORM_FRAME_COUNTER),
    FRAME_TIME(Shaders.UNIFORM_FRAME_TIME),
    FRAME_TIME_COUNTER(Shaders.UNIFORM_FRAME_TIME_COUNTER),
    SUN_ANGLE(Shaders.UNIFORM_SUN_ANGLE),
    SHADOW_ANGLE(Shaders.UNIFORM_SHADOW_ANGLE),
    RAIN_STRENGTH(Shaders.UNIFORM_RAIN_STRENGTH),
    ASPECT_RATIO(Shaders.UNIFORM_ASPECT_RATIO),
    VIEW_WIDTH(Shaders.UNIFORM_VIEW_WIDTH),
    VIEW_HEIGHT(Shaders.UNIFORM_VIEW_HEIGHT),
    NEAR(Shaders.UNIFORM_NEAR),
    FAR(Shaders.UNIFORM_FAR),
    WETNESS(Shaders.UNIFORM_WETNESS),
    EYE_ALTITUDE(Shaders.UNIFORM_EYE_ALTITUDE),
    EYE_BRIGHTNESS(Shaders.UNIFORM_EYE_BRIGHTNESS, new String[]{"x", "y"}),
    TERRAIN_TEXTURE_SIZE(Shaders.UNIFORM_TERRAIN_TEXTURE_SIZE, new String[]{"x", "y"}),
    TERRRAIN_ICON_SIZE(Shaders.UNIFORM_TERRAIN_ICON_SIZE),
    IS_EYE_IN_WATER(Shaders.UNIFORM_IS_EYE_IN_WATER),
    NIGHT_VISION(Shaders.UNIFORM_NIGHT_VISION),
    BLINDNESS(Shaders.UNIFORM_BLINDNESS),
    SCREEN_BRIGHTNESS(Shaders.UNIFORM_SCREEN_BRIGHTNESS),
    HIDE_GUI(Shaders.UNIFORM_HIDE_GUI),
    CENTER_DEPT_SMOOTH(Shaders.UNIFORM_CENTER_DEPTH_SMOOTH),
    ATLAS_SIZE(Shaders.UNIFORM_ATLAS_SIZE, new String[]{"x", "y"}),
    CAMERA_POSITION(Shaders.UNIFORM_CAMERA_POSITION, new String[]{"x", "y", "z"}),
    PREVIOUS_CAMERA_POSITION(Shaders.UNIFORM_PREVIOUS_CAMERA_POSITION, new String[]{"x", "y", "z"}),
    SUN_POSITION(Shaders.UNIFORM_SUN_POSITION, new String[]{"x", "y", "z"}),
    MOON_POSITION(Shaders.UNIFORM_MOON_POSITION, new String[]{"x", "y", "z"}),
    SHADOW_LIGHT_POSITION(Shaders.UNIFORM_SHADOW_LIGHT_POSITION, new String[]{"x", "y", "z"}),
    UP_POSITION(Shaders.UNIFORM_UP_POSITION, new String[]{"x", "y", "z"}),
    SKY_COLOR(Shaders.UNIFORM_SKY_COLOR, new String[]{"r", "g", "b"}),
    GBUFFER_PROJECTION(Shaders.UNIFORM_GBUFFER_PROJECTION, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    GBUFFER_PROJECTION_INVERSE(Shaders.UNIFORM_GBUFFER_PROJECTION_INVERSE, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    GBUFFER_PREVIOUS_PROJECTION(Shaders.UNIFORM_GBUFFER_PREVIOUS_PROJECTION, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    GBUFFER_MODEL_VIEW(Shaders.UNIFORM_GBUFFER_MODEL_VIEW, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    GBUFFER_MODEL_VIEW_INVERSE(Shaders.UNIFORM_GBUFFER_MODEL_VIEW_INVERSE, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    GBUFFER_PREVIOUS_MODEL_VIEW(Shaders.UNIFORM_GBUFFER_PREVIOUS_MODEL_VIEW, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    SHADOW_PROJECTION(Shaders.UNIFORM_SHADOW_PROJECTION, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    SHADOW_PROJECTION_INVERSE(Shaders.UNIFORM_SHADOW_PROJECTION_INVERSE, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    SHADOW_MODEL_VIEW(Shaders.UNIFORM_SHADOW_MODEL_VIEW, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"}),
    SHADOW_MODEL_VIEW_INVERSE(Shaders.UNIFORM_SHADOW_MODEL_VIEW_INVERSE, new String[]{"0", "1", "2", "3"}, new String[]{"0", "1", "2", "3"});

    private final String name;
    private ShaderUniformBase uniform;
    private String[] indexNames1;
    private String[] indexNames2;

    ShaderParameterFloat(String name) {
        this.name = name;
    }

    ShaderParameterFloat(ShaderUniformBase uniform) {
        this.name = uniform.getName();
        this.uniform = uniform;

        if (!instanceOf(uniform, ShaderUniform1f.class, ShaderUniform1i.class)) {
            throw new IllegalArgumentException("Invalid uniform type for enum: " + this + ", uniform: " + uniform.getClass().getName());
        }
    }

    ShaderParameterFloat(ShaderUniformBase uniform, String[] indexNames1) {
        this.name = uniform.getName();
        this.uniform = uniform;
        this.indexNames1 = indexNames1;

        if (!instanceOf(uniform, ShaderUniform2i.class, ShaderUniform2f.class, ShaderUniform3f.class, ShaderUniform4f.class)) {
            throw new IllegalArgumentException("Invalid uniform type for enum: " + this + ", uniform: " + uniform.getClass().getName());
        }
    }

    ShaderParameterFloat(ShaderUniformBase uniform, String[] indexNames1, String[] indexNames2) {
        this.name = uniform.getName();
        this.uniform = uniform;
        this.indexNames1 = indexNames1;
        this.indexNames2 = indexNames2;

        if (!instanceOf(uniform, ShaderUniformM4.class)) {
            throw new IllegalArgumentException("Invalid uniform type for enum: " + this + ", uniform: " + uniform.getClass().getName());
        }
    }

    private static boolean instanceOf(ShaderUniformBase obj, Class<? extends ShaderUniformBase>... classes) {
        if (obj != null) {
            Class<? extends Object> objClass = obj.getClass();

            for (Class<? extends ShaderUniformBase> clazz : classes) {
                if (clazz.isAssignableFrom(objClass)) {
                    return true;
                }
            }

        }
        return false;
    }

    public String getName() {
        return this.name;
    }

    public ShaderUniformBase getUniform() {
        return this.uniform;
    }

    public String[] getIndexNames1() {
        return this.indexNames1;
    }

    public String[] getIndexNames2() {
        return this.indexNames2;
    }

    public float eval(int index1, int index2) {
        if (this.indexNames1 == null || index1 >= 0 && index1 <= this.indexNames1.length) {
            if (this.indexNames2 == null || index2 >= 0 && index2 <= this.indexNames2.length) {
                switch (this) {
                    case BIOME:
                        BlockPos blockpos2 = Shaders.getCameraPosition();
                        BiomeGenBase biomegenbase2 = Shaders.getCurrentWorld().getBiomeGenForCoords(blockpos2);
                        return biomegenbase2.biomeID;

                    case TEMPERATURE:
                        BlockPos blockpos1 = Shaders.getCameraPosition();
                        BiomeGenBase biomegenbase1 = Shaders.getCurrentWorld().getBiomeGenForCoords(blockpos1);
                        return biomegenbase1 != null ? biomegenbase1.getFloatTemperature(blockpos1) : 0.0F;

                    case RAINFALL:
                        BlockPos pos = Shaders.getCameraPosition();
                        BiomeGenBase biome = Shaders.getCurrentWorld().getBiomeGenForCoords(pos);
                        return biome != null ? biome.getFloatRainfall() : 0.0F;

                    default:
                        return switch (this.uniform) {
                            case ShaderUniform1f shaderUniform1f -> shaderUniform1f.getValue();
                            case ShaderUniform1i shaderUniform1i -> shaderUniform1i.getValue();
                            case ShaderUniform2i shaderUniform2i -> shaderUniform2i.getValue()[index1];
                            case ShaderUniform2f shaderUniform2f -> shaderUniform2f.getValue()[index1];
                            case ShaderUniform3f shaderUniform3f -> shaderUniform3f.getValue()[index1];
                            case ShaderUniform4f shaderUniform4f -> shaderUniform4f.getValue()[index1];
                            case ShaderUniformM4 shaderUniformM4 -> shaderUniformM4.getValue(index1, index2);
                            case null, default -> throw new IllegalArgumentException("Unknown uniform type: " + this);
                        };
                }
            } else {
                Log.error("Invalid index2, parameter: " + this + ", index: " + index2);
                return 0.0F;
            }
        } else {
            Log.error("Invalid index1, parameter: " + this + ", index: " + index1);
            return 0.0F;
        }
    }
}
