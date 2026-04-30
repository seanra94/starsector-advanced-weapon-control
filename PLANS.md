# PLANS.md

## Active task

Tag-system standardization.

Current status: baseline list curation, TF/SF helper groundwork, ammo-threshold canonicalization, preferred `NoPD(Waste>...)`, retained `NoPD(H<...)` support, and HF support are complete. The active implementation focus is now narrow `NoPD(Waste>...)` burst-beam packet-estimation remediation. LunaSettings exposure of the soft-flux total-flux cap resumes after that fix is validated.

## Goal

Keep the tag-system roadmap staged and compatibility-safe: preserve completed canonicalization work, fix the current `NoPD(Waste>...)` beam-estimation issue, then continue with settings exposure, audits, and deferred naming waves.

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

`NoPD(Waste>...)` uses bounded waste: `max(0, estimatedAttackPacketDamage - estimatedTargetDamageRequired) / estimatedAttackPacketDamage`. Continuous beams should estimate attack packet damage as `beamDps * 0.5s`. Burst beams should estimate one committed burst packet: `beamDps * (burstDuration + beamChargeupTime / 3 + beamChargedownTime / 3)`. This predicts Phase Lance at `1000 * (1.0 + 0.375/3 + 0.375/3) = 1250` and Tachyon Lance at `750 * (2.5 + 0.75/3 + 0.75/3) = 2250`, matching expected vanilla burst damage within rounding.

## Near-term queue

1. `NoPD(Waste>...)` remediation:
   - split continuous-beam and burst-beam packet estimates
   - keep continuous beams on the cleanup-window model
   - treat burst beams as one committed burst packet
   - preserve `NoPD(H<...)` and `IgnoreMinorPD` compatibility
2. Remaining parameterized thresholds/settings:
   - LunaSettings exposure of the soft-flux total-flux cap
3. Review/audit wave:
   - tag incompatibility review
   - tooltip accuracy/consistency review
   - README tag table canonical-name and alias synchronization
   - README tag table examples column: add one realistic grounded example/use case for every tag
   - text consistency sweep, for example `Avd -> Avoid`
4. Naming/logic review wave:
   - `Hold -> HoldFire`, preserving `Hold(...)` aliases
   - `ForceAF -> ForceAutoFire`, preserving `ForceAF` alias
   - add `PrioBig`
   - review `PrioPD -> PrioSmall`
   - review `BigShip/SmallShip -> TargetBig/TargetSmall`
   - review `TargetPhase -> PrioPhase`, only if semantics match prioritization
5. Larger system work:
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
- [ ] `NoPD(Waste>...)` estimates burst beams as one committed burst packet rather than as a short continuous-beam slice.
- [ ] Remaining threshold backlog and naming direction remain explicitly staged for follow-up.
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
- The rename requests are intentionally deferred until logic confirmation.
- `HoldFire` / `ForceAutoFire` canonical rename work should watch for UI density and button-label length constraints, especially outside the campaign GUI.
