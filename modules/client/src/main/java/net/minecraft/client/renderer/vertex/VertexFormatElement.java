package net.minecraft.client.renderer.vertex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record VertexFormatElement(int index, EnumType type, EnumUsage usage, int elementCount) {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertexFormatElement.class);

    public VertexFormatElement(int index, EnumType type, EnumUsage usage, int elementCount) {
        this.index = index;
        this.type = type;
        this.elementCount = elementCount;

        if (!isValidIndexAndUsage(index, usage)) {
            LOGGER.warn("Multiple vertex elements of the same type other than UVs are not supported. Forcing type to UV.");
            this.usage = EnumUsage.UV;
        } else {
            this.usage = usage;
        }
    }

    private static boolean isValidIndexAndUsage(int index, EnumUsage usage) {
        return index == 0 || usage == EnumUsage.UV;
    }

    @Override
    public String toString() {
        return this.elementCount + "," + this.usage.getDisplayName() + "," + this.type.getDisplayName();
    }

    public int getSize() {
        return this.type.getSize() * this.elementCount;
    }

    public boolean isPositionElement() {
        return this.usage == EnumUsage.POSITION;
    }

    public int getIndex() {
        return index();
    }

    public EnumType getType() {
        return type();
    }

    public EnumUsage getUsage() {
        return usage();
    }

    public int getElementCount() {
        return elementCount();
    }

    public enum EnumType {
        FLOAT(4, "Float", 5126),
        UBYTE(1, "Unsigned Byte", 5121),
        BYTE(1, "Byte", 5120),
        USHORT(2, "Unsigned Short", 5123),
        SHORT(2, "Short", 5122),
        UINT(4, "Unsigned Int", 5125),
        INT(4, "Int", 5124);

        private final int size;
        private final String displayName;
        private final int glConstant;

        EnumType(int sizeIn, String displayNameIn, int glConstantIn) {
            this.size = sizeIn;
            this.displayName = displayNameIn;
            this.glConstant = glConstantIn;
        }

        public int getSize() {
            return this.size;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public int getGlConstant() {
            return this.glConstant;
        }
    }

    public enum EnumUsage {
        POSITION("Position"),
        NORMAL("Normal"),
        COLOR("Vertex Color"),
        UV("UV"),
        MATRIX("Bone Matrix"),
        BLEND_WEIGHT("Blend Weight"),
        PADDING("Padding");

        private final String displayName;

        EnumUsage(String displayNameIn) {
            this.displayName = displayNameIn;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }
}
