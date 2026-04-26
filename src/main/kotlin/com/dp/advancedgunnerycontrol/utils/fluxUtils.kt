package com.dp.advancedgunnerycontrol.utils

import com.dp.advancedgunnerycontrol.settings.Settings
import com.fs.starfarer.api.combat.ShipAPI

fun ShipAPI.softFluxLevel(): Float = fluxLevel - hardFluxLevel

fun ShipAPI.softFluxBelowThreshold(softFluxLimit: Float): Boolean {
    if (fluxLevel >= Settings.softFluxTotalFluxCap()) return false
    return softFluxLevel() < softFluxLimit
}
