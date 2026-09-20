package com.google.browser.examples.twa_per_url;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Assigns stable, collision-free browser session IDs to exact URL identity strings. */
final class UrlSessionStore {
    private static final String PREFS = "per_url_sessions";
    private static final String URL_PREFIX = "url:";
    private static final String NEXT_ID = "next_id";
    private static final int FIRST_ID = 100000;

    private final SharedPreferences preferences;

    UrlSessionStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    int sessionIdFor(String urlIdentity) {
        String key = URL_PREFIX + urlIdentity;
        int existing = preferences.getInt(key, 0);
        if (existing != 0) {
            return existing;
        }

        int candidate = preferences.getInt(NEXT_ID, FIRST_ID);
        Set<Integer> used = new HashSet<>();
        for (String preferenceKey : preferences.getAll().keySet()) {
            if (preferenceKey.startsWith(URL_PREFIX)) {
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
}
