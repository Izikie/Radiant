package net.optifine.reflect;

import net.optifine.Log;

public class ReflectorClass {
    private final String targetClassName;
    private boolean checked = false;
    private Class targetClass = null;

    public ReflectorClass(String targetClassName) {
        this.targetClassName = targetClassName;
    }

    public ReflectorClass(Class targetClass) {
        this.targetClass = targetClass;
        this.targetClassName = targetClass.getName();
        this.checked = true;
    }

    public Class getTargetClass() {
        if (!this.checked) {
            this.checked = true;

            try {
                this.targetClass = Class.forName(this.targetClassName);
            } catch (ClassNotFoundException var2) {
                Log.log("(Reflector) Class not present: " + this.targetClassName);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

        }
        return this.targetClass;
    }

    public boolean exists() {
        return this.getTargetClass() != null;
    }
}
