# Advanced Weapon Control - Starsector Mod #

![Cover Image](imgs/agc.png "Cover Image")

This is a utility mod that allows you to set your auto-fire weapon groups to different modes.
For example, in PD-Only mode, weapons will ONLY fire at missiles and fighters, not at enemy ships.
There are many modes, check out the full list of available modes below!

Sections of this readme are roughly ordered by importance. For the most important stuff, stop after Settings.

Note: If you don't have a markdown renderer handy, you can read the online version at 
<https://github.com/DesperatePeter/starsector-advanced-weapon-control/blob/master/README.md>

Also visit the forums post: <https://fractalsoftworks.com/forum/index.php?topic=21280.0>

## TL;DR Instructions ##

- Press the "J" key in combat to open up a GUI (select an ally via "R" key beforehand to modify their modes)
- Press the "J" key in the campaign map to open a GUI
- By default, firing modes are automatically saved/loaded between combats (per ship)
- (optional) edit Starsector/mods/AdvancedGunneryControl/Settings.editme or use LunaSettings to customize the mod behavior.

## Installation ##

Simply download the latest release from <https://github.com/DesperatePeter/starsector-advanced-weapon-control/releases> 
and unzip it in your mods folder. 
If you install a new version, please make sure to delete the old folder before doing so. Backup you Settings.editme 
if you wish to keep it.

## Controls (Combat) ##

Press the "J"-Key to open up a GUI. This will pause the game and lock the camera. Press "J" again to close the GUI.

Target an ally ("R"-Key) **before** opening the GUI to instead adjust their modes.

In that GUI, you will be able to apply one or more tags to each weapon group. Each added tag will change the target 
priority of that weapon group and/or prevent it from firing in certain situations. Please note that a weapon group
will only fire if **all** tags allow firing, so if you e.g. set a weapon group to HoldFire(TF>50%) and Fighter, it will
only fire when your ship's total flux is below 50% and will only target fighters.
Rule of thumb: Usually, less is more. Don't set too many tags if you want your weapons to actually fire.
Note that tags only affect weapon groups set to autofire.

You can also set ship AI modes in a very similar fashion. These will only affect AI-controlled ships, not the player ship.

### Gunnery Control GUI ###

If you don't like having to set up your ships firing modes during (simulated) combat, there is also a campaign GUI available.
Simply press the "J"-Key while on the campaign map, and the interface will guide you through configuring your
firing modes. Unfortunately, I **can't directly interface with the ship refit screen**, which would be much better,
so this is the best I can do.

### Loadouts ###

You can define (by default 3) different mode loadouts for your fleet. You can then cycle through these loadouts for all
ships by pressing the "+"-Key in combat or clicking the corresponding button in the GUI. Doing so will switch all firing modes,
suffixes and ship modes to those defined in the next loadout. Loadouts are cycled fleet-wide, not per ship.

You can configure the number of available loadouts and their names in the Settings.editme file.

I would recommend leaving one loadout blank (i.e. everything default) for your entire fleet to give you a fallback option.

## Tags ##

Note: Not all tags are enabled by default, cf. `Settings.editme` or LunaSettings to customize which tags are available.
If you have ConsoleCommands installed, you can also hotload new tags via `AGC_addTags`.
Ship refers to a non-fighter ship in the table below.
Replace `N` with a number when a tag name contains `N`; percent forms use `N%`, while raw-value forms such as `H<N` use literal values.
Flux shorthand: `TF` = total flux, `SF` = soft flux, `HF` = hard flux.

Legacy tag names remain accepted for saved-loadout compatibility:
`Hold(...)` -> `HoldFire(...)`; `ForceAF` -> `ForceAutoFire`; `PrioPD` / `PrioritisePD` / `PrioritizePD` -> `PrioSmall`;
`BigShip` / `BigShips` -> `TargetBig`; `SmallShip` / `SmallShips` -> `TargetSmall`;
`ConserveAmmo(A<N%)` -> `Opportunist(A<N%)`; `ConservePDAmmo(A<N%)` / `CnsrvPDAmmo(A<N%)` -> `PD(A<N%)`;
`IgnoreMinorPD` / `IgnoreMinorPD(H<N)` -> `NoPD(H<N)`; old `BurstPD...` soft-flux forms -> `PD(SF>N%)`.

