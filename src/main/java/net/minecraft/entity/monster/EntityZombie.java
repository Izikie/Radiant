package net.minecraft.entity.monster;

import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class EntityZombie extends EntityMob {
    protected static final IAttribute REINFORCEMENT_CHANCE = (new RangedAttribute(null, "zombie.spawnReinforcements", 0.0D, 0.0D, 1.0D)).setDescription("Spawn Reinforcements Chance");
    private static final UUID BABY_SPEED_BOOST_UUID = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836");
    private static final AttributeModifier BABY_SPEED_BOOST_MODIFIER = new AttributeModifier(BABY_SPEED_BOOST_UUID, "Baby speed boost", 0.5D, 1);
    private final EntityAIBreakDoor breakDoor = new EntityAIBreakDoor(this);
    private int conversionTime;
    private boolean isBreakDoorsTaskSet = false;
    private float zombieWidth = -1.0F;
    private float zombieHeight;

    public EntityZombie(World worldIn) {
        super(worldIn);
        ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(2, new EntityAIAttackOnCollide(this, EntityPlayer.class, 1.0D, false));
        this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
        this.tasks.addTask(7, new EntityAIWander(this, 1.0D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.applyEntityAI();
        this.setSize(0.6F, 1.95F);
    }

    protected void applyEntityAI() {
        this.tasks.addTask(4, new EntityAIAttackOnCollide(this, EntityVillager.class, 1.0D, true));
        this.tasks.addTask(4, new EntityAIAttackOnCollide(this, EntityIronGolem.class, 1.0D, true));
        this.tasks.addTask(6, new EntityAIMoveThroughVillage(this, 1.0D, false));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPigZombie.class));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, true));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityIronGolem.class, true));
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(35.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23000000417232513D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
        this.getAttributeMap().registerAttribute(REINFORCEMENT_CHANCE).setBaseValue(this.rand.nextDouble() * 0.10000000149011612D);
    }

    protected void entityInit() {
        super.entityInit();
        this.getDataWatcher().addObject(12, (byte) 0);
        this.getDataWatcher().addObject(13, (byte) 0);
        this.getDataWatcher().addObject(14, (byte) 0);
    }

    public int getTotalArmorValue() {
        int i = super.getTotalArmorValue() + 2;

        if (i > 20) {
            i = 20;
        }

        return i;
    }

    public boolean isBreakDoorsTaskSet() {
        return this.isBreakDoorsTaskSet;
    }

    public void setBreakDoorsAItask(boolean par1) {
        if (this.isBreakDoorsTaskSet != par1) {
            this.isBreakDoorsTaskSet = par1;

            if (par1) {
                this.tasks.addTask(1, this.breakDoor);
            } else {
                this.tasks.removeTask(this.breakDoor);
            }
        }
    }

    public boolean isChild() {
        return this.getDataWatcher().getWatchableObjectByte(12) == 1;
    }

    protected int getExperiencePoints(EntityPlayer player) {
        if (this.isChild()) {
            this.experienceValue = (int) (this.experienceValue * 2.5F);
        }

        return super.getExperiencePoints(player);
    }

    public void setChild(boolean childZombie) {
        this.getDataWatcher().updateObject(12, (byte) (childZombie ? 1 : 0));

        if (this.worldObj != null && !this.worldObj.isRemote) {
            IAttributeInstance iattributeinstance = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            iattributeinstance.removeModifier(BABY_SPEED_BOOST_MODIFIER);

            if (childZombie) {
                iattributeinstance.applyModifier(BABY_SPEED_BOOST_MODIFIER);
            }
        }

        this.setChildSize(childZombie);
    }

    public boolean isVillager() {
        return this.getDataWatcher().getWatchableObjectByte(13) == 1;
    }

    public void setVillager(boolean villager) {
        this.getDataWatcher().updateObject(13, (byte) (villager ? 1 : 0));
    }

    public void onLivingUpdate() {
        if (this.worldObj.isDaytime() && !this.worldObj.isRemote && !this.isChild()) {
            float f = this.getBrightness(1.0F);
            BlockPos blockpos = new BlockPos(this.posX, Math.round(this.posY), this.posZ);

            if (f > 0.5F && this.rand.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && this.worldObj.canSeeSky(blockpos)) {
                boolean flag = true;
                ItemStack itemstack = this.getEquipmentInSlot(4);

                if (itemstack != null) {
                    if (itemstack.isItemStackDamageable()) {
                        itemstack.setItemDamage(itemstack.getItemDamage() + this.rand.nextInt(2));

                        if (itemstack.getItemDamage() >= itemstack.getMaxDamage()) {
                            this.renderBrokenItemStack(itemstack);
                            this.setCurrentItemOrArmor(4, null);
                        }
                    }

                    flag = false;
                }

                if (flag) {
                    this.setFire(8);
                }
            }
        }

        if (this.isRiding() && this.getAttackTarget() != null && this.ridingEntity instanceof EntityChicken) {
            ((EntityLiving) this.ridingEntity).getNavigator().setPath(this.getNavigator().getPath(), 1.5D);
        }

        super.onLivingUpdate();
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (super.attackEntityFrom(source, amount)) {
            EntityLivingBase entitylivingbase = this.getAttackTarget();

            if (entitylivingbase == null && source.getEntity() instanceof EntityLivingBase entityLivingBase) {
                entitylivingbase = entityLivingBase;
            }

            if (entitylivingbase != null && this.worldObj.getDifficulty() == Difficulty.HARD && this.rand.nextFloat() < this.getEntityAttribute(REINFORCEMENT_CHANCE).getAttributeValue()) {
                int i = MathHelper.floor(this.posX);
                int j = MathHelper.floor(this.posY);
                int k = MathHelper.floor(this.posZ);
                EntityZombie entityzombie = new EntityZombie(this.worldObj);

                for (int l = 0; l < 50; ++l) {
                    int i1 = i + MathHelper.getRandomIntegerInRange(this.rand, 7, 40) * MathHelper.getRandomIntegerInRange(this.rand, -1, 1);
                    int j1 = j + MathHelper.getRandomIntegerInRange(this.rand, 7, 40) * MathHelper.getRandomIntegerInRange(this.rand, -1, 1);
                    int k1 = k + MathHelper.getRandomIntegerInRange(this.rand, 7, 40) * MathHelper.getRandomIntegerInRange(this.rand, -1, 1);

                    if (World.doesBlockHaveSolidTopSurface(this.worldObj, new BlockPos(i1, j1 - 1, k1)) && this.worldObj.getLightFromNeighbors(new BlockPos(i1, j1, k1)) < 10) {
                        entityzombie.setPosition(i1, j1, k1);

                        if (!this.worldObj.isAnyPlayerWithinRangeAt(i1, j1, k1, 7.0D) && this.worldObj.checkNoEntityCollision(entityzombie.getEntityBoundingBox(), entityzombie) && this.worldObj.getCollidingBoundingBoxes(entityzombie, entityzombie.getEntityBoundingBox()).isEmpty() && !this.worldObj.isAnyLiquid(entityzombie.getEntityBoundingBox())) {
                            this.worldObj.spawnEntityInWorld(entityzombie);
                            entityzombie.setAttackTarget(entitylivingbase);
                            entityzombie.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entityzombie)), null);
                            this.getEntityAttribute(REINFORCEMENT_CHANCE).applyModifier(new AttributeModifier("Zombie reinforcement caller charge", -0.05000000074505806D, 0));
                            entityzombie.getEntityAttribute(REINFORCEMENT_CHANCE).applyModifier(new AttributeModifier("Zombie reinforcement callee charge", -0.05000000074505806D, 0));
                            break;
                        }
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void onUpdate() {
        if (!this.worldObj.isRemote && this.isConverting()) {
            int i = this.getConversionTimeBoost();
            this.conversionTime -= i;

            if (this.conversionTime <= 0) {
                this.convertToVillager();
            }
        }

        super.onUpdate();
    }

    public boolean attackEntityAsMob(Entity entityIn) {
        boolean flag = super.attackEntityAsMob(entityIn);

        if (flag) {
            int i = this.worldObj.getDifficulty().getDifficultyId();

            if (this.getHeldItem() == null && this.isBurning() && this.rand.nextFloat() < i * 0.3F) {
                entityIn.setFire(2 * i);
            }
        }

        return flag;
    }

    protected String getLivingSound() {
        return "mob.zombie.say";
    }

    protected String getHurtSound() {
        return "mob.zombie.hurt";
    }

    protected String getDeathSound() {
        return "mob.zombie.death";
    }

    protected void playStepSound(BlockPos pos, Block blockIn) {
        this.playSound("mob.zombie.step", 0.15F, 1.0F);
    }

    protected Item getDropItem() {
        return Items.ROTTEN_FLESH;
    }

    public EntityGroup getCreatureAttribute() {
        return EntityGroup.UNDEAD;
    }

    protected void addRandomDrop() {
        switch (this.rand.nextInt(3)) {
            case 0:
                this.dropItem(Items.IRON_INGOT, 1);
                break;

            case 1:
                this.dropItem(Items.CARROT, 1);
                break;

            case 2:
                this.dropItem(Items.POTATO, 1);
        }
    }

    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        super.setEquipmentBasedOnDifficulty(difficulty);

        if (this.rand.nextFloat() < (this.worldObj.getDifficulty() == Difficulty.HARD ? 0.05F : 0.01F)) {
            int i = this.rand.nextInt(3);

            if (i == 0) {
                this.setCurrentItemOrArmor(0, new ItemStack(Items.IRON_SWORD));
            } else {
                this.setCurrentItemOrArmor(0, new ItemStack(Items.IRON_SHOVEL));
            }
        }
    }

    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);

        if (this.isChild()) {
            tagCompound.setBoolean("IsBaby", true);
        }

        if (this.isVillager()) {
            tagCompound.setBoolean("IsVillager", true);
        }

        tagCompound.setInteger("ConversionTime", this.isConverting() ? this.conversionTime : -1);
        tagCompound.setBoolean("CanBreakDoors", this.isBreakDoorsTaskSet());
    }

    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        super.readEntityFromNBT(tagCompund);

        if (tagCompund.getBoolean("IsBaby")) {
            this.setChild(true);
        }

        if (tagCompund.getBoolean("IsVillager")) {
            this.setVillager(true);
        }

        if (tagCompund.hasKey("ConversionTime", 99) && tagCompund.getInteger("ConversionTime") > -1) {
            this.startConversion(tagCompund.getInteger("ConversionTime"));
        }

        this.setBreakDoorsAItask(tagCompund.getBoolean("CanBreakDoors"));
    }

    public void onKillEntity(EntityLivingBase entityLivingIn) {
        super.onKillEntity(entityLivingIn);

        if ((this.worldObj.getDifficulty() == Difficulty.NORMAL || this.worldObj.getDifficulty() == Difficulty.HARD) && entityLivingIn instanceof EntityVillager entityliving) {
            if (this.worldObj.getDifficulty() != Difficulty.HARD && this.rand.nextBoolean()) {
                return;
            }

            EntityZombie entityzombie = new EntityZombie(this.worldObj);
            entityzombie.copyLocationAndAnglesFrom(entityLivingIn);
            this.worldObj.removeEntity(entityLivingIn);
            entityzombie.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entityzombie)), null);
            entityzombie.setVillager(true);

            if (entityLivingIn.isChild()) {
                entityzombie.setChild(true);
            }

            entityzombie.setNoAI(entityliving.isAIDisabled());

            if (entityliving.hasCustomName()) {
                entityzombie.setCustomNameTag(entityliving.getCustomNameTag());
                entityzombie.setAlwaysRenderNameTag(entityliving.getAlwaysRenderNameTag());
            }

            this.worldObj.spawnEntityInWorld(entityzombie);
            this.worldObj.playAuxSFXAtEntity(null, 1016, new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ), 0);
        }
    }

    public float getEyeHeight() {
        float f = 1.74F;

        if (this.isChild()) {
            f = (float) (f - 0.81D);
        }

        return f;
    }

    protected boolean func_175448_a(ItemStack stack) {
        return (stack.getItem() != Items.EGG || !this.isChild() || !this.isRiding()) && super.func_175448_a(stack);
    }

    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        float f = difficulty.getClampedAdditionalDifficulty();
        this.setCanPickUpLoot(this.rand.nextFloat() < 0.55F * f);

        if (livingdata == null) {
            livingdata = new GroupData(this.worldObj.rand.nextFloat() < 0.05F, this.worldObj.rand.nextFloat() < 0.05F);
        }

        if (livingdata instanceof GroupData entityzombie$groupdata) {

            if (entityzombie$groupdata.isVillager) {
                this.setVillager(true);
            }

            if (entityzombie$groupdata.isChild) {
                this.setChild(true);

                if (this.worldObj.rand.nextFloat() < 0.05D) {
                    List<EntityChicken> list = this.worldObj.getEntitiesWithinAABB(EntityChicken.class, this.getEntityBoundingBox().expand(5.0D, 3.0D, 5.0D), EntitySelectors.IS_STANDALONE);

                    if (!list.isEmpty()) {
                        EntityChicken entitychicken = list.getFirst();
                        entitychicken.setChickenJockey(true);
                        this.mountEntity(entitychicken);
                    }
                } else if (this.worldObj.rand.nextFloat() < 0.05D) {
                    EntityChicken entitychicken1 = new EntityChicken(this.worldObj);
                    entitychicken1.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, 0.0F);
                    entitychicken1.onInitialSpawn(difficulty, null);
                    entitychicken1.setChickenJockey(true);
                    this.worldObj.spawnEntityInWorld(entitychicken1);
                    this.mountEntity(entitychicken1);
                }
            }
        }

        this.setBreakDoorsAItask(this.rand.nextFloat() < f * 0.1F);
        this.setEquipmentBasedOnDifficulty(difficulty);
        this.setEnchantmentBasedOnDifficulty(difficulty);

        if (this.getEquipmentInSlot(4) == null) {
            Calendar calendar = this.worldObj.getCurrentDate();

            if (calendar.get(Calendar.MONTH) + 1 == 10 && calendar.get(Calendar.DATE) == 31 && this.rand.nextFloat() < 0.25F) {
                this.setCurrentItemOrArmor(4, new ItemStack(this.rand.nextFloat() < 0.1F ? Blocks.LIT_PUMPKIN : Blocks.PUMPKIN));
                this.equipmentDropChances[4] = 0.0F;
            }
        }

        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).applyModifier(new AttributeModifier("Random spawn bonus", this.rand.nextDouble() * 0.05000000074505806D, 0));
        double d0 = this.rand.nextDouble() * 1.5D * f;

        if (d0 > 1.0D) {
            this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).applyModifier(new AttributeModifier("Random zombie-spawn bonus", d0, 2));
        }

        if (this.rand.nextFloat() < f * 0.05F) {
            this.getEntityAttribute(REINFORCEMENT_CHANCE).applyModifier(new AttributeModifier("Leader zombie bonus", this.rand.nextDouble() * 0.25D + 0.5D, 0));
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).applyModifier(new AttributeModifier("Leader zombie bonus", this.rand.nextDouble() * 3.0D + 1.0D, 2));
            this.setBreakDoorsAItask(true);
        }

        return livingdata;
    }

    public boolean interact(EntityPlayer player) {
        ItemStack itemstack = player.getCurrentEquippedItem();

        if (itemstack != null && itemstack.getItem() == Items.GOLDEN_APPLE && itemstack.getMetadata() == 0 && this.isVillager() && this.isPotionActive(Potion.WEAKNESS)) {
            if (!player.capabilities.isCreativeMode) {
                --itemstack.stackSize;
            }

            if (itemstack.stackSize <= 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }

            if (!this.worldObj.isRemote) {
                this.startConversion(this.rand.nextInt(2401) + 3600);
            }

            return true;
        } else {
            return false;
        }
    }

    protected void startConversion(int ticks) {
        this.conversionTime = ticks;
        this.getDataWatcher().updateObject(14, (byte) 1);
        this.removePotionEffect(Potion.WEAKNESS.id);
        this.addPotionEffect(new PotionEffect(Potion.DAMAGE_BOOST.id, ticks, Math.min(this.worldObj.getDifficulty().getDifficultyId() - 1, 0)));
        this.worldObj.setEntityState(this, (byte) 16);
    }

    public void handleStatusUpdate(byte id) {
        if (id == 16) {
            if (!this.isSilent()) {
                this.worldObj.playSound(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D, "mob.zombie.remedy", 1.0F + this.rand.nextFloat(), this.rand.nextFloat() * 0.7F + 0.3F, false);
            }
        } else {
            super.handleStatusUpdate(id);
        }
    }

    protected boolean canDespawn() {
        return !this.isConverting();
    }

    public boolean isConverting() {
        return this.getDataWatcher().getWatchableObjectByte(14) == 1;
    }

    protected void convertToVillager() {
        EntityVillager entityvillager = new EntityVillager(this.worldObj);
        entityvillager.copyLocationAndAnglesFrom(this);
        entityvillager.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entityvillager)), null);
        entityvillager.setLookingForHome();

        if (this.isChild()) {
            entityvillager.setGrowingAge(-24000);
        }

        this.worldObj.removeEntity(this);
        entityvillager.setNoAI(this.isAIDisabled());

        if (this.hasCustomName()) {
            entityvillager.setCustomNameTag(this.getCustomNameTag());
            entityvillager.setAlwaysRenderNameTag(this.getAlwaysRenderNameTag());
        }

        this.worldObj.spawnEntityInWorld(entityvillager);
        entityvillager.addPotionEffect(new PotionEffect(Potion.CONFUSION.id, 200, 0));
        this.worldObj.playAuxSFXAtEntity(null, 1017, new BlockPos((int) this.posX, (int) this.posY, (int) this.posZ), 0);
    }

    protected int getConversionTimeBoost() {
        int i = 1;

        if (this.rand.nextFloat() < 0.01F) {
            int j = 0;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k = (int) this.posX - 4; k < (int) this.posX + 4 && j < 14; ++k) {
                for (int l = (int) this.posY - 4; l < (int) this.posY + 4 && j < 14; ++l) {
                    for (int i1 = (int) this.posZ - 4; i1 < (int) this.posZ + 4 && j < 14; ++i1) {
                        Block block = this.worldObj.getBlockState(blockpos$mutableblockpos.set(k, l, i1)).getBlock();

                        if (block == Blocks.IRON_BARS || block == Blocks.BED) {
                            if (this.rand.nextFloat() < 0.3F) {
                                ++i;
                            }

                            ++j;
                        }
                    }
                }
            }
        }

        return i;
    }

    public void setChildSize(boolean isChild) {
        this.multiplySize(isChild ? 0.5F : 1.0F);
    }

    protected final void setSize(float width, float height) {
        boolean flag = this.zombieWidth > 0.0F && this.zombieHeight > 0.0F;
        this.zombieWidth = width;
        this.zombieHeight = height;

        if (!flag) {
            this.multiplySize(1.0F);
        }
    }

    protected final void multiplySize(float size) {
        super.setSize(this.zombieWidth * size, this.zombieHeight * size);
    }

    public double getYOffset() {
        return this.isChild() ? 0.0D : -0.35D;
    }

    public void onDeath(DamageSource cause) {
        super.onDeath(cause);

        if (cause.getEntity() instanceof EntityCreeper entityCreeper && !(this instanceof EntityPigZombie) && entityCreeper.getPowered() && entityCreeper.isAIEnabled()) {
            entityCreeper.func_175493_co();
            this.entityDropItem(new ItemStack(Items.SKULL, 1, 2), 0.0F);
        }
    }

    class GroupData implements IEntityLivingData {
        public final boolean isChild;
        public final boolean isVillager;

        private GroupData(boolean isBaby, boolean isVillagerZombie) {
            this.isChild = isBaby;
            this.isVillager = isVillagerZombie;
        }
    }
}
