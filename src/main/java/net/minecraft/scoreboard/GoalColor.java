package net.minecraft.scoreboard;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Formatting;

import java.util.List;

public class GoalColor implements IScoreObjectiveCriteria {
    private final String goalName;

    public GoalColor(String p_i45549_1_, Formatting p_i45549_2_) {
        this.goalName = p_i45549_1_ + p_i45549_2_.getFriendlyName();
        IScoreObjectiveCriteria.INSTANCES.put(this.goalName, this);
    }

    public String getName() {
        return this.goalName;
    }

    public int setScore(List<EntityPlayer> p_96635_1_) {
        return 0;
    }

    public boolean isReadOnly() {
        return false;
    }

    public EnumRenderType getRenderType() {
        return EnumRenderType.INTEGER;
    }
}
