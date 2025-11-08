package net.minecraft.util.math;

public class Vec4b {
    private final byte x;
    private final byte y;
    private final byte z;
    private final byte w;

    public Vec4b(byte x, byte y, byte z, byte w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vec4b(Vec4b vec4b) {
        this.x = vec4b.x;
        this.y = vec4b.y;
        this.z = vec4b.z;
        this.w = vec4b.w;
    }

    public byte x() {
        return this.x;
    }

    public byte y() {
        return this.y;
    }

    public byte z() {
        return this.z;
    }

    public byte w() {
        return this.w;
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Vec4b vec4b)) {
            return false;
        } else {
            return this.x == vec4b.x && (this.w == vec4b.w && (this.y == vec4b.y && this.z == vec4b.z));
        }
    }

    public int hashCode() {
        int i = this.x;
        i = 31 * i + this.y;
        i = 31 * i + this.z;
        i = 31 * i + this.w;
        return i;
    }
}