| Tag | Targets | Prioritizes | Requirements | Uses Custom AI When | Comments | Incompatible with | Suggested Use Case | Recommended as suggested tag? |
|:---:|:---|:---|:---|:---:|:---|:---:|:---|:---|
| `PD` | Fighters/Missiles | Fighters/Missiles | PD Weapon | No | Will never shoot regular ships. | `Fighter`, `Opportunist`, `NoPD`, `PD(TF>N%)`, `PD(SF>N%)`, `PD(HF>N%)`, `TargetBig`, `TargetSmall` | Weapons that only make sense as PD weapons and should not waste shots/flux on enemy ships. | Yes |
| `Fighter` | Fighters | Fighters | None | Base AI targets non-fighter | Restricts targeting to fighters. | `PD`, `NoFighter`, `Opportunist`, `NoPD`, PD flux tags, `TargetBig`, `TargetSmall` | Dedicated anti-fighter weapons. | Usually not |
| `NoMissile` | Anything but missiles | Same as base AI | None | Base AI targets a missile | Prevents missile targeting. | `PD` | Weapons that should not snap to low-value missile targets. | Sometimes |
| `NoFighter` | Anything but fighters | Same as base AI | None | Base AI targets a fighter | Prevents fighter targeting. | `Fighter`, `Opportunist` | Low rate-of-fire or slow projectile weapons. | Yes |
| `NoPD` | Ships/Fighters | Ships | PD Weapon | Target is not Ship | Does not change weapon classification. | `PD`, `Fighter`, PD flux tags | Weapons that have the PD hint but are not really PD weapons, e.g. machine guns. | No |
| `NoPD(Waste>N%)` | Ships, efficient fighters/missiles | Same as base AI | None | Base AI target fails waste check | Blocks fighter/missile targets when this weapon would waste more than `N%` of estimated attack-packet damage. Weapons at or below the cleanup damage cap are unaffected. | Review with other PD target-restriction tags | High-impact weapons that can still hit fighters, e.g. a Phase Lance should ignore a nearly-dead fighter that a PD Laser can finish, but still shoot healthy fighters. | Usually not |
| `NoPD(H<N)` | Ships, durable fighters/missiles | Same as base AI | None | Base AI target fails health check | Blocks fighter/missile targets when this weapon estimates their health below `N`. Poor damage matchups count armor or shields as tougher; strong matchups do not lower the estimate. | Review with other PD target-restriction tags | Medium or heavy weapons ignore fragile fighters/missiles but still shoot durable bombers or healthy fighters. | Usually not |
| `PrioSmall` | Everything | Missile > Fighter > small ship > big ship | None | Always | Consistently prioritizes smaller targets over larger targets. Legacy aliases: `PrioPD`, `PrioritisePD`, `PrioritizePD`. | `Opportunist`, `NoPD`, `TargetBig`, `TargetSmall`, `Fighter` | Weapons that should reliably prioritize fighters and missiles over ships. | Yes |
| `PrioBig` | Everything | Larger ships | None | Always | Priority-only tag; no targeting restrictions. | - | Large weapons prefer cruisers/capitals while retaining fallback targets. | Sometimes |
| `TargetBig` | Larger ships/Fighters | Bigger=Better | None | Base AI targets below threshold | Formerly `BigShip`. Cf. strict mode in `Settings.editme`. | `TargetSmall`, `PD`, `Fighter`, PD flux tags | Low rate-of-fire or slow projectile weapons that should focus larger ships. | No |
| `TargetSmall` | Smaller ships/Fighters | Smaller=Better | None | Base AI targets above threshold | Formerly `SmallShip`. Cf. strict mode in `Settings.editme`. | `TargetBig`, `PD`, `Fighter`, PD flux tags | Precise, fast-firing, cone, or AoE weapons that should clean up smaller ships. | No |
| `AvoidShield` | Anything | Ships without shields, flanked shields, shield-off state, or high flux | None | Base AI targets shielded target | Will target missiles if applied to a PD weapon. | Other shield modes | Weapons that are ineffective vs shields. | Yes |
| `AvoidShield+` | Anything | Better AvoidShield targets | None | Base AI targets shielded target | Stricter AvoidShield variant. | Other shield modes | Frag or HE weapons that should be very selective against shields. | Sometimes |
| `TargetShield` | Anything, usually no missiles | Ships with shields and low flux | None | Base AI targets unshielded target | Takes shield flanking into consideration. | Other shield modes | Weapons that are effective against shields, e.g. needlers. | Yes |
| `TargetShield+` | Anything, usually no missiles | Better TargetShield targets | None | Base AI targets unshielded target | More aggressive TargetShield variant. | Other shield modes | Kinetic weapons that should keep up shield pressure. | Sometimes |
| `AvoidShield(TF>N%)` | Anything | As AvoidShield while total flux condition is active | None | Base AI target fails active condition | AvoidShield activates while own total flux is greater than `N%`. | Other shield modes | HE weapons become selective when the firing ship is fluxed. | Sometimes |
| `AvoidShield(SF>N%)` | Anything | As AvoidShield while soft flux condition is active | None | Base AI target fails active condition | AvoidShield activates while own soft flux is greater than `N%` and total flux is below the soft-flux cap. | Other shield modes | Weapons ease off shield-inefficient shots once soft flux builds. | Sometimes |
| `AvoidShield(HF>N%)` | Anything | As AvoidShield while hard flux condition is active | None | Base AI target fails active condition | AvoidShield activates while own hard flux is greater than `N%`. | Other shield modes | Weapons become selective after the ship has taken hard flux. | Sometimes |
| `TargetShield(TF>N%)` | Anything, usually no missiles | As TargetShield while total flux condition is active | None | Base AI target fails active condition | TargetShield activates while own total flux is greater than `N%`. | Other shield modes | Kinetic weapons keep shield pressure while the ship is already committed. | Sometimes |
| `TargetShield(SF>N%)` | Anything, usually no missiles | As TargetShield while soft flux condition is active | None | Base AI target fails active condition | TargetShield activates while own soft flux is greater than `N%` and total flux is below the soft-flux cap. | Other shield modes | Kinetics help convert soft-flux pressure into shield pressure. | Sometimes |
| `TargetShield(HF>N%)` | Anything, usually no missiles | As TargetShield while hard flux condition is active | None | Base AI target fails active condition | TargetShield activates while own hard flux is greater than `N%`. | Other shield modes | Kinetics prioritize shield pressure after taking hard flux. | Sometimes |
| `ShieldOff` | Targets without active shields | Targets without active shields | None | Base AI targets shields | Simple boolean logic: if a target has no shields or shields are turned off, it can be targeted. | Other shield modes | EMP or HE weapons that should wait for shield-down openings. | No |
| `AvoidArmor(N%)` | Everything | Low armor targets | None | Base AI targets high armor | Fires if the shot should hit shields or armor weak enough to reach at least `N%` armor effectiveness. | - | Weapons that are ineffective vs armor. | Yes |
| `AvoidPhased` | Anything | Non-phased or vulnerable phase ships | None | Base AI targets phase ships | Avoids phase ships unless they are vulnerable or unlikely to avoid the shot by phasing before impact. | `TargetPhase` | High-impact shots that should not waste fire into active phase defense. | Usually not |
| `TargetPhase` | Phase ships | Phase ships | None | Base AI targets non-phase | Targets phase-capable ships whether currently phased or not. | `AvoidPhased` | Beam or rapid-fire weapons that keep phase coils under pressure. | No |
| `PD(TF>N%)` | Varies | Same as base AI when inactive; same as PD when active | PD Weapon | No | Restricts targeting to fighters/missiles while total flux is greater than `N%`. | Cf. `PD` | Flux-hungry PD weapons, e.g. flak cannons. | No |
| `PD(SF>N%)` | Varies | Same as base AI when inactive; same as PD when active | PD Weapon | No | Restricts targeting to fighters/missiles while soft flux is greater than `N%` and total flux is below the soft-flux cap. | Cf. `PD` | Burst PD behavior when soft flux starts building. | No |
| `PD(HF>N%)` | Varies | Same as base AI when inactive; same as PD when active | PD Weapon | No | Restricts targeting to fighters/missiles while hard flux is greater than `N%`. | Cf. `PD` | Defensive PD behavior after taking hard flux. | No |
| `Opportunist` | Ignores fighters/missiles | Special* | None | Always | Only shoots when the shot is likely to hit and be effective. | `Fighter`, `PD`, `NoFighter`, PD flux tags, `Opportunist(A<N%)` | Weapons with severely limited ammo or extreme refire delay. | Yes |
| `Opportunist(A<N%)` | Varies | Varies | Uses ammo | Weapon ammo below `N%` | Behaves like Opportunist while ammo is below `N%`. Weapons without ammo are unaffected. Legacy alias: `ConserveAmmo(A<N%)`. | `Opportunist`, `PD(A<N%)` | Limited-ammo weapons that should become selective late. | No |
| `PD(A<N%)` | Varies | Fighters/Missiles while active | Uses non-missile ammo | Weapon ammo below `N%` | Restricts targeting to fighters and missiles while ammo is below `N%`. Weapons without ammo and missile weapons are unaffected. Legacy aliases: `ConservePDAmmo(A<N%)`, `CnsrvPDAmmo(A<N%)`. | `PD`, `Fighter`, `NoPD`, `Opportunist`, `Opportunist(A<N%)` | Limited-ammo PD weapons that should save remaining ammo for defense. | Usually not |
| `HoldFire(TF>N%)` | - | - | None | No | Stops firing when own total flux exceeds `N%`. Legacy alias: `Hold(TF>N%)`. | - | High-flux weapons. | Very |
| `HoldFire(SF>N%)` | - | - | None | No | Stops firing when own soft flux exceeds `N%` or total flux reaches the soft-flux safety cap. Legacy alias: `Hold(SF>N%)`. | - | Weapons that should stay aggressive on hard flux but ease off once soft flux stacks up. | Sometimes |
| `HoldFire(HF>N%)` | - | - | None | No | Stops firing when own hard flux exceeds `N%`. Legacy alias: `Hold(HF>N%)`. | - | Weapons that should stop after taking dangerous hard flux. | Sometimes |
| `ForceAutoFire` | - | - | None | - | Forces AI-controlled ships to keep this weapon group on autofire. This modifies ShipAI. Legacy alias: `ForceAF`. For flux weapons, combine with `HoldFire(TF>N%)`. | - | Weapons where the AI is too hesitant to fire. Combine with other tags. | Usually not |
| `Force(TF<N%)` | - | - | None | - | Circumvents firing restrictions, not targeting restrictions, while own total flux is below `N%`. | - | Fire weapons more liberally while total flux is low. | No |
| `Force(SF<N%)` | - | - | None | - | Circumvents firing restrictions, not targeting restrictions, while own soft flux is below `N%` and total flux is below the soft-flux cap. | - | Fire weapons more liberally while soft flux is low. | No |
| `Force(HF<N%)` | - | - | None | - | Circumvents firing restrictions, not targeting restrictions, while own hard flux is below `N%`. | - | Fire weapons more liberally until hard flux rises. | No |
| `Panic(H<N%)` | - | - | None | Hull < `N%` | When own hull is below `N%`, weapon fires blindly; AI ships also force autofire for the group. | - | Limited-ammo missiles with tracking. | Usually not |
| `ShipTarget` | Ships/Fighters | Selected ship target | None | Base AI does not target selected target | Restricts targeting to the selected ship target via `R`; for AI ships, uses the ShipAI maneuver target. | - | Charge-based weapons that should not waste shots against secondary targets. | Usually not |
| `Range<N%` | Anything | - | None | Base AI target out of range | Limits targeting/firing to `N%` of base weapon range. Predicted, not actual, locations are used. | - | Slow projectiles or shotgun-style weapons, e.g. Devastator Cannon. | Sometimes |
| `Overloaded` | Overloaded ships | Overloaded ships | None | Base AI targets non-overloaded | Only targets and fires at overloaded ships. | - | Finisher-type weapons. | No |
| `PrioFighter` | Anything | Fighters | None | If base AI does not target fighters | Prioritizes fighters over other targets when fighters are present. | - | Dual-purpose weapons that should lean toward fighter defense. | Yes |
| `PrioMissile` | Anything | Missiles | None | If base AI does not target missiles | Prioritizes missiles over other targets when missiles are present. | - | Dual-purpose PD that should lean toward missile defense. | Yes |
| `PrioShip` | Anything | Non-fighter ships | None | If base AI does not target ships | Prioritizes non-fighter ships over other targets when ships are present. | - | General weapons that should avoid being distracted by fighters. | Sometimes |
| `PrioWounded` | Anything | Targets with hull damage | None | Always | Prioritizes targets that have already taken hull damage. | - | Finishers focus damaged ships. | Sometimes |
| `PrioHealthy` | Anything | High-hull targets | None | Always | Prioritizes targets with high hull. | - | Opening-volley weapons prefer fresh targets. | Sometimes |
| `PrioDense` | Anything | Target-rich areas | None | Always | Prioritizes targets that are large and/or have many other targets nearby. | - | AoE weapons prefer clustered fighters or ships. | Sometimes |
| `Merge` | N/A | Special | None | N/A | Lets the player merge all tagged weapon groups into the active group with the merge hotkey, then press again to cancel. | - | Main-battery type weapons. | No |
| `SyncWindow` | Same as base AI | Shared release target | Same weapon group | Always | Tagged weapons in the same group wait until every tagged weapon is ready and on target, then fire together in synchronized windows. | Review with other sync tags | Similar weapons that should fire in coordinated windows without interrupting natural bursts. | Sometimes |
| `SyncVolley` | Same as base AI | Shared release target | Same weapon group | Always | Tagged weapons in the same group synchronize one firing decision, then wait to sync again. Intrinsic bursts and beams finish naturally. | Review with other sync tags | Alpha-strike weapons begin a volley together. | Sometimes |
| `Ambush` | Same as base AI | Shared ambush target | Same weapon group | Always | Tagged weapons wait until every tagged weapon is ready and on the same target, then open fire together. | Review with other sync tags | Burst weapons hold fire until an ambush target is ready. | Sometimes |
| `AvoidDebris` | - | - | None | No | Prevents firing when the shot is blocked by debris or asteroids. Only affects custom AI; Opportunist already includes this behavior. | - | Limited-ammo weapons or very high-flux weapons. | No |
| `BlockBeams` | Anything | Enemies shooting this ship with beams | None | Always | Shoots at enemies that are shooting this ship with beams, even when out of range. Intended mainly for the SVC Ink Spitter gun. | - | Weapons that block beams, such as the Ink Spitter. | Only for very specific weapons |
| `CustomAI` | Anything | Same as custom AI | None | Always | Prevents vanilla weapon AI from acting; intended for specific custom-AI setups. | - | Special cases where vanilla targeting is undesirable. | No |
| `LowRoF(N%)` | Anything | Same as base AI | None | Always | Reduces the weapon's rate of fire by the configured factor. | - | Weapons that should deliberately fire less often to conserve flux or ammo. | No |
## Settings ##

