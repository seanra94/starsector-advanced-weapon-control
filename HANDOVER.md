# HANDOVER.md

## Project purpose

Advanced Gunnery Control Fork is a Starsector utility mod derived from Advanced Weapon Control / Advanced Gunnery Control. It lets players assign autofire weapon-group tags and ship AI modes from combat, refit, campaign, and suggested-tag GUIs.

## User goals

- Maintain this as a separate fork, not a replacement for the original AGC install.
- Continue the GUI work from the prior agent, especially the campaign ship editor and suggested-tags screens.
- Keep changes minimal and verified with the smallest useful Gradle/game check.
- Push project changes to GitHub.

## Mod and game context

- Local repo: `D:\Sean Mods\Advanced Gunnery Control Fork`
- GitHub remote: `https://github.com/seanra94/starsector-advanced-weapon-control`
- Current branch when this handover was rewritten: `codex/agc-fork-baseline-tag-standardization`
- Fork mod id: `advanced_gunnery_control_fork_dbeaa06e`
- Display name / folder name: `Advanced Gunnery Control Fork` / `Advanced-Gunnery-Control-Fork`
- Intended live mod target: `C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork`
- Starsector root used by the build: set `STARSECTOR_DIRECTORY=C:\Games\Starsector`
- Build dependencies are resolved from `C:\Games\Starsector\starsector-core` and installed mod folders under `C:\Games\Starsector\mods` for MagicLib, LazyLib, LunaLib, and Console Commands.

## Architecture and file layout

- `src/main/kotlin/com/dp/advancedgunnerycontrol/gui/` contains the combat/refit/campaign GUI code.
- `AGCGUI.kt` opens the campaign fleet picker and then the custom campaign ship editor dialog.
- `CampaignShipEditorDialog.kt` owns the full-screen custom visual campaign editor shell, action rail, fallback error panel, and forwarded input routing.
- `ShipView.kt` owns the campaign ship editor body layout: misc column, ship picture/details, options insertion point, ship modes, seven weapon-group columns, weapon entries, tag lists, pinned selected tags, and per-column scroll regions.
- `CampaignGuiStyle.kt` centralizes campaign/suggested GUI dimensions, border modes, label truncation helpers, and wrapped label sizing.
- `CampaignPanelPlugin.kt` draws the production row/container fills and outlines used by campaign and suggested-tag custom panels.
- `TagButton.kt`, `ShipModeButton.kt`, and `SuggestedTagButton.kt` build campaign-style item controls over Starsector area checkboxes while rendering visible text separately.
- `src/main/kotlin/com/dp/advancedgunnerycontrol/gui/suggesttaggui/` contains the suggested-tags screen. `SuggestedTagGui.kt` owns the custom dialog/action rail; `SuggestedTagGuiView.kt` owns the seven-column weapon grid; `WeaponFilter.kt` defines type/size/fire-form/range filters.
- `src/main/kotlin/com/dp/advancedgunnerycontrol/weaponais/tags/` contains weapon AI tag implementations, including the fork's sync/ambush work.
- `build.gradle.kts` is the source of truth for generated `mod_info.json`, version files, and generated `Settings.editme` content.

## Build and verification commands

Run from `D:\Sean Mods\Advanced Gunnery Control Fork`:

```powershell
$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat compileKotlin
```

Package/generated-file check:

```powershell
$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat jar create-metadata-files write-settings-file
```

Useful runtime log:

```text
C:\Games\Starsector\starsector-core\starsector.log
```

## Packaging and deploy workflow

- The checked-in Gradle `install-mod` task is disabled.
- Existing deploy practice has been to package the jar, then copy the repo contents to `C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork`, excluding `.git`, `.github`, `.gradle`, `.idea`, `.run`, `gradle`, and `build`.
- For local testing, always generate the updated jar and deploy/copy the mod to `C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork` after code changes, unless explicitly told not to.
- Do not update release/version metadata unless explicitly doing a release/version task.
- Do not overwrite the original/non-fork AGC mod folder.
- After deploy, a small useful check is comparing the repo jar and deployed jar SHA-256 and size.

