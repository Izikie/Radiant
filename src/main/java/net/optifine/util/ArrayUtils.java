package net.optifine.util;

import java.util.Objects;

public class ArrayUtils {
	public static boolean contains(Object[] arr, Object val) {
		if (arr != null) {
			for (Object object : arr) {
				if (object == val) {
					return true;
				}
			}

		}
		return false;
	}

	public static String arrayToString(float[] arr, String separator) {
		if (arr == null) {
			return "";
		} else {
			StringBuilder stringbuffer = new StringBuilder(arr.length * 5);

			for (int i = 0; i < arr.length; ++i) {
				float f = arr[i];

				if (i > 0) {
					stringbuffer.append(separator);
				}

				stringbuffer.append(f);
			}

			return stringbuffer.toString();
		}
	}

	public static String arrayToString(int[] arr, String separator) {
		if (arr == null) {
			return "";
		} else {
			StringBuilder stringbuffer = new StringBuilder(arr.length * 5);

			for (int i = 0; i < arr.length; ++i) {
				int j = arr[i];

				if (i > 0) {
					stringbuffer.append(separator);
				}

				stringbuffer.append(j);
			}

			return stringbuffer.toString();
		}
	}

	public static String arrayToString(Object[] arr) {
		return arrayToString(arr, ", ");
	}

	public static String arrayToString(Object[] arr, String separator) {
		if (arr == null) {
			return "";
		} else {
			StringBuilder stringbuffer = new StringBuilder(arr.length * 5);

			for (int i = 0; i < arr.length; ++i) {
				Object object = arr[i];

				if (i > 0) {
					stringbuffer.append(separator);
				}

				stringbuffer.append(object);
			}

			return stringbuffer.toString();
		}
	}

	public static boolean equals(Object o1, Object o2) {
		return Objects.equals(o1, o2);
	}

}
