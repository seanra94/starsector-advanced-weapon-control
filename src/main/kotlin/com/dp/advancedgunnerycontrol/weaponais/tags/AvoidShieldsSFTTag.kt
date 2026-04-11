package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.softFluxBelowThreshold
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.computeShieldFactor
import com.dp.advancedgunnerycontrol.weaponais.computeTimeToTravel
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

// SEAN
// Allows targeting of anything when soft flux < softFluxThreshold, otherwise avoids shields. Always prioritises by target shield factor
class AvoidShieldsSFTTag(
    weapon: WeaponAPI,
    private val shieldThreshold: Float = Settings.avoidShieldsThreshold(),
    private val softFluxThreshold: Float = Settings.avoidShieldsAtFT()
) : WeaponAITagBase(weapon) {

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean {
        return if (weapon.ship.softFluxBelowThreshold(softFluxThreshold)) {
            true
        } else {
            computeShieldFactor(entity, weapon) < shieldThreshold
        }
    }

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float {
        return computeShieldFactor(solution.target, weapon) + 0.1f
    }

    override fun shouldFire(solution: FiringSolution): Boolean {
        return if (weapon.ship.softFluxBelowThreshold(softFluxThreshold)) {
            true
        } else if (solution.target is ShipAPI) {
            if (Settings.ignoreFighterShields() && solution.target.isFighter) {
                true
            } else {
                val ttt = computeTimeToTravel(weapon, solution.aimPoint)
                computeShieldFactor(solution.target, weapon, ttt) < shieldThreshold
            }
        } else {
            false
        }

    }

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
