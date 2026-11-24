# Architecture — Voice Chat+

## 1. Goals and constraints

- Proximity voice chat for Minecraft players, with an Android client as the primary platform.
- Clean-room implementation: interoperability is a goal, source reuse is not (see `docs/RESEARCH.md`).
- Android UI must never contain protocol logic; the audio core must be independent of Android.
- Every module must build and be testable in isolation on a stock JDK.
- No unverified compatibility claims, no fake implementations, no hardcoded secrets.

## 2. Module map

```
                          android-app  (Android UI, permissions, AudioRecord/AudioTrack)
                               |
        +----------------------+----------------------+
        |            |              |                 |
     common      audio-engine   voice-protocol   network-engine   minecraft-integration
        \            |              |                 |                  /
         +-----------+--------------+-----------------+------------------+
                             (pure Kotlin/JVM, no Android SDK)
```

| Module | Depends on | Responsibility | Status (Phase 3) |
|---|---|---|---|
| `common` | — | Shared constants, version/capability value types | Implemented + unit tested |
| `audio-engine` | `common` | PCM formats, ring buffers, VAD, frame assembly, level metering, session lifecycle | Implemented + unit tested; Opus pending dependency verification |
| `voice-protocol` | `common` | Internal wire types, byte buffer/codec utilities, encryption and handshake state machines | Byte buffer implemented + unit tested; remainder planned |
| `network-engine` | `common`, `voice-protocol` | UDP socket lifecycle, connection state machine, packet dispatch, error handling | State machine implemented + unit tested; transport planned |
| `minecraft-integration` | `common`, `voice-protocol` | Proximity model, player voice state, PTT bindings, loader adapters | Planned — not started by design |
| `android-app` | all of the above | UI screens, runtime permissions, audio focus, device discovery, AudioRecord/AudioTrack adapters | Capture/playback/test screen implemented; foreground service planned |

### Dependency rules

1. JVM modules must not depend on `android-app` or on any Android class.
2. `voice-protocol` must not know about sockets, threads, or UI.
3. `network-engine` must not encode/decode packet internals itself — it hands byte arrays to `voice-protocol`.
4. `android-app` is the only module that may use `android.*` APIs.
5. `minecraft-integration` is not created in source form until Phase 5; the build currently includes only its Gradle project and a scope note.

## 3. Voice data flow (target design)

```
Microphone (AudioRecord)
   -> PCM frame (20 ms @ 48 kHz mono)      [android-app adapter]
   -> optional AGC / echo cancel / denoise [Android AudioProcessor, capability-detected]
   -> VAD gate + gain control              [audio-engine]
   -> Opus encode                          [codec interface, implementation TBD]
   -> encrypt + frame                      [voice-protocol]
   -> UDP datagram                         [network-engine]
   -> server -> other players
   -> decrypt + decode                     [voice-protocol]
   -> Opus decode + jitter buffer          [audio-engine]
   -> AudioTrack (USAGE_VOICE_COMMUNICATION)[android-app adapter]
```

As of Phase 3, capture, audio effects, frame assembly, VAD, metering and playback are real and implemented. The codec/encryption/network stages are a design target, not an implemented pipeline. `MicrophoneCapture.onFrame` is the boundary where a verified codec will connect.

## 4. Threading model (target)

- **Capture thread:** high priority, owns `AudioRecord`; performs no allocation and no UI work.
- **Playback thread:** owns `AudioTrack`; drains jitter buffer.
- **Network thread:** single UDP send/receive loop; no blocking calls on the capture thread.
- **Main thread:** UI only. Cross-thread communication uses immutable snapshots.
- Back-pressure is handled by fixed-capacity ring buffers that drop the oldest sample rather than blocking the microphone.

## 5. State machines

### Connection (`network-engine`, implemented)

```
Disconnected -> Connecting -> Authenticating -> Connected -> Disconnected
      |              |              |              |
      +--------------+--------------+--------------+--> Failed(reason)
      Failed -> Connecting | Disconnected
```

Invalid transitions are rejected rather than silently applied; see `ConnectionStateMachine` and its tests.

### Audio focus (planned, Android only)

`UNFOCUSED -> GRANTED -> LOST_TRANSIENT -> GRANTED | LOST` — focus loss suspends capture and playback; transient loss auto-resumes.

## 6. Platform boundary

`audio-engine` defines values and algorithms only. All platform IO is supplied by adapters in `android-app`:

- `MicrophoneAdapter` — wraps `AudioRecord`, runtime permission check, audio effects, focus awareness.
- `SpeakerAdapter` — wraps `AudioTrack`, routing, focus awareness.
- `DeviceDiscoveryAdapter` — wraps `AudioManager.getDevices()` for the Audio Devices screen.

These adapters are intentionally not stubbed in milestone 1: they will be introduced together with the working audio capture path so there is no fake implementation.

## 7. Build and quality gates

- Gradle 8.9 wrapper, Android Gradle Plugin 8.6.1, Kotlin 2.0.20, `compileSdk`/`targetSdk` 35, `minSdk` 26 (Android 8.0).
- JVM modules target Java 11 bytecode for broad Android desugaring compatibility; the toolchain running Gradle is Java 17+.
- Unit tests use JUnit 5 on all JVM modules; Android unit tests use JUnit 4.
- CI runs a debug APK build, unit tests, Android lint, and a manual release build. Every workflow supports `workflow_dispatch` where appropriate.

## 8. Decision log

| Decision | Rationale |
|---|---|
| Clean-room, no reference source | Reference license is "All Rights Reserved" |
| Kotlin for shared modules, no Android SDK dependency | Keeps protocol/audio logic testable on a plain JDK |
| Gradle wrapper committed | Reproducible CI builds without a preinstalled Gradle |
| No codec dependency in milestone 1 | Dependency source/license not yet verified; avoids an unverifiable build |
| `develop` branch for work, `main` for stable | Staged, non-destructive Git history |
