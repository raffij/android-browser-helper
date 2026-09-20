package com.google.browser.examples.twa_per_url;

import android.net.Uri;

import java.net.URISyntaxException;
import java.util.Locale;

/** Parses the supported URL and derives the exact-hostname task identity. */
final class UrlIdentity {
    final String originalUrl;
    final String hostname;
    final Uri origin;

    private UrlIdentity(String originalUrl, String hostname, Uri origin) {
        this.originalUrl = originalUrl;
        this.hostname = hostname;
        this.origin = origin;
    }

    static UrlIdentity parse(String rawUrl) {
        String originalUrl = rawUrl.trim();
        java.net.URI parsed;
        try {
            parsed = new java.net.URI(originalUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("enter a well-formed absolute HTTPS URL.", e);
        }
        String host = parsed.getHost();
        if (!"https".equalsIgnoreCase(parsed.getScheme()) || host == null || host.isEmpty()
                || parsed.getUserInfo() != null || parsed.getRawAuthority() == null) {
            throw new IllegalArgumentException(
                    "Enter an absolute HTTPS URL without user info.");
        }
        String hostname = host.toLowerCase(Locale.ROOT);
        return new UrlIdentity(originalUrl, hostname,
                Uri.parse(parsed.getScheme() + "://" + parsed.getRawAuthority()));
    }

    static String hostnameKeyOrNull(String url) {
        try {
            return parse(url).hostname;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
