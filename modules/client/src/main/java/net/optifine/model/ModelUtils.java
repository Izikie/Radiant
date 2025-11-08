package net.optifine.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.util.Direction;

import java.util.ArrayList;
import java.util.List;

public class ModelUtils {
	public static IBakedModel duplicateModel(IBakedModel model) {
		List<BakedQuad> generalQuadsCopy = duplicateQuadList(model.getGeneralQuads());
		List<List<BakedQuad>> faceQuadsCopy = new ArrayList<>();

		for (Direction direction : Direction.VALUES) {
			faceQuadsCopy.add(duplicateQuadList(model.getFaceQuads(direction)));
		}

		return new SimpleBakedModel(generalQuadsCopy, faceQuadsCopy, model.isAmbientOcclusion(), model.isGui3d(), model.getParticleTexture(), model.getItemCameraTransforms());
	}

	public static List<BakedQuad> duplicateQuadList(List<BakedQuad> lists) {
		List<BakedQuad> list = new ArrayList<>();

		for (BakedQuad quad : lists) {
			list.add(duplicateQuad(quad));
		}

		return list;
	}

	public static BakedQuad duplicateQuad(BakedQuad quad) {
		return new BakedQuad(quad.getVertexData().clone(), quad.getTintIndex(), quad.getFace(), quad.getSprite());
	}
}
