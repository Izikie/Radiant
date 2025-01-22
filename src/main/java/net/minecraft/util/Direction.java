package net.minecraft.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public enum Direction implements IStringSerializable {
    DOWN(0, 1, -1, "Down", Direction.AxisDirection.NEGATIVE, Direction.Axis.Y, new Vec3i(0, -1, 0)),
    UP(1, 0, -1, "Up", Direction.AxisDirection.POSITIVE, Direction.Axis.Y, new Vec3i(0, 1, 0)),
    NORTH(2, 3, 2, "North", Direction.AxisDirection.NEGATIVE, Direction.Axis.Z, new Vec3i(0, 0, -1)),
    SOUTH(3, 2, 0, "South", Direction.AxisDirection.POSITIVE, Direction.Axis.Z, new Vec3i(0, 0, 1)),
    WEST(4, 5, 1, "West", Direction.AxisDirection.NEGATIVE, Direction.Axis.X, new Vec3i(-1, 0, 0)),
    EAST(5, 4, 3, "East", Direction.AxisDirection.POSITIVE, Direction.Axis.X, new Vec3i(1, 0, 0));

    private final int index;
    private final int opposite;
    private final int horizontalIndex;
    private final String name;
    private final Direction.Axis axis;
    private final Direction.AxisDirection axisDirection;
    private final Vec3i directionVec;
    public static final Direction[] VALUES = new Direction[6];
    private static final Direction[] HORIZONTALS = new Direction[4];
    private static final Map<String, Direction> NAME_LOOKUP = Maps.newHashMap();

    Direction(int indexIn, int oppositeIn, int horizontalIndexIn, String nameIn, Direction.AxisDirection axisDirectionIn, Direction.Axis axisIn, Vec3i directionVecIn) {
        this.index = indexIn;
        this.horizontalIndex = horizontalIndexIn;
        this.opposite = oppositeIn;
        this.name = nameIn;
        this.axis = axisIn;
        this.axisDirection = axisDirectionIn;
        this.directionVec = directionVecIn;
    }

    public int getIndex() {
        return this.index;
    }

    public int getHorizontalIndex() {
        return this.horizontalIndex;
    }

    public Direction.AxisDirection getAxisDirection() {
        return this.axisDirection;
    }

    public Direction getOpposite() {
        return VALUES[this.opposite];
    }

    public Direction rotateAround(Direction.Axis axis) {
        return switch (axis) {
            case X -> {
                if (this != WEST && this != EAST) {
                    yield this.rotateX();
                }

                yield this;
            }
            case Y -> {
                if (this != UP && this != DOWN) {
                    yield this.rotateY();
                }

                yield this;
            }
            case Z -> {
                if (this != NORTH && this != SOUTH) {
                    yield this.rotateZ();
                }

                yield this;
            }
            default -> throw new IllegalStateException("Unable to get CW facing for axis " + axis);
        };
    }

    public Direction rotateY() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
        };
    }

    private Direction rotateX() {
        return switch (this) {
            case NORTH -> DOWN;
            case SOUTH -> UP;
            case UP -> NORTH;
            case DOWN -> SOUTH;
            default -> throw new IllegalStateException("Unable to get X-rotated facing of " + this);
        };
    }

    private Direction rotateZ() {
        return switch (this) {
            case EAST -> DOWN;
            case WEST -> UP;
            case UP -> EAST;
            case DOWN -> WEST;
            default -> throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
        };
    }

    public Direction rotateYCCW() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            default -> throw new IllegalStateException("Unable to get CCW facing of " + this);
        };
    }

    public int getFrontOffsetX() {
        return this.axis == Direction.Axis.X ? this.axisDirection.getOffset() : 0;
    }

    public int getFrontOffsetY() {
        return this.axis == Direction.Axis.Y ? this.axisDirection.getOffset() : 0;
    }

    public int getFrontOffsetZ() {
        return this.axis == Direction.Axis.Z ? this.axisDirection.getOffset() : 0;
    }

    public String getName2() {
        return this.name;
    }

    public Direction.Axis getAxis() {
        return this.axis;
    }

    public static Direction byName(String name) {
        return name == null ? null : NAME_LOOKUP.get(name.toLowerCase());
    }

    public static Direction getFront(int index) {
        return VALUES[MathHelper.abs_int(index % VALUES.length)];
    }

    public static Direction getHorizontal(int p_176731_0_) {
        return HORIZONTALS[MathHelper.abs_int(p_176731_0_ % HORIZONTALS.length)];
    }

    public static Direction fromAngle(double angle) {
        return getHorizontal(MathHelper.floor_double(angle / 90.0D + 0.5D) & 3);
    }

    public static Direction random(Random rand) {
        return values()[rand.nextInt(values().length)];
    }

    public static Direction getFacingFromVector(float p_176737_0_, float p_176737_1_, float p_176737_2_) {
        Direction enumfacing = NORTH;
        float f = Float.MIN_VALUE;

        for (Direction enumfacing1 : values()) {
            float f1 = p_176737_0_ * enumfacing1.directionVec.getX() + p_176737_1_ * enumfacing1.directionVec.getY() + p_176737_2_ * enumfacing1.directionVec.getZ();

            if (f1 > f) {
                f = f1;
                enumfacing = enumfacing1;
            }
        }

        return enumfacing;
    }

    public String toString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    public static Direction getFacingFromAxis(Direction.AxisDirection p_181076_0_, Direction.Axis p_181076_1_) {
        for (Direction enumfacing : values()) {
            if (enumfacing.getAxisDirection() == p_181076_0_ && enumfacing.getAxis() == p_181076_1_) {
                return enumfacing;
            }
        }

        throw new IllegalArgumentException("No such direction: " + p_181076_0_ + " " + p_181076_1_);
    }

    public Vec3i getDirectionVec() {
        return this.directionVec;
    }

    static {
        for (Direction enumfacing : values()) {
            VALUES[enumfacing.index] = enumfacing;

            if (enumfacing.getAxis().isHorizontal()) {
                HORIZONTALS[enumfacing.horizontalIndex] = enumfacing;
            }

            NAME_LOOKUP.put(enumfacing.getName2().toLowerCase(), enumfacing);
        }
    }

    public enum Axis implements Predicate<Direction>, IStringSerializable {
        X("x", Direction.Plane.HORIZONTAL),
        Y("y", Direction.Plane.VERTICAL),
        Z("z", Direction.Plane.HORIZONTAL);

        private static final Map<String, Direction.Axis> NAME_LOOKUP = Maps.newHashMap();
        private final String name;
        private final Direction.Plane plane;

        Axis(String name, Direction.Plane plane) {
            this.name = name;
            this.plane = plane;
        }

        public static Direction.Axis byName(String name) {
            return name == null ? null : NAME_LOOKUP.get(name.toLowerCase());
        }

        public String getName2() {
            return this.name;
        }

        public boolean isVertical() {
            return this.plane == Direction.Plane.VERTICAL;
        }

        public boolean isHorizontal() {
            return this.plane == Direction.Plane.HORIZONTAL;
        }

        public String toString() {
            return this.name;
        }

        public boolean apply(Direction p_apply_1_) {
            return p_apply_1_ != null && p_apply_1_.getAxis() == this;
        }

        public Direction.Plane getPlane() {
            return this.plane;
        }

        public String getName() {
            return this.name;
        }

        static {
            for (Direction.Axis enumfacing$axis : values()) {
                NAME_LOOKUP.put(enumfacing$axis.getName2().toLowerCase(), enumfacing$axis);
            }
        }
    }

    public enum AxisDirection {
        POSITIVE(1, "Towards positive"),
        NEGATIVE(-1, "Towards negative");

        private final int offset;
        private final String description;

        AxisDirection(int offset, String description) {
            this.offset = offset;
            this.description = description;
        }

        public int getOffset() {
            return this.offset;
        }

        public String toString() {
            return this.description;
        }
    }

    public enum Plane implements Predicate<Direction>, Iterable<Direction> {
        HORIZONTAL,
        VERTICAL;

        public Direction[] facings() {
            return switch (this) {
                case HORIZONTAL ->
                        new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                case VERTICAL -> new Direction[]{Direction.UP, Direction.DOWN};
                default -> throw new Error("Someone's been tampering with the universe!");
            };
        }

        public Direction random(Random rand) {
            Direction[] aenumfacing = this.facings();
            return aenumfacing[rand.nextInt(aenumfacing.length)];
        }

        public boolean apply(Direction p_apply_1_) {
            return p_apply_1_ != null && p_apply_1_.getAxis().getPlane() == this;
        }

        public Iterator<Direction> iterator() {
            return Iterators.forArray(this.facings());
        }
    }
}
