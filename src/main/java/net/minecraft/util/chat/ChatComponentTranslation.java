package net.minecraft.util.chat;

import com.google.common.collect.Iterators;
import net.minecraft.util.StatCollector;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatComponentTranslation extends ChatComponentStyle {
    private final String key;
    private final Object[] formatArgs;
    private final Object syncLock = new Object();
    private long lastTranslationUpdateTimeInMilliseconds = -1L;
    final List<IChatComponent> children = new ArrayList<>();
    public static final Pattern STRING_VARIABLE_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    public ChatComponentTranslation(String translationKey, Object... args) {
        this.key = translationKey;
        this.formatArgs = args;

        for (Object object : args) {
            if (object instanceof IChatComponent iChatComponent) {
                iChatComponent.getChatStyle().setParentStyle(this.getChatStyle());
            }
        }
    }

    synchronized void ensureInitialized() {
        synchronized (this.syncLock) {
            long i = StatCollector.getLastTranslationUpdateTimeInMilliseconds();

            if (i == this.lastTranslationUpdateTimeInMilliseconds) {
                return;
            }

            this.lastTranslationUpdateTimeInMilliseconds = i;
            this.children.clear();
        }

        try {
            this.initializeFromFormat(StatCollector.translateToLocal(this.key));
        } catch (ChatComponentTranslationFormatException exception) {
            this.children.clear();

            try {
                this.initializeFromFormat(StatCollector.translateToFallback(this.key));
            } catch (ChatComponentTranslationFormatException translationException) {
                throw exception;
            }
        }
    }

    protected void initializeFromFormat(String format) {
        boolean flag = false;
        Matcher matcher = STRING_VARIABLE_PATTERN.matcher(format);
        int i = 0;
        int j = 0;

        try {
            int l;

            for (; matcher.find(j); j = l) {
                int k = matcher.start();
                l = matcher.end();

                if (k > j) {
                    ChatComponentText chatcomponenttext = new ChatComponentText(String.format(format.substring(j, k)));
                    chatcomponenttext.getChatStyle().setParentStyle(this.getChatStyle());
                    this.children.add(chatcomponenttext);
                }

                String s2 = matcher.group(2);
                String s = format.substring(k, l);

                if ("%".equals(s2) && "%%".equals(s)) {
                    ChatComponentText chatcomponenttext2 = new ChatComponentText("%");
                    chatcomponenttext2.getChatStyle().setParentStyle(this.getChatStyle());
                    this.children.add(chatcomponenttext2);
                } else {
                    if (!"s".equals(s2)) {
                        throw new ChatComponentTranslationFormatException(this, "Unsupported format: '" + s + "'");
                    }

                    String s1 = matcher.group(1);
                    int i1 = s1 != null ? Integer.parseInt(s1) - 1 : i++;

                    if (i1 < this.formatArgs.length) {
                        this.children.add(this.getFormatArgumentAsComponent(i1));
                    }
                }
            }

            if (j < format.length()) {
                ChatComponentText chatcomponenttext1 = new ChatComponentText(String.format(format.substring(j)));
                chatcomponenttext1.getChatStyle().setParentStyle(this.getChatStyle());
                this.children.add(chatcomponenttext1);
            }
        } catch (IllegalFormatException exception) {
            throw new ChatComponentTranslationFormatException(this, exception);
        }
    }

    private IChatComponent getFormatArgumentAsComponent(int index) {
        if (index >= this.formatArgs.length) {
            throw new ChatComponentTranslationFormatException(this, index);
        } else {
            Object object = this.formatArgs[index];
            IChatComponent ichatcomponent;

            if (object instanceof IChatComponent iChatComponent) {
                ichatcomponent = iChatComponent;
            } else {
                ichatcomponent = new ChatComponentText(object == null ? "null" : object.toString());
                ichatcomponent.getChatStyle().setParentStyle(this.getChatStyle());
            }

            return ichatcomponent;
        }
    }

    @Override
    public IChatComponent setChatStyle(ChatStyle style) {
        super.setChatStyle(style);

        for (Object object : this.formatArgs) {
            if (object instanceof IChatComponent iChatComponent) {
                iChatComponent.getChatStyle().setParentStyle(this.getChatStyle());
            }
        }

        if (this.lastTranslationUpdateTimeInMilliseconds > -1L) {
            for (IChatComponent ichatcomponent : this.children) {
                ichatcomponent.getChatStyle().setParentStyle(style);
            }
        }

        return this;
    }

    @Override
    public Iterator<IChatComponent> iterator() {
        this.ensureInitialized();
        return Iterators.concat(createDeepCopyIterator(this.children), createDeepCopyIterator(this.siblings));
    }

    @Override
    public String getUnformattedTextForChat() {
        this.ensureInitialized();
        StringBuilder stringbuilder = new StringBuilder();

        for (IChatComponent ichatcomponent : this.children) {
            stringbuilder.append(ichatcomponent.getUnformattedTextForChat());
        }

        return stringbuilder.toString();
    }

    @Override
    public ChatComponentTranslation createCopy() {
        Object[] aobject = new Object[this.formatArgs.length];

        for (int i = 0; i < this.formatArgs.length; ++i) {
            if (this.formatArgs[i] instanceof IChatComponent iChatComponent) {
                aobject[i] = iChatComponent.createCopy();
            } else {
                aobject[i] = this.formatArgs[i];
            }
        }

        ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation(this.key, aobject);
        chatcomponenttranslation.setChatStyle(this.getChatStyle().createShallowCopy());

        for (IChatComponent ichatcomponent : this.getSiblings()) {
            chatcomponenttranslation.appendSibling(ichatcomponent.createCopy());
        }

        return chatcomponenttranslation;
    }

    public boolean equals(Object p_equals_1_) {
        if (this == p_equals_1_) {
            return true;
        } else if (!(p_equals_1_ instanceof ChatComponentTranslation chatcomponenttranslation)) {
            return false;
        } else {
            return Arrays.equals(this.formatArgs, chatcomponenttranslation.formatArgs) && this.key.equals(chatcomponenttranslation.key) && super.equals(p_equals_1_);
        }
    }

    public int hashCode() {
        int i = super.hashCode();
        i = 31 * i + this.key.hashCode();
        i = 31 * i + Arrays.hashCode(this.formatArgs);
        return i;
    }

    public String toString() {
        return "TranslatableComponent{key='" + this.key + '\'' + ", args=" + Arrays.toString(this.formatArgs) + ", siblings=" + this.siblings + ", style=" + this.getChatStyle() + '}';
    }

    public String getKey() {
        return this.key;
    }

    public Object[] getFormatArgs() {
        return this.formatArgs;
    }
}
