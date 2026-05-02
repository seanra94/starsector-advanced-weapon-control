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
- Canonical minor-PD waste suppression is `NoPD(Waste>...)`, meaning do not target fighters/missiles when this weapon would waste more than the configured share of its estimated attack packet damage. `NoPD(H<...)` remains supported as the simpler health-threshold form, and legacy `IgnoreMinorPD` / `IgnoreMinorPD(H<...)` names remain compatibility aliases.
- `NoPD(Waste>...)` uses a bounded waste ratio and exempts weapons at or below `noPDWasteCleanupDamageCap` so low-damage PD can clean up nearly-dead targets. Its attack-packet estimate treats continuous beams as `beamDps * 0.5s`, but burst beams as one committed burst packet: `beamDps * (burstDuration + beamChargeupTime / 3 + beamChargedownTime / 3)`. This matches vanilla Phase Lance/Tachyon Lance burst-damage expectations and avoids treating burst beams like short continuous cleanup beams.
- HF support is complete for conditional tag families that already had TF/SF forms.
- `SFTUpperFluxLimit` / `Settings.softFluxTotalFluxCap()` is now Luna-exposed with default `0.9`; Settings.editme fallback remains supported. This cap is the total-flux safety cap used by soft-flux conditional tags.
- Default-source decision: current fork `data/config/LunaSettings.csv` defaults are authoritative for Luna-exposed settings. Commit `e74fc07` aligned `Settings.kt` runtime defaults and generated `Settings.editme` fallback defaults to current Luna defaults; `data/config/LunaSettings.csv` was unchanged.
- Original-upstream default restoration is a lowest-priority backlog task. When reached, restore Luna and Settings defaults to original upstream defaults, but if original upstream LunaSettings and upstream Settings/runtime defaults differ, prefer original upstream LunaSettings. Do not restore upstream tag-list contents, old tag names, fork metadata, generated version files, or other non-default surfaces unless explicitly requested.
- Current fork values backed up for the future upstream-default restoration task:
  - Luna `agc_opportunist_HEThreshold`: original upstream Luna default `0.2`; current fork Luna/runtime/generated value `0.15`.
  - Luna `agc_spamSystemPreventsDeactivation`: original upstream Luna default `true`; current fork Luna/runtime value `false`.
  - Generated `Settings.editme` fallback `conservePDAmmo_ammo`: original upstream generated fallback `0.8`, but upstream runtime default is `0.9`; current fork runtime/generated fallback/Luna-facing behavior is `0.9`. Do not restore `0.8` blindly without deciding whether original generated fallback or original runtime behavior is authoritative.
  - Luna duplicate `agc_ignoreFighterShields`: original upstream LunaSettings contains conflicting duplicate defaults (`false` in the AI tab and `true` in the Advanced tab), while upstream runtime/generated fallback and current fork use `true`. Do not reintroduce the duplicate unless deliberately restoring upstream Luna surface quirks.
