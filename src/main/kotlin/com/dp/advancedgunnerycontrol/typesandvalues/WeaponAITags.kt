package com.dp.advancedgunnerycontrol.typesandvalues

import com.dp.advancedgunnerycontrol.WeaponControlPlugin
import com.dp.advancedgunnerycontrol.gui.isElligibleForPD
import com.dp.advancedgunnerycontrol.gui.isEverythingBlacklisted
import com.dp.advancedgunnerycontrol.gui.usesAmmo
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.loadPersistentTags
import com.dp.advancedgunnerycontrol.utils.persistTags
import com.dp.advancedgunnerycontrol.weaponais.mapBooleanToSpecificString
import com.dp.advancedgunnerycontrol.weaponais.tags.*
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.lwjgl.input.Keyboard
import kotlin.math.roundToInt

// NOTE: Tag names should NOT exceed 13 characters to be able to cleanly fit on buttons!

val pdTags = listOf("PD", "NoPD", "PD(TF>N%)", "NoMissile")
val ammoTags = listOf("ConserveAmmo", "ConservePDAmmo")

private const val LEGACY_TOTAL_FLUX_TOKEN = "Fl?u?x?"

val holdTotalFluxRegex = Regex("Hold\\(TF>(\\d+)%\\)")
val holdTotalFluxLegacyRegex = Regex("Hold(?:FT)?\\($LEGACY_TOTAL_FLUX_TOKEN>(\\d+)%\\)")
val holdSoftFluxRegex = Regex("Hold\\(SF>(\\d+)%\\)")
val holdSoftFluxLegacyRegex = Regex("HoldSFT\\($LEGACY_TOTAL_FLUX_TOKEN>(\\d+)%\\)")
val forceFireTotalFluxRegex = Regex("Force\\(TF<(\\d+)%\\)")
val forceFireTotalFluxLegacyRegex = Regex("Force(?:F|FT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val forceFireSoftFluxRegex = Regex("Force\\(SF<(\\d+)%\\)")
val forceFireSoftFluxLegacyRegex = Regex("ForceSFT\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val avoidShieldTotalFluxRegex = Regex("AvoidShield\\(TF>(\\d+)%\\)")
val avoidShieldSoftFluxRegex = Regex("AvoidShield\\(SF>(\\d+)%\\)")
val avoidShieldTotalFluxLegacyRegex = Regex("(?:AvShldFT|AvdShieldsFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val avoidShieldSoftFluxLegacyRegex = Regex("(?:AvShldSFT|AvShlddSFT|AvdShieldsSFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val targetShieldTotalFluxRegex = Regex("TargetShield\\(TF>(\\d+)%\\)")
val targetShieldSoftFluxRegex = Regex("TargetShield\\(SF>(\\d+)%\\)")
val targetShieldTotalFluxLegacyRegex = Regex("(?:TgtShldFT|TgtShieldsFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val targetShieldSoftFluxLegacyRegex = Regex("(?:TgtShldSFT|TgtShieldsSFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val burstPDSoftFluxRegex = Regex("BurstPD\\(SF>(\\d+)%\\)")
val burstPDSoftFluxLegacyRegex = Regex("BurstPDSFT\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val pdTotalFluxRegex = Regex("PD\\(TF>(\\d+)%\\)")
val pdTotalFluxLegacyRegex = Regex("PD\\($LEGACY_TOTAL_FLUX_TOKEN>(\\d+)%\\)")
val avoidArmorRegex = Regex("(?:AvoidArmor|AvdArmor)\\((\\d+)%\\)")
val panicFireRegex = Regex("Panic\\(H<(\\d+)%\\)")
val rangeRegex = Regex("Range<(\\d+)%")
val rofRegex = Regex("LowRoF\\((\\d+)%\\)")

//val prioPdRegex = Regex("PrioP[dD]\\((\\d+)\\)")
//val prioFightersRegex = Regex("PrioFighter\\((\\d+)\\)")
//val prioMissilesRegex = Regex("PrioMissile\\((\\d+)\\)")

fun extractRegexThreshold(regex: Regex, name: String): Float {
    return (regex.matchEntire(name)?.groupValues?.get(1)?.toFloat() ?: 0f) / 100f
}

fun extractRegexThresholdAsPercentageString(regex: Regex, name: String): String {
    return "${(extractRegexThreshold(regex, name) * 100f).toInt()}%"
}

private fun invertThreshold(threshold: Float): Float = (1f - threshold).coerceIn(0f, 1f)

private fun invertedRegexThreshold(regex: Regex, name: String): Float = invertThreshold(extractRegexThreshold(regex, name))

private fun thresholdAsPercent(threshold: Float): Int = (threshold.coerceIn(0f, 1f) * 100f).roundToInt()

private fun invertedRegexThresholdAsPercentageString(regex: Regex, name: String): String {
    return "${thresholdAsPercent(invertThreshold(extractRegexThreshold(regex, name)))}%"
}

private fun settingAsActivationThreshold(settingValue: Float): String {
    return "${thresholdAsPercent(invertThreshold(settingValue))}%"
}

fun shouldTagBeDisabled(groupIndex: Int, sh: FleetMemberAPI, tag: String): Boolean {
    val modTag = tagNameToRegexName(tag)
    if (pdTags.contains(modTag) && !isElligibleForPD(groupIndex, sh)) return true
    if (ammoTags.contains(modTag) && !usesAmmo(groupIndex, sh)) return true
    return isEverythingBlacklisted(groupIndex, sh)
}

val priorityBoilerplateText = "\nIncreases priority by a factor of ${Settings.prioXModifier()} (adjustable in Settigs.editme)." +
        "\nCombine multiple Prio-tags to de-prioritize everything else."

val tagTooltips = mapOf(
    "PD" to "Restricts targeting to fighters and missiles.",
    "PrioPD" to "Weapon will always prioritize from small to large (Missiles > fighters > small ships > big ships)." +
            if (Settings.strictBigSmall()) "\nRestricts targeting to missiles and ships smaller than cruisers." else "\nNo targeting restrictions.",
    "NoPD" to "Forbids targeting missiles and prioritizes ships over fighters.",
    "Fighter" to "Restricts targeting to fighters.",
    "AvoidShield" to "Weapon will prioritize targets without shields, flanked shields or high flux/shield off. \nShields of fighters will ${
        mapBooleanToSpecificString(
            Settings.ignoreFighterShield(),
            "",
            "not"
        )
    } be ignored (configurable in settings)" +
            "\nNo targeting restrictions.",
    "TargetShield" to "Weapon will prioritize shooting shields. Will stop firing against enemies with very high flux. \nShields of fighters will ${
        mapBooleanToSpecificString(
            Settings.ignoreFighterShield(),
            "",
            "not"
        )
    } be ignored (configurable in settings)" +
            "\nTip: Keep one kinetic weapon on default to keep up pressure." +
            "\nNo targeting restrictions.",
    "TargetShield+" to "As TargetShield, but more aggressive." +
            "\nWill only stop shooting when flanking shields or shields are disabled." +
            "\nShields of fighters will ${
        mapBooleanToSpecificString(
            Settings.ignoreFighterShield(),
            "",
            "not"
        )
    } be ignored (configurable in settings)",
    "AvoidShield+" to "As AvoidShield, but less aggressive." +
            "\nWill only shoot when flanking shields or shields are disabled." +
            "\nShields of fighters will ${
        mapBooleanToSpecificString(
            Settings.ignoreFighterShield(),
            "",
            "not"
        )
    } be ignored (configurable in settings)",
    "NoFighter" to "Forbids targeting fighters.",
    "ConserveAmmo" to "Weapon will be much more hesitant to fire when ammo below ${(Settings.conserveAmmo() * 100f).toInt()}%." +
            "\nNo targeting restrictions.",
    "ConservePDAmmo" to "When ammo is below ${(Settings.conservePDAmmo() * 100f).toInt()}%, weapon will only fire when the target is a fighter/missile." +
            "\nFor non-PD weapons, only fighters will be fired upon in that case." +
            "\nNo targeting restrictions.",
    "Opportunist" to "Weapon will be much more hesitant to fire and won't target missiles or fighters. Use for e.g. limited ammo weapons.",
    "AvoidDebris" to "Weapon will not fire when the shot is blocked by debris/asteroids." +
            "\nNote: This only affects the custom AI and the Opportunist mode already includes this option.",
    "BigShip" to "Weapon won't target missiles and prioritize big ships" +
            if (Settings.strictBigSmall()) " and won't target anything smaller than destroyers." else ".",
    "SmallShip" to "Weapon will ignore missiles and prioritize small ships (including fighters)" +
            if (Settings.strictBigSmall()) " and won't target anything bigger than destroyers." else ".",
    "ForceAF" to "Will force AI-controlled ships to set this group to autofire, like the ForceAF ship mode does to all groups." +
            "\nNote: This will modify the ShipAI, as the Starsector API doesn't allow to directly set a weapon group to autofire." +
            "\n      The ShipAI might still try to select this weapon group, but will be forced to deselect it again.",
    "AvoidPhased" to "Weapon will ignore phase-ships, unless they are unable to avoid the shot by phasing (due to flux or cooldown)." +
            "\nWhen used to fight phase-ships, it's best to use this on high-impact weapons and set some rapid-fire or beam weapons on TargetPhase." +
            "\nNo targeting restrictions.",
    "TargetPhase" to "Weapon will prioritize phase-ships. Does not care if the ship is currently phased or not." +
            "\nUseful for rapid-fire or beam weapons to keep up pressure on enemy phase coils." +
            "\nNo targeting restrictions.",
    "ShipTarget" to "Weapon will only target the selected ship target (R-Key). I like to use this for regenerating missiles." +
            "\nFor AI-controlled ships, this will limit them to the maneuver-target that the ShipAI has chosen.",
    "NoMissile" to "Weapon won't target missiles.",
    "Overloaded" to "Weapon will only target and fire at overloaded ships.",
    "ShieldOff" to "Simplified version of AvoidShield. Will only fire at targets that have no shields or have shields turned off.",
    "Merge" to "Press [${Keyboard.getKeyName(Settings.mergeHotkey())}] to merge all weapons with this tag into current weapon group. " +
            "\nFor player controlled ship only! Press [${Keyboard.getKeyName(Settings.mergeHotkey())}] again to undo." +
            "\nUse this tag to unleash big manually aimed barrages at your enemies!",
    "SyncWindow" to "Synchronizes tagged weapons in the same weapon group into firing windows. " +
            "All tagged weapons wait until every tagged weapon is ready and on target, then fire together. " +
            "Fast weapons may keep firing until the longest-burst tagged weapon finishes, then the group waits to sync again.",
    "SyncVolley" to "Synchronizes tagged weapons in the same weapon group for one firing decision. " +
            "All tagged weapons wait until every tagged weapon is ready and on target, begin firing together, then wait to sync again. " +
            "Intrinsic weapon bursts and beams are allowed to finish naturally.",
    "Ambush" to "Tagged weapons in the same weapon group wait until every tagged weapon is ready and on the same target, then open fire together. " +
            "After the ambush starts, weapons keep prioritizing that target, but weapons that can no longer bear on it may fire at other valid targets. " +
            "The ambush resets when the target is lost by the whole group.",
    "IgnoreMinorPD" to "Ignores very low-health missiles and fighters. Useful on PD weapons that should not waste shots on tiny threats.",
    "PrioFighter" to "Prioritize fighters over all other targets but target other things if no fighters present.$priorityBoilerplateText",
    "PrioMissile" to "Prioritize missiles over all other targets but target other things if no missiles present.$priorityBoilerplateText",
    "PrioShip" to "Prioritize non-fighter ships over all other targets but target other things if no ships present.$priorityBoilerplateText",
    "PrioWounded" to "Prioritize targets that have already taken lots of hull damage.",
    "PrioHealthy" to "Prioritize targets that have high hull level",
    "BlockBeams" to "Will shoot at enemies that are shooting this ship with beams, even when out of range. Intended mainly for the SVC Ink Spitter gun.",
    "CustomAI" to "This tag does nothing but prevent the vanilla AI from doing anything. I use this for devastators to prevent vanilla jank.",
    "PrioDense" to "Prioritize target rich areas. Weapon will prioritize shooting at targets that are big and/or have lots of other targets nearby. Good for AoE weapons."
)

fun canonicalizeWeaponTagName(tag: String): String {
    return when {
        holdTotalFluxRegex.matches(tag) -> tag
        holdTotalFluxLegacyRegex.matches(tag) -> "Hold(TF>${extractRegexThresholdAsPercentageString(holdTotalFluxLegacyRegex, tag)})"
        holdSoftFluxRegex.matches(tag) -> tag
        holdSoftFluxLegacyRegex.matches(tag) -> "Hold(SF>${extractRegexThresholdAsPercentageString(holdSoftFluxLegacyRegex, tag)})"
        forceFireTotalFluxRegex.matches(tag) -> tag
        forceFireTotalFluxLegacyRegex.matches(tag) -> "Force(TF<${extractRegexThresholdAsPercentageString(forceFireTotalFluxLegacyRegex, tag)})"
        forceFireSoftFluxRegex.matches(tag) -> tag
        forceFireSoftFluxLegacyRegex.matches(tag) -> "Force(SF<${extractRegexThresholdAsPercentageString(forceFireSoftFluxLegacyRegex, tag)})"
        avoidShieldTotalFluxRegex.matches(tag) -> tag
        avoidShieldTotalFluxLegacyRegex.matches(tag) -> "AvoidShield(TF>${invertedRegexThresholdAsPercentageString(avoidShieldTotalFluxLegacyRegex, tag)})"
        avoidShieldSoftFluxRegex.matches(tag) -> tag
        avoidShieldSoftFluxLegacyRegex.matches(tag) -> "AvoidShield(SF>${invertedRegexThresholdAsPercentageString(avoidShieldSoftFluxLegacyRegex, tag)})"
        targetShieldTotalFluxRegex.matches(tag) -> tag
        targetShieldTotalFluxLegacyRegex.matches(tag) -> "TargetShield(TF>${invertedRegexThresholdAsPercentageString(targetShieldTotalFluxLegacyRegex, tag)})"
        targetShieldSoftFluxRegex.matches(tag) -> tag
        targetShieldSoftFluxLegacyRegex.matches(tag) -> "TargetShield(SF>${invertedRegexThresholdAsPercentageString(targetShieldSoftFluxLegacyRegex, tag)})"
        burstPDSoftFluxRegex.matches(tag) -> tag
        burstPDSoftFluxLegacyRegex.matches(tag) -> "BurstPD(SF>${invertedRegexThresholdAsPercentageString(burstPDSoftFluxLegacyRegex, tag)})"
        pdTotalFluxRegex.matches(tag) -> tag
        pdTotalFluxLegacyRegex.matches(tag) -> "PD(TF>${extractRegexThresholdAsPercentageString(pdTotalFluxLegacyRegex, tag)})"
        avoidArmorRegex.matches(tag) -> "AvoidArmor(${extractRegexThresholdAsPercentageString(avoidArmorRegex, tag)})"
        tag == "PrioritisePD" -> "PrioPD"
        tag == "PrioritizePD" -> "PrioPD"
        tag == "CnsrvPDAmmo" -> "ConservePDAmmo"
        tag == "NoFighters" -> "NoFighter"
        tag == "NoMissiles" -> "NoMissile"
        tag == "BigShips" -> "BigShip"
        tag == "SmallShips" -> "SmallShip"
        tag == "PrioShips" -> "PrioShip"
        tag == "AvoidShields" -> "AvoidShield"
        tag == "TargetShields" -> "TargetShield"
        tag == "AvdShields+" -> "AvoidShield+"
        tag == "TgtShields+" -> "TargetShield+"
        tag == "AvdShieldsFT" -> "AvoidShield(TF>${settingAsActivationThreshold(Settings.avoidShieldAtTotalFlux())})"
        tag == "TgtShieldsFT" -> "TargetShield(TF>${settingAsActivationThreshold(Settings.targetShieldAtTotalFlux())})"
        tag == "ShieldsOff" -> "ShieldOff"
        else -> tag
    }
}

fun canonicalizeWeaponTagNames(tags: List<String>): List<String> = tags.map { canonicalizeWeaponTagName(it) }.distinct()

fun getTagTooltip(tag: String): String {
    val canonicalTag = canonicalizeWeaponTagName(tag)
    if (tagTooltips.containsKey(canonicalTag)) {
        return tagTooltips[canonicalTag] ?: "No description available."
    }
    return when {
        holdTotalFluxRegex.matches(canonicalTag) -> "Weapon will stop firing if ship total flux exceeds ${
            extractRegexThresholdAsPercentageString(
                holdTotalFluxRegex,
                canonicalTag
            )
        }."

        holdSoftFluxRegex.matches(canonicalTag) -> "Weapon will stop firing if ship soft flux exceeds ${
            extractRegexThresholdAsPercentageString(
                holdSoftFluxRegex,
                canonicalTag
            )
        } or total flux reaches ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%."

        forceFireTotalFluxRegex.matches(canonicalTag) -> "ForceFire: Weapon will ignore firing restrictions of other tags while total flux is below ${
            extractRegexThresholdAsPercentageString(
                forceFireTotalFluxRegex,
                canonicalTag
            )
        }." +
                "\nNote: This will not circumvent targeting restrictions, only firing restrictions."

        forceFireSoftFluxRegex.matches(canonicalTag) -> "ForceFire: Weapon will ignore firing restrictions of other tags while soft flux is below ${
            extractRegexThresholdAsPercentageString(
                forceFireSoftFluxRegex,
                canonicalTag
            )
        } and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%." +
                "\nNote: This will not circumvent targeting restrictions, only firing restrictions."

        avoidShieldTotalFluxRegex.matches(canonicalTag) -> "As AvoidShield, but avoids shields while total flux is above ${
            extractRegexThresholdAsPercentageString(
                avoidShieldTotalFluxRegex,
                canonicalTag
            )
        }."

        avoidShieldSoftFluxRegex.matches(canonicalTag) -> "As AvoidShield, but avoids shields while soft flux is above ${
            extractRegexThresholdAsPercentageString(
                avoidShieldSoftFluxRegex,
                canonicalTag
            )
        } and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%."

        targetShieldTotalFluxRegex.matches(canonicalTag) -> "As TargetShield, but targets shields while total flux is above ${
            extractRegexThresholdAsPercentageString(
                targetShieldTotalFluxRegex,
                canonicalTag
            )
        }."

        targetShieldSoftFluxRegex.matches(canonicalTag) -> "As TargetShield, but targets shields while soft flux is above ${
            extractRegexThresholdAsPercentageString(
                targetShieldSoftFluxRegex,
                canonicalTag
            )
        } and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%."

        burstPDSoftFluxRegex.matches(canonicalTag) -> "Weapon acts as PD mode while soft flux is greater than ${
            extractRegexThresholdAsPercentageString(
                burstPDSoftFluxRegex,
                canonicalTag
            )
        } and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%."

        pdTotalFluxRegex.matches(canonicalTag) -> "Weapon will act as PD mode while ship total flux > ${
            extractRegexThresholdAsPercentageString(
                pdTotalFluxRegex,
                canonicalTag
            )
        }."

        avoidArmorRegex.matches(canonicalTag) -> "Weapon will fire when the shot is likely to hit shields (as TargetShield) OR a section of hull " +
                "\nwhere the armor is low enough to achieve at least ${
                    extractRegexThresholdAsPercentageString(
                        avoidArmorRegex,
                        canonicalTag
                    )
                } " +
                "effectiveness vs armor." +
                "\nCombine with AvoidShield to also avoid shields (e.g. for frag weapons)."

        panicFireRegex.matches(canonicalTag) -> "Weapon will blindly fire without considering if/what the shot will hit as long as the ship" +
                " hull level is below ${extractRegexThresholdAsPercentageString(panicFireRegex, canonicalTag)}." +
                "\nFor AI-controlled ships, this will put the weapon group into ForceAF-mode once the hull threshold has been reached."

        rangeRegex.matches(canonicalTag) -> "Weapon will only target and fire at targets if they are closer than ${
            extractRegexThresholdAsPercentageString(
                rangeRegex, canonicalTag
            )
        } of weapon range." +
                "\nThis is useful for weapons (especially missiles) with slow projectiles, such as e.g. sabots" +
                " or shotgun-style weapons, such as the devastator cannon." +
                "\nNote: This does not modify the actual range of the weapon, it only affects autofire behavior!"

        rofRegex.matches(canonicalTag) -> "Reduces the rate of fire of the weapon by a factor of ${
            extractRegexThresholdAsPercentageString(
                rofRegex, canonicalTag
            )
        }. E.g. LowRoF(200%) makes the weapon shot half as often."

        else -> "No description available."
    }
}

