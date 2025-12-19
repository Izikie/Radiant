package net.optifine.model;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.optifine.Config;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class BlockModelUtils {
    private static final float VERTEX_COORD_ACCURACY = 1.0E-6F;

    public static IBakedModel makeModelCube(TextureAtlasSprite sprite, int tintIndex) {
        List<BakedQuad> generalQuads = new ArrayList<>();
        List<List<BakedQuad>> faceQuads = new ArrayList<>();

        for (Direction direction : Direction.VALUES) {
            List<BakedQuad> list2 = new ArrayList<>();
            list2.add(makeBakedQuad(direction, sprite, tintIndex));
            faceQuads.add(list2);
        }

        return new SimpleBakedModel(generalQuads, faceQuads, true, true, sprite, ItemCameraTransforms.DEFAULT);
    }

    public static IBakedModel joinModelsCube(IBakedModel modelBase, IBakedModel modelAdd) {
        List<BakedQuad> list = new ArrayList<>();
        list.addAll(modelBase.getGeneralQuads());
        list.addAll(modelAdd.getGeneralQuads());
        List<List<BakedQuad>> list1 = new ArrayList<>();

        for (Direction direction : Direction.VALUES) {
            List<BakedQuad> quads = new ArrayList<>();
            quads.addAll(modelBase.getFaceQuads(direction));
            quads.addAll(modelAdd.getFaceQuads(direction));
            list1.add(quads);
        }

        boolean ambientOcclusion = modelBase.isAmbientOcclusion();
        boolean builtInRenderer = modelBase.isBuiltInRenderer();
        TextureAtlasSprite atlasSprite = modelBase.getParticleTexture();
        ItemCameraTransforms cameraTransforms = modelBase.getItemCameraTransforms();
        return new SimpleBakedModel(list, list1, ambientOcclusion, builtInRenderer, atlasSprite, cameraTransforms);
    }

    public static BakedQuad makeBakedQuad(Direction facing, TextureAtlasSprite sprite, int tintIndex) {
        BlockFaceUV blockFaceUV = new BlockFaceUV(new float[]{0.0F, 0.0F, 16.0F, 16.0F}, 0);
        BlockPartFace blockPartFace = new BlockPartFace(facing, tintIndex, "#" + facing.getName(), blockFaceUV);

        return new FaceBakery().makeBakedQuad(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Vector3f(16.0F, 16.0F, 16.0F),
                blockPartFace,
                sprite,
                facing,
                ModelRotation.X0_Y0,
                null,
                false,
                true
        );
    }

    public static IBakedModel makeModel(String modelName, String spriteOldName, String spriteNewName) {
        TextureMap texturemap = Config.getMinecraft().getTextureMapBlocks();
        TextureAtlasSprite oldAtlasSprite = texturemap.getSpriteSafe(spriteOldName);
        TextureAtlasSprite newAtlasSprite = texturemap.getSpriteSafe(spriteNewName);
        return makeModel(modelName, oldAtlasSprite, newAtlasSprite);
    }

    public static IBakedModel makeModel(String modelName, TextureAtlasSprite spriteOld, TextureAtlasSprite spriteNew) {
        if (spriteOld != null && spriteNew != null) {
            ModelManager modelmanager = Config.getModelManager();

            if (modelmanager == null) {
                return null;
            } else {
                ModelResourceLocation modelresourcelocation = new ModelResourceLocation(modelName, "normal");
                IBakedModel ibakedmodel = modelmanager.getModel(modelresourcelocation);

                if (ibakedmodel != null && ibakedmodel != modelmanager.getMissingModel()) {
                    IBakedModel ibakedmodel1 = ModelUtils.duplicateModel(ibakedmodel);

                    for (Direction direction : Direction.VALUES) {
                        List<BakedQuad> list = ibakedmodel1.getFaceQuads(direction);
                        replaceTexture(list, spriteOld, spriteNew);
                    }

                    List<BakedQuad> list1 = ibakedmodel1.getGeneralQuads();
                    replaceTexture(list1, spriteOld, spriteNew);
                    return ibakedmodel1;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private static void replaceTexture(List<BakedQuad> quads, TextureAtlasSprite spriteOld, TextureAtlasSprite spriteNew) {
        List<BakedQuad> list = new ArrayList<>();

        for (BakedQuad bakedquad : quads) {
            if (bakedquad.getSprite() == spriteOld) {
                bakedquad = new BreakingFour(bakedquad, spriteNew);
            }

            list.add(bakedquad);
        }

        quads.clear();
        quads.addAll(list);
    }

    public static void snapVertexPosition(Vector3f pos) {
        pos.set(snapVertexCoord(pos.x), snapVertexCoord(pos.y), snapVertexCoord(pos.z));
    }

    private static float snapVertexCoord(float cord) {
        return cord > -1.0E-6F && cord < 1.0E-6F ? 0.0F : (cord > 0.999999F && cord < 1.000001F ? 1.0F : cord);
    }

    public static AxisAlignedBB getOffsetBoundingBox(AxisAlignedBB aabb, Block.EnumOffsetType offsetType, BlockPos pos) {
        int i = pos.getX();
        int j = pos.getZ();
        long k = (i * 3129871L) ^ j * 116129781L;
        k = k * k * 42317861L + k * 11L;
        double d0 = (((k >> 16 & 15L) / 15.0F) - 0.5D) * 0.5D;
        double d1 = (((k >> 24 & 15L) / 15.0F) - 0.5D) * 0.5D;
        double d2 = 0.0D;

        if (offsetType == Block.EnumOffsetType.XYZ) {
            d2 = (((k >> 20 & 15L) / 15.0F) - 1.0D) * 0.2D;
        }

        return aabb.offset(d0, d2, d1);
    }
}
