package net.minecraft.world;

public enum Difficulty {
    PEACEFUL(0, "options.difficulty.peaceful"),
    EASY(1, "options.difficulty.easy"),
    NORMAL(2, "options.difficulty.normal"),
    HARD(3, "options.difficulty.hard");

    private static final Difficulty[] DIFFICULTIES = new Difficulty[values().length];
    private final int difficultyId;
    private final String difficultyResourceKey;

    Difficulty(int difficultyIdIn, String difficultyResourceKeyIn) {
        this.difficultyId = difficultyIdIn;
        this.difficultyResourceKey = difficultyResourceKeyIn;
    }

    public int getDifficultyId() {
        return this.difficultyId;
    }

    public static Difficulty getDifficultyEnum(int p_151523_0_) {
        return DIFFICULTIES[p_151523_0_ % DIFFICULTIES.length];
    }

    public String getDifficultyResourceKey() {
        return this.difficultyResourceKey;
    }

    static {
        for (Difficulty enumdifficulty : values()) {
            DIFFICULTIES[enumdifficulty.difficultyId] = enumdifficulty;
        }
    }
}
