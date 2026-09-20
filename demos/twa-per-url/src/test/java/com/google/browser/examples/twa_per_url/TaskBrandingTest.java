package com.google.browser.examples.twa_per_url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TaskBrandingTest {
    @Test
    public void usesDistinctBundledIconsForKnownExampleHosts() {
        TaskBranding.Selection airhorner = TaskBranding.forHostname("airhorner.com");
        TaskBranding.Selection example = TaskBranding.forHostname("example.com");

        assertEquals("AirHorner hostname", airhorner.label);
        assertEquals("Example hostname", example.label);
        assertNotEquals(airhorner.iconResource, example.iconResource);
    }

    @Test
    public void usesDefaultIconForArbitraryOrMissingHostname() {
        TaskBranding.Selection arbitrary = TaskBranding.forHostname("custom.example");
        TaskBranding.Selection missing = TaskBranding.forHostname(null);

        assertEquals("Hostname: custom.example", arbitrary.label);
        assertEquals(R.drawable.ic_brand_default, arbitrary.iconResource);
        assertEquals("Per-hostname TWA", missing.label);
        assertEquals(R.drawable.ic_brand_default, missing.iconResource);
    }
}
