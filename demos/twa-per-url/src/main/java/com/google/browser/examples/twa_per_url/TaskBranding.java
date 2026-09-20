package com.google.browser.examples.twa_per_url;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

/** Selects and applies sample-owned Recents branding without changing browser launch flags. */
final class TaskBranding {
    private static final int ICON_SIZE_DP = 64;

    private TaskBranding() {}

    static Selection forHostname(String hostname) {
        if (hostname == null || hostname.isEmpty()) {
            return new Selection("Per-hostname TWA", R.drawable.ic_brand_default);
        }
        if ("airhorner.com".equals(hostname)) {
            return new Selection("AirHorner hostname", R.drawable.ic_brand_airhorner);
        }
        if ("example.com".equals(hostname)) {
            return new Selection("Example hostname", R.drawable.ic_brand_example);
        }
        return new Selection("Hostname: " + hostname, R.drawable.ic_brand_default);
    }

    static void apply(Activity activity, String hostname) {
        Selection selection = forHostname(hostname);
        Drawable drawable = activity.getDrawable(selection.iconResource);
        if (drawable == null) {
            throw new IllegalStateException("Missing task icon resource "
                    + selection.iconResource);
        }
        int size = (int) (ICON_SIZE_DP * activity.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(new Canvas(bitmap));
        activity.setTaskDescription(new ActivityManager.TaskDescription(
                selection.label, bitmap, activity.getColor(R.color.branding_color)));
    }

    static final class Selection {
        final String label;
        final int iconResource;

        Selection(String label, int iconResource) {
            this.label = label;
            this.iconResource = iconResource;
        }
    }
}
