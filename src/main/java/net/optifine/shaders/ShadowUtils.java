package net.optifine.shaders;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ShadowUtils {
	public static Iterator<RenderChunk> makeShadowChunkIterator(WorldClient world, double partialTicks, Entity viewEntity, int renderDistanceChunks, ViewFrustum viewFrustum) {
		float f = Shaders.getShadowRenderDistance();

		if (f > 0.0F && f < ((renderDistanceChunks - 1) * 16)) {
			int i = MathHelper.ceil(f / 16.0F) + 1;
			float f6 = world.getCelestialAngleRadians((float) partialTicks);
			float f1 = Shaders.sunPathRotation * MathHelper.DEG_TO_RAD;
			float f2 = f6 > MathHelper.PI_HALF && f6 < 3.0F * MathHelper.PI_HALF ? f6 + MathHelper.PI : f6;
			float f3 = -MathHelper.sin(f2);
			float f4 = MathHelper.cos(f2) * MathHelper.cos(f1);
			float f5 = -MathHelper.cos(f2) * MathHelper.sin(f1);
			BlockPos blockpos = new BlockPos(MathHelper.floor(viewEntity.posX) >> 4, MathHelper.floor(viewEntity.posY) >> 4, MathHelper.floor(viewEntity.posZ) >> 4);
			BlockPos blockpos1 = blockpos.add((-f3 * i), (-f4 * i), (-f5 * i));
			BlockPos blockpos2 = blockpos.add((f3 * renderDistanceChunks), (f4 * renderDistanceChunks), (f5 * renderDistanceChunks));
			return new IteratorRenderChunks(viewFrustum, blockpos1, blockpos2, i, i);
		} else {
			List<RenderChunk> list = Arrays.asList(viewFrustum.renderChunks);
			return list.iterator();
		}
	}
}
