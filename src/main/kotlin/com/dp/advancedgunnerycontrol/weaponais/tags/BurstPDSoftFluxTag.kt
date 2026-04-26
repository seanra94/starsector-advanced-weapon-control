package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.utils.softFluxAboveThresholdAndTotalFluxBelowCap
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.isPD
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

class BurstPDSoftFluxTag(weapon: WeaponAPI, private val freeFireSoftFluxThreshold: Float) : WeaponAITagBase(weapon) {
    override fun isValidTarget(entity: CombatEntityAPI): Boolean {
        if (!(weapon.ship?.softFluxAboveThresholdAndTotalFluxBelowCap(freeFireSoftFluxThreshold) ?: false)) {
            return super.isValidTarget(entity)
        }
        return (entity as? MissileAPI) != null || (entity as? ShipAPI)?.isFighter == true
    }

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1.0f

    override fun shouldFire(solution: FiringSolution): Boolean = true

    override fun isBaseAiOverridable(): Boolean = false

    override fun avoidDebris(): Boolean = false

    override fun isValid(): Boolean {
        return isPD(weapon) && super.isValid()
    }
}
