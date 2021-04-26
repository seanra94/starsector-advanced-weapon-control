package com.dp.advancedgunnerycontrol.weaponais

import com.dp.advancedgunnerycontrol.enums.FireMode
import com.fs.starfarer.api.combat.AutofireAIPlugin
import com.fs.starfarer.api.combat.MissileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI

import org.lwjgl.util.vector.Vector2f



class AdjustableAIPlugin constructor(private var baseAI: AutofireAIPlugin)
    : AutofireAIPlugin {
    var fireMode = FireMode.DEFAULT
        set(value) {
            field = value
            activeAI = when(value){
                FireMode.DEFAULT -> baseAI
                FireMode.PD -> pdAI
                FireMode.MISSILE -> missileAI
                FireMode.FIGHTER -> fighterAI
                FireMode.NO_FIGHTERS -> noFighterAI
            }
            if(isInvalid(activeAI)) activeAI = baseAI
        }

    private val fighterAI = AdvancedFighterAIPlugin(baseAI)
    private val pdAI = PDAIPlugin(baseAI)
    private val missileAI = AdvancedMissileAIPlugin(baseAI)
    private val noFighterAI = NoFighterAIPlugin(baseAI)
    private var activeAI = baseAI

    override fun advance(p0: Float) = activeAI.advance(p0)

    override fun shouldFire(): Boolean = activeAI.shouldFire()

    override fun forceOff() = activeAI.forceOff()

    override fun getTarget(): Vector2f? = activeAI.target

    override fun getTargetShip(): ShipAPI? = activeAI.targetShip

    override fun getWeapon(): WeaponAPI? = activeAI.weapon

    override fun getTargetMissile(): MissileAPI? = activeAI.targetMissile
}