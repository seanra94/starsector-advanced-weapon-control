package com.dp.advancedgunnerycontrol.gui.suggesttaggui

import com.dp.advancedgunnerycontrol.gui.AGCGUI
import com.dp.advancedgunnerycontrol.gui.CampaignContainerType
import com.dp.advancedgunnerycontrol.gui.CampaignGuiStyle
import com.dp.advancedgunnerycontrol.gui.CampaignPanelPlugin
import com.dp.advancedgunnerycontrol.gui.GUIShower
import com.dp.advancedgunnerycontrol.gui.WrappedLabelLayout
import com.dp.advancedgunnerycontrol.gui.addAgcText
import com.dp.advancedgunnerycontrol.gui.applyAgcDefaultTextStyle
import com.dp.advancedgunnerycontrol.gui.computeWrappedLabelLayout
import com.dp.advancedgunnerycontrol.settings.LunaSettingHandler
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.Values
import com.dp.advancedgunnerycontrol.utils.invokeMethodByName
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max

class SuggestedTagGui : InteractionDialogPlugin {
    private enum class SuggestedActionStyle {
        DEFAULT,
        GREEN,
        RED,
    }

    private enum class PendingDangerousSuggestedAction {
        BACKUP,
        RESTORE,
    }

    companion object {
        private const val SECTION_HEADER_HEIGHT = 20f
        private const val ACTION_ROW_GAP = 2f
        private const val ACTION_ROW_PADDING = 4f
        private const val ACTION_LABEL_APPROX_CHAR_WIDTH = 6.8f
        private const val ACTION_LABEL_LINE_HEIGHT = 15f
        private const val ACTION_LABEL_MAX_LINES = 2
        private val ACTION_SHORTCUT_HIGHLIGHTS = listOf(
            "[TAB]",
            "[DELETE]",
            "[ESCAPE]",
            "[LEFT]",
            "[LEFT ARROW]",
            "[D]",
            "[A]",
            "[RIGHT]",
            "[RIGHT ARROW]",
            "[F]",
        )
    }

    private data class SuggestedGuiAction(
        val name: String,
        val shortcuts: List<Int> = emptyList(),
        val tooltip: String = "",
        val rebuildAfter: Boolean = true,
        val active: Boolean = false,
        val style: SuggestedActionStyle = SuggestedActionStyle.DEFAULT,
        val callback: () -> Unit,
    )

    private inner class SuggestedTagDialogDelegate(
        private val panelPlugin: SuggestedTagPanelPlugin,
    ) : CustomVisualDialogDelegate {
        override fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks) {
            panelPlugin.init(panel, callbacks)
        }

