# Per-hostname TWA sample

This sample demonstrates one document task per **exact hostname**. `MainActivity` is the root of
each app-owned document task, and the browser's TWA activity is started above it. On a later open,
the app scans `ActivityManager.getAppTasks()`, verifies that the task root is this app and that its
`baseIntent.data` has the requested hostname, then calls `AppTask.moveToFront()`. It does not
launch the browser in that case, so the existing page is not navigated, reloaded, or replaced.
The first URL opened for the hostname remains the page and postMessage origin. The sample-only
launcher uses the checked-in helper's explicit session-ID overload and retains that same
`CustomTabsSession` for its postMessage channel.

The task key is the parsed hostname, lowercased with `Locale.ROOT`; subdomains are not collapsed
and suffix matching is never used. Paths, queries, fragments, and explicit ports do not change
the task key, so `https://example.com/a`, `https://example.com/b?x=1#fragment`, and
`https://EXAMPLE.com:8443/c` foreground the same task. The actual first URL is retained unchanged
for initial navigation, and its full `https` origin (including explicit port) remains the
postMessage/Digital Asset Links origin. A different port therefore shares the task but never
pretends that the channel belongs to the requested port. Malformed, non-HTTPS, user-info, and
hostless URLs are rejected. Session IDs are stable, persisted, and allocated from a monotonic
collision-free sequence rather than a hash.

The app migrates legacy `url:` session preferences from earlier revisions to `hostname:` entries,
preserving the first stored session ID for each hostname and retaining old entries for safety.
Existing tasks are matched by their original root URL's hostname, so an upgrade does not silently
reuse a session for a different hostname.

## Run

```shell
./gradlew :demos:twa-per-url:assembleDebug
```

The APK is written to
`demos/twa-per-url/build/outputs/apk/debug/twa-per-url-debug.apk`.

Install it on an emulator/device with a browser that supports Custom Tabs/TWAs. A TWA is
fullscreen only when the selected origin has a valid Digital Asset Links relationship to this
app's signing certificate. The editable examples are not claimed to be verified. For a
user-controlled domain, publish `/.well-known/assetlinks.json` containing this app's package name
and SHA-256 signing certificate fingerprint, and use that HTTPS origin in the input. Without
verification the browser may show a Custom Tab instead; the app reports launch errors rather than
pretending that arbitrary domains are verified.

## Manual reproduction

1. Open URL A, then navigate and scroll within the page.
2. Return to the sample and open URL B with another path/query/fragment on the same hostname.
   The existing A task is foregrounded and its page is not navigated to B.
3. Open a subdomain or unrelated hostname and observe a separate task.
4. Close the hostname task with Back, then open it again. Its task is gone, so a fresh TWA is
   created with that new original URL.

Task/page memory is best-effort: Android or the browser may kill processes while backgrounded.
After process death, the browser can restore or recreate its own state, but this sample cannot
promise in-memory preservation. Rapid duplicate taps and stale task handles are handled by
rechecking the current task list and reporting foregrounding failures.

## Per-hostname postMessage channel

The sample also keeps the `CustomTabsSession` created for each new hostname task and exposes a
`Send postMessage` control. The browser callback reports ready, received web messages, browser
disconnects, and rejected sends explicitly. A matching task is foregrounded without creating a
second session; after the browser or app process dies, the channel must be recreated and is not
claimed to survive.

`web-demo/index.html` is a minimal companion page: host it over HTTPS, load its URL in the sample,
and use the Android control and the page control to exercise both directions. The companion
echoes Android messages and sends a page-loaded event plus user-entered messages back to Android.
The page's full origin must equal the first URL's origin, including any explicit port; no wildcard
origin is used. The browser must support postMessage and the origin must be authorized by Digital
Asset Links for a real TWA. The stock example URLs do not host this companion page and third-party
pages are not expected to implement this protocol. Opening another path or port for the same
hostname foregrounds the existing page and does not retarget its channel.

For a user-controlled host, publish `/.well-known/assetlinks.json` on that origin with this app's
package name (`com.google.browser.examples.twa_per_url`) and the SHA-256 fingerprint of the
installed signing certificate. Serve the companion page and asset links over HTTPS, enter its URL,
open it once, and wait for `postMessage channel ready`. The local file is a static demo only; it
is not an HTTPS server or a DAL configuration.

## Recents task branding

The app applies an `Activity.setTaskDescription` label and rendered vector icon to the app-owned
root before launching the browser. The browser is intentionally launched without `NEW_TASK` or
`NEW_DOCUMENT`, so it remains above that root task and can inherit its task branding. On root
creation/restoration the same hostname selection is reapplied; foreground-only reuse does not
relaunch the browser or update the page just to change branding.

`airhorner.com` and `example.com` use distinct bundled sample icons. Arbitrary hostnames use
`ic_brand_default` and a generated `Hostname: <hostname>` label. To customize a hostname, add a
sample-owned vector drawable under `src/main/res/drawable` and one exact hostname branch in
`TaskBranding.forHostname`; do not fetch or bundle third-party brand assets. The manifest's
application icon and round fallback use the same default vector.

Manual Recents checks: open two distinct hostnames and confirm their task labels/icons differ;
open a different path or explicit port for an existing hostname and confirm the same task,
icon, and page are foregrounded; then restore the task from Recents and confirm branding remains.
Browser versions, launchers, and OEM task rendering may differ, and process death can affect
browser state, so these visual results require a device/browser check.
