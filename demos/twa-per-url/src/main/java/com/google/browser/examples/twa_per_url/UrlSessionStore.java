package com.google.browser.examples.twa_per_url;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Assigns stable, collision-free browser session IDs to exact hostname keys. */
final class UrlSessionStore {
    private static final String PREFS = "per_url_sessions";
    private static final String HOSTNAME_PREFIX = "hostname:";
    private static final String LEGACY_URL_PREFIX = "url:";
    private static final String NEXT_ID = "next_id";
    private static final int FIRST_ID = 100000;

    private final SharedPreferences preferences;

    UrlSessionStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        migrateLegacyMappings();
    }

    int sessionIdFor(String hostname) {
        String key = HOSTNAME_PREFIX + hostname;
        int existing = preferences.getInt(key, 0);
        if (existing != 0) {
            return existing;
        }

        int candidate = preferences.getInt(NEXT_ID, FIRST_ID);
        Set<Integer> used = new HashSet<>();
        for (String preferenceKey : preferences.getAll().keySet()) {
            if (preferenceKey.startsWith(HOSTNAME_PREFIX)
                    || preferenceKey.startsWith(LEGACY_URL_PREFIX)) {
                used.add(preferences.getInt(preferenceKey, 0));
            }
        }
        while (used.contains(candidate)) {
            candidate++;
        }
        preferences.edit()
                .putInt(key, candidate)
                .putInt(NEXT_ID, candidate + 1)
                .apply();
        return candidate;
    }

    private void migrateLegacyMappings() {
        SharedPreferences.Editor migration = preferences.edit();
        boolean changed = false;
        ArrayList<String> legacyKeys = new ArrayList<>();
        for (String preferenceKey : preferences.getAll().keySet()) {
            if (!preferenceKey.startsWith(LEGACY_URL_PREFIX)) {
                continue;
            }
            legacyKeys.add(preferenceKey);
        }
        Collections.sort(legacyKeys);
        for (String preferenceKey : legacyKeys) {
            String hostname = UrlIdentity.hostnameKeyOrNull(
                    preferenceKey.substring(LEGACY_URL_PREFIX.length()));
            if (hostname == null) {
                continue;
            }
            String hostnameKey = HOSTNAME_PREFIX + hostname;
            if (!preferences.contains(hostnameKey)) {
                migration.putInt(hostnameKey, preferences.getInt(preferenceKey, 0));
                changed = true;
            }
        }
        if (changed) {
            migration.apply();
        }
    }
}
