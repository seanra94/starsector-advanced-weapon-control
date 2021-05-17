package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.typesandvalues.Suffixes
import com.dp.advancedgunnerycontrol.typesandvalues.defaultSuffixString
import com.dp.advancedgunnerycontrol.typesandvalues.suffixDescriptions
import com.dp.advancedgunnerycontrol.typesandvalues.suffixFromString
import com.dp.advancedgunnerycontrol.utils.SuffixStorage
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Color

class SuffixButton(ship: FleetMemberAPI, group : Int, suffix : Suffixes, button: ButtonAPI) : ButtonBase<Suffixes>(ship, group, suffix, button) {

    companion object{
        private var storage = SuffixStorage[AGCGUI.storageIndex]

        public fun createModeButtonGroup(ship: FleetMemberAPI, group: Int, tooltip: TooltipMakerAPI) : List<SuffixButton>{
            storage = SuffixStorage[AGCGUI.storageIndex]
            val toReturn = mutableListOf<SuffixButton>()
            var isSomethingChecked = false
            Suffixes.values().forEach {
                toReturn.add(SuffixButton(ship, group, it, tooltip.addAreaCheckbox(suffixDescriptions[it], it,
                    Color.BLUE, Color.BLUE, Color.WHITE, 160f, 24f, 3f)))
                if(suffixFromString[storage.modesByShip[ship.id]?.get(group)] == it){
                    toReturn.last().check()
                    isSomethingChecked = true
                }
            }
            if(!isSomethingChecked) toReturn.firstOrNull()?.check()
            toReturn.forEach {
                it.sameGroupButtons = toReturn
            }
            return toReturn
        }
    }

    override fun onActivate() {
        if(storage.modesByShip[ship.id] == null){
            storage.modesByShip[ship.id] = mutableMapOf()
        }
        storage.modesByShip[ship.id]?.let { it[group] = (suffixDescriptions[associatedValue] ?: defaultSuffixString) }
    }
}