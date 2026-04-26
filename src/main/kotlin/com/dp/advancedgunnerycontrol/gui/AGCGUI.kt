package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.gui.actions.ExitAction
import com.dp.advancedgunnerycontrol.gui.actions.GUIAction
import com.dp.advancedgunnerycontrol.gui.suggesttaggui.SuggestedTagGui
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.Values
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import kotlin.math.min

class AGCGUI : InteractionDialogPlugin {
    companion object {
        var storageIndex = Values.storageIndex //

        fun makeTooltip(description: String): TooltipMakerAPI.TooltipCreator {
            return object : TooltipMakerAPI.TooltipCreator {
                override fun isTooltipExpandable(p0: Any?): Boolean = false
                override fun getTooltipWidth(p0: Any?): Float = min(description.length.toFloat() * 7f, 850f)
                override fun createTooltip(tooltip: TooltipMakerAPI?, p1: Boolean, p2: Any?) {
                    tooltip?.addPara(description, CampaignGuiStyle.TOOLTIP_TEXT_COLOR, 5f)
                }
            }
        }
    }

    private var attributes = GUIAttributes()
    private var campaignEditorPanel: CampaignShipEditorPanelPlugin? = null
    private var pendingShipEditorOpen = false
    private var pendingShipEditorDelay = 0f

    private fun addAction(action: GUIAction) {
        attributes.options?.addOption(action.getName(), action, action.getTooltip())
        action.getShortcut()?.let {
            attributes.options?.setShortcut(action, it, false, false, false, false)
        }
    }

    override fun init(dialog: InteractionDialogAPI?) {
        storageIndex = 0
        pendingShipEditorOpen = false
        pendingShipEditorDelay = 0f
        attributes.init(dialog)
        if (!Settings.enablePersistentModes()) {
            attributes.text?.addPara("Persistent Storage has been disabled in the settings.")
            attributes.text?.addPara("Enable it to use this GUI")
            addAction(ExitAction(attributes))
            return
        }
        displayOptions()
    }

    override fun optionSelected(str: String?, data: Any?) {
        (data as? GUIAction)?.execute()
        displayOptions()
        return
    }

    private fun displayOptions() {
        attributes.options?.clearOptions()
        when (attributes.level) {
            Level.TOP -> displayFleetOptions()
            Level.SHIP -> openShipEditor()
        }
    }

    private fun displayFleetOptions() {
        pendingShipEditorOpen = false
        pendingShipEditorDelay = 0f
        clear()
        attributes.dialog?.showTextPanel()
        attributes.dialog?.showVisualPanel()
        attributes.dialog?.showFleetMemberPickerDialog("Pick a ship to adjust weapon modes & suffixes for",
            "Confirm",
            "Exit",
            5,
            6,
            100f,
            true,
            false,
            Global.getSector().playerFleet.membersWithFightersCopy.filter { !it.isFighterWing },
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(selected: MutableList<FleetMemberAPI>?) {
                    selected?.firstOrNull()?.let {
                        attributes.ship = it
                        attributes.level = Level.SHIP
                        pendingShipEditorOpen = true
                        pendingShipEditorDelay = 0.05f
                        Global.getLogger(AGCGUI::class.java).info(
                            "[AGC_CAMPAIGN_UI] queued ship editor open ship=${it.shipName} hull=${it.variant?.hullVariantId}"
                        )
                        return
                    } ?: attributes.dialog?.dismiss()
                }

                override fun cancelledFleetMemberPicking() {
                    attributes.dialog?.dismiss()
                }
            })
    }

    private fun openShipEditor() {
        clear()
        attributes.options?.clearOptions()
        attributes.dialog?.hideTextPanel()
        attributes.dialog?.hideVisualPanel()
        attributes.dialog?.setPromptText("")
        Global.getLogger(AGCGUI::class.java).info(
            "[AGC_CAMPAIGN_UI] openShipEditor ship=${attributes.ship?.shipName} hull=${attributes.ship?.variant?.hullVariantId}"
        )
        campaignEditorPanel = CampaignShipEditorPanelPlugin(attributes) {
            attributes.level = Level.TOP
            displayFleetOptions()
        }
        attributes.dialog?.showCustomVisualDialog(
            Global.getSettings().screenWidth.toFloat(),
            Global.getSettings().screenHeight.toFloat(),
            CampaignShipEditorDialogDelegate(campaignEditorPanel ?: return)
        )
    }

    private fun clear() {
        attributes.text?.clear()
        attributes.visualPanel?.fadeVisualOut()
    }

    override fun optionMousedOver(optionString: String?, optionData: Any?) {}
    override fun advance(p0: Float) {
        if (!pendingShipEditorOpen) return
        pendingShipEditorDelay -= p0
        if (pendingShipEditorDelay > 0f) return
        pendingShipEditorOpen = false
        displayOptions()
    }

    override fun backFromEngagement(p0: EngagementResultAPI?) {}
    override fun getContext(): Any? = null
    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? = null
}
