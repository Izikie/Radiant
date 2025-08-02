package com.mojang.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.*;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilGameProfileRepository.class);

    private static final String BASE_URL = "https://api.mojang.com/";
    private static final String SEARCH_PAGE_URL = BASE_URL + "profiles/";
    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;
    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }


    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
        Set<String> criteria = Sets.newHashSet();

        for (String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }

        int page = 0;

        label48:
        for (List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            int failCount = 0;


            while (true) {
                boolean failed = false;

                try {
                    ProfileSearchResultsResponse response = this.authenticationService.makeRequest(HttpAuthenticationService.constantURL(SEARCH_PAGE_URL + agent.getName().toLowerCase()), request, ProfileSearchResultsResponse.class);
                    failCount = 0;

                    LOGGER.debug("Page {} returned {} results, parsing", new Object[]{page, (response.getProfiles()).length});

                    Set<String> missing = Sets.newHashSet(request);
                    for (GameProfile profile : response.getProfiles()) {
                        LOGGER.debug("Successfully looked up profile {}", new Object[]{profile});
                        missing.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                    }

                    for (String name : missing) {
                        LOGGER.debug("Couldn't find profile {}", new Object[]{name});
                        callback.onProfileLookupFailed(new GameProfile(null, name), new ProfileNotFoundException("Server did not find the requested profile"));
                    }

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (InterruptedException ignored) {
                    }
                } catch (AuthenticationException e) {
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT) {
                        for (String name : request) {
                            LOGGER.debug("Couldn't find profile {} because of a server error", new Object[]{name});
                            callback.onProfileLookupFailed(new GameProfile(null, name), e);
                        }
                    } else {
                        try {
                            Thread.sleep(DELAY_BETWEEN_FAILURES);
                        } catch (InterruptedException ignored) {
                        }

                        failed = true;
                    }
                }
                if (!failed) {
                    continue label48;
                }
            }
        }
    }
}
