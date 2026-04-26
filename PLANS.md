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
- [ ] Colored tag labels render correctly in-game without clipping, formatter crashes, or visible markup tokens.
- [ ] Temporary scroll-test tags and sync debug logging are removed or intentionally retained with a documented reason.
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

The best recent campaign baseline uses forwarded `InputEventAPI` wheel handling, pinned selected tags, no duplicate selected tags in normal rows, seven fixed weapon-group columns, full-width item rows, ASCII scroll indicators, direct colored text segments, and fallback error panels for failed custom UI builds.

## Near-term queue

- Confirm or reject the suspected `saveShipModesInShip()` custom-data key mismatch in `ShipModes.kt`.
- Finish campaign GUI row/background polish: consistent inactive grey rows, matching confirm/cancel text brightness, grey scroll rows, and brighter incompatible-tag red.
- Rename `PrioritisePD` to canonical `PrioPD` while preserving legacy `PrioritisePD` loading compatibility.

## Plan

- [ ] Run a focused compile check on the inherited dirty tree before new GUI edits if the next task changes Kotlin.
- [ ] Inspect the exact in-game issue or screenshot the user provides before changing layout constants.
- [ ] Make the smallest local GUI change in the owning file.
- [ ] Re-run `compileKotlin`.
- [ ] Package/deploy only when the user needs to test in Starsector.
- [ ] After GUI behavior is validated, remove `completeListScrollTestTags` if no longer needed.
- [ ] After sync testing is complete, set `DEBUG_SYNC` false unless the user wants ongoing diagnostics.
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
- Existing `Settings.kt` scroll-test tags are selectable real tags and should not ship unintentionally.
- Existing sync debug logs can add noise to user testing and should not ship unintentionally.
- Suggested-tags direct colored text segments still need in-game confirmation after the clipping fix.

## Current status

Documentation has been reset to the templates and filled with durable project state. GUI code is inherited from the prior agent and still needs runtime validation before further cleanup.
