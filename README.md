# Voice Chat+

**Next-Gen Gaming Communication**

An independent, Android-first proximity voice chat project for Minecraft.

[![Android Debug Build](https://github.com/rkbgamerz4/Voice-Chat-Plus/actions/workflows/android-build.yml/badge.svg)](https://github.com/rkbgamerz4/Voice-Chat-Plus/actions/workflows/android-build.yml)
[![Unit Tests](https://github.com/rkbgamerz4/Voice-Chat-Plus/actions/workflows/tests.yml/badge.svg)](https://github.com/rkbgamerz4/Voice-Chat-Plus/actions/workflows/tests.yml)

---

> **Status: early foundation (v0.1.0).** This repository currently contains research, architecture, a multi-module Gradle build, DSP/protocol primitives with unit tests, and a minimal Android diagnostics screen. Voice transport and Minecraft integration are **not implemented**.
>
> **Compatibility notice:** Voice Chat+ is **not verified** to be compatible with Simple Voice Chat or any Minecraft voice chat server. No interoperability claim is made. See `docs/RESEARCH.md`.

---

## What this project is

Voice Chat+ aims to provide proximity-based voice communication for Minecraft players, with a native Android client. The long-term feature set includes:

- Proximity voice chat, push-to-talk, voice activity detection, and configurable microphone sensitivity
- Microphone/speaker mute and per-player mute with speaking indicators
- Opus encode/decode, low-latency capture and playback, device detection, Bluetooth and wired headset support
- Android 8.0+ (API 26), ARM64, audio focus, runtime microphone permissions, low-RAM operation
- A clean, dark, gaming-oriented interface (Home, Voice Settings, Audio Devices, Microphone Test, Players, Connection, About)

These are **goals**, not shipped features. Progress is tracked in the roadmap below.

## Project structure

```
common/                  Shared value types (pure Kotlin/JVM)
audio-engine/            PCM buffers, VAD, DSP primitives (pure Kotlin/JVM)
voice-protocol/          Wire types and byte codec utilities (pure Kotlin/JVM)
network-engine/          Connection state machine, future UDP transport
minecraft-integration/   Planned for Phase 5 — not started
android-app/             Android application, UI and platform adapters
docs/                    Research, architecture and limitations
.github/workflows/       CI: debug build, tests, lint, release
```

See `docs/ARCHITECTURE.md` for dependency rules and the target data flow.

## Building

Requirements: JDK 17 or newer, Android SDK platform 35 and build-tools (or let CI do it).

```bash
./gradlew test                      # unit tests (JVM modules)
./gradlew :android-app:assembleDebug  # debug APK
./gradlew :android-app:lintDebug      # static analysis
```

The debug APK is written to `android-app/build/outputs/apk/debug/`.

On a machine without an Android SDK, use the GitHub Actions **Android Debug Build** workflow: push a branch, or open the Actions tab and run the workflow manually. The artifact is uploaded on completion.

## Roadmap

| Phase | Scope | State |
|---|---|---|
| 1 | Research reference project, license and Android audio constraints | Done |
| 2 | Repository, docs, project structure, Gradle, initial README | Done |
| 3 | Android audio foundation: capture, playback, Opus, mic test, tests | In progress (DSP primitives + tests only) |
| 4 | Network engine and verified protocol | Not started |
| 5 | Minecraft integration and proximity | Not started (blocked on Phase 3/4) |
| 6 | Full UI: settings, devices, players, diagnostics, about | Not started |
| 7 | CI/CD: builds, tests, static analysis, release artifacts | Workflows in place |

## Legal and attribution

- Voice Chat+ is an independent project and is **not affiliated with or endorsed by** the authors of Simple Voice Chat.
- Simple Voice Chat (`https://github.com/henkelmax/simple-voice-chat`) is used strictly as an interoperability reference. Its license is "Copyright (c) Max Henkel - All Rights Reserved"; no source code, assets, or binaries from it are present here.
- Compatibility will only be announced after successful end-to-end testing against a real server.

Voice Chat+ source code is released under the MIT License — see `LICENSE`. Third-party dependencies keep their own licenses.

## Contributing

See `CONTRIBUTING.md`. In particular: no RKB branding, no secrets in commits, no compatibility claims without tests.
