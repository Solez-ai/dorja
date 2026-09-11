# Dorja Homestation — agent rules

## Never compile or run Gradle locally

**Do not compile, assemble, test, or otherwise run Gradle on this machine.** That includes:

- `gradle`, `gradlew`, `./gradlew`, `.\gradlew.bat`
- Android Studio / local JDK builds
- `assembleDebug`, `assembleRelease`, `build`, `test`, `connectedCheck`, or any other Gradle task

Builds happen **only** through GitHub Actions (`.github/workflows/build-apk.yml`). Push to `main` or use `workflow_dispatch`. Debug APKs land on the `latest` GitHub Release and https://solez-ai.github.io/dorja/.

If a change needs a compile check, push and wait for CI. On failure, read the `ci-logs` branch (`ci-errors.txt`) or the Actions run log — do not try to reproduce the compile locally.
