package net.minecraft.command.client;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.world.World;

public class CommandShowSeed extends CommandBase {
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return MinecraftServer.getServer().isSinglePlayer() || super.canCommandSenderUseCommand(sender);
    }

    public String getCommandName() {
        return "seed";
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.seed.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        World world = sender instanceof EntityPlayer entityPlayer ? entityPlayer.worldObj : MinecraftServer.getServer().worldServerForDimension(0);
        sender.addChatMessage(new ChatComponentTranslation("commands.seed.success", world.getSeed()));
    }
}
