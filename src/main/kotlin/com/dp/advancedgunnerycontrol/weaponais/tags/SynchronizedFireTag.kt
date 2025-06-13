package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.isInRangeOf
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.WeaponAPI

/**
 * Fires as a volley when **all** weapons in the same group are
 *     • ready (cool-down == 0) **and**
 *     • in range of the current target (range × [factor]).
 *
 * While a volley is in progress **at least one weapon must still be able to
 * hit**; otherwise the volley is aborted.
 *
 * The tag is *stateless*: every decision is derived from current weapon fields.
 */
class SynchronizedFireTag(
    weapon: WeaponAPI,
    private val factor: Float = 1f          // 1 = 100 % nominal range
) : WeaponAITagBase(weapon) {

    private fun WeaponAPI.groupShouldFireAgainst(target: CombatEntityAPI?): Boolean {

        val guns = ship?.getWeaponGroupFor(this)?.weaponsCopy ?: return true
        if (target == null) return true

        var anyActive   = false    // at least one gun mid-shot
        var allReady    = true     // every gun cooldown == 0
        var allInRange  = true     // every gun can reach target
        var thisReady   = cooldownRemaining == 0f
        val location       = target.location

        for (g in guns) {
            if (g.isDisabled || g.ammo <= 0) continue

            val active = g.isFiring || g.isInBurst || g.chargeLevel > 0f
            val ready  = g.cooldownRemaining == 0f
            val inRange= g.isInRangeOf(location)

            if (active)    anyActive  = true
            if (!ready)    allReady   = false
            if (!inRange)  allInRange = false

            if (g === this) thisReady = ready          // local gun ready flag
        }

        return when {
            /* volley already under way ------------------------------------------------ */
            anyActive             -> thisReady                         // keep firing only if this gun is now ready
            /* no volley; start only if *all* guns meet both conditions ---------------- */
            else                  -> allInRange && allReady
        }
    }

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean =
        weapon.groupShouldFireAgainst(entity)

    override fun shouldFire(solution: FiringSolution): Boolean =
        weapon.groupShouldFireAgainst(solution.target)

    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1f
    override fun isBaseAiOverridable(): Boolean = true
    override fun avoidDebris(): Boolean = false
}


