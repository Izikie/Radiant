package net.minecraft.command.server;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.EnumSet;
import java.util.List;

public class CommandTeleport extends CommandBase {
    public String getCommandName() {
        return "tp";
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.tp.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException("commands.tp.usage");
        } else {
            int i = 0;
            Entity entity;

            if (args.length != 2 && args.length != 4 && args.length != 6) {
                entity = getCommandSenderAsPlayer(sender);
            } else {
                entity = getEntity(sender, args[0]);
                i = 1;
            }

            if (args.length != 1 && args.length != 2) {
                if (args.length < i + 3) {
                    throw new WrongUsageException("commands.tp.usage");
                } else if (entity.worldObj != null) {
                    int lvt_5_2_ = i + 1;
                    CoordinateArg commandbase$coordinatearg = parseCoordinate(entity.posX, args[i], true);
                    CoordinateArg commandbase$coordinatearg1 = parseCoordinate(entity.posY, args[lvt_5_2_++], 0, 0, false);
                    CoordinateArg commandbase$coordinatearg2 = parseCoordinate(entity.posZ, args[lvt_5_2_++], true);
                    CoordinateArg commandbase$coordinatearg3 = parseCoordinate(entity.rotationYaw, args.length > lvt_5_2_ ? args[lvt_5_2_++] : "~", false);
                    CoordinateArg commandbase$coordinatearg4 = parseCoordinate(entity.rotationPitch, args.length > lvt_5_2_ ? args[lvt_5_2_] : "~", false);

                    if (entity instanceof EntityPlayerMP entityPlayerMP) {
                        EnumSet<S08PacketPlayerPosLook.Flag> set = EnumSet.noneOf(S08PacketPlayerPosLook.Flag.class);

                        if (commandbase$coordinatearg.func_179630_c()) {
                            set.add(S08PacketPlayerPosLook.Flag.X);
                        }

                        if (commandbase$coordinatearg1.func_179630_c()) {
                            set.add(S08PacketPlayerPosLook.Flag.Y);
                        }

                        if (commandbase$coordinatearg2.func_179630_c()) {
                            set.add(S08PacketPlayerPosLook.Flag.Z);
                        }

                        if (commandbase$coordinatearg4.func_179630_c()) {
                            set.add(S08PacketPlayerPosLook.Flag.X_ROT);
                        }

                        if (commandbase$coordinatearg3.func_179630_c()) {
                            set.add(S08PacketPlayerPosLook.Flag.Y_ROT);
                        }

                        float f = (float) commandbase$coordinatearg3.func_179629_b();

                        if (!commandbase$coordinatearg3.func_179630_c()) {
                            f = MathHelper.wrapAngle(f);
                        }

                        float f1 = (float) commandbase$coordinatearg4.func_179629_b();

                        if (!commandbase$coordinatearg4.func_179630_c()) {
                            f1 = MathHelper.wrapAngle(f1);
                        }

                        if (f1 > 90.0F || f1 < -90.0F) {
                            f1 = MathHelper.wrapAngle(180.0F - f1);
                            f = MathHelper.wrapAngle(f + 180.0F);
                        }

                        entity.mountEntity(null);
                        entityPlayerMP.playerNetServerHandler.setPlayerLocation(commandbase$coordinatearg.func_179629_b(), commandbase$coordinatearg1.func_179629_b(), commandbase$coordinatearg2.func_179629_b(), f, f1, set);
                        entity.setRotationYawHead(f);
                    } else {
                        float f2 = (float) MathHelper.wrapAngle(commandbase$coordinatearg3.func_179628_a());
                        float f3 = (float) MathHelper.wrapAngle(commandbase$coordinatearg4.func_179628_a());

                        if (f3 > 90.0F || f3 < -90.0F) {
                            f3 = MathHelper.wrapAngle(180.0F - f3);
                            f2 = MathHelper.wrapAngle(f2 + 180.0F);
                        }

                        entity.setLocationAndAngles(commandbase$coordinatearg.func_179628_a(), commandbase$coordinatearg1.func_179628_a(), commandbase$coordinatearg2.func_179628_a(), f2, f3);
                        entity.setRotationYawHead(f2);
                    }

                    notifyOperators(sender, this, "commands.tp.success.coordinates", entity.getName(), commandbase$coordinatearg.func_179628_a(), commandbase$coordinatearg1.func_179628_a(), commandbase$coordinatearg2.func_179628_a());
                }
            } else {
                Entity entity1 = getEntity(sender, args[args.length - 1]);

                if (entity1.worldObj != entity.worldObj) {
                    throw new CommandException("commands.tp.notSameDimension");
                } else {
                    entity.mountEntity(null);

                    if (entity instanceof EntityPlayerMP entityPlayerMP) {
                        entityPlayerMP.playerNetServerHandler.setPlayerLocation(entity1.posX, entity1.posY, entity1.posZ, entity1.rotationYaw, entity1.rotationPitch);
                    } else {
                        entity.setLocationAndAngles(entity1.posX, entity1.posY, entity1.posZ, entity1.rotationYaw, entity1.rotationPitch);
                    }

                    notifyOperators(sender, this, "commands.tp.success", entity.getName(), entity1.getName());
                }
            }
        }
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length != 1 && args.length != 2 ? null : getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
    }

    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
