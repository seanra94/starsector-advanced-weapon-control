# PLANS.md

## Active task

Resume GUI work on Advanced Gunnery Control Fork.
Status: active UI polish and tag-rename follow-up.

## Goal

Stabilize the campaign ship editor and suggested-tags custom visual dialogs so they are usable in-game at the target Starsector UI scales, then remove temporary diagnostics/test scaffolding when no longer needed.

## Why this task matters

The prior agent moved the campaign and suggested-tags workflows into custom full-screen GUIs. The user wants to continue from that point, so the next work should preserve the best working baseline while tightening layout, scrolling, coloring, and runtime behavior.

## Acceptance criteria

- [ ] Campaign ship editor opens from the campaign `J` hotkey, selects a ship, and returns safely with `Esc`/Back.
- [ ] Campaign seven-column weapon-group layout remains readable at the user's target scales, with empty groups handled cleanly.
- [ ] Per-column tag scrolling affects only the hovered column and selected pinned tags update immediately.
- [ ] Suggested-tags GUI opens, paginates, filters, pins selected tags, scrolls per weapon column, and returns to AGC safely.
- [ ] Plain tag labels render correctly in-game without clipping, formatter crashes, or visible markup tokens.
- [x] Temporary scroll-test tags and sync debug logging are removed or intentionally retained with a documented reason.
- [ ] `compileKotlin` passes before pushing.

## Constraints

- [ ] Minimize unrelated changes.
- [ ] Preserve existing style and structure.
- [ ] Prefer targeted verification first.
- [ ] Do not revert prior-agent/user work in the dirty tree.
- [ ] Do not overwrite the original AGC mod folder.
- [ ] Treat `build.gradle.kts` as the source for generated `mod_info.json`, version files, and `Settings.editme`.

## Current understanding

The current GUI work is centered on `CampaignShipEditorDialog.kt`, `ShipView.kt`, `CampaignGuiStyle.kt`, `TagButton.kt`, `ShipModeButton.kt`, `SuggestedTagGui.kt`, `SuggestedTagGuiView.kt`, `SuggestedTagButton.kt`, and `WeaponFilter.kt`.

The best recent campaign baseline uses forwarded `InputEventAPI` wheel handling, pinned selected tags, no duplicate selected tags in normal rows, seven fixed weapon-group columns, full-width item rows, ASCII scroll indicators, plain single-label tag text, dynamic wrapped action-row sizing, and fallback error panels for failed custom UI builds.

## Near-term queue

- Confirm or reject the suspected `saveShipModesInShip()` custom-data key mismatch in `ShipModes.kt`.
- Continue campaign/suggested GUI polish only from concrete in-game regressions.
- Fix and validate literal threshold semantics for soft/total flux weapon tags: `TargetShield(SF>N%)`, `AvoidShield(SF>N%)`, `TargetShield(TF>N%)`, `AvoidShield(TF>N%)`, `PD(SF>N%)`, and verify `Force(SF<N%)` / `IgnoreMinorPD`. For `SF>N%`, activation means soft flux greater than N and total flux below the configured cap.
- Standardize weapon tag names/tooltips: rename canonical `BurstPD(SF>N%)` to `PD(SF>N%)`, preserve legacy aliases, and make PD/TargetShield/AvoidShield conditional tooltips use shared baseline wording plus only the flux condition.
- Improve `IgnoreMinorPD` effective durability estimation if compile-safe: use remaining shield buffer rather than current flux for fighter shield contribution, while preserving hull/armor/missile behavior.

## Plan

- [ ] Run a focused compile check on the inherited dirty tree before new GUI edits if the next task changes Kotlin.
- [ ] Inspect the exact in-game issue or screenshot the user provides before changing layout constants.
- [ ] Make the smallest local GUI change in the owning file.
- [ ] Re-run `compileKotlin`.
- [ ] Package/deploy only when the user needs to test in Starsector.
- [x] Remove `completeListScrollTestTags` once scroll validation is no longer needed.
- [x] Set `DEBUG_SYNC` false unless the user wants ongoing diagnostics.
- [ ] Commit and push the confirmed scope to GitHub.

## Verification needed

Minimum code check:

```powershell
$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat compileKotlin
```

For deploy/test passes, package with:

```powershell
$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat jar create-metadata-files write-settings-file
```

Then copy to `C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork` using the established exclusions and compare repo/deployed jar hashes.

## Risks and open questions

- Runtime UI behavior cannot be fully proven by compile alone because Starsector custom UI layout has engine-specific quirks.
- Suggested-tags plain labels still need in-game confirmation after the clipping fix.

## Current status

Documentation has been reset to the templates and filled with durable project state. GUI code is inherited from the prior agent and still needs runtime validation before further cleanup.
