package net.minecraft.potion;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.src.Config;
import net.optifine.CustomColors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PotionHelper {
    public static final String UNUSED_STRING = null;
    public static final String SUGAR_EFFECT = "-0+1-2-3&4-4+13";
    public static final String GHAST_TEAR_EFFECT = "+0-1-2-3&4-4+13";
    public static final String SPIDER_EYE_EFFECT = "-0-1+2-3&4-4+13";
    public static final String FERMENTED_SPIDER_EYE_EFFECT = "-0+3-4+13";
    public static final String SPECKLED_MELON_EFFECT = "+0-1+2-3&4-4+13";
    public static final String BLAZE_POWDER_EFFECT = "+0-1-2+3&4-4+13";
    public static final String MAGMA_CREAM_EFFECT = "+0+1-2-3&4-4+13";
    public static final String REDSTONE_EFFECT = "-5+6-7";
    public static final String GLOWSTONE_EFFECT = "+5-6-7";
    public static final String GUNPOWDER_EFFECT = "+14&13-13";
    public static final String GOLDEN_CARROT_EFFECT = "-0+1+2-3+13&4-4";
    public static final String PUFFERFISH_EFFECT = "+0-1+2+3+13&4-4";
    public static final String RABBIT_FOOT_EFFECT = "+0+1-2+3&4-4+13";
    private static final Int2ObjectOpenHashMap<String> POTION_REQUIREMENTS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<String> POTION_AMPLIFIERS = new Int2ObjectOpenHashMap<>();
    private static final Int2IntOpenHashMap DATAVALUE_COLORS = new Int2IntOpenHashMap();
    private static final String[] POTION_PREFIXES = new String[]{"potion.prefix.mundane", "potion.prefix.uninteresting", "potion.prefix.bland", "potion.prefix.clear", "potion.prefix.milky", "potion.prefix.diffuse", "potion.prefix.artless", "potion.prefix.thin", "potion.prefix.awkward", "potion.prefix.flat", "potion.prefix.bulky", "potion.prefix.bungling", "potion.prefix.buttered", "potion.prefix.smooth", "potion.prefix.suave", "potion.prefix.debonair", "potion.prefix.thick", "potion.prefix.elegant", "potion.prefix.fancy", "potion.prefix.charming", "potion.prefix.dashing", "potion.prefix.refined", "potion.prefix.cordial", "potion.prefix.sparkling", "potion.prefix.potent", "potion.prefix.foul", "potion.prefix.odorless", "potion.prefix.rank", "potion.prefix.harsh", "potion.prefix.acrid", "potion.prefix.gross", "potion.prefix.stinky"};

    public static boolean checkFlag(int p_77914_0_, int p_77914_1_) {
        return (p_77914_0_ & 1 << p_77914_1_) != 0;
    }

    private static int isFlagSet(int p_77910_0_, int p_77910_1_) {
        return checkFlag(p_77910_0_, p_77910_1_) ? 1 : 0;
    }

    private static int isFlagUnset(int p_77916_0_, int p_77916_1_) {
        return checkFlag(p_77916_0_, p_77916_1_) ? 0 : 1;
    }

    public static int getPotionPrefixIndex(int dataValue) {
        return getPotionPrefixIndexFlags(dataValue, 5, 4, 3, 2, 1);
    }

    public static int calcPotionLiquidColor(Collection<PotionEffect> p_77911_0_) {
        int i = 3694022;

        if (p_77911_0_ != null && !p_77911_0_.isEmpty()) {
            float f = 0.0F;
            float f1 = 0.0F;
            float f2 = 0.0F;
            float f3 = 0.0F;

            for (PotionEffect potioneffect : p_77911_0_) {
                if (potioneffect.getIsShowParticles()) {
                    int j = Potion.POTION_TYPES[potioneffect.getPotionID()].getLiquidColor();

                    if (Config.isCustomColors()) {
                        j = CustomColors.getPotionColor(potioneffect.getPotionID(), j);
                    }

                    for (int k = 0; k <= potioneffect.getAmplifier(); ++k) {
                        f += (j >> 16 & 255) / 255.0F;
                        f1 += (j >> 8 & 255) / 255.0F;
                        f2 += (j & 255) / 255.0F;
                        ++f3;
                    }
                }
            }

            if (f3 == 0.0F) {
                return 0;
            } else {
                f = f / f3 * 255.0F;
                f1 = f1 / f3 * 255.0F;
                f2 = f2 / f3 * 255.0F;
                return (int) f << 16 | (int) f1 << 8 | (int) f2;
            }
        } else {
            return Config.isCustomColors() ? CustomColors.getPotionColor(0, i) : i;
        }
    }

    public static boolean getAreAmbient(Collection<PotionEffect> potionEffects) {
        for (PotionEffect potioneffect : potionEffects) {
            if (!potioneffect.getIsAmbient()) {
                return false;
            }
        }

        return true;
    }

    public static int getLiquidColor(int dataValue, boolean bypassCache) {
        if (!bypassCache) {
            if (DATAVALUE_COLORS.containsKey(dataValue)) {
                return DATAVALUE_COLORS.get(dataValue);
            } else {
                int i = calcPotionLiquidColor(getPotionEffects(dataValue, false));
                DATAVALUE_COLORS.put(dataValue, i);
                return i;
            }
        } else {
            return calcPotionLiquidColor(getPotionEffects(dataValue, true));
        }
    }

    public static String getPotionPrefix(int dataValue) {
        int i = getPotionPrefixIndex(dataValue);
        return POTION_PREFIXES[i];
    }

    private static int getPotionEffect(boolean p_77904_0_, boolean p_77904_1_, boolean p_77904_2_, int p_77904_3_, int p_77904_4_, int p_77904_5_, int p_77904_6_) {
        int i = 0;

        if (p_77904_0_) {
            i = isFlagUnset(p_77904_6_, p_77904_4_);
        } else if (p_77904_3_ != -1) {
            if (p_77904_3_ == 0 && countSetFlags(p_77904_6_) == p_77904_4_) {
                i = 1;
            } else if (p_77904_3_ == 1 && countSetFlags(p_77904_6_) > p_77904_4_) {
                i = 1;
            } else if (p_77904_3_ == 2 && countSetFlags(p_77904_6_) < p_77904_4_) {
                i = 1;
            }
        } else {
            i = isFlagSet(p_77904_6_, p_77904_4_);
        }

        if (p_77904_1_) {
            i *= p_77904_5_;
        }

        if (p_77904_2_) {
            i *= -1;
        }

        return i;
    }

    private static int countSetFlags(int p_77907_0_) {
        int i;

        for (i = 0; p_77907_0_ > 0; ++i) {
            p_77907_0_ &= p_77907_0_ - 1;
        }

        return i;
    }

    private static int parsePotionEffects(String p_77912_0_, int p_77912_1_, int p_77912_2_, int p_77912_3_) {
        if (p_77912_1_ < p_77912_0_.length() && p_77912_2_ >= 0 && p_77912_1_ < p_77912_2_) {
            int i = p_77912_0_.indexOf(124, p_77912_1_);

            if (i >= 0 && i < p_77912_2_) {
                int l1 = parsePotionEffects(p_77912_0_, p_77912_1_, i - 1, p_77912_3_);

                if (l1 > 0) {
                    return l1;
                } else {
                    int j2 = parsePotionEffects(p_77912_0_, i + 1, p_77912_2_, p_77912_3_);
                    return j2 > 0 ? j2 : 0;
                }
            } else {
                int j = p_77912_0_.indexOf(38, p_77912_1_);

                if (j >= 0 && j < p_77912_2_) {
                    int i2 = parsePotionEffects(p_77912_0_, p_77912_1_, j - 1, p_77912_3_);

                    if (i2 <= 0) {
                        return 0;
                    } else {
                        int k2 = parsePotionEffects(p_77912_0_, j + 1, p_77912_2_, p_77912_3_);
                        return k2 <= 0 ? 0 : (i2 > k2 ? i2 : k2);
                    }
                } else {
                    boolean flag = false;
                    boolean flag1 = false;
                    boolean flag2 = false;
                    boolean flag3 = false;
                    boolean flag4 = false;
                    int k = -1;
                    int l = 0;
                    int i1 = 0;
                    int j1 = 0;

                    for (int k1 = p_77912_1_; k1 < p_77912_2_; ++k1) {
                        char c0 = p_77912_0_.charAt(k1);

                        if (c0 >= 48 && c0 <= 57) {
                            if (flag) {
                                i1 = c0 - 48;
                                flag1 = true;
                            } else {
                                l = l * 10;
                                l = l + (c0 - 48);
                                flag2 = true;
                            }
                        } else if (c0 == 42) {
                            flag = true;
                        } else if (c0 == 33) {
                            if (flag2) {
                                j1 += getPotionEffect(flag3, flag1, flag4, k, l, i1, p_77912_3_);
                                flag4 = false;
                                flag = false;
                                flag1 = false;
                                flag2 = false;
                                i1 = 0;
                                l = 0;
                                k = -1;
                            }

                            flag3 = true;
                        } else if (c0 == 45) {
                            if (flag2) {
                                j1 += getPotionEffect(flag3, flag1, flag4, k, l, i1, p_77912_3_);
                                flag3 = false;
                                flag = false;
                                flag1 = false;
                                flag2 = false;
                                i1 = 0;
                                l = 0;
                                k = -1;
                            }

                            flag4 = true;
                        } else if (c0 != 61 && c0 != 60 && c0 != 62) {
                            if (c0 == 43 && flag2) {
                                j1 += getPotionEffect(flag3, flag1, flag4, k, l, i1, p_77912_3_);
                                flag3 = false;
                                flag4 = false;
                                flag = false;
                                flag1 = false;
                                flag2 = false;
                                i1 = 0;
                                l = 0;
                                k = -1;
                            }
                        } else {
                            if (flag2) {
                                j1 += getPotionEffect(flag3, flag1, flag4, k, l, i1, p_77912_3_);
                                flag3 = false;
                                flag4 = false;
                                flag = false;
                                flag1 = false;
                                flag2 = false;
                                i1 = 0;
                                l = 0;
                                k = -1;
                            }

                            if (c0 == 61) {
                                k = 0;
                            } else if (c0 == 60) {
                                k = 2;
                            } else if (c0 == 62) {
                                k = 1;
                            }
                        }
                    }

                    if (flag2) {
                        j1 += getPotionEffect(flag3, flag1, flag4, k, l, i1, p_77912_3_);
                    }

                    return j1;
                }
            }
        } else {
            return 0;
        }
    }

    public static List<PotionEffect> getPotionEffects(int p_77917_0_, boolean p_77917_1_) {
        List<PotionEffect> list = null;

        for (Potion potion : Potion.POTION_TYPES) {
            if (potion != null && (!potion.isUsable() || p_77917_1_)) {
                String s = POTION_REQUIREMENTS.get(potion.getId());

                if (s != null) {
                    int i = parsePotionEffects(s, 0, s.length(), p_77917_0_);

                    if (i > 0) {
                        int j = 0;
                        String s1 = POTION_AMPLIFIERS.get(potion.getId());

                        if (s1 != null) {
                            j = parsePotionEffects(s1, 0, s1.length(), p_77917_0_);

                            if (j < 0) {
                                j = 0;
                            }
                        }

                        if (potion.isInstant()) {
                            i = 1;
                        } else {
                            i = 1200 * (i * 3 + (i - 1) * 2);
                            i = i >> j;
                            i = (int) Math.round(i * potion.getEffectiveness());

                            if ((p_77917_0_ & 16384) != 0) {
                                i = (int) Math.round(i * 0.75D + 0.5D);
                            }
                        }

                        if (list == null) {
                            list = new ArrayList<>();
                        }

                        PotionEffect potioneffect = new PotionEffect(potion.getId(), i, j);

                        if ((p_77917_0_ & 16384) != 0) {
                            potioneffect.setSplashPotion(true);
                        }

                        list.add(potioneffect);
                    }
                }
            }
        }

        return list;
    }

    private static int brewBitOperations(int p_77906_0_, int p_77906_1_, boolean p_77906_2_, boolean p_77906_3_, boolean p_77906_4_) {
        if (p_77906_4_) {
            if (!checkFlag(p_77906_0_, p_77906_1_)) {
                return 0;
            }
        } else if (p_77906_2_) {
            p_77906_0_ &= ~(1 << p_77906_1_);
        } else if (p_77906_3_) {
            if ((p_77906_0_ & 1 << p_77906_1_) == 0) {
                p_77906_0_ |= 1 << p_77906_1_;
            } else {
                p_77906_0_ &= ~(1 << p_77906_1_);
            }
        } else {
            p_77906_0_ |= 1 << p_77906_1_;
        }

        return p_77906_0_;
    }

    public static int applyIngredient(int p_77913_0_, String p_77913_1_) {
        int i = 0;
        int j = p_77913_1_.length();
        boolean flag = false;
        boolean flag1 = false;
        boolean flag2 = false;
        boolean flag3 = false;
        int k = 0;

        for (int l = i; l < j; ++l) {
            char c0 = p_77913_1_.charAt(l);

            if (c0 >= 48 && c0 <= 57) {
                k = k * 10;
                k = k + (c0 - 48);
                flag = true;
            } else if (c0 == 33) {
                if (flag) {
                    p_77913_0_ = brewBitOperations(p_77913_0_, k, flag2, flag1, flag3);
                    flag3 = false;
                    flag2 = false;
                    flag = false;
                    k = 0;
                }

                flag1 = true;
            } else if (c0 == 45) {
                if (flag) {
                    p_77913_0_ = brewBitOperations(p_77913_0_, k, flag2, flag1, flag3);
                    flag3 = false;
                    flag1 = false;
                    flag = false;
                    k = 0;
                }

                flag2 = true;
            } else if (c0 == 43) {
                if (flag) {
                    p_77913_0_ = brewBitOperations(p_77913_0_, k, flag2, flag1, flag3);
                    flag3 = false;
                    flag1 = false;
                    flag2 = false;
                    flag = false;
                    k = 0;
                }
            } else if (c0 == 38) {
                if (flag) {
                    p_77913_0_ = brewBitOperations(p_77913_0_, k, flag2, flag1, flag3);
                    flag1 = false;
                    flag2 = false;
                    flag = false;
                    k = 0;
                }

                flag3 = true;
            }
        }

        if (flag) {
            p_77913_0_ = brewBitOperations(p_77913_0_, k, flag2, flag1, flag3);
        }

        return p_77913_0_ & 32767;
    }

    public static int getPotionPrefixIndexFlags(int p_77908_0_, int p_77908_1_, int p_77908_2_, int p_77908_3_, int p_77908_4_, int p_77908_5_) {
        return (checkFlag(p_77908_0_, p_77908_1_) ? 16 : 0) | (checkFlag(p_77908_0_, p_77908_2_) ? 8 : 0) | (checkFlag(p_77908_0_, p_77908_3_) ? 4 : 0) | (checkFlag(p_77908_0_, p_77908_4_) ? 2 : 0) | (checkFlag(p_77908_0_, p_77908_5_) ? 1 : 0);
    }

    static {
        POTION_REQUIREMENTS.put(Potion.REGENERATION.getId(), "0 & !1 & !2 & !3 & 0+6");
        POTION_REQUIREMENTS.put(Potion.MOVE_SPEED.getId(), "!0 & 1 & !2 & !3 & 1+6");
        POTION_REQUIREMENTS.put(Potion.FIRE_RESISTANCE.getId(), "0 & 1 & !2 & !3 & 0+6");
        POTION_REQUIREMENTS.put(Potion.HEAL.getId(), "0 & !1 & 2 & !3");
        POTION_REQUIREMENTS.put(Potion.POISON.getId(), "!0 & !1 & 2 & !3 & 2+6");
        POTION_REQUIREMENTS.put(Potion.WEAKNESS.getId(), "!0 & !1 & !2 & 3 & 3+6");
        POTION_REQUIREMENTS.put(Potion.HARM.getId(), "!0 & !1 & 2 & 3");
        POTION_REQUIREMENTS.put(Potion.MOVE_SLOWDOWN.getId(), "!0 & 1 & !2 & 3 & 3+6");
        POTION_REQUIREMENTS.put(Potion.DAMAGE_BOOST.getId(), "0 & !1 & !2 & 3 & 3+6");
        POTION_REQUIREMENTS.put(Potion.NIGHT_VISION.getId(), "!0 & 1 & 2 & !3 & 2+6");
        POTION_REQUIREMENTS.put(Potion.INVISIBILITY.getId(), "!0 & 1 & 2 & 3 & 2+6");
        POTION_REQUIREMENTS.put(Potion.WATER_BREATHING.getId(), "0 & !1 & 2 & 3 & 2+6");
        POTION_REQUIREMENTS.put(Potion.JUMP.getId(), "0 & 1 & !2 & 3 & 3+6");
        POTION_AMPLIFIERS.put(Potion.MOVE_SPEED.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.DIG_SPEED.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.DAMAGE_BOOST.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.REGENERATION.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.HARM.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.HEAL.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.RESISTANCE.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.POISON.getId(), "5");
        POTION_AMPLIFIERS.put(Potion.JUMP.getId(), "5");
    }
}