- Fork-only defaults with no original upstream counterpart should normally be retained unless explicitly removed: `noPDWasteCleanupDamageCap = 100` and `SFTUpperFluxLimit = 0.9`.
- Runtime/generated fallback defaults for Luna-exposed settings are now aligned to current Luna defaults. The resolved mismatches were: `listVariant=classic`, `messageDisplayDuration=250`, `messagePositionX=0.2`, `messagePositionY=0.4`, `combatUiAnchorX=0.025`, `combatUiAnchorY=0.8`, `customAITriggerHappiness=1.1`, `customAIFriendlyFireCaution=1.1`, `strictBigSmallShipMode=false`, `targetShields_threshold=0.1`, and `avoidShields_threshold=0.2`.
- Naming/logic review wave progress: `PrioSmall` is now the canonical name for former `PrioPD` behavior with `PrioPD` / `PrioritisePD` / `PrioritizePD` aliases preserved; `PrioBig` is implemented as priority-only with no extra targeting restrictions; `TargetBig` / `TargetSmall` are canonical for former `BigShip` / `SmallShip` target-restriction behavior with old aliases preserved.
- `TargetPhase -> PrioPhase` was reviewed and rejected for now. `TargetPhaseTag` is not a pure priority tag: it strongly prioritizes phase ships with a low priority modifier and its base-AI validity path accepts only phase ships. However, it does not override `isValidTarget(...)`, so the custom target-selection path can still consider other valid targets. This mixed behavior makes `TargetPhase` more accurate than `PrioPhase` unless the implementation changes.
- Phase-tag behavior is original-author code and should be treated cautiously. `AvoidPhaseTag.isBaseAiValid(...)` appears suspicious because it accepts non-ships, accepts phase ships, and rejects normal ships in the base-AI validity path. That may be inverted for an "avoid phased" tag, because accepting a phase-ship base target can prevent custom retargeting before `shouldFire(...)` later refuses to fire. This may still be intentional original-author behavior or an interaction with the base/custom AI handoff that is not obvious from static inspection.
- Threshold-class naming direction for future touched code: prefer comparator-explicit names such as `...AboveFluxTag` / `...BelowFluxTag`; avoid vague new `At...` names.
- If/when threshold behavior families are consolidated safely, prefer one behavior class per family plus a shared FluxCondition model over separate SoftFlux/TotalFlux classes.
- `BurstPDSoftFluxTag` is semantically outdated relative to canonical `PD(SF>N%)` and is an internal rename candidate when that area is touched again.
- Legacy settings-backed wrappers `TargetShieldAtTotalFluxTag` / `AvoidShieldAtTotalFluxTag` should be treated as legacy wrappers, not preferred naming models.
- Code-side tag review/audit status: incompatibility/family mapping audit found no clear rename-wave drift requiring code changes; narrow non-phase tooltip cleanup is complete for `ShipTarget`, `PrioFighter`, `PrioMissile`, `PrioShip`, `PrioWounded`, and `PrioHealthy`. README tag-table synchronization/examples and the broader rename-wave text pass are no longer the active implementation focus. Active follow-up work is campaign preset GUI polish, confirm/cancel protection for broad-copy or destructive campaign actions, and a narrow weapon-group heading recolor test. Put phase-tag behavior changes at the bottom of the work priority: revisit `TargetPhaseTag` / `AvoidPhaseTag` only after higher-priority audits and with cautious runtime testing, because the behavior comes from the original author and may rely on non-obvious base-AI/custom-AI interactions.
- Ship-mode persistence correctness audit fixed two code-side bugs: local ship-mode custom data now writes to `Values.CUSTOM_SHIP_DATA_SHIP_MODES_KEY` / `AGC_ShipTags` instead of the weapon-tag key, and persistent mode add/remove now reads using the passed `loadoutIndex` instead of `AGCGUI.storageIndex`. A type-compatible one-time migration moves legacy wrong-key `InShipShipModeStorage` data from `AGC_Tags` to `AGC_ShipTags`; normal weapon-tag storage is not treated as ship-mode storage.
- Canonical user-facing renames completed: `Hold(...) -> HoldFire(...)` with `Hold(...)` aliases preserved, and `ForceAF -> ForceAutoFire` with `ForceAF` tag/mode aliases preserved. Generated `Settings.editme` allowed-values comments include canonical `ForceAutoFire` and legacy `ForceAF` compatibility.
- `NoPD(H<...)` and `NoPD(Waste>...)` fighter durability now share a cheap weapon-relative estimate: `hull + armorRating * armorPenalty + remainingShieldBuffer * shieldPenalty`. Bad damage matchups may make armor or shields count as tougher; strong matchups do not reduce durability below baseline. Missiles remain simple hitpoints. The preferred visible minor-PD suppression tag is `NoPD(Waste>...)`; `NoPD(H<...)` is retained for compatibility and simpler threshold use.
- `PrioBig` is implemented as the priority-only companion to `PrioSmall`: larger ships get higher priority, but the tag should not restrict non-ship target validity.
- Campaign tag scrolling should use the single forwarded `InputEventAPI` wheel path with explicit event consumption. Do not reintroduce direct LWJGL mouse polling unless coordinate logs prove it is needed.
- Campaign selected tags are pinned at the top of each tag column and removed from the normal scroll slice while pinned. Selected-state colors should follow the shared colored-button rules documented below rather than the old pale-yellow treatment.
- Campaign/suggested tag labels currently render as plain single-label text. Avoid `addParaWithMarkup()`, highlighted `addPara(...)` overloads, or segmented text for tag cells because earlier attempts leaked literal markup, crashed on `%`, or clipped characters in-game.
- Current GUI heading policy: use the custom container-heading path for current and future campaign/suggested-tags container headings instead of Starsector `addSectionHeading(...)` bars. Default heading background is `Color(40, 40, 40, 225)`. Stale weapon-group headings use `Color(80, 80, 80, 225)`.
- Empty weapon groups keep their seven-column layout footprint; recent behavior shows the group heading but leaves the rest blank.
- Suggested-tags GUI mirrors campaign tag behavior: forwarded wheel scrolling, pinned selected tags, selected tags removed from normal rows, and colored tag-label segments.