var unknownTagWarnCounter = 0
fun createTag(name: String, weapon: WeaponAPI): WeaponAITagBase? {
    when {
        holdTotalFluxRegex.matches(name) -> return HoldTotalFluxTag(weapon, extractRegexThreshold(holdTotalFluxRegex, name))
        holdTotalFluxLegacyRegex.matches(name) -> return HoldTotalFluxTag(weapon, extractRegexThreshold(holdTotalFluxLegacyRegex, name))
        holdSoftFluxRegex.matches(name) -> return HoldSoftFluxTag(weapon, extractRegexThreshold(holdSoftFluxRegex, name))
        holdSoftFluxLegacyRegex.matches(name) -> return HoldSoftFluxTag(weapon, extractRegexThreshold(holdSoftFluxLegacyRegex, name))
        forceFireTotalFluxRegex.matches(name) -> return ForceFireTotalFluxTag(weapon, extractRegexThreshold(forceFireTotalFluxRegex, name))
        forceFireTotalFluxLegacyRegex.matches(name) -> return ForceFireTotalFluxTag(weapon, extractRegexThreshold(forceFireTotalFluxLegacyRegex, name))
        forceFireSoftFluxRegex.matches(name) -> return ForceFireSoftFluxTag(weapon, extractRegexThreshold(forceFireSoftFluxRegex, name))
        forceFireSoftFluxLegacyRegex.matches(name) -> return ForceFireSoftFluxTag(weapon, extractRegexThreshold(forceFireSoftFluxLegacyRegex, name))
        avoidShieldTotalFluxRegex.matches(name) -> return AvoidShieldTotalFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldTotalFluxRegex, name)
        )
        avoidShieldTotalFluxLegacyRegex.matches(name) -> return AvoidShieldTotalFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldTotalFluxLegacyRegex, name)
        )
        avoidShieldSoftFluxRegex.matches(name) -> return AvoidShieldSoftFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldSoftFluxRegex, name)
        )
        avoidShieldSoftFluxLegacyRegex.matches(name) -> return AvoidShieldSoftFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldSoftFluxLegacyRegex, name)
        )
        targetShieldTotalFluxRegex.matches(name) -> return TargetShieldTotalFluxTag(
            weapon,
            extractRegexThreshold(targetShieldTotalFluxRegex, name)
        )
        targetShieldTotalFluxLegacyRegex.matches(name) -> return TargetShieldTotalFluxTag(
            weapon,
            extractRegexThreshold(targetShieldTotalFluxLegacyRegex, name)
        )
        targetShieldSoftFluxRegex.matches(name) -> return TargetShieldSoftFluxTag(
            weapon,
            extractRegexThreshold(targetShieldSoftFluxRegex, name)
        )
        targetShieldSoftFluxLegacyRegex.matches(name) -> return TargetShieldSoftFluxTag(
            weapon,
            extractRegexThreshold(targetShieldSoftFluxLegacyRegex, name)
        )
        burstPDSoftFluxRegex.matches(name) -> return BurstPDSoftFluxTag(weapon, extractRegexThreshold(burstPDSoftFluxRegex, name))
        burstPDSoftFluxLegacyRegex.matches(name) -> return BurstPDSoftFluxTag(weapon, extractRegexThreshold(burstPDSoftFluxLegacyRegex, name))
        pdTotalFluxRegex.matches(name) -> return PDAtTotalFluxTag(weapon, extractRegexThreshold(pdTotalFluxRegex, name))
        pdTotalFluxLegacyRegex.matches(name) -> return PDAtTotalFluxTag(weapon, extractRegexThreshold(pdTotalFluxLegacyRegex, name))
        avoidArmorRegex.matches(name) -> return AvoidArmorTag(weapon, extractRegexThreshold(avoidArmorRegex, name))
        panicFireRegex.matches(name) -> return PanicFireTag(weapon, extractRegexThreshold(panicFireRegex, name))
        rangeRegex.matches(name) -> return RangeTag(weapon, extractRegexThreshold(rangeRegex, name))
        rofRegex.matches(name) -> return ReduceRoFTag(weapon, extractRegexThreshold(rofRegex, name))
    }
    return when (name) {
        "PD" -> PDTag(weapon)
        "PrioPD", "PrioritizePD", "PrioritisePD" -> PrioritizePDTag(weapon, Settings.prioXModifier())
        "NoPD" -> NoPDTag(weapon)
        "Fighter" -> FighterTag(weapon)
        "AvoidShield", "AvoidShields" -> AvoidShieldTag(weapon)
        "TargetShield", "TargetShields" -> TargetShieldTag(weapon)
        "AvoidShield+", "AvdShields+" -> AvoidShieldTag(weapon, 0.02f)
        "TargetShield+", "TgtShields+" -> TargetShieldTag(weapon, 0.01f)
        "NoFighter", "NoFighters" -> NoFighterTag(weapon)
        "ConserveAmmo" -> ConserveAmmoTag(weapon, Settings.conserveAmmo())
        "ConservePDAmmo", "CnsrvPDAmmo" -> ConservePDAmmoTag(weapon, Settings.conservePDAmmo())
        "Opportunist" -> OpportunistTag(weapon)
        "AvoidDebris" -> AvoidDebrisTag(weapon)
        "BigShip", "BigShips" -> BigShipTag(weapon)
        "SmallShip", "SmallShips" -> SmallShipTag(weapon)
        "ForceAF" -> ForceAutofireTag(weapon)
        "AvoidPhased" -> AvoidPhaseTag(weapon)
        "TargetPhase" -> TargetPhaseTag(weapon)
        "ShipTarget" -> ShipTargetTag(weapon)
        "TgtShieldsFT" -> TargetShieldAtTotalFluxTag(weapon)
        "AvdShieldsFT" -> AvoidShieldAtTotalFluxTag(weapon)
        "NoMissile", "NoMissiles" -> NoMissileTag(weapon)
        "Overloaded" -> OverloadTag(weapon)
        "ShieldOff", "ShieldsOff" -> ShieldOffTag(weapon)
        "Merge" -> MergeTag(weapon)
        "SyncWindow" -> SynchronizedFireTag(weapon, SyncFireMode.WINDOW)
        "SyncVolley" -> SynchronizedFireTag(weapon, SyncFireMode.VOLLEY)
        "Ambush" -> SynchronizedFireTag(weapon, SyncFireMode.AMBUSH)
        "IgnoreMinorPD" -> IgnoreMinorPDTag(weapon)
        "PrioFighter" -> PrioritizeFightersTag(weapon, Settings.prioXModifier())
        "PrioMissile" -> PrioritizeMissilesTag(weapon, Settings.prioXModifier())
        "PrioShip", "PrioShips" -> PrioritizeShipTag(weapon, Settings.prioXModifier())
        "PrioWounded" -> PrioritizeWoundedTag(weapon)
        "PrioHealthy" -> PrioritizeHealthyTag(weapon)
        "BlockBeams" -> InterdictBeamsTag(weapon)
        "CustomAI" -> CustomAITag(weapon)
        "PrioDense" -> PrioritizeDense(weapon)
        else -> {
            unknownTagWarnCounter++
            when {
                unknownTagWarnCounter < 10 -> Global.getLogger(WeaponControlPlugin.Companion::class.java)
                    .warn("Unknown weapon tag: $name! Will be ignored.")

                unknownTagWarnCounter == 10 -> Global.getLogger(WeaponControlPlugin.Companion::class.java).warn(
                    "Unknown weapon tag: $name! Future warnings of this type will be skipped."
                )
            }
            null
        }
    }
}

