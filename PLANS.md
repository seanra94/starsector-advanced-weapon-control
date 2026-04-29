# PLANS.md

## Active task

Tag-system standardization.
Status: baseline and TF/SF helper groundwork accepted; ammo-threshold tags implemented; active next step is `IgnoreMinorPD(H<...)`.

## Goal

Stage the tag-system roadmap cleanly: baseline list is canonicalized, TF/SF helper groundwork is complete, and ammo-threshold canonicalization is done before broader threshold/naming waves.

## Why this task matters

With TF/SF helper groundwork and parameterized ammo tags in place, `IgnoreMinorPD(H<...)` is the next narrow threshold feature in the staged backlog.

## Acceptance criteria

- [x] `completeTagList` is canonical and parses cleanly against current tag support.
- [x] Obvious stale/legacy names are normalized to canonical equivalents.
- [x] Supported tag families have representation in baseline unless intentionally excluded.
- [x] Allowed-values comment block in generated settings source reflects current support.
- [x] TF/SF conditional parsing/canonicalization/tooltip-condition/evaluation duplication is reduced via narrow helper abstractions without changing behavior.
- [x] Parameterized ammo-threshold tags are implemented with plain-tag compatibility preserved (`Opportunist(A<...)` and `PD(A<...)` canonicals plus legacy aliases).
- [ ] Remaining threshold backlog and naming direction are explicitly staged for follow-up.
- [ ] `compileKotlin` passes before push.

## Constraints

- [ ] Minimize unrelated changes.
- [ ] Preserve existing style and structure.
- [ ] Prefer targeted verification first.
- [ ] Do not revert prior-agent/user work in the dirty tree.
- [ ] Do not overwrite the original AGC mod folder.
- [ ] Treat `build.gradle.kts` as the source for generated `mod_info.json`, version files, and `Settings.editme`.

## Current understanding

Current canonical names and alias rules are defined in `WeaponAITags.kt`; generated settings text/lists are authored in `build.gradle.kts`. Canonical ammo-threshold names are `Opportunist(A<...)` and `PD(A<...)`, with legacy `ConserveAmmo` / `ConservePDAmmo` / `CnsrvPDAmmo` aliases preserved.

## Near-term queue

1. Remaining parameterized thresholds:
   - `IgnoreMinorPD(H<...)`
   - HF support where TF/SF currently exist
   - LunaSettings exposure of the soft-flux total-flux cap
2. Review/audit wave:
   - tag incompatibility review
   - tooltip accuracy/consistency review
   - README update with realistic per-tag example use cases
   - text consistency sweep (for example `Avd -> Avoid`)
3. Naming/logic review wave:
   - `Hold -> HoldFire` (preserve `Hold(...)` aliases)
   - `ForceAF -> ForceAutoFire` (preserve `ForceAF` alias)
   - add `PrioBig`
   - review `PrioPD -> PrioSmall`
   - review `BigShip/SmallShip -> TargetBig/TargetSmall`
   - review `TargetPhase -> PrioPhase` (only if semantics match prioritization)
4. Larger system work:
   - rotate-toward-closest-valid-target behavior as ship mode rather than global aiming behavior
   - deep dive on priority-system consistency/transparency
   - broader LunaLib/settings migration strategy

## Plan

- [x] Validate proposed baseline entries against parser and canonicalizer.
- [x] Normalize stale aliases and remove post-canonical duplicates.
- [x] Update complete/default list source and allowed-values comments in `build.gradle.kts`.
- [x] Introduce narrow helper abstractions for existing TF/SF conditional tags only.
- [ ] Preserve canonical names, legacy compatibility, and gameplay behavior.
- [x] Implement canonical `Opportunist(A<...)` and `PD(A<...)` with plain-tag compatibility.
- [ ] Implement canonical `IgnoreMinorPD(H<...)` with plain-tag compatibility.
- [ ] Defer HF support and remaining staged thresholds until after `IgnoreMinorPD(H<...)`.
- [ ] Record `Hold->HoldFire` and `ForceAF->ForceAutoFire` as canonical rename goals while preserving old aliases in the later rename wave.
- [ ] Re-run `compileKotlin`.
- [ ] Run `jar create-metadata-files write-settings-file` when generation inputs changed.
- [ ] Deploy updated mod to `C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork`.
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

- Settings comment blocks can drift from actual support if list/regex updates are not mirrored in `build.gradle.kts`.
- Large rename waves should be staged only after behavior confirmation to avoid compatibility regressions.
- `IgnoreMinorPD(H<...)` must describe `H` as effective durability (not literal hull) when implemented.
- The rename requests are intentionally deferred until logic confirmation (`BigShip/SmallShip` target naming and `TargetPhase` prioritization semantics).
- `HoldFire` / `ForceAutoFire` canonical rename work should watch for UI density and button-label length constraints, especially outside the campaign GUI.

## Current status

Baseline curation, TF/SF helper groundwork, and ammo-threshold tags are complete. The active implementation focus is now `IgnoreMinorPD(H<...)`.
