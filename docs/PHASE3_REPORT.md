# Phase 3 — Implementation & Verification Report

**Branch:** `develop`
**Commits:** `bf65da7` (testable audio primitives), `8b6cd7a` (Android adapters + UI), `fbdf99a` (docs)

## Implemented

- `PcmFrameProcessor` — assembles arbitrary `AudioRecord` chunks into exact 20 ms frames using the existing bounded ring buffer.
- `AudioLevelMeter` — smoothed peak level and decaying peak hold, computed from real PCM samples.
- `AudioSessionStateMachine` — permission, prepare, running, focus suspend/resume, failure and idempotent resource release.
- `AudioDeviceInfo` model — platform-independent endpoint classification (built-in, wired, Bluetooth SCO/A2DP, USB).
- `MicrophoneCapture` — real `AudioRecord` capture on a high-priority thread, optional wired/Bluetooth input via `setPreferredDevice`, capability-detected echo cancellation / noise suppression / AGC, idempotent start/stop and full resource release.
- `SpeakerPlayback` — real `AudioTrack` streaming playback with `USAGE_VOICE_COMMUNICATION`, bounded enqueue queue and clean teardown.
- `AudioFocusManager` — `AudioFocusRequest`-based focus management with suspend/resume callbacks.
- `AndroidAudioDevices` — enumerates input and output endpoints using `AudioManager.getDevices()`.
- `MicrophoneTestActivity` — permission status, recording status, live level meter driven by captured samples, sample rate, channel count, selected input device, enabled DSP effects, device diagnostics, Start/Stop, 440 Hz AudioTrack test tone, and explicit error messages for denied/blocked permission.
- `docs/OPUS_INVESTIGATION.md` — verified codec research; no unverified dependency added.

## Verification performed

| Check | Result |
|---|---|
| JVM unit tests | 47 tests, 0 failures / 0 errors (local) |
| Android lint | 0 errors, 19 informational warnings (mostly untranslated UI strings) |
| Debug APK | Built locally and in CI; 855 KB; package `com.voicechatplus.android`, minSdk 26, two activities |
| GitHub Actions: Android Debug Build | Pass (manual dispatch and push on `develop`) |
| GitHub Actions: Unit Tests | Pass (manual dispatch and push on `develop`) |
| GitHub Actions: Static Analysis | Pass (manual dispatch and push on `develop`) |
| GitHub Actions: Release Build | Pass (manual dispatch on `develop`) |

Two real compile defects were found and fixed against the actual SDK stubs: `AudioRecord.Builder` has no `setAudioDeviceInfo` (correct API is `setPreferredDevice`) and `AudioDeviceInfo` has no `isDefault()`; `AudioEffect` is in `android.media.audiofx`.

## NOT verified — explicit limitation

**No physical-device audio test was performed.** This development session runs inside Termux as an untrusted Android app (`u0_a257`), with no `adb` access and no permission to install an APK or grant `RECORD_AUDIO`. Attempts to install the debug APK failed with `Permission denied` when copying to `/data/local/tmp` and with SELinux access denial when installing from app-private storage.

Therefore the following acceptance items remain **unverified on hardware**:

- runtime permission flow with an actual user response
- real microphone capture and a responding level meter on a physical device
- repeated Start/Stop stability and resource-leak checks on a device
- wired/Bluetooth device routing on a device

These require a manual test pass on a physical Android 8.0+ device using the debug APK from the **Android Debug Build** workflow artifact. CI and JVM tests verify compilation, state transitions, cleanup semantics and resource handling logic, but cannot substitute for hardware verification.

## Next

Phase 4: network engine — connection management, packet handling, authentication and error handling, with no compatibility claim until interop is tested.
