package com.google.browser.examples.twa_per_url;

/** Pure task identity rules, kept separate so they can be tested without a device. */
final class TaskReuseDecider {
    private TaskReuseDecider() {}

    static boolean matches(TaskDescriptor task, String packageName, String activityName,
            String urlIdentity) {
        return packageName.equals(task.packageName)
                && activityName.equals(task.activityName)
                && urlIdentity.equals(task.baseIntentData);
    }

    static final class TaskDescriptor {
        final String packageName;
        final String activityName;
        final String baseIntentData;

        TaskDescriptor(String packageName, String activityName, String baseIntentData) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.baseIntentData = baseIntentData;
        }
    }
}
