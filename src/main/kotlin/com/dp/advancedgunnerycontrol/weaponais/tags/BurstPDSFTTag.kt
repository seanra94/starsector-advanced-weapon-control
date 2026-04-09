package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.utils.softFluxBelowThreshold
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.ammoLevel
import com.dp.advancedgunnerycontrol.weaponais.isValidPDTargetForWeapon
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.WeaponAPI

// SEAN
// Only fire at full ROF if target is missile or fighter and ammo < ammoThreshold
class BurstPDSFTTag(weapon: WeaponAPI, private val softFluxThreshold: Float) : WeaponAITagBase(weapon) {

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1.0f
    override fun shouldFire(solution: FiringSolution): Boolean {
        val isBelowThreshold = weapon.ship?.softFluxBelowThreshold(softFluxThreshold) ?: false
        return if (isBelowThreshold) {
            true
        } else {
            return isValidPDTargetForWeapon(solution.target, weapon)
        }
    }
    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
