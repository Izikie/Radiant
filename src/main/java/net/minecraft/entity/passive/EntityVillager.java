package net.minecraft.entity.passive;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.util.collection.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.village.MerchantRecipe;
import net.minecraft.world.village.MerchantRecipeList;
import net.minecraft.world.village.Village;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.ParticleTypes;
import net.minecraft.world.World;

import java.util.Random;

public class EntityVillager extends EntityAgeable implements IMerchant, INpc {
    private int randomTickDivider;
    private boolean isMating;
    private boolean isPlaying;
    Village villageObj;
    private EntityPlayer buyingPlayer;
    private MerchantRecipeList buyingList;
    private int timeUntilReset;
    private boolean needsInitilization;
    private boolean isWillingToMate;
    private int wealth;
    private String lastBuyingPlayer;
    private int careerId;
    private int careerLevel;
    private boolean isLookingForHome;
    private boolean areAdditionalTasksSet;
    private final InventoryBasic villagerInventory;
    private static final ITradeList[][][][] DEFAULT_TRADE_LIST_MAP = new ITradeList[][][][]{{{{new EmeraldForItems(Items.WHEAT, new PriceInfo(18, 22)), new EmeraldForItems(Items.POTATO, new PriceInfo(15, 19)), new EmeraldForItems(Items.CARROT, new PriceInfo(15, 19)), new ListItemForEmeralds(Items.BREAD, new PriceInfo(-4, -2))}, {new EmeraldForItems(Item.getItemFromBlock(Blocks.PUMPKIN), new PriceInfo(8, 13)), new ListItemForEmeralds(Items.PUMPKIN_PIE, new PriceInfo(-3, -2))}, {new EmeraldForItems(Item.getItemFromBlock(Blocks.MELON_BLOCK), new PriceInfo(7, 12)), new ListItemForEmeralds(Items.APPLE, new PriceInfo(-5, -7))}, {new ListItemForEmeralds(Items.COOKIE, new PriceInfo(-6, -10)), new ListItemForEmeralds(Items.CAKE, new PriceInfo(1, 1))}}, {{new EmeraldForItems(Items.STRING, new PriceInfo(15, 20)), new EmeraldForItems(Items.COAL, new PriceInfo(16, 24)), new ItemAndEmeraldToItem(Items.FISH, new PriceInfo(6, 6), Items.COOKED_FISH, new PriceInfo(6, 6))}, {new ListEnchantedItemForEmeralds(Items.FISHING_ROD, new PriceInfo(7, 8))}}, {{new EmeraldForItems(Item.getItemFromBlock(Blocks.WOOL), new PriceInfo(16, 22)), new ListItemForEmeralds(Items.SHEARS, new PriceInfo(3, 4))}, {new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 0), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 1), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 2), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 3), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 4), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 5), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 6), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 7), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 8), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 9), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 10), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 11), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 12), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 13), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 14), new PriceInfo(1, 2)), new ListItemForEmeralds(new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 1, 15), new PriceInfo(1, 2))}}, {{new EmeraldForItems(Items.STRING, new PriceInfo(15, 20)), new ListItemForEmeralds(Items.ARROW, new PriceInfo(-12, -8))}, {new ListItemForEmeralds(Items.BOW, new PriceInfo(2, 3)), new ItemAndEmeraldToItem(Item.getItemFromBlock(Blocks.GRAVEL), new PriceInfo(10, 10), Items.FLINT, new PriceInfo(6, 10))}}}, {{{new EmeraldForItems(Items.PAPER, new PriceInfo(24, 36)), new ListEnchantedBookForEmeralds()}, {new EmeraldForItems(Items.BOOK, new PriceInfo(8, 10)), new ListItemForEmeralds(Items.COMPASS, new PriceInfo(10, 12)), new ListItemForEmeralds(Item.getItemFromBlock(Blocks.BOOKSHELF), new PriceInfo(3, 4))}, {new EmeraldForItems(Items.WRITTEN_BOOK, new PriceInfo(2, 2)), new ListItemForEmeralds(Items.CLOCK, new PriceInfo(10, 12)), new ListItemForEmeralds(Item.getItemFromBlock(Blocks.GLASS), new PriceInfo(-5, -3))}, {new ListEnchantedBookForEmeralds()}, {new ListEnchantedBookForEmeralds()}, {new ListItemForEmeralds(Items.NAME_TAG, new PriceInfo(20, 22))}}}, {{{new EmeraldForItems(Items.ROTTEN_FLESH, new PriceInfo(36, 40)), new EmeraldForItems(Items.GOLD_INGOT, new PriceInfo(8, 10))}, {new ListItemForEmeralds(Items.REDSTONE, new PriceInfo(-4, -1)), new ListItemForEmeralds(new ItemStack(Items.DYE, 1, DyeColor.BLUE.getDyeDamage()), new PriceInfo(-2, -1))}, {new ListItemForEmeralds(Items.ENDER_EYE, new PriceInfo(7, 11)), new ListItemForEmeralds(Item.getItemFromBlock(Blocks.GLOWSTONE), new PriceInfo(-3, -1))}, {new ListItemForEmeralds(Items.EXPERIENCE_BOTTLE, new PriceInfo(3, 11))}}}, {{{new EmeraldForItems(Items.COAL, new PriceInfo(16, 24)), new ListItemForEmeralds(Items.IRON_HELMET, new PriceInfo(4, 6))}, {new EmeraldForItems(Items.IRON_INGOT, new PriceInfo(7, 9)), new ListItemForEmeralds(Items.IRON_CHESTPLATE, new PriceInfo(10, 14))}, {new EmeraldForItems(Items.DIAMOND, new PriceInfo(3, 4)), new ListEnchantedItemForEmeralds(Items.DIAMOND_CHESTPLATE, new PriceInfo(16, 19))}, {new ListItemForEmeralds(Items.CHAINMAIL_BOOTS, new PriceInfo(5, 7)), new ListItemForEmeralds(Items.CHAINMAIL_LEGGINGS, new PriceInfo(9, 11)), new ListItemForEmeralds(Items.CHAINMAIL_HELMET, new PriceInfo(5, 7)), new ListItemForEmeralds(Items.CHAINMAIL_CHESTPLATE, new PriceInfo(11, 15))}}, {{new EmeraldForItems(Items.COAL, new PriceInfo(16, 24)), new ListItemForEmeralds(Items.IRON_AXE, new PriceInfo(6, 8))}, {new EmeraldForItems(Items.IRON_INGOT, new PriceInfo(7, 9)), new ListEnchantedItemForEmeralds(Items.IRON_SWORD, new PriceInfo(9, 10))}, {new EmeraldForItems(Items.DIAMOND, new PriceInfo(3, 4)), new ListEnchantedItemForEmeralds(Items.DIAMOND_SWORD, new PriceInfo(12, 15)), new ListEnchantedItemForEmeralds(Items.DIAMOND_AXE, new PriceInfo(9, 12))}}, {{new EmeraldForItems(Items.COAL, new PriceInfo(16, 24)), new ListEnchantedItemForEmeralds(Items.IRON_SHOVEL, new PriceInfo(5, 7))}, {new EmeraldForItems(Items.IRON_INGOT, new PriceInfo(7, 9)), new ListEnchantedItemForEmeralds(Items.IRON_PICKAXE, new PriceInfo(9, 11))}, {new EmeraldForItems(Items.DIAMOND, new PriceInfo(3, 4)), new ListEnchantedItemForEmeralds(Items.DIAMOND_PICKAXE, new PriceInfo(12, 15))}}}, {{{new EmeraldForItems(Items.PORKCHOP, new PriceInfo(14, 18)), new EmeraldForItems(Items.CHICKEN, new PriceInfo(14, 18))}, {new EmeraldForItems(Items.COAL, new PriceInfo(16, 24)), new ListItemForEmeralds(Items.COOKED_PORKCHOP, new PriceInfo(-7, -5)), new ListItemForEmeralds(Items.COOKED_CHICKEN, new PriceInfo(-8, -6))}}, {{new EmeraldForItems(Items.LEATHER, new PriceInfo(9, 12)), new ListItemForEmeralds(Items.LEATHER_LEGGINGS, new PriceInfo(2, 4))}, {new ListEnchantedItemForEmeralds(Items.LEATHER_CHESTPLATE, new PriceInfo(7, 12))}, {new ListItemForEmeralds(Items.SADDLE, new PriceInfo(8, 10))}}}};

    public EntityVillager(World worldIn) {
        this(worldIn, 0);
    }

    public EntityVillager(World worldIn, int professionId) {
        super(worldIn);
        this.villagerInventory = new InventoryBasic("Items", false, 8);
        this.setProfession(professionId);
        this.setSize(0.6F, 1.8F);
        ((PathNavigateGround) this.getNavigator()).setBreakDoors(true);
        ((PathNavigateGround) this.getNavigator()).setAvoidsWater(true);
        this.tasks.addTask(0, new EntityAISwimming(this));
        this.tasks.addTask(1, new EntityAIAvoidEntity<>(this, EntityZombie.class, 8.0F, 0.6D, 0.6D));
        this.tasks.addTask(1, new EntityAITradePlayer(this));
        this.tasks.addTask(1, new EntityAILookAtTradePlayer(this));
        this.tasks.addTask(2, new EntityAIMoveIndoors(this));
        this.tasks.addTask(3, new EntityAIRestrictOpenDoor(this));
        this.tasks.addTask(4, new EntityAIOpenDoor(this, true));
        this.tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 0.6D));
        this.tasks.addTask(6, new EntityAIVillagerMate(this));
        this.tasks.addTask(7, new EntityAIFollowGolem(this));
        this.tasks.addTask(9, new EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        this.tasks.addTask(9, new EntityAIVillagerInteract(this));
        this.tasks.addTask(9, new EntityAIWander(this, 0.6D));
        this.tasks.addTask(10, new EntityAIWatchClosest(this, EntityLiving.class, 8.0F));
        this.setCanPickUpLoot(true);
    }

    private void setAdditionalAItasks() {
        if (!this.areAdditionalTasksSet) {
            this.areAdditionalTasksSet = true;

            if (this.isChild()) {
                this.tasks.addTask(8, new EntityAIPlay(this, 0.32D));
            } else if (this.getProfession() == 0) {
                this.tasks.addTask(6, new EntityAIHarvestFarmland(this, 0.6D));
            }
        }
    }

    @Override
    protected void onGrowingAdult() {
        if (this.getProfession() == 0) {
            this.tasks.addTask(8, new EntityAIHarvestFarmland(this, 0.6D));
        }

        super.onGrowingAdult();
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
    }

    @Override
    protected void updateAITasks() {
        if (--this.randomTickDivider <= 0) {
            BlockPos blockpos = new BlockPos(this);
            this.worldObj.getVillageCollection().addToVillagerPositionList(blockpos);
            this.randomTickDivider = 70 + this.rand.nextInt(50);
            this.villageObj = this.worldObj.getVillageCollection().getNearestVillage(blockpos, 32);

            if (this.villageObj == null) {
                this.detachHome();
            } else {
                BlockPos blockpos1 = this.villageObj.getCenter();
                this.setHomePosAndDistance(blockpos1, (int) (this.villageObj.getVillageRadius() * 1.0F));

                if (this.isLookingForHome) {
                    this.isLookingForHome = false;
                    this.villageObj.setDefaultPlayerReputation(5);
                }
            }
        }

        if (!this.isTrading() && this.timeUntilReset > 0) {
            --this.timeUntilReset;

            if (this.timeUntilReset <= 0) {
                if (this.needsInitilization) {
                    for (MerchantRecipe merchantrecipe : this.buyingList) {
                        if (merchantrecipe.isRecipeDisabled()) {
                            merchantrecipe.increaseMaxTradeUses(this.rand.nextInt(6) + this.rand.nextInt(6) + 2);
                        }
                    }

                    this.populateBuyingList();
                    this.needsInitilization = false;

                    if (this.villageObj != null && this.lastBuyingPlayer != null) {
                        this.worldObj.setEntityState(this, (byte) 14);
                        this.villageObj.setReputationForPlayer(this.lastBuyingPlayer, 1);
                    }
                }

                this.addPotionEffect(new PotionEffect(Potion.REGENERATION.id, 200, 0));
            }
        }

        super.updateAITasks();
    }

    @Override
    public boolean interact(EntityPlayer player) {
        ItemStack itemstack = player.inventory.getCurrentItem();
        boolean flag = itemstack != null && itemstack.getItem() == Items.SPAWN_EGG;

        if (!flag && this.isEntityAlive() && !this.isTrading() && !this.isChild()) {
            if (!this.worldObj.isRemote && (this.buyingList == null || !this.buyingList.isEmpty())) {
                this.setCustomer(player);
                player.displayVillagerTradeGui(this);
            }

            player.triggerAchievement(StatList.TIMES_TALKED_TO_VILLAGER_STAT);
            return true;
        } else {
            return super.interact(player);
        }
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(16, 0);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tagCompound) {
        super.writeEntityToNBT(tagCompound);
        tagCompound.setInteger("Profession", this.getProfession());
        tagCompound.setInteger("Riches", this.wealth);
        tagCompound.setInteger("Career", this.careerId);
        tagCompound.setInteger("CareerLevel", this.careerLevel);
        tagCompound.setBoolean("Willing", this.isWillingToMate);

        if (this.buyingList != null) {
            tagCompound.setTag("Offers", this.buyingList.getRecipiesAsTags());
        }

        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
            ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

            if (itemstack != null) {
                nbttaglist.appendTag(itemstack.writeToNBT(new NBTTagCompound()));
            }
        }

        tagCompound.setTag("Inventory", nbttaglist);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tagCompund) {
        super.readEntityFromNBT(tagCompund);
        this.setProfession(tagCompund.getInteger("Profession"));
        this.wealth = tagCompund.getInteger("Riches");
        this.careerId = tagCompund.getInteger("Career");
        this.careerLevel = tagCompund.getInteger("CareerLevel");
        this.isWillingToMate = tagCompund.getBoolean("Willing");

        if (tagCompund.hasKey("Offers", 10)) {
            NBTTagCompound nbttagcompound = tagCompund.getCompoundTag("Offers");
            this.buyingList = new MerchantRecipeList(nbttagcompound);
        }

        NBTTagList nbttaglist = tagCompund.getTagList("Inventory", 10);

        for (int i = 0; i < nbttaglist.tagCount(); ++i) {
            ItemStack itemstack = ItemStack.loadItemStackFromNBT(nbttaglist.getCompoundTagAt(i));

            if (itemstack != null) {
                this.villagerInventory.func_174894_a(itemstack);
            }
        }

        this.setCanPickUpLoot(true);
        this.setAdditionalAItasks();
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    protected String getLivingSound() {
        return this.isTrading() ? "mob.villager.haggle" : "mob.villager.idle";
    }

    @Override
    protected String getHurtSound() {
        return "mob.villager.hit";
    }

    @Override
    protected String getDeathSound() {
        return "mob.villager.death";
    }

    public void setProfession(int professionId) {
        this.dataWatcher.updateObject(16, professionId);
    }

    public int getProfession() {
        return Math.max(this.dataWatcher.getWatchableObjectInt(16) % 5, 0);
    }

    public boolean isMating() {
        return this.isMating;
    }

    public void setMating(boolean mating) {
        this.isMating = mating;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }

    @Override
    public void setRevengeTarget(EntityLivingBase livingBase) {
        super.setRevengeTarget(livingBase);

        if (this.villageObj != null && livingBase != null) {
            this.villageObj.addOrRenewAgressor(livingBase);

            if (livingBase instanceof EntityPlayer) {
                int i = -1;

                if (this.isChild()) {
                    i = -3;
                }

                this.villageObj.setReputationForPlayer(livingBase.getName(), i);

                if (this.isEntityAlive()) {
                    this.worldObj.setEntityState(this, (byte) 13);
                }
            }
        }
    }

    @Override
    public void onDeath(DamageSource cause) {
        if (this.villageObj != null) {
            Entity entity = cause.getEntity();

            if (entity != null) {
                if (entity instanceof EntityPlayer) {
                    this.villageObj.setReputationForPlayer(entity.getName(), -2);
                } else if (entity instanceof IMob) {
                    this.villageObj.endMatingSeason();
                }
            } else {
                EntityPlayer entityplayer = this.worldObj.getClosestPlayerToEntity(this, 16.0D);

                if (entityplayer != null) {
                    this.villageObj.endMatingSeason();
                }
            }
        }

        super.onDeath(cause);
    }

    @Override
    public void setCustomer(EntityPlayer p_70932_1_) {
        this.buyingPlayer = p_70932_1_;
    }

    @Override
    public EntityPlayer getCustomer() {
        return this.buyingPlayer;
    }

    public boolean isTrading() {
        return this.buyingPlayer != null;
    }

    public boolean getIsWillingToMate(boolean updateFirst) {
        if (!this.isWillingToMate && updateFirst && this.func_175553_cp()) {
            boolean flag = false;

            for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
                ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

                if (itemstack != null) {
                    if (itemstack.getItem() == Items.BREAD && itemstack.stackSize >= 3) {
                        flag = true;
                        this.villagerInventory.decrStackSize(i, 3);
                    } else if ((itemstack.getItem() == Items.POTATO || itemstack.getItem() == Items.CARROT) && itemstack.stackSize >= 12) {
                        flag = true;
                        this.villagerInventory.decrStackSize(i, 12);
                    }
                }

                if (flag) {
                    this.worldObj.setEntityState(this, (byte) 18);
                    this.isWillingToMate = true;
                    break;
                }
            }
        }

        return this.isWillingToMate;
    }

    public void setIsWillingToMate(boolean willingToTrade) {
        this.isWillingToMate = willingToTrade;
    }

    @Override
    public void useRecipe(MerchantRecipe recipe) {
        recipe.incrementToolUses();
        this.livingSoundTime = -this.getTalkInterval();
        this.playSound("mob.villager.yes", this.getSoundVolume(), this.getSoundPitch());
        int i = 3 + this.rand.nextInt(4);

        if (recipe.getToolUses() == 1 || this.rand.nextInt(5) == 0) {
            this.timeUntilReset = 40;
            this.needsInitilization = true;
            this.isWillingToMate = true;

            if (this.buyingPlayer != null) {
                this.lastBuyingPlayer = this.buyingPlayer.getName();
            } else {
                this.lastBuyingPlayer = null;
            }

            i += 5;
        }

        if (recipe.getItemToBuy().getItem() == Items.EMERALD) {
            this.wealth += recipe.getItemToBuy().stackSize;
        }

        if (recipe.getRewardsExp()) {
            this.worldObj.spawnEntityInWorld(new EntityXPOrb(this.worldObj, this.posX, this.posY + 0.5D, this.posZ, i));
        }
    }

    @Override
    public void verifySellingItem(ItemStack stack) {
        if (!this.worldObj.isRemote && this.livingSoundTime > -this.getTalkInterval() + 20) {
            this.livingSoundTime = -this.getTalkInterval();

            if (stack != null) {
                this.playSound("mob.villager.yes", this.getSoundVolume(), this.getSoundPitch());
            } else {
                this.playSound("mob.villager.no", this.getSoundVolume(), this.getSoundPitch());
            }
        }
    }

    @Override
    public MerchantRecipeList getRecipes(EntityPlayer p_70934_1_) {
        if (this.buyingList == null) {
            this.populateBuyingList();
        }

        return this.buyingList;
    }

    private void populateBuyingList() {
        ITradeList[][][] aentityvillager$itradelist = DEFAULT_TRADE_LIST_MAP[this.getProfession()];

        if (this.careerId != 0 && this.careerLevel != 0) {
            ++this.careerLevel;
        } else {
            this.careerId = this.rand.nextInt(aentityvillager$itradelist.length) + 1;
            this.careerLevel = 1;
        }

        if (this.buyingList == null) {
            this.buyingList = new MerchantRecipeList();
        }

        int i = this.careerId - 1;
        int j = this.careerLevel - 1;
        ITradeList[][] aentityvillager$itradelist1 = aentityvillager$itradelist[i];

        if (j >= 0 && j < aentityvillager$itradelist1.length) {
            ITradeList[] aentityvillager$itradelist2 = aentityvillager$itradelist1[j];

            for (ITradeList entityvillager$itradelist : aentityvillager$itradelist2) {
                entityvillager$itradelist.modifyMerchantRecipeList(this.buyingList, this.rand);
            }
        }
    }

    @Override
    public void setRecipes(MerchantRecipeList recipeList) {
    }

    @Override
    public IChatComponent getDisplayName() {
        String s = this.getCustomNameTag();

        if (s != null && !s.isEmpty()) {
            ChatComponentText chatcomponenttext = new ChatComponentText(s);
            chatcomponenttext.getChatStyle().setChatHoverEvent(this.getHoverEvent());
            chatcomponenttext.getChatStyle().setInsertion(this.getUniqueID().toString());
            return chatcomponenttext;
        } else {
            if (this.buyingList == null) {
                this.populateBuyingList();
            }

            String s1 = null;

            switch (this.getProfession()) {
                case 0:
                    if (this.careerId == 1) {
                        s1 = "farmer";
                    } else if (this.careerId == 2) {
                        s1 = "fisherman";
                    } else if (this.careerId == 3) {
                        s1 = "shepherd";
                    } else if (this.careerId == 4) {
                        s1 = "fletcher";
                    }

                    break;

                case 1:
                    s1 = "librarian";
                    break;

                case 2:
                    s1 = "cleric";
                    break;

                case 3:
                    if (this.careerId == 1) {
                        s1 = "armor";
                    } else if (this.careerId == 2) {
                        s1 = "weapon";
                    } else if (this.careerId == 3) {
                        s1 = "tool";
                    }

                    break;

                case 4:
                    if (this.careerId == 1) {
                        s1 = "butcher";
                    } else if (this.careerId == 2) {
                        s1 = "leather";
                    }
            }

            if (s1 != null) {
                ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("entity.Villager." + s1);
                chatcomponenttranslation.getChatStyle().setChatHoverEvent(this.getHoverEvent());
                chatcomponenttranslation.getChatStyle().setInsertion(this.getUniqueID().toString());
                return chatcomponenttranslation;
            } else {
                return super.getDisplayName();
            }
        }
    }

    @Override
    public float getEyeHeight() {
        float f = 1.62F;

        if (this.isChild()) {
            f = (float) (f - 0.81D);
        }

        return f;
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 12) {
            this.spawnParticles(ParticleTypes.HEART);
        } else if (id == 13) {
            this.spawnParticles(ParticleTypes.VILLAGER_ANGRY);
        } else if (id == 14) {
            this.spawnParticles(ParticleTypes.VILLAGER_HAPPY);
        } else {
            super.handleStatusUpdate(id);
        }
    }

    private void spawnParticles(ParticleTypes particleType) {
        for (int i = 0; i < 5; ++i) {
            double d0 = this.rand.nextGaussian() * 0.02D;
            double d1 = this.rand.nextGaussian() * 0.02D;
            double d2 = this.rand.nextGaussian() * 0.02D;
            this.worldObj.spawnParticle(particleType, this.posX + (this.rand.nextFloat() * this.width * 2.0F) - this.width, this.posY + 1.0D + (this.rand.nextFloat() * this.height), this.posZ + (this.rand.nextFloat() * this.width * 2.0F) - this.width, d0, d1, d2);
        }
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
        livingdata = super.onInitialSpawn(difficulty, livingdata);
        this.setProfession(this.worldObj.rand.nextInt(5));
        this.setAdditionalAItasks();
        return livingdata;
    }

    public void setLookingForHome() {
        this.isLookingForHome = true;
    }

    @Override
    public EntityVillager createChild(EntityAgeable ageable) {
        EntityVillager entityvillager = new EntityVillager(this.worldObj);
        entityvillager.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entityvillager)), null);
        return entityvillager;
    }

    @Override
    public boolean allowLeashing() {
        return false;
    }

    @Override
    public void onStruckByLightning(EntityLightningBolt lightningBolt) {
        if (!this.worldObj.isRemote && !this.isDead) {
            EntityWitch entitywitch = new EntityWitch(this.worldObj);
            entitywitch.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            entitywitch.onInitialSpawn(this.worldObj.getDifficultyForLocation(new BlockPos(entitywitch)), null);
            entitywitch.setNoAI(this.isAIDisabled());

            if (this.hasCustomName()) {
                entitywitch.setCustomNameTag(this.getCustomNameTag());
                entitywitch.setAlwaysRenderNameTag(this.getAlwaysRenderNameTag());
            }

            this.worldObj.spawnEntityInWorld(entitywitch);
            this.setDead();
        }
    }

    public InventoryBasic getVillagerInventory() {
        return this.villagerInventory;
    }

    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        ItemStack itemstack = itemEntity.getEntityItem();
        Item item = itemstack.getItem();

        if (this.canVillagerPickupItem(item)) {
            ItemStack itemstack1 = this.villagerInventory.func_174894_a(itemstack);

            if (itemstack1 == null) {
                itemEntity.setDead();
            } else {
                itemstack.stackSize = itemstack1.stackSize;
            }
        }
    }

    private boolean canVillagerPickupItem(Item itemIn) {
        return itemIn == Items.BREAD || itemIn == Items.POTATO || itemIn == Items.CARROT || itemIn == Items.WHEAT || itemIn == Items.WHEAT_SEEDS;
    }

    public boolean func_175553_cp() {
        return this.hasEnoughItems(1);
    }

    public boolean canAbondonItems() {
        return this.hasEnoughItems(2);
    }

    public boolean func_175557_cr() {
        boolean flag = this.getProfession() == 0;
        return flag ? !this.hasEnoughItems(5) : !this.hasEnoughItems(1);
    }

    private boolean hasEnoughItems(int multiplier) {
        boolean flag = this.getProfession() == 0;

        for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
            ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

            if (itemstack != null) {
                if (itemstack.getItem() == Items.BREAD && itemstack.stackSize >= 3 * multiplier || itemstack.getItem() == Items.POTATO && itemstack.stackSize >= 12 * multiplier || itemstack.getItem() == Items.CARROT && itemstack.stackSize >= 12 * multiplier) {
                    return true;
                }

                if (flag && itemstack.getItem() == Items.WHEAT && itemstack.stackSize >= 9 * multiplier) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isFarmItemInInventory() {
        for (int i = 0; i < this.villagerInventory.getSizeInventory(); ++i) {
            ItemStack itemstack = this.villagerInventory.getStackInSlot(i);

            if (itemstack != null && (itemstack.getItem() == Items.WHEAT_SEEDS || itemstack.getItem() == Items.POTATO || itemstack.getItem() == Items.CARROT)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean replaceItemInInventory(int inventorySlot, ItemStack itemStackIn) {
        if (super.replaceItemInInventory(inventorySlot, itemStackIn)) {
            return true;
        } else {
            int i = inventorySlot - 300;

            if (i >= 0 && i < this.villagerInventory.getSizeInventory()) {
                this.villagerInventory.setInventorySlotContents(i, itemStackIn);
                return true;
            } else {
                return false;
            }
        }
    }

    public int getCareerId() {
        return careerId;
    }

    public void setCareerId(int careerId) {
        this.careerId = careerId;
    }

    public int getCareerLevel() {
        return careerLevel;
    }

    public void setCareerLevel(int careerLevel) {
        this.careerLevel = careerLevel;
    }

    static class EmeraldForItems implements ITradeList {
        public final Item sellItem;
        public final PriceInfo price;

        public EmeraldForItems(Item itemIn, PriceInfo priceIn) {
            this.sellItem = itemIn;
            this.price = priceIn;
        }

        @Override
        public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
            int i = 1;

            if (this.price != null) {
                i = this.price.getPrice(random);
            }

            recipeList.add(new MerchantRecipe(new ItemStack(this.sellItem, i, 0), Items.EMERALD));
        }
    }

    interface ITradeList {
        void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random);
    }

    static class ItemAndEmeraldToItem implements ITradeList {
        public final ItemStack buyingItemStack;
        public final PriceInfo buyingPriceInfo;
        public final ItemStack sellingItemstack;
        public final PriceInfo field_179408_d;

        public ItemAndEmeraldToItem(Item p_i45813_1_, PriceInfo p_i45813_2_, Item p_i45813_3_, PriceInfo p_i45813_4_) {
            this.buyingItemStack = new ItemStack(p_i45813_1_);
            this.buyingPriceInfo = p_i45813_2_;
            this.sellingItemstack = new ItemStack(p_i45813_3_);
            this.field_179408_d = p_i45813_4_;
        }

        @Override
        public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
            int i = 1;

            if (this.buyingPriceInfo != null) {
                i = this.buyingPriceInfo.getPrice(random);
            }

            int j = 1;

            if (this.field_179408_d != null) {
                j = this.field_179408_d.getPrice(random);
            }

            recipeList.add(new MerchantRecipe(new ItemStack(this.buyingItemStack.getItem(), i, this.buyingItemStack.getMetadata()), new ItemStack(Items.EMERALD), new ItemStack(this.sellingItemstack.getItem(), j, this.sellingItemstack.getMetadata())));
        }
    }

    static class ListEnchantedBookForEmeralds implements ITradeList {
        @Override
        public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
            Enchantment enchantment = Enchantment.ENCHANTMENTS_BOOK_LIST[random.nextInt(Enchantment.ENCHANTMENTS_BOOK_LIST.length)];
            int i = MathHelper.getRandomIntegerInRange(random, enchantment.getMinLevel(), enchantment.getMaxLevel());
            ItemStack itemstack = Items.ENCHANTED_BOOK.getEnchantedItemStack(new EnchantmentData(enchantment, i));
            int j = 2 + random.nextInt(5 + i * 10) + 3 * i;

            if (j > 64) {
                j = 64;
            }

            recipeList.add(new MerchantRecipe(new ItemStack(Items.BOOK), new ItemStack(Items.EMERALD, j), itemstack));
        }
    }

    static class ListEnchantedItemForEmeralds implements ITradeList {
        public final ItemStack enchantedItemStack;
        public final PriceInfo priceInfo;

        public ListEnchantedItemForEmeralds(Item p_i45814_1_, PriceInfo p_i45814_2_) {
            this.enchantedItemStack = new ItemStack(p_i45814_1_);
            this.priceInfo = p_i45814_2_;
        }

        @Override
        public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
            int i = 1;

            if (this.priceInfo != null) {
                i = this.priceInfo.getPrice(random);
            }

            ItemStack itemstack = new ItemStack(Items.EMERALD, i, 0);
            ItemStack itemstack1 = new ItemStack(this.enchantedItemStack.getItem(), 1, this.enchantedItemStack.getMetadata());
            itemstack1 = EnchantmentHelper.addRandomEnchantment(random, itemstack1, 5 + random.nextInt(15));
            recipeList.add(new MerchantRecipe(itemstack, itemstack1));
        }
    }

    static class ListItemForEmeralds implements ITradeList {
        public final ItemStack itemToBuy;
        public final PriceInfo priceInfo;

        public ListItemForEmeralds(Item par1Item, PriceInfo priceInfo) {
            this.itemToBuy = new ItemStack(par1Item);
            this.priceInfo = priceInfo;
        }

        public ListItemForEmeralds(ItemStack stack, PriceInfo priceInfo) {
            this.itemToBuy = stack;
            this.priceInfo = priceInfo;
        }

        @Override
        public void modifyMerchantRecipeList(MerchantRecipeList recipeList, Random random) {
            int i = 1;

            if (this.priceInfo != null) {
                i = this.priceInfo.getPrice(random);
            }

            ItemStack itemstack;
            ItemStack itemstack1;

            if (i < 0) {
                itemstack = new ItemStack(Items.EMERALD, 1, 0);
                itemstack1 = new ItemStack(this.itemToBuy.getItem(), -i, this.itemToBuy.getMetadata());
            } else {
                itemstack = new ItemStack(Items.EMERALD, i, 0);
                itemstack1 = new ItemStack(this.itemToBuy.getItem(), 1, this.itemToBuy.getMetadata());
            }

            recipeList.add(new MerchantRecipe(itemstack, itemstack1));
        }
    }

    static class PriceInfo extends Tuple<Integer, Integer> {
        public PriceInfo(int first, int second) {
            super(first, second);
        }

        public int getPrice(Random rand) {
            return this.getFirst() >= this.getSecond() ? this.getFirst() : this.getFirst() + rand.nextInt(this.getSecond() - this.getFirst() + 1);
        }
    }
}
