package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.utils.totalFluxAtOrBelowThreshold
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.WeaponAPI

class HoldTotalFluxTag(weapon: WeaponAPI, private val threshold: Float) : WeaponAITagBase(weapon) {
    override fun isValidTarget(entity: CombatEntityAPI): Boolean = true

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean {
        return weapon.ship?.totalFluxAtOrBelowThreshold(threshold) ?: true
    }

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1.0f

    override fun shouldFire(solution: FiringSolution): Boolean {
        return weapon.ship?.totalFluxAtOrBelowThreshold(threshold) ?: true
    }

    override fun isBaseAiOverridable(): Boolean = false

    override fun avoidDebris(): Boolean = false
}
