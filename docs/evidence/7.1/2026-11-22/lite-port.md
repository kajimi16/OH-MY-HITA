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

## Detail/event

LiteTimetableDetailActivity, subject/teacher adapters and Lite event add/info
dialogs were migrated against TimetableService/TimetableUiService. Build passes.
They are not screenshot-verified because no fixed timetable data was seeded yet.

`:app:testDebugUnitTest` passes after this slice. Legacy module audit finds no
Room/Retrofit/AppDatabase/old repositories or AppWidgetProvider declarations.

## Fixed data

Debug receiver imports docs fixture `fixtures_week12_multi.ics` via TimetableService.
Host rendered 1 timetable and 9 imported events. Fixed-data screenshots were taken
for today, timetable, navigation, manager and detail; detail/manager opened without
crash. Original Lite fixed-data baseline was not seeded, so SSIM for fixed states
remains unverified.

## Widgets

LiteTodayWidget, LiteTodayWidgetSlim and LiteListWidgetService are registered; the
system provider list shows both receivers. Lite services now bind in
HApplication.onCreate, and WidgetUtils refreshes Lite widget classes. No launcher
instance was placed, so widget pixels remain unverified.
