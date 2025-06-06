package net.minecraft.entity.item;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ParticleTypes;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class EntityMinecartTNT extends EntityMinecart {
    private int minecartTNTFuse = -1;

    public EntityMinecartTNT(World worldIn) {
        super(worldIn);
    }

    public EntityMinecartTNT(World worldIn, double x, double y, double z) {
        super(worldIn, x, y, z);
    }

    public MinecartType getMinecartType() {
        return MinecartType.TNT;
    }

    public IBlockState getDefaultDisplayTile() {
        return Blocks.TNT.getDefaultState();
    }

    public void onUpdate() {
        super.onUpdate();

        if (this.minecartTNTFuse > 0) {
            --this.minecartTNTFuse;
            this.worldObj.spawnParticle(ParticleTypes.SMOKE_NORMAL, this.posX, this.posY + 0.5D, this.posZ, 0.0D, 0.0D, 0.0D);
        } else if (this.minecartTNTFuse == 0) {
            this.explodeCart(this.motionX * this.motionX + this.motionZ * this.motionZ);
        }

        if (this.isCollidedHorizontally) {
            double d0 = this.motionX * this.motionX + this.motionZ * this.motionZ;

            if (d0 >= 0.009999999776482582D) {
                this.explodeCart(d0);
            }
        }
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        Entity entity = source.getSourceOfDamage();

        if (entity instanceof EntityArrow entityarrow) {

            if (entityarrow.isBurning()) {
                this.explodeCart(entityarrow.motionX * entityarrow.motionX + entityarrow.motionY * entityarrow.motionY + entityarrow.motionZ * entityarrow.motionZ);
            }
        }

        return super.attackEntityFrom(source, amount);
    }

    public void killMinecart(DamageSource source) {
        super.killMinecart(source);
        double d0 = this.motionX * this.motionX + this.motionZ * this.motionZ;

        if (!source.isExplosion() && this.worldObj.getGameRules().getBoolean("doEntityDrops")) {
            this.entityDropItem(new ItemStack(Blocks.TNT, 1), 0.0F);
        }

        if (source.isFireDamage() || source.isExplosion() || d0 >= 0.009999999776482582D) {
            this.explodeCart(d0);
        }
    }

    protected void explodeCart(double p_94103_1_) {
        if (!this.worldObj.isRemote) {
            double d0 = Math.sqrt(p_94103_1_);

            if (d0 > 5.0D) {
                d0 = 5.0D;
            }

            this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, (float) (4.0D + this.rand.nextDouble() * 1.5D * d0), true);
            this.setDead();
        }
    }

    public void fall(float distance, float damageMultiplier) {
        if (distance >= 3.0F) {
            float f = distance / 10.0F;
            this.explodeCart((f * f));
        }

        super.fall(distance, damageMultiplier);
    }

    public void onActivatorRailPass(int x, int y, int z, boolean receivingPower) {
        if (receivingPower && this.minecartTNTFuse < 0) {
            this.ignite();
        }
    }

    public void handleStatusUpdate(byte id) {
        if (id == 10) {
            this.ignite();
        } else {
            super.handleStatusUpdate(id);
        }
    }

    public void ignite() {
        this.minecartTNTFuse = 80;

        if (!this.worldObj.isRemote) {
            this.worldObj.setEntityState(this, (byte) 10);

            if (!this.isSilent()) {
                this.worldObj.playSoundAtEntity(this, "game.tnt.primed", 1.0F, 1.0F);
            }
        }
    }

    public int getFuseTicks() {
        return this.minecartTNTFuse;
    }

    public boolean isIgnited() {
        return this.minecartTNTFuse > -1;
    }

    public float getExplosionResistance(Explosion explosionIn, World worldIn, BlockPos pos, IBlockState blockStateIn) {
        return !this.isIgnited() || !BlockRailBase.isRailBlock(blockStateIn) && !BlockRailBase.isRailBlock(worldIn, pos.up()) ? super.getExplosionResistance(explosionIn, worldIn, pos, blockStateIn) : 0.0F;
    }

    public boolean verifyExplosion(Explosion explosionIn, World worldIn, BlockPos pos, IBlockState blockStateIn, float p_174816_5_) {
        return (!this.isIgnited() || !BlockRailBase.isRailBlock(blockStateIn) && !BlockRailBase.isRailBlock(worldIn, pos.up())) && super.verifyExplosion(explosionIn, worldIn, pos, blockStateIn, p_174816_5_);
    }

    protected void readEntityFromNBT(NBTTagCompound tagCompund) {
        super.readEntityFromNBT(tagCompund);

        if (tagCompund.hasKey("TNTFuse", 99)) {
            this.minecartTNTFuse = tagCompund.getInteger("TNTFuse");
        }
    }

    protected void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);
        tagCompound.setInteger("TNTFuse", this.minecartTNTFuse);
    }
}