The settings allow you to configure many aspects of the mod.

Simply open the file ***Settings.editme*** (located in the folder of this mod) in a text editor of your choice 
and modify the lines marked with <---- EDIT HERE ----

Please be careful to adhere to the syntax and allowed values. If your settings file contains errors, the mod will use
the default settings instead! Make sure to check the log (Starsector/starsector.log) if your settings don't apply!

### Enable Custom AI ###

There are three different AI settings:

- If the custom AI is **disabled**, the weapon will use the baseAI to acquire a target. If the target doesn't match
  the tags, the weapon won't fire. (base AI)
- (default) If the custom AI is **enabled**, the weapon will first try the base AI. If the target doesn't 
  match the selected tags, the custom AI will take over. (custom AI)
- If you **force and enable** the custom AI, the weapon will immediately try to acquire a target via custom AI. (override AI)

You should **disable** the custom AI, if:

- You want an experience that is as close to vanilla Starsector as possible
- You absolutely hate it when your weapons occasionally fire at weird stuff (as my algorithm is still undergoing development, though mostly complete)

You should **enable or force-enable** the custom AI, if:

- You want to set weapons to prioritize targets they normally wouldn't (e.g. phase lances as anti-fighter weapons)
- You dislike it when your weapons don't fire even if there is a reasonable target
- You want to be able to customize the AI behaviour (in Settings.editme)
- You want to use advanced tags (Opportunist etc.)
- You want to get the "full experience"
- You want to help me improve my custom AI by sending me written reports/video snippets of glitchy weapon behaviour

