package com.dp.advancedgunnerycontrol.utils

import com.dp.advancedgunnerycontrol.settings.Settings
import com.fs.starfarer.api.combat.ShipAPI


// SEAN
/**
 * - 0 = all flux capacity is free
 * - 1 = all flux capacity is occupied by soft flux <br>
 * @return soft-flux level as a float, 0-1
 */
fun ShipAPI.softFluxLevel(): Float = fluxLevel - hardFluxLevel


/**
 * @param softFLuxLimit 0–1
 */
fun ShipAPI.softFluxBelowThreshold(softFLuxLimit: Float): Boolean {
    if (fluxLevel >= Settings.SFTUpperFluxLimit.value) return false
    return softFluxLevel() < softFLuxLimit
}
