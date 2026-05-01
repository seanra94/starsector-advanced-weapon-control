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
will only fire if **all** tags allow firing, so if you e.g. set a weapon group to Hold(TF>50%) and Fighter, it will
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

Note: Not all tags are enabled by default; check `Settings.editme` or LunaSettings to customize which tags are available.
If you have ConsoleCommands installed, you can hotload new tags via `AGC_addTags`.

This catalogue is split into smaller tables so it stays readable in both rendered Markdown and raw text.

Notation:
- `N%` means replace `N` with a percentage value, usually from `1` to `99`.
- `TF`, `SF`, and `HF` mean total flux, soft flux, and hard flux.
- `A<N%` means ammo below `N%`.
- `H<N` means estimated target health/durability below raw value `N`.
- `Waste>N%` means estimated wasted attack-packet damage above `N%`.
- "Ship" means a non-fighter ship.
- Tags only affect weapon groups set to autofire.
- A weapon group fires only when all of its tags allow firing.

Important aliases are still accepted for saved-loadout compatibility:
- `Hold(...)` aliases normalize to `HoldFire(...)`.
- `ForceAF` normalizes to `ForceAutoFire`.
- `PrioPD`, `PrioritisePD`, and `PrioritizePD` normalize to `PrioSmall`.
- `BigShip` / `BigShips` normalize to `TargetBig`.
- `SmallShip` / `SmallShips` normalize to `TargetSmall`.
- `ConserveAmmo(A<N%)` normalizes to `Opportunist(A<N%)`.
- `ConservePDAmmo(A<N%)` and `CnsrvPDAmmo(A<N%)` normalize to `PD(A<N%)`.
- `IgnoreMinorPD` and `IgnoreMinorPD(H<N>)` normalize to `NoPD(H<N>)`.
- Legacy `BurstPD...` soft-flux forms normalize to canonical `PD(SF>N%)` forms.

### Core targeting tags ###

| Tag | Effect | Example use | Suggested? |
|---|---|---|---|
| `PD` | Restricts targeting to fighters and missiles. | Dedicated PD Lasers, Vulcans, or flak that should never shoot ships. | Yes |
| `Fighter` | Restricts targeting to fighters. | Anti-fighter guns that should ignore missiles and ships. | Usually no |
| `NoFighter` | Prevents targeting fighters. | Slow, low-rate weapons that waste shots on fighters. | Yes |
| `NoMissile` | Prevents targeting missiles. | Weapons that should not snap to low-value missile targets. | Sometimes |
| `NoPD` | Prevents missile targeting and prioritizes ships over fighters. | Machine guns with a PD weapon hint that should mainly pressure ships. | No |
| `NoPD(Waste>N%)` | Avoids fighters/missiles when this weapon would waste more than `N%` of estimated attack-packet damage. Low-damage cleanup weapons are exempt. | Phase Lance ignores a nearly-dead fighter that a PD Laser can finish, but still shoots healthy fighters. | Usually no |
| `NoPD(H<N)` | Avoids fighters/missiles below estimated health `N`; poor damage matchups count armor/shields as tougher. | Medium weapons ignore very fragile fighters but still shoot durable bombers. | Usually no |
| `TargetBig` | Restricts targeting to larger ships and prioritizes the largest valid targets. | Heavy, slow projectiles focus cruisers/capitals over frigates. | No |
| `TargetSmall` | Restricts targeting to smaller ships and prioritizes the smallest valid targets. | Accurate weapons clean up frigates and destroyers. | No |
| `ShipTarget` | Restricts targeting to the selected ship target. For AI ships, uses the ShipAI maneuver target. | Charge weapons follow the target selected with `R`. | Usually no |
| `Range<N%` | Only targets/fires at targets within `N%` of weapon range. | Devastator-style or slow projectile weapons wait for close targets. | Sometimes |
| `Overloaded` | Restricts targeting to overloaded ships. | Finisher weapons wait for overload openings. | No |

### Priority tags ###

| Tag | Effect | Example use | Suggested? |
|---|---|---|---|
| `PrioSmall` | Prioritizes missiles, fighters, and smaller ships over larger ships. | Fast tracking guns prefer missiles/fighters before larger ships. | Yes |
| `PrioBig` | Prioritizes larger ships over smaller ships without adding targeting restrictions. | Large weapons prefer cruisers/capitals while keeping fallback targets. | Sometimes |
| `PrioFighter` | Prioritizes fighters when fighters are present. | Dual-purpose weapons lean toward fighter defense. | Yes |
| `PrioMissile` | Prioritizes missiles when missiles are present. | Dual-purpose PD leans toward missile defense. | Yes |
| `PrioShip` | Prioritizes non-fighter ships when ships are present. | General weapons avoid being distracted by fighters. | Sometimes |
| `PrioWounded` | Prioritizes targets that have already taken hull damage. | Finishers focus damaged ships. | Sometimes |
| `PrioHealthy` | Prioritizes targets with high hull. | Opening-volley weapons prefer fresh targets. | Sometimes |
| `PrioDense` | Prioritizes target-rich areas. | AoE weapons prefer clustered fighters or ships. | Sometimes |

### Shield, armor, and phase tags ###