### Performance Considerations ###

This mod will have a negative effect on performance. That effect will range from barely noticeable to considerable,
depending on the settings. On my machine (which is ~9 years old), the mod generally doesn't have a noticeable impact unless
I go crazy in the settings. Below I will list a few options for improving performance:

- Either force customAI, or disable it (as this prevents the occasional computation of two firing solutions).
- Try not to set every weapon group for every ship to a special fire mode.
- Leave the AI recursion level and friendly fire complexity at 1.
- Stick to ship mode Default (unfortunately, the ship mode implementation is a little hacky and performance intensive)

## How does the mod work? ##

In Starsector, each Weapon has a so-called AutofireAIPlugin. When that weapon is on autofire, this plugin will make the
decision where the weapon should aim and whether it should fire or not.

When you first set tags for a weapon, this mod will extract the original AutofireAIPlugin (AKA the base AI)
from the weapon and store it in a new Plugin called the TagBasedAIPlugin. Then, the selected tags are added to that plugin.
In each frame, the TagBasedAIPlugin will consult all tags and make decisions based on the combined results.

The TagBasedAIPlugin also contains a reference to the base Plugin. Each time the plugin has to make
a decision, it first asks the base plugin what it would like to do. If that behaviour is in line with the selected tags,
the plugin will simply let the base AI do its thing. Otherwise, depending on whether customAI is enabled or not, it will
tell the weapon to not fire, or try to come up with its own firing solution.

