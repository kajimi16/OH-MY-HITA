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
