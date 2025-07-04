package net.optifine.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.src.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.UUID;

public class IntegratedServerUtils {
	public static WorldServer getWorldServer() {
		Minecraft minecraft = Config.getMinecraft();
		World world = minecraft.world;

		if (world == null) {
			return null;
		} else if (!minecraft.isIntegratedServerRunning()) {
			return null;
		} else {
			IntegratedServer integratedserver = minecraft.getIntegratedServer();

			if (integratedserver == null) {
				return null;
			} else {
				WorldProvider worldprovider = world.provider;

				if (worldprovider == null) {
					return null;
				} else {
					int i = worldprovider.getDimensionId();

					try {
						return integratedserver.worldServerForDimension(i);
					} catch (NullPointerException exception) {
						return null;
					}
				}
			}
		}
	}

	public static Entity getEntity(UUID uuid) {
		WorldServer worldserver = getWorldServer();

		if (worldserver == null) {
			return null;
		} else {
			return worldserver.getEntityFromUuid(uuid);
		}
	}

	public static TileEntity getTileEntity(BlockPos pos) {
		WorldServer worldserver = getWorldServer();

		if (worldserver == null) {
			return null;
		} else {
			Chunk chunk = worldserver.getChunkProvider().provideChunk(pos.getX() >> 4, pos.getZ() >> 4);

			if (chunk == null) {
				return null;
			} else {
				return chunk.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);
			}
		}
	}
}
