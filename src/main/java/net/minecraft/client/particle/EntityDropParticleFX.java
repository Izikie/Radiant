package net.minecraft.client.particle;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ParticleTypes;
import net.minecraft.world.World;

public class EntityDropParticleFX extends EntityFX {
    private final Material materialType;
    private int bobTimer;

    protected EntityDropParticleFX(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, Material p_i1203_8_) {
        super(worldIn, xCoordIn, yCoordIn, zCoordIn, 0.0D, 0.0D, 0.0D);
        this.motionX = this.motionY = this.motionZ = 0.0D;

        if (p_i1203_8_ == Material.WATER) {
            this.particleRed = 0.0F;
            this.particleGreen = 0.0F;
            this.particleBlue = 1.0F;
        } else {
            this.particleRed = 1.0F;
            this.particleGreen = 0.0F;
            this.particleBlue = 0.0F;
        }

        this.setParticleTextureIndex(113);
        this.setSize(0.01F, 0.01F);
        this.particleGravity = 0.06F;
        this.materialType = p_i1203_8_;
        this.bobTimer = 40;
        this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
        this.motionX = this.motionY = this.motionZ = 0.0D;
    }

    public int getBrightnessForRender(float partialTicks) {
        return this.materialType == Material.WATER ? super.getBrightnessForRender(partialTicks) : 257;
    }

    public float getBrightness(float partialTicks) {
        return this.materialType == Material.WATER ? super.getBrightness(partialTicks) : 1.0F;
    }

    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;

        if (this.materialType == Material.WATER) {
            this.particleRed = 0.2F;
            this.particleGreen = 0.3F;
            this.particleBlue = 1.0F;
        } else {
            this.particleRed = 1.0F;
            this.particleGreen = 16.0F / (40 - this.bobTimer + 16);
            this.particleBlue = 4.0F / (40 - this.bobTimer + 8);
        }

        this.motionY -= this.particleGravity;

        if (this.bobTimer-- > 0) {
            this.motionX *= 0.02D;
            this.motionY *= 0.02D;
            this.motionZ *= 0.02D;
            this.setParticleTextureIndex(113);
        } else {
            this.setParticleTextureIndex(112);
        }

        this.moveEntity(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9800000190734863D;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= 0.9800000190734863D;

        if (this.particleMaxAge-- <= 0) {
            this.setDead();
        }

        if (this.onGround) {
            if (this.materialType == Material.WATER) {
                this.setDead();
                this.worldObj.spawnParticle(ParticleTypes.WATER_SPLASH, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D);
            } else {
                this.setParticleTextureIndex(114);
            }

            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }

        BlockPos blockpos = new BlockPos(this);
        IBlockState iblockstate = this.worldObj.getBlockState(blockpos);
        Material material = iblockstate.getBlock().getMaterial();

        if (material.isLiquid() || material.isSolid()) {
            double d0 = 0.0D;

            if (iblockstate.getBlock() instanceof BlockLiquid) {
                d0 = BlockLiquid.getLiquidHeightPercent(iblockstate.getValue(BlockLiquid.LEVEL));
            }

            double d1 = (MathHelper.floor(this.posY) + 1) - d0;

            if (this.posY < d1) {
                this.setDead();
            }
        }
    }

    public static class LavaFactory implements IParticleFactory {
        public EntityFX getEntityFX(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, int... p_178902_15_) {
            return new EntityDropParticleFX(worldIn, xCoordIn, yCoordIn, zCoordIn, Material.LAVA);
        }
    }

    public static class WaterFactory implements IParticleFactory {
        public EntityFX getEntityFX(int particleID, World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn, double zSpeedIn, int... p_178902_15_) {
            return new EntityDropParticleFX(worldIn, xCoordIn, yCoordIn, zCoordIn, Material.WATER);
        }
    }
}
