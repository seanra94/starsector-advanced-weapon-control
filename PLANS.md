# PLANS.md

## Active task

External weapon-composition tag preset GUI follow-up.

Current status: baseline list curation, TF/SF helper groundwork, ammo-threshold canonicalization, preferred `NoPD(Waste>...)`, retained `NoPD(H<...)` support, burst-beam packet-estimation remediation, HF support, LunaSettings exposure of `SFTUpperFluxLimit`, weapon-relative `NoPD(H<...>)` durability, first static tooltip/text consistency pass, `HoldFire(...)` canonicalization, `ForceAutoFire` canonicalization, generated allowed-values repair, `PrioSmall` canonicalization, `PrioBig` implementation, `TargetBig` / `TargetSmall` canonicalization, `TargetPhase` semantic review, code-side incompatibility/family mapping audit, narrow non-phase tooltip cleanup, ship-mode local storage key/index repair, current-fork default backup documentation, Luna-default alignment, and README tag catalogue synchronization/examples are complete.

The active implementation focus is now external weapon-composition tag preset GUI follow-up. Remaining README/text consistency can wait. Phase-tag behavior questions and original-upstream default restoration are deliberately deferred to the bottom of the priority list.

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
## Near-term queue

1. External weapon-composition tag preset GUI follow-up:
   - remove the tiny horizontal gap between `Save Preset` and `Load Preset`
   - investigate and remove the stray extra white text shown under the yellow save/load status text
   - move preset controls to the top of each weapon-group container directly beneath the `Group N` heading
   - remove the need for per-group status text spacing below the preset buttons if possible
   - add confirm/cancel protection to destructive or broad-copy option actions in the options container
   - test whether weapon-group heading section bars can be recolored orange
2. Larger system work:
   - manual external weapon-composition tag preset buttons in the campaign weapon-group GUI
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
## Future feature: external weapon-composition tag preset buttons

User goal: eventually add two buttons to each campaign weapon-group panel, placed below the weapon container and above the active tag buttons:
- Save tag loadout for this weapon combination.
- Load the saved tag loadout for this weapon combination.

Desired user-facing behavior:
- The feature should combine the customizability of `Index` mode with the convenience of `WeaponCompositionGlobal` style reuse.
- Normal tag storage mode should not have to change. A user should be able to stay in `Index` mode while manually saving and loading reusable presets for repeated weapon combinations.
- Presets must work across campaign saves and profiles. They should be backed by a real external file the user can keep, copy, back up, or move between installs/profiles, not only by `Global.getSector().persistentData`.
- The external preset file does not need to be human-friendly if that is inconvenient, but it must be durable and portable outside a single save.
- The buttons should operate on the active loadout slot unless explicitly redesigned later.

Concrete expected matching semantics:
- Matching should be based on the set of weapon types/components in the group, not the exact weapon count.
- Example: a Medusa group with `2x PD Laser + 2x Tactical Laser` saves `TargetShield`. A different group with only `1x PD Laser` should not load that preset because it lacks the Tactical Laser component. A Paragon group with `3x PD Laser + 1x Tactical Laser` should load the same preset because it has the same weapon-type combination, even though counts differ.
- Saving from the Paragon group after adding `NoFighter` should update the shared preset for the `PD Laser + Tactical Laser` combination. The Medusa group remains unchanged until the user explicitly presses Load, then it receives the updated preset.
- Keying should therefore be normalized and count-insensitive by default: sort unique weapon IDs in the group and use that as the preset identity. Do not blindly reuse any existing weapon-composition key until verifying whether it includes counts or slot/index details.

Current persistence model to account for:
- Existing tag persistence has three user-facing storage modes: `Index`, `WeaponComposition`, and `WeaponCompositionGlobal`.
- `Index` is the most customizable mode: tags are stored per ship and weapon group index, but refits or group reordering can make saved tags no longer match the intended weapons.
- `WeaponComposition` trades some customization for convenience: tags are stored per ship by weapon composition, so moving a group to a different index can preserve tags, but changing the weapon mix changes the key.
- `WeaponCompositionGlobal` is the most convenient and least flexible mode: tags are stored by weapon composition without being tied to a specific ship, so identical weapon combinations can share tags across ships.
- Existing in-save persistent data is routed through `StorageBase` / `Settings.tagStorage` / `Settings.tagStorageByWeaponComposition`, and loadout slots are keyed by active `AGCGUI.storageIndex`.
- Campaign tag buttons currently use `loadPersistentTags(...)` and `persistTags(...)`; future preset buttons should reuse the same canonicalization, compatibility, validation, and UI-refresh behavior where possible, but the preset store itself must be external and cross-save.

Current implementation status:
- External preset data layer is implemented.
- Campaign `Save Preset` and `Load Preset` buttons are implemented and functionally work.
- Current button placement is below the weapon list and above the tag container, but the user now prefers the buttons at the very top of each weapon-group container directly beneath the `Group N` heading.
- Current button row has a small visible horizontal gap between `Save Preset` and `Load Preset`; the user wants that gap removed or reduced.
- Current click feedback inserts yellow status text such as `Saved preset for this weapon combination.` or `Loaded preset for this weapon combination.` between the preset buttons and the tag list. There is also stray extra white text beneath that status line, which should not be there and needs investigation.
- The user does not want extra status text spacing below the preset controls if it can be avoided.
- The current external preset feature is good enough functionally to continue iterating on the campaign GUI presentation rather than revisiting the data layer first.