| Tag | Effect | Example use | Suggested? |
|---|---|---|---|
| `AvoidShield` | Prioritizes targets without useful shields, flanked shields, shield-off state, or high flux. | HE or frag weapons avoid strong shields. | Yes |
| `AvoidShield+` | Stricter AvoidShield variant. | Frag weapons fire only into weak shield situations. | Sometimes |
| `TargetShield` | Prioritizes shielded, low-flux targets. | Kinetic weapons such as needlers pressure shields. | Yes |
| `TargetShield+` | More aggressive TargetShield variant. | Kinetics keep firing unless shields are disabled or flanked. | Sometimes |
| `AvoidShield(TF>N%)` | AvoidShield behavior activates while own total flux is above `N%`. | HE weapons become selective when the ship is fluxed. | Sometimes |
| `AvoidShield(SF>N%)` | AvoidShield behavior activates while soft flux is above `N%` and total flux is below the soft-flux cap. | Soft-flux management for shield-inefficient shots. | Sometimes |
| `AvoidShield(HF>N%)` | AvoidShield behavior activates while hard flux is above `N%`. | Weapons become selective after taking hard flux. | Sometimes |
| `TargetShield(TF>N%)` | TargetShield behavior activates while own total flux is above `N%`. | Kinetics keep pressure while the ship is already committed. | Sometimes |
| `TargetShield(SF>N%)` | TargetShield behavior activates while soft flux is above `N%` and total flux is below the soft-flux cap. | Kinetics help convert soft-flux pressure into shield pressure. | Sometimes |
| `TargetShield(HF>N%)` | TargetShield behavior activates while hard flux is above `N%`. | Kinetics prioritize shield pressure after taking hard flux. | Sometimes |
| `ShieldOff` | Only fires at targets with no active shield. | EMP or HE weapons wait for shield-down openings. | No |
| `AvoidArmor(N%)` | Fires when the shot should hit shields or low enough armor to reach at least `N%` armor effectiveness. | Frag weapons avoid heavy armor unless shields are available. | Yes |
| `AvoidPhased` | Avoids phase ships that are likely to phase before the shot lands. | High-impact shots avoid wasting fire into active phase defense. | Usually no |
| `TargetPhase` | Pressures phase ships whether currently phased or not. | Beams or rapid-fire weapons keep phase coils under pressure. | No |

### Flux, ammo, and firing-condition tags ###

| Tag | Effect | Example use | Suggested? |
|---|---|---|---|
| `PD(TF>N%)` | Restricts targeting to fighters/missiles while total flux is above `N%`. | Flux-hungry PD saves flux until defense is needed. | No |
| `PD(SF>N%)` | Restricts targeting to fighters/missiles while soft flux is above `N%` and total flux is below the soft-flux cap. | Burst PD behavior when soft flux starts building. | No |
| `PD(HF>N%)` | Restricts targeting to fighters/missiles while hard flux is above `N%`. | Defensive behavior after absorbing hard flux. | No |
| `Opportunist` | Avoids fighters/missiles and fires only at likely effective shots. | Limited-ammo or long-cooldown weapons wait for good shots. | Yes |
| `Opportunist(A<N%)` | Opportunist behavior only while ammo is below `N%`; weapons without ammo are unaffected. | Autocannons with ammo charges become selective late. | No |
| `PD(A<N%)` | Restricts targeting to fighters/missiles while ammo is below `N%`; weapons without ammo and missile weapons are unaffected. | Limited-ammo PD weapon saves remaining ammo for defense. | Usually no |
| `HoldFire(TF>N%)` | Stops firing while own total flux is above `N%`. | High-flux weapons stop before overloading the ship. | Very |
| `HoldFire(SF>N%)` | Stops firing while own soft flux is above `N%` or total flux reaches the soft-flux safety cap. | Weapons ease off once soft flux stacks up. | Sometimes |
| `HoldFire(HF>N%)` | Stops firing while own hard flux is above `N%`. | Weapons stop after taking dangerous hard flux. | Sometimes |
| `ForceAutoFire` | Forces AI-controlled ships to keep this weapon group on autofire; modifies ShipAI. | AI-hesitant guns are kept on autofire, usually with HoldFire. | Usually no |
| `Force(TF<N%)` | Ignores firing restrictions, but not targeting restrictions, while total flux is below `N%`. | Weapons fire more freely while flux is low. | No |
| `Force(SF<N%)` | Ignores firing restrictions, but not targeting restrictions, while soft flux is below `N%` and total flux is below the soft-flux cap. | Weapons fire freely while soft flux is low. | No |
| `Force(HF<N%)` | Ignores firing restrictions, but not targeting restrictions, while hard flux is below `N%`. | Weapons fire freely until hard flux rises. | No |
| `Panic(H<N%)` | Fires blindly while own hull is below `N%`; AI ships also force autofire for the group. | Tracking missiles dump ammo before the ship dies. | Usually no |
| `LowRoF(N%)` | Reduces rate of fire by the configured factor. | Weapons deliberately fire less often to conserve flux/ammo. | No |

### Utility and synchronization tags ###

| Tag | Effect | Example use | Suggested? |
|---|---|---|---|
| `Merge` | Lets the player merge tagged weapon groups into the active group with the merge hotkey. | Main-battery weapons can be fired manually together. | No |
| `SyncWindow` | Tagged weapons in the same group wait for a shared firing window, then fire together. | Similar weapons synchronize volleys without interrupting natural bursts. | Sometimes |
| `SyncVolley` | Tagged weapons in the same group synchronize one firing decision, then wait to sync again. | Alpha-strike weapons begin a volley together. | Sometimes |
| `Ambush` | Tagged weapons wait until every tagged weapon is ready on the same target, then open fire together. | Burst weapons hold fire until an ambush target is ready. | Sometimes |
| `AvoidDebris` | Prevents firing when debris or asteroids block the shot; only affects custom AI. | High-flux or limited-ammo weapons avoid blocked shots. | No |
| `BlockBeams` | Shoots enemies currently hitting this ship with beams, even when out of range. | Specialized beam-blocking weapons such as the Ink Spitter. | Only for specific weapons |
| `CustomAI` | Prevents vanilla AI from acting; intended only for specific custom-AI setups. | Devastator-style weapons avoid vanilla targeting quirks. | No |
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
