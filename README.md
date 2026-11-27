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
