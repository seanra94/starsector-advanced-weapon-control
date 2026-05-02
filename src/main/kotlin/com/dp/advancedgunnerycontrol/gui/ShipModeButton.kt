package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.*
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.max

class ShipModeButton(var ship: FleetMemberAPI, mode: ShipModes, button: ButtonAPI) :
    ButtonBase<ShipModes>(mode, button, false) {

    companion object {
        var campaignShipModeSelectionVersion = 0
            private set

        fun createModeButtonGroup(
            ship: FleetMemberAPI,
            panel: CustomPanelAPI,
            position: UIComponentAPI
        ): List<ShipModeButton> {
            val toReturn = mutableListOf<ShipModeButton>()
            val elementList = mutableListOf<TooltipMakerAPI>()
            Settings.getCurrentShipModes().forEach {
                val tooltip = panel.createUIElement(130f, 30f, false)
                toReturn.add(
                    ShipModeButton(
                        ship, it, tooltip.addAreaCheckbox(
                            shipModeToString[it],
                            it,
                            Misc.getBasePlayerColor(),
                            Misc.getDarkPlayerColor(),
                            Misc.getBrightPlayerColor(),
                            130f,
                            18f,
                            3f
                        )
                    )
                )
                tooltip.addTooltipToPrevious(
                    AGCGUI.makeTooltip(detailedShipModeDescriptions[it] ?: ""),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                if (elementList.isEmpty()) {
                    panel.addUIElement(tooltip).belowLeft(position, 5f)
                } else {
                    panel.addUIElement(tooltip).rightOfTop(elementList.last(), 12f)
                }
                elementList.add(tooltip)
            }

            toReturn.forEach {
                it.sameGroupButtons = toReturn
                it.updateIfCheckedBasedOnData()
            }
            return toReturn
        }

        fun createCampaignModeButtonGroup(
            ship: FleetMemberAPI,
            panel: CustomPanelAPI,
        ): List<ShipModeButton> {
            val modes = Settings.getCurrentShipModes()
            val columns = if (
                modes.size > 1 &&
                panel.position.width >= (CampaignGuiStyle.SHIP_MODE_ITEM_MIN_WIDTH * 2f + CampaignGuiStyle.SHIP_MODE_ITEM_HGAP)
            ) 2 else 1
            val itemWidth =
                (panel.position.width - CampaignGuiStyle.SHIP_MODE_ITEM_HGAP * (columns - 1)) / columns
            val rows = max(1, ceil(max(modes.size, 1).toFloat() / columns.toFloat()).toInt())
            val requiredItemHeight = modes.map { mode ->
                val label = truncateLabel(shipModeToString[mode] ?: defaultShipMode, itemWidth, 24f)
                computeWrappedLabelLayout(
                    text = label,
                    rowWidth = itemWidth - 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    minButtonHeight = CampaignGuiStyle.SHIP_MODE_ITEM_HEIGHT,
                    horizontalPadding = 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    verticalPadding = 2f * CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    maxLines = 1
                ).rowHeight
            }.maxOrNull() ?: CampaignGuiStyle.SHIP_MODE_ITEM_HEIGHT
            val itemHeight = if (rows <= 0) {
                requiredItemHeight
            } else {
                minOf(
                    requiredItemHeight,
                    (panel.position.height - CampaignGuiStyle.SHIP_MODE_ITEM_VGAP * (rows - 1)) / rows
                )
            }
            val toReturn = mutableListOf<ShipModeButton>()

            modes.forEachIndexed { index, mode ->
                val label = truncateLabel(shipModeToString[mode] ?: defaultShipMode, itemWidth, 24f)
                val toggleColors = CampaignGuiStyle.toggleableCheckboxColors()
                val itemPanel = panel.createCustomPanel(
                    itemWidth,
                    itemHeight,
                    CampaignPanelPlugin(
                        CampaignContainerType.ITEM,
                        fillColor = null
                    )
                )
                panel.addComponent(itemPanel)
                itemPanel.position.inTL(
                    (index % columns) * (itemWidth + CampaignGuiStyle.SHIP_MODE_ITEM_HGAP),
                    (index / columns) * (itemHeight + CampaignGuiStyle.SHIP_MODE_ITEM_VGAP)
                )
                val inner = itemPanel.createUIElement(
                    itemWidth,
                    itemHeight,
                    false
                )
                toReturn.add(
                    ShipModeButton(
                        ship,
                        mode,
                        inner.addAreaCheckbox(
                            "",
                            mode,
                            toggleColors.base,
                            toggleColors.bg,
                            toggleColors.bright,
                            itemWidth,
                            itemHeight,
                            0f
                        )
                    )
                )
                inner.addTooltipToPrevious(
                    AGCGUI.makeTooltip(detailedShipModeDescriptions[mode] ?: ""),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                itemPanel.addUIElement(inner).inTL(CampaignGuiStyle.ITEM_HIGHLIGHT_X_OFFSET, 0f)
                renderCenteredTagLabel(
                    panel = itemPanel,
                    text = label,
                    width = itemWidth,
                    height = itemHeight - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    centerRegionOffsetX = CampaignGuiStyle.ITEM_HIGHLIGHT_X_OFFSET,
                    centerRegionWidth = itemWidth
                )
            }

            toReturn.forEach {
                it.sameGroupButtons = toReturn
                it.updateIfCheckedBasedOnData()
            }
            return toReturn
        }
    }

    override fun executeCallbackIfChecked() {
        if (!active && button.isChecked) {
            check()
            campaignShipModeSelectionVersion++
            sameGroupButtons.forEach { (it as? ShipModeButton)?.updateIfCheckedBasedOnData() }
        } else if (active && !button.isChecked) {
            removePersistentShipMode(ship.id, AGCGUI.storageIndex, shipModeToString[associatedValue] ?: defaultShipMode)
            uncheck()
            campaignShipModeSelectionVersion++
            sameGroupButtons.forEach { (it as? ShipModeButton)?.updateIfCheckedBasedOnData() }
        }
        button.isChecked = active
    }

    private fun updateIfCheckedBasedOnData() {
        if (loadPersistedShipModes(ship.id, AGCGUI.storageIndex).contains(shipModeToString[associatedValue] ?: defaultShipMode)) {
            check()
        } else {
            uncheck()
        }
    }

    override fun onActivate() {
        val id = ship.id
        val index = AGCGUI.storageIndex
        if (associatedValue == ShipModes.DEFAULT) {
            persistShipModes(id, index, emptyList())
        }
        if (associatedValue != ShipModes.DEFAULT) {
            removePersistentShipMode(id, index, defaultShipMode)
        }
        addPersistentShipMode(id, index, shipModeToString[associatedValue] ?: defaultShipMode)
    }
}
