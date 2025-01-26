package net;

public final class FastMath {
    private static final float DEGREES_TO_RADIANS = 0.017453292F;

    public static float toRadians(float angdeg) {
        return angdeg * DEGREES_TO_RADIANS;
    }
}
