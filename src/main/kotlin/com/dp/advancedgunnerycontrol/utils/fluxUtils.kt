package com.dp.advancedgunnerycontrol.utils

import com.dp.advancedgunnerycontrol.settings.Settings
import com.fs.starfarer.api.combat.ShipAPI

fun ShipAPI.softFluxLevel(): Float = fluxLevel - hardFluxLevel

fun ShipAPI.totalFluxAtOrBelowThreshold(totalFluxLimit: Float): Boolean = fluxLevel <= totalFluxLimit

fun ShipAPI.totalFluxBelowThreshold(totalFluxLimit: Float): Boolean = fluxLevel < totalFluxLimit

fun ShipAPI.totalFluxAboveThreshold(totalFluxLimit: Float): Boolean = fluxLevel > totalFluxLimit

fun ShipAPI.softFluxBelowThresholdAndTotalFluxBelowCap(softFluxLimit: Float): Boolean {
    if (!totalFluxBelowThreshold(Settings.softFluxTotalFluxCap())) return false
    return softFluxLevel() < softFluxLimit
}

fun ShipAPI.softFluxAboveThresholdAndTotalFluxBelowCap(softFluxLimit: Float): Boolean {
    if (!totalFluxBelowThreshold(Settings.softFluxTotalFluxCap())) return false
    return softFluxLevel() > softFluxLimit
}

fun ShipAPI.softFluxBelowThreshold(softFluxLimit: Float): Boolean {
    return softFluxBelowThresholdAndTotalFluxBelowCap(softFluxLimit)
}
