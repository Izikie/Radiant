package net.minecraft.nbt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class NBTUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NBTUtil.class);

    // CRASHFIX: Invalid texture URL in skull NBT data
    private static String fixSkullCrash(NBTTagCompound nbtTagCompound) {
        try {
            String valueURL = nbtTagCompound.getString("Value");
            String decodedValueString = new String(Base64.getDecoder().decode(valueURL), StandardCharsets.UTF_8);
            JsonObject jsonObject = JsonParser.parseString(decodedValueString).getAsJsonObject();

            String url = jsonObject
                    .getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString();
            if (url.matches("(http|https)://textures.minecraft.net/texture/.*")) {
                return valueURL;
            } else {
                jsonObject.remove("textures");
                return Base64.getEncoder().encodeToString(jsonObject.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            LOGGER.error("Failed to decode or parse skull value: {}", e.getMessage());
            return "";
        } catch (JsonParseException exception) {
            LOGGER.error("Could not parse skull owner data: {}", exception.getMessage());
            return "";
        }
    }

    public static GameProfile readGameProfileFromNBT(NBTTagCompound compound) {
        String name = compound.hasKey("Name", 8)
                ? compound.getString("Name")
                : null;
        String id = compound.hasKey("Id", 8)
                ? compound.getString("Id")
                : null;

        if (StringUtils.isNullOrEmpty(name) && StringUtils.isNullOrEmpty(id)) {
            return null;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException throwable) {
            uuid = null;
        }

        GameProfile gameProfile = new GameProfile(uuid, name);

        if (compound.hasKey("Properties", 10)) {
            NBTTagCompound properties = compound.getCompoundTag("Properties");

            for (String key : properties.getKeySet()) {
                NBTTagList propertyList = properties.getTagList(key, 10);

                for (int i = 0; i < propertyList.tagCount(); ++i) {
                    NBTTagCompound propertyTag = propertyList.getCompoundTagAt(i);
                    String value = fixSkullCrash(propertyTag);

                    if (propertyTag.hasKey("Signature", 8)) {
                        gameProfile.getProperties().put(key, new Property(key, value, propertyTag.getString("Signature")));
                    } else {
                        gameProfile.getProperties().put(key, new Property(key, value));
                    }
                }
            }
        }

        return gameProfile;
    }

    public static NBTTagCompound writeGameProfile(NBTTagCompound tagCompound, GameProfile profile) {
        if (!StringUtils.isNullOrEmpty(profile.getName())) {
            tagCompound.setString("Name", profile.getName());
        }

        if (profile.getId() != null) {
            tagCompound.setString("Id", profile.getId().toString());
        }

        if (!profile.getProperties().isEmpty()) {
            NBTTagCompound propertiesTag = new NBTTagCompound();

            for (String key : profile.getProperties().keySet()) {
                NBTTagList propertyList = new NBTTagList();

                for (Property property : profile.getProperties().get(key)) {
                    NBTTagCompound propertyTag = new NBTTagCompound();
                    propertyTag.setString("Value", fixSkullCrash(propertyTag));

                    if (property.hasSignature()) {
                        propertyTag.setString("Signature", property.getSignature());
                    }

                    propertyList.appendTag(propertyTag);
                }

                propertiesTag.setTag(key, propertyList);
            }

            tagCompound.setTag("Properties", propertiesTag);
        }

        return tagCompound;
    }

    public static boolean func_181123_a(NBTBase p_181123_0_, NBTBase p_181123_1_, boolean p_181123_2_) {
        if (p_181123_0_ == p_181123_1_) {
            return true;
        } else if (p_181123_0_ == null) {
            return true;
        } else if (p_181123_1_ == null) {
            return false;
        } else if (!p_181123_0_.getClass().equals(p_181123_1_.getClass())) {
            return false;
        } else if (p_181123_0_ instanceof NBTTagCompound nbttagcompound) {
            NBTTagCompound nbttagcompound1 = (NBTTagCompound) p_181123_1_;

            for (String s : nbttagcompound.getKeySet()) {
                NBTBase nbtbase1 = nbttagcompound.getTag(s);

                if (!func_181123_a(nbtbase1, nbttagcompound1.getTag(s), p_181123_2_)) {
                    return false;
                }
            }

            return true;
        } else if (p_181123_0_ instanceof NBTTagList nbttaglist && p_181123_2_) {
            NBTTagList nbttaglist1 = (NBTTagList) p_181123_1_;

            if (nbttaglist.tagCount() == 0) {
                return nbttaglist1.tagCount() == 0;
            } else {
                for (int i = 0; i < nbttaglist.tagCount(); ++i) {
                    NBTBase nbtbase = nbttaglist.get(i);
                    boolean flag = false;

                    for (int j = 0; j < nbttaglist1.tagCount(); ++j) {
                        if (func_181123_a(nbtbase, nbttaglist1.get(j), p_181123_2_)) {
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return p_181123_0_.equals(p_181123_1_);
        }
    }
}