        override fun getCustomPanelPlugin(): CustomUIPanelPlugin = panelPlugin
        override fun getNoiseAlpha(): Float = 0f
        override fun advance(amount: Float) {}
        override fun reportDismissed(option: Int) {}
    }

    private inner class SuggestedTagPanelPlugin : BaseCustomUIPanelPlugin() {
        private val log = Global.getLogger(SuggestedTagPanelPlugin::class.java)
        private var panel: CustomPanelAPI? = null
        private var callbacks: CustomVisualDialogDelegate.DialogCallbacks? = null
        private var contentPanel: CustomPanelAPI? = null
        private var view: SuggestedTagGuiView? = null
        private var buttonListenerPanel: CustomPanelAPI? = null
        private val actionButtons = linkedMapOf<ButtonAPI, SuggestedGuiAction>()
        private var currentActions: List<SuggestedGuiAction> = emptyList()
        private var persistedTagScrollOffsets: Map<String, Int> = emptyMap()

        fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks) {
            this.panel = panel
            this.callbacks = callbacks
            this.buttonListenerPanel = Global.getSettings().createCustom(0f, 0f, this)
            rebuild()
        }

        override fun advance(amount: Float) {
            if (view?.shouldRegenerate() == true || weaponListView.hasChanged()) {
                rebuild()
            }
        }

        override fun processInput(events: MutableList<InputEventAPI>) {
            view?.processInput(events)
            val shortcutAction = events.firstNotNullOfOrNull { event ->
                if (event.isConsumed || !event.isKeyDownEvent) return@firstNotNullOfOrNull null
                if (event.eventValue == Keyboard.KEY_ESCAPE) {
                    event.consume()
                    if (isFilterView) {
                        isFilterView = false
                        return@firstNotNullOfOrNull SuggestedGuiAction("Back") {}
                    }
                    return@firstNotNullOfOrNull SuggestedGuiAction("Back", rebuildAfter = false) { backToAgc(callbacks) }
                }
                currentActions.firstOrNull { event.eventValue in it.shortcuts }?.also { event.consume() }
            }
            shortcutAction?.let(::executeAction)
        }

        override fun buttonPressed(buttonId: Any?) {
            (buttonId as? SuggestedGuiAction)?.let(::executeAction)
        }

        private fun executeAction(action: SuggestedGuiAction) {
            action.callback()
            if (action.rebuildAfter) rebuild()
        }

        private fun rebuild() {
            val root = panel ?: return
            try {
                persistedTagScrollOffsets = view?.captureTagScrollOffsets() ?: persistedTagScrollOffsets
                contentPanel?.let(root::removeComponent)
                contentPanel = null
                view = null
                actionButtons.clear()
                currentActions = buildActions(callbacks)

                val width = max(300f, root.position.width - 2f * CampaignGuiStyle.MAIN_PADDING)
                val height = max(300f, root.position.height - 2f * CampaignGuiStyle.MAIN_PADDING)
                val content = root.createCustomPanel(
                    width,
                    height,
                    SuggestedTagGuiView(weaponListView, persistedTagScrollOffsets)
                )
                root.addComponent(content).inTL(CampaignGuiStyle.MAIN_PADDING, CampaignGuiStyle.MAIN_PADDING)
                contentPanel = content
                view = content.plugin as? SuggestedTagGuiView
                view?.buildIn(content, ::buildOptionsPanel)
            } catch (ex: Throwable) {
                log.error("[AGC_SUGGESTED_UI] rebuild failed", ex)
                buildFallback(root, ex)
            }
        }

        private fun buildFallback(root: CustomPanelAPI, ex: Throwable) {
            contentPanel?.let(root::removeComponent)
            contentPanel = null
            view = null
            actionButtons.clear()

            val width = max(500f, root.position.width - 2f * CampaignGuiStyle.MAIN_PADDING)
            val height = 220f
            val fallback = root.createCustomPanel(width, height, CampaignPanelPlugin(CampaignContainerType.OPTIONS))
            root.addComponent(fallback).inTL(CampaignGuiStyle.MAIN_PADDING, CampaignGuiStyle.MAIN_PADDING)

            val element = fallback.createUIElement(width - 16f, height - 16f, false)
            element.addSectionHeading("AGC Suggested Tags UI Error", Alignment.MID, 0f)
            element.applyAgcDefaultTextStyle()
            element.addPara(
                "The suggested-tags editor failed to build. Press [Esc] to return.",
                6f,
                Misc.getNegativeHighlightColor(),
                "[Esc]"
            )
            element.addAgcText("Reason: ${ex.javaClass.simpleName}: ${ex.message ?: "no message"}", 6f)
            fallback.addUIElement(element).inTL(8f, 8f)
        }

        private fun buildOptionsPanel(panel: CustomPanelAPI) {
            val width = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
            val headerPanel = panel.createCustomPanel(width, SECTION_HEADER_HEIGHT, CampaignPanelPlugin(CampaignContainerType.HEADER))
            panel.addComponent(headerPanel)
            headerPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, CampaignGuiStyle.PANEL_PADDING)
            val header = headerPanel.createUIElement(width, SECTION_HEADER_HEIGHT, false)
            header.addSectionHeading("Options", Alignment.MID, 0f)
            headerPanel.addUIElement(header).inTL(0f, 0f)

            var currentTop = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT
            currentActions.forEach { action ->
                val labelLayout = actionLabelLayout(action, width)
                val label = labelLayout.wrappedText
                val rowHeight = labelLayout.rowHeight
                val isGreen = action.active || action.style == SuggestedActionStyle.GREEN
                val isRed = action.style == SuggestedActionStyle.RED
                val rowFillColor = when {
                    isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_BACKGROUND_COLOR
                    isGreen -> CampaignGuiStyle.ACTIVE_GREEN_BACKGROUND_COLOR
                    else -> null
                }
                val itemPanel = panel.createCustomPanel(
                    width,
                    rowHeight,
                    CampaignPanelPlugin(CampaignContainerType.ITEM, fillColor = rowFillColor)
                )
                panel.addComponent(itemPanel)
                itemPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, currentTop)

                val inner = itemPanel.createUIElement(width, rowHeight, false)
                val baseColor = when {
                    isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_BACKGROUND_COLOR
                    isGreen -> CampaignGuiStyle.ACTIVE_GREEN_BACKGROUND_COLOR
                    else -> Misc.getBasePlayerColor()
                }
                val darkColor = when {
                    isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_DARK_COLOR
                    isGreen -> CampaignGuiStyle.ACTIVE_GREEN_DARK_COLOR
                    else -> Misc.getDarkPlayerColor()
                }
                val brightColor = when {
                    isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_BRIGHT_COLOR
                    isGreen -> CampaignGuiStyle.ACTIVE_GREEN_BRIGHT_COLOR
                    else -> Misc.getBrightPlayerColor()
                }
                val button = inner.addAreaCheckbox(
                    "",
                    action,
                    baseColor,
                    darkColor,
                    brightColor,
                    width,
                    rowHeight,
                    0f
                )
                button.isChecked = action.active
                inner.addTooltipToPrevious(
                    AGCGUI.makeTooltip(action.tooltip),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                bindButton(button)
                itemPanel.addUIElement(inner).inTL(CampaignGuiStyle.ITEM_HIGHLIGHT_X_OFFSET, 0f)

                val textPanel = itemPanel.createUIElement(
                    width - 2f * ACTION_ROW_PADDING,
                    rowHeight - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    false
                )
                val labelText = label
                if (isRed || isGreen) {
                    addActionLabel(textPanel, labelText, CampaignGuiStyle.UNAVAILABLE_TAG_TEXT_COLOR)
                } else {
                    addActionLabel(textPanel, labelText)
                }
                itemPanel.addUIElement(textPanel).inTL(ACTION_ROW_PADDING, CampaignGuiStyle.ITEM_TEXT_TOP_PADDING)
                actionButtons[button] = action
                currentTop += rowHeight + ACTION_ROW_GAP
            }

            val infoPanel = panel.createUIElement(width, max(40f, panel.position.height - currentTop - CampaignGuiStyle.PANEL_PADDING), false)
            infoPanel.addAgcText(weaponListView.pageString, 4f)
            if (isFilterView) {
                infoPanel.addAgcText("Filter mode", 4f, Misc.getHighlightColor())
            }
            panel.addUIElement(infoPanel).inTL(CampaignGuiStyle.PANEL_PADDING, currentTop)
        }

        private fun actionLabelLayout(action: SuggestedGuiAction, width: Float): WrappedLabelLayout {
            val shortcutText = action.shortcuts.joinToString("") { "[${Keyboard.getKeyName(it)}]" }
            val suffix = if (shortcutText.isBlank()) "" else " $shortcutText"
            return computeWrappedLabelLayout(
                text = action.name + suffix,
                rowWidth = width - 2f * ACTION_ROW_PADDING,
                minButtonHeight = 18f,
                horizontalPadding = 2f * ACTION_ROW_PADDING,
                verticalPadding = 2f * ACTION_ROW_PADDING,
                approxCharWidthPx = ACTION_LABEL_APPROX_CHAR_WIDTH,
                lineHeightPx = ACTION_LABEL_LINE_HEIGHT,
                maxLines = ACTION_LABEL_MAX_LINES
            )
        }

        private fun addActionLabel(
            panel: TooltipMakerAPI,
            labelText: String,
            baseColor: java.awt.Color? = null,
        ) {
            val label = if (baseColor == null) {
                panel.addAgcText(labelText, 0f)
            } else {
                panel.addAgcText(labelText, 0f, baseColor)
            }
            val highlights = ACTION_SHORTCUT_HIGHLIGHTS.filter { labelText.contains(it) }
            if (highlights.isNotEmpty()) {
                label.setHighlight(*highlights.toTypedArray())
                label.setHighlightColors(*Array(highlights.size) { Misc.getHighlightColor() })
            }
        }

        private fun bindButton(button: ButtonAPI) {
            button.setShowTooltipWhileInactive(true)
            buttonListenerPanel?.let {
                invokeMethodByName("setListener", button, it, narrativeContext = "Bind AGC suggested-tag action button")
            }
        }
    }

    private var dialog: InteractionDialogAPI? = null
    private var weaponListView = WeaponListView(7)
    private var isFilterView = false
    private var resetConfirmationPending = false
    private var pendingDangerousAction: PendingDangerousSuggestedAction? = null

    override fun init(i: InteractionDialogAPI?) {
        if (Settings.customSuggestedTags.isEmpty()) Settings.customSuggestedTags = Settings.defaultSuggestedTags
        dialog = i
        openCustomEditor()
    }

    private fun openCustomEditor() {
        dialog?.textPanel?.clear()
        dialog?.optionPanel?.clearOptions()
        dialog?.hideTextPanel()
        dialog?.hideVisualPanel()
        dialog?.setPromptText("")
        val panelPlugin = SuggestedTagPanelPlugin()
        dialog?.showCustomVisualDialog(
            Global.getSettings().screenWidth.toFloat(),
            Global.getSettings().screenHeight.toFloat(),
            SuggestedTagDialogDelegate(panelPlugin)
        )
    }

    private fun buildActions(callbacks: CustomVisualDialogDelegate.DialogCallbacks?): List<SuggestedGuiAction> {
        if (isFilterView) {
            pendingDangerousAction = null
            val actions = mutableListOf<SuggestedGuiAction>()
            actions.add(SuggestedGuiAction("Next Page", listOf(Keyboard.KEY_RIGHT, Keyboard.KEY_D), "Display the next set of weapons.") { weaponListView.cycle() })
            actions.add(SuggestedGuiAction("Prev Page", listOf(Keyboard.KEY_LEFT, Keyboard.KEY_A), "Display the previous set of weapons.") { weaponListView.cycleBackwards() })
            WeaponFilter.allFilters.forEach { filter ->
                val active = weaponListView.containsFilter(filter)
                actions.add(
                    SuggestedGuiAction(
                        filter.name(),
                        tooltip = if (active) "Deactivate filter." else "Activate filter.",
                        active = active
                    ) {
                        weaponListView.toggleFilter(filter)
                    }
                )
            }
            actions.add(SuggestedGuiAction("Reset Filters", tooltip = "Clear all active weapon filters.") { weaponListView.clearFilters() })
            actions.add(SuggestedGuiAction("Back", listOf(Keyboard.KEY_ESCAPE), "Return to suggested-tags options.") { isFilterView = false })
            return actions
        }

        val actions = mutableListOf<SuggestedGuiAction>()
        actions.add(SuggestedGuiAction("Next Page", listOf(Keyboard.KEY_RIGHT, Keyboard.KEY_D), "Display the next set of weapons.") { weaponListView.cycle() })
        actions.add(SuggestedGuiAction("Prev Page", listOf(Keyboard.KEY_LEFT, Keyboard.KEY_A), "Display the previous set of weapons.") { weaponListView.cycleBackwards() })
        actions.add(SuggestedGuiAction("Filter...", listOf(Keyboard.KEY_F), "Display filter options for the weapon list.") {
            resetConfirmationPending = false
            pendingDangerousAction = null
            isFilterView = true
        })
        if (resetConfirmationPending) {
            actions.add(
                SuggestedGuiAction(
                    "Confirm Reset",
                    tooltip = "This will replace all custom suggested weapon tags with the defaults.",
                    style = SuggestedActionStyle.GREEN
                ) {
                    Settings.customSuggestedTags = Settings.defaultSuggestedTags
                    resetConfirmationPending = false
                }
            )
            actions.add(
                SuggestedGuiAction(
                    "Cancel Reset",
                    tooltip = "Do not reset suggested tags.",
                    style = SuggestedActionStyle.RED
                ) { resetConfirmationPending = false }
            )
        } else {
            actions.add(SuggestedGuiAction("Reset", tooltip = "Show confirmation before resetting all suggested tags back to defaults.") {
                resetConfirmationPending = true
            })
        }
        if (!LunaSettingHandler.isLunaLibPresent) {
            actions.add(
                if (Settings.autoApplySuggestedTags) {
                    SuggestedGuiAction("Disable Auto-apply", tooltip = "Suggested tags will no longer be applied automatically.") {
                        Settings.autoApplySuggestedTags = false
                    }
                } else {
                    SuggestedGuiAction("Enable Auto-apply", tooltip = "Suggested tags will be applied automatically to weapon groups with no tags.") {
                        Settings.autoApplySuggestedTags = true
                    }
                }
            )
        }
        when (pendingDangerousAction) {
            PendingDangerousSuggestedAction.BACKUP -> {
                actions.add(
                    SuggestedGuiAction(
                        "Confirm",
                        tooltip = "",
                        style = SuggestedActionStyle.GREEN
                    ) {
                        backupSuggestedTagsToJson()
                        pendingDangerousAction = null
                    }
                )
                actions.add(
                    SuggestedGuiAction(
                        "Cancel",
                        tooltip = "",
                        style = SuggestedActionStyle.RED
                    ) { pendingDangerousAction = null }
                )
                actions.add(
                    SuggestedGuiAction(
                        "Restore",
                        tooltip = "Load tags previously saved via Backup."
                    ) {
                        pendingDangerousAction = PendingDangerousSuggestedAction.RESTORE
                    }
                )
            }

            PendingDangerousSuggestedAction.RESTORE -> {
                actions.add(
                    SuggestedGuiAction(
                        "Backup",
                        tooltip = "Save currently configured suggested tags to saves/common/${Values.CUSTOM_SUGGESTED_TAG_JSON_FILE_NAME}."
                    ) {
                        pendingDangerousAction = PendingDangerousSuggestedAction.BACKUP
                    }
                )
                actions.add(
                    SuggestedGuiAction(
                        "Confirm",
                        tooltip = "",
                        style = SuggestedActionStyle.GREEN
                    ) {
                        restoreSuggestedTagsFromJson()
                        pendingDangerousAction = null
                    }
                )
                actions.add(
                    SuggestedGuiAction(
                        "Cancel",
                        tooltip = "",
                        style = SuggestedActionStyle.RED
                    ) { pendingDangerousAction = null }
                )
            }

            null -> {
                actions.add(
                    SuggestedGuiAction(
                        "Backup",
                        tooltip = "Save currently configured suggested tags to saves/common/${Values.CUSTOM_SUGGESTED_TAG_JSON_FILE_NAME}."
                    ) {
                        pendingDangerousAction = PendingDangerousSuggestedAction.BACKUP
                    }
                )
                actions.add(
                    SuggestedGuiAction(
                        "Restore",
                        tooltip = "Load tags previously saved via Backup."
                    ) {
                        pendingDangerousAction = PendingDangerousSuggestedAction.RESTORE
                    }
                )
            }
        }
        actions.add(SuggestedGuiAction("Back", listOf(Keyboard.KEY_ESCAPE), "Return to the AGC ship editor.", rebuildAfter = false) { backToAgc(callbacks) })
        actions.add(SuggestedGuiAction("Exit", tooltip = "Close AGC.", rebuildAfter = false) { callbacks?.dismissDialog(); dialog?.dismiss() })
        return actions
    }

    private fun backToAgc(callbacks: CustomVisualDialogDelegate.DialogCallbacks?) {
        callbacks?.dismissDialog()
        dialog?.dismiss()
        GUIShower.shouldOpenAgcGui = true
    }

    override fun optionSelected(str: String?, data: Any?) {}
    override fun advance(p0: Float) {}
    override fun optionMousedOver(p0: String?, p1: Any?) {}
    override fun backFromEngagement(p0: EngagementResultAPI?) {}
    override fun getContext(): Any? = null
    override fun getMemoryMap(): MutableMap<String, MemoryAPI>? = null
}
