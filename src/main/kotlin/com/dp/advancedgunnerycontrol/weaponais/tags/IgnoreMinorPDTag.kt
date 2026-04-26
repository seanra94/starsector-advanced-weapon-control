package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

class IgnoreMinorPDTag(weapon: WeaponAPI, private val healthThreshold: Float = 145f) : WeaponAITagBase(weapon) {
    override fun isValidTarget(entity: CombatEntityAPI): Boolean {
        return when (entity) {
            is ShipAPI -> {
                if (!entity.isFighter) {
                    true
                } else {
                    val shieldHp = if (entity.shield != null &&
                        entity.shield?.type != ShieldAPI.ShieldType.NONE &&
                        entity.shield?.type != ShieldAPI.ShieldType.PHASE
                    ) {
                        effectiveShieldHp(entity.currFlux)
                    } else {
                        0f
                    }
                    val armorHp = effectiveArmorHp(entity.armorGrid.armorRating)
                    entity.hitpoints + shieldHp + armorHp > healthThreshold
                }
            }

            is MissileAPI -> entity.hitpoints > healthThreshold

            else -> false
        }
    }

    private fun effectiveArmorHp(armorHp: Float): Float = when (weapon.damageType) {
        DamageType.FRAGMENTATION -> armorHp * 4f
        DamageType.KINETIC -> armorHp * 2f
        DamageType.HIGH_EXPLOSIVE -> armorHp * 0.5f
        else -> armorHp
    }

    private fun effectiveShieldHp(shieldHp: Float): Float = when (weapon.damageType) {
        DamageType.FRAGMENTATION -> shieldHp * 4f
        DamageType.KINETIC -> shieldHp * 0.5f
        DamageType.HIGH_EXPLOSIVE -> shieldHp * 2f
        else -> shieldHp
    }

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean = isValidTarget(entity)

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1f

    override fun shouldFire(solution: FiringSolution): Boolean = true

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
