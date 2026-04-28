# PLANS.md

## Active task

Tag-system standardization.
Status: baseline completeTagList pass done; next is narrow TF/SF helper consolidation.

## Goal

Stage the tag-system roadmap cleanly: baseline list is canonicalized, next reduce TF/SF duplication with behavior preservation, then add parameterized thresholds, and only after that do broader naming/review waves.

## Why this task matters

The baseline pass fixed immediate list drift, but remaining TF/SF conditional plumbing is still duplicated and brittle. Consolidating that narrowly first reduces regression risk before adding new parameterized tags and later renames.

## Acceptance criteria

- [x] `completeTagList` is canonical and parses cleanly against current tag support.
- [x] Obvious stale/legacy names are normalized to canonical equivalents.
- [x] Supported tag families have representation in baseline unless intentionally excluded.
- [x] Allowed-values comment block in generated settings source reflects current support.
- [ ] TF/SF conditional parsing/canonicalization/tooltip-condition/evaluation duplication is reduced via narrow helper abstractions without changing behavior.
- [ ] Parameterized threshold backlog and naming direction are explicitly staged for follow-up.
- [ ] `compileKotlin` passes before push.

## Constraints

- [ ] Minimize unrelated changes.
- [ ] Preserve existing style and structure.
- [ ] Prefer targeted verification first.
- [ ] Do not revert prior-agent/user work in the dirty tree.
- [ ] Do not overwrite the original AGC mod folder.
- [ ] Treat `build.gradle.kts` as the source for generated `mod_info.json`, version files, and `Settings.editme`.

## Current understanding

Current canonical names and alias rules are defined in `WeaponAITags.kt`; generated settings text/lists are authored in `build.gradle.kts`. TF/SF behavior is spread across helper utilities and per-tag classes; consolidation should stay mechanical and preserve semantics.

## Near-term queue

1. Narrow TF/SF flux-condition helper pass.
   - Introduce helper abstractions for canonical TF/SF conditional parsing, canonicalization, tooltip-condition wording, and evaluation.
   - Naming direction for threshold classes: prefer comparator-explicit `...AboveFluxTag` / `...BelowFluxTag` naming over vague `At...` naming.
   - Assess whether one behavior class per family plus a shared FluxCondition model is practical for `TargetShield` / `AvoidShield` / `PD` / `HoldFire` / `ForceFire`.
2. Parameterized thresholds:
   - `IgnoreMinorPD(H<...)`
   - `ConserveAmmo(A<...)`
   - `ConservePDAmmo(A<...)`
   - HF support where TF/SF currently exist
   - LunaSettings exposure of the soft-flux total-flux cap
3. Review/audit wave:
   - tag incompatibility review
   - tooltip accuracy/consistency review
   - README update with realistic per-tag example use cases
   - text consistency sweep (for example `Avd -> Avoid`)
4. Naming/logic review wave:
   - `Hold -> HoldFire` (preserve `Hold(...)` aliases)
   - `ForceAF -> ForceAutoFire` (preserve `ForceAF` alias)
   - add `PrioBig`
   - review `PrioPD -> PrioSmall`
   - review `BigShip/SmallShip -> TargetBig/TargetSmall`
   - review `TargetPhase -> PrioPhase` (only if semantics match prioritization)
5. Larger system work:
   - rotate-toward-closest-valid-target behavior as ship mode rather than global aiming behavior
   - deep dive on priority-system consistency/transparency
   - broader LunaLib/settings migration strategy

## Plan

- [x] Validate proposed baseline entries against parser and canonicalizer.
- [x] Normalize stale aliases and remove post-canonical duplicates.
- [x] Update complete/default list source and allowed-values comments in `build.gradle.kts`.
- [ ] Introduce narrow helper abstractions for existing TF/SF conditional tags only.
- [ ] Preserve canonical names, legacy compatibility, and gameplay behavior.
- [ ] Defer HF support and new parameterized tags until after helper consolidation.
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

Baseline curation is complete. The active implementation focus is now narrow TF/SF helper consolidation before adding new parameterized tag families.
