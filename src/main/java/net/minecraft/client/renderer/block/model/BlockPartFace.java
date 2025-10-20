package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.util.Direction;
import net.minecraft.util.json.JsonUtils;

import java.lang.reflect.Type;

public class BlockPartFace {
    public static final Direction FACING_DEFAULT = null;
    public final Direction cullFace;
    public final int tintIndex;
    public final String texture;
    public final BlockFaceUV blockFaceUV;

    public BlockPartFace(Direction cullFaceIn, int tintIndexIn, String textureIn, BlockFaceUV blockFaceUVIn) {
        this.cullFace = cullFaceIn;
        this.tintIndex = tintIndexIn;
        this.texture = textureIn;
        this.blockFaceUV = blockFaceUVIn;
    }

    static class Deserializer implements JsonDeserializer<BlockPartFace> {
        @Override
        public BlockPartFace deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonobject = jsonElement.getAsJsonObject();
            Direction direction = this.parseCullFace(jsonobject);
            int i = this.parseTintIndex(jsonobject);
            String s = this.parseTexture(jsonobject);
            BlockFaceUV blockfaceuv = ctx.deserialize(jsonobject, BlockFaceUV.class);
            return new BlockPartFace(direction, i, s, blockfaceuv);
        }

        protected int parseTintIndex(JsonObject jsonObject) {
            return JsonUtils.getInt(jsonObject, "tintindex", -1);
        }

        private String parseTexture(JsonObject jsonObject) {
            return JsonUtils.getString(jsonObject, "texture");
        }

        private Direction parseCullFace(JsonObject jsonObject) {
            String s = JsonUtils.getString(jsonObject, "cullface", "");
            return Direction.byName(s);
        }
    }
}
