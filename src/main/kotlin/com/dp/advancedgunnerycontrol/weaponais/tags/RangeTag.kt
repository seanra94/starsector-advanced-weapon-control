package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.dp.advancedgunnerycontrol.weaponais.isInRangeOf
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.WeaponAPI
import org.lazywizard.lazylib.ext.minus
import org.lwjgl.util.vector.Vector2f

class RangeTag(weapon: WeaponAPI, private val threshold: Float) : WeaponAITagBase(weapon) {
    override fun computeTargetPriorityModifier(solution: FiringSolution): Float {
        return if (weapon.isInRangeOf(solution.target.location, threshold)) 1.0f else 100f
    }

    override fun shouldFire(solution: FiringSolution): Boolean {
        return weapon.isInRangeOf(solution.target.location, threshold)
    }

    override fun isBaseAiOverridable(): Boolean = true

    override fun avoidDebris(): Boolean = false

    override fun isBaseAiValid(entity: CombatEntityAPI): Boolean {
        return weapon.isInRangeOf(entity.location, threshold)
    }

}