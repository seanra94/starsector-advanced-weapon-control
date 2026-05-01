# PLANS.md

## Active task

Documentation/text consistency wave.

Current status: baseline list curation, TF/SF helper groundwork, ammo-threshold canonicalization, preferred `NoPD(Waste>...)`, retained `NoPD(H<...)` support, burst-beam packet-estimation remediation, HF support, LunaSettings exposure of `SFTUpperFluxLimit`, weapon-relative `NoPD(H<...>)` durability, first static tooltip/text consistency pass, `HoldFire(...)` canonicalization, `ForceAutoFire` canonicalization, generated allowed-values repair, `PrioSmall` canonicalization, `PrioBig` implementation, `TargetBig` / `TargetSmall` canonicalization, `TargetPhase` semantic review, code-side incompatibility/family mapping audit, narrow non-phase tooltip cleanup, ship-mode local storage key/index repair, current-fork default backup documentation, Luna-default alignment, and README tag catalogue synchronization/examples are complete.

The active implementation focus is now remaining README/text consistency. Phase-tag behavior questions and original-upstream default restoration are deliberately deferred to the bottom of the priority list.

## Goal

Keep the tag system compatibility-safe and user-understandable after canonicalization. Synchronize user-facing README tag documentation and examples with the current canonical tag set while preserving important legacy-alias information.

## Current understanding

Canonical names and alias rules are defined in `WeaponAITags.kt`; generated settings text and tag lists are authored in `build.gradle.kts`.

Default-source rule: for Luna-exposed settings, the fork's current `data/config/LunaSettings.csv` default is authoritative. `Settings.kt` runtime defaults and generated `Settings.editme` fallback defaults should match Luna defaults. For settings not exposed in Luna, preserve the existing runtime/generated fallback behavior unless there is a separate explicit task.

Current canonical names:
- Ammo-gated opportunist behavior: `Opportunist(A<...%)`
- Ammo-gated PD reservation behavior: `PD(A<...%)`
- Preferred minor-PD suppression: `NoPD(Waste>...)`
- Simpler retained health-threshold minor-PD suppression: `NoPD(H<...)`
- Hold-fire flux gates: `HoldFire(TF>...%)`, `HoldFire(SF>...%)`, `HoldFire(HF>...%)`
- Force autofire tag/mode: `ForceAutoFire`
- Small-target priority: `PrioSmall`
- Big-target priority: `PrioBig`
- Target-size restrictions: `TargetBig`, `TargetSmall`
- Phase-ship targeting/pressure tag: `TargetPhase`

Compatibility aliases remain important:
- `ConserveAmmo` / `ConserveAmmo(A<...%)`
- `ConservePDAmmo` / `ConservePDAmmo(A<...%)`
- `CnsrvPDAmmo` / `CnsrvPDAmmo(A<...%)`
- `IgnoreMinorPD` / `IgnoreMinorPD(H<...)`
- `Hold(...)` legacy forms and older hold flux spellings
- `ForceAF`
- `PrioPD` / `PrioritisePD` / `PrioritizePD`
- `BigShip` / `BigShips`
- `SmallShip` / `SmallShips`

`NoPD(Waste>...)` uses bounded waste: `max(0, estimatedAttackPacketDamage - estimatedTargetDamageRequired) / estimatedAttackPacketDamage`. Continuous beams estimate attack packet damage as `beamDps * 0.5s`. Burst beams estimate one committed burst packet: `beamDps * (burstDuration + beamChargeupTime / 3 + beamChargedownTime / 3)`.

`NoPD(H<...>)` and `NoPD(Waste>...)` fighter durability use the shared cheap weapon-relative estimate `hull + armorRating * armorPenalty + remainingShieldBuffer * shieldPenalty`. Bad damage matchups may increase apparent armor or shield durability; favorable matchups do not reduce durability below baseline. Missiles remain simple hitpoints.

`SFTUpperFluxLimit` / `Settings.softFluxTotalFluxCap()` is Luna-exposed with default `0.9`; Settings.editme fallback remains supported. It is the total-flux safety cap used by soft-flux conditional tags.

Luna-default alignment is complete. `Settings.kt` and generated `Settings.editme` now follow current fork Luna defaults for Luna-exposed settings. The resolved mismatches were:
- `listVariant`: `classic`
- `messageDisplayDuration`: `250`
- `messagePositionX`: `0.2`
- `messagePositionY`: `0.4`
- `combatUiAnchorX`: `0.025`
- `combatUiAnchorY`: `0.8`
- `customAITriggerHappiness`: `1.1`
- `customAIFriendlyFireCaution`: `1.1`
- `strictBigSmallShipMode`: `false`
- `targetShields_threshold`: `0.1`
- `avoidShields_threshold`: `0.2`

