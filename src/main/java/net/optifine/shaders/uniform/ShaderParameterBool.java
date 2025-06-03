package net.optifine.shaders.uniform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.optifine.expr.ExpressionType;
import net.optifine.expr.IExpressionBool;

public enum ShaderParameterBool implements IExpressionBool {
    IS_ALIVE("is_alive"),
    IS_BURNING("is_burning"),
    IS_CHILD("is_child"),
    IS_HURT("is_hurt"),
    IS_IN_LAVA("is_in_lava"),
    IS_IN_WATER("is_in_water"),
    IS_INVISIBLE("is_invisible"),
    IS_ON_GROUND("is_on_ground"),
    IS_RIDDEN("is_ridden"),
    IS_RIDING("is_riding"),
    IS_SNEAKING("is_sneaking"),
    IS_SPRINTING("is_sprinting"),
    IS_WET("is_wet");

    private static final ShaderParameterBool[] VALUES = values();
    private final String name;

    ShaderParameterBool(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public ExpressionType getExpressionType() {
        return ExpressionType.BOOL;
    }

    public boolean eval() {
        Entity entity = Minecraft.getMinecraft().getRenderViewEntity();

        if (entity instanceof EntityLivingBase entitylivingbase) {

            return switch (this) {
                case IS_ALIVE -> entitylivingbase.isEntityAlive();
                case IS_BURNING -> entitylivingbase.isBurning();
                case IS_CHILD -> entitylivingbase.isChild();
                case IS_HURT -> entitylivingbase.hurtTime > 0;
                case IS_IN_LAVA -> entitylivingbase.isInLava();
                case IS_IN_WATER -> entitylivingbase.isInWater();
                case IS_INVISIBLE -> entitylivingbase.isInvisible();
                case IS_ON_GROUND -> entitylivingbase.onGround;
                case IS_RIDDEN -> entitylivingbase.riddenByEntity != null;
                case IS_RIDING -> entitylivingbase.isRiding();
                case IS_SNEAKING -> entitylivingbase.isSneaking();
                case IS_SPRINTING -> entitylivingbase.isSprinting();
                case IS_WET -> entitylivingbase.isWet();
            };
        }

        return false;
    }

    public static ShaderParameterBool parse(String str) {
        if (str != null) {
            for (ShaderParameterBool parameterBool : VALUES) {
                if (parameterBool.getName().equals(str))
                    return parameterBool;
            }

        }
        return null;
    }
}
