package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import net.minecraft.util.Direction;
import net.minecraft.util.JsonUtils;

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
        public BlockPartFace deserialize(JsonElement p_deserialize_1_, Type p_deserialize_2_, JsonDeserializationContext p_deserialize_3_) throws JsonParseException {
            JsonObject jsonobject = p_deserialize_1_.getAsJsonObject();
            Direction enumfacing = this.parseCullFace(jsonobject);
            int i = this.parseTintIndex(jsonobject);
            String s = this.parseTexture(jsonobject);
            BlockFaceUV blockfaceuv = p_deserialize_3_.deserialize(jsonobject, BlockFaceUV.class);
            return new BlockPartFace(enumfacing, i, s, blockfaceuv);
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
