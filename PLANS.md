# PLANS.md

## Active task

Tag-system review/audit wave.

Current status: baseline list curation, TF/SF helper groundwork, ammo-threshold canonicalization, preferred `NoPD(Waste>...)`, retained `NoPD(H<...)` support, burst-beam packet-estimation remediation, HF support, LunaSettings exposure of `SFTUpperFluxLimit`, weapon-relative `NoPD(H<...>)` durability, first static tooltip/text consistency pass, `HoldFire(...)` canonicalization, `ForceAutoFire` canonicalization, generated allowed-values repair, `PrioSmall` canonicalization, `PrioBig` implementation, `TargetBig` / `TargetSmall` canonicalization, and `TargetPhase` semantic review are complete.

The active implementation focus is now the review/audit wave: incompatibility consistency, remaining tooltip text, README tag-table synchronization, and README examples. Phase-tag behavior questions are deliberately deferred to the bottom of the priority list.

## Goal

Keep the tag system compatibility-safe and user-understandable after canonicalization. Finish remaining naming/logic work without breaking saved tags or legacy aliases, then synchronize user-facing README tag documentation and examples.

## Current understanding

Canonical names and alias rules are defined in `WeaponAITags.kt`; generated settings text and tag lists are authored in `build.gradle.kts`.

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
- Phase-ship targeting/pressure tag: `TargetPhase` remains canonical. Do not rename it to `PrioPhase` unless behavior changes to priority-only.

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

## Near-term queue
## Near-term queue

1. Review/audit wave:
   - tag incompatibility review
   - remaining tooltip accuracy/consistency review
   - README tag table canonical-name and alias synchronization
   - README tag table examples column: add one realistic grounded example/use case for every tag
   - text consistency sweep, for example `Avd -> Avoid`
2. Larger system work:
   - rotate-toward-closest-valid-target behavior as ship mode rather than global aiming behavior
   - deep dive on priority-system consistency/transparency
   - broader LunaLib/settings migration strategy
3. Lowest-priority deferred phase-tag review:
   - revisit `TargetPhaseTag` and `AvoidPhaseTag` only after higher-priority audits and with cautious runtime testing
   - preserve original-author behavior unless in-game evidence shows the current base-AI/custom-AI interaction is wrong
   - specifically investigate whether `AvoidPhaseTag.isBaseAiValid(...)` intentionally accepts phase ships and rejects normal ships, or whether that is inverted
   - only then decide whether tooltip-only changes, base-AI validity changes, or no changes are appropriate

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
- [ ] Incompatibility definitions are audited against current canonical families and legacy aliases.
- [ ] Remaining tooltips are audited for canonical names, thresholds, ammo/flux notation, and legacy behavior.
- [ ] README tag table is synchronized with current canonical names and important aliases.
- [ ] README tag table examples column is staged or implemented with realistic use cases.
- [ ] `compileKotlin` passes before push.
- [ ] Deferred phase-tag review is resolved or intentionally closed after cautious runtime testing.

## Constraints

- Minimize unrelated changes.
- Preserve existing style and structure unless cleanup is the task.
- Prefer targeted verification first.
- Do not revert prior-agent/user work in the dirty tree.
- Do not overwrite the original AGC mod folder.
- Treat `build.gradle.kts` as the source for generated `mod_info.json`, version files, and `Settings.editme`.
- Preserve persisted tag/loadout compatibility when renaming or canonicalizing tags.

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
- Large rename waves should be staged and compatibility-preserving because persisted saves/settings/loadouts may contain old tag strings.
- `NoPD(H<...)` must continue to describe `H` as effective durability, not literal hull.
- `NoPD(Waste>...)` must not treat burst beams as short continuous cleanup beams; Phase Lance/Tachyon Lance-style weapons should estimate as high committed burst packets.
- LunaSettings and Settings.editme defaults for `SFTUpperFluxLimit` must remain aligned at `0.9`.
- Scoped Luna/default audit left some mismatches unchanged as ambiguous: `targetShields_threshold`, `avoidShields_threshold`, `strictBigSmallShipMode`, and nearby custom-AI defaults. Do not normalize these blindly.
- `PrioSmall` is canonical for former `PrioPD` behavior; legacy `PrioPD` / `PrioritisePD` / `PrioritizePD` aliases must remain accepted.
- `PrioBig` must remain priority-only and must not restrict non-ship target validity.
- `TargetBig` / `TargetSmall` are canonical for former `BigShip` / `SmallShip` target-restriction tags; legacy aliases must remain accepted.
- `TargetPhase` remains canonical because its behavior is mixed, not priority-only. Static inspection shows `TargetPhaseTag` strongly prioritizes phase ships and accepts only phase ships through `isBaseAiValid(...)`, but does not override `isValidTarget(...)`, so custom target selection may still consider other valid targets. This makes `TargetPhase` more accurate than `PrioPhase` unless behavior changes.
- Deferred phase-tag caution: `AvoidPhaseTag.isBaseAiValid(...)` appears suspicious because it accepts phase ships and rejects normal ships in the base-AI validity path, while `shouldFire(...)` then refuses shots when the phase ship may phase before impact. This could prevent custom retargeting away from a bad phase target, but it is original-author code and may rely on non-obvious AI handoff behavior. Treat this as lowest-priority until runtime evidence justifies changing it.
