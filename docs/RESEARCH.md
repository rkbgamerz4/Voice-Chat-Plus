# Phase 1 — Research Notes

**Project:** Voice Chat+  
**Date:** 2026-11-24  
**Method:** public sources only. No source code, assets, or binaries from the reference project were copied into this repository.

---

## 1. Sources inspected

| Source | What was examined |
|---|---|
| `https://github.com/henkelmax/simple-voice-chat` (default branch `26.3`) | Repository metadata, file tree, `readme.md`, `license`, root `build.gradle`, `common-client/build.gradle` |
| `https://modrepo.de/minecraft/voicechat/wiki/` | Official documentation index |

Only public information was used. No private repositories, decompiled binaries, or copied source files were involved.

---

## 2. Legal boundary (read this first)

The reference project's `license` file contains exactly one line:

> `Copyright (c) Max Henkel - All Rights Reserved`

There is **no** MIT, GPL, Apache, or other open-source grant. "All rights reserved" means the source may not be copied, modified, or redistributed by us.

Consequences for Voice Chat+:

- This project is a **clean-room, independent implementation**. We study observable architecture, feature behavior, and interoperability contracts. We do **not** copy, translate, adapt, or vendor any Simple Voice Chat source file, asset, or binary.
- We do not use the Simple Voice Chat name, logo, icons, or UI in our product. It is referenced only as an interoperability target in documentation.
- We make **no affiliation, endorsement, or compatibility claim** unless and until end-to-end interoperability has actually been tested against a running server.
- Any future contributor who has contributed to the reference project must not bring that code into this repository.
- If the rights holder objects to this clean-room reference, we will remove the reference material and cease the interoperability work.

This boundary is deliberately stricter than "study the architecture": it governs what may legally be written into this repository.

---

## 3. Observed architecture of the reference project

The reference is a multi-module Gradle/Java project. Its published modules (from the branch tree) are:

| Area | Modules | Role (observed) |
|---|---|---|
| Shared | `common`, `common-client`, `common-proxy` | Shared logic, client logic, proxy logic |
| API | `api`, `fabric/api-stub` | Public plugin API and stubs |
| Minecraft client | `fabric`, `forge`, `neoforge`, `quilt` | Loader-specific client mods |
| Server | `bukkit`, `paper`, `bungeecord`, `velocity` | Server/proxy plugins |

Notable implementation characteristics observed from metadata, build files and the file tree:

- Voice transport is **UDP**, on port **24454/udp by default**, configurable server-side. The TCP/Minecraft connection is used for control messages; the voice stream itself is separate.
- A **secret** is exchanged over the Minecraft plugin channel before UDP authentication (files named `RequestSecretPacket`, `SecretPacket`, `AuthenticatePacket`, `AuthenticateAckPacket` are present). This is consistent with a "Minecraft login proves identity → short-lived voice secret → UDP auth" design.
- Voice data uses the **Opus** codec. Both a native binding (`de.maxhenkel.opus4j:opus4j`) and a pure-Java implementation (`org.concentus:Concentus`) appear in the reference build.
- Additional DSP libraries are present: `de.maxhenkel.rnnoise4j:rnnoise4j` (RNNoise noise suppression), `de.maxhenkel.speex4j:speex4j` (Speex, typically AGC/denoise), `de.maxhenkel.lame4j:lame4j` (recording/export).
- Playback uses **OpenAL** (3D positional audio), capture uses the Java audio stack in desktop builds.
- Encryption is **AES** based (documented in the reference readme) covering the UDP voice traffic.
- Features documented by the reference include proximity voice, push-to-talk, voice activation, whispering, password groups, per-player volume, 3D audio, configurable voice distance, and recording.

These are **observations**, not a protocol specification. Packet layouts, field widths, ordering, versioning, and cryptographic details are deliberately *not* reverse engineered here.

---

## 4. Working protocol model (hypothesis — unverified)

The following model is a planning aid only. Every item is marked unverified until a real interop test is performed.

| Stage | Hypothesised behavior | Verification required |
|---|---|---|
| 1. Discovery | Server advertises voice-chat capability over a Minecraft plugin channel | Capture and inspect channel handshake |
| 2. Secret | Client requests a secret; server returns one bound to the player identity | Capture plugin-channel payloads |
| 3. UDP auth | Client opens UDP socket and authenticates with secret + player identity | Capture initial UDP datagram |
| 4. Heartbeat | Periodic keepalive maintains NAT mapping and presence | Observe idle traffic |
| 5. Voice | Datagrams carry sequence number, sender identity, and Opus payload; positions may be sent for distance attenuation | Decode a known spoken sample |
| 6. Encryption | Payload encrypted with AES derived from the handshake secret | Verify with a controlled server |