fun tagNameToRegexName(tag: String): String {
    val canonicalTag = canonicalizeWeaponTagName(tag)
    return when {
        holdTotalFluxRegex.matches(canonicalTag) -> "Hold(TF>N%)"
        holdSoftFluxRegex.matches(canonicalTag) -> "Hold(SF>N%)"
        forceFireTotalFluxRegex.matches(canonicalTag) -> "Force(TF<N%)"
        forceFireSoftFluxRegex.matches(canonicalTag) -> "Force(SF<N%)"
        avoidShieldTotalFluxRegex.matches(canonicalTag) -> "AvoidShield(TF>N%)"
        avoidShieldSoftFluxRegex.matches(canonicalTag) -> "AvoidShield(SF>N%)"
        targetShieldTotalFluxRegex.matches(canonicalTag) -> "TargetShield(TF>N%)"
        targetShieldSoftFluxRegex.matches(canonicalTag) -> "TargetShield(SF>N%)"
        burstPDSoftFluxRegex.matches(canonicalTag) -> "BurstPD(SF>N%)"
        pdTotalFluxRegex.matches(canonicalTag) -> "PD(TF>N%)"
        avoidArmorRegex.matches(canonicalTag) -> "AvoidArmor"
        panicFireRegex.matches(canonicalTag) -> "Panic"
        rangeRegex.matches(canonicalTag) -> "Range"
        else -> canonicalTag
    }
}

