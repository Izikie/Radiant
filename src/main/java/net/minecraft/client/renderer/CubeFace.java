package net.minecraft.client.renderer;

import net.minecraft.util.Direction;

public enum CubeFace {
    DOWN(new CubeFace.VertexInformation[]{new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.SOUTH_INDEX)}),
    UP(new CubeFace.VertexInformation[]{new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.NORTH_INDEX)}),
    NORTH(new CubeFace.VertexInformation[]{new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.NORTH_INDEX)}),
    SOUTH(new CubeFace.VertexInformation[]{new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.SOUTH_INDEX)}),
    WEST(new CubeFace.VertexInformation[]{new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.WEST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.SOUTH_INDEX)}),
    EAST(new CubeFace.VertexInformation[]{new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.SOUTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.DOWN_INDEX, CubeFace.Constants.NORTH_INDEX), new CubeFace.VertexInformation(CubeFace.Constants.EAST_INDEX, CubeFace.Constants.UP_INDEX, CubeFace.Constants.NORTH_INDEX)});

    private static final CubeFace[] CUBE_FACES = new CubeFace[6];
    private final CubeFace.VertexInformation[] vertexInfos;

    public static CubeFace getFacing(Direction facing) {
        return CUBE_FACES[facing.getIndex()];
    }

    CubeFace(CubeFace.VertexInformation[] vertexInfosIn) {
        this.vertexInfos = vertexInfosIn;
    }

    public CubeFace.VertexInformation getVertexInformation(int index) {
        return this.vertexInfos[index];
    }

    static {
        CUBE_FACES[CubeFace.Constants.DOWN_INDEX] = DOWN;
        CUBE_FACES[CubeFace.Constants.UP_INDEX] = UP;
        CUBE_FACES[CubeFace.Constants.NORTH_INDEX] = NORTH;
        CUBE_FACES[CubeFace.Constants.SOUTH_INDEX] = SOUTH;
        CUBE_FACES[CubeFace.Constants.WEST_INDEX] = WEST;
        CUBE_FACES[CubeFace.Constants.EAST_INDEX] = EAST;
    }

    public static final class Constants {
        public static final int SOUTH_INDEX = Direction.SOUTH.getIndex();
        public static final int UP_INDEX = Direction.UP.getIndex();
        public static final int EAST_INDEX = Direction.EAST.getIndex();
        public static final int NORTH_INDEX = Direction.NORTH.getIndex();
        public static final int DOWN_INDEX = Direction.DOWN.getIndex();
        public static final int WEST_INDEX = Direction.WEST.getIndex();
    }

    public static class VertexInformation {
        public final int xIndex;
        public final int yIndex;
        public final int zIndex;

        private VertexInformation(int xIndexIn, int yIndexIn, int zIndexIn) {
            this.xIndex = xIndexIn;
            this.yIndex = yIndexIn;
            this.zIndex = zIndexIn;
        }
    }
}
