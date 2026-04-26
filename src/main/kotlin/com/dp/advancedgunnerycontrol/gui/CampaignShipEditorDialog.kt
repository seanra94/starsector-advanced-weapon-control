package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.gui.actions.BackAction
import com.dp.advancedgunnerycontrol.gui.actions.GUIAction
import com.dp.advancedgunnerycontrol.gui.actions.GoToSuggestedTagsAction
import com.dp.advancedgunnerycontrol.gui.actions.ResetAction
import com.dp.advancedgunnerycontrol.gui.actions.generateShipActions
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.utils.invokeMethodByName
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.PositionAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.input.Keyboard
import kotlin.math.max

class CampaignShipEditorDialogDelegate(
    private val panelPlugin: CampaignShipEditorPanelPlugin,
) : CustomVisualDialogDelegate {
    override fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks) {
        panelPlugin.init(panel, callbacks)
    }

    override fun getCustomPanelPlugin(): CustomUIPanelPlugin = panelPlugin

    override fun getNoiseAlpha(): Float = 0f

    override fun advance(amount: Float) {}

    override fun reportDismissed(option: Int) {}
}

class CampaignShipEditorPanelPlugin(
    private val attributes: GUIAttributes,
    private val onBackToPicker: () -> Unit,
) : BaseCustomUIPanelPlugin() {
    private enum class CampaignOptionRowStyle {
        DEFAULT,
        GREEN,
        RED,
    }

    private data class CampaignOptionRow(
        val label: String,
        val tooltip: String,
        val shortcut: Int? = null,
        val style: CampaignOptionRowStyle = CampaignOptionRowStyle.DEFAULT,
        val rebuildAfter: Boolean = true,
        val callback: () -> Unit,
    )

    companion object {
        private const val SECTION_HEADER_HEIGHT = 20f
        private const val ACTION_LINE_HEIGHT = 14f
        private const val ACTION_ROW_GAP = 2f
        private const val ACTION_ROW_PADDING = 4f
        private const val ACTION_LABEL_MAX_CHARS_PER_LINE = 36
        private const val ACTION_LABEL_MAX_LINES = 2
    }

    private val log = Global.getLogger(CampaignShipEditorPanelPlugin::class.java)
    private var panel: CustomPanelAPI? = null
    private var callbacks: CustomVisualDialogDelegate.DialogCallbacks? = null
    private var position: PositionAPI? = null
    private var buttonListenerPanel: CustomPanelAPI? = null

    private var contentPanel: CustomPanelAPI? = null
    private var shipView: ShipView? = null
    private var persistedTagScrollOffsets: Map<Int, Int> = emptyMap()

    private var currentActions: List<GUIAction> = emptyList()
    private var currentOptionRows: List<CampaignOptionRow> = emptyList()
    private val actionButtons = linkedMapOf<ButtonAPI, CampaignOptionRow>()
    private var lastModifierKeys = GUIAction.modifierKeys()
    private var resetConfirmationPending = false

    fun init(panel: CustomPanelAPI, callbacks: CustomVisualDialogDelegate.DialogCallbacks) {
        this.panel = panel
        this.callbacks = callbacks
        this.buttonListenerPanel = Global.getSettings().createCustom(0f, 0f, this)
        log.info("[AGC_CAMPAIGN_UI] init root=${panel.position.width}x${panel.position.height}")
        rebuild()
    }

    override fun positionChanged(position: PositionAPI?) {
        this.position = position
    }

    override fun render(alphaMult: Float) {
    }

    override fun advance(amount: Float) {
        if (lastModifierKeys != GUIAction.modifierKeys()) {
            lastModifierKeys = GUIAction.modifierKeys()
            rebuild()
            return
        }

        if (shipView?.shouldRegenerate() == true) {
            rebuild()
        }
    }

    override fun processInput(events: MutableList<InputEventAPI>) {
        shipView?.processInput(events)
        val shortcutAction = events.firstNotNullOfOrNull { event ->
            if (event.isConsumed || !event.isKeyDownEvent) return@firstNotNullOfOrNull null
            if (event.eventValue == Keyboard.KEY_ESCAPE || event.eventValue == Settings.guiHotkey()) {
                event.consume()
                dismissToPicker()
                return@firstNotNullOfOrNull null
            }
            currentOptionRows.firstOrNull { it.shortcut == event.eventValue }?.also { event.consume() }
        }
        shortcutAction?.let(::executeOptionRow)
    }

    override fun buttonPressed(buttonId: Any?) {
        (buttonId as? CampaignOptionRow)?.let(::executeOptionRow)
    }

    private fun rebuild() {
        val root = panel ?: return
        try {
            persistedTagScrollOffsets = shipView?.captureTagScrollOffsets() ?: persistedTagScrollOffsets
            contentPanel?.let(root::removeComponent)
            contentPanel = null
            shipView = null
            actionButtons.clear()
            currentActions = generateShipActions(attributes)
            if (currentActions.none { it is ResetAction }) {
                resetConfirmationPending = false
            }
            currentOptionRows = buildOptionRows()

            val rootWidth = root.position.width
            val rootHeight = root.position.height
            val mainWidth = max(300f, rootWidth - 2f * CampaignGuiStyle.MAIN_PADDING)
            val mainHeight = max(300f, rootHeight - 2f * CampaignGuiStyle.MAIN_PADDING)
            log.info("[AGC_CAMPAIGN_UI] rebuild root=${rootWidth}x${rootHeight} inner=${mainWidth}x${mainHeight}")

            val content = root.createCustomPanel(
                mainWidth,
                mainHeight,
                ShipView(
                    attributes.tagView,
                    enableTagScroll = false,
                    drawFrame = false,
                    initialTagScrollOffsets = persistedTagScrollOffsets
                )
            )
            root.addComponent(content).inTL(CampaignGuiStyle.MAIN_PADDING, CampaignGuiStyle.MAIN_PADDING)
            contentPanel = content
            shipView = content.plugin as? ShipView
            attributes.ship?.let { ship ->
                shipView?.buildIn(content, ship, ::buildOptionsPanel, estimateOptionsPanelHeight())
            }
        } catch (ex: Throwable) {
            log.error("[AGC_CAMPAIGN_UI] rebuild failed", ex)
            buildFallback(root, ex)
        }
    }

    private fun buildFallback(root: CustomPanelAPI, ex: Throwable) {
        contentPanel?.let(root::removeComponent)
        contentPanel = null
        shipView = null
        actionButtons.clear()

        val width = max(500f, root.position.width - 2f * CampaignGuiStyle.MAIN_PADDING)
        val height = 220f
        val fallback = root.createCustomPanel(width, height, DebugBorderPanelPlugin(CampaignContainerType.OPTIONS))
        root.addComponent(fallback).inTL(CampaignGuiStyle.MAIN_PADDING, CampaignGuiStyle.MAIN_PADDING)

        val element = fallback.createUIElement(width - 16f, height - 16f, false)
        element.addSectionHeading("AGC Campaign UI Error", Alignment.MID, 0f)
        element.addPara(
            "The campaign editor failed to build. Press [Esc] or the AGC GUI hotkey to exit this screen.",
            6f,
            Misc.getNegativeHighlightColor(),
            "[Esc]"
        )
        element.addPara("Reason: ${ex.javaClass.simpleName}: ${ex.message ?: "no message"}", 6f)
        fallback.addUIElement(element).inTL(8f, 8f)
    }

    private fun dismissToPicker() {
        callbacks?.dismissDialog()
        onBackToPicker()
    }

    private fun buildOptionsPanel(panel: CustomPanelAPI) {
        actionButtons.clear()
        val width = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
        val headerPanel = panel.createCustomPanel(
            width,
            SECTION_HEADER_HEIGHT,
            DebugBorderPanelPlugin(CampaignContainerType.HEADER)
        )
        panel.addComponent(headerPanel)
        headerPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, CampaignGuiStyle.PANEL_PADDING)
        val header = headerPanel.createUIElement(width, SECTION_HEADER_HEIGHT, false)
        header.addSectionHeading("Options", Alignment.MID, 0f)
        headerPanel.addUIElement(header).inTL(0f, 0f)

        val bodyTop = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT
        var currentTop = bodyTop

        currentOptionRows.forEach { row ->
            val rowHeight = actionRowHeight(row)
            renderActionRow(panel, width, row, currentTop, rowHeight)
            currentTop += rowHeight + ACTION_ROW_GAP
        }

        val infoPanel = panel.createUIElement(
            width,
            max(22f, panel.position.height - currentTop - CampaignGuiStyle.PANEL_PADDING),
            false
        )
        infoPanel.addPara(
            "MODIFIERS:\n[SHIFT] = FLEET\n[CTRL] = ALL LOADOUTS",
            0f,
            Misc.getHighlightColor(),
            "[SHIFT]",
            "[CTRL]"
        )
        panel.addUIElement(infoPanel).inTL(
            CampaignGuiStyle.PANEL_PADDING,
            currentTop
        )
    }

    private fun actionRowHeight(action: CampaignOptionRow): Float {
        val lineCount = minOf(ACTION_LABEL_MAX_LINES, buildActionLabel(action).split("\n").size)
        return ACTION_ROW_PADDING * 2f + ACTION_LINE_HEIGHT * lineCount
    }

    private fun renderActionRow(
        panel: CustomPanelAPI,
        width: Float,
        action: CampaignOptionRow,
        top: Float,
        rowHeight: Float,
    ) {
        val isGreen = action.style == CampaignOptionRowStyle.GREEN
        val isRed = action.style == CampaignOptionRowStyle.RED
        val rowFillColor = when {
            isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_BACKGROUND_COLOR
            isGreen -> CampaignGuiStyle.ACTIVE_GREEN_BACKGROUND_COLOR
            else -> CampaignGuiStyle.INACTIVE_ROW_BACKGROUND_COLOR
        }
        val itemPanel = panel.createCustomPanel(
            width,
            rowHeight,
            DebugBorderPanelPlugin(CampaignContainerType.ITEM, fillColor = rowFillColor)
        )
        panel.addComponent(itemPanel)
        itemPanel.position.inTL(
            CampaignGuiStyle.PANEL_PADDING,
            top
        )

        val inner = itemPanel.createUIElement(width, rowHeight, false)
        val button = inner.addAreaCheckbox(
            "",
            action,
            when {
                isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_BACKGROUND_COLOR
                isGreen -> CampaignGuiStyle.ACTIVE_GREEN_BACKGROUND_COLOR
                else -> Misc.getBasePlayerColor()
            },
            when {
                isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_DARK_COLOR
                isGreen -> CampaignGuiStyle.ACTIVE_GREEN_DARK_COLOR
                else -> Misc.getDarkPlayerColor()
            },
            when {
                isRed -> CampaignGuiStyle.UNAVAILABLE_TAG_BRIGHT_COLOR
                isGreen -> CampaignGuiStyle.ACTIVE_GREEN_BRIGHT_COLOR
                else -> Misc.getBrightPlayerColor()
            },
            width,
            rowHeight,
            0f
        )
        inner.addTooltipToPrevious(
            AGCGUI.makeTooltip(action.tooltip),
            TooltipMakerAPI.TooltipLocation.BELOW
        )
        bindButton(button)
        itemPanel.addUIElement(inner).inTL(CampaignGuiStyle.ITEM_HIGHLIGHT_X_OFFSET, 0f)

        val textPanel = itemPanel.createUIElement(
            width - 2f * ACTION_ROW_PADDING,
            rowHeight - ACTION_ROW_PADDING - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
            false
        )
        if (isRed) {
            textPanel.addPara(buildActionLabel(action), CampaignGuiStyle.UNAVAILABLE_TAG_TEXT_COLOR, 0f)
        } else {
            textPanel.addPara(buildActionLabel(action), 0f)
        }
        itemPanel.addUIElement(textPanel).inTL(ACTION_ROW_PADDING, CampaignGuiStyle.ITEM_TEXT_TOP_PADDING)
        actionButtons[button] = action
    }

    private fun wrapActionLine(line: String, maxChars: Int, maxLines: Int): String {
        if (line.length <= maxChars || !line.contains(" ")) return line
        val wrapped = mutableListOf<String>()
        var current = ""
        line.split(" ").forEach { word ->
            if (current.isBlank()) {
                current = word
            } else if ((current.length + 1 + word.length) <= maxChars) {
                current += " $word"
            } else {
                wrapped.add(current)
                current = word
            }
        }
        if (current.isNotBlank()) wrapped.add(current)
        if (wrapped.size > maxLines) {
            val visible = wrapped.take(maxLines).toMutableList()
            val lastIndex = visible.lastIndex
            val joinedRemainder = wrapped.drop(maxLines - 1).joinToString(" ")
            visible[lastIndex] = truncateLabelByLength(joinedRemainder, maxChars)
            return visible.joinToString("\n")
        }
        return wrapped.joinToString("\n")
    }

    private fun buildActionLabel(action: CampaignOptionRow): String {
        val shortcut = action.shortcut?.let { " [${Keyboard.getKeyName(it)}]" } ?: ""
        return wrapActionLine(action.label + shortcut, ACTION_LABEL_MAX_CHARS_PER_LINE, ACTION_LABEL_MAX_LINES)
    }

    private fun estimateOptionsPanelHeight(): Float {
        val rowsHeight = currentOptionRows.sumOf { actionRowHeight(it).toDouble() }.toFloat()
        val rowGaps = max(0, currentOptionRows.size - 1) * ACTION_ROW_GAP
        val infoHeight = 46f
        return 2f * CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT + rowsHeight + rowGaps + infoHeight + 4f
    }

    private fun bindButton(button: ButtonAPI) {
        button.setShowTooltipWhileInactive(true)
        buttonListenerPanel?.let {
            invokeMethodByName("setListener", button, it, narrativeContext = "Bind AGC campaign action button")
        }
    }

    private fun buildOptionRows(): List<CampaignOptionRow> {
        val rows = mutableListOf<CampaignOptionRow>()
        val backAction = currentActions.firstOrNull { it is BackAction }
        val normalActions = currentActions.filterNot { it is BackAction }
        normalActions.forEach { action ->
            when (action) {
                is ResetAction -> {
                    if (resetConfirmationPending) {
                        rows.add(
                            CampaignOptionRow(
                                label = "Confirm Reset",
                                tooltip = action.getTooltip(),
                                style = CampaignOptionRowStyle.GREEN,
                                callback = {
                                    action.execute()
                                    resetConfirmationPending = false
                                }
                            )
                        )
                        rows.add(
                            CampaignOptionRow(
                                label = "Cancel Reset",
                                tooltip = "Do not reset current settings.",
                                style = CampaignOptionRowStyle.RED,
                                callback = { resetConfirmationPending = false }
                            )
                        )
                    } else {
                        rows.add(
                            CampaignOptionRow(
                                label = action.getName(),
                                tooltip = action.getTooltip(),
                                shortcut = action.getShortcut(),
                                callback = { resetConfirmationPending = true }
                            )
                        )
                    }
                }

                else -> rows.add(
                    CampaignOptionRow(
                        label = action.getName(),
                        tooltip = action.getTooltip(),
                        shortcut = action.getShortcut(),
                        rebuildAfter = false,
                        callback = { executeAction(action) }
                    )
                )
            }
        }
        if (backAction != null) {
            rows.add(
                CampaignOptionRow(
                    label = backAction.getName(),
                    tooltip = backAction.getTooltip(),
                    shortcut = backAction.getShortcut(),
                    rebuildAfter = false,
                    callback = { executeAction(backAction) }
                )
            )
        }
        return rows
    }

    private fun executeOptionRow(action: CampaignOptionRow) {
        action.callback()
        if (action.rebuildAfter) {
            rebuild()
        }
    }

    private fun executeAction(action: GUIAction) {
        action.execute()
        when {
            action is GoToSuggestedTagsAction -> {
                callbacks?.dismissDialog()
            }

            attributes.level == Level.TOP || action is BackAction -> {
                callbacks?.dismissDialog()
                onBackToPicker()
            }

            else -> {
                rebuild()
            }
        }
    }
}
