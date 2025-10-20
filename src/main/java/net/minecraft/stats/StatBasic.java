package net.minecraft.stats;

import net.minecraft.util.chat.IChatComponent;

public class StatBasic extends StatBase {
    public StatBasic(String statIdIn, IChatComponent statNameIn, IStatType typeIn) {
        super(statIdIn, statNameIn, typeIn);
    }

    public StatBasic(String statIdIn, IChatComponent statNameIn) {
        super(statIdIn, statNameIn);
    }

    @Override
    public StatBase registerStat() {
        super.registerStat();
        StatList.GENERAL_STATS.add(this);
        return this;
    }
}
