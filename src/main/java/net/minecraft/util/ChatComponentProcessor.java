package net.minecraft.util;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.EntityNotFoundException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.Entity;

public class ChatComponentProcessor {
    public static IChatComponent processComponent(ICommandSender commandSender, IChatComponent component, Entity entityIn) throws CommandException {
        IChatComponent ichatcomponent;

        if (component instanceof ChatComponentScore chatcomponentscore) {
            String s = chatcomponentscore.getName();

            if (PlayerSelector.hasArguments(s)) {
                List<Entity> list = PlayerSelector.matchEntities(commandSender, s, Entity.class);

                if (list.size() != 1) {
                    throw new EntityNotFoundException();
                }

                s = list.get(0).getName();
            }

            ichatcomponent = entityIn != null && s.equals("*") ? new ChatComponentScore(entityIn.getName(), chatcomponentscore.getObjective()) : new ChatComponentScore(s, chatcomponentscore.getObjective());
            ((ChatComponentScore) ichatcomponent).setValue(chatcomponentscore.getUnformattedTextForChat());
        } else if (component instanceof ChatComponentSelector components) {
            String s1 = components.getSelector();
            ichatcomponent = PlayerSelector.matchEntitiesToChatComponent(commandSender, s1);

            if (ichatcomponent == null) {
                ichatcomponent = new ChatComponentText("");
            }
        } else if (component instanceof ChatComponentText chatComponents) {
            ichatcomponent = new ChatComponentText(chatComponents.getChatComponentText_TextValue());
        } else {
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
