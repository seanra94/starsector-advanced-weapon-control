package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import kotlin.math.max

class NoPDWasteTag(weapon: WeaponAPI, private val wasteThreshold: Float) : WeaponAITagBase(weapon) {
    companion object {
        private const val ARMOR_HEALTH_MULTIPLIER = 2f
        private const val BEAM_PACKET_WINDOW_SECONDS = 0.5f
        private const val EPSILON = 0.0001f
    }

    override fun isValidTarget(entity: CombatEntityAPI): Boolean {
        if (entity is MissileAPI) {
            return passesWasteCheck(entity.hitpoints)
        }
        if (entity is ShipAPI && entity.isFighter) {
            return passesWasteCheck(estimateFighterRequiredDamage(entity))
        }
        return true
    }

    private fun passesWasteCheck(targetDamageRequired: Float): Boolean {
        val attackPacketDamage = estimateAttackPacketDamage()
        if (attackPacketDamage <= Settings.noPDWasteCleanupDamageCap()) {
            return true
        }
        if (attackPacketDamage <= EPSILON) {
            return true
        }
        val boundedRequired = targetDamageRequired.coerceAtLeast(0f)
        val wasteRatio = max(0f, attackPacketDamage - boundedRequired) / attackPacketDamage
        return wasteRatio <= wasteThreshold
    }

    private fun estimateAttackPacketDamage(): Float {
        val baseDamage = weapon.damage.damage.coerceAtLeast(0f)
        val packetFactor = when {
            // Local repo evidence: AGCUtils treats beam damage as DPS-like (`weapon.damage.damage * 2f`), so use a short fixed window.
            weapon.isBeam || weapon.isBurstBeam -> BEAM_PACKET_WINDOW_SECONDS
            else -> (weapon.spec?.burstSize ?: 1).coerceAtLeast(1).toFloat()
        }
        return (baseDamage * packetFactor).coerceAtLeast(0f)
    }

    private fun estimateFighterRequiredDamage(fighter: ShipAPI): Float {
        val hull = fighter.hitpoints.coerceAtLeast(0f)
        val armorBase = (fighter.armorGrid?.armorRating ?: 0f).coerceAtLeast(0f) * ARMOR_HEALTH_MULTIPLIER
        val shieldBase = estimateRemainingShieldBuffer(fighter)

        // Bad-matchup penalty only: ineffective matchups can increase required damage, but effective matchups never reduce it.
        val armorPenalty = when (weapon.damageType) {
            DamageType.KINETIC -> 2f
            DamageType.FRAGMENTATION -> 4f
            else -> 1f
        }
        val shieldPenalty = when (weapon.damageType) {
            DamageType.HIGH_EXPLOSIVE -> 2f
            DamageType.FRAGMENTATION -> 4f
            else -> 1f
        }

        return hull + (armorBase * armorPenalty) + (shieldBase * shieldPenalty)
    }

    private fun estimateRemainingShieldBuffer(fighter: ShipAPI): Float {
        val shield = fighter.shield ?: return 0f
        val fluxTracker = fighter.fluxTracker ?: return 0f
        if (shield.type == ShieldAPI.ShieldType.NONE || shield.type == ShieldAPI.ShieldType.PHASE) return 0f
        if (fluxTracker.isOverloadedOrVenting) return 0f

        val shieldDamageTakenMult = fighter.mutableStats?.shieldDamageTakenMult?.modifiedValue ?: 1f
        val safeShieldDamageTakenMult = max(EPSILON, shieldDamageTakenMult)
        val remainingFluxCapacity = (fluxTracker.maxFlux - fluxTracker.currFlux).coerceAtLeast(0f)
        return remainingFluxCapacity / safeShieldDamageTakenMult
    }

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean = isValidTarget(entity)

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1f

    override fun shouldFire(solution: FiringSolution): Boolean = true

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false
}
