package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.weaponais.*
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

// Prioritises missiles > fighters > small ships > big ships
class IgnoreMinorPDTag(weapon: WeaponAPI, private val healthThreshold: Float = 145f) : WeaponAITagBase(weapon) {

    override fun isValidTarget(entity: CombatEntityAPI): Boolean {
        weapon.refireDelay
        weapon.cooldownRemaining
        return when (entity) {
            is ShipAPI -> {
                if (!entity.isFighter) {
                    true
                } else {
                    val shieldHp = if (
                        entity.shield?.type != ShieldAPI.ShieldType.NONE &&
                        entity.shield?.type != ShieldAPI.ShieldType.PHASE
                    ) {
                        effectiveShieldHp(entity.currFlux)
                    } else 0f

                    val armourHp = effectiveArmourHp(entity.armorGrid.armorRating)
                    val totalHp = entity.hitpoints + shieldHp + armourHp
                    totalHp > healthThreshold
                }
            }
            is MissileAPI -> {
                entity.hitpoints > healthThreshold
            }
            else -> false
        }
    }

    private fun effectiveArmourHp(armourHp: Float): Float = when (weapon.damageType) {
        DamageType.FRAGMENTATION   -> armourHp * 4f
        DamageType.KINETIC         -> armourHp * 2f
        DamageType.HIGH_EXPLOSIVE  -> armourHp * 0.5f
        else                       -> armourHp
    }

    private fun effectiveShieldHp(armourHp: Float): Float = when (weapon.damageType) {
        DamageType.FRAGMENTATION   -> armourHp * 4f
        DamageType.KINETIC         -> armourHp * 0.5f
        DamageType.HIGH_EXPLOSIVE  -> armourHp * 2f
        else                       -> armourHp
    }

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean = true
    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1f
    override fun shouldFire(solution: FiringSolution): Boolean = true
    override fun isBaseAiOverridable(): Boolean = true
    override fun avoidDebris(): Boolean = false
}