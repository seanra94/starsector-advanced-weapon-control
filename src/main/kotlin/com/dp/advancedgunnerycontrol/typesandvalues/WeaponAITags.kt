package com.dp.advancedgunnerycontrol.typesandvalues

import com.dp.advancedgunnerycontrol.WeaponControlPlugin
import com.dp.advancedgunnerycontrol.gui.isElligibleForPD
import com.dp.advancedgunnerycontrol.gui.isEverythingBlacklisted
import com.dp.advancedgunnerycontrol.gui.usesAmmo
import com.dp.advancedgunnerycontrol.gui.usesAmmoNonMissile
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.FluxComparator
import com.dp.advancedgunnerycontrol.utils.FluxCondition
import com.dp.advancedgunnerycontrol.utils.FluxMetric
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

val pdTags = listOf("PD", "NoPD", "PD(TF>N%)", "PD(SF>N%)", "PD(HF>N%)", "NoMissile")
val opportunistAmmoTags = listOf("ConserveAmmo", "Opportunist(A<N%)")
val pdAmmoTags = listOf("ConservePDAmmo", "PD(A<N%)")

private const val LEGACY_TOTAL_FLUX_TOKEN = "Fl?u?x?"

val holdTotalFluxRegex = Regex("Hold\\(TF>(\\d+)%\\)")
val holdTotalFluxLegacyRegex = Regex("Hold(?:FT)?\\($LEGACY_TOTAL_FLUX_TOKEN>(\\d+)%\\)")
val holdSoftFluxRegex = Regex("Hold\\(SF>(\\d+)%\\)")
val holdHardFluxRegex = Regex("Hold\\(HF>(\\d+)%\\)")
val holdSoftFluxLegacyRegex = Regex("HoldSFT\\($LEGACY_TOTAL_FLUX_TOKEN>(\\d+)%\\)")
val forceFireTotalFluxRegex = Regex("Force\\(TF<(\\d+)%\\)")
val forceFireTotalFluxLegacyRegex = Regex("Force(?:F|FT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val forceFireSoftFluxRegex = Regex("Force\\(SF<(\\d+)%\\)")
val forceFireHardFluxRegex = Regex("Force\\(HF<(\\d+)%\\)")
val forceFireSoftFluxLegacyRegex = Regex("ForceSFT\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val avoidShieldTotalFluxRegex = Regex("AvoidShield\\(TF>(\\d+)%\\)")
val avoidShieldSoftFluxRegex = Regex("AvoidShield\\(SF>(\\d+)%\\)")
val avoidShieldHardFluxRegex = Regex("AvoidShield\\(HF>(\\d+)%\\)")
val avoidShieldTotalFluxLegacyRegex = Regex("(?:AvShldFT|AvdShieldsFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val avoidShieldSoftFluxLegacyRegex = Regex("(?:AvShldSFT|AvShlddSFT|AvdShieldsSFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val targetShieldTotalFluxRegex = Regex("TargetShield\\(TF>(\\d+)%\\)")
val targetShieldSoftFluxRegex = Regex("TargetShield\\(SF>(\\d+)%\\)")
val targetShieldHardFluxRegex = Regex("TargetShield\\(HF>(\\d+)%\\)")
val targetShieldTotalFluxLegacyRegex = Regex("(?:TgtShldFT|TgtShieldsFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val targetShieldSoftFluxLegacyRegex = Regex("(?:TgtShldSFT|TgtShieldsSFT)\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val pdSoftFluxRegex = Regex("PD\\(SF>(\\d+)%\\)")
val burstPDSoftFluxAliasRegex = Regex("BurstPD\\(SF>(\\d+)%\\)")
val burstPDSoftFluxLegacyRegex = Regex("BurstPDSFT\\($LEGACY_TOTAL_FLUX_TOKEN<(\\d+)%\\)")
val pdTotalFluxRegex = Regex("PD\\(TF>(\\d+)%\\)")
val pdHardFluxRegex = Regex("PD\\(HF>(\\d+)%\\)")
val pdTotalFluxLegacyRegex = Regex("PD\\($LEGACY_TOTAL_FLUX_TOKEN>(\\d+)%\\)")
val opportunistAmmoRegex = Regex("Opportunist\\(A<(\\d+)%\\)")
val conserveAmmoRegex = Regex("ConserveAmmo\\(A<(\\d+)%\\)")
val pdAmmoRegex = Regex("PD\\(A<(\\d+)%\\)")
val conservePDAmmoRegex = Regex("ConservePDAmmo\\(A<(\\d+)%\\)")
val conservePDAmmoShortRegex = Regex("CnsrvPDAmmo\\(A<(\\d+)%\\)")
val noPdHealthRegex = Regex("NoPD\\(H<(\\d+)\\)")
val ignoreMinorPDRegex = Regex("IgnoreMinorPD\\(H<(\\d+)\\)")
val avoidArmorRegex = Regex("(?:AvoidArmor|AvdArmor)\\((\\d+)%\\)")
val panicFireRegex = Regex("Panic\\(H<(\\d+)%\\)")
val rangeRegex = Regex("Range<(\\d+)%")
val rofRegex = Regex("LowRoF\\((\\d+)%\\)")

//val prioPdRegex = Regex("PrioP[dD]\\((\\d+)\\)")
//val prioFightersRegex = Regex("PrioFighter\\((\\d+)\\)")
//val prioMissilesRegex = Regex("PrioMissile\\((\\d+)\\)")

private data class ThresholdCanonicalizationRule(
    val canonicalRegex: Regex,
    val legacyRegex: Regex,
    val canonicalPrefix: String,
    val invertLegacyThreshold: Boolean = false
)

private val thresholdCanonicalizationRules = listOf(
    ThresholdCanonicalizationRule(holdTotalFluxRegex, holdTotalFluxLegacyRegex, "Hold(TF>"),
    ThresholdCanonicalizationRule(holdSoftFluxRegex, holdSoftFluxLegacyRegex, "Hold(SF>"),
    ThresholdCanonicalizationRule(forceFireTotalFluxRegex, forceFireTotalFluxLegacyRegex, "Force(TF<"),
    ThresholdCanonicalizationRule(forceFireSoftFluxRegex, forceFireSoftFluxLegacyRegex, "Force(SF<"),
    ThresholdCanonicalizationRule(avoidShieldTotalFluxRegex, avoidShieldTotalFluxLegacyRegex, "AvoidShield(TF>", invertLegacyThreshold = true),
    ThresholdCanonicalizationRule(avoidShieldSoftFluxRegex, avoidShieldSoftFluxLegacyRegex, "AvoidShield(SF>", invertLegacyThreshold = true),
    ThresholdCanonicalizationRule(targetShieldTotalFluxRegex, targetShieldTotalFluxLegacyRegex, "TargetShield(TF>", invertLegacyThreshold = true),
    ThresholdCanonicalizationRule(targetShieldSoftFluxRegex, targetShieldSoftFluxLegacyRegex, "TargetShield(SF>", invertLegacyThreshold = true),
    ThresholdCanonicalizationRule(pdSoftFluxRegex, burstPDSoftFluxLegacyRegex, "PD(SF>", invertLegacyThreshold = true),
    ThresholdCanonicalizationRule(pdTotalFluxRegex, pdTotalFluxLegacyRegex, "PD(TF>")
)

fun extractRegexThreshold(regex: Regex, name: String): Float {
    return (regex.matchEntire(name)?.groupValues?.get(1)?.toFloat() ?: 0f) / 100f
}

private fun extractRawRegexThreshold(regex: Regex, name: String): Float {
    return regex.matchEntire(name)?.groupValues?.get(1)?.toFloat() ?: 0f
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

private fun canonicalizeThresholdTag(
    inputTag: String,
    canonicalRegex: Regex,
    legacyRegex: Regex,
    canonicalPrefix: String,
    invertLegacyThreshold: Boolean = false
): String? {
    if (canonicalRegex.matches(inputTag)) return inputTag
    if (!legacyRegex.matches(inputTag)) return null
    val threshold = if (invertLegacyThreshold) {
        invertedRegexThresholdAsPercentageString(legacyRegex, inputTag)
    } else {
        extractRegexThresholdAsPercentageString(legacyRegex, inputTag)
    }
    return "$canonicalPrefix$threshold)"
}

private fun parseCanonicalFluxCondition(
    tag: String,
    regex: Regex,
    metric: FluxMetric,
    comparator: FluxComparator,
    requireSoftFluxCap: Boolean = false
): FluxCondition? {
    if (!regex.matches(tag)) return null
    return FluxCondition(
        metric = metric,
        comparator = comparator,
        threshold = extractRegexThreshold(regex, tag),
        requireTotalFluxBelowSoftFluxCap = requireSoftFluxCap
    )
}

private fun fluxConditionWording(condition: FluxCondition): String {
    val metric = when (condition.metric) {
        FluxMetric.TOTAL -> "total flux"
        FluxMetric.SOFT -> "soft flux"
        FluxMetric.HARD -> "hard flux"
    }
    val comparator = when (condition.comparator) {
        FluxComparator.GREATER_THAN -> "greater than"
        FluxComparator.LESS_THAN -> "below"
        FluxComparator.GREATER_OR_EQUAL -> "greater than or equal to"
        FluxComparator.LESS_OR_EQUAL -> "below or equal to"
    }
    val threshold = "${thresholdAsPercent(condition.threshold)}%"
    val base = "$metric is $comparator $threshold"
    return if (condition.requireTotalFluxBelowSoftFluxCap) {
        "$base and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%"
    } else {
        base
    }
}

private fun fluxConditionThresholdPercent(condition: FluxCondition): String = "${thresholdAsPercent(condition.threshold)}%"

fun shouldTagBeDisabled(groupIndex: Int, sh: FleetMemberAPI, tag: String): Boolean {
    val modTag = tagNameToRegexName(tag)
    if (pdTags.contains(modTag) && !isElligibleForPD(groupIndex, sh)) return true
    if (opportunistAmmoTags.contains(modTag) && !usesAmmo(groupIndex, sh)) return true
    if (pdAmmoTags.contains(modTag) && !usesAmmoNonMissile(groupIndex, sh)) return true
    return isEverythingBlacklisted(groupIndex, sh)
}

val priorityBoilerplateText = "\nIncreases priority by a factor of ${Settings.prioXModifier()} (adjustable in Settigs.editme)." +
        "\nCombine multiple Prio-tags to de-prioritize everything else."

private fun pdTargetingRestrictionTooltip(): String = "Restricts targeting to fighters and missiles."

private fun tooltipWithActivationCondition(base: String, condition: String): String {
    return "$base\nActivation condition: $condition."
}

val tagTooltips = mapOf(
    "PD" to pdTargetingRestrictionTooltip(),
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
    "IgnoreMinorPD" to "Ignores missiles and fighters below a default effective durability threshold. Effective durability is combined hull + armor + shield contribution.",
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
    thresholdCanonicalizationRules.forEach { rule ->
        canonicalizeThresholdTag(
            inputTag = tag,
            canonicalRegex = rule.canonicalRegex,
            legacyRegex = rule.legacyRegex,
            canonicalPrefix = rule.canonicalPrefix,
            invertLegacyThreshold = rule.invertLegacyThreshold
        )?.let { return it }
    }

    return when {
        burstPDSoftFluxAliasRegex.matches(tag) -> "PD(SF>${invertedRegexThresholdAsPercentageString(burstPDSoftFluxAliasRegex, tag)})"
        pdSoftFluxRegex.matches(tag) -> tag
        pdHardFluxRegex.matches(tag) -> tag
        holdHardFluxRegex.matches(tag) -> tag
        forceFireHardFluxRegex.matches(tag) -> tag
        avoidShieldHardFluxRegex.matches(tag) -> tag
        targetShieldHardFluxRegex.matches(tag) -> tag
        opportunistAmmoRegex.matches(tag) -> tag
        conserveAmmoRegex.matches(tag) -> "Opportunist(A<${extractRegexThresholdAsPercentageString(conserveAmmoRegex, tag)})"
        pdAmmoRegex.matches(tag) -> tag
        conservePDAmmoRegex.matches(tag) -> "PD(A<${extractRegexThresholdAsPercentageString(conservePDAmmoRegex, tag)})"
        conservePDAmmoShortRegex.matches(tag) -> "PD(A<${extractRegexThresholdAsPercentageString(conservePDAmmoShortRegex, tag)})"
        noPdHealthRegex.matches(tag) -> tag
        ignoreMinorPDRegex.matches(tag) -> "NoPD(H<${extractRawRegexThreshold(ignoreMinorPDRegex, tag).toInt()})"
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
        tag == "IgnoreMinorPD" -> "NoPD(H<145)"
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

        holdHardFluxRegex.matches(canonicalTag) -> "Weapon will stop firing if ship hard flux exceeds ${
            extractRegexThresholdAsPercentageString(
                holdHardFluxRegex,
                canonicalTag
            )
        }."

        forceFireTotalFluxRegex.matches(canonicalTag) -> "ForceFire: Weapon will ignore firing restrictions of other tags while total flux is below ${
            fluxConditionThresholdPercent(
                parseCanonicalFluxCondition(canonicalTag, forceFireTotalFluxRegex, FluxMetric.TOTAL, FluxComparator.LESS_THAN)!!
            )
        }." +
                "\nNote: This will not circumvent targeting restrictions, only firing restrictions."

        forceFireSoftFluxRegex.matches(canonicalTag) -> "ForceFire: Weapon will ignore firing restrictions of other tags while soft flux is below ${
            fluxConditionThresholdPercent(
                parseCanonicalFluxCondition(canonicalTag, forceFireSoftFluxRegex, FluxMetric.SOFT, FluxComparator.LESS_THAN, requireSoftFluxCap = true)!!
            )
        } and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%." +
                "\nNote: This will not circumvent targeting restrictions, only firing restrictions."

        forceFireHardFluxRegex.matches(canonicalTag) -> "ForceFire: Weapon will ignore firing restrictions of other tags while hard flux is below ${
            fluxConditionThresholdPercent(
                parseCanonicalFluxCondition(canonicalTag, forceFireHardFluxRegex, FluxMetric.HARD, FluxComparator.LESS_THAN)!!
            )
        }." +
                "\nNote: This will not circumvent targeting restrictions, only firing restrictions."

        avoidShieldTotalFluxRegex.matches(canonicalTag) -> tooltipWithActivationCondition(
            tagTooltips["AvoidShield"] ?: "No description available.",
            fluxConditionWording(
                parseCanonicalFluxCondition(canonicalTag, avoidShieldTotalFluxRegex, FluxMetric.TOTAL, FluxComparator.GREATER_THAN)!!
            )
        )

        avoidShieldSoftFluxRegex.matches(canonicalTag) -> tooltipWithActivationCondition(
            tagTooltips["AvoidShield"] ?: "No description available.",
            fluxConditionWording(
                parseCanonicalFluxCondition(canonicalTag, avoidShieldSoftFluxRegex, FluxMetric.SOFT, FluxComparator.GREATER_THAN, requireSoftFluxCap = true)!!
            )
        )

        avoidShieldHardFluxRegex.matches(canonicalTag) -> tooltipWithActivationCondition(
            tagTooltips["AvoidShield"] ?: "No description available.",
            fluxConditionWording(
                parseCanonicalFluxCondition(canonicalTag, avoidShieldHardFluxRegex, FluxMetric.HARD, FluxComparator.GREATER_THAN)!!
            )
        )

        targetShieldTotalFluxRegex.matches(canonicalTag) -> tooltipWithActivationCondition(
            tagTooltips["TargetShield"] ?: "No description available.",
            fluxConditionWording(
                parseCanonicalFluxCondition(canonicalTag, targetShieldTotalFluxRegex, FluxMetric.TOTAL, FluxComparator.GREATER_THAN)!!
            )
        )

        targetShieldSoftFluxRegex.matches(canonicalTag) -> tooltipWithActivationCondition(
            tagTooltips["TargetShield"] ?: "No description available.",
            fluxConditionWording(
                parseCanonicalFluxCondition(canonicalTag, targetShieldSoftFluxRegex, FluxMetric.SOFT, FluxComparator.GREATER_THAN, requireSoftFluxCap = true)!!
            )
        )

        targetShieldHardFluxRegex.matches(canonicalTag) -> tooltipWithActivationCondition(
            tagTooltips["TargetShield"] ?: "No description available.",
            fluxConditionWording(
                parseCanonicalFluxCondition(canonicalTag, targetShieldHardFluxRegex, FluxMetric.HARD, FluxComparator.GREATER_THAN)!!
            )
        )

        pdSoftFluxRegex.matches(canonicalTag) -> "${pdTargetingRestrictionTooltip().removeSuffix(".")} while soft flux is greater than ${
            fluxConditionThresholdPercent(
                parseCanonicalFluxCondition(canonicalTag, pdSoftFluxRegex, FluxMetric.SOFT, FluxComparator.GREATER_THAN, requireSoftFluxCap = true)!!
            )
        } and total flux is below ${(Settings.softFluxTotalFluxCap() * 100f).toInt()}%."

        pdTotalFluxRegex.matches(canonicalTag) -> "${pdTargetingRestrictionTooltip().removeSuffix(".")} while total flux is greater than ${
            fluxConditionThresholdPercent(
                parseCanonicalFluxCondition(canonicalTag, pdTotalFluxRegex, FluxMetric.TOTAL, FluxComparator.GREATER_THAN)!!
            )
        }."

        pdHardFluxRegex.matches(canonicalTag) -> "${pdTargetingRestrictionTooltip().removeSuffix(".")} while hard flux is greater than ${
            fluxConditionThresholdPercent(
                parseCanonicalFluxCondition(canonicalTag, pdHardFluxRegex, FluxMetric.HARD, FluxComparator.GREATER_THAN)!!
            )
        }."

        opportunistAmmoRegex.matches(canonicalTag) -> "While ammo is less than ${
            extractRegexThresholdAsPercentageString(opportunistAmmoRegex, canonicalTag)
        }, only fires at opportune targets. Weapons without ammo are unaffected."

        pdAmmoRegex.matches(canonicalTag) -> "Restricts targeting to fighters and missiles while ammo is less than ${
            extractRegexThresholdAsPercentageString(pdAmmoRegex, canonicalTag)
        }. Weapons without ammo and missile weapons are unaffected."

        noPdHealthRegex.matches(canonicalTag) -> "Does not target fighters or missiles while target health is less than ${
            extractRawRegexThreshold(noPdHealthRegex, canonicalTag).toInt()
        }. Health is hull + armor × 2 + effective remaining shield."

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
    val canonicalName = canonicalizeWeaponTagName(name)
    when {
        holdTotalFluxRegex.matches(canonicalName) -> return HoldTotalFluxTag(weapon, extractRegexThreshold(holdTotalFluxRegex, canonicalName))
        holdSoftFluxRegex.matches(canonicalName) -> return HoldSoftFluxTag(weapon, extractRegexThreshold(holdSoftFluxRegex, canonicalName))
        holdHardFluxRegex.matches(canonicalName) -> return HoldHardFluxTag(weapon, extractRegexThreshold(holdHardFluxRegex, canonicalName))
        forceFireTotalFluxRegex.matches(canonicalName) -> return ForceFireTotalFluxTag(weapon, extractRegexThreshold(forceFireTotalFluxRegex, canonicalName))
        forceFireSoftFluxRegex.matches(canonicalName) -> return ForceFireSoftFluxTag(weapon, extractRegexThreshold(forceFireSoftFluxRegex, canonicalName))
        forceFireHardFluxRegex.matches(canonicalName) -> return ForceFireHardFluxTag(weapon, extractRegexThreshold(forceFireHardFluxRegex, canonicalName))
        avoidShieldTotalFluxRegex.matches(canonicalName) -> return AvoidShieldTotalFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldTotalFluxRegex, canonicalName)
        )
        avoidShieldSoftFluxRegex.matches(canonicalName) -> return AvoidShieldSoftFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldSoftFluxRegex, canonicalName)
        )
        avoidShieldHardFluxRegex.matches(canonicalName) -> return AvoidShieldHardFluxTag(
            weapon,
            extractRegexThreshold(avoidShieldHardFluxRegex, canonicalName)
        )
        targetShieldTotalFluxRegex.matches(canonicalName) -> return TargetShieldTotalFluxTag(
            weapon,
            extractRegexThreshold(targetShieldTotalFluxRegex, canonicalName)
        )
        targetShieldSoftFluxRegex.matches(canonicalName) -> return TargetShieldSoftFluxTag(
            weapon,
            extractRegexThreshold(targetShieldSoftFluxRegex, canonicalName)
        )
        targetShieldHardFluxRegex.matches(canonicalName) -> return TargetShieldHardFluxTag(
            weapon,
            extractRegexThreshold(targetShieldHardFluxRegex, canonicalName)
        )
        pdSoftFluxRegex.matches(canonicalName) -> return BurstPDSoftFluxTag(weapon, extractRegexThreshold(pdSoftFluxRegex, canonicalName))
        pdTotalFluxRegex.matches(canonicalName) -> return PDAtTotalFluxTag(weapon, extractRegexThreshold(pdTotalFluxRegex, canonicalName))
        pdHardFluxRegex.matches(canonicalName) -> return PDAtHardFluxTag(weapon, extractRegexThreshold(pdHardFluxRegex, canonicalName))
        opportunistAmmoRegex.matches(canonicalName) -> return ConserveAmmoTag(weapon, extractRegexThreshold(opportunistAmmoRegex, canonicalName))
        pdAmmoRegex.matches(canonicalName) -> return ConservePDAmmoTag(weapon, extractRegexThreshold(pdAmmoRegex, canonicalName))
        noPdHealthRegex.matches(canonicalName) -> return IgnoreMinorPDTag(weapon, extractRawRegexThreshold(noPdHealthRegex, canonicalName))
        avoidArmorRegex.matches(canonicalName) -> return AvoidArmorTag(weapon, extractRegexThreshold(avoidArmorRegex, canonicalName))
        panicFireRegex.matches(canonicalName) -> return PanicFireTag(weapon, extractRegexThreshold(panicFireRegex, canonicalName))
        rangeRegex.matches(canonicalName) -> return RangeTag(weapon, extractRegexThreshold(rangeRegex, canonicalName))
        rofRegex.matches(canonicalName) -> return ReduceRoFTag(weapon, extractRegexThreshold(rofRegex, canonicalName))
    }
    return when (canonicalName) {
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
        "IgnoreMinorPD", "NoPD(H<145)" -> IgnoreMinorPDTag(weapon)
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
                    .warn("Unknown weapon tag: $canonicalName (from: $name)! Will be ignored.")

                unknownTagWarnCounter == 10 -> Global.getLogger(WeaponControlPlugin.Companion::class.java).warn(
                    "Unknown weapon tag: $canonicalName (from: $name)! Future warnings of this type will be skipped."
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
        holdHardFluxRegex.matches(canonicalTag) -> "Hold(HF>N%)"
        forceFireTotalFluxRegex.matches(canonicalTag) -> "Force(TF<N%)"
        forceFireSoftFluxRegex.matches(canonicalTag) -> "Force(SF<N%)"
        forceFireHardFluxRegex.matches(canonicalTag) -> "Force(HF<N%)"
        avoidShieldTotalFluxRegex.matches(canonicalTag) -> "AvoidShield(TF>N%)"
        avoidShieldSoftFluxRegex.matches(canonicalTag) -> "AvoidShield(SF>N%)"
        avoidShieldHardFluxRegex.matches(canonicalTag) -> "AvoidShield(HF>N%)"
        targetShieldTotalFluxRegex.matches(canonicalTag) -> "TargetShield(TF>N%)"
        targetShieldSoftFluxRegex.matches(canonicalTag) -> "TargetShield(SF>N%)"
        targetShieldHardFluxRegex.matches(canonicalTag) -> "TargetShield(HF>N%)"
        pdSoftFluxRegex.matches(canonicalTag) -> "PD(SF>N%)"
        pdTotalFluxRegex.matches(canonicalTag) -> "PD(TF>N%)"
        pdHardFluxRegex.matches(canonicalTag) -> "PD(HF>N%)"
        canonicalTag == "ConserveAmmo" -> "Opportunist(A<N%)"
        opportunistAmmoRegex.matches(canonicalTag) -> "Opportunist(A<N%)"
        canonicalTag == "ConservePDAmmo" -> "PD(A<N%)"
        pdAmmoRegex.matches(canonicalTag) -> "PD(A<N%)"
        noPdHealthRegex.matches(canonicalTag) -> "NoPD(H<N>)"
        canonicalTag == "IgnoreMinorPD" -> "NoPD(H<N>)"
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
    "AvoidShield(HF>N%)",
    "TargetShield(TF>N%)",
    "TargetShield(SF>N%)",
    "TargetShield(HF>N%)"
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
        "PD(SF>N%)",
        "PD(HF>N%)",
        "PD(A<N%)",
        "BigShip",
        "SmallShip",
        "PrioPD"
    ),
    "PrioPD" to listOf("Opportunist", "NoPD", "BigShip", "SmallShip", "Fighter", "PD"),
    "Fighter" to listOf(
        "PD",
        "NoFighter",
        "Opportunist",
        "NoPD",
        "PD(TF>N%)",
        "PD(SF>N%)",
        "PD(HF>N%)",
        "PD(A<N%)",
        "BigShip",
        "SmallShip",
        "PrioPD",
    ),
    "NoPD" to listOf("PD", "Fighter", "PD(TF>N%)", "PD(SF>N%)", "PD(HF>N%)", "PD(A<N%)", "PrioPD"),
    "ShieldOff" to shieldTagIncompatibilities("ShieldOff"),
    "AvoidShield" to shieldTagIncompatibilities("AvoidShield"),
    "TargetShield" to shieldTagIncompatibilities("TargetShield"),
    "TargetShield+" to shieldTagIncompatibilities("TargetShield+"),
    "AvoidShield+" to shieldTagIncompatibilities("AvoidShield+"),
    "AvoidShield(TF>N%)" to shieldTagIncompatibilities("AvoidShield(TF>N%)"),
    "AvoidShield(SF>N%)" to shieldTagIncompatibilities("AvoidShield(SF>N%)"),
    "AvoidShield(HF>N%)" to shieldTagIncompatibilities("AvoidShield(HF>N%)"),
    "TargetShield(TF>N%)" to shieldTagIncompatibilities("TargetShield(TF>N%)"),
    "TargetShield(SF>N%)" to shieldTagIncompatibilities("TargetShield(SF>N%)"),
    "TargetShield(HF>N%)" to shieldTagIncompatibilities("TargetShield(HF>N%)"),
    "NoFighter" to listOf("Fighter", "Opportunist", "Opportunist(A<N%)"),
    "PD(A<N%)" to listOf("PD", "Fighter", "NoPD", "Opportunist", "Opportunist(A<N%)", "BigShip", "SmallShip"),
    "Opportunist" to listOf("Fighter", "PD", "NoFighter", "PD(TF>N%)", "PD(SF>N%)", "PD(HF>N%)", "PD(A<N%)", "PrioPD", "Opportunist(A<N%)", "NoMissile"),
    "Opportunist(A<N%)" to listOf("Fighter", "PD", "NoFighter", "PD(TF>N%)", "PD(SF>N%)", "PD(HF>N%)", "PD(A<N%)", "PrioPD", "Opportunist", "NoMissile"),
    "PD(TF>N%)" to listOf("Fighter", "Opportunist", "Opportunist(A<N%)", "NoPD", "PD", "BigShip", "SmallShip"),
    "PD(SF>N%)" to listOf("Fighter", "Opportunist", "Opportunist(A<N%)", "NoPD", "PD", "BigShip", "SmallShip"),
    "PD(HF>N%)" to listOf("Fighter", "Opportunist", "Opportunist(A<N%)", "NoPD", "PD", "BigShip", "SmallShip"),
    "SmallShip" to listOf("BigShip", "PD", "Fighter", "PD(TF>N%)", "PD(SF>N%)", "PD(HF>N%)", "PD(A<N%)", "PrioPD"),
    "BigShip" to listOf("SmallShip", "PD", "Fighter", "PD(TF>N%)", "PD(SF>N%)", "PD(HF>N%)", "PD(A<N%)", "PrioPD"),
    "NoMissile" to listOf("Opportunist", "Opportunist(A<N%)"),
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
