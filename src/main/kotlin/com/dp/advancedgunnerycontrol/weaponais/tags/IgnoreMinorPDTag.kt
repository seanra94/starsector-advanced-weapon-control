package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import kotlin.math.max

class IgnoreMinorPDTag(weapon: WeaponAPI, private val healthThreshold: Float = 145f) : WeaponAITagBase(weapon) {
    companion object {
        private const val ARMOR_HEALTH_MULTIPLIER = 2f
    }

    override fun isValidTarget(entity: CombatEntityAPI): Boolean {
        return when (entity) {
            is ShipAPI -> {
                if (!entity.isFighter) {
                    true
                } else {
                    val shieldHp = effectiveRemainingShieldHp(entity)
                    val armorHp = entity.armorGrid.armorRating * ARMOR_HEALTH_MULTIPLIER
                    entity.hitpoints + armorHp + shieldHp > healthThreshold
                }
            }

            is MissileAPI -> entity.hitpoints > healthThreshold

            else -> false
        }
    }

    private fun effectiveRemainingShieldHp(fighter: ShipAPI): Float {
        val shield = fighter.shield ?: return 0f
        val fluxTracker = fighter.fluxTracker ?: return 0f
        if (shield.type == ShieldAPI.ShieldType.NONE || shield.type == ShieldAPI.ShieldType.PHASE) return 0f
        if (fluxTracker.isOverloadedOrVenting) return 0f

        val shieldDamageTakenMult = fighter.mutableStats?.shieldDamageTakenMult?.modifiedValue ?: 1f
        val safeShieldDamageTakenMult = max(0.0001f, shieldDamageTakenMult)
        val remainingFluxCapacity = (fluxTracker.maxFlux - fluxTracker.currFlux).coerceAtLeast(0f)
        return remainingFluxCapacity / safeShieldDamageTakenMult
    }

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean = isValidTarget(entity)

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1f

    override fun shouldFire(solution: FiringSolution): Boolean = true

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
