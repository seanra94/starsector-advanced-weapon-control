package com.dp.advancedgunnerycontrol.weaponais

import com.dp.advancedgunnerycontrol.weaponais.suffixes.SuffixBase
import com.fs.starfarer.api.combat.AutofireAIPlugin
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import org.lazywizard.lazylib.combat.CombatUtils
import org.lwjgl.util.vector.Vector2f

class TargetShieldsAI (baseAI: AutofireAIPlugin, suffix: SuffixBase) : SpecificAIPluginBase(baseAI, suffix = suffix)  {
    override fun computeTargetPriority(entity: CombatEntityAPI, predictedLocation: Vector2f): Float {
        val tgtShip = (entity as? ShipAPI) ?: return computeBasePriority(entity, predictedLocation)
        return 1f/(computeShieldFactor(tgtShip) + 0.0001f)*computeBasePriority(entity, predictedLocation)
    }

    override fun shouldFire(): Boolean {
        if (!super.shouldFire()) return false
        val tgtShip = (targetEntity as? ShipAPI) ?: return false
        return computeShieldFactor(tgtShip) > 1.2f
    }

    override fun getRelevantEntitiesWithinRange(): List<CombatEntityAPI> {
        return CombatUtils.getShipsWithinRange(weapon.location, weapon.range + 200f).filterNotNull()
    }

    override fun isBaseAITargetValid(ship: ShipAPI?, missile: MissileAPI?): Boolean = false

    override fun isBaseAIOverwritable(): Boolean = true

    override fun isValid(): Boolean = true
}