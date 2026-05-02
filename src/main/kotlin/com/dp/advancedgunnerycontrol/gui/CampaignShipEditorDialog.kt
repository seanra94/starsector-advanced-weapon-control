package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.gui.actions.ApplySuggestedModeAction
import com.dp.advancedgunnerycontrol.gui.actions.CopyLoadoutAction
import com.dp.advancedgunnerycontrol.gui.actions.CopyToSameVariantAction
import com.dp.advancedgunnerycontrol.gui.actions.GUIAction
import com.dp.advancedgunnerycontrol.gui.actions.GoToSuggestedTagsAction
import com.dp.advancedgunnerycontrol.gui.actions.ReloadSettingsAction
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
        SAVE,
        LOAD,
    }

    private fun CampaignOptionRowStyle.toButtonKind(): CampaignActionButtonKind {
        return when (this) {
            CampaignOptionRowStyle.DEFAULT -> CampaignActionButtonKind.UNCOLOURED
            CampaignOptionRowStyle.GREEN -> CampaignActionButtonKind.CONFIRM
            CampaignOptionRowStyle.RED -> CampaignActionButtonKind.CANCEL
            CampaignOptionRowStyle.SAVE -> CampaignActionButtonKind.SAVE
            CampaignOptionRowStyle.LOAD -> CampaignActionButtonKind.LOAD
        }
    }

    private data class CampaignOptionRow(
        val label: String,
        val tooltip: String,
        val shortcut: Int? = null,
        val style: CampaignOptionRowStyle = CampaignOptionRowStyle.DEFAULT,
        val rebuildAfter: Boolean = true,
        val callback: () -> Unit,
    )

    private data class CampaignOptionsLayout(
        val rows: List<Pair<CampaignOptionRow, WrappedLabelLayout>>,
        val modifiersLayout: WrappedLabelLayout,
        val requiredHeight: Float,
    )

    private data class PendingOptionConfirmation(
        val actionClass: Class<out GUIAction>,
        val actionDisplayName: String,
        val allLoadouts: Boolean,
        val wholeFleet: Boolean,
    )

    companion object {
        private const val SECTION_HEADER_HEIGHT = 20f
        private const val ACTION_LABEL_MAX_LINES = 3
        private const val MODIFIERS_MAX_LINES = 6
        private const val MODIFIERS_TOP_GAP = 2f
        private const val MODIFIERS_TEXT = "MODIFIERS:\n[SHIFT] = FLEET\n[CTRL] = ALL LOADOUTS"
    }

    private val log = Global.getLogger(CampaignShipEditorPanelPlugin::class.java)
    private var panel: CustomPanelAPI? = null
    private var callbacks: CustomVisualDialogDelegate.DialogCallbacks? = null
    private var position: PositionAPI? = null
    private var buttonListenerPanel: CustomPanelAPI? = null

    private var contentPanel: CustomPanelAPI? = null
    private var shipView: ShipView? = null
    private var persistedTagScrollOffsets: Map<Int, Int> = emptyMap()
    private var persistedPendingPresetActions: Map<Int, ShipView.PendingPresetAction> = emptyMap()

    private var currentActions: List<GUIAction> = emptyList()
    private var currentOptionRows: List<CampaignOptionRow> = emptyList()
    private val actionButtons = linkedMapOf<ButtonAPI, CampaignOptionRow>()
    private var lastModifierKeys = GUIAction.modifierKeys()
    private var pendingOptionConfirmation: PendingOptionConfirmation? = null

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
            persistedPendingPresetActions = shipView?.capturePendingPresetActions() ?: persistedPendingPresetActions
            contentPanel?.let(root::removeComponent)
            contentPanel = null
            shipView = null
            actionButtons.clear()
            currentActions = generateShipActions(attributes)
            val currentModifiers = GUIAction.modifierKeys()
            val pending = pendingOptionConfirmation
            if (pending != null && !hasMatchingActionVariant(pending, currentModifiers)) {
                pendingOptionConfirmation = null
            }
            currentOptionRows = buildOptionRows()

            val rootWidth = root.position.width
            val rootHeight = root.position.height
            val mainWidth = max(300f, rootWidth - 2f * CampaignGuiStyle.MAIN_PADDING)
            val mainHeight = max(300f, rootHeight - 2f * CampaignGuiStyle.MAIN_PADDING)
            val content = root.createCustomPanel(
                mainWidth,
                mainHeight,
                ShipView(
                    attributes.tagView,
                    enableTagScroll = false,
                    drawFrame = false,
                    initialTagScrollOffsets = persistedTagScrollOffsets,
                    initialPendingPresetActions = persistedPendingPresetActions,
                    onPendingPresetActionUpdate = { groupIndex, action ->
                        val updated = persistedPendingPresetActions.toMutableMap()
                        if (action == null) {
                            updated.remove(groupIndex)
                        } else {
                            updated[groupIndex] = action
                        }
                        persistedPendingPresetActions = updated
                    }
                )
            )
            root.addComponent(content).inTL(CampaignGuiStyle.MAIN_PADDING, CampaignGuiStyle.MAIN_PADDING)
            contentPanel = content
            shipView = content.plugin as? ShipView
            attributes.ship?.let { ship ->
                shipView?.buildIn(content, ship, ::buildOptionsPanel, ::estimateOptionsPanelHeight)
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
        val fallback = root.createCustomPanel(width, height, CampaignPanelPlugin(CampaignContainerType.OPTIONS))
        root.addComponent(fallback).inTL(CampaignGuiStyle.MAIN_PADDING, CampaignGuiStyle.MAIN_PADDING)

        val element = fallback.createUIElement(width - 16f, height - 16f, false)
        element.addSectionHeading("AGC Campaign UI Error", Alignment.MID, 0f)
        element.applyAgcDefaultTextStyle()
        element.addPara(
            "The campaign editor failed to build. Press [Esc] or the AGC GUI hotkey to exit this screen.",
            6f,
            CampaignGuiStyle.CANCEL_BUTTON_HOVER_COLOR,
            "[Esc]"
        )
        element.addAgcText("Reason: ${ex.javaClass.simpleName}: ${ex.message ?: "no message"}", 6f)
        fallback.addUIElement(element).inTL(8f, 8f)
    }

    private fun dismissToPicker() {
        callbacks?.dismissDialog()
        onBackToPicker()
    }

    private fun buildOptionsPanel(panel: CustomPanelAPI) {
        actionButtons.clear()
        val width = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
        addCustomContainerHeading(panel, "Options", headingHeight = SECTION_HEADER_HEIGHT)

        val bodyTop = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT
        var currentTop = bodyTop
        val layout = computeOptionsLayout(width)

        layout.rows.forEachIndexed { index, (row, rowLayout) ->
            renderActionRow(panel, width, row, rowLayout.wrappedText, currentTop, rowLayout.rowHeight)
            currentTop += rowLayout.rowHeight
            if (index < layout.rows.lastIndex) {
                currentTop += CampaignGuiStyle.ACTION_ROW_GAP
            }
        }
        if (layout.rows.isNotEmpty()) {
            currentTop += MODIFIERS_TOP_GAP
        }

        val infoPanel = panel.createUIElement(width, layout.modifiersLayout.rowHeight, false)
        infoPanel.applyAgcDefaultTextStyle()
        infoPanel.addPara(
            MODIFIERS_TEXT,
            0f,
            CampaignGuiStyle.MODIFIER_TEXT_COLOUR,
            "[SHIFT]",
            "[CTRL]"
        )
        panel.addUIElement(infoPanel).inTL(
            CampaignGuiStyle.PANEL_PADDING,
            currentTop
        )
    }

    private fun renderActionRow(
        panel: CustomPanelAPI,
        width: Float,
        action: CampaignOptionRow,
        labelText: String,
        top: Float,
        rowHeight: Float,
    ) {
        val buttonShell = addStyledCampaignActionRow(
            parent = panel,
            data = action,
            x = CampaignGuiStyle.PANEL_PADDING,
            y = top,
            width = width,
            height = rowHeight,
            kind = action.style.toButtonKind(),
            labelText = labelText,
            highlightTokens = CampaignGuiStyle.ACTION_SHORTCUT_HIGHLIGHTS,
            tooltip = action.tooltip,
            textPadding = CampaignGuiStyle.ACTION_ROW_PADDING,
        )
        val button = buttonShell.button
        bindButton(button)
        actionButtons[button] = action
    }

    private fun actionLabelLayout(action: CampaignOptionRow, width: Float): WrappedLabelLayout {
        val labelText = CampaignGuiStyle.actionLabelText(action.label, action.shortcut?.let(::listOf) ?: emptyList())
        return CampaignGuiStyle.actionLabelLayout(labelText, width, ACTION_LABEL_MAX_LINES)
    }

    private fun modifiersLabelLayout(width: Float): WrappedLabelLayout {
        return computeWrappedLabelLayout(
            text = MODIFIERS_TEXT,
            rowWidth = width,
            minButtonHeight = 0f,
            horizontalPadding = 0f,
            verticalPadding = 0f,
            approxCharWidthPx = CampaignGuiStyle.ACTION_LABEL_APPROX_CHAR_WIDTH,
            lineHeightPx = CampaignGuiStyle.ACTION_LABEL_LINE_HEIGHT,
            maxLines = MODIFIERS_MAX_LINES
        )
    }

    private fun computeOptionsLayout(width: Float): CampaignOptionsLayout {
        val rowLayouts = currentOptionRows.map { row -> row to actionLabelLayout(row, width) }
        val rowsHeight = rowLayouts.sumOf { (_, layout) -> layout.rowHeight.toDouble() }.toFloat()
        val rowGapsHeight = max(0, rowLayouts.size - 1) * CampaignGuiStyle.ACTION_ROW_GAP
        val modifiersTopGap = if (rowLayouts.isEmpty()) 0f else MODIFIERS_TOP_GAP
        val modifiersLayout = modifiersLabelLayout(width)
        val requiredHeight =
            CampaignGuiStyle.PANEL_PADDING +
                SECTION_HEADER_HEIGHT +
                rowsHeight +
                rowGapsHeight +
                modifiersTopGap +
                modifiersLayout.rowHeight +
                CampaignGuiStyle.PANEL_PADDING
        return CampaignOptionsLayout(
            rows = rowLayouts,
            modifiersLayout = modifiersLayout,
            requiredHeight = requiredHeight
        )
    }

    private fun estimateOptionsPanelHeight(width: Float): Float {
        return computeOptionsLayout(width).requiredHeight
    }

    private fun bindButton(button: ButtonAPI) {
        button.setShowTooltipWhileInactive(true)
        buttonListenerPanel?.let {
            invokeMethodByName("setListener", button, it, narrativeContext = "Bind AGC campaign action button")
        }
    }

    private fun buildOptionRows(): List<CampaignOptionRow> {
        val rows = mutableListOf<CampaignOptionRow>()
        val currentModifiers = GUIAction.modifierKeys()
        val pending = pendingOptionConfirmation
        currentActions.forEach { action ->
            if (requiresConfirmation(action)) {
                val currentActionVariant = actionVariant(action, currentModifiers)
                if (pending != null && pending == currentActionVariant) {
                    rows.add(
                        CampaignOptionRow(
                            label = "Confirm",
                            tooltip = "",
                            style = CampaignOptionRowStyle.GREEN,
                            callback = {
                                executeConfirmedAction(action, pending)
                                pendingOptionConfirmation = null
                            }
                        )
                    )
                    rows.add(
                        CampaignOptionRow(
                            label = "Cancel",
                            tooltip = "",
                            style = CampaignOptionRowStyle.RED,
                            callback = {
                                pendingOptionConfirmation = null
                            }
                        )
                    )
                } else {
                    rows.add(
                        CampaignOptionRow(
                            label = action.getName(),
                            tooltip = action.getTooltip(),
                            shortcut = action.getShortcut(),
                            style = actionFamilyStyle(action),
                            callback = {
                                pendingOptionConfirmation = actionVariant(action, GUIAction.modifierKeys())
                            }
                        )
                    )
                }
            } else {
                rows.add(
                    CampaignOptionRow(
                        label = action.getName(),
                        tooltip = action.getTooltip(),
                        shortcut = action.getShortcut(),
                        style = actionFamilyStyle(action),
                        rebuildAfter = false,
                        callback = { executeAction(action) }
                    )
                )
            }
        }
        return rows
    }

    private fun actionFamilyStyle(action: GUIAction): CampaignOptionRowStyle {
        return when (action) {
            is CopyToSameVariantAction -> CampaignOptionRowStyle.SAVE
            is CopyLoadoutAction,
            is ApplySuggestedModeAction,
            is ResetAction,
            is ReloadSettingsAction -> CampaignOptionRowStyle.LOAD
            else -> CampaignOptionRowStyle.DEFAULT
        }
    }

    private fun requiresConfirmation(action: GUIAction): Boolean {
        return action is ResetAction ||
            action is ApplySuggestedModeAction ||
            action is CopyToSameVariantAction ||
            action is CopyLoadoutAction ||
            action is ReloadSettingsAction
    }

    private fun actionVariant(
        action: GUIAction,
        modifiers: Pair<Boolean, Boolean>
    ): PendingOptionConfirmation {
        return PendingOptionConfirmation(
            actionClass = action::class.java,
            actionDisplayName = action.getName(),
            allLoadouts = modifiers.first,
            wholeFleet = modifiers.second,
        )
    }

    private fun hasMatchingActionVariant(
        pending: PendingOptionConfirmation,
        modifiers: Pair<Boolean, Boolean>
    ): Boolean {
        return currentActions.any { action -> actionVariant(action, modifiers) == pending }
    }

    private fun executeConfirmedAction(action: GUIAction, pending: PendingOptionConfirmation) {
        if (action is ResetAction) {
            action.executeWithModifiers(pending.allLoadouts, pending.wholeFleet)
            return
        }
        if (action is CopyToSameVariantAction) {
            action.executeWithModifiers(
                allLoadouts = pending.allLoadouts,
                sameHullType = pending.wholeFleet
            )
            return
        }
        if (action is CopyLoadoutAction) {
            action.executeWithModifiers(
                allLoadouts = pending.allLoadouts,
                wholeFleet = pending.wholeFleet
            )
            return
        }
        if (action is ApplySuggestedModeAction) {
            action.executeWithModifiers(
                allLoadouts = pending.allLoadouts,
                wholeFleet = pending.wholeFleet
            )
            return
        }
        executeAction(action)
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
            else -> {
                rebuild()
            }
        }
    }
}
