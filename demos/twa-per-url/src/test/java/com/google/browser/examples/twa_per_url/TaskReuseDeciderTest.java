package com.google.browser.examples.twa_per_url;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TaskReuseDeciderTest {
    private final TaskReuseDecider.TaskDescriptor task =
            new TaskReuseDecider.TaskDescriptor("com.example", "MainActivity",
                    "https://example.com/a?x=1");

    @Test
    public void matchesOnlyOwnRootAndExactOriginalUrl() {
        assertTrue(TaskReuseDecider.matches(task, "com.example", "MainActivity",
                "https://example.com/a?x=1"));
        assertFalse(TaskReuseDecider.matches(task, "com.example", "MainActivity",
                "https://example.com/a?x=2"));
        assertFalse(TaskReuseDecider.matches(task, "other", "MainActivity",
                "https://example.com/a?x=1"));
        assertFalse(TaskReuseDecider.matches(task, "com.example", "OtherActivity",
                "https://example.com/a?x=1"));
    }
}