Original-upstream default restoration is deferred. When that task is reached, restore Luna and Settings defaults to the original upstream repo's defaults, but if original upstream LunaSettings and original upstream Settings/runtime defaults differ, prefer the original upstream LunaSettings default. Do not restore upstream tag-list contents, old tag names, fork metadata, or generated version metadata as part of that task unless explicitly requested.

Original-upstream default backup notes:
- Scope is scalar/config defaults only.
- Luna `agc_opportunist_HEThreshold`: original upstream Luna default `0.2`; current fork value `0.15`.
- Luna `agc_spamSystemPreventsDeactivation`: original upstream Luna default `true`; current fork value `false`.
- Generated `Settings.editme` fallback `conservePDAmmo_ammo`: original upstream generated fallback `0.8`, but upstream runtime default is `0.9`; current fork runtime/generated value is `0.9`. Treat this as source-of-truth ambiguous rather than an automatic `0.8` restore.
- Luna `agc_ignoreFighterShields`: original upstream LunaSettings has duplicate conflicting defaults (`false` and `true`); upstream runtime/generated fallback and current fork use `true`. Do not reintroduce the duplicate unless explicitly restoring upstream Luna quirks.
- Fork-only defaults with no original upstream counterpart: `noPDWasteCleanupDamageCap = 100`, `SFTUpperFluxLimit = 0.9`.

`TargetPhase -> PrioPhase` was reviewed and rejected for now. `TargetPhaseTag` is not pure priority behavior: it strongly prioritizes phase ships and accepts only phase ships through the base-AI validity path, but it does not override `isValidTarget(...)`, so custom target selection may still consider other valid targets. `TargetPhase` remains the more accurate canonical name unless the behavior changes to priority-only.

`AvoidPhaseTag` / `AvoidPhased` behavior is deferred. Static inspection suggests `AvoidPhaseTag.isBaseAiValid(...)` may be inverted because it appears to accept phase ships and reject normal ships in the base-AI validity path, while `shouldFire(...)` later refuses shots when the phase ship may phase before impact. This is original-author code and may rely on non-obvious base-AI/custom-AI handoff behavior, so do not change it from static reasoning alone.

## Near-term queue

1. Documentation/text consistency wave:
   - remaining README/text consistency sweep, for example `Avd -> Avoid`
2. Larger system work:
   - rotate-toward-closest-valid-target behavior as ship mode rather than global aiming behavior
   - deep dive on priority-system consistency/transparency
   - broader LunaLib/settings migration strategy
3. Lowest-priority deferred phase-tag review:
   - revisit `TargetPhaseTag` and `AvoidPhaseTag` only after higher-priority audits and with cautious runtime testing
   - preserve original-author behavior unless in-game evidence shows the current base-AI/custom-AI interaction is wrong
   - specifically investigate whether `AvoidPhaseTag.isBaseAiValid(...)` intentionally accepts phase ships and rejects normal ships, or whether that is inverted
   - only then decide whether tooltip-only changes, base-AI validity changes, or no changes are appropriate
4. Lowest-priority original-upstream default restoration:
   - restore Luna and Settings defaults to the original upstream repo's defaults
   - if original upstream LunaSettings and original upstream Settings/runtime defaults differ, prefer the original upstream LunaSettings default
   - do not restore upstream tag-list contents, old tag names, fork metadata, or generated version metadata as part of that task unless explicitly requested

## Acceptance criteria

