package com.mojang.authlib.yggdrasil.request;

import java.util.UUID;

public record JoinMinecraftServerRequest(String accessToken, UUID selectedProfile, String serverId) {}
