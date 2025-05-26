package net.minecraft.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;

import java.util.ArrayList;
import java.util.List;

public class EntitySenses {
    final EntityLiving entityObj;
    final List<Entity> seenEntities = new ArrayList<>();
    final List<Entity> unseenEntities = new ArrayList<>();

    public EntitySenses(EntityLiving entityObjIn) {
        this.entityObj = entityObjIn;
    }

    public void clearSensingCache() {
        this.seenEntities.clear();
        this.unseenEntities.clear();
    }

    public boolean canSee(Entity entityIn) {
        if (this.seenEntities.contains(entityIn)) {
            return true;
        } else if (this.unseenEntities.contains(entityIn)) {
            return false;
        } else {
            boolean flag = this.entityObj.canEntityBeSeen(entityIn);

            if (flag) {
                this.seenEntities.add(entityIn);
            } else {
                this.unseenEntities.add(entityIn);
            }

            return flag;
        }
    }
}
