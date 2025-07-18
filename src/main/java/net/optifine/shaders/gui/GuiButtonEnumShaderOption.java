package net.optifine.shaders.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.config.ShaderOptions;

public class GuiButtonEnumShaderOption extends GuiButton {
	private final ShaderOptions enumShaderOption;

	public GuiButtonEnumShaderOption(ShaderOptions enumShaderOption, int x, int y, int widthIn, int heightIn) {
		super(enumShaderOption.ordinal(), x, y, widthIn, heightIn, getButtonText(enumShaderOption));
		this.enumShaderOption = enumShaderOption;
	}

	private static String getButtonText(ShaderOptions eso) {
		String s = I18n.format(eso.getResourceKey()) + ": ";

		return switch (eso) {
			case ANTIALIASING -> s + GuiShaders.toStringAa(Shaders.configAntialiasingLevel);
			case NORMAL_MAP -> s + GuiShaders.toStringOnOff(Shaders.configNormalMap);
			case SPECULAR_MAP -> s + GuiShaders.toStringOnOff(Shaders.configSpecularMap);
			case RENDER_RES_MUL -> s + GuiShaders.toStringQuality(Shaders.configRenderResMul);
			case SHADOW_RES_MUL -> s + GuiShaders.toStringQuality(Shaders.configShadowResMul);
			case HAND_DEPTH_MUL -> s + GuiShaders.toStringHandDepth(Shaders.configHandDepthMul);
			case CLOUD_SHADOW -> s + GuiShaders.toStringOnOff(Shaders.configCloudShadow);
			case OLD_HAND_LIGHT -> s + Shaders.CONFIG_OLD_HAND_LIGHT.getUserValue();
			case OLD_LIGHTING -> s + Shaders.CONFIG_OLD_LIGHTING.getUserValue();
			case SHADOW_CLIP_FRUSTRUM -> s + GuiShaders.toStringOnOff(Shaders.configShadowClipFrustrum);
			case TWEAK_BLOCK_DAMAGE -> s + GuiShaders.toStringOnOff(Shaders.configTweakBlockDamage);
			default -> s + Shaders.getEnumShaderOption(eso);
		};
	}

	public ShaderOptions getEnumShaderOption() {
		return this.enumShaderOption;
	}

	public void updateButtonText() {
		this.displayString = getButtonText(this.enumShaderOption);
	}
}
