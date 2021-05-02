package com.dp.advancedgunnerycontrol.weaponais

import com.fs.starfarer.api.combat.*
import org.lwjgl.util.vector.Vector2f

class NoFighterAIPlugin(baseAI: AutofireAIPlugin) : SpecificAIPluginBase(baseAI, false) {
    override fun computeTargetPriority(entity: CombatEntityAPI, predictedLocation: Vector2f): Float {
        return 0f
    }

    override fun getRelevantEntitiesWithinRange(): List<CombatEntityAPI> {
        return emptyList()
    }

    override fun isTargetValid(ship: ShipAPI?, missile: MissileAPI?): Boolean {
        return ship?.let { !it.isFighter } ?: return true
    }

    override fun isValid(): Boolean {
        return true
    }
}