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
- Scoped Luna/default audit resolved duplicate `agc_ignoreFighterShields`, aligned Luna `opportunist_HEThreshold` to `0.15`, corrected Luna `customAIRecursionLevel` bounds to `0..2`, and updated `strictBigSmallShipMode` description to canonical `TargetBig` / `TargetSmall` names. The user later decided that original upstream defaults should be restored going forward, but current fork defaults must be backed up first.
- Original-default restore backup scope: compare scalar/config defaults only, not tag-list contents, fork metadata, generated version files, or canonical tag-list renames unless explicitly requested. Current canonical tag lists are intentional fork state.
- Current fork values that differ from original upstream default surfaces and should be preserved as backup before restoration:
  - Luna `agc_opportunist_HEThreshold`: original upstream Luna default `0.2`; current fork Luna/runtime/generated value `0.15`.
  - Luna `agc_spamSystemPreventsDeactivation`: original upstream Luna default `true`; current fork Luna/runtime value `false`.
  - Generated `Settings.editme` fallback `conservePDAmmo_ammo`: original upstream generated fallback `0.8`, but upstream runtime default is `0.9`; current fork runtime/generated fallback/Luna-facing behavior is `0.9`. Do not restore `0.8` blindly without deciding whether original generated fallback or original runtime behavior is authoritative.
  - Luna duplicate `agc_ignoreFighterShields`: original upstream LunaSettings contains conflicting duplicate defaults (`false` in the AI tab and `true` in the Advanced tab), while upstream runtime/generated fallback and current fork use `true`. Do not reintroduce the duplicate unless deliberately restoring upstream Luna surface quirks.
- Fork-only defaults with no original upstream counterpart should normally be retained unless explicitly removed: `noPDWasteCleanupDamageCap = 100` and `SFTUpperFluxLimit = 0.9`.
- Existing ambiguous runtime-vs-generated mismatches that are not current-vs-original runtime changes still need cautious handling: `targetShields_threshold` runtime `0.2` vs generated/Luna `0.1`, `avoidShields_threshold` runtime `0.5` vs generated/Luna `0.2`, `strictBigSmallShipMode` runtime `true` vs generated/Luna `false`, `customAITriggerHappiness` runtime `1.2` vs generated fallback `1.0` / Luna `1.1`, and `customAIFriendlyFireCaution` runtime `1.0` vs generated fallback `1.25` / Luna `1.1`. These patterns also exist in the original upstream repo or are not clearly fork-created, so do not normalize them as fork drift without a deliberate source-of-truth decision.
- Naming/logic review wave progress: `PrioSmall` is now the canonical name for former `PrioPD` behavior with `PrioPD` / `PrioritisePD` / `PrioritizePD` aliases preserved; `PrioBig` is implemented as priority-only with no extra targeting restrictions; `TargetBig` / `TargetSmall` are canonical for former `BigShip` / `SmallShip` target-restriction behavior with old aliases preserved.
- `TargetPhase -> PrioPhase` was reviewed and rejected for now. `TargetPhaseTag` is not a pure priority tag: it strongly prioritizes phase ships with a low priority modifier and its base-AI validity path accepts only phase ships. However, it does not override `isValidTarget(...)`, so the custom target-selection path can still consider other valid targets. This mixed behavior makes `TargetPhase` more accurate than `PrioPhase` unless the implementation changes.
- Phase-tag behavior is original-author code and should be treated cautiously. `AvoidPhaseTag.isBaseAiValid(...)` appears suspicious because it accepts non-ships, accepts phase ships, and rejects normal ships in the base-AI validity path. That may be inverted for an "avoid phased" tag, because accepting a phase-ship base target can prevent custom retargeting before `shouldFire(...)` later refuses to fire. This may still be intentional original-author behavior or an interaction with the base/custom AI handoff that is not obvious from static inspection.
- Threshold-class naming direction for future touched code: prefer comparator-explicit names such as `...AboveFluxTag` / `...BelowFluxTag`; avoid vague new `At...` names.
- If/when threshold behavior families are consolidated safely, prefer one behavior class per family plus a shared FluxCondition model over separate SoftFlux/TotalFlux classes.
- `BurstPDSoftFluxTag` is semantically outdated relative to canonical `PD(SF>N%)` and is an internal rename candidate when that area is touched again.
- Legacy settings-backed wrappers `TargetShieldAtTotalFluxTag` / `AvoidShieldAtTotalFluxTag` should be treated as legacy wrappers, not preferred naming models.
- Code-side tag review/audit status: incompatibility/family mapping audit found no clear rename-wave drift requiring code changes; narrow non-phase tooltip cleanup is complete for `ShipTarget`, `PrioFighter`, `PrioMissile`, `PrioShip`, `PrioWounded`, and `PrioHealthy`. Remaining active work is README tag-table synchronization/examples and text consistency. Put phase-tag behavior changes at the bottom of the work priority: revisit `TargetPhaseTag` / `AvoidPhaseTag` only after higher-priority audits and with cautious runtime testing, because the behavior comes from the original author and may rely on non-obvious base-AI/custom-AI interactions.
- Ship-mode persistence correctness audit fixed two code-side bugs: local ship-mode custom data now writes to `Values.CUSTOM_SHIP_DATA_SHIP_MODES_KEY` / `AGC_ShipTags` instead of the weapon-tag key, and persistent mode add/remove now reads using the passed `loadoutIndex` instead of `AGCGUI.storageIndex`. A type-compatible one-time migration moves legacy wrong-key `InShipShipModeStorage` data from `AGC_Tags` to `AGC_ShipTags`; normal weapon-tag storage is not treated as ship-mode storage.
- Canonical user-facing renames completed: `Hold(...) -> HoldFire(...)` with `Hold(...)` aliases preserved, and `ForceAF -> ForceAutoFire` with `ForceAF` tag/mode aliases preserved. Generated `Settings.editme` allowed-values comments include canonical `ForceAutoFire` and legacy `ForceAF` compatibility.
- `NoPD(H<...)` and `NoPD(Waste>...)` fighter durability now share a cheap weapon-relative estimate: `hull + armorRating * armorPenalty + remainingShieldBuffer * shieldPenalty`. Bad damage matchups may make armor or shields count as tougher; strong matchups do not reduce durability below baseline. Missiles remain simple hitpoints. The preferred visible minor-PD suppression tag is `NoPD(Waste>...)`; `NoPD(H<...)` is retained for compatibility and simpler threshold use.
- `PrioBig` is implemented as the priority-only companion to `PrioSmall`: larger ships get higher priority, but the tag should not restrict non-ship target validity.
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

## Open questions

- Confirm in-game that the latest campaign/suggested plain tag labels still render without clipping.
- Confirm suggested-tags reset flow, filter actions, page controls, and Esc/back behavior in-game.
