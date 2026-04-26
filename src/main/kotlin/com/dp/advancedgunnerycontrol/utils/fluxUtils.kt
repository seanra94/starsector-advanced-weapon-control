package com.dp.advancedgunnerycontrol.utils

import com.dp.advancedgunnerycontrol.settings.Settings
import com.fs.starfarer.api.combat.ShipAPI

fun ShipAPI.softFluxLevel(): Float = fluxLevel - hardFluxLevel

fun ShipAPI.softFluxBelowThresholdAndTotalFluxBelowCap(softFluxLimit: Float): Boolean {
    if (fluxLevel >= Settings.softFluxTotalFluxCap()) return false
    return softFluxLevel() < softFluxLimit
}

fun ShipAPI.softFluxAboveThresholdAndTotalFluxBelowCap(softFluxLimit: Float): Boolean {
    if (fluxLevel >= Settings.softFluxTotalFluxCap()) return false
    return softFluxLevel() > softFluxLimit
}

fun ShipAPI.softFluxBelowThreshold(softFluxLimit: Float): Boolean {
    return softFluxBelowThresholdAndTotalFluxBelowCap(softFluxLimit)
}
