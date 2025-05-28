package net.minecraft.init;

import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.projectile.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.util.LoggingPrintStream;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.util.Random;

public class Bootstrap {
    private static final PrintStream SYSOUT = System.out;
    private static boolean alreadyRegistered = false;
    private static final Logger LOGGER = LogManager.getLogger();

    public static boolean isRegistered() {
        return alreadyRegistered;
    }

    static void registerDispenserBehaviors() {
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.ARROW, new BehaviorProjectileDispense() {
            protected IProjectile getProjectileEntity(World worldIn, IPosition position) {
                EntityArrow arrow = new EntityArrow(worldIn, position.getX(), position.getY(), position.getZ());
                arrow.canBePickedUp = 1;
                return arrow;
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.EGG, new BehaviorProjectileDispense() {
            protected IProjectile getProjectileEntity(World worldIn, IPosition position) {
                return new EntityEgg(worldIn, position.getX(), position.getY(), position.getZ());
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.SNOWBALL, new BehaviorProjectileDispense() {
            protected IProjectile getProjectileEntity(World worldIn, IPosition position) {
                return new EntitySnowball(worldIn, position.getX(), position.getY(), position.getZ());
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.EXPERIENCE_BOTTLE, new BehaviorProjectileDispense() {
            protected IProjectile getProjectileEntity(World worldIn, IPosition position) {
                return new EntityExpBottle(worldIn, position.getX(), position.getY(), position.getZ());
            }

            protected float func_82498_a() {
                return super.func_82498_a() * 0.5F;
            }

            protected float func_82500_b() {
                return super.func_82500_b() * 1.25F;
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.POTION, new IBehaviorDispenseItem() {
            private final BehaviorDefaultDispenseItem field_150843_b = new BehaviorDefaultDispenseItem();

            public ItemStack dispense(IBlockSource source, final ItemStack stack) {
                return ItemPotion.isSplash(stack.getMetadata()) ? (new BehaviorProjectileDispense() {
                    protected IProjectile getProjectileEntity(World worldIn, IPosition position) {
                        return new EntityPotion(worldIn, position.getX(), position.getY(), position.getZ(), stack.copy());
                    }

                    protected float func_82498_a() {
                        return super.func_82498_a() * 0.5F;
                    }

                    protected float func_82500_b() {
                        return super.func_82500_b() * 1.25F;
                    }
                }).dispense(source, stack) : this.field_150843_b.dispense(source, stack);
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.SPAWN_EGG, new BehaviorDefaultDispenseItem() {
            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                Direction direction = BlockDispenser.getFacing(source.getBlockMetadata());
                double x = source.getX() + direction.getFrontOffsetX();
                double y = (source.getBlockPos().getY() + 0.2F);
                double z = source.getZ() + direction.getFrontOffsetZ();
                Entity entity = ItemMonsterPlacer.spawnCreature(source.getWorld(), stack.getMetadata(), x, y, z);

                if (entity instanceof EntityLivingBase && stack.hasDisplayName()) {
                    entity.setCustomNameTag(stack.getDisplayName());
                }

                stack.splitStack(1);
                return stack;
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.FIREWORKS, new BehaviorDefaultDispenseItem() {
            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                Direction direction = BlockDispenser.getFacing(source.getBlockMetadata());
                double d0 = source.getX() + direction.getFrontOffsetX();
                double d1 = (source.getBlockPos().getY() + 0.2F);
                double d2 = source.getZ() + direction.getFrontOffsetZ();
                EntityFireworkRocket entityfireworkrocket = new EntityFireworkRocket(source.getWorld(), d0, d1, d2, stack);
                source.getWorld().spawnEntityInWorld(entityfireworkrocket);
                stack.splitStack(1);
                return stack;
            }

            protected void playDispenseSound(IBlockSource source) {
                source.getWorld().playAuxSFX(1002, source.getBlockPos(), 0);
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.FIRE_CHARGE, new BehaviorDefaultDispenseItem() {
            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                Direction direction = BlockDispenser.getFacing(source.getBlockMetadata());
                IPosition position = BlockDispenser.getDispensePosition(source);
                double x = position.getX() + (direction.getFrontOffsetX() * 0.3F);
                double y = position.getY() + (direction.getFrontOffsetY() * 0.3F);
                double z = position.getZ() + (direction.getFrontOffsetZ() * 0.3F);
                World world = source.getWorld();
                Random random = world.rand;
                double accelX = random.nextGaussian() * 0.05D + direction.getFrontOffsetX();
                double accelY = random.nextGaussian() * 0.05D + direction.getFrontOffsetY();
                double accelZ = random.nextGaussian() * 0.05D + direction.getFrontOffsetZ();
                world.spawnEntityInWorld(new EntitySmallFireball(world, x, y, z, accelX, accelY, accelZ));
                stack.splitStack(1);
                return stack;
            }

            protected void playDispenseSound(IBlockSource source) {
                source.getWorld().playAuxSFX(1009, source.getBlockPos(), 0);
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.BOAT, new BehaviorDefaultDispenseItem() {
            private final BehaviorDefaultDispenseItem field_150842_b = new BehaviorDefaultDispenseItem();

            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                Direction direction = BlockDispenser.getFacing(source.getBlockMetadata());
                World world = source.getWorld();
                double x = source.getX() + (direction.getFrontOffsetX() * 1.125F);
                double y = source.getY() + (direction.getFrontOffsetY() * 1.125F);
                double z = source.getZ() + (direction.getFrontOffsetZ() * 1.125F);
                BlockPos blockPos = source.getBlockPos().offset(direction);
                Material material = world.getBlockState(blockPos).getBlock().getMaterial();
                double d3;

                if (Material.WATER.equals(material)) {
                    d3 = 1.0D;
                } else {
                    if (!Material.AIR.equals(material) || !Material.WATER.equals(world.getBlockState(blockPos.down()).getBlock().getMaterial())) {
                        return this.field_150842_b.dispense(source, stack);
                    }

                    d3 = 0.0D;
                }

                EntityBoat entityboat = new EntityBoat(world, x, y + d3, z);
                world.spawnEntityInWorld(entityboat);
                stack.splitStack(1);
                return stack;
            }

            protected void playDispenseSound(IBlockSource source) {
                source.getWorld().playAuxSFX(1000, source.getBlockPos(), 0);
            }
        });
        IBehaviorDispenseItem ibehaviordispenseitem = new BehaviorDefaultDispenseItem() {
            private final BehaviorDefaultDispenseItem field_150841_b = new BehaviorDefaultDispenseItem();

            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                ItemBucket bucket = (ItemBucket) stack.getItem();
                BlockPos blockPos = source.getBlockPos().offset(BlockDispenser.getFacing(source.getBlockMetadata()));

                if (bucket.tryPlaceContainedLiquid(source.getWorld(), blockPos)) {
                    stack.setItem(Items.BUCKET);
                    stack.stackSize = 1;
                    return stack;
                } else {
                    return this.field_150841_b.dispense(source, stack);
                }
            }
        };
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.LAVA_BUCKET, ibehaviordispenseitem);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.WATER_BUCKET, ibehaviordispenseitem);
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.BUCKET, new BehaviorDefaultDispenseItem() {
            private final BehaviorDefaultDispenseItem field_150840_b = new BehaviorDefaultDispenseItem();

            public ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos blockPos = source.getBlockPos().offset(BlockDispenser.getFacing(source.getBlockMetadata()));
                IBlockState iblockstate = world.getBlockState(blockPos);
                Block block = iblockstate.getBlock();
                Material material = block.getMaterial();
                Item item;

                if (Material.WATER.equals(material) && block instanceof BlockLiquid && iblockstate.getValue(BlockLiquid.LEVEL) == 0) {
                    item = Items.WATER_BUCKET;
                } else {
                    if (!Material.LAVA.equals(material) || !(block instanceof BlockLiquid) || iblockstate.getValue(BlockLiquid.LEVEL) != 0) {
                        return super.dispenseStack(source, stack);
                    }

                    item = Items.LAVA_BUCKET;
                }

                world.setBlockToAir(blockPos);

                if (--stack.stackSize == 0) {
                    stack.setItem(item);
                    stack.stackSize = 1;
                } else if (((TileEntityDispenser) source.getBlockTileEntity()).addItemStack(new ItemStack(item)) < 0) {
                    this.field_150840_b.dispense(source, new ItemStack(item));
                }

                return stack;
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.FLINT_AND_STEEL, new BehaviorDefaultDispenseItem() {
            private boolean field_150839_b = true;

            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos blockpos = source.getBlockPos().offset(BlockDispenser.getFacing(source.getBlockMetadata()));

                if (world.isAirBlock(blockpos)) {
                    world.setBlockState(blockpos, Blocks.FIRE.getDefaultState());

                    if (stack.attemptDamageItem(1, world.rand)) {
                        stack.stackSize = 0;
                    }
                } else if (world.getBlockState(blockpos).getBlock() == Blocks.TNT) {
                    Blocks.TNT.onBlockDestroyedByPlayer(world, blockpos, Blocks.TNT.getDefaultState().withProperty(BlockTNT.EXPLODE, Boolean.TRUE));
                    world.setBlockToAir(blockpos);
                } else {
                    this.field_150839_b = false;
                }

                return stack;
            }

            protected void playDispenseSound(IBlockSource source) {
                if (this.field_150839_b) {
                    source.getWorld().playAuxSFX(1000, source.getBlockPos(), 0);
                } else {
                    source.getWorld().playAuxSFX(1001, source.getBlockPos(), 0);
                }
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.DYE, new BehaviorDefaultDispenseItem() {
            private boolean field_150838_b = true;

            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                if (DyeColor.WHITE == DyeColor.byDyeDamage(stack.getMetadata())) {
                    World world = source.getWorld();
                    BlockPos blockpos = source.getBlockPos().offset(BlockDispenser.getFacing(source.getBlockMetadata()));

                    if (ItemDye.applyBonemeal(stack, world, blockpos)) {
                        if (!world.isRemote) {
                            world.playAuxSFX(2005, blockpos, 0);
                        }
                    } else {
                        this.field_150838_b = false;
                    }

                    return stack;
                } else {
                    return super.dispenseStack(source, stack);
                }
            }

            protected void playDispenseSound(IBlockSource source) {
                if (this.field_150838_b) {
                    source.getWorld().playAuxSFX(1000, source.getBlockPos(), 0);
                } else {
                    source.getWorld().playAuxSFX(1001, source.getBlockPos(), 0);
                }
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.getItemFromBlock(Blocks.TNT), new BehaviorDefaultDispenseItem() {
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos blockpos = source.getBlockPos().offset(BlockDispenser.getFacing(source.getBlockMetadata()));
                EntityTNTPrimed entitytntprimed = new EntityTNTPrimed(world, blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D, null);
                world.spawnEntityInWorld(entitytntprimed);
                world.playSoundAtEntity(entitytntprimed, "game.tnt.primed", 1.0F, 1.0F);
                --stack.stackSize;
                return stack;
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Items.SKULL, new BehaviorDefaultDispenseItem() {
            private boolean field_179240_b = true;

            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                Direction enumfacing = BlockDispenser.getFacing(source.getBlockMetadata());
                BlockPos blockpos = source.getBlockPos().offset(enumfacing);
                BlockSkull blockskull = Blocks.SKULL;

                if (world.isAirBlock(blockpos) && blockskull.canDispenserPlace(world, blockpos, stack)) {
                    if (!world.isRemote) {
                        world.setBlockState(blockpos, blockskull.getDefaultState().withProperty(BlockSkull.FACING, Direction.UP), 3);
                        TileEntity tileentity = world.getTileEntity(blockpos);

                        if (tileentity instanceof TileEntitySkull tileEntitySkull) {
                            if (stack.getMetadata() == 3) {
                                GameProfile gameprofile = null;

                                if (stack.hasTagCompound()) {
                                    NBTTagCompound nbttagcompound = stack.getTagCompound();

                                    if (nbttagcompound.hasKey("SkullOwner", 10)) {
                                        gameprofile = NBTUtil.readGameProfileFromNBT(nbttagcompound.getCompoundTag("SkullOwner"));
                                    } else if (nbttagcompound.hasKey("SkullOwner", 8)) {
                                        String s = nbttagcompound.getString("SkullOwner");

                                        if (!StringUtils.isNullOrEmpty(s)) {
                                            gameprofile = new GameProfile(null, s);
                                        }
                                    }
                                }

                                ((TileEntitySkull) tileentity).setPlayerProfile(gameprofile);
                            } else {
                                ((TileEntitySkull) tileentity).setType(stack.getMetadata());
                            }

                            tileEntitySkull.setSkullRotation(enumfacing.getOpposite().getHorizontalIndex() * 4);
                            Blocks.SKULL.checkWitherSpawn(world, blockpos, tileEntitySkull);
                        }

                        --stack.stackSize;
                    }
                } else {
                    this.field_179240_b = false;
                }

                return stack;
            }

            protected void playDispenseSound(IBlockSource source) {
                if (this.field_179240_b) {
                    source.getWorld().playAuxSFX(1000, source.getBlockPos(), 0);
                } else {
                    source.getWorld().playAuxSFX(1001, source.getBlockPos(), 0);
                }
            }
        });
        BlockDispenser.dispenseBehaviorRegistry.putObject(Item.getItemFromBlock(Blocks.PUMPKIN), new BehaviorDefaultDispenseItem() {
            private boolean field_179241_b = true;

            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos blockpos = source.getBlockPos().offset(BlockDispenser.getFacing(source.getBlockMetadata()));
                BlockPumpkin blockpumpkin = (BlockPumpkin) Blocks.PUMPKIN;

                if (world.isAirBlock(blockpos) && blockpumpkin.canDispenserPlace(world, blockpos)) {
                    if (!world.isRemote) {
                        world.setBlockState(blockpos, blockpumpkin.getDefaultState(), 3);
                    }

                    --stack.stackSize;
                } else {
                    this.field_179241_b = false;
                }

                return stack;
            }

            protected void playDispenseSound(IBlockSource source) {
                if (this.field_179241_b) {
                    source.getWorld().playAuxSFX(1000, source.getBlockPos(), 0);
                } else {
                    source.getWorld().playAuxSFX(1001, source.getBlockPos(), 0);
                }
            }
        });
    }

    public static void register() {
        if (!alreadyRegistered) {
            alreadyRegistered = true;

            if (LOGGER.isDebugEnabled()) {
                redirectOutputToLog();
            }

            Block.registerBlocks();
            BlockFire.init();
            Item.registerItems();
            StatList.init();
            registerDispenserBehaviors();
        }
    }

    private static void redirectOutputToLog() {
        System.setErr(new LoggingPrintStream("STDERR", System.err));
        System.setOut(new LoggingPrintStream("STDOUT", SYSOUT));
    }

    public static void printToSYSOUT(String p_179870_0_) {
        SYSOUT.println(p_179870_0_);
    }
}
