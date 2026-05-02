# Active task

Campaign GUI consistency, visual semantics, and navigation follow-up after the preset-controls and dangerous-action confirmation passes.

# Current status

The external weapon-composition preset flow is implemented and functioning.
Campaign `Save` / `Load` preset controls now sit at the top of each non-empty weapon-group container and use explicit confirm/cancel.
Inline save/load status text is removed.
Dangerous campaign options actions now use confirm/cancel protection, including copy-previous-loadout variants, reload settings, reset, and load suggested modes.
Suggested-tags customization `Backup` and `Restore` now use confirm/cancel protection.
Weapon-group headings now use the custom heading path instead of Starsector built-in section headings, and the stale marker behavior is accepted:
- dirty group => `Group N (*)`
- clean group => `Group N`
- no preset + empty current tags => clean
- no preset + non-empty current tags => dirty

The current implementation direction is now:
- custom container headings rather than built-in section headings
- compact centered top-row controls
- explicit confirm/cancel for impactful actions
- dirty-state weapon-group headings as the visual signal for divergence from saved preset state

# Goal

Make the campaign ship editor and suggested-tags customization UI visually consistent, easier to read, and safer against misclicks while preserving the accepted preset semantics and current confirmation model.

# Current understanding

All current container headings on the touched campaign/suggested-tags surfaces should use the custom heading system going forward.
Default heading background should be darker than the current implementation, while stale weapon-group headings should remain visibly distinct from clean groups.
`Save`, `Load`, and ship-mode labels should remain properly centered.
Ship-mode text color should match the normal tag-label text color rather than using a separate green tint.
Save-related actions and load-related actions should have distinct dark color semantics so the action family is visually obvious.
Active weapon tags and active ship modes should share a dark teal selected-state treatment.
The weapon-group page no longer needs a `Back` button.
On the suggested-tags customization page, `Back` should return to the weapon-group page rather than the ship-select page.
Pressing `Escape` on either the weapon-group page or the suggested-tags customization page should return to the ship-select page.

# Near-term queue

## Heading system and heading colors
- Use the custom heading system for all current container headings on the touched campaign and suggested-tags surfaces, and treat that as the default approach for future container headings in this UI family.
- Change default custom heading background to `Color(40, 40, 40, 225)`.
- Keep stale weapon-group headings visually lighter at `Color(60, 60, 60, 225)`.
- Preserve accepted stale-marker semantics exactly.
- Keep heading text readable and centered.

## Label colors and action-family colors
- Change ship-mode label text to the same color used for normal tag text.
- Make save-related buttons use a dark blue background/highlight family around `Color(0, 0, 60, 225)`.
- Make load-related buttons use a dark yellow background/highlight family around `Color(60, 60, 0, 225)`.
- Save-related buttons include:
  - `Save`
  - `Copy to other ships of same variant`
  - all variants of `Copy to other ships of same variant`
  - `Backup`
- Load-related buttons include:
  - `Copy previous loadout`
  - all variants of `Copy previous loadout`
  - `Load suggested modes`
  - all variants of `Load suggested modes`
  - `Reset [DELETE]`
  - all variants of reset
  - `Reload Settings`
  - all variants of reload settings
  - `Restore`

## Selected-state colors
- Change the background and hover background of active weapon tags and active ship modes to a dark teal selected-state treatment around `Color(0, 60, 60, 225)`.
- Apply the same selected-state treatment consistently where the same active-state semantics are used on the touched campaign/suggested-tags surfaces.

## Options-panel ordering and navigation
Set the campaign options panel order to:

1. `Cycle loadout [Current 1 / 3] <Normal>`
2. `Next Ship [TAB]`
3. `Copy previous loadout`
4. `Load suggested modes`
5. `Reset [DELETE]`
6. `Reload Settings`
7. `Copy to other ships of same variant`
8. `Customize suggested tags`
9. `Switch to simple mode`

## Suggested-tags customization ordering and navigation
Set the suggested-tags customization page order to:

1. `Next Page [RIGHT][D]`
2. `Prev Page [LEFT][A]`
3. `Filter... [F]`
4. `Reset`
5. `Backup`
6. `Restore`
7. `Back [ESCAPE]`

Navigation behavior:
- remove `Back` from the weapon-group page
- on the suggested-tags customization page, `Back` returns to the weapon-group page
- pressing `Escape` on either page returns to the ship-select page

# Acceptance criteria

- Current touched container headings use the custom heading path rather than Starsector built-in section headings.
- Default heading background is `Color(40, 40, 40, 225)`.
- Stale weapon-group heading background is `Color(60, 60, 60, 225)`.
- Clean weapon-group headings use the default heading background.
- Stale-marker semantics remain:
  - dirty weapon group => `Group N (*)`
  - clean weapon group => `Group N`
  - no preset + empty current tags => clean
  - no preset + non-empty current tags => dirty
- `Save`, `Load`, and ship-mode labels remain visually centered.
- Ship-mode text color matches ordinary tag text color.
- Save-related buttons use the dark blue family.
- Load-related buttons use the dark yellow family.
- Active weapon tags and active ship modes use the dark teal selected-state treatment.
- Campaign options panel order matches the requested order.
- Suggested-tags customization page order matches the requested order.
- `Back` is removed from the weapon-group page.
- `Back` on suggested-tags customization returns to the weapon-group page.
- `Escape` on either page returns to the ship-select page.
- Existing accepted confirm/cancel protections continue to work and do not regress.
- compileKotlin passes before push.

# Constraints

Minimize unrelated changes.
Preserve current preset storage and matching semantics.
Preserve accepted stale-marker semantics.
Do not silently mutate other groups when saving presets.
Do not re-open phase-tag behavior from static reasoning alone.
Do not overwrite the original AGC mod folder.
Treat `build.gradle.kts` as the source of truth for generated `mod_info.json`, version files, and `Settings.editme`.
For Luna-exposed settings, treat `data/config/LunaSettings.csv` defaults as the source of truth.
Preserve persisted tag, loadout, and legacy-alias compatibility when changing surrounding code or user-facing text.

# Verification needed

Minimum code check:

```powershell
$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat compileKotlin
