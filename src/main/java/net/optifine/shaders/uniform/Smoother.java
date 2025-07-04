package net.optifine.shaders.uniform;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.optifine.util.CounterInt;
import net.optifine.util.SmoothFloat;

public class Smoother {
	private static final Int2ObjectOpenHashMap<SmoothFloat> mapSmoothValues = new Int2ObjectOpenHashMap<>();
	private static final CounterInt COUNTER_IDS = new CounterInt(1);

	public static float getSmoothValue(int id, float value, float timeFadeUpSec, float timeFadeDownSec) {
		synchronized (mapSmoothValues) {
			SmoothFloat smoothfloat = mapSmoothValues.get(id);

			if (smoothfloat == null) {
				smoothfloat = new SmoothFloat(value, timeFadeUpSec, timeFadeDownSec);
				mapSmoothValues.put(id, smoothfloat);
			}

			return smoothfloat.getSmoothValue(value, timeFadeUpSec, timeFadeDownSec);
		}
	}

	public static int getNextId() {
		synchronized (COUNTER_IDS) {
			return COUNTER_IDS.nextValue();
		}
	}

	public static void resetValues() {
		synchronized (mapSmoothValues) {
			mapSmoothValues.clear();
		}
	}
}