- Colored-button/tag semantics should follow two patterns unless a specific surface has an explicitly accepted exception.
  - Toggleable weapon tags and ship modes share one `CampaignGuiStyle` color family. Current raw values are: untoggled idle fill `Color(0, 0, 0, 225)`; untoggled hover/glow `Color(184, 90, 0, 225)`; toggled idle fill/border `Color(184, 90, 0, 225)`; toggled hover/glow `Color(184, 90, 0, 225)`. This is an orange hue tuned to roughly the same perceived brightness as the previously acceptable `Color(123, 106, 21, 225)` value.
  - Untoggleable buttons use their explicit family colors. Neutral uncolored buttons keep a gray idle fill and use raw white hover/glow because Starsector dims that toward a dark gray in practice.
- Brightness language for future UI work should be interpreted consistently:
  - "dark" means roughly the brightness of `Color(0, 69, 92, 225)`, `Color(95, 80, 14, 225)`, or `Color(62, 34, 82, 225)`
  - "moderately bright" means roughly the brightness of `Color(0, 109, 145, 225)`, `Color(145, 125, 25, 225)`, or `Color(95, 55, 125, 225)`
  - "bright" means roughly the brightness of `Color(205, 180, 70, 225)` or `Color(150, 105, 190, 225)`
- Current accepted action-family color semantics: save-family buttons use dark-purple fills, load-family buttons use dark-yellow fills, and confirm/cancel remain green/red. Save/Load visible text should stay neutral rather than inheriting the family color.
- Active weapon tags and ship modes are toggleable controls and must use the same shared `SHARED_TAG_MODE_*` constants and `applyToggleableCheckboxVisualState(...)` path. Ship modes should match weapon tags in background fill, hover/highlight behavior, and overall selected-state treatment unless a specific future task intentionally changes both together.
## Sensitive areas

- `assignShipModes()` and `shouldNotOverrideShipAI()` in `ShipModes.kt`.
- `CustomShipAI.advance()` delegation to `baseAI`.
- Any use of `ship.resetDefaultAI()`: recent history indicates this caused unintended AI resets.
- Settings additions may need matching updates in `Settings.kt`, `Settings.editme` generation in `build.gradle.kts`, and `data/config/LunaSettings.csv`.
- Campaign GUI row backgrounds are primarily controlled by row/panel fill plugins rather than only the colors passed to `addAreaCheckbox(...)`. Transparent row fills can read as black from the parent container.
- Weapon tag renames are compatibility-sensitive because persisted saves/settings/loadouts may contain old tag strings. Prefer legacy aliases that normalize to the new canonical tag over breaking existing stored data.
- Phase-tag behavior is a sensitive original-author area. Do not "fix" `TargetPhaseTag` or `AvoidPhaseTag` solely from static reasoning. If revisited later, first document the observed in-game behavior, then make the smallest change that aligns tooltip, base-AI validity, custom target selection, priority, and firing decision behavior.

## Known pitfalls and lessons learned

