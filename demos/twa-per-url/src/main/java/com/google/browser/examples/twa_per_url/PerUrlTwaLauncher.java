package com.google.browser.examples.twa_per_url;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;
import com.google.androidbrowserhelper.trusted.TwaProviderPicker;

/** Small sample-only launcher that keeps the CustomTabsSession for postMessage. */
final class PerUrlTwaLauncher {
    interface FailureCallback {
        void onFailure(String message);
    }

    private final Activity activity;
    private final int sessionId;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CustomTabsServiceConnection connection;
    private CustomTabsSession session;
    private String providerPackage;

    PerUrlTwaLauncher(Activity activity, int sessionId) {
        this.activity = activity;
        this.sessionId = sessionId;
    }

    void launch(Uri url, CustomTabsCallback callback, Runnable completion,
            FailureCallback failure) {
        TwaProviderPicker.Action provider =
                TwaProviderPicker.pickProvider(activity.getPackageManager());
        providerPackage = provider.provider;
        if (providerPackage == null
                || provider.launchMode != TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY) {
            failure.onFailure("No browser with Trusted Web Activity support is installed.");
            return;
        }

        TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(url);
        connection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                session = client.newSession(callback, sessionId);
                if (session == null) {
                    failure.onFailure("Browser could not create a Custom Tabs session.");
                    return;
                }
                new SharedPreferencesTokenStore(activity).store(
                        Token.create(providerPackage, activity.getPackageManager()));
                mainHandler.post(() -> {
                    builder.build(session).launchTrustedWebActivity(activity);
                    completion.run();
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                session = null;
                mainHandler.post(() -> failure.onFailure(
                        "Browser disconnected; postMessage is no longer available."));
            }
        };
        if (!CustomTabsClient.bindCustomTabsServicePreservePriority(
                activity, providerPackage, connection)) {
            failure.onFailure("Browser rejected the Custom Tabs connection.");
        }
    }

    @Nullable
    CustomTabsSession getSession() {
        return session;
    }

    void destroy() {
        if (connection != null) {
            activity.unbindService(connection);
            connection = null;
        }
        session = null;
    }
}
