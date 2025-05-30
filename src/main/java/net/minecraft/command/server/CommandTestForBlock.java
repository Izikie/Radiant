package net.minecraft.command.server;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.*;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class CommandTestForBlock extends CommandBase {
    public String getCommandName() {
        return "testforblock";
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.testforblock.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            throw new WrongUsageException("commands.testforblock.usage");
        } else {
            sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, 0);
            BlockPos blockpos = parseBlockPos(sender, args, 0, false);
            Block block = Block.getBlockFromName(args[3]);

            if (block == null) {
                throw new NumberInvalidException("commands.setblock.notFound", args[3]);
            } else {
                int i = -1;

                if (args.length >= 5) {
                    i = parseInt(args[4], -1, 15);
                }

                World world = sender.getEntityWorld();

                if (!world.isBlockLoaded(blockpos)) {
                    throw new CommandException("commands.testforblock.outOfWorld");
                } else {
                    NBTTagCompound nbttagcompound = new NBTTagCompound();
                    boolean flag = false;

                    if (args.length >= 6 && block.hasTileEntity()) {
                        String s = getChatComponentFromNthArg(sender, args, 5).getUnformattedText();

                        try {
                            nbttagcompound = JsonToNBT.getTagFromJson(s);
                            flag = true;
                        } catch (NBTException exception) {
                            throw new CommandException("commands.setblock.tagError", exception.getMessage());
                        }
                    }

                    IBlockState iblockstate = world.getBlockState(blockpos);
                    Block block1 = iblockstate.getBlock();

                    if (block1 != block) {
                        throw new CommandException("commands.testforblock.failed.tile", blockpos.getX(), blockpos.getY(), blockpos.getZ(), block1.getLocalizedName(), block.getLocalizedName());
                    } else {
                        if (i > -1) {
                            int j = iblockstate.getBlock().getMetaFromState(iblockstate);

                            if (j != i) {
                                throw new CommandException("commands.testforblock.failed.data", blockpos.getX(), blockpos.getY(), blockpos.getZ(), j, i);
                            }
                        }

                        if (flag) {
                            TileEntity tileentity = world.getTileEntity(blockpos);

                            if (tileentity == null) {
                                throw new CommandException("commands.testforblock.failed.tileEntity", blockpos.getX(), blockpos.getY(), blockpos.getZ());
                            }

                            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                            tileentity.writeToNBT(nbttagcompound1);

                            if (!NBTUtil.func_181123_a(nbttagcompound, nbttagcompound1, true)) {
                                throw new CommandException("commands.testforblock.failed.nbt", blockpos.getX(), blockpos.getY(), blockpos.getZ());
                            }
                        }

                        sender.setCommandStat(CommandResultStats.Type.AFFECTED_BLOCKS, 1);
                        notifyOperators(sender, this, "commands.testforblock.success", blockpos.getX(), blockpos.getY(), blockpos.getZ());
                    }
                }
            }
        }
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length > 0 && args.length <= 3 ? func_175771_a(args, 0, pos) : (args.length == 4 ? getListOfStringsMatchingLastWord(args, Block.blockRegistry.getKeys()) : null);
    }
}
