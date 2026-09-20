package com.google.browser.examples.twa_per_url;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TaskReuseDeciderTest {
    private final TaskReuseDecider.TaskDescriptor task =
            new TaskReuseDecider.TaskDescriptor("com.example", "MainActivity",
                    "https://example.com/a?x=1");

    @Test
    public void matchesSameHostnameAcrossPathQueryAndFragment() {
        assertTrue("example.com".equals(UrlIdentity.parse(
                "https://example.com/b?x=1#fragment").hostname));
        assertTrue(TaskReuseDecider.matches(task, "com.example", "MainActivity",
                "example.com"));
        assertTrue(TaskReuseDecider.matches(task, "com.example", "MainActivity",
                "EXAMPLE.COM"));
        assertFalse(TaskReuseDecider.matches(task, "com.example", "MainActivity",
                "shop.example.com"));
        assertFalse(TaskReuseDecider.matches(task, "com.example", "MainActivity",
                "other.example"));
    }

    @Test
    public void matchesDifferentPortsByHostnameButDoesNotChangeLoadedOrigin() {
        TaskReuseDecider.TaskDescriptor portTask =
                new TaskReuseDecider.TaskDescriptor("com.example", "MainActivity",
                        "https://example.com:8443/a");
        assertTrue(TaskReuseDecider.matches(portTask, "com.example", "MainActivity",
                "example.com"));
        assertFalse(UrlIdentity.parse("https://example.com:8443/a").origin
                .equals(UrlIdentity.parse("https://example.com/a").origin));
    }

    @Test
    public void rejectsMalformedOrUnsafeUrls() {
        assertFalse(UrlIdentity.hostnameKeyOrNull("example.com/a") != null);
        assertFalse(UrlIdentity.hostnameKeyOrNull("https://example.com:bad") != null);
        assertFalse(UrlIdentity.hostnameKeyOrNull("https:///missing-host") != null);
        try {
            UrlIdentity.parse("https://user@example.com/a");
            throw new AssertionError("user info should be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void matchesOnlyOwnRoot() {
        assertFalse(TaskReuseDecider.matches(task, "other", "MainActivity",
                "example.com"));
        assertFalse(TaskReuseDecider.matches(task, "com.example", "OtherActivity",
                "example.com"));
    }
}
