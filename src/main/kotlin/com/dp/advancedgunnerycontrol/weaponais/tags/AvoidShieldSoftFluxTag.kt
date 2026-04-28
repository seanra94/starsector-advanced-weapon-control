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

class AvoidShieldSoftFluxTag(
    weapon: WeaponAPI,
    private val freeFireSoftFluxThreshold: Float,
    private val shieldThreshold: Float = Settings.avoidShieldThreshold()
) : WeaponAITagBase(weapon) {
    private val restrictionCondition = FluxCondition(
        metric = FluxMetric.SOFT,
        comparator = FluxComparator.GREATER_THAN,
        threshold = freeFireSoftFluxThreshold,
        requireTotalFluxBelowSoftFluxCap = true
    )

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean {
        return if (weapon.ship?.meetsFluxCondition(restrictionCondition) ?: false) {
            computeShieldFactor(entity, weapon) < shieldThreshold
        } else {
            true
        }
    }

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float {
        return computeShieldFactor(solution.target, weapon) + 0.1f
    }

    override fun shouldFire(solution: FiringSolution): Boolean {
        return if (weapon.ship?.meetsFluxCondition(restrictionCondition) ?: false) {
            if (solution.target is ShipAPI) {
                if (Settings.ignoreFighterShield() && solution.target.isFighter) {
                    true
                } else {
                    val timeToTravel = computeTimeToTravel(weapon, solution.aimPoint)
                    computeShieldFactor(solution.target, weapon, timeToTravel) < shieldThreshold
                }
            } else {
                false
            }
        } else {
            true
        }
    }

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
