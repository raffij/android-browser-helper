package com.google.browser.examples.twa_per_url;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsSession;

import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "PerUrlTwa";
    private static final String DEFAULT_A = "https://airhorner.com/";
    private static final String DEFAULT_B = "https://www.google.com/";

    private TextView status;
    private TextView messages;
    private UrlSessionStore sessionStore;
    private PerUrlTwaLauncher launcher;
    private Uri messageOrigin;
    private boolean channelRequested;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        sessionStore = new UrlSessionStore(this);
        setContentView(createContent());
        Uri requestedUrl = getIntent().getData();
        if (requestedUrl != null) {
            openUrl(requestedUrl.toString());
        }
    }

    private LinearLayout createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        TextView explanation = new TextView(this);
        explanation.setText("Each exact HTTPS URL gets its own document task. "
                + "Existing tasks are foregrounded without sending a new navigation.");
        explanation.setTextSize(16);
        root.addView(explanation, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText first = urlInput(DEFAULT_A);
        EditText second = urlInput(DEFAULT_B);
        root.addView(first);
        root.addView(openButton("Open URL A", first));
        root.addView(second);
        root.addView(openButton("Open URL B", second));

        EditText custom = urlInput("");
        custom.setHint("Custom HTTPS URL");
        root.addView(custom);
        root.addView(openButton("Open custom URL", custom));

        EditText message = urlInput("");
        message.setHint("Message to the companion web page");
        root.addView(message);
        Button send = new Button(this);
        send.setText("Send postMessage");
        send.setOnClickListener(view -> sendMessage(message.getText().toString()));
        root.addView(send);

        status = new TextView(this);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, 24, 0, 0);
        root.addView(status);
        messages = new TextView(this);
        root.addView(messages);
        return root;
    }

    private EditText urlInput(String value) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(value);
        input.setSelectAllOnFocus(false);
        return input;
    }

    private Button openButton(String label, EditText input) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> requestUrl(input.getText().toString()));
        return button;
    }

    private void requestUrl(String rawUrl) {
        String urlIdentity = rawUrl.trim();
        Uri url = Uri.parse(urlIdentity);
        if (!isSupportedUrl(url)) {
            showError("Error: enter an absolute HTTPS URL without user info.");
            return;
        }

        ActivityManager.AppTask existing = findTask(urlIdentity);
        if (existing != null) {
            foregroundTask(existing, urlIdentity);
            return;
        }

        Intent intent = new Intent(this, MainActivity.class)
                .setData(url)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        try {
            startActivity(intent);
            showStatus("Opening a new document task for " + urlIdentity + "...");
        } catch (RuntimeException e) {
            showError("Error: could not create a document task: " + e.getMessage());
        }
    }

    private void openUrl(String rawUrl) {
        String urlIdentity = rawUrl.trim();
        Uri url = Uri.parse(urlIdentity);
        if (!isSupportedUrl(url)) {
            showError("Error: enter an absolute HTTPS URL without user info.");
            return;
        }

        ActivityManager.AppTask existing = findTask(urlIdentity);
        if (existing != null) {
            foregroundTask(existing, urlIdentity);
            return;
        }

        int sessionId = sessionStore.sessionIdFor(urlIdentity);
        showStatus("Created TWA task for " + urlIdentity + " (session " + sessionId + ").");
        try {
            messageOrigin = Uri.parse(url.getScheme() + "://" + url.getAuthority());
            launcher = new PerUrlTwaLauncher(this, sessionId);
            launcher.launch(url, new CustomTabsCallback() {
                @Override
                public void onMessageChannelReady(Bundle extras) {
                    mainHandler.post(() ->
                            showStatus("postMessage channel ready for " + messageOrigin));
                }

                @Override
                public void onNavigationEvent(int navigationEvent, Bundle extras) {
                    if (navigationEvent == NAVIGATION_FINISHED) {
                        mainHandler.post(MainActivity.this::connectPostMessageChannel);
                    }
                }

                @Override
                public void onPostMessage(String message, Bundle extras) {
                    mainHandler.post(() -> appendMessage("Web -> Android: " + message));
                }
            }, () -> {}, message ->
                    mainHandler.post(() -> showError(message)));
        } catch (RuntimeException e) {
            Log.e(TAG, "TWA launch failed", e);
            showError("Error: TWA launch failed: " + e.getMessage()
                    + ". The selected browser may not support TWAs.");
        }
    }

    private void connectPostMessageChannel() {
        if (launcher == null || messageOrigin == null) {
            return;
        }
        if (channelRequested) {
            return;
        }
        CustomTabsSession session = launcher.getSession();
        if (session == null) {
            return;
        }
        channelRequested = true;
        if (!session.requestPostMessageChannel(messageOrigin, messageOrigin, new Bundle())) {
            showError("Browser rejected postMessage origin " + messageOrigin
                    + ". Check DAL and browser support.");
        } else {
            showStatus("Connecting postMessage channel to " + messageOrigin + "...");
        }
    }

    private void sendMessage(String value) {
        if (value.isEmpty()) {
            showError("Enter a message first.");
            return;
        }
        if (launcher == null || launcher.getSession() == null) {
            showError("postMessage channel is not connected for this URL.");
            return;
        }
        int result = launcher.getSession().postMessage(value, null);
        if (result != CustomTabsService.RESULT_SUCCESS) {
            showError("Browser rejected postMessage (result " + result + ").");
            return;
        }
        appendMessage("Android -> Web: " + value);
    }

    private void appendMessage(String message) {
        if (messages != null) {
            messages.append(message + "\n");
        }
    }

    private boolean isSupportedUrl(Uri url) {
        return "https".equalsIgnoreCase(url.getScheme()) && url.getHost() != null
                && url.getUserInfo() == null;
    }

    private void foregroundTask(ActivityManager.AppTask task, String urlIdentity) {
        try {
            task.moveToFront();
            showStatus("Resumed existing TWA task for " + urlIdentity
                    + " (the browser page was not relaunched).");
            finish();
        } catch (SecurityException e) {
            showError("Error: could not foreground the existing task: " + e.getMessage());
        }
    }

    private ActivityManager.AppTask findTask(String urlIdentity) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ComponentName root = new ComponentName(this, MainActivity.class);
        List<ActivityManager.AppTask> tasks = manager.getAppTasks();
        for (ActivityManager.AppTask task : tasks) {
            ActivityManager.RecentTaskInfo info = task.getTaskInfo();
            if (info == null || info.baseIntent == null
                    || info.baseIntent.getData() == null) {
                continue;
            }
            ComponentName component = info.baseIntent.getComponent();
            if (component != null && TaskReuseDecider.matches(
                    new TaskReuseDecider.TaskDescriptor(component.getPackageName(),
                            component.getClassName(), info.baseIntent.getData().toString()),
                    root.getPackageName(), root.getClassName(), urlIdentity)) {
                return task;
            }
        }
        return null;
    }

    private void showStatus(String message) {
        if (status != null) {
            status.setText(message);
        }
    }

    private void showError(String message) {
        showStatus(message);
        Log.e(TAG, message);
    }
}
