package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.softFluxBelowThreshold
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.computeShieldFactor
import com.dp.advancedgunnerycontrol.weaponais.computeTimeToTravel
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

class AvoidShieldSoftFluxTag(
    weapon: WeaponAPI,
    private val freeFireSoftFluxThreshold: Float,
    private val shieldThreshold: Float = Settings.avoidShieldThreshold()
) : WeaponAITagBase(weapon) {
    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean {
        return if (weapon.ship?.softFluxBelowThreshold(freeFireSoftFluxThreshold) ?: true) {
            true
        } else {
            computeShieldFactor(entity, weapon) < shieldThreshold
        }
    }

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float {
        return computeShieldFactor(solution.target, weapon) + 0.1f
    }

    override fun shouldFire(solution: FiringSolution): Boolean {
        return if (weapon.ship?.softFluxBelowThreshold(freeFireSoftFluxThreshold) ?: true) {
            true
        } else if (solution.target is ShipAPI) {
            if (Settings.ignoreFighterShield() && solution.target.isFighter) {
                true
            } else {
                val timeToTravel = computeTimeToTravel(weapon, solution.aimPoint)
                computeShieldFactor(solution.target, weapon, timeToTravel) < shieldThreshold
            }
        } else {
            false
        }
    }

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
