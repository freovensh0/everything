# Everything

An open-source Android launcher that aims to reproduce the Nothing Launcher look:
4×6 grid, 72dp icons, dot-matrix typography, large folders, category-based app
drawer and a monochrome-first theme.

## Base

Fork of [Lawnchair 16](https://github.com/LawnchairLauncher/lawnchair), branch
`16-dev`, commit `d2f0c664` (2026-11-15), which is in turn built on AOSP
Launcher3 (Android 16).

## License

Apache License 2.0. See [LICENSE.txt](LICENSE.txt). AOSP copyright notices and
Lawnchair's modifications notice are retained there.

Nothing Launcher is proprietary; this project contains no code or assets copied
from it. Visual values (grid size, icon size, label size) are measurements, and
all resources here are original.

## Build

CI builds `assembleLawnWithQuickstepGithubDebug`; install that APK:

```
adb install -r build/outputs/apk/lawnWithQuickstep/github/debug/*.apk
```

Local build needs Android SDK with build-tools 37 and JDK 21.

## Status

- [x] Fork builds under our applicationId `dev.everything.launcher`
- [ ] Nothing-style device profile (4×6, 72dp icon, 13sp label)
- [ ] Default workspace (clock + weather cards, Google folder, 4-icon dock)
- [ ] Monochrome icon treatment and dot-matrix font fallback
- [ ] App drawer categories from a bundled package→category database
- [ ] Prediction row using recent-launch counters
