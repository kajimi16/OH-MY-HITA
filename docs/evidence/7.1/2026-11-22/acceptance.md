# HITA_Lite port acceptance

Date: 2026-11-22
Verdict: **NOT ACCEPTED**. Goal stays active.

## Objective form

User chose a build-time merged UI module (`:legacy-hitalite`), not a runtime
Compose/APK plugin. New Lite widget receivers were separately authorized.

## Delivered

- `:legacy-hitalite` Android library with Lite shell, three pages, timetable
  manager/detail, event dialogs, score, exam, empty classroom, import, search,
  teacher results, news, profile, about, agreement dialogs and widgets.
- `hita-lite-2.0` preset in `market/index.json`, main assets and debug assets.
- Shell mapping and host-service injection; launch from widget cold start.
- No second Lite Room/EAS/Retrofit/login implementation; pages use
  TimetableService/TimetableUiService/EasService/ScoreService/ExamService/
  EmptyClassroomService/TeacherService/UserProfileService.
- Debug-only ICS import receiver for fixed-data screenshots.

## Verified

- `:app:assembleDebug` exits 0.
- `:app:testDebugUnitTest` exits 0.
- Preset round-trip Lite → HITA-L → Lite keeps active preset/shell and returns
  the correct shell.
- Score, exam, empty classroom, import, search, about, drawer and timetable
  manager open without fatal exceptions.
- Fixed ICS import: 1 timetable, 9 events; today/timetable/navigation/manager/
  detail render.
- No legacy Room/Retrofit/AppDatabase/old repository implementations.

## SSIM (1080x2400, light, Chinese, crop y=120..2280)

| Page | Score | Result |
|---|---:|---|
| Today | 0.997225 | pass |
| Navigation | 0.996267 | pass |
| Drawer | 0.985328 | pass |
| Search | 0.997225 | pass |
| About | 1.000000 | pass |
| Manager | 0.996312 | pass |
| Timetable | 0.967311 | fail, stopped after 3 rounds |

Diff images: `diffs/`. Report: `ssim-report.json`.

## Not accepted

- Timetable diff shows two sets of grid/time rows: host `TimetableWeekView`
  metrics differ from Lite. Fixing requires replacing the host grid with Lite
  native fragment/view, which is forbidden by the page stop condition.
- Original Lite fixed-data baseline unavailable: release APK is not debuggable,
  has no local ICS import, and EAS import requires login. Detail/event/score/
  exam/classroom/import/news/profile fixed-state SSIM cannot be produced.
- Profile/news have no visible Lite entry in the shell; runtime unverified.
- Widgets are registered in `dumpsys appwidget` but were not placed on a
  launcher, so RemoteViews pixels are unverified.

This report must not be read as completion.
