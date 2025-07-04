package net.minecraft.util;

import net.optifine.util.MathUtils;

import java.util.Random;
import java.util.UUID;

public class MathHelper {
	public static final float PI = MathUtils.roundToFloat(Math.PI);
	public static final float PId2 = MathUtils.roundToFloat((Math.PI / 2.0D));
	public static final float DEG_2_RAD = MathUtils.roundToFloat(0.017453292519943295D);
	private static final float RAD_TO_INDEX = MathUtils.roundToFloat(651.8986469044033D);
	private static final float[] SIN_TABLE_FAST = new float[4096];
	private static final float[] SIN_TABLE = new float[65536];
	private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION;
	public static boolean fastMath = false;

	static {
		for (int i = 0; i < 65536; ++i) {
			SIN_TABLE[i] = (float) Math.sin(i * Math.PI * 2.0D / 65536.0D);
		}

		for (int j = 0; j < SIN_TABLE_FAST.length; ++j) {
			SIN_TABLE_FAST[j] = MathUtils.roundToFloat(Math.sin(j * Math.PI * 2.0D / 4096.0D));
		}

		MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};
	}

	public static float sin(float angle) {
		return fastMath ? SIN_TABLE_FAST[(int) (angle * RAD_TO_INDEX) & 4095] : SIN_TABLE[(int) (angle * 10430.378F) & 65535];
	}

	public static float cos(float value) {
		return fastMath ? SIN_TABLE_FAST[(int) (value * RAD_TO_INDEX + 1024.0F) & 4095] : SIN_TABLE[(int) (value * 10430.378F + 16384.0F) & 65535];
	}

	public static float sqrt(float value) {
		return (float) Math.sqrt(value);
	}

	public static float sqrt(double value) {
		return (float) Math.sqrt(value);
	}

	public static int floor(float value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	public static int truncateDoubleToInt(double value) {
		return (int) (value + 1024.0D) - 1024;
	}

	public static int floor(double value) {
		int i = (int) value;
		return value < i ? i - 1 : i;
	}

	public static long floor_double_long(double value) {
		long i = (long) value;
		return value < i ? i - 1L : i;
	}

	public static float abs(float value) {
		return Math.abs(value);
	}

	public static int abs(int value) {
		return Math.abs(value);
	}

	public static int ceil(float value) {
		int i = (int) value;
		return value > i ? i + 1 : i;
	}

	public static int ceil(double value) {
		int i = (int) value;
		return value > i ? i + 1 : i;
	}

	public static int clamp(int num, int min, int max) {
		return Math.clamp(num, min, max);
	}

	public static float clamp(float num, float min, float max) {
		return Math.clamp(num, min, max);
	}

	public static double clamp(double num, double min, double max) {
		return Math.clamp(num, min, max);
	}

	public static double denormalizeClamp(double lowerBnd, double upperBnd, double slide) {
		return slide < 0.0D ? lowerBnd : (slide > 1.0D ? upperBnd : lowerBnd + (upperBnd - lowerBnd) * slide);
	}

	public static double absMax(double a, double b) {
		return Math.max(Math.abs(a), Math.abs(b));
	}

	public static int bucketInt(int value, int bucketSize) {
		return value < 0 ? -((-value - 1) / bucketSize) - 1 : value / bucketSize;
	}

	public static int getRandomIntegerInRange(Random random, int min, int max) {
		return clamp(random.nextInt(max - min + 1), min, max);
	}

	public static float randomFloatClamp(Random random, float min, float max) {
		return clamp(random.nextFloat() * (max - min) + min, min, max);
	}

	public static double getRandomDoubleInRange(Random random, double min, double max) {
		return clamp(random.nextDouble() * (max - min) + min, min, max);
	}

	public static double average(long[] values) {
		long i = 0L;

		for (long j : values) i += j;

		return (double) i / values.length;
	}

	public static boolean epsilonEquals(float a, float b) {
		return abs(b - a) < 1.0E-5F;
	}

	public static int normalizeAngle(int value, int modulus) {
		return (value % modulus + modulus) % modulus;
	}

	public static float wrapAngle(float value) {
		value = value % 360.0F;

		if (value >= 180.0F) value -= 360.0F;
		if (value < -180.0F) value += 360.0F;

		return value;
	}

	public static double wrapAngle(double value) {
		value = value % 360.0D;

		if (value >= 180.0D) value -= 360.0D;
		if (value < -180.0D) value += 360.0D;

		return value;
	}

	public static int parseIntWithDefault(String value, int defaultValue) {
		try {
			return Integer.parseInt(value);
		} catch (Throwable throwable) {
			return defaultValue;
		}
	}

	public static int parseIntWithDefaultAndMax(String value, int defaultValue, int min) {
		return Math.max(min, parseIntWithDefault(value, defaultValue));
	}

	public static double parseDoubleWithDefault(String value, double defaultValue) {
		try {
			return Double.parseDouble(value);
		} catch (Throwable throwable) {
			return defaultValue;
		}
	}

	public static double parseDoubleWithDefaultAndMax(String value, double defaultValue, double min) {
		return Math.max(min, parseDoubleWithDefault(value, defaultValue));
	}

	public static int roundUpToPowerOfTwo(int value) {
		int i = value - 1;
		i = i | i >> 1;
		i = i | i >> 2;
		i = i | i >> 4;
		i = i | i >> 8;
		i = i | i >> 16;
		return i + 1;
	}

	private static boolean isPowerOfTwo(int value) {
		return value != 0 && (value & value - 1) == 0;
	}

	private static int calculateLogBaseTwoDeBruijn(int value) {
		value = isPowerOfTwo(value) ? value : roundUpToPowerOfTwo(value);
		return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) (value * 125613361L >> 27) & 31];
	}

	public static int calculateLogBaseTwo(int value) {
		return calculateLogBaseTwoDeBruijn(value) - (isPowerOfTwo(value) ? 0 : 1);
	}

	public static int roundUp(int number, int multiple) {
		if (multiple == 0) return 0;
		else if (number == 0) return multiple;
		else {
			if (number < 0) multiple *= -1;

			int i = number % multiple;
			return i == 0 ? number : number + multiple - i;
		}
	}

	public static int rgb(float red, float green, float blue) {
		return packRGB(
				floor(red * 255.0F),
				floor(green * 255.0F),
				floor(blue * 255.0F)
		);
	}

	public static int packRGB(int red, int green, int blue) {
		int i = (red << 8) + green;
		i = (i << 8) + blue;
		return i;
	}

	public static int mulColor(int colorA, int colorB) {
		int i = (colorA & 16711680) >> 16;
		int j = (colorB & 16711680) >> 16;
		int k = (colorA & 65280) >> 8;
		int l = (colorB & 65280) >> 8;
		int i1 = (colorA & 255);
		int j1 = (colorB & 255);
		int k1 = (int) (i * j / 255.0F);
		int l1 = (int) (k * l / 255.0F);
		int i2 = (int) (i1 * j1 / 255.0F);
		return colorA & -16777216 | k1 << 16 | l1 << 8 | i2;
	}

	public static double frac(double value) {
		return value - Math.floor(value);
	}

	public static long getPositionRandom(Vec3i pos) {
		return getCoordinateRandom(pos.getX(), pos.getY(), pos.getZ());
	}

	public static long getCoordinateRandom(int x, int y, int z) {
		long i = (x * 3129871L) ^ z * 116129781L ^ y;
		i = i * i * 42317861L + i * 11L;
		return i;
	}

	public static UUID getRandomUuid(Random rand) {
		long i = rand.nextLong() & -61441L | 16384L;
		long j = rand.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
		return new UUID(i, j);
	}

	public static double frac(double value, double min, double max) {
		return (value - min) / (max - min);
	}

	public static double atan2(double y, double x) {
		return Math.atan2(y, x);
	}

	public static int hsvToRGB(float p_181758_0_, float p_181758_1_, float p_181758_2_) {
		int i = (int) (p_181758_0_ * 6.0F) % 6;
		float f = p_181758_0_ * 6.0F - i;
		float f1 = p_181758_2_ * (1.0F - p_181758_1_);
		float f2 = p_181758_2_ * (1.0F - f * p_181758_1_);
		float f3 = p_181758_2_ * (1.0F - (1.0F - f) * p_181758_1_);
		float f4;
		float f5;
		float f6;

		switch (i) {
			case 0 -> {
				f4 = p_181758_2_;
				f5 = f3;
				f6 = f1;
			}
			case 1 -> {
				f4 = f2;
				f5 = p_181758_2_;
				f6 = f1;
			}
			case 2 -> {
				f4 = f1;
				f5 = p_181758_2_;
				f6 = f3;
			}
			case 3 -> {
				f4 = f1;
				f5 = f2;
				f6 = p_181758_2_;
			}
			case 4 -> {
				f4 = f3;
				f5 = f1;
				f6 = p_181758_2_;
			}
			case 5 -> {
				f4 = p_181758_2_;
				f5 = f1;
				f6 = f2;
			}
			default ->
					throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + p_181758_0_ + ", " + p_181758_1_ + ", " + p_181758_2_);
		}

		int j = clamp((int) (f4 * 255.0F), 0, 255);
		int k = clamp((int) (f5 * 255.0F), 0, 255);
		int l = clamp((int) (f6 * 255.0F), 0, 255);
		return j << 16 | k << 8 | l;
	}
}
