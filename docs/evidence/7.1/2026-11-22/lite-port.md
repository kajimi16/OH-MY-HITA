# HITA_Lite v2.1 UI port evidence

Date: 2026-11-22

Target decision: build-time legacy-hitalite UI module merged into the host APK, user-selected; not a runtime Compose plugin.

## Environment

- Emulator API 36, 1080x2400.
- Baseline APK: app/build/research/hita-lite-apk/app-release-v21.apk, package com.stupidtree.hitax.
- Port APK: io.github.kajimi16.ohmyhita debug.
- Light theme, Chinese.
- SSIM crop: y=120..2280, excluding status bar.

## Scores

| Page | SSIM |
|---|---:|
| Today | 0.997225 |
| Navigation | 0.996267 |
| Drawer | 0.985328 |
| Search (empty) | 0.997225 |
| Timetable | 0.967311 |
| Timetable manager (empty) | 0.966595 |

## Status

Today, navigation, drawer, and search meet the 0.98 threshold. Timetable stopped after three attempts. Manager has had one attempt. Remaining pages are in migration.

## Widget boundary

Lite widget layouts differ from plugin-hita layouts. The existing widget receivers
belong to plugin-hita; adding Lite receivers would violate the objective. Current
Lite preset reuses the host widget path, so Lite-specific widget pixels remain
unverified pending a host seam decision.

## Additional pages

| Page | SSIM / result |
|---|---|
| About | 1.000000 |
| Search (empty) | 0.997225 |
| Score entry, unlogged | opened; no crash |
| Exam entry, unlogged | opened; no crash |
| Classroom entry, unlogged | opened; no crash |
| Import entry, no timetable | opened; no crash |

Unit tests: `:app:testDebugUnitTest` BUILD SUCCESSFUL.

## Preset switch

Manually wrote preset_state to `hita-l-1.5.3` (active_shell removed), cold-started:
HITA-L shell appeared. Restored `hita-lite-2.0` + `active_shell=hita-lite-2.0`,
cold-started: Lite shell returned. No preset keys were dropped during the round trip.

## Smoke entries

Score, exam, empty classroom, import, search, about and drawer were opened on the
debug host without a fatal exception. Profile/news pages are compiled and registered
but have no visible Lite entry in the current shell, so they remain unverified.