- Starsector custom UI components must be created by the same panel they are added to. Creating a `TooltipMakerAPI` from a parent and adding it to a child panel caused crashes.
- `addPara(...)` highlighted overload treats `%` as a formatter marker. Labels like `Hold(TF>90%)` can crash if passed through the wrong overload.
- Starsector did not render arrow glyphs consistently in this UI; scroll indicators use ASCII `^ ^ ^` and `v v v`.
- Direct LWJGL mouse coordinates previously routed wheel input to the wrong tag columns after rebuilds. Preserve forwarded input routing unless there is new evidence.
- Rebuilds reset scroll state unless `captureTagScrollOffsets()` is preserved and passed back as `initialTagScrollOffsets`.
- MagicLib combat/refit buttons are narrow and do not clip long text. Widening combat/refit tag buttons reduces visible tag capacity on 1080p and is a poor default tradeoff.
- Temporary complete-list scroll-test tags were removed from `Settings.getCurrentWeaponTagList()` after GUI scroll validation.
- `SynchronizedFireTag.kt` has `DEBUG_SYNC = false` by default. Turn it on only for focused runtime sync diagnostics.
- Starsector `TooltipMakerAPI.addAreaCheckbox(text, data, base, bg, bright, ...)` color slots are not a four-state toggle API. The decompiled runtime behavior observed for the campaign item controls is: `base` drives hover/glow, `bg` drives checked fill/border, and `bright` drives the built-in label text color. AGC passes blank built-in labels and renders its own text, so `bright` mostly matters as a fallback, not as the visible tag/mode label color.
- There is no constructor color slot for "unchecked hover" versus "checked hover". The current working campaign tag/mode path gets distinct state behavior by applying cached `setGlowOverride(...)` and `setBorderOverride(...)` after checkbox creation, choosing the override from `button.isChecked`.
- Do not reintroduce per-frame generic reflection for button color overrides. A previous version invoked methods by name repeatedly and caused large click/UI delays even on unrelated buttons. Cache the reflected `Method` objects by runtime button class and only reapply the toggle override when the checked state changes.
- Do not call `setBgOverride(...)` for campaign weapon tags or ship modes unless a focused test proves it is needed. The constructor `bg` slot already controls checked fill/border; adding a background override was redundant in the working path and increased risk of stale or expensive rendering.
- The area-checkbox render path first draws a black base, draws checked fill only when checked, draws the border, then draws glow while `glowAmount > 0`. A bright hover/glow can look like a bright border around an otherwise black unchecked tag. Setting `setBorderThickness(0f)` did not remove the colored-border appearance in-game, which supports the theory that the visible ring is the glow region rather than the nominal border. The current tag/mode path experiments with `setBorderThickness(-1f)` to expand the black/glow rectangles and reduce the ring/black-center effect; unavailable tags restore `1f` so disabled borders remain visible.
- Keep `fillColor = null` on campaign weapon-tag and ship-mode item panels unless intentionally testing a new layout. Extra row/container fill can mask hover, double-brighten selected state, or make weapon tags and ship modes drift even when they share the same checkbox colors.
- Raw Starsector UI colors can appear much darker in game than their RGB values suggest, especially checkbox glow/hover colors. Current visual evidence suggests roughly 75% effective dimming on these controls, so using raw white for neutral hover can be the correct way to get a readable dark-gray in-game result.
- For normal action buttons, remember that `base` is the hover/glow slot. Passing a neutral hover color as `bg`/`bright` will not affect normal unchecked hover; this caused `Switch to simple mode`-style neutral buttons to ignore the intended white hover.
- Weapon tags and ship modes should be treated as the same toggleable control family. Change `CampaignGuiStyle.SHARED_TAG_MODE_*` values rather than editing one container path; otherwise the two surfaces will drift again.

## Open questions

- Confirm in-game that the latest campaign/suggested plain tag labels still render without clipping.
- Confirm suggested-tags reset flow, filter actions, page controls, and Esc/back behavior in-game.
- Campaign preset GUI follow-up remains the active UI task:
  - move Save Preset and Load Preset to the top of each weapon-group container directly beneath the Group N heading
  - remove or reduce the tiny gap between Save Preset and Load Preset
  - investigate and remove the stray extra white text currently appearing beneath the yellow save/load status line
  - avoid leaving extra status-text spacing between the preset controls and the tag list if possible
- In the options container, broad-copy or destructive actions should not execute on a stray click. Add confirm/cancel protection for Copy to other ships of same variant, Copy to other ships of same variant (same hull type), Copy previous loadout, Copy to other ships of same variant for all loadouts, Copy to other ships of same variant for all loadouts (same hull type), Copy previous loadout for entire fleet, and Reload settings.
- Test whether weapon-group heading section bars such as `Group 1` can be recolored orange without affecting unrelated section headings.
- Original-upstream default restoration remains a lowest-priority backlog task. When that task is reached, restore Luna and Settings defaults to original upstream defaults, but if original upstream LunaSettings and original upstream Settings/runtime defaults differ, prefer original upstream LunaSettings. Do not use that future task to restore upstream tag-list contents, old tag names, fork metadata, generated version metadata, or other non-default surfaces unless explicitly requested.

## Current GUI heading and colored-control policy

This section supersedes older stale GUI notes about pale-yellow selected tags and built-in Starsector section headings.

- Current heading policy for the touched campaign/suggested-tags surfaces is to use the custom container-heading path rather than Starsector `addSectionHeading(...)` for normal container headings.
- Default custom heading background is `Color(40, 40, 40, 225)`.
- Stale weapon-group heading background is `Color(80, 80, 80, 225)`.

### Colored-button semantics

When working on colored buttons or tags in this GUI family, use these two patterns unless a specific surface has an explicitly accepted exception.

#### Approach A: toggleable buttons
- Untoggled idle fill: `Color(0, 0, 0, 225)`
- Untoggled hover/glow: `Color(184, 90, 0, 225)`
- Toggled idle fill/border: `Color(184, 90, 0, 225)`
- Toggled hover/glow: `Color(184, 90, 0, 225)`

#### Approach B: untoggleable buttons
- Untoggled idle fill: use the button family's accepted idle color.
- Untoggled hover/glow: use the button family's accepted hover color. For neutral buttons, AGC currently uses raw white hover because Starsector dims it toward gray.

