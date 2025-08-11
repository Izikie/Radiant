package net.radiant.logger;

public enum AnsiStyle {
    RESET(0, 0),
    BOLD(1, 21),
    FAINT(2, 22),
    ITALIC(3, 23),
    UNDERLINE(4, 24),
    BLINK(5, 25),
    REVERSE(7, 27),
    BLACK(30, 39),
    RED(31, 39),
    GREEN(32, 39),
    YELLOW(33, 39),
    BLUE(34, 39),
    MAGENTA(35, 39),
    CYAN(36, 39),
    WHITE(37, 39),
    BG_BLACK(40, 49),
    BG_RED(41, 49),
    BG_GREEN(42, 49),
    BG_YELLOW(43, 49),
    BG_BLUE(44, 49),
    BG_MAGENTA(45, 49),
    BG_CYAN(46, 49),
    BG_WHITE(47, 49),
    ;

    private static final String CSI = "\u001B[";

    private final int startCode, endCode;

    AnsiStyle(final int startCode, final int endCode) {
        this.startCode = startCode;
        this.endCode = endCode;
    }

    public String on() {
        return CSI + startCode + "m";
    }

    public String off() {
        return CSI + endCode + "m";
    }

    public String encase(String str) {
        return on() + str + off();
    }

    @Override
    public String toString() {
        return on();
    }
}