## Stable decisions

- Preserve the forked identity in generated files, runtime metadata, docs, and deploy target.
- Preserve the current sync/ambush weapon-behavior model unless a focused runtime log proves it is the issue. The best baseline from prior user testing had `SyncWindow`, `SyncVolley`, and `Ambush` working well.
- Sync and hold-style tags must not interrupt a weapon sequence that a player could not manually interrupt. Avoid `stopFiring()` and avoid blocking an already-started charge, burst, or beam sequence.
- Sync readiness should be group-level and target-convergent: participants must be mechanically ready, in effective range, aligned on their predicted aim solution, and able to join the shared release target.
- `SynchronizedFireTag.evaluate(...)` is intentionally called from multiple `TagBasedAI` hooks in a frame. Do not add broad per-frame memoization unless runtime evidence proves a safe boundary, because target candidates and protected-sequence state can change between hooks.
- Threshold weapon tags are sign-sensitive. New syntax such as `TargetShield(SF>10%)`, `AvoidShield(SF>10%)`, `TargetShield(TF>10%)`, `AvoidShield(TF>10%)`, and `PD(SF>10%)` should parse `10%` as `0.10`, not `0.90`. Legacy aliases may still require inversion depending on their old semantics.
- Soft-flux conditional tags should use clearly named helpers for activation semantics. The intended `SF>N%` rule is “soft flux greater than N and total flux below `Settings.softFluxTotalFluxCap()`.” Avoid relying on negated `softFluxBelowThreshold(...)` for `SF>` activation.
- Threshold variants of `TargetShield` / `AvoidShield` currently preserve the original-style target-priority bias pattern even when the firing restriction is not active. Leave this alone unless intentionally doing a future balance/tuning pass; the user does not want to change original-author feel without testing.
- `IgnoreMinorPD` should account for hull, armor, and shield durability. Prefer remaining shield buffer over current flux when estimating fighter shield contribution, if the Starsector API exposes a compile-safe max-flux/shield-efficiency path.
- User-facing tag names and tooltips should be standardized by behavior family. Conditional variants should reuse the same baseline wording as the plain tag and only add the flux activation condition.
- `BurstPD(SF>N%)` should become canonical `PD(SF>N%)`, while old `BurstPD...` strings remain legacy aliases for saved/settings/loadout compatibility.
- When preserving legacy `BurstPD(SF>N%)` compatibility, verify whether the old parser inverted the threshold. If so, old `BurstPD(SF>90%)` should normalize to canonical `PD(SF>10%)`, while new canonical `PD(SF>10%)` remains literal.
- A generic flux-condition parser/helper may be useful later, but broad tag-class refactors should be staged separately from naming/tooltip standardization.
- The `completeTagList` baseline/coverage pass is complete; treat the current canonical list in `build.gradle.kts` as the user-facing ordering baseline.
- The narrow TF/SF helper pass is accepted groundwork and complete for this stage.
- Canonical parameterized ammo-threshold support is complete with renamed canonicals: `Opportunist(A<...)` (ammo-gated opportunist) and `PD(A<...)` (ammo-gated PD reservation). Legacy names `ConserveAmmo` / `ConservePDAmmo` / `CnsrvPDAmmo` remain compatibility aliases.
- The next preferred implementation task is `IgnoreMinorPD(H<...)`; later follow-up remains HF support where TF/SF currently exist and LunaSettings exposure of the soft-flux total-flux cap.
- Threshold-class naming direction for future touched code: prefer comparator-explicit names such as `...AboveFluxTag` / `...BelowFluxTag`; avoid vague new `At...` names.
- If/when threshold behavior families are consolidated safely, prefer one behavior class per family plus a shared FluxCondition model over separate SoftFlux/TotalFlux classes.
- `BurstPDSoftFluxTag` is semantically outdated relative to canonical `PD(SF>N%)` and is an internal rename candidate when that area is touched again.
- Legacy settings-backed wrappers `TargetShieldAtTotalFluxTag` / `AvoidShieldAtTotalFluxTag` should be treated as legacy wrappers, not preferred naming models.
- Naming/review wave should be staged after behavior/plumbing work: incompatibility audit, tooltip accuracy audit, README/examples, text consistency sweep, `PrioPD -> PrioSmall` review, `BigShip/SmallShip -> TargetBig/TargetSmall` review, `TargetPhase -> PrioPhase` review, and adding `PrioBig`.
- Canonical user-facing rename goals for a later wave: `Hold(...) -> HoldFire(...)` (with `Hold(...)` aliases preserved) and `ForceAF -> ForceAutoFire` (with `ForceAF` alias preserved). Check UI label density before applying these.
- `IgnoreMinorPD(H<...)` should be documented as effective durability (combined durability concept), not literal hull.
- The user requested `PrioBig` twice in backlog discussion; track it once.
- Campaign tag scrolling should use the single forwarded `InputEventAPI` wheel path with explicit event consumption. Do not reintroduce direct LWJGL mouse polling unless coordinate logs prove it is needed.
- Campaign selected tags are pinned at the top of each tag column, removed from the normal scroll slice while pinned, and use pale yellow selected backgrounds.
- Campaign/suggested tag labels currently render as plain single-label text. Avoid `addParaWithMarkup()`, highlighted `addPara(...)` overloads, or segmented text for tag cells because earlier attempts leaked literal markup, crashed on `%`, or clipped characters in-game.
- Section headings such as `Ship`, `Options`, `Ship Modes`, and group headings should remain Starsector `addSectionHeading(...)` bars.
- Empty weapon groups keep their seven-column layout footprint; recent behavior shows the group heading but leaves the rest blank.
- Suggested-tags GUI mirrors campaign tag behavior: forwarded wheel scrolling, pinned selected tags, selected tags removed from normal rows, and colored tag-label segments.

