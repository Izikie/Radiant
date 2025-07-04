package net.optifine.config;

import net.minecraft.world.World;

public enum Weather {
	CLEAR,
	RAIN,
	THUNDER;

	public static Weather getWeather(World world, float partialTicks) {
		float thunderStrength = world.getThunderStrength(partialTicks);

		if (thunderStrength > 0.5F) {
			return THUNDER;
		} else {
			return world.getRainStrength(partialTicks) > 0.5F ? RAIN : CLEAR;
		}
	}
}
