package com.mojang.authlib.legacy;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import net.radiant.json.adapter.impl.UUIDAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LegacyUserAuthentication extends HttpUserAuthentication {
    private static final URL AUTHENTICATION_URL = HttpAuthenticationService.constantURL("https://login.minecraft.net");

    private static final int AUTHENTICATION_VERSION = 14;
    private static final int RESPONSE_PART_PROFILE_NAME = 2;
    private static final int RESPONSE_PART_SESSION_TOKEN = 3;
    private static final int RESPONSE_PART_PROFILE_ID = 4;

    private String sessionToken;

    protected LegacyUserAuthentication(LegacyAuthenticationService authenticationService) {
        super(authenticationService);
    }

    public void logIn() throws AuthenticationException {
        String response;
        if (StringUtils.isBlank(getUsername())) {
            throw new InvalidCredentialsException("Invalid username");
        }
        if (StringUtils.isBlank(getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        Map<String, Object> args = new HashMap<>();
        args.put("user", getUsername());
        args.put("password", getPassword());
        args.put("version", Integer.valueOf(AUTHENTICATION_VERSION));


        try {
            response = getAuthenticationService().performPostRequest(AUTHENTICATION_URL, HttpAuthenticationService.buildQuery(args), "application/x-www-form-urlencoded").trim();
        } catch (IOException e) {
            throw new AuthenticationException("Authentication server is not responding", e);
        }

        String[] split = response.split(":");

        if (split.length == 5) {
            String profileId = split[RESPONSE_PART_PROFILE_ID];
            String profileName = split[RESPONSE_PART_PROFILE_NAME];
            String sessionToken = split[RESPONSE_PART_SESSION_TOKEN];

            if (StringUtils.isBlank(profileId) || StringUtils.isBlank(profileName) || StringUtils.isBlank(sessionToken)) {
                throw new AuthenticationException("Unknown response from authentication server: " + response);
            }

            setSelectedProfile(new GameProfile(UUIDAdapter.fromString(profileId), profileName));
            this.sessionToken = sessionToken;
            setUserType(UserType.LEGACY);
        } else {
            throw new InvalidCredentialsException(response);
        }
    }


    public void logOut() {
        super.logOut();
        this.sessionToken = null;
    }


    public boolean canPlayOnline() {
        return (isLoggedIn() && getSelectedProfile() != null && getAuthenticatedToken() != null);
    }


    public GameProfile[] getAvailableProfiles() {
        if (getSelectedProfile() != null) {
            return new GameProfile[]{getSelectedProfile()};
        }
        return new GameProfile[0];
    }


    public void selectGameProfile(GameProfile profile) throws AuthenticationException {
        throw new UnsupportedOperationException("Game profiles cannot be changed in the legacy authentication service");
    }


    public String getAuthenticatedToken() {
        return this.sessionToken;
    }


    public String getUserID() {
        return getUsername();
    }


    public LegacyAuthenticationService getAuthenticationService() {
        return (LegacyAuthenticationService) super.getAuthenticationService();
    }
}