private val shieldTargetingTags = listOf(
    "ShieldOff",
    "AvoidShield",
    "TargetShield",
    "TargetShield+",
    "AvoidShield+",
    "AvoidShield(TF>N%)",
    "AvoidShield(SF>N%)",
    "TargetShield(TF>N%)",
    "TargetShield(SF>N%)"
)

private fun shieldTagIncompatibilities(tag: String): List<String> {
    return shieldTargetingTags.filter { it != tag }
}

val tagIncompatibility = mapOf(
    "PD" to listOf(
        "Fighter",
        "Opportunist",
        "NoPD",
        "PD(TF>N%)",
        "BigShip",
        "SmallShip",
        "ConservePDAmmo",
        "PrioPD"
    ),
    "PrioPD" to listOf("Opportunist", "NoPD", "BigShip", "SmallShip", "Fighter", "PD"),
    "Fighter" to listOf(
        "PD",
        "NoFighter",
        "Opportunist",
        "NoPD",
        "PD(TF>N%)",
        "BigShip",
        "SmallShip",
        "PrioPD",
        "ConservePDAmmo",
    ),
    "NoPD" to listOf("PD", "Fighter", "PD(TF>N%)", "PrioPD", "ConservePDAmmo"),
    "ShieldOff" to shieldTagIncompatibilities("ShieldOff"),
    "AvoidShield" to shieldTagIncompatibilities("AvoidShield"),
    "TargetShield" to shieldTagIncompatibilities("TargetShield"),
    "TargetShield+" to shieldTagIncompatibilities("TargetShield+"),
    "AvoidShield+" to shieldTagIncompatibilities("AvoidShield+"),
    "AvoidShield(TF>N%)" to shieldTagIncompatibilities("AvoidShield(TF>N%)"),
    "AvoidShield(SF>N%)" to shieldTagIncompatibilities("AvoidShield(SF>N%)"),
    "TargetShield(TF>N%)" to shieldTagIncompatibilities("TargetShield(TF>N%)"),
    "TargetShield(SF>N%)" to shieldTagIncompatibilities("TargetShield(SF>N%)"),
    "NoFighter" to listOf("Fighter", "Opportunist"),
    "ConservePDAmmo" to listOf("PD", "Fighter", "NoPD"),
    "Opportunist" to listOf("Fighter", "PD", "NoFighter", "PD(TF>N%)", "PrioPD", "ConservePDAmmo", "NoMissile"),
    "PD(TF>N%)" to listOf("Fighter", "Opportunist", "NoPD", "PD", "BigShip", "SmallShip"),
    "SmallShip" to listOf("BigShip", "PD", "Fighter", "PD(TF>N%)", "PrioPD"),
    "BigShip" to listOf("SmallShip", "PD", "Fighter", "PD(TF>N%)", "PrioPD"),
    "NoMissile" to listOf("Opportunist"),
    "TargetPhase" to listOf("AvoidPhased"),
    "AvoidPhased" to listOf("TargetPhase"),
    "PrioHealthy" to listOf("PrioWounded"),
    "PrioWounded" to listOf("PrioHealthy"),
    "SyncWindow" to listOf("SyncVolley", "Ambush"),
    "SyncVolley" to listOf("SyncWindow", "Ambush"),
    "Ambush" to listOf("SyncWindow", "SyncVolley")
)