## Sensitive areas

- `assignShipModes()` and `shouldNotOverrideShipAI()` in `ShipModes.kt`.
- `CustomShipAI.advance()` delegation to `baseAI`.
- Any use of `ship.resetDefaultAI()`: recent history indicates this caused unintended AI resets.
- Settings additions may need matching updates in `Settings.kt`, `Settings.editme` generation in `build.gradle.kts`, and `data/config/LunaSettings.csv`.
- Campaign GUI row backgrounds are primarily controlled by row/panel fill plugins rather than only the colors passed to `addAreaCheckbox(...)`. Transparent row fills can read as black from the parent container.
- Weapon tag renames are compatibility-sensitive because persisted saves/settings/loadouts may contain old tag strings. Prefer legacy aliases that normalize to the new canonical tag over breaking existing stored data.

## Known pitfalls and lessons learned

- Starsector custom UI components must be created by the same panel they are added to. Creating a `TooltipMakerAPI` from a parent and adding it to a child panel caused crashes.
- `addPara(...)` highlighted overload treats `%` as a formatter marker. Labels like `Hold(TF>90%)` can crash if passed through the wrong overload.
- Starsector did not render arrow glyphs consistently in this UI; scroll indicators use ASCII `^ ^ ^` and `v v v`.
- Direct LWJGL mouse coordinates previously routed wheel input to the wrong tag columns after rebuilds. Preserve forwarded input routing unless there is new evidence.
- Rebuilds reset scroll state unless `captureTagScrollOffsets()` is preserved and passed back as `initialTagScrollOffsets`.
- MagicLib combat/refit buttons are narrow and do not clip long text. Widening combat/refit tag buttons reduces visible tag capacity on 1080p and is a poor default tradeoff.
- Temporary complete-list scroll-test tags were removed from `Settings.getCurrentWeaponTagList()` after GUI scroll validation.
- `SynchronizedFireTag.kt` has `DEBUG_SYNC = false` by default. Turn it on only for focused runtime sync diagnostics.

## Open questions

- Confirm in-game that the latest campaign/suggested plain tag labels still render without clipping.
- Confirm suggested-tags reset flow, filter actions, page controls, and Esc/back behavior in-game.
