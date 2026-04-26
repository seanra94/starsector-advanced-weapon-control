package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.utils.softFluxBelowThreshold
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.isValidPDTargetForWeapon
import com.fs.starfarer.api.combat.WeaponAPI

class BurstPDSoftFluxTag(weapon: WeaponAPI, private val freeFireSoftFluxThreshold: Float) : WeaponAITagBase(weapon) {
    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1.0f

    override fun shouldFire(solution: FiringSolution): Boolean {
        val isBelowThreshold = weapon.ship?.softFluxBelowThreshold(freeFireSoftFluxThreshold) ?: false
        return isBelowThreshold || isValidPDTargetForWeapon(solution.target, weapon)
    }

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
