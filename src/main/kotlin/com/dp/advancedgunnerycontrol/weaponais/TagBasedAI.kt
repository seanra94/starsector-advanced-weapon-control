package com.dp.advancedgunnerycontrol.weaponais

import com.dp.advancedgunnerycontrol.typesandvalues.Values
import com.dp.advancedgunnerycontrol.utils.InEngineTagStorage
import com.dp.advancedgunnerycontrol.weaponais.tags.WeaponAITagBase
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.AutofireAIPlugin
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import org.lazywizard.lazylib.combat.CombatUtils
import java.lang.ref.WeakReference

data class WeaponDecisionSnapshot(
    val weapon: WeaponAPI,
    val solution: FiringSolution?,
    val baseDecision: Boolean,
    val timestamp: Float
)

class TagBasedAI(baseAI: AutofireAIPlugin, tags: MutableList<WeaponAITagBase> = mutableListOf()) :
    SpecificAIPluginBase(baseAI) {

    var decisionSnapshot: WeaponDecisionSnapshot? = null
        private set

    var tags = listOf<WeaponAITagBase>()
        set(value) {
            unregisterTagsForEveryFrameAdvance(field)
            field = value
            registerTagsForEveryFrameAdvance(field)
        }

    init {
        this.tags = tags
    }

    override fun computeTargetPriority(solution: FiringSolution): Float {
        val basePriority = computeBasePriority(solution)
        val groupTargetPriority = basePriority *
                (tags.map { it.computeTargetPriorityModifierForGroupTargetChoice(solution) }
                    .reduceOrNull(Float::times) ?: 1.0f)
        tags.forEach { it.observeTargetPriority(solution, groupTargetPriority) }

        return basePriority *
                (tags.map { it.computeTargetPriorityModifier(solution) }.reduceOrNull(Float::times)
                    ?: 1.0f)
    }

    override fun getRelevenEntitiesOutOfRange(): List<CombatEntityAPI> {
        return tags.map { it.addFarAwayTargets() }.flatten()
    }

    override fun getRelevantEntitiesWithinRange(): List<CombatEntityAPI> {
        val ships = CombatUtils.getShipsWithinRange(weapon.location, weapon.range + 200f).filterNotNull()
        val missiles = CombatUtils.getMissilesWithinRange(weapon.location, weapon.range + 200f).filterNotNull()
        val entities: MutableList<CombatEntityAPI> = (ships + missiles) as MutableList<CombatEntityAPI>

        return entities.toSet().filter { entity -> tags.all { it.isValidTarget(entity) } }
    }

    override fun isBaseAITargetValid(ship: ShipAPI?, missile: MissileAPI?): Boolean {
        val tgt = ship as? CombatEntityAPI ?: missile as? CombatEntityAPI ?: return false
        return tags.all { it.isBaseAiValid(tgt) }
    }

    override fun isBaseAIOverwritable(): Boolean {
        return tags.any { it.isBaseAiOverridable() }
    }

    override fun isValid(): Boolean = true

    override fun shouldFire(): Boolean {
        val baseDecision = super.shouldFire()
        if (tags.any { it.forceFire(solution, baseDecision) }) return true
        val sol = tags.firstNotNullOfOrNull { it.overrideFiringSolution() } ?: solution ?: return false
        tags.forEach { it.observeFiringDecision(sol, baseDecision) }
        if (!baseDecision && tags.none { it.overrideBaseFireDecision(sol, baseDecision) }) return false
        val synchronizedReleaseActive = tags.any { it.isSynchronizedReleaseActive(sol) }
        val shouldFire = tags.all {
            if (synchronizedReleaseActive) {
                it.shouldFireDuringSynchronizedRelease(sol)
            } else {
                it.shouldFire(sol)
            }
        }
        if (shouldFire) {
            tags.forEach { it.onFireAllowed(sol) }
        }
        return shouldFire
    }

    override fun shouldConsiderNeutralsAsFriendlies(): Boolean {
        return tags.any { it.avoidDebris() }
    }

    override fun advance(p0: Float) {
        super.advance(p0)
        decisionSnapshot = WeaponDecisionSnapshot(
            weapon,
            solution,
            super.shouldFire(),
            Global.getCombatEngine()?.getTotalElapsedTime(false) ?: 0f
        )
        tags.forEach { if (!it.advanceWhenTurnedOff) it.advance() }
        tags.firstNotNullOfOrNull { it.overrideFiringSolution() }?.let { solution = it }
    }

    companion object {
        fun unregisterTagsForEveryFrameAdvance(tags: List<WeaponAITagBase>) {
            Global.getCombatEngine()?.let { engine ->
                if (!engine.customData.containsKey(Values.CUSTOM_ENGINE_TAGS_KEY)) {
                    return
                }
                (engine.customData[Values.CUSTOM_ENGINE_TAGS_KEY] as? InEngineTagStorage)?.tags?.removeAll {
                    tags.contains(it.get())
                }
            }
        }

        fun registerTagsForEveryFrameAdvance(tags: List<WeaponAITagBase>) {
            Global.getCombatEngine()?.let { engine ->
                if (!engine.customData.containsKey(Values.CUSTOM_ENGINE_TAGS_KEY)) {
                    engine.customData[Values.CUSTOM_ENGINE_TAGS_KEY] = InEngineTagStorage()
                }
                (engine.customData[Values.CUSTOM_ENGINE_TAGS_KEY] as? InEngineTagStorage)?.tags?.addAll(tags.filter {
                    it.advanceWhenTurnedOff
                }.map {
                    WeakReference(it)
                })
            }
        }

        fun getTagsRegisteredForEveryFrameAdvancement(): List<WeaponAITagBase> {
            (Global.getCombatEngine()?.customData?.get(Values.CUSTOM_ENGINE_TAGS_KEY) as? InEngineTagStorage)?.let { store ->
                return store.tags.mapNotNull { it.get() }
            }
            return listOf()
        }
    }

}
