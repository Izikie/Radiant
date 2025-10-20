package net.minecraft.command.client;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.world.WorldSettings;

import java.util.List;

public class CommandGameMode extends CommandBase {
    @Override
    public String getCommandName() {
        return "gamemode";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.gamemode.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException("commands.gamemode.usage");
        } else {
            WorldSettings.GameType worldsettings$gametype = this.getGameModeFromCommand(sender, args[0]);
            EntityPlayer entityplayer = args.length >= 2 ? getPlayer(sender, args[1]) : getCommandSenderAsPlayer(sender);
            entityplayer.setGameType(worldsettings$gametype);
            entityplayer.fallDistance = 0.0F;

            if (sender.getEntityWorld().getGameRules().getBoolean("sendCommandFeedback")) {
                entityplayer.addChatMessage(new ChatComponentTranslation("gameMode.changed"));
            }

            IChatComponent ichatcomponent = new ChatComponentTranslation("gameMode." + worldsettings$gametype.getName());

            if (entityplayer != sender) {
                notifyOperators(sender, this, 1, "commands.gamemode.success.other", entityplayer.getName(), ichatcomponent);
            } else {
                notifyOperators(sender, this, 1, "commands.gamemode.success.self", ichatcomponent);
            }
        }
    }

    protected WorldSettings.GameType getGameModeFromCommand(ICommandSender p_71539_1_, String p_71539_2_) throws CommandException {
        return !p_71539_2_.equalsIgnoreCase(WorldSettings.GameType.SURVIVAL.getName()) && !p_71539_2_.equalsIgnoreCase("s") ? (!p_71539_2_.equalsIgnoreCase(WorldSettings.GameType.CREATIVE.getName()) && !p_71539_2_.equalsIgnoreCase("c") ? (!p_71539_2_.equalsIgnoreCase(WorldSettings.GameType.ADVENTURE.getName()) && !p_71539_2_.equalsIgnoreCase("a") ? (!p_71539_2_.equalsIgnoreCase(WorldSettings.GameType.SPECTATOR.getName()) && !p_71539_2_.equalsIgnoreCase("sp") ? WorldSettings.getGameTypeById(parseInt(p_71539_2_, 0, WorldSettings.GameType.values().length - 2)) : WorldSettings.GameType.SPECTATOR) : WorldSettings.GameType.ADVENTURE) : WorldSettings.GameType.CREATIVE) : WorldSettings.GameType.SURVIVAL;
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "survival", "creative", "adventure", "spectator") : (args.length == 2 ? getListOfStringsMatchingLastWord(args, this.getListOfPlayerUsernames()) : null);
    }

    protected String[] getListOfPlayerUsernames() {
        return MinecraftServer.getServer().getAllUsernames();
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 1;
    }
}
