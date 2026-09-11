# Dorja Homestation — agent rules

## Never compile or run Gradle locally

**Do not compile, assemble, test, or otherwise run Gradle on this machine.** That includes:

- `gradle`, `gradlew`, `./gradlew`, `.\gradlew.bat`
- Android Studio / local JDK builds
- `assembleDebug`, `assembleRelease`, `build`, `test`, `connectedCheck`, or any other Gradle task

Builds happen **only** through GitHub Actions. Do not compile or run Gradle locally.

- Android APK: `.github/workflows/build-apk.yml` — push to `main` or `workflow_dispatch`. Debug APKs land on the `latest` GitHub Release.
- Website (GitHub Pages): `.github/workflows/deploy-pages.yml` — builds `dorja-website-portable` and publishes https://solez-ai.github.io/dorja/. Do not run `pnpm build` locally for Pages; CI owns that too.

If a change needs a compile check, push and wait for CI. On failure, read the `ci-logs` branch (`ci-errors.txt`) or the Actions run log — do not try to reproduce the compile locally.
