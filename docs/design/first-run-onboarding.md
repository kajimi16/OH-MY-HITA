# First-run onboarding redesign

## Reference patterns

Reviewed open-source onboarding examples (OnboardingKit, Appllama welcome screens,
Jetpack Compose onboarding samples) and onboarding UX guidance. Common patterns
worth copying:

- One message per screen; do not combine explanation and configuration.
- Value proposition before personalization; the user chooses a preset only after
  understanding why presets exist.
- Single primary CTA, secondary escape hatch, visible progress (`1 / 2`).
- Card-based choice with a stable selected state, not a radio list in an
  AlertDialog.
- Loading/error/empty/applying are explicit states, never fake progress.

## Flow

1. Intro: hero + `万物皆插件`; three concepts (微内核 / 按需组合 / 随时切换);
   primary `选择整合包`, secondary `从零开始`.
2. Preset: preset cards from the live market index; recommended and active
   badges; apply and start.

## Scope

- `OnboardingContract` freezes state and actions.
- `PresetOnboardingViewModel` adapts `PluginMarketViewModel`.
- Intro and preset screens are separate Compose files.
- `PresetOnboardingActivity` owns flow and marks onboarding seen only on
  completion; MainActivity only routes to it.
