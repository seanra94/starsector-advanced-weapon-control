package com.dp.advancedgunnerycontrol.gui.actions

import com.dp.advancedgunnerycontrol.gui.AGCGUI
import com.dp.advancedgunnerycontrol.gui.GUIAttributes
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.ShipModeStorage
import com.dp.advancedgunnerycontrol.utils.loadPersistentTags
import com.dp.advancedgunnerycontrol.utils.persistTags
import com.fs.starfarer.api.Global

class CopyLoadoutAction(attributes: GUIAttributes) : GUIAction(attributes) {
    override fun execute() {
        val (allLoadouts, wholeFleet) = modifierKeys()
        executeWithModifiers(allLoadouts = allLoadouts, wholeFleet = wholeFleet)
    }

    fun executeWithModifiers(allLoadouts: Boolean, wholeFleet: Boolean) {
        val ships = if (wholeFleet) {
            Global.getSector().playerFleet.membersWithFightersCopy.filterNot { m -> m.isFighterWing }.filterNotNull()
        } else {
            attributes.ship?.let { listOf(it) } ?: emptyList()
        }
        val destinationLoadoutIndexes = if (allLoadouts) {
            (0 until Settings.maxLoadouts()).toList()
        } else {
            listOf(AGCGUI.storageIndex)
        }

        ships.forEach { ship ->
            destinationLoadoutIndexes.forEach { destinationLoadoutIndex ->
                val sourceLoadoutIndex =
                    if (destinationLoadoutIndex == 0) Settings.maxLoadouts() - 1 else destinationLoadoutIndex - 1

                ShipModeStorage[destinationLoadoutIndex].modesByShip[ship.id] =
                    (ShipModeStorage[sourceLoadoutIndex].modesByShip[ship.id]?.toMutableMap() ?: mutableMapOf())

                for (i in 0 until (ship.variant?.weaponGroups?.size ?: 0)) {
                    val sourceTags = loadPersistentTags(ship.id, ship, i, sourceLoadoutIndex)
                    persistTags(ship.id, ship, i, destinationLoadoutIndex, sourceTags)
                }
            }
        }
    }

    override fun getTooltip(): String {
        return "Copy loadout #${lastIndex() + 1} into current loadout." +
                "\n$fleetBoilerplateText"
    }

    override fun getName(): String = "Copy previous loadout" + nameSuffix(allLoadouts = false)
}
