package net.minecraft.command;

import java.util.List;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.Difficulty;

public class CommandDifficulty extends CommandBase {
    public String getCommandName() {
        return "difficulty";
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.difficulty.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            throw new WrongUsageException("commands.difficulty.usage");
        } else {
            Difficulty enumdifficulty = this.getDifficultyFromCommand(args[0]);
            MinecraftServer.getServer().setDifficultyForAllWorlds(enumdifficulty);
            notifyOperators(sender, this, "commands.difficulty.success", new ChatComponentTranslation(enumdifficulty.getDifficultyResourceKey()));
        }
    }

    protected Difficulty getDifficultyFromCommand(String p_180531_1_) throws CommandException, NumberInvalidException {
        return !p_180531_1_.equalsIgnoreCase("peaceful") && !p_180531_1_.equalsIgnoreCase("p") ? (!p_180531_1_.equalsIgnoreCase("easy") && !p_180531_1_.equalsIgnoreCase("e") ? (!p_180531_1_.equalsIgnoreCase("normal") && !p_180531_1_.equalsIgnoreCase("n") ? (!p_180531_1_.equalsIgnoreCase("hard") && !p_180531_1_.equalsIgnoreCase("h") ? Difficulty.getDifficultyEnum(parseInt(p_180531_1_, 0, 3)) : Difficulty.HARD) : Difficulty.NORMAL) : Difficulty.EASY) : Difficulty.PEACEFUL;
    }

    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "peaceful", "easy", "normal", "hard") : null;
    }
}