fun isIncompatibleWithExistingTags(tag: String, existingTags: List<String>): Boolean {
    val modTag = tagNameToRegexName(tag)
    if (tagIncompatibility.containsKey(modTag)) {
        return existingTags.map { tagNameToRegexName(it) }.any { tagIncompatibility[modTag]?.contains(it) == true }
    }
    return false
}

fun createTags(names: List<String>, weapon: WeaponAPI): List<WeaponAITagBase> {
    return names.mapNotNull { createTag(it, weapon) }.filter { it.isValid() }
}

fun applySuggestedModes(ship: FleetMemberAPI, storageIndex: Int, allowOverriding: Boolean = true, shipId: String? = null) {
    val id = shipId ?: ship.id
    val groups = ship.variant.weaponGroups

    groups.forEachIndexed { index, group ->
        if(allowOverriding || loadPersistentTags(id, ship, index, storageIndex).isEmpty()){
            val weaponID = group.slots?.firstOrNull()?.let { ship.variant.getWeaponId(it) } ?: ""
            persistTags(id, ship, index, storageIndex, getSuggestedModesForWeaponId(weaponID))
        }
    }
}

fun getSuggestedModesForWeaponId(weaponID: String) : List<String>{
    val tagKey: String = if (Settings.getCurrentSuggestedTags().containsKey(weaponID)) {
        weaponID
    } else {
        Settings.getCurrentSuggestedTags().keys.map { Regex(it) }.find { it.matches(weaponID) }.toString()
    }
    return Settings.getCurrentSuggestedTags()[tagKey] ?: emptyList()
}
