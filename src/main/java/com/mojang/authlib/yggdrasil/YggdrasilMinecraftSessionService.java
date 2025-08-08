package com.mojang.authlib.yggdrasil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDAdapter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {

    private static final String[] WHITELISTED_DOMAINS = new String[]{".minecraft.net", ".mojang.com"};
    private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilMinecraftSessionService.class);
    private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
    private static final URL JOIN_URL = HttpAuthenticationService.constantURL(BASE_URL + "join");
    private static final URL CHECK_URL = HttpAuthenticationService.constantURL(BASE_URL + "hasJoined");

    private final PublicKey publicKey;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDAdapter()).create();
    private final LoadingCache<GameProfile, GameProfile> insecureProfiles = CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build(new CacheLoader<>() {
        @Override
        public @NotNull GameProfile load(@NotNull GameProfile key) {
            return YggdrasilMinecraftSessionService.this.fillGameProfile(key, false);
        }
    });

    protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(Objects.requireNonNull(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der"))));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }

    @Override
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        getAuthenticationService().makeRequest(
            JOIN_URL,
            new JoinMinecraftServerRequest(authenticationToken, profile.getId(), serverId),
            Response.class
        );
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);

        URL url = HttpAuthenticationService.concatenateURL(CHECK_URL, HttpAuthenticationService.buildQuery(arguments));

        try {
            HasJoinedMinecraftServerResponse response = getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class);

            if (response != null && response.getId() != null) {
                GameProfile result = new GameProfile(response.getId(), user.getName());

                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }

                return result;
            }
            return null;
        } catch (AuthenticationUnavailableException e) {
            throw e;
        } catch (AuthenticationException e) {
            return null;
        }
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        MinecraftTexturesPayload result;
        Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);

        if (textureProperty == null) {
            return new HashMap<>();
        }

        if (requireSecure) {
            if (!textureProperty.hasSignature()) {
                LOGGER.error("Signature is missing from textures payload");
                throw new InsecureTextureException("Signature is missing from textures payload");
            }

            if (!textureProperty.isSignatureValid(this.publicKey)) {
                LOGGER.error("Textures payload has been tampered with (signature invalid)");
                throw new InsecureTextureException("Textures payload has been tampered with (signature invalid)");
            }
        }


        try {
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), StandardCharsets.UTF_8);
            result = this.gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<>();
        }

        if (result.getTextures() == null) {
            return new HashMap<>();
        }

        for (Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : result.getTextures().entrySet()) {
            if (!isWhitelistedDomain(entry.getValue().getUrl())) {
                LOGGER.error("Textures payload has been tampered with (non-whitelisted domain)");
                return new HashMap<>();
            }
        }

        return result.getTextures();
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
        if (profile.getId() == null) {
            return profile;
        }

        if (!requireSecure) {
            return this.insecureProfiles.getUnchecked(profile);
        }

        return fillGameProfile(profile, true);
    }

    protected GameProfile fillGameProfile(GameProfile profile, boolean requireSecure) {
        try {
            URL url = HttpAuthenticationService.constantURL("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDAdapter.fromUUID(profile.getId()));
            url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + (!requireSecure ? 1 : 0));
            MinecraftProfilePropertiesResponse response = getAuthenticationService().makeRequest(url, null, MinecraftProfilePropertiesResponse.class);

            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for " + profile + " as the profile does not exist");
                return profile;
            }
            GameProfile result = new GameProfile(response.getId(), response.getName());
            result.getProperties().putAll(response.getProperties());
            profile.getProperties().putAll(response.getProperties());
            LOGGER.debug("Successfully fetched profile properties for " + profile);
            return result;
        } catch (AuthenticationException e) {
            LOGGER.warn("Couldn't look up profile properties for " + profile, e);
            return profile;
        }
    }

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }

    private static boolean isWhitelistedDomain(String url) {
        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL '" + url + "'");
        }

        String domain = uri.getHost();

        for (String whitelistedDomain : WHITELISTED_DOMAINS) {
            if (domain.endsWith(whitelistedDomain)) {
                return true;
            }
        }
        return false;
    }
}
