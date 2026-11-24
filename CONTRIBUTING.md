# Contributing to Voice Chat+

Thank you for helping build Voice Chat+. Please keep changes small, verifiable and in scope.

## Ground rules

1. **No RKB branding.** This project is independent. Do not add RKB names, logos, package names, or references to other RKB projects in code, UI, docs, or commit messages.
2. **No secrets.** Never commit tokens, credentials, keystores, signing passwords, or `local.properties`. Use GitHub Secrets for CI credentials.
3. **Clean room.** Do not copy, translate, or adapt source from Simple Voice Chat or any all-rights-reserved project. Do not bring code from a project you contributed to unless you have the right to relicense it. Read `docs/RESEARCH.md` first.
4. **No fake implementations.** Do not add empty methods that pretend to work, hardcoded "success" results, or placeholder network responses. If something is not implemented, say so in docs and keep it out of the build path.
5. **No compatibility claims.** Do not state or imply Simple Voice Chat interoperability until an end-to-end test against a real server has passed and been documented.
6. **No destructive Git operations.** No force pushes, branch deletion, history rewriting, or pushes to any repository other than `Voice-Chat-Plus`.

## Workflow

- `main` is the stable branch; `develop` is the integration branch. Open pull requests into `develop`.
- Use logical, focused commits with imperative messages, e.g. `Add PCM ring buffer with unit tests`.
- Keep each commit buildable where practical.
- Format Kotlin with the official style (`kotlin.code.style=official` is set).
- Add tests for new logic in JVM modules. Run `./gradlew test` before opening a PR.
- Run `./gradlew :android-app:assembleDebug :android-app:lintDebug` when touching Android code.

## Code organisation

- Never put protocol logic in `android-app`.
- Never put Android API calls in `common`, `audio-engine`, `voice-protocol`, `network-engine`, or `minecraft-integration`.
- Keep `audio-engine` independent of the network stack; it deals in PCM frames and DSP only.
- New platform capabilities must be capability-detected at runtime (e.g. Android audio effects), never assumed.

## Reporting issues

Include device model, Android version, `adb logcat` output (with personal data removed), the exact build/commit, and steps to reproduce. For compatibility reports, include server version and loader.

## License

By contributing, you agree that your contributions are licensed under the MIT License in `LICENSE`.
