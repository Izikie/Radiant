package net.optifine.config;

public record RangeInt(int min, int max) {
    public RangeInt(int min, int max) {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public boolean isInRange(int val) {
        return val >= this.min && val <= this.max;
    }

    public String toString() {
        return "min: " + this.min + ", max: " + this.max;
    }
}