If the tag list for a weapon group is empty, the base AI will not be replaced.

Similarly, when setting ship AI modes, the mod will replace the base ship AI plugin with a custom plugin that will perform
some actions and then let the base AI take back over.

### Compatibility and Integration with other mods ###

Note: If you are unsure how anything in this section is supposed to work and need help, 
please feel free to DM me on Discord (Jannes on the unofficial Starsector Discord).

This mod should be compatible with other mods that provide custom AIs for their weapons, as long as they don't try to
manipulate the weapon AI mid-combat. This mod will simply use the custom AI of that weapon as the base AI.
This mod doesn't affect anything outside of combat, so it's very unlikely to cause problems on the campaign level.

#### Weapon Blocklist ####

If you are a mod-author and want to explicitly tell my mod to not tweak the AI of your weapon(s), include the weapon id
into your mod's modSettings.json:

```json
{
  "AdvancedGunneryControl": {
    "weaponBlacklist": [
      "weapon_id_1", "weapon_id_2"
    ]
  }
}
```

#### Suggested Tags ####

Similarly, this mod has a feature for suggested tags for weapons. If you want to include suggested tags for your
weapons to allow users to quickly or automatically set up their tags, include a key suggestedWeaponTags in your modSettings.json.
Have a look at this mod's modSettings.json for an example. Refer to the tag table above to decide on tags.
If in doubt, the safe bet is always to simply omit a weapon and assign no tags.

