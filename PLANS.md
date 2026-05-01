Active task
Campaign GUI follow-up for external weapon-composition presets and option-action safety.

Current status
The external weapon-composition preset data layer is already implemented.
Presets are stored externally and intended to work across saves and profiles.
Preset matching is based on normalized unique weapon IDs and is count-insensitive by design.
Campaign Save Preset and Load Preset buttons are already implemented and function in the campaign ship editor.
The active implementation work is now GUI follow-up and action-safety polish, not core preset data-layer design.
The previous tag-canonicalization, README tag-table synchronization, Luna-default alignment, ship-mode storage fix, and related cleanup passes are complete enough to treat them as finished groundwork rather than the current workstream.

Goal
Keep the campaign preset workflow convenient, explicit, and compatibility-safe.
Polish the campaign GUI so the preset controls read as a natural part of each weapon-group container.
Require explicit confirm/cancel steps before broad-copy or destructive option actions execute.

Current understanding
Manual external presets are meant to complement normal tag storage modes, not replace them with another hidden mode.
Users should be able to stay in Index mode while manually saving and loading reusable presets for repeated weapon combinations.
Matching is by weapon-type set, not by exact count. A group with 2x PD Laser + 2x Tactical Laser should match a group with 3x PD Laser + 1x Tactical Laser, but not a group that lacks Tactical Laser entirely.
Save updates the shared external preset for the current normalized weapon-type key and active loadout slot.
Other matching groups do not change until the user explicitly clicks Load for that group.
The current persistence and canonicalization flow should continue to reuse the normal campaign tag sanitization and compatibility logic where practical.
Current fork data/config/LunaSettings.csv defaults remain authoritative for Luna-exposed settings.

Near-term queue
Campaign weapon-group GUI polish:
- move Save Preset and Load Preset to the top of each weapon-group container directly beneath the Group N heading
- remove or reduce the tiny horizontal gap between the two preset buttons
- investigate and remove the stray extra white text shown under the yellow save/load status text
- avoid leaving extra status-text spacing between the preset controls and the tag list if possible

Campaign options-container safety:
- add confirm/cancel protection for Copy to other ships of same variant
- add confirm/cancel protection for Copy to other ships of same variant (same hull type)
- add confirm/cancel protection for Copy previous loadout
- add confirm/cancel protection for Copy to other ships of same variant for all loadouts
- add confirm/cancel protection for Copy to other ships of same variant for all loadouts (same hull type)
- add confirm/cancel protection for Copy previous loadout for entire fleet
- add confirm/cancel protection for Reload settings

Scoped experiment:
- test whether weapon-group heading section bars such as Group 1 can be recolored orange without affecting unrelated section headings

Deferred backlog, not active implementation:
- phase-tag behavior review
- original-upstream default restoration
- broader LunaLib/settings migration strategy
- rotate-toward-closest-valid-target behavior as ship mode rather than global aiming behavior

Acceptance criteria
Preset buttons remain available once per non-empty weapon group in the campaign ship editor.
Preset buttons are positioned directly beneath the Group N heading.
The horizontal gap between Save Preset and Load Preset is removed or reduced to the intended compact spacing.
Clicking Save or Load does not leave unwanted extra white text or awkward blank spacing above the tag list.
Save and Load continue to operate on the existing external preset store and current normalized count-insensitive matching semantics.
The feature continues to work while normal tag storage mode remains Index.
Other matching groups remain unchanged until the user explicitly presses Load on them.
The listed broad-copy and Reload settings actions all require explicit confirm/cancel before execution.
The orange heading-bar test determines whether the weapon-group section bars can be recolored without collateral styling changes.
Normal tag persistence remains backward-compatible.
compileKotlin passes before push.
If campaign GUI code changes, in-game validation confirms the campaign ship editor still opens, tag scrolling still works, and the added controls fit the 1080p layout acceptably.

Constraints
Minimize unrelated changes.
Preserve current preset data semantics unless a concrete bug requires change.
Do not silently mutate other groups when saving a preset.
Do not re-open phase-tag behavior from static reasoning alone.
Do not overwrite the original AGC mod folder.
Treat build.gradle.kts as the source of truth for generated mod_info.json, version files, and Settings.editme.
For Luna-exposed settings, treat data/config/LunaSettings.csv defaults as the source of truth.
Preserve persisted tag, loadout, and legacy-alias compatibility when changing surrounding code or user-facing text.

Verification needed
Minimum code check:

$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat compileKotlin

For deploy and runtime checks, package with:

$env:STARSECTOR_DIRECTORY='C:\Games\Starsector'
.\gradlew.bat jar create-metadata-files write-settings-file

Then deploy or copy to C:\Games\Starsector\mods\Advanced-Gunnery-Control-Fork using the established exclusions and check the campaign ship editor for:
- preset button placement
- preset button spacing
- save/load status-text behavior
- confirm/cancel behavior for the listed option actions
- whether the orange heading-bar test is viable
- whether tag scrolling and layout remain usable at 1080p

Risks and open questions
The source of the stray extra white text beneath the yellow preset status line is still unknown.
If the inline preset status text is removed or minimized, the replacement feedback pattern still needs to be chosen deliberately.
Adding more UI affordances in the campaign editor reduces available vertical space for tag scrolling and tag lists.
Weapon-group section-heading recoloring may be limited by how Starsector addSectionHeading(...) bars are rendered.
The exact future behavior for clearing or deleting an external preset remains undecided and is not part of the current task.
Original-upstream default restoration remains bottom backlog work; if it is ever revisited, compare original upstream LunaSettings and original upstream Settings/runtime defaults and prefer original upstream LunaSettings when they differ.
Phase-tag review remains bottom backlog work and should only be revisited with runtime evidence, not static suspicion alone.