- [x] `completeTagList` is canonical and parses cleanly against current tag support.
- [x] Obvious stale/legacy names are normalized to canonical equivalents.
- [x] Supported tag families have representation in baseline unless intentionally excluded.
- [x] Allowed-values comment block in generated settings source reflects current support.
- [x] TF/SF conditional parsing/canonicalization/tooltip-condition/evaluation duplication is reduced via narrow helper abstractions without changing behavior.
- [x] Parameterized ammo-threshold tags are implemented with plain-tag compatibility preserved.
- [x] `NoPD(Waste>...)` is implemented with `NoPD(H<...)` and `IgnoreMinorPD` compatibility retained.
- [x] `NoPD(Waste>...)` estimates burst beams as one committed burst packet rather than as a short continuous-beam slice.
- [x] `NoPD(H<...>)` uses weapon-relative bad-matchup-only effective durability.
- [x] LunaSettings exposes the soft-flux total-flux cap (`SFTUpperFluxLimit`) with default `0.9`.
- [x] First static tooltip/text consistency pass fixed obvious drift.
- [x] `HoldFire(...)` canonicalization preserves `Hold(...)` aliases.
- [x] `ForceAutoFire` canonicalization preserves `ForceAF` tag/mode aliases.
- [x] Generated allowed-values comments include visible canonical tags after the HoldFire/ForceAutoFire rename packet.
- [x] `PrioBig` is implemented as priority-only with no added targeting restrictions.
- [x] `PrioPD -> PrioSmall` is reviewed/canonicalized with aliases preserved.
- [x] `BigShip/SmallShip -> TargetBig/TargetSmall` is reviewed/canonicalized with aliases preserved.
- [x] `TargetPhase -> PrioPhase` was reviewed and rejected for now; `TargetPhase` remains canonical because behavior is not priority-only.
- [x] Incompatibility definitions were audited against current canonical families and legacy aliases; no clear rename-wave drift required code changes.
- [x] Non-phase priority/targeting tooltip cleanup is complete for the narrow code-side pass.
- [x] Ship-mode local storage key/index audit is complete: local ship-mode custom data uses `AGC_ShipTags`, type-compatible wrong-key local data migrates once from `AGC_Tags`, and persistent mode add/remove uses the passed loadout index.
- [x] Current fork default values that differ from original upstream default surfaces are backed up for the future upstream-default restoration task.
- [x] Runtime and generated fallback defaults are aligned to current fork LunaSettings defaults for Luna-exposed settings.
- [x] README tag catalogue is synchronized with current canonical names and important aliases.
- [x] README tag catalogue includes realistic example use cases.
- [ ] Remaining text consistency sweep is complete.
- [ ] `compileKotlin` passes before push.
- [ ] Deferred phase-tag review is resolved or intentionally closed after cautious runtime testing.
- [ ] Original-upstream default restoration is resolved or intentionally deferred after Luna-first source-of-truth comparison.

## Constraints

- Minimize unrelated changes.
- Preserve existing style and structure unless cleanup is the task.
- Prefer targeted verification first.
- Do not revert prior-agent/user work in the dirty tree.
- Do not overwrite the original AGC mod folder.
- Treat `build.gradle.kts` as the source for generated `mod_info.json`, version files, and `Settings.editme`.
- For Luna-exposed settings, treat `data/config/LunaSettings.csv` defaults as the source of truth.
- Preserve persisted tag/loadout compatibility when renaming or canonicalizing tags.
- Do not change `TargetPhaseTag` or `AvoidPhaseTag` behavior without runtime evidence.

## Verification needed

Minimum code check:

    $env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
    .\gradlew.bat compileKotlin

For deploy/test passes, package with:

    $env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
    .\gradlew.bat jar create-metadata-files write-settings-file

Then copy to `C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork` using the established exclusions and compare repo/deployed jar hashes.

## Risks and open questions

- Settings comment blocks can drift from actual support if list/regex updates are not mirrored in `build.gradle.kts`.
- LunaSettings is now the source of truth for defaults. Future Luna-exposed settings should keep `data/config/LunaSettings.csv`, `Settings.kt`, generated `Settings.editme`, and deployed config behavior aligned.
- Original-upstream default restoration is a bottom-backlog task. When that task is reached, compare original upstream LunaSettings and original upstream Settings/runtime defaults; if they differ, prefer original upstream LunaSettings.
- Large rename waves should be staged and compatibility-preserving because persisted saves/settings/loadouts may contain old tag strings.
- `NoPD(H<...)` must continue to describe `H` as effective durability, not literal hull.
- `NoPD(Waste>...)` must not treat burst beams as short continuous cleanup beams; Phase Lance/Tachyon Lance-style weapons should estimate as high committed burst packets.
- LunaSettings and Settings.editme defaults for `SFTUpperFluxLimit` must remain aligned at `0.9`.
- `PrioSmall` is canonical for former `PrioPD` behavior; legacy `PrioPD` / `PrioritisePD` / `PrioritizePD` aliases must remain accepted.
- `PrioBig` must remain priority-only and must not restrict non-ship target validity.
- `TargetBig` / `TargetSmall` are canonical for former `BigShip` / `SmallShip` target-restriction tags; legacy aliases must remain accepted.
- `TargetPhase` remains canonical because its behavior is mixed, not priority-only. Static inspection shows `TargetPhaseTag` strongly prioritizes phase ships and accepts only phase ships through `isBaseAiValid(...)`, but does not override `isValidTarget(...)`, so custom target selection may still consider other valid targets. This makes `TargetPhase` more accurate than `PrioPhase` unless behavior changes.
- Deferred phase-tag caution: `AvoidPhaseTag.isBaseAiValid(...)` appears suspicious because it accepts phase ships and rejects normal ships in the base-AI validity path, while `shouldFire(...)` then refuses shots when the phase ship may phase before impact. This could prevent custom retargeting away from a bad phase target, but it is original-author code and may rely on non-obvious AI handoff behavior. Treat this as lowest-priority until runtime evidence justifies changing it.