A simple modSettings.json that only includes suggested tags would look like this:

```json
{
  "AdvancedGunneryControl": {
    "suggestedWeaponTags" : {
            "my_awesome_pd_also_weapon_id": "PrioSmall,PrioMissile",
      "my_needler_style_weapon_id": "TargetShield"
    }
  }
}
```

#### Assigning tags to enemy ship weapons ####

If you want enemy ships of your mod to have tags assigned to their weapons, you can tell my mod to do so by setting
custom data to the ship. You can do that however you want to, the easiest solutions probably being a hullmod-script
or a BaseEveryFrameCombatPlugin/BaseEveryFrameCombatScript.

Use the setCustomData-method of the ShipAPI. Use the key "AGC_ApplyCustomOptions" and a Map<String, List<String>> as the value.
My mod will parse that value, apply the desired tags to the applicable weapons (Note: Tags are applied on a per-weapon basis
rather than on a per-weapon-group basis for enemy ships) and then remove the entry from the custom ship data.
After it's finished, it will write the key "AGC_CustomOptionsHaveBeenApplied" to the custom data, so you can search
for that key to see if tags have already been applied to a ship (though my mod doesn't check that key).

The map must adhere to the following syntax:

Its keys can be:
- a weapon id -> will affect all weapons with exactly this id
- "!MAGIC!Missile", "!MAGIC!Energy" or "!MAGIC!Ballistic" -> will affect all missiles/energy weapons/ballistics
- a regex-string -> will affect all weapons with ids that match the regex

The values must be lists of tag-names.
All tags listed will be applied to all weapons that match the given key.

For instance, if you want all missile weapons to receive the ForceAutoFire and NoFighter tags, the following call should get the job done:

```kotlin
// Kotlin
// assuming ship is an object of type ShipAPI
ship.setCustomData("AGC_ApplyCustomOptions", mapOf("!MAGIC!Missile" to listOf("ForceAutoFire", "NoFighter")))
```

```java
// Java
// assuming ship is an object of type ShipAPI
ship.setCustomData("AGC_ApplyCustomOptions", Collections.singletonMap("!MAGIC!Missile", Arrays.asList("ForceAutoFire", "NoFighter")));
```

If you want to apply this via Hullmod, the hullmod effect could look something like this:

```kotlin
// Kotlin
class ExampleTagSettingHullmod : BaseHullMod {
    // we only care to write to ship data in combat (after the ship is spawned), not before that or in the campaign
    override fun applyEffectsAfterShipCreation(ship: ShipAPI?, id: String?) {
        if(ship != null){// nullptr check just to be safe (I know, this is not the Kotlin-way, but easier to read for Java devs :P)           
            val tagMap = mapOf(
                                "!MAGIC!Missile" to listOf("ForceAutoFire", "NoFighter") // set all missiles to ForceAutoFire and NoFighter
                , "hveldriver" to listOf("TargetShield") // set all hyper-velocity drivers to TargetShield
                , ".*flak" to listOf("PD") // all weapons with IDs ending in flak (vanilla: single + dual flak) to PD 
            )
            ship.setCustomData("AGC_ApplyCustomOptions", tagMap)
        }
        // apply other effects your hullmod might have below this, as usual
    }
    
    // override other methods as required, as per usual
    // [...]
}
```

If you want to check whether AGC has been installed/loaded by the user:
It will write the key "AGC_Present" to the CombatEngine custom data. Though there's usually no harm in simply setting the ship
custom data. If AGC is present, it will handle it, if not, it won't have any effect.

#### Assigning ship modes to enemy ships ####

You can also assign ship modes to enemy ships in a very similar fashion. Since ship modes are assigned on a per-ship basis
rather than a per-weapon basis, this is much simpler. Instead of writing a map to custom data, you simply need to write a
list of ship modes to assign.

Use the key "AGC_ApplyCustomShipModes" and a List<String> as the value. 
For instance, assigning the SpamSystem mode to an enemy ship could be achieved with the following code:

```kotlin
// Kotlin
ship.setCustomData("AGC_ApplyCustomShipModes", listOf("SpamSystem"))
```

```java
// Java
ship.setCustomData("AGC_ApplyCustomShipModes", Arrays.asList("SpamSystem");
```

Same as before, after finishing the mode application, "AGC_CustomOptionsHaveBeenApplied" will be written to the ship's
custom data and the original entry will be removed.

#### Block AGC from assigning ship modes ####

Ship modes (and the ForceAF tag) replace the default ship AI of ships. The replacement AI delegates most calls 
to the original AI, so in most cases, this shouldn't pose a problem. If you, however, replace the ship AI with your 
own custom AI and need to access that AI from another script, that will cause problems.
You can circumvent that by simply storing a reference to your custom AI somewhere (e.g. in custom ship data).
However, if whatever you are trying to do is truly incompatible with AGC tinkering with the ship AI, you can set the
following key in the ships custom data: AGC_doNotReplaceShipAI
If that key is set, AGC will not replace the ship AI, essentially disabling ship modes for that ship for as long as 
that key is set.

```kotlin
ship.setCustomData("AGC_doNotReplaceShipAI", true)
```

#### Using the combat gui library ####

Has been migrated to [MagicLib](https://magiclibstarsector.github.io/MagicLib/root/org.magiclib.combatgui/index.html)

## Known Issues ##

- Versions before 0.8.2 saved custom classes as persistent data, meaning it was not possible to remove the mod.

## Changelog ## 

Has been moved to changelog.txt

## Acknowledgements ##

Special thanks to Seanra for contributing the option to ignore fighter shields and the legacy shield-threshold tags plus PrioPD
and ConservePDAmmo tags!

Special thanks to Genir for fixing an issue where weapons that should be aimed were incorrectly assumed to be non-aimable.
And many more thanks to Genir for his efforts of refactoring, improving code quality and performance!

Many thanks to Wisp(borne) for answering my endless questions about Kotlin and Starsector modding and for providing
an awesome repository template.

Thanks to LazyWizard for providing the LazyLib.

Thanks to stormbringer951 for inspiring me to create this mod by creating his mod Weapons Group Controls.

Last but not least: Thanks to everyone using this mod and giving me feedback!

## Support me ##

If you'd like to review my code and give me some hints what I could improve, please do! Also, feel free to create PRs!

If you happen to know how to make good videos, I'd very much appreciate if you could make a nice video showcasing the features
of this mod. If you came here from the Starsector mod forum, you know why I'm asking for this xD

As you might know, writing the code is the easy part. Making sure that it works properly is where the challenge lies.
I'm grateful for any help with testing this mod.

Do you have an idea for a cool new tag? Please feel free to contribute them!

On the off-chance that you want to support me financially, please don't :P 
