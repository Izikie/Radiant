package net.minecraft.entity.monster;

import com.google.common.base.Predicate;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.impl.play.server.S2BPacketChangeGameState;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.Difficulty;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.World;

public class EntityGuardian extends EntityMob {
    private float field_175482_b;
    private float field_175484_c;
    private float field_175483_bk;
    private float field_175485_bl;
    private float field_175486_bm;
    private EntityLivingBase targetedEntity;
    private int field_175479_bo;
    private boolean field_175480_bp;
    private final EntityAIWander wander;

    public EntityGuardian(World worldIn) {
        super(worldIn);
        this.experienceValue = 10;
        this.setSize(0.85F, 0.85F);
        this.tasks.addTask(4, new AIGuardianAttack(this));
        EntityAIMoveTowardsRestriction entityaimovetowardsrestriction;
        this.tasks.addTask(5, entityaimovetowardsrestriction = new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(7, this.wander = new EntityAIWander(this, 1.0D, 80));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityGuardian.class, 12.0F, 0.01F));
        this.tasks.addTask(9, new EntityAILookIdle(this));
        this.wander.setMutexBits(3);
        entityaimovetowardsrestriction.setMutexBits(3);
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityLivingBase.class, 10, true, false, new GuardianTargetSelector(this)));
        this.moveHelper = new GuardianMoveHelper(this);
        this.field_175484_c = this.field_175482_b = this.rand.nextFloat();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        super.readEntityFromNBT(tagCompund);
        this.setElder(tagCompund.getBoolean("Elder"));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);
        tagCompound.setBoolean("Elder", this.isElder());
    }

    @Override
    protected PathNavigate getNewNavigator(World worldIn) {
        return new PathNavigateSwimmer(this, worldIn);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(16, 0);
        this.dataWatcher.addObject(17, 0);
    }

    private boolean isSyncedFlagSet(int flagId) {
        return (this.dataWatcher.getWatchableObjectInt(16) & flagId) != 0;
    }

    private void setSyncedFlag(int flagId, boolean state) {
        int i = this.dataWatcher.getWatchableObjectInt(16);

        if (state) {
            this.dataWatcher.updateObject(16, i | flagId);
        } else {
            this.dataWatcher.updateObject(16, i & ~flagId);
        }
    }

    public boolean func_175472_n() {
        return this.isSyncedFlagSet(2);
    }

    private void func_175476_l(boolean p_175476_1_) {
        this.setSyncedFlag(2, p_175476_1_);
    }

    public int func_175464_ck() {
        return this.isElder() ? 60 : 80;
    }

    public boolean isElder() {
        return this.isSyncedFlagSet(4);
    }

    public void setElder(boolean elder) {
        this.setSyncedFlag(4, elder);

        if (elder) {
            this.setSize(1.9975F, 1.9975F);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.30000001192092896D);
            this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(8.0D);
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(80.0D);
            this.enablePersistence();
            this.wander.setExecutionChance(400);
        }
    }

    public void setElder() {
        this.setElder(true);
        this.field_175486_bm = this.field_175485_bl = 1.0F;
    }

    private void setTargetedEntity(int entityId) {
        this.dataWatcher.updateObject(17, entityId);
    }

    public boolean hasTargetedEntity() {
        return this.dataWatcher.getWatchableObjectInt(17) != 0;
    }

    public EntityLivingBase getTargetedEntity() {
        if (!this.hasTargetedEntity()) {
            return null;
        } else if (this.worldObj.isRemote) {
            if (this.targetedEntity != null) {
                return this.targetedEntity;
            } else {
                Entity entity = this.worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(17));

                if (entity instanceof EntityLivingBase entityLivingBase) {
                    this.targetedEntity = entityLivingBase;
                    return this.targetedEntity;
                } else {
                    return null;
                }
            }
        } else {
            return this.getAttackTarget();
        }
    }

    @Override
    public void onDataWatcherUpdate(int dataID) {
        super.onDataWatcherUpdate(dataID);

        if (dataID == 16) {
            if (this.isElder() && this.width < 1.0F) {
                this.setSize(1.9975F, 1.9975F);
            }
        } else if (dataID == 17) {
            this.field_175479_bo = 0;
            this.targetedEntity = null;
        }
    }

    @Override
    public int getTalkInterval() {
        return 160;
    }

    @Override
    protected String getLivingSound() {
        return !this.isInWater() ? "mob.guardian.land.idle" : (this.isElder() ? "mob.guardian.elder.idle" : "mob.guardian.idle");
    }

    @Override
    protected String getHurtSound() {
        return !this.isInWater() ? "mob.guardian.land.hit" : (this.isElder() ? "mob.guardian.elder.hit" : "mob.guardian.hit");
    }

    @Override
    protected String getDeathSound() {
        return !this.isInWater() ? "mob.guardian.land.death" : (this.isElder() ? "mob.guardian.elder.death" : "mob.guardian.death");
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.5F;
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        return this.worldObj.getBlockState(pos).getBlock().getMaterial() == Material.WATER ? 10.0F + this.worldObj.getLightBrightness(pos) - 0.5F : super.getBlockPathWeight(pos);
    }

    @Override
    public void onLivingUpdate() {
        if (this.worldObj.isRemote) {
            this.field_175484_c = this.field_175482_b;

            if (!this.isInWater()) {
                this.field_175483_bk = 2.0F;

                if (this.motionY > 0.0D && this.field_175480_bp && !this.isSilent()) {
                    this.worldObj.playSound(this.posX, this.posY, this.posZ, "mob.guardian.flop", 1.0F, 1.0F, false);
                }

                this.field_175480_bp = this.motionY < 0.0D && this.worldObj.isBlockNormalCube((new BlockPos(this)).down(), false);
            } else if (this.func_175472_n()) {
                if (this.field_175483_bk < 0.5F) {
                    this.field_175483_bk = 4.0F;
                } else {
                    this.field_175483_bk += (0.5F - this.field_175483_bk) * 0.1F;
                }
            } else {
                this.field_175483_bk += (0.125F - this.field_175483_bk) * 0.2F;
            }

            this.field_175482_b += this.field_175483_bk;
            this.field_175486_bm = this.field_175485_bl;

            if (!this.isInWater()) {
                this.field_175485_bl = this.rand.nextFloat();
            } else if (this.func_175472_n()) {
                this.field_175485_bl += (0.0F - this.field_175485_bl) * 0.25F;
            } else {
                this.field_175485_bl += (1.0F - this.field_175485_bl) * 0.06F;
            }

            if (this.func_175472_n() && this.isInWater()) {
                Vec3 vec3 = this.getLook(0.0F);

                for (int i = 0; i < 2; ++i) {
                    this.worldObj.spawnParticle(ParticleTypes.WATER_BUBBLE, this.posX + (this.rand.nextDouble() - 0.5D) * this.width - vec3.xCoord * 1.5D, this.posY + this.rand.nextDouble() * this.height - vec3.yCoord * 1.5D, this.posZ + (this.rand.nextDouble() - 0.5D) * this.width - vec3.zCoord * 1.5D, 0.0D, 0.0D, 0.0D);
                }
            }

            if (this.hasTargetedEntity()) {
                if (this.field_175479_bo < this.func_175464_ck()) {
                    ++this.field_175479_bo;
                }

                EntityLivingBase entitylivingbase = this.getTargetedEntity();

                if (entitylivingbase != null) {
                    this.getLookHelper().setLookPositionWithEntity(entitylivingbase, 90.0F, 90.0F);
                    this.getLookHelper().onUpdateLook();
                    double d5 = this.getAttackAnimationScale(0.0F);
                    double d0 = entitylivingbase.posX - this.posX;
                    double d1 = entitylivingbase.posY + (entitylivingbase.height * 0.5F) - (this.posY + this.getEyeHeight());
                    double d2 = entitylivingbase.posZ - this.posZ;
                    double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                    d0 = d0 / d3;
                    d1 = d1 / d3;
                    d2 = d2 / d3;
                    double d4 = this.rand.nextDouble();

                    while (d4 < d3) {
                        d4 += 1.8D - d5 + this.rand.nextDouble() * (1.7D - d5);
                        this.worldObj.spawnParticle(ParticleTypes.WATER_BUBBLE, this.posX + d0 * d4, this.posY + d1 * d4 + this.getEyeHeight(), this.posZ + d2 * d4, 0.0D, 0.0D, 0.0D);
                    }
                }
            }
        }

        if (this.inWater) {
            this.setAir(300);
        } else if (this.onGround) {
            this.motionY += 0.5D;
            this.motionX += ((this.rand.nextFloat() * 2.0F - 1.0F) * 0.4F);
            this.motionZ += ((this.rand.nextFloat() * 2.0F - 1.0F) * 0.4F);
            this.rotationYaw = this.rand.nextFloat() * 360.0F;
            this.onGround = false;
            this.isAirBorne = true;
        }

        if (this.hasTargetedEntity()) {
            this.rotationYaw = this.rotationYawHead;
        }

        super.onLivingUpdate();
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 21) {
            float scale = this.getAttackAnimationScale(0.0F);
            float volume = scale * scale;
            float pitch = 0.7F + 0.5F * scale;

            Minecraft.get().getSoundHandler().playSound(
                    new PositionedSoundRecord(
                            new ResourceLocation("minecraft:mob.guardian.attack"),
                            volume,
                            pitch,
                            true,
                            0,
                            ISound.AttenuationType.NONE,
                            (float) this.posX, (float) this.posY, (float) this.posZ
                    )
            );
        } else {
            super.handleStatusUpdate(id);
        }
    }

    public float func_175471_a(float p_175471_1_) {
        return this.field_175484_c + (this.field_175482_b - this.field_175484_c) * p_175471_1_;
    }

    public float func_175469_o(float p_175469_1_) {
        return this.field_175486_bm + (this.field_175485_bl - this.field_175486_bm) * p_175469_1_;
    }

    public float getAttackAnimationScale(float p_175477_1_) {
        return (this.field_175479_bo + p_175477_1_) / this.func_175464_ck();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();

        if (this.isElder()) {
            int i = 1200;
            int j = 1200;
            int k = 6000;
            int l = 2;

            if ((this.ticksExisted + this.getEntityId()) % 1200 == 0) {
                Potion potion = Potion.DIG_SLOWDOWN;

                for (EntityPlayerMP entityplayermp : this.worldObj.getPlayers(EntityPlayerMP.class, p_apply_1_ -> EntityGuardian.this.getDistanceSqToEntity(p_apply_1_) < 2500.0D && p_apply_1_.theItemInWorldManager.survivalOrAdventure())) {
                    if (!entityplayermp.isPotionActive(potion) || entityplayermp.getActivePotionEffect(potion).getAmplifier() < 2 || entityplayermp.getActivePotionEffect(potion).getDuration() < 1200) {
                        entityplayermp.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(10, 0.0F));
                        entityplayermp.addPotionEffect(new PotionEffect(potion.id, 6000, 2));
                    }
                }
            }

            if (!this.hasHome()) {
                this.setHomePosAndDistance(new BlockPos(this), 16);
            }
        }
    }

    @Override
    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
        int i = this.rand.nextInt(3) + this.rand.nextInt(lootingModifier + 1);

        if (i > 0) {
            this.entityDropItem(new ItemStack(Items.PRISMARINE_SHARD, i, 0), 1.0F);
        }

        if (this.rand.nextInt(3 + lootingModifier) > 1) {
            this.entityDropItem(new ItemStack(Items.FISH, 1, ItemFishFood.FishType.COD.getMetadata()), 1.0F);
        } else if (this.rand.nextInt(3 + lootingModifier) > 1) {
            this.entityDropItem(new ItemStack(Items.PRISMARINE_CRYSTALS, 1, 0), 1.0F);
        }

        if (wasRecentlyHit && this.isElder()) {
            this.entityDropItem(new ItemStack(Blocks.SPONGE, 1, 1), 1.0F);
        }
    }

    @Override
    protected void addRandomDrop() {
        ItemStack itemstack = WeightedRandom.getRandomItem(this.rand, EntityFishHook.func_174855_j()).getItemStack(this.rand);
        this.entityDropItem(itemstack, 1.0F);
    }

    @Override
    protected boolean isValidLightLevel() {
        return true;
    }

    @Override
    public boolean isNotColliding() {
        return this.worldObj.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.worldObj.getCollidingBoundingBoxes(this, this.getEntityBoundingBox()).isEmpty();
    }

    @Override
    public boolean getCanSpawnHere() {
        return (this.rand.nextInt(20) == 0 || !this.worldObj.canBlockSeeSky(new BlockPos(this))) && super.getCanSpawnHere();
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!this.func_175472_n() && !source.isMagicDamage() && source.getSourceOfDamage() instanceof EntityLivingBase entitylivingbase) {

            if (!source.isExplosion()) {
                entitylivingbase.attackEntityFrom(DamageSource.causeThornsDamage(this), 2.0F);
                entitylivingbase.playSound("damage.thorns", 0.5F, 1.0F);
            }
        }

        this.wander.makeUpdate();
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public int getVerticalFaceSpeed() {
        return 180;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (this.isServerWorld()) {
            if (this.isInWater()) {
                this.moveFlying(strafe, forward, 0.1F);
                this.moveEntity(this.motionX, this.motionY, this.motionZ);
                this.motionX *= 0.8999999761581421D;
                this.motionY *= 0.8999999761581421D;
                this.motionZ *= 0.8999999761581421D;

                if (!this.func_175472_n() && this.getAttackTarget() == null) {
                    this.motionY -= 0.005D;
                }
            } else {
                super.moveEntityWithHeading(strafe, forward);
            }
        } else {
            super.moveEntityWithHeading(strafe, forward);
        }
    }

    static class AIGuardianAttack extends EntityAIBase {
        private final EntityGuardian theEntity;
        private int tickCounter;

        public AIGuardianAttack(EntityGuardian guardian) {
            this.theEntity = guardian;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase entitylivingbase = this.theEntity.getAttackTarget();
            return entitylivingbase != null && entitylivingbase.isEntityAlive();
        }

        @Override
        public boolean continueExecuting() {
            return super.continueExecuting() && (this.theEntity.isElder() || this.theEntity.getDistanceSqToEntity(this.theEntity.getAttackTarget()) > 9.0D);
        }

        @Override
        public void startExecuting() {
            this.tickCounter = -10;
            this.theEntity.getNavigator().clearPathEntity();
            this.theEntity.getLookHelper().setLookPositionWithEntity(this.theEntity.getAttackTarget(), 90.0F, 90.0F);
            this.theEntity.isAirBorne = true;
        }

        @Override
        public void resetTask() {
            this.theEntity.setTargetedEntity(0);
            this.theEntity.setAttackTarget(null);
            this.theEntity.wander.makeUpdate();
        }

        @Override
        public void updateTask() {
            EntityLivingBase entitylivingbase = this.theEntity.getAttackTarget();
            this.theEntity.getNavigator().clearPathEntity();
            this.theEntity.getLookHelper().setLookPositionWithEntity(entitylivingbase, 90.0F, 90.0F);

            if (!this.theEntity.canEntityBeSeen(entitylivingbase)) {
                this.theEntity.setAttackTarget(null);
            } else {
                ++this.tickCounter;

                if (this.tickCounter == 0) {
                    this.theEntity.setTargetedEntity(this.theEntity.getAttackTarget().getEntityId());
                    this.theEntity.worldObj.setEntityState(this.theEntity, (byte) 21);
                } else if (this.tickCounter >= this.theEntity.func_175464_ck()) {
                    float f = 1.0F;

                    if (this.theEntity.worldObj.getDifficulty() == Difficulty.HARD) {
                        f += 2.0F;
                    }

                    if (this.theEntity.isElder()) {
                        f += 2.0F;
                    }

                    entitylivingbase.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this.theEntity, this.theEntity), f);
                    entitylivingbase.attackEntityFrom(DamageSource.causeMobDamage(this.theEntity), (float) this.theEntity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
                    this.theEntity.setAttackTarget(null);
                } else if (this.tickCounter >= 60 && this.tickCounter % 20 == 0) {
                }

                super.updateTask();
            }
        }
    }

    static class GuardianMoveHelper extends EntityMoveHelper {
        private final EntityGuardian entityGuardian;

        public GuardianMoveHelper(EntityGuardian guardian) {
            super(guardian);
            this.entityGuardian = guardian;
        }

        @Override
        public void onUpdateMoveHelper() {
            if (this.update && !this.entityGuardian.getNavigator().noPath()) {
                double d0 = this.posX - this.entityGuardian.posX;
                double d1 = this.posY - this.entityGuardian.posY;
                double d2 = this.posZ - this.entityGuardian.posZ;
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                d3 = MathHelper.sqrt(d3);
                d1 = d1 / d3;
                float f = (float) (MathHelper.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
                this.entityGuardian.rotationYaw = this.limitAngle(this.entityGuardian.rotationYaw, f, 30.0F);
                this.entityGuardian.renderYawOffset = this.entityGuardian.rotationYaw;
                float f1 = (float) (this.speed * this.entityGuardian.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                this.entityGuardian.setAIMoveSpeed(this.entityGuardian.getAIMoveSpeed() + (f1 - this.entityGuardian.getAIMoveSpeed()) * 0.125F);
                double d4 = Math.sin((this.entityGuardian.ticksExisted + this.entityGuardian.getEntityId()) * 0.5D) * 0.05D;
                double d5 = Math.cos((this.entityGuardian.rotationYaw * (float) Math.PI / 180.0F));
                double d6 = Math.sin((this.entityGuardian.rotationYaw * (float) Math.PI / 180.0F));
                this.entityGuardian.motionX += d4 * d5;
                this.entityGuardian.motionZ += d4 * d6;
                d4 = Math.sin((this.entityGuardian.ticksExisted + this.entityGuardian.getEntityId()) * 0.75D) * 0.05D;
                this.entityGuardian.motionY += d4 * (d6 + d5) * 0.25D;
                this.entityGuardian.motionY += this.entityGuardian.getAIMoveSpeed() * d1 * 0.1D;
                EntityLookHelper entitylookhelper = this.entityGuardian.getLookHelper();
                double d7 = this.entityGuardian.posX + d0 / d3 * 2.0D;
                double d8 = this.entityGuardian.getEyeHeight() + this.entityGuardian.posY + d1 / d3;
                double d9 = this.entityGuardian.posZ + d2 / d3 * 2.0D;
                double d10 = entitylookhelper.getLookPosX();
                double d11 = entitylookhelper.getLookPosY();
                double d12 = entitylookhelper.getLookPosZ();

                if (!entitylookhelper.getIsLooking()) {
                    d10 = d7;
                    d11 = d8;
                    d12 = d9;
                }

                this.entityGuardian.getLookHelper().setLookPosition(d10 + (d7 - d10) * 0.125D, d11 + (d8 - d11) * 0.125D, d12 + (d9 - d12) * 0.125D, 10.0F, 40.0F);
                this.entityGuardian.func_175476_l(true);
            } else {
                this.entityGuardian.setAIMoveSpeed(0.0F);
                this.entityGuardian.func_175476_l(false);
            }
        }
    }

    static class GuardianTargetSelector implements Predicate<EntityLivingBase> {
        private final EntityGuardian parentEntity;

        public GuardianTargetSelector(EntityGuardian guardian) {
            this.parentEntity = guardian;
        }

        @Override
        public boolean apply(EntityLivingBase p_apply_1_) {
            return (p_apply_1_ instanceof EntityPlayer || p_apply_1_ instanceof EntitySquid) && p_apply_1_.getDistanceSqToEntity(this.parentEntity) > 9.0D;
        }
    }
}
