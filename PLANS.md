# PLANS.md

## Active task

Tag-system baseline and standardization.
Status: active completeTagList validity/coverage pass.

## Goal

Make `completeTagList` a stable canonical baseline: valid against current parser/canonicalizer support, ordered intentionally, and complete enough to represent active tag families without changing gameplay behavior.

## Why this task matters

The tag list itself is now part of user-facing configuration UX and influences discoverability. A stale or inconsistent baseline causes confusion, hidden features, and invalid/stale examples in generated settings.

## Acceptance criteria

- [ ] `completeTagList` is canonical and parses cleanly against current tag support.
- [ ] Obvious stale/legacy names are normalized to canonical equivalents.
- [ ] Supported tag families have representation in baseline unless intentionally excluded.
- [ ] Allowed-values comment block in generated settings source reflects current support.
- [ ] `compileKotlin` passes before push.

## Constraints

- [ ] Minimize unrelated changes.
- [ ] Preserve existing style and structure.
- [ ] Prefer targeted verification first.
- [ ] Do not revert prior-agent/user work in the dirty tree.
- [ ] Do not overwrite the original AGC mod folder.
- [ ] Treat `build.gradle.kts` as the source for generated `mod_info.json`, version files, and `Settings.editme`.

## Current understanding

Current canonical names are defined in `WeaponAITags.kt`, and generated settings text/lists are authored in `build.gradle.kts`. That pair is the source of truth for baseline ordering and visible allowed-values guidance.

## Near-term queue

- completeTagList baseline/validity audit
- narrow TF/SF flux-condition helper pass
- parameterized tags:
- IgnoreMinorPD(H<...)
- ConserveAmmo(A<...)
- ConservePDAmmo(A<...)
- HF support where TF/SF currently exist
- LunaSettings exposure of the soft-flux total-flux cap
- broad review tasks:
- tag incompatibility review
- tooltip accuracy/consistency review
- priority-system review
- README/example review
- text consistency sweep
- rename wave only after logic confirmation

## Plan

- [ ] Validate proposed baseline entries against parser and canonicalizer.
- [ ] Normalize stale aliases and remove post-canonical duplicates.
- [ ] Update complete/default list source and allowed-values comments in `build.gradle.kts`.
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

## Current status

Tag naming/threshold semantics have been actively updated; baseline curation now takes priority over further GUI polish in this planning window.
