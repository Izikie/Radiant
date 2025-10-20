package net.minecraft.command.client;

import com.google.gson.JsonParseException;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.chat.ChatComponentProcessor;
import net.minecraft.util.chat.IChatComponent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

public class CommandTitle extends CommandBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandTitle.class);

    @Override
    public String getCommandName() {
        return "title";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "commands.title.usage";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 2) {
            throw new WrongUsageException("commands.title.usage");
        } else {
            if (args.length < 3) {
                if ("title".equals(args[1]) || "subtitle".equals(args[1])) {
                    throw new WrongUsageException("commands.title.usage.title");
                }

                if ("times".equals(args[1])) {
                    throw new WrongUsageException("commands.title.usage.times");
                }
            }

            EntityPlayerMP entityplayermp = getPlayer(sender, args[0]);
            S45PacketTitle.Type s45packettitle$type = S45PacketTitle.Type.byName(args[1]);

            if (s45packettitle$type != S45PacketTitle.Type.CLEAR && s45packettitle$type != S45PacketTitle.Type.RESET) {
                if (s45packettitle$type == S45PacketTitle.Type.TIMES) {
                    if (args.length != 5) {
                        throw new WrongUsageException("commands.title.usage");
                    } else {
                        int i = parseInt(args[2]);
                        int j = parseInt(args[3]);
                        int k = parseInt(args[4]);
                        S45PacketTitle s45packettitle2 = new S45PacketTitle(i, j, k);
                        entityplayermp.playerNetServerHandler.sendPacket(s45packettitle2);
                        notifyOperators(sender, this, "commands.title.success");
                    }
                } else if (args.length < 3) {
                    throw new WrongUsageException("commands.title.usage");
                } else {
                    String s = buildString(args, 2);
                    IChatComponent ichatcomponent;

                    try {
                        ichatcomponent = IChatComponent.Serializer.jsonToComponent(s);
                    } catch (JsonParseException exception) {
                        Throwable throwable = ExceptionUtils.getRootCause(exception);
                        throw new SyntaxErrorException("commands.tellraw.jsonException", throwable == null ? "" : throwable.getMessage());
                    }

                    S45PacketTitle s45packettitle1 = new S45PacketTitle(s45packettitle$type, ChatComponentProcessor.processComponent(sender, ichatcomponent, entityplayermp));
                    entityplayermp.playerNetServerHandler.sendPacket(s45packettitle1);
                    notifyOperators(sender, this, "commands.title.success");
                }
            } else if (args.length != 2) {
                throw new WrongUsageException("commands.title.usage");
            } else {
                S45PacketTitle s45packettitle = new S45PacketTitle(s45packettitle$type, null);
                entityplayermp.playerNetServerHandler.sendPacket(s45packettitle);
                notifyOperators(sender, this, "commands.title.success");
            }
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames()) : (args.length == 2 ? getListOfStringsMatchingLastWord(args, S45PacketTitle.Type.getNames()) : null);
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return index == 0;
    }
}
