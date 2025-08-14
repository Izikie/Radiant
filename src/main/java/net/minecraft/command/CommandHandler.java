package net.minecraft.command;

import net.minecraft.command.client.CommandResultStats;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.Formatting;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.Map.Entry;

public class CommandHandler implements ICommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    private final Map<String, ICommand> commandMap = new HashMap<>();
    private final Set<ICommand> commandSet = new HashSet<>();

    public int executeCommand(ICommandSender sender, String rawCommand) {
        rawCommand = rawCommand.trim().toLowerCase(); // BUGFIX: Commands case-sensitive

        if (rawCommand.startsWith("/")) {
            rawCommand = rawCommand.substring(1);
        }

        String[] astring = rawCommand.split(" ");
        String s = astring[0];
        astring = dropFirstString(astring);
        ICommand icommand = this.commandMap.get(s);
        int i = this.getUsernameIndex(icommand, astring);
        int j = 0;

        if (icommand == null) {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("commands.generic.notFound");
            chatcomponenttranslation.getChatStyle().setColor(Formatting.RED);
            sender.addChatMessage(chatcomponenttranslation);
        } else if (icommand.canCommandSenderUseCommand(sender)) {
            if (i > -1) {
                List<Entity> list = PlayerSelector.matchEntities(sender, astring[i], Entity.class);
                String s1 = astring[i];
                sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, list.size());

                for (Entity entity : list) {
                    astring[i] = entity.getUniqueID().toString();

                    if (this.tryExecute(sender, astring, icommand, rawCommand)) {
                        ++j;
                    }
                }

                astring[i] = s1;
            } else {
                sender.setCommandStat(CommandResultStats.Type.AFFECTED_ENTITIES, 1);

                if (this.tryExecute(sender, astring, icommand, rawCommand)) {
                    ++j;
                }
            }
        } else {
            ChatComponentTranslation chatcomponenttranslation1 = new ChatComponentTranslation("commands.generic.permission");
            chatcomponenttranslation1.getChatStyle().setColor(Formatting.RED);
            sender.addChatMessage(chatcomponenttranslation1);
        }

        sender.setCommandStat(CommandResultStats.Type.SUCCESS_COUNT, j);
        return j;
    }

    protected boolean tryExecute(ICommandSender sender, String[] args, ICommand command, String input) {
        try {
            command.processCommand(sender, args);
            return true;
        } catch (WrongUsageException exception) {
            ChatComponentTranslation chatcomponenttranslation2 = new ChatComponentTranslation("commands.generic.usage", new ChatComponentTranslation(exception.getMessage(), exception.getErrorObjects()));
            chatcomponenttranslation2.getChatStyle().setColor(Formatting.RED);
            sender.addChatMessage(chatcomponenttranslation2);
        } catch (CommandException exception) {
            ChatComponentTranslation chatcomponenttranslation1 = new ChatComponentTranslation(exception.getMessage(), exception.getErrorObjects());
            chatcomponenttranslation1.getChatStyle().setColor(Formatting.RED);
            sender.addChatMessage(chatcomponenttranslation1);
        } catch (Throwable throwable) {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("commands.generic.exception");
            chatcomponenttranslation.getChatStyle().setColor(Formatting.RED);
            sender.addChatMessage(chatcomponenttranslation);
            LOGGER.warn("Couldn't process command: '{}'", input);
        }

        return false;
    }

    public ICommand registerCommand(ICommand command) {
        this.commandMap.put(command.getCommandName(), command);
        this.commandSet.add(command);

        for (String s : command.getCommandAliases()) {
            ICommand icommand = this.commandMap.get(s);

            if (icommand == null || !icommand.getCommandName().equals(s)) {
                this.commandMap.put(s, command);
            }
        }

        return command;
    }

    private static String[] dropFirstString(String[] input) {
        String[] astring = new String[input.length - 1];
        System.arraycopy(input, 1, astring, 0, input.length - 1);
        return astring;
    }

    public List<String> getTabCompletionOptions(ICommandSender sender, String input, BlockPos pos) {
        String[] astring = input.split(" ", -1);
        String s = astring[0];

        if (astring.length == 1) {
            List<String> list = new ArrayList<>();

            for (Entry<String, ICommand> entry : this.commandMap.entrySet()) {
                if (CommandBase.doesStringStartWith(s, entry.getKey()) && entry.getValue().canCommandSenderUseCommand(sender)) {
                    list.add(entry.getKey());
                }
            }

            return list;
        } else {
            if (astring.length > 1) {
                ICommand icommand = this.commandMap.get(s);

                if (icommand != null && icommand.canCommandSenderUseCommand(sender)) {
                    return icommand.addTabCompletionOptions(sender, dropFirstString(astring), pos);
                }
            }

            return null;
        }
    }

    public List<ICommand> getPossibleCommands(ICommandSender sender) {
        List<ICommand> list = new ArrayList<>();

        for (ICommand icommand : this.commandSet) {
            if (icommand.canCommandSenderUseCommand(sender)) {
                list.add(icommand);
            }
        }

        return list;
    }

    public Map<String, ICommand> getCommands() {
        return this.commandMap;
    }

    private int getUsernameIndex(ICommand command, String[] args) {
        if (command != null) {
            for (int i = 0; i < args.length; ++i) {
                if (command.isUsernameIndex(args, i) && PlayerSelector.matchesMultiplePlayers(args[i])) {
                    return i;
                }
            }

        }
        return -1;
    }
}