## Future feature: manual weapon-composition tag loadout buttons

User goal: eventually add two buttons to each campaign weapon-group panel, placed below the weapon container and above the active tag buttons:
- Save tag loadout for this combination of weapons.
- Load the saved tag loadout for this combination of weapons.

Current persistence model:
- Existing tag persistence already has three user-facing storage modes: `Index`, `WeaponComposition`, and `WeaponCompositionGlobal`.
- `Index` is the most customizable mode: tags are stored per ship and weapon group index, but refits or group reordering can make saved tags no longer match the intended weapons.
- `WeaponComposition` trades some customization for convenience: tags are stored per ship by the weapon composition of the group, so moving a group to a different index can preserve tags, but changing the weapon mix changes the key.
- `WeaponCompositionGlobal` is the most convenient and least flexible mode: tags are stored by weapon composition without being tied to a specific ship, so identical weapon combinations can share tags across ships.
- Existing persistent data is routed through `StorageBase` / `Settings.tagStorage` / `Settings.tagStorageByWeaponComposition` and loadout slots are keyed by the active `AGCGUI.storageIndex`.
- Campaign tag buttons currently use `loadPersistentTags(...)` and `persistTags(...)`; future preset buttons should prefer those existing persistence/canonicalization paths rather than inventing an unrelated storage system.

Design direction:
- Treat the new buttons as an explicit manual composition-preset layer, not as an automatic change to `tagStorageMode`.
- The save button should capture the current tags for the current weapon group and store them under the weapon-composition key for the current active loadout slot.
- The load button should read the saved tags for the current weapon composition and apply them to the current group using the normal persistence path for the active loadout slot.
- This should let a user stay in `Index` mode for maximum customization while still manually saving/reusing a tag preset for repeated weapon combinations.
- Loaded tags should be canonicalized and sanitized through the same constraints as normal tag buttons: remove unknown tags, disabled tags, and incompatible combinations rather than reviving stale or invalid saved state.
- Reuse the existing save/persistent-data file path where possible. Do not introduce a new external file unless local inspection proves the existing persistent storage cannot safely represent manual composition presets.

Likely implementation touchpoints:
- `ShipView.buildWeaponGroupContainer(...)`: insert a small button row between the weapon list container and the tag container.
- `TagButton` / campaign tag-button persistence flow: reuse or factor existing `loadPersistentTags(...)`, `persistTags(...)`, sanitization, and `campaignTagSelectionVersion` behavior.
- `utils` persistence helpers: inspect the exact weapon-composition key generation and storage-mode dispatch before implementing.
- `AGCGUI.storageIndex`: ensure save/load preset operations are scoped to the active loadout slot unless deliberately designing cross-loadout behavior.

Acceptance criteria for eventual implementation:
- Buttons appear once per non-empty weapon group below the weapon list and above active tag buttons.
- Save stores the currently selected tags for that weapon composition without changing the current `tagStorageMode`.
- Load applies the saved composition tags to the current group and refreshes the campaign UI state.
- The feature works when normal storage mode is `Index`, which is the main use case.
- Behavior is defined and tested for `WeaponComposition` and `WeaponCompositionGlobal` modes rather than accidentally double-applying or fighting the current mode.
- Save/load respects the active `AGCGUI.storageIndex` loadout slot, or explicitly documents any cross-loadout behavior.
- Loaded tags are canonical, valid for the current weapon group, and compatible with each other.
- Existing normal tag persistence remains backward-compatible.

Risks and open questions:
- Need local inspection of `loadPersistentTags(...)`, `persistTags(...)`, `loadAllTags(...)`, and the weapon-composition key helper before coding.
- Need to decide whether the manual preset store should reuse `tagStorageByWeaponComposition` directly or use a separate persistent-data key to avoid interfering with automatic `WeaponComposition` mode.
- Need to decide what happens when no preset exists for a weapon composition: disabled Load button, tooltip, harmless no-op, or status message.
- Need to decide whether saving an empty tag list should clear the saved preset or create an explicit empty preset.
- Need in-game UI validation at 1080p because adding two buttons reduces vertical space available for tag scrolling.
