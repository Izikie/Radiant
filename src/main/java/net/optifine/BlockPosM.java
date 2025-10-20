package net.optifine;

import com.google.common.collect.AbstractIterator;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.Iterator;

public class BlockPosM extends BlockPos {
	private final int level;
	private int mx;
	private int my;
	private int mz;
	private BlockPosM[] facings;
	private boolean needsUpdate;

	public BlockPosM(int x, int y, int z) {
		this(x, y, z, 0);
	}

	public BlockPosM(double xIn, double yIn, double zIn) {
		this(MathHelper.floor(xIn), MathHelper.floor(yIn), MathHelper.floor(zIn));
	}

	public BlockPosM(int x, int y, int z, int level) {
		super(0, 0, 0);
		this.mx = x;
		this.my = y;
		this.mz = z;
		this.level = level;
	}

	public static Iterable getAllInBoxMutable(BlockPos from, BlockPos to) {
		final BlockPos blockpos = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
		final BlockPos blockpos1 = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
		return new Iterable<>() {
			@Override
            public Iterator iterator() {
				return new AbstractIterator<>() {
					private BlockPosM theBlockPosM = null;

					private BlockPosM computeNext0() {
						if (this.theBlockPosM == null) {
							this.theBlockPosM = new BlockPosM(blockpos.getX(), blockpos.getY(), blockpos.getZ(), 3);
							return this.theBlockPosM;
						} else if (this.theBlockPosM.equals(blockpos1)) {
							return (BlockPosM) this.endOfData();
						} else {
							int x = this.theBlockPosM.getX();
							int y = this.theBlockPosM.getY();
							int z = this.theBlockPosM.getZ();

							if (x < blockpos1.getX()) {
								++x;
							} else if (y < blockpos1.getY()) {
								x = blockpos.getX();
								++y;
							} else if (z < blockpos1.getZ()) {
								x = blockpos.getX();
								y = blockpos.getY();
								++z;
							}

							this.theBlockPosM.setXyz(x, y, z);
							return this.theBlockPosM;
						}
					}

					@Override
                    protected BlockPosM computeNext() {
						return this.computeNext0();
					}
				};
			}
		};
	}

	@Override
    public int getX() {
		return this.mx;
	}

	@Override
    public int getY() {
		return this.my;
	}

	@Override
    public int getZ() {
		return this.mz;
	}

	public void setXyz(int x, int y, int z) {
		this.mx = x;
		this.my = y;
		this.mz = z;
		this.needsUpdate = true;
	}

	public void setXyz(double xIn, double yIn, double zIn) {
		this.setXyz(MathHelper.floor(xIn), MathHelper.floor(yIn), MathHelper.floor(zIn));
	}

	public BlockPosM set(Vec3i vec) {
		this.setXyz(vec.getX(), vec.getY(), vec.getZ());
		return this;
	}

	public BlockPosM set(int xIn, int yIn, int zIn) {
		this.setXyz(xIn, yIn, zIn);
		return this;
	}

	public BlockPos offsetMutable(Direction facing) {
		return this.offset(facing);
	}

	@Override
    public BlockPos offset(Direction facing) {
		if (this.level <= 0) {
			return super.offset(facing, 1);
		} else {
			if (this.facings == null) {
				this.facings = new BlockPosM[Direction.VALUES.length];
			}

			if (this.needsUpdate) {
				this.update();
			}

			int i = facing.getIndex();
			BlockPosM blockposm = this.facings[i];

			if (blockposm == null) {
				int j = this.mx + facing.getFrontOffsetX();
				int k = this.my + facing.getFrontOffsetY();
				int l = this.mz + facing.getFrontOffsetZ();
				blockposm = new BlockPosM(j, k, l, this.level - 1);
				this.facings[i] = blockposm;
			}

			return blockposm;
		}
	}

	@Override
    public BlockPos offset(Direction facing, int n) {
		return n == 1 ? this.offset(facing) : super.offset(facing, n);
	}

	private void update() {
		for (int i = 0; i < 6; ++i) {
			BlockPosM blockposm = this.facings[i];

			if (blockposm != null) {
				Direction enumfacing = Direction.VALUES[i];
				int j = this.mx + enumfacing.getFrontOffsetX();
				int k = this.my + enumfacing.getFrontOffsetY();
				int l = this.mz + enumfacing.getFrontOffsetZ();
				blockposm.setXyz(j, k, l);
			}
		}

		this.needsUpdate = false;
	}

	public BlockPos toImmutable() {
		return new BlockPos(this.mx, this.my, this.mz);
	}
}