Design direction:
- Treat this as an explicit manual external preset layer, not as another automatic storage mode and not as a hidden switch to `WeaponCompositionGlobal`.
- Save should capture the current selected tags for the current weapon group, canonicalize them, sanitize them, then write them to the external preset file under the normalized count-insensitive weapon-type key and active loadout slot.
- Load should read the external preset for the current normalized weapon-type key and active loadout slot, sanitize it against current tag support and current weapon-group validity, then apply it to the current group through the normal persistence path.
- Loading should refresh the campaign GUI state and pinned selected tags just like manual tag-button changes.
- The feature should not silently mutate other groups. Other matching groups only change when the user presses Load on those groups.

Related campaign GUI requirements:
- In the options container, broad-copy or destructive actions should require explicit confirm/cancel controls before executing. This includes:
  - `Copy to other ships of same variant`
  - `Copy to other ships of same variant (same hull type)`
  - `Copy previous loadout`
  - `Copy to other ships of same variant for all loadouts`
  - `Copy to other ships of same variant for all loadouts (same hull type)`
  - `Copy previous loadout for entire fleet`
  - `Reload settings`
- A stray click should never immediately execute one of those actions.
- Test whether the background color of weapon-group heading section bars such as `Group 1` can be changed to orange. This is only a test target for weapon-group container headings for now.

Resilience requirements:
- Missing weapons should not crash loading or parsing the external preset file. A preset whose weapon IDs no longer exist should remain inert unless deliberately cleaned up.
- Removed, renamed, or changed tags should not crash loading. Load should canonicalize known legacy aliases, discard unknown/unsupported tags, discard tags disabled for the current weapon group, and remove incompatible combinations using the same rules as normal tag-button persistence.
- Corrupt or partially invalid preset files should fail softly: preserve recoverable entries where possible, ignore invalid entries, and log/report enough information to diagnose the issue.
- Saving should avoid writing duplicate tags and should preserve canonical tag names rather than legacy aliases.
- Decide later whether saving an empty tag list clears the preset, creates an explicit empty preset, or asks for a separate delete/clear behavior.

Likely implementation touchpoints:
- `ShipView.buildWeaponGroupContainer(...)`: move the preset button row from below the weapon list to directly below the `Group N` heading; inspect spacing/padding in the preset row and adjacent containers.
- Campaign weapon-group status rendering: inspect why the preset action status line leaves a gap and why stray extra white text appears beneath the yellow status text.
- Campaign option-button actions: reuse or extend the existing confirm/cancel button pattern so all broad-copy and reload-settings actions require confirmation before execution.
- Campaign heading styling: inspect whether weapon-group section-heading bars can be recolored orange without affecting unrelated section headings.
- `TagButton` / campaign tag-button persistence flow: continue to reuse existing `loadPersistentTags(...)`, `persistTags(...)`, sanitization, and `campaignTagSelectionVersion` behavior.
- `utils` persistence helpers: keep the existing external preset key generation and file format unless a specific bug requires change.
- External file read/write layer: keep the save-independent external preset path and format.

Acceptance criteria for eventual implementation:
- Buttons appear once per non-empty weapon group.
- Preset buttons are positioned directly beneath the `Group N` heading.
- The horizontal gap between `Save Preset` and `Load Preset` is removed or reduced to the intended compact spacing.
- Clicking save/load does not leave unwanted extra white text or awkward spacing above the tag list.
- Save writes the currently selected tags for the normalized weapon-type combination to a portable external preset file.
- Load applies the saved external preset for the normalized weapon-type combination to the current group.
- The feature works while normal tag storage mode remains `Index`.
- Behavior is defined and tested under `WeaponComposition` and `WeaponCompositionGlobal` modes.
- Presets work across campaign saves/profiles by copying or retaining the external file.
- Loaded tags are canonical, valid for the current weapon group, compatible with each other, and resilient to missing old tags or removed weapons.
- Broad-copy and reload-settings actions in the options container all require explicit confirm/cancel before executing.
- The weapon-group heading recolor test determines whether orange heading bars are viable.
- Existing normal tag persistence remains backward-compatible.
- UI validation confirms the added buttons and confirm/cancel controls do not break 1080p campaign weapon-group layout or tag scrolling.

Risks and open questions:
- Need local inspection of the current weapon-composition key helper; existing key semantics may include weapon counts, while this feature should match by unique weapon IDs regardless of counts.
- Need to choose how preset action feedback is shown if inline status text is removed or minimized.
- Need to identify the source of the stray extra white text currently rendered beneath the yellow preset status line.
- Need to decide whether presets should be per active loadout slot or shared across loadout slots by default.
- Need to decide whether a future delete/clear preset button is necessary, or whether saving an empty tag list is sufficient.
- Need in-game UI validation because adding preset buttons and more confirm/cancel affordances reduces vertical space available for tag scrolling.
- Need in-game UI validation because adding two buttons reduces vertical space available for tag scrolling.
