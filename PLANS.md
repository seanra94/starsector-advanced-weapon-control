# PLANS.md

## Active task

Tag-system review/audit wave.

Current status: baseline list curation, TF/SF helper groundwork, ammo-threshold canonicalization, preferred `NoPD(Waste>...)`, retained `NoPD(H<...)` support, burst-beam packet-estimation remediation, HF support, and LunaSettings exposure of `SFTUpperFluxLimit` are complete. The active implementation focus is now the review/audit wave: tag incompatibilities, tooltip consistency, README tag-table synchronization/examples, and text consistency.

## Goal

Keep the tag system compatibility-safe and user-understandable after the recent canonicalization work. Audit the visible/user-facing layer before starting larger rename or behavior waves.

## Current understanding

Canonical names and alias rules are defined in `WeaponAITags.kt`; generated settings text and tag lists are authored in `build.gradle.kts`.

Current canonical names:
- Ammo-gated opportunist behavior: `Opportunist(A<...%)`
- Ammo-gated PD reservation behavior: `PD(A<...%)`
- Preferred minor-PD suppression: `NoPD(Waste>...)`
- Simpler retained health-threshold minor-PD suppression: `NoPD(H<...)`

Compatibility aliases remain important:
- `ConserveAmmo` / `ConserveAmmo(A<...%)`
- `ConservePDAmmo` / `ConservePDAmmo(A<...%)`
- `CnsrvPDAmmo` / `CnsrvPDAmmo(A<...%)`
- `IgnoreMinorPD` / `IgnoreMinorPD(H<...)`

`NoPD(Waste>...)` uses bounded waste: `max(0, estimatedAttackPacketDamage - estimatedTargetDamageRequired) / estimatedAttackPacketDamage`. Continuous beams estimate attack packet damage as `beamDps * 0.5s`. Burst beams estimate one committed burst packet: `beamDps * (burstDuration + beamChargeupTime / 3 + beamChargedownTime / 3)`. This predicts Phase Lance at `1000 * (1.0 + 0.375/3 + 0.375/3) = 1250` and Tachyon Lance at `750 * (2.5 + 0.75/3 + 0.75/3) = 2250`, matching expected vanilla burst damage within rounding.

`SFTUpperFluxLimit` / `Settings.softFluxTotalFluxCap()` is Luna-exposed with default `0.9`; Settings.editme fallback remains supported. It is the total-flux safety cap used by soft-flux conditional tags.

## Near-term queue

1. Review/audit wave:
   - tag incompatibility review
   - tooltip accuracy/consistency review
   - README tag table canonical-name and alias synchronization
   - README tag table examples column: add one realistic grounded example/use case for every tag
   - text consistency sweep, for example `Avd -> Avoid`
2. Naming/logic review wave:
   - `Hold -> HoldFire`, preserving `Hold(...)` aliases
   - `ForceAF -> ForceAutoFire`, preserving `ForceAF` alias
   - add `PrioBig`
   - review `PrioPD -> PrioSmall`
   - review `BigShip/SmallShip -> TargetBig/TargetSmall`
   - review `TargetPhase -> PrioPhase`, only if semantics match prioritization
3. Larger system work:
   - rotate-toward-closest-valid-target behavior as ship mode rather than global aiming behavior
   - deep dive on priority-system consistency/transparency
   - broader LunaLib/settings migration strategy

## Acceptance criteria

- [x] `completeTagList` is canonical and parses cleanly against current tag support.
- [x] Obvious stale/legacy names are normalized to canonical equivalents.
- [x] Supported tag families have representation in baseline unless intentionally excluded.
- [x] Allowed-values comment block in generated settings source reflects current support.
- [x] TF/SF conditional parsing/canonicalization/tooltip-condition/evaluation duplication is reduced via narrow helper abstractions without changing behavior.
- [x] Parameterized ammo-threshold tags are implemented with plain-tag compatibility preserved.
- [x] `NoPD(Waste>...)` is implemented with `NoPD(H<...)` and `IgnoreMinorPD` compatibility retained.
- [x] `NoPD(Waste>...)` estimates burst beams as one committed burst packet rather than as a short continuous-beam slice.
- [x] LunaSettings exposes the soft-flux total-flux cap (`SFTUpperFluxLimit`) with default `0.9`.
- [ ] Incompatibility definitions are audited against current canonical families and legacy aliases.
- [ ] Tooltips are audited for canonical names, thresholds, ammo/flux notation, and legacy behavior.
- [ ] README tag table is synchronized with current canonical names and important aliases.
- [ ] README tag table examples column is staged or implemented with realistic use cases.
- [ ] `compileKotlin` passes before push.

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
- Large rename waves should be staged only after behavior confirmation to avoid compatibility regressions.
- `NoPD(H<...)` must continue to describe `H` as effective durability, not literal hull.
- `NoPD(Waste>...)` must not treat burst beams as short continuous cleanup beams; Phase Lance/Tachyon Lance-style weapons should estimate as high committed burst packets.
- LunaSettings and Settings.editme defaults for `SFTUpperFluxLimit` must remain aligned at `0.9`.
- Scoped Luna/default audit left some mismatches unchanged as ambiguous: `targetShields_threshold`, `avoidShields_threshold`, `strictBigSmallShipMode`, and nearby custom-AI defaults. Do not normalize these blindly.
- The rename requests are intentionally deferred until logic confirmation.
- `HoldFire` / `ForceAutoFire` canonical rename work should watch for UI density and button-label length constraints, especially outside the campaign GUI.
