package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.FluxComparator
import com.dp.advancedgunnerycontrol.utils.FluxCondition
import com.dp.advancedgunnerycontrol.utils.FluxMetric
import com.dp.advancedgunnerycontrol.utils.meetsFluxCondition
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.computeShieldFactor
import com.dp.advancedgunnerycontrol.weaponais.computeTimeToTravel
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

class TargetShieldTotalFluxTag(
    weapon: WeaponAPI,
    private val freeFireTotalFluxThreshold: Float,
    private val shieldThreshold: Float = Settings.targetShieldThreshold()
) : WeaponAITagBase(weapon) {
    private val freeFireCondition = FluxCondition(FluxMetric.TOTAL, FluxComparator.LESS_OR_EQUAL, freeFireTotalFluxThreshold)

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean {
        return if (weapon.ship?.meetsFluxCondition(freeFireCondition) ?: true) {
            true
        } else {
            computeShieldFactor(entity, weapon) > shieldThreshold
        }
    }

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float {
        val targetShip = solution.target as? ShipAPI ?: return 1f
        return 1f / (computeShieldFactor(targetShip, weapon) + 0.5f)
    }

    override fun shouldFire(solution: FiringSolution): Boolean {
        return if (weapon.ship?.meetsFluxCondition(freeFireCondition) ?: true) {
            true
        } else if (solution.target is ShipAPI) {
            if (Settings.ignoreFighterShield() && solution.target.isFighter) {
                true
            } else {
                val timeToTravel = computeTimeToTravel(weapon, solution.aimPoint)
                computeShieldFactor(solution.target, weapon, timeToTravel) > shieldThreshold
            }
        } else {
            false
        }
    }

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