### Brightness language

Interpret brightness requests consistently:
- "dark" means roughly the brightness of `Color(0, 69, 92, 225)`, `Color(95, 80, 14, 225)`, or `Color(62, 34, 82, 225)`
- "moderately bright" means roughly the brightness of `Color(0, 109, 145, 225)`, `Color(145, 125, 25, 225)`, or `Color(95, 55, 125, 225)`
- "bright" means roughly the brightness of `Color(205, 180, 70, 225)` or `Color(150, 105, 190, 225)`

### Current accepted GUI color-family semantics

- Save-family untoggleable buttons use a purple family.
- Load-family untoggleable buttons use a yellow family.
- Confirm/Cancel remain green/red.
- Save/Load visible text should stay neutral rather than inheriting the family color.
- Neutral uncolored buttons such as navigation/back/reset-style buttons should use a gray hover family rather than pale blue.

### Weapon tags and ship modes

- Active weapon tags and ship modes are toggleable controls and should follow Approach A exactly through the shared `CampaignGuiStyle.SHARED_TAG_MODE_*` constants.
- Weapon tags and ship modes should match in background fill, hover/highlight behavior, and overall selected-state treatment unless a future task intentionally changes both together.
- Weapon tags are not required to match ship modes in text centering/layout, but their color/state semantics should match.

## Current GUI button-state policy

This section supersedes older stale GUI notes about pale-yellow selected tags and built-in heading styling when working on the current campaign and suggested-tags GUI surfaces.

### Current heading policy
- Use the custom container-heading path for current and future campaign/suggested-tags container headings instead of Starsector `addSectionHeading(...)` for normal container headings.
- Default custom heading background is `Color(40, 40, 40, 225)`.
- Stale weapon-group heading background is `Color(80, 80, 80, 225)`.

### Button behavior models

When working on colored buttons or tags in this GUI family, use these two patterns unless a specific surface has an explicitly accepted exception.

#### Approach A: toggleable buttons
- Untoggled idle fill: `Color(0, 0, 0, 225)`
- Untoggled hover/glow: `Color(184, 90, 0, 225)`
- Toggled idle fill/border: `Color(184, 90, 0, 225)`
- Toggled hover/glow: `Color(184, 90, 0, 225)`

Reference brightness anchors for this family:
- current orange anchor: `Color(184, 90, 0, 225)`
- note: `Color(123, 106, 21, 225)` had acceptable apparent brightness but read as yellow, so the active family was shifted to a more orange hue with similar perceived brightness. Selected hover intentionally matches selected idle for now because the brighter highlight read too bright in-game.

#### Approach B: untoggleable buttons
- Untoggled idle fill: `Color(145, 125, 25, 225)`
- Untoggled hover fill: `Color(205, 180, 70, 225)`

Reference brightness anchors for this family:
- dark anchor: `Color(73, 63, 13, 225)`
- base anchor: `Color(145, 125, 25, 225)`
- highlight anchor: `Color(205, 180, 70, 225)`

### Brightness language
When the user asks for different colors in future GUI work, extrapolate brightness from these baseline values rather than treating color names alone as sufficient.
- "dark" means approximately the brightness of `Color(73, 63, 13, 225)`, `Color(95, 80, 14, 225)`, or `Color(62, 34, 82, 225)`.
- "base" or "moderately bright" means approximately the brightness of `Color(145, 125, 25, 225)`, `Color(95, 55, 125, 225)`, or `Color(0, 109, 145, 225)`.
- "bright" or "highlight" means approximately the brightness of `Color(205, 180, 70, 225)` or `Color(150, 105, 190, 225)`.

### Current accepted semantics for weapon tags and ship modes
- Active weapon tags and ship modes are toggleable controls and should follow Approach A.
- Weapon tags and ship modes should match in background fill, hover/highlight behavior, and overall selected-state treatment unless a future task intentionally changes both together. Use the shared `CampaignGuiStyle.SHARED_TAG_MODE_*` constants for both.
- Weapon tags are not required to match ship modes in text centering/layout, but their state-color semantics should match.

### Current accepted semantics for other button families
- Save-family untoggleable buttons use the accepted save-family hue with brightness interpreted from the baseline rules above.
- Load-family untoggleable buttons use the accepted load-family hue with brightness interpreted from the baseline rules above.
- Confirm/Cancel remain green/red.
- Save/Load visible text should stay neutral rather than inheriting the family color.
- Neutral uncolored buttons such as navigation/back/reset-style buttons should use a neutral gray hover family rather than pale blue.
