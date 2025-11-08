package net.optifine.shaders;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.optifine.Config;
import net.optifine.config.MatchBlock;

import java.util.ArrayList;
import java.util.List;

public class BlockAlias {
	private final int blockAliasId;
	private final MatchBlock[] matchBlocks;

	public BlockAlias(int blockAliasId, MatchBlock[] matchBlocks) {
		this.blockAliasId = blockAliasId;
		this.matchBlocks = matchBlocks;
	}

	public int getBlockAliasId() {
		return this.blockAliasId;
	}

	public boolean matches(int id, int metadata) {
		for (MatchBlock matchblock : this.matchBlocks) {
			if (matchblock.matches(id, metadata)) {
				return true;
			}
		}

		return false;
	}

	public int[] getMatchBlockIds() {
		IntSet blockIDs = new IntOpenHashSet();

		for (MatchBlock matchBlock : this.matchBlocks) {
			blockIDs.add(matchBlock.getBlockId());
		}

		return blockIDs.toIntArray();
	}

	public MatchBlock[] getMatchBlocks(int matchBlockId) {
		List<MatchBlock> list = new ArrayList<>();

		for (MatchBlock matchblock : this.matchBlocks) {
			if (matchblock.getBlockId() == matchBlockId) {
				list.add(matchblock);
			}
		}

		return list.toArray(new MatchBlock[0]);
	}

	public String toString() {
		return "block." + this.blockAliasId + "=" + Config.arrayToString(this.matchBlocks);
	}
}
