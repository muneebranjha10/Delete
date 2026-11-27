# App Delete FX — 0.2 alpha
Experimental LSPosed module, scope com.sec.android.app.launcher only. Not a KernelSU ZIP.

## Reference analysis
Supplied clip: first icon breaks up around 1.35–1.39 s; particles expand and decay until around 1.8 s. Second icon breaks up around 4.35–4.40 s; final specks vanish around 4.8 s. The blur recedes concurrently. Video is compressed and about 24 FPS, so exact subframe timing and 120 Hz behavior cannot be inferred.
Supplied biometric APK contains FingerprintProgressEffectView, VisualEffectContainer and fingerprint_registration_progress.json (60 FPS, vector shapes), plus touch effect JSON. No standalone reusable deletion particle engine was identified. No Samsung assets are redistributed.

## Implementation
480 ms reference-tuned effect: rapid collapse and breakup, source-colored micro particles, irregular square and circular fragments, fast expansion with cubic deceleration, variable lifetimes and white glints. Up to 1156 samples. Original launcher callbacks and background blur retained. When safe, hide only the separate icon child during its native animation and restore alpha on end/cancel. Overlay is local to launcher view, not a separate window.
The supplied launcher declares IconContainer.getRemoveAnimation(Function0): AnimatorSet. Firmware compatibility, actual deletion trigger, cancellation behavior, 120 FPS and visual equivalence must be tested on device. Stock fallback on missing signature or invalid state. No root commands or changes to uninstall confirmation.

## Install/test
Get the APK from Actions > successful build > Artifacts. Install, enable in LSPosed, scope One UI Home only, restart launcher or reboot. Preview is available without root. Remove a disposable shortcut and check logs for AppDeleteFX 0.2. Disable module if launcher misbehaves. One UI 8 compatibility is unverified. Debug APK is for testing; persistent signing needed for updates.

## 0.3 alpha
New uninstall path captures the exact selected icon before the confirmation dialog and listens for successful removal via LauncherApps.Callback for the current user. Renders once the launcher is visible/focused and the original icon is gone. Launcher-owned uninstall popup dim is disabled only for its own WindowManager call; normal package installer activity may still dim. No change to confirmation or package operations. Brighter and denser particle glints, shorter breakup. Firmware behavior and exact match remain unverified.

## 0.4 alpha
Opt-in one-tap uninstall via existing launcher DELETE_PACKAGES permission and PackageInstaller, scoped to the explicit stock Uninstall click and its final PackageUtils intent launch. No extra root grant. Only downloaded apps in current non-managed user; stock confirmation remains when permission, preferences or icon capture are missing, or Android requires confirmation. Enable the toggle in the module app after LSPosed activation. Local app data may be deleted. Corrected IconViewChild.getView() based on supplied APK (IconViewChild is an interface, not a View); reduced particle travel and white tint to better preserve the purple reference. Runtime and visual match unverified.

## 0.5 alpha
Capture original icon on touch-down before the long-press popup changes visibility. Match snapshot by selected item ID. Fall back to attached icon lookup including hidden views. Add package-removed broadcast backup with package absence check, and increase focus wait to 4 seconds. Log load, click, snapshot, confirmed removal and render stages. No change to particle design or one-tap eligibility. Runtime not yet validated.

## 0.6 alpha
Reference clip at ~23.7 fps: popup closes, icon fades for one frame, dense small white/source-colored specks expand and decelerate while wallpaper unblurs. The video does not establish OS package deletion timing. Direct uninstall no longer depends on successful icon capture; still requires opt-in, exact selected package/current user and OS permission. Begin visual feedback on accepted request as soon as launcher regains focus (up to 200 ms); actual deletion remains asynchronous. Restore hidden icon on failure or OS confirmation. Confirmed-removal fallback no longer waits for stale icon visibility. Use Samsung's actual Kotlin default argument helper for selected component/user. Renderer now uses a sparser selection of source-color motes, brighter sparkles and rapid expansion. Background blur remains native, not independently controlled. No device validation.
