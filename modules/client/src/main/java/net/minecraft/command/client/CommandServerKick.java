package net.minecraft.command.client;

import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;

import java.util.List;

public class CommandServerKick extends CommandBase {
    @Override
    public String getCommandName() {
        return "kick";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.kick.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length > 0 && args[0].length() > 1) {
            EntityPlayerMP entityplayermp = MinecraftServer.getServer().getConfigurationManager().getPlayerByUsername(args[0]);
            String s = "Kicked by an operator.";
            boolean flag = false;

            if (entityplayermp == null) {
                throw new PlayerNotFoundException();
            } else {
                if (args.length >= 2) {
                    s = getChatComponentFromNthArg(sender, args, 1).getUnformattedText();
                    flag = true;
                }

                entityplayermp.playerNetServerHandler.kickPlayerFromServer(s);

                if (flag) {
                    notifyOperators(sender, this, "commands.kick.success.reason", entityplayermp.getName(), s);
                } else {
                    notifyOperators(sender, this, "commands.kick.success", entityplayermp.getName());
                }
            }
        } else {
            throw new WrongUsageException("commands.kick.usage");
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length >= 1 ? getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames()) : null;
    }
}
