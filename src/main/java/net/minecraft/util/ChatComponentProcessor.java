package net.minecraft.util;

import net.minecraft.command.CommandException;
import net.minecraft.command.EntityNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.Entity;

import java.util.List;

public class ChatComponentProcessor {
    public static IChatComponent processComponent(ICommandSender commandSender, IChatComponent component, Entity entityIn) throws CommandException {
        IChatComponent ichatcomponent;

        switch (component) {
            case ChatComponentScore chatcomponentscore -> {
                String s = chatcomponentscore.getName();

                if (PlayerSelector.hasArguments(s)) {
                    List<Entity> list = PlayerSelector.matchEntities(commandSender, s, Entity.class);

                    if (list.size() != 1) {
                        throw new EntityNotFoundException();
                    }

                    s = list.getFirst().getName();
                }

                ichatcomponent = entityIn != null && s.equals("*") ? new ChatComponentScore(entityIn.getName(), chatcomponentscore.getObjective()) : new ChatComponentScore(s, chatcomponentscore.getObjective());
                ((ChatComponentScore) ichatcomponent).setValue(chatcomponentscore.getUnformattedTextForChat());
            }
            case ChatComponentSelector components -> {
                String s1 = components.getSelector();
                ichatcomponent = PlayerSelector.matchEntitiesToChatComponent(commandSender, s1);

                if (ichatcomponent == null) {
                    ichatcomponent = new ChatComponentText("");
                }
            }
            case ChatComponentText chatComponents ->
                    ichatcomponent = new ChatComponentText(chatComponents.getChatComponentText_TextValue());
            case null, default -> {
                if (!(component instanceof ChatComponentTranslation iChatComponents)) {
                    return component;
                }

                Object[] aobject = iChatComponents.getFormatArgs();

                for (int i = 0; i < aobject.length; ++i) {
                    Object object = aobject[i];

                    if (object instanceof IChatComponent iChatComponent) {
                        aobject[i] = processComponent(commandSender, iChatComponent, entityIn);
                    }
                }

                ichatcomponent = new ChatComponentTranslation(iChatComponents.getKey(), aobject);
            }
        }

        ChatStyle chatstyle = component.getChatStyle();

        if (chatstyle != null) {
            ichatcomponent.setChatStyle(chatstyle.createShallowCopy());
        }

        for (IChatComponent ichatcomponent1 : component.getSiblings()) {
            ichatcomponent.appendSibling(processComponent(commandSender, ichatcomponent1, entityIn));
        }

        return ichatcomponent;
    }
}
