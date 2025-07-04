package net.optifine.util;

import net.minecraft.util.MathHelper;

public class MathUtils {
	public static final float PI = (float) Math.PI;
	private static final float[] ASIN_TABLE = new float[65536];

	static {
		for (int i = 0; i < 65536; ++i) {
			ASIN_TABLE[i] = (float) Math.asin(i / 32767.5D - 1.0D);
		}

		for (int j = -1; j < 2; ++j) {
			ASIN_TABLE[(int) ((j + 1.0D) * 32767.5D) & 65535] = (float) Math.asin(j);
		}
	}

	public static float asin(float value) {
		return ASIN_TABLE[(int) ((value + 1.0F) * 32767.5D) & 65535];
	}

	public static float acos(float value) {
		return ((float) Math.PI / 2.0F) - ASIN_TABLE[(int) ((value + 1.0F) * 32767.5D) & 65535];
	}

	public static int getAverage(int[] vals) {
		if (vals.length == 0) {
			return 0;
		} else {
			int i = getSum(vals);
			return i / vals.length;
		}
	}

	public static int getSum(int[] vals) {
		if (vals.length == 0) {
			return 0;
		} else {
			int i = 0;

			for (int k : vals) {
				i += k;
			}

			return i;
		}
	}

	public static float toDeg(float angle) {
		return angle * 180.0F / MathHelper.PI;
	}

	public static float toRad(float angle) {
		return angle / 180.0F * MathHelper.PI;
	}

	public static float roundToFloat(double d) {
		return (float) (Math.round(d * 1.0E8D) / 1.0E8D);
	}
}
