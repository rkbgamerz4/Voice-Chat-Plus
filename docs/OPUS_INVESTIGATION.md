# Opus Implementation Investigation — Phase 3

**Date:** 2026-11-24
**Conclusion:** No Opus dependency was added. The investigation below is verified; the artifact availability requirement was not met, so integration is deferred rather than guessed.

---

## 1. Requirement

Find a maintained, verifiable Opus implementation and verify: dependency availability, Android ARM64 compatibility, license, encode/decode APIs, and sample-rate support. Do not invent APIs or add an unverified dependency.

## 2. Candidate examined: Concentus (pure Java)

Source: `https://github.com/lostromb/concentus`, default branch `master` (repository last pushed 2026-11-17). Java sources live at `Java/Concentus/src/main/java/org/concentus/`.

### 2.1 License — verified

`LICENSE` is the BSD-style Opus license. It lists multiple copyright holders (Skype Limited, Xiph.Org Foundation, CSIRO, Microsoft Corporation, Jean-Marc Valin, Gregory Maxwell, Mark Borgerding, Timothy B. Terriberry, Logan Stromberg). Redistribution in source and binary form is permitted provided the copyright notice, conditions and disclaimer are retained, and contributor names are not used for endorsement.

Assessment: compatible with this MIT project **if** the full notice is included in distributions. GitHub reports the license as `NOASSERTION` because it is a multi-holder custom BSD notice, so it must be handled explicitly rather than treated as MIT.

### 2.2 ARM64 compatibility — verified by construction

Concentus is pure Java with no JNI and no native `.so` files. It runs on ART on any Android ABI, including ARM64. No NDK toolchain or per-ABI packaging is required. This is the strongest argument for the candidate.

### 2.3 Sample-rate support — verified in source

`OpusEncoder(int Fs, int channels, OpusApplication application)` explicitly accepts **8000, 12000, 16000, 24000 and 48000 Hz** and **1 or 2 channels**. 48 kHz mono (our `PcmFormat.VOICE`) is supported. `OpusDecoder(int Fs, int channels)` uses the same rate set.

### 2.4 APIs — verified by reading source (not assumed)

```java
// OpusEncoder.java
public OpusEncoder(int Fs, int channels, OpusApplication application) throws OpusException;
public int encode(short[] in_pcm, int pcm_offset, int frame_size,
                  byte[] out_data, int out_data_offset, int max_data_bytes) throws OpusException;
public int encode(byte[] in_pcm, int pcm_offset, int frame_size,
                  byte[] out_data, int out_data_offset, int max_data_bytes) throws OpusException;
public void setBitrate(int value);
public void setComplexity(int value);
public boolean getUseDTX();
public void setUseDTX(boolean value);
public boolean getUseInbandFEC();
public void setUseInbandFEC(boolean value);
public int getLookahead();
public int getSampleRate();

// OpusDecoder.java
public OpusDecoder(int Fs, int channels) throws OpusException;
public int decode(byte[] in_data, int in_data_offset, int len,
                  short[] out_pcm, int out_pcm_offset, int frame_size,
                  boolean decode_fec) throws OpusException;

// OpusApplication.java
OPUS_APPLICATION_UNIMPLEMENTED, OPUS_APPLICATION_VOIP,
OPUS_APPLICATION_AUDIO, OPUS_APPLICATION_RESTRICTED_LOWDELAY
```

`OPUS_APPLICATION_VOIP` is the appropriate mode for speech.

### 2.5 Dependency availability — FAILED verification

| Repository / coordinate | Result |
|---|---|
| Maven Central search for `org.concentus:Concentus` | 0 results |
| `repo1.maven.org/.../org/concentus/Concentus/maven-metadata.xml` | 404 |
| `repo1.maven.org/.../org/concentus/concentus/maven-metadata.xml` | 404 |
| JitPack `com.github.lostromb:concentus:v1.0-java` POM | 404 |
| Project's own `Java/Concentus/pom.xml` | `org.concentus:Concentus:1.0-SNAPSHOT` — a snapshot, not a published release |

The source is available and the API is known, but there is **no verifiable published binary artifact** from a public repository. Adding a guessed coordinate would violate the task constraint.

### 2.6 Maintenance caveat

The repository is active, but the most recent Java release tag is `v1.0-java` from **2016-10-10**; recent repository activity is not evidence of an updated Java artifact. A dependency on an unmaintained Java port needs a vendoring strategy and its own test suite before production use.

## 3. Alternatives considered

| Option | Assessment |
|---|---|
| Native libopus via JNI | Excellent performance and actively maintained upstream, but requires NDK, per-ABI `.so` packaging (ARM64 + others), reproducible native builds and license notices. Not justified for the current milestone. |
| Android `MediaCodec` Opus | No external dependency, but encoder support is device-dependent and not guaranteed on all API 26+ devices; unsuitable as the primary path. |
| Reference project's `opus4j` | Native bindings from an all-rights-reserved project; excluded by the clean-room policy in `docs/RESEARCH.md`. |

## 4. Decision

**Defer Opus integration.** The audio pipeline currently stops at exact 20 ms PCM frames produced by `PcmFrameProcessor`, exposed via `MicrophoneCapture.onFrame`. This is a real, usable boundary: a verified codec can be added later without changing the capture path.

Before adding any codec, one of these must be true:

1. A published artifact with a resolvable coordinate, declared license and Android-compatible bytecode is verified; or
2. The project explicitly chooses to vendor the Concentus Java source, includes the full BSD notice, and adds round-trip encode/decode tests.

No compatibility claim with any third-party voice protocol is made in the meantime.
