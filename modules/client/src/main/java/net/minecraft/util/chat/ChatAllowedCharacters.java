package net.minecraft.util.chat;

public class ChatAllowedCharacters {
    public static final char[] ALLOWED_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public static boolean isAllowedCharacter(char character) {
        return character != 167 && character >= 32 && character != 127;
    }

    public static String filterAllowedCharacters(String input) {
        StringBuilder builder = new StringBuilder();

        for (char c0 : input.toCharArray()) {
            if (isAllowedCharacter(c0)) {
                builder.append(c0);
            }
        }

        return builder.toString();
    }
}
