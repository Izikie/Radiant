package com.mojang.authlib.minecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

import java.util.Map;

public interface MinecraftSessionService {
    void joinServer(GameProfile paramGameProfile, String paramString1, String paramString2) throws AuthenticationException;

    GameProfile hasJoinedServer(GameProfile paramGameProfile, String paramString) throws AuthenticationUnavailableException;

    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile paramGameProfile, boolean paramBoolean);

    GameProfile fillProfileProperties(GameProfile paramGameProfile, boolean paramBoolean);
}
