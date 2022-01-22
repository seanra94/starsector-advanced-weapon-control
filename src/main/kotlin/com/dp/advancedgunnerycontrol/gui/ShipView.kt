package com.dp.advancedgunnerycontrol.gui

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.*
import org.lwjgl.opengl.GL11

class ShipView : CustomUIPanelPlugin {
    private var pos : PositionAPI? = null
    private val buttons : MutableList<ButtonBase<*>> = mutableListOf()
    override fun positionChanged(pos: PositionAPI?) {
        pos?.let {
            this.pos = it
        }
    }

    override fun renderBelow(alpha: Float) {}

    override fun render(alpha: Float) {
        pos?.let { p ->
            GL11.glPushMatrix()
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glColor4f(0f, 150f / 255f, 90f / 255f, 0.6f * alpha)
            GL11.glBegin(GL11.GL_LINES)
            GL11.glLineWidth(10f)

            GL11.glVertex2f(p.x, p.y + p.height)
            GL11.glVertex2f(p.x + p.width, p.y + p.height)
            GL11.glVertex2f(p.x + p.width, p.y + p.height)
            GL11.glVertex2f(p.x + p.width, p.y)
            GL11.glVertex2f(p.x + p.width, p.y)
            GL11.glVertex2f(p.x, p.y)
            GL11.glVertex2f(p.x, p.y)
            GL11.glVertex2f(p.x, p.y + p.height)

            GL11.glEnd()
            GL11.glPopMatrix()
        }
    }

    fun addModeButtonGroup(group: Int, ship: FleetMemberAPI, tooltip: TooltipMakerAPI){
        buttons.addAll(ModeButton.createModeButtonGroup(ship, group, tooltip))
    }

    fun addSuffixButtons(group: Int, ship: FleetMemberAPI, tooltip: TooltipMakerAPI){
        buttons.addAll(SuffixButton.createModeButtonGroup(ship, group, tooltip))
    }

    fun addShipModeButtonGroup(ship: FleetMemberAPI, panel: CustomPanelAPI, position: UIComponentAPI){
        buttons.addAll(ShipModeButton.createModeButtonGroup(ship, panel, position))
    }

    override fun advance(t: Float) {
        buttons.forEach { it.executeCallbackIfChecked() }
    }

    override fun processInput(events: MutableList<InputEventAPI>?) {}

}