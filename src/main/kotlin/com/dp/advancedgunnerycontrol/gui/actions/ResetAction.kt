package com.dp.advancedgunnerycontrol.gui.actions

import com.dp.advancedgunnerycontrol.gui.AGCGUI
import com.dp.advancedgunnerycontrol.gui.GUIAttributes
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.ShipModeStorage
import com.dp.advancedgunnerycontrol.utils.persistTags
import org.lwjgl.input.Keyboard

class ResetAction(attributes: GUIAttributes) : GUIAction(attributes) {
    fun executeWithModifiers(allLoadouts: Boolean, wholeFleet: Boolean) {
        val loadouts = if (allLoadouts) {
            (0 until Settings.maxLoadouts()).toList()
        } else {
            listOf(AGCGUI.storageIndex)
        }
        val ships = if (wholeFleet) {
            com.fs.starfarer.api.Global.getSector().playerFleet.membersWithFightersCopy
                .filterNot { it.isFighterWing }
                .filterNotNull()
        } else {
            attributes.ship?.let { listOf(it) } ?: emptyList()
        }

        loadouts.forEach { index ->
            ships.forEach { ship ->
                ShipModeStorage[index].modesByShip[ship.id]?.clear()
                for(i in 0 until (ship.variant?.weaponGroups?.size ?: 0)){
                    persistTags(ship.id, ship, i, index, emptyList())
                }
            }
        }
    }

    override fun execute() {
        executeWithModifiers(
            allLoadouts = isAllLoadoutsKeyHeld(),
            wholeFleet = isWholeFleetKeyHeld()
        )
    }

    override fun getTooltip(): String {
        return "Resets all fire modes, fire modes and suffixes.\n$modifiersBoilerplateText " +
                "\nIf storage mode is WeaponCompositionGlobal, this action might affect other ships, too!"
    }

    override fun getName(): String = "Reset" + nameSuffix()

    override fun getShortcut(): Int = Keyboard.KEY_DELETE
}
