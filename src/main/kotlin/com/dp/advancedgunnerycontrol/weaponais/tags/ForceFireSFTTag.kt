package com.dp.advancedgunnerycontrol.weaponais.tags

import com.dp.advancedgunnerycontrol.utils.softFluxBelowThreshold
import com.dp.advancedgunnerycontrol.weaponais.FiringSolution
import com.fs.starfarer.api.combat.WeaponAPI

class ForceFireSFTTag(weapon: WeaponAPI, private val softFluxThreshold: Float) : WeaponAITagBase(weapon) {
    override fun computeTargetPriorityModifier(solution: FiringSolution): Float = 1f

    override fun shouldFire(solution: FiringSolution): Boolean = true

    override fun isBaseAiOverridable(): Boolean = false

    override fun avoidDebris(): Boolean = false

    override fun forceFire(solution: FiringSolution?, baseDecision: Boolean): Boolean {
        return baseDecision && (weapon.ship?.softFluxBelowThreshold(softFluxThreshold) ?: true)

    }
}