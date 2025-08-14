package net.minecraft.entity.damage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.util.StatCollector;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.chat.IChatComponent;
import net.minecraft.world.Explosion;

public class DamageSource {
    public static final DamageSource IN_FIRE = (new DamageSource("inFire")).setFireDamage();
    public static final DamageSource LIGHTNING_BOLT = new DamageSource("lightningBolt");
    public static final DamageSource ON_FIRE = (new DamageSource("onFire")).setDamageBypassesArmor().setFireDamage();
    public static final DamageSource LAVA = (new DamageSource("lava")).setFireDamage();
    public static final DamageSource IN_WALL = (new DamageSource("inWall")).setDamageBypassesArmor();
    public static final DamageSource DROWN = (new DamageSource("drown")).setDamageBypassesArmor();
    public static final DamageSource STARVE = (new DamageSource("starve")).setDamageBypassesArmor().setDamageIsAbsolute();
    public static final DamageSource CACTUS = new DamageSource("cactus");
    public static final DamageSource FALL = (new DamageSource("fall")).setDamageBypassesArmor();
    public static final DamageSource OUT_OF_WORLD = (new DamageSource("outOfWorld")).setDamageBypassesArmor().setDamageAllowedInCreativeMode();
    public static final DamageSource GENERIC = (new DamageSource("generic")).setDamageBypassesArmor();
    public static final DamageSource MAGIC = (new DamageSource("magic")).setDamageBypassesArmor().setMagicDamage();
    public static final DamageSource WITHER = (new DamageSource("wither")).setDamageBypassesArmor();
    public static final DamageSource ANVIL = new DamageSource("anvil");
    public static final DamageSource FALLING_BLOCK = new DamageSource("fallingBlock");
    private boolean isUnblockable;
    private boolean isDamageAllowedInCreativeMode;
    private boolean damageIsAbsolute;
    private float hungerDamage = 0.3F;
    private boolean fireDamage;
    private boolean projectile;
    private boolean difficultyScaled;
    private boolean magicDamage;
    private boolean explosion;
    public final String damageType;

    public static DamageSource causeMobDamage(EntityLivingBase mob) {
        return new EntityDamageSource("mob", mob);
    }

    public static DamageSource causePlayerDamage(EntityPlayer player) {
        return new EntityDamageSource("player", player);
    }

    public static DamageSource causeArrowDamage(EntityArrow arrow, Entity indirectEntityIn) {
        return (new EntityDamageSourceIndirect("arrow", arrow, indirectEntityIn)).setProjectile();
    }

    public static DamageSource causeFireballDamage(EntityFireball fireball, Entity indirectEntityIn) {
        return indirectEntityIn == null ? (new EntityDamageSourceIndirect("onFire", fireball, fireball)).setFireDamage().setProjectile() : (new EntityDamageSourceIndirect("fireball", fireball, indirectEntityIn)).setFireDamage().setProjectile();
    }

    public static DamageSource causeThrownDamage(Entity source, Entity indirectEntityIn) {
        return (new EntityDamageSourceIndirect("thrown", source, indirectEntityIn)).setProjectile();
    }

    public static DamageSource causeIndirectMagicDamage(Entity source, Entity indirectEntityIn) {
        return (new EntityDamageSourceIndirect("indirectMagic", source, indirectEntityIn)).setDamageBypassesArmor().setMagicDamage();
    }

    public static DamageSource causeThornsDamage(Entity source) {
        return (new EntityDamageSource("thorns", source)).setIsThornsDamage().setMagicDamage();
    }

    public static DamageSource setExplosionSource(Explosion explosionIn) {
        return explosionIn != null && explosionIn.getExplosivePlacedBy() != null ? (new EntityDamageSource("explosion.player", explosionIn.getExplosivePlacedBy())).setDifficultyScaled().setExplosion() : (new DamageSource("explosion")).setDifficultyScaled().setExplosion();
    }

    public boolean isProjectile() {
        return this.projectile;
    }

    public DamageSource setProjectile() {
        this.projectile = true;
        return this;
    }

    public boolean isExplosion() {
        return this.explosion;
    }

    public DamageSource setExplosion() {
        this.explosion = true;
        return this;
    }

    public boolean isUnblockable() {
        return this.isUnblockable;
    }

    public float getHungerDamage() {
        return this.hungerDamage;
    }

    public boolean canHarmInCreative() {
        return this.isDamageAllowedInCreativeMode;
    }

    public boolean isDamageAbsolute() {
        return this.damageIsAbsolute;
    }

    protected DamageSource(String damageTypeIn) {
        this.damageType = damageTypeIn;
    }

    public Entity getSourceOfDamage() {
        return this.getEntity();
    }

    public Entity getEntity() {
        return null;
    }

    protected DamageSource setDamageBypassesArmor() {
        this.isUnblockable = true;
        this.hungerDamage = 0.0F;
        return this;
    }

    protected DamageSource setDamageAllowedInCreativeMode() {
        this.isDamageAllowedInCreativeMode = true;
        return this;
    }

    protected DamageSource setDamageIsAbsolute() {
        this.damageIsAbsolute = true;
        this.hungerDamage = 0.0F;
        return this;
    }

    protected DamageSource setFireDamage() {
        this.fireDamage = true;
        return this;
    }

    public IChatComponent getDeathMessage(EntityLivingBase entityLivingBaseIn) {
        EntityLivingBase entitylivingbase = entityLivingBaseIn.getAttackingEntity();
        String s = "death.attack." + this.damageType;
        String s1 = s + ".player";
        return entitylivingbase != null && StatCollector.canTranslate(s1) ? new ChatComponentTranslation(s1, entityLivingBaseIn.getDisplayName(), entitylivingbase.getDisplayName()) : new ChatComponentTranslation(s, entityLivingBaseIn.getDisplayName());
    }

    public boolean isFireDamage() {
        return this.fireDamage;
    }

    public String getDamageType() {
        return this.damageType;
    }

    public DamageSource setDifficultyScaled() {
        this.difficultyScaled = true;
        return this;
    }

    public boolean isDifficultyScaled() {
        return this.difficultyScaled;
    }

    public boolean isMagicDamage() {
        return this.magicDamage;
    }

    public DamageSource setMagicDamage() {
        this.magicDamage = true;
        return this;
    }

    public boolean isCreativePlayer() {
        Entity entity = this.getEntity();
        return entity instanceof EntityPlayer entityPlayer && entityPlayer.capabilities.isCreativeMode;
    }
}
