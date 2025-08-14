package net.minecraft.client.renderer.block.model;

import com.google.gson.*;
import net.minecraft.util.Direction;
import net.minecraft.util.json.JsonUtils;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class BlockPart {
    public final Vector3f positionFrom;
    public final Vector3f positionTo;
    public final Map<Direction, BlockPartFace> mapFaces;
    public final BlockPartRotation partRotation;
    public final boolean shade;

    public BlockPart(Vector3f positionFromIn, Vector3f positionToIn, Map<Direction, BlockPartFace> mapFacesIn, BlockPartRotation partRotationIn, boolean shadeIn) {
        this.positionFrom = positionFromIn;
        this.positionTo = positionToIn;
        this.mapFaces = mapFacesIn;
        this.partRotation = partRotationIn;
        this.shade = shadeIn;
        this.setDefaultUvs();
    }

    private void setDefaultUvs() {
        for (Entry<Direction, BlockPartFace> entry : this.mapFaces.entrySet()) {
            float[] afloat = this.getFaceUvs(entry.getKey());
            entry.getValue().blockFaceUV.setUvs(afloat);
        }
    }

    private float[] getFaceUvs(Direction p_178236_1_) {

        return switch (p_178236_1_) {
            case DOWN, UP ->
                    new float[]{this.positionFrom.x, this.positionFrom.z, this.positionTo.x, this.positionTo.z};
            case NORTH, SOUTH ->
                    new float[]{this.positionFrom.x, 16.0F - this.positionTo.y, this.positionTo.x, 16.0F - this.positionFrom.y};
            case WEST, EAST ->
                    new float[]{this.positionFrom.z, 16.0F - this.positionTo.y, this.positionTo.z, 16.0F - this.positionFrom.y};
        };
    }

    static class Deserializer implements JsonDeserializer<BlockPart> {
        public BlockPart deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject jsonobject = jsonElement.getAsJsonObject();
            Vector3f vector3f = this.parsePositionFrom(jsonobject);
            Vector3f vector3f1 = this.parsePositionTo(jsonobject);
            BlockPartRotation blockpartrotation = this.parseRotation(jsonobject);
            Map<Direction, BlockPartFace> map = this.parseFacesCheck(ctx, jsonobject);

            if (jsonobject.has("shade") && !JsonUtils.isBoolean(jsonobject, "shade")) {
                throw new JsonParseException("Expected shade to be a Boolean");
            } else {
                boolean flag = JsonUtils.getBoolean(jsonobject, "shade", true);
                return new BlockPart(vector3f, vector3f1, map, blockpartrotation, flag);
            }
        }

        private BlockPartRotation parseRotation(JsonObject modelJson) {
            if (!modelJson.has("rotation")) {
                return null;
            }

            JsonObject rotationJson = JsonUtils.getJsonObject(modelJson, "rotation");
            Direction.Axis axis = this.parseAxis(rotationJson);
            float angle = this.parseAngle(rotationJson);
            boolean rescale = JsonUtils.getBoolean(rotationJson, "rescale", false);

            return new BlockPartRotation(
                    this.parsePosition(rotationJson, "origin").mul(0.0625F),
                    axis,
                    angle,
                    rescale
            );
        }

        private float parseAngle(JsonObject jsonObject) {
            float f = JsonUtils.getFloat(jsonObject, "angle");

            if (f != 0.0F && MathHelper.abs(f) != 22.5F && MathHelper.abs(f) != 45.0F) {
                throw new JsonParseException("Invalid rotation " + f + " found, only -45/-22.5/0/22.5/45 allowed");
            } else {
                return f;
            }
        }

        private Direction.Axis parseAxis(JsonObject jsonObject) {
            String s = JsonUtils.getString(jsonObject, "axis");
            Direction.Axis enumfacing$axis = Direction.Axis.byName(s.toLowerCase());

            if (enumfacing$axis == null) {
                throw new JsonParseException("Invalid rotation axis: " + s);
            } else {
                return enumfacing$axis;
            }
        }

        private Map<Direction, BlockPartFace> parseFacesCheck(JsonDeserializationContext ctx, JsonObject jsonObject) {
            Map<Direction, BlockPartFace> map = this.parseFaces(ctx, jsonObject);

            if (map.isEmpty()) {
                throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
            } else {
                return map;
            }
        }

        private Map<Direction, BlockPartFace> parseFaces(JsonDeserializationContext ctx, JsonObject jsonObject) {
            Map<Direction, BlockPartFace> map = new EnumMap<>(Direction.class);
            JsonObject jsonobject = JsonUtils.getJsonObject(jsonObject, "faces");

            for (Entry<String, JsonElement> entry : jsonobject.entrySet()) {
                Direction enumfacing = this.parseEnumFacing(entry.getKey());
                map.put(enumfacing, ctx.deserialize(entry.getValue(), BlockPartFace.class));
            }

            return map;
        }

        private Direction parseEnumFacing(String name) {
            Direction enumfacing = Direction.byName(name);

            if (enumfacing == null) {
                throw new JsonParseException("Unknown facing: " + name);
            } else {
                return enumfacing;
            }
        }

        private Vector3f parsePositionTo(JsonObject jsonObject) {
            Vector3f vector3f = this.parsePosition(jsonObject, "to");

            if (vector3f.x >= -16.0F && vector3f.y >= -16.0F && vector3f.z >= -16.0F && vector3f.x <= 32.0F && vector3f.y <= 32.0F && vector3f.z <= 32.0F) {
                return vector3f;
            } else {
                throw new JsonParseException("'to' specifier exceeds the allowed boundaries: " + vector3f);
            }
        }

        private Vector3f parsePositionFrom(JsonObject jsonObject) {
            Vector3f vector3f = this.parsePosition(jsonObject, "from");

            if (vector3f.x >= -16.0F && vector3f.y >= -16.0F && vector3f.z >= -16.0F && vector3f.x <= 32.0F && vector3f.y <= 32.0F && vector3f.z <= 32.0F) {
                return vector3f;
            } else {
                throw new JsonParseException("'from' specifier exceeds the allowed boundaries: " + vector3f);
            }
        }

        private Vector3f parsePosition(JsonObject jsonObject, String p_178251_2_) {
            JsonArray jsonarray = JsonUtils.getJsonArray(jsonObject, p_178251_2_);

            if (jsonarray.size() != 3) {
                throw new JsonParseException("Expected 3 " + p_178251_2_ + " values, found: " + jsonarray.size());
            } else {
                float x = JsonUtils.getFloat(jsonarray.get(0), p_178251_2_ + "[0]");
                float y = JsonUtils.getFloat(jsonarray.get(1), p_178251_2_ + "[1]");
                float z = JsonUtils.getFloat(jsonarray.get(2), p_178251_2_ + "[2]");
                return new Vector3f(x, y, z);
            }
        }
    }
}