**Rule:** no code in `voice-protocol` or `network-engine` may present itself as "Simple Voice Chat compatible" until at least stages 1–5 are verified in an integration test against a real server. Until then, those modules implement our own internal format and a clearly-labelled compatibility adapter boundary.

---

## 5. Android audio constraints

These constraints drive the module split (audio core independent of Android UI/protocol).

1. **No `javax.sound.sampled` on Android.** Desktop capture/playback code cannot be reused; capture must use `AudioRecord` and playback `AudioTrack` (or AAudio via Oboe where justified).
2. **Latency.** `AudioRecord` in `MODE_STREAM` with `AudioFormat.ENCODING_PCM_16BIT`, mono, 48 kHz. Use `getMinBufferSize()` as a floor; request low-latency attributes. True sub-20 ms latency is not guaranteed on all devices — document rather than promise.
3. **Built-in DSP.** Android exposes `AcousticEchoCanceler`, `NoiseSuppressor`, and `AutomaticGainControl` as `AudioEffect`/`AudioProcessor` attached to an `AudioRecord` session. Availability is device-dependent and must be queried at runtime (`isAvailable()`), never assumed.
4. **Audio focus.** Android 8.0 (API 26) uses `AudioFocusRequest` with `AudioAttributes.Builder().setUsage(USAGE_VOICE_COMMUNICATION)`. Focus loss must pause capture/playback and restore on regain.
5. **Routing.** Bluetooth SCO and wired headsets are governed by audio attributes and `AudioManager` state; automatic device detection is an OS service, not something to hardcode. A device list UI must come from `AudioManager.getDevices()`.
6. **Permissions.** `RECORD_AUDIO` is a dangerous permission requiring a runtime request. `MODIFY_AUDIO_SETTINGS` and `INTERNET` are normal permissions. Denied permission is a normal state, not an error to hide.
7. **Background work.** Continuous capture needs a foreground service with a visible notification; doze and OEM battery optimisation will otherwise kill the session.
8. **Architecture / footprint.** ARM64 is the primary target; keep native code optional, avoid large per-frame allocations, reuse buffers, and keep the UI module free of DSP/protocol code.
9. **Codecs.** Options for Opus on Android:
   - **Pure Java (Concentus)** — no JNI, no ABI-specific library, easiest reproducible build; dependency source must be verified before adding (it was *not* resolvable from Maven Central under `org.concentus` during this research).
   - **Native libopus via JNI** — best performance but requires NDK, per-ABI `.so` packaging, and a reproducible native build.
   - **Android `MediaCodec` Opus** — encoder availability is inconsistent across devices; not reliable as a primary path.
   Decision for the first milestone: **no codec dependency yet**; build the DSP core and verify the dependency source before integrating Opus.

---

## 6. Identified technical risks

| Risk | Severity | Mitigation |
|---|---|---|
| Reference is all-rights-reserved; no reuse of source is permitted | High | Clean-room implementation, documented boundary, attribution only |
| Protocol details are not public/verified | High | Adapter boundary, staged verification, no compatibility claims before tests |
| Opus dependency source unclear | Medium | Verify artifact repository/license first; keep codec behind an interface |
| Android device DSP availability varies | Medium | Runtime capability detection + graceful fallback |
| Audio focus / background service complexity | Medium | Explicit state machine, foreground service in a later phase |
| Low-latency expectations on Android hardware | Medium | Measure and document real figures; do not promise numbers |
| Gradle/AGP/Kotlin version incompatibility | Medium | Pin versions, build in CI on a clean runner, reproduce locally where possible |
| CI APK signed with no release key | Low | Release workflow produces unsigned artefacts until signing is configured |

---

## 7. What is deliberately out of scope for now

- Minecraft loader integration (Fabric/Forge/NeoForge) — Phase 5, and only after the Android audio foundation works.
- Any claim of interoperability with the reference server.
- Reverse engineering of packet layouts or encryption keys.
- Recording, groups, whispering, and 3D audio — documented as future work only.
