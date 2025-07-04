package net.optifine.reflect;

import net.optifine.Log;
import net.optifine.util.ArrayUtils;

import java.lang.reflect.Constructor;

public class ReflectorConstructor {
	private final ReflectorClass reflectorClass;
	private final Class<?>[] parameterTypes;
	private boolean checked = false;
	private Constructor<?> targetConstructor = null;

	public ReflectorConstructor(ReflectorClass reflectorClass, Class<?>[] parameterTypes) {
		this.reflectorClass = reflectorClass;
		this.parameterTypes = parameterTypes;
	}

	private static Constructor<?> findConstructor(Class<?> cls, Class<?>[] paramTypes) {
		Constructor<?>[] aconstructor = cls.getDeclaredConstructors();

		for (Constructor<?> constructor : aconstructor) {
			Class<?>[] aclass = constructor.getParameterTypes();

			if (Reflector.matchesTypes(paramTypes, aclass)) {
				return constructor;
			}
		}

		return null;
	}

	public Constructor<?> getTargetConstructor() {
		if (this.checked) {
			return this.targetConstructor;
		} else {
			this.checked = true;
			Class<?> oclass = this.reflectorClass.getTargetClass();

			if (oclass == null) {
				return null;
			} else {
				try {
					this.targetConstructor = findConstructor(oclass, this.parameterTypes);

					if (this.targetConstructor == null) {
						Log.info("(Reflector) Constructor not present: " + oclass.getName() + ", params: " + ArrayUtils.arrayToString(this.parameterTypes));
					}

					if (this.targetConstructor != null) {
						this.targetConstructor.setAccessible(true);
					}
				} catch (Throwable throwable) {
					throwable.printStackTrace();
				}

				return this.targetConstructor;
			}
		}
	}

	public boolean exists() {
		return this.checked ? this.targetConstructor != null : this.getTargetConstructor() != null;
	}

	public void deactivate() {
		this.checked = true;
		this.targetConstructor = null;
	}

	public Object newInstance(Object... params) {
		return Reflector.newInstance(this, params);
	}
}
