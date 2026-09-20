# Per-URL TWA sample

This sample demonstrates one document task per **exact initial URL**. `MainActivity` is the root
of each app-owned document task, and the browser's TWA activity is started above it. On a later
open, the app scans `ActivityManager.getAppTasks()`, verifies that the task root is this app and
that its `baseIntent.data` exactly equals the requested URL, then calls `AppTask.moveToFront()`.
It does not call `TwaLauncher.launch()` in that case, so the browser does not navigate, reload, or
replace the current page. The original URL remains the identity even after the page navigates.
The sample-only launcher uses the checked-in helper's explicit session-ID overload and retains that
same `CustomTabsSession` for its URL's postMessage channel.

URL identity is the trimmed input string parsed as an absolute `https` URI with a host. No
lowercasing, path cleanup, query sorting, fragment removal, decoding, or other normalization is
performed; distinct URL strings remain distinct. Session IDs are stable, persisted, and allocated
from a monotonic collision-free sequence rather than a hash.

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
2. Return to the sample, open URL B, and observe a separate task.
3. Open URL A again. The existing A task is foregrounded and its page state is preserved.
4. Close A with Back, then open A again. Its task is gone, so a fresh TWA is created.

Task/page memory is best-effort: Android or the browser may kill processes while backgrounded.
After process death, the browser can restore or recreate its own state, but this sample cannot
promise in-memory preservation. Rapid duplicate taps and stale task handles are handled by
rechecking the current task list and reporting foregrounding failures.

## Per-URL postMessage channel

The sample also keeps the `CustomTabsSession` created for each new URL task and exposes a
`Send postMessage` control. The browser callback reports ready, received web messages, browser
disconnects, and rejected sends explicitly. A matching task is foregrounded without creating a
second session; after the browser or app process dies, the channel must be recreated and is not
claimed to survive.

`web-demo/index.html` is a minimal companion page: host it over HTTPS, load its exact URL in the
sample, and use the Android control and the page control to exercise both directions. The
companion echoes Android messages and sends a page-loaded event plus user-entered messages back to
Android. The page's origin must be the same exact HTTPS origin passed to
`requestPostMessageChannel`; no wildcard origin is used. The browser must support postMessage and
the origin must be authorized by Digital Asset Links for a real TWA. The stock example URLs do
not host this companion page and third-party pages are not expected to implement this protocol.

For a user-controlled host, publish `/.well-known/assetlinks.json` on that origin with this app's
package name (`com.google.browser.examples.twa_per_url`) and the SHA-256 fingerprint of the
installed signing certificate. Serve the companion page and asset links over HTTPS, enter its
exact URL, open it once, and wait for `postMessage channel ready`. The local file is a static
demo only; it is not an HTTPS server or a DAL configuration.
