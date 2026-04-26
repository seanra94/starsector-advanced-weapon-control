package com.dp.advancedgunnerycontrol.gui.suggesttaggui

import com.dp.advancedgunnerycontrol.gui.AGCGUI
import com.dp.advancedgunnerycontrol.gui.ButtonBase
import com.dp.advancedgunnerycontrol.gui.CampaignContainerType
import com.dp.advancedgunnerycontrol.gui.CampaignGuiStyle
import com.dp.advancedgunnerycontrol.gui.DebugBorderPanelPlugin
import com.dp.advancedgunnerycontrol.gui.computeWrapGridMetrics
import com.dp.advancedgunnerycontrol.gui.renderColoredTagLabel
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.TagListView
import com.dp.advancedgunnerycontrol.typesandvalues.getSuggestedModesForWeaponId
import com.dp.advancedgunnerycontrol.typesandvalues.getTagTooltip
import com.dp.advancedgunnerycontrol.typesandvalues.isIncompatibleWithExistingTags
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import kotlin.math.max

class SuggestedTagButton(private val weaponId: String, tag: String, button: ButtonAPI) : ButtonBase<String>(tag, button, false) {
    companion object{
        var suggestedTagSelectionVersion = 0
            private set

        fun createButtonGroup(weaponId: String, tooltip: TooltipMakerAPI, tagView: TagListView) : List<SuggestedTagButton>
        {
            val toReturn = mutableListOf<SuggestedTagButton>()
            tagView.view().forEach { tag ->
                toReturn.add(SuggestedTagButton(weaponId, tag, tooltip.addAreaCheckbox(
                    tag,
                    tag,
                    Misc.getBasePlayerColor(),
                    Misc.getDarkPlayerColor(),
                    Misc.getBrightPlayerColor(),
                    160f,
                    18f,
                    3f
                )))
                tooltip.addTooltipToPrevious(
                    AGCGUI.makeTooltip(getTagTooltip(tag)),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                if(getSuggestedModesForWeaponId(weaponId).contains(tag)){
                    toReturn.last().check()
                }
            }
            toReturn.forEach {
                it.sameGroupButtons = toReturn
            }
            toReturn.forEach {
                it.updateDisabledButtons()
            }
            return toReturn
        }

        fun createCampaignButtonGroup(
            weaponId: String,
            panel: CustomPanelAPI,
            visibleTags: List<String>,
            pinned: Boolean = false,
        ): List<SuggestedTagButton> {
            val tags = visibleTags
            val metrics = computeWrapGridMetrics(
                itemCount = max(tags.size, 1),
                availableWidth = panel.position.width,
                availableHeight = panel.position.height,
                minItemWidth = CampaignGuiStyle.TAG_ITEM_MIN_WIDTH,
                itemHeight = CampaignGuiStyle.TAG_ITEM_HEIGHT,
                horizontalGap = CampaignGuiStyle.TAG_ITEM_HGAP,
                verticalGap = CampaignGuiStyle.TAG_ITEM_VGAP,
                maxColumns = 1
            )
            val selectedTags = getSuggestedModesForWeaponId(weaponId)
            val toReturn = mutableListOf<SuggestedTagButton>()

            tags.forEachIndexed { index, tag ->
                val otherSelectedTags = selectedTags.toMutableList().apply { remove(tag) }
                val unavailable = !pinned && isIncompatibleWithExistingTags(tag, otherSelectedTags)
                val rowFillColor = when {
                    pinned -> CampaignGuiStyle.ACTIVE_GREEN_BACKGROUND_COLOR
                    unavailable -> CampaignGuiStyle.DISABLED_TAG_BACKGROUND_COLOR
                    else -> null
                }
                val itemPanel = panel.createCustomPanel(
                    metrics.itemWidth,
                    metrics.itemHeight,
                    DebugBorderPanelPlugin(CampaignContainerType.ITEM, fillColor = rowFillColor)
                )
                panel.addComponent(itemPanel)
                itemPanel.position.inTL(metrics.xFor(index), metrics.yFor(index))

                val inner = itemPanel.createUIElement(metrics.itemWidth, metrics.itemHeight, false)
                val baseColor = when {
                    pinned -> CampaignGuiStyle.ACTIVE_GREEN_BACKGROUND_COLOR
                    unavailable -> CampaignGuiStyle.DISABLED_TAG_BACKGROUND_COLOR
                    else -> Misc.getBasePlayerColor()
                }
                val darkColor = when {
                    pinned -> CampaignGuiStyle.ACTIVE_GREEN_DARK_COLOR
                    unavailable -> CampaignGuiStyle.DISABLED_TAG_DARK_COLOR
                    else -> Misc.getDarkPlayerColor()
                }
                val brightColor = when {
                    pinned -> CampaignGuiStyle.ACTIVE_GREEN_BRIGHT_COLOR
                    unavailable -> CampaignGuiStyle.DISABLED_TAG_BRIGHT_COLOR
                    else -> Misc.getBrightPlayerColor()
                }
                val createdButton = inner.addAreaCheckbox(
                    "",
                    tag,
                    baseColor,
                    darkColor,
                    brightColor,
                    metrics.itemWidth,
                    metrics.itemHeight,
                    0f
                )
                val suggestedButton = SuggestedTagButton(weaponId, tag, createdButton)
                toReturn.add(suggestedButton)
                inner.addTooltipToPrevious(
                    AGCGUI.makeTooltip(getTagTooltip(tag)),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                itemPanel.addUIElement(inner).inTL(CampaignGuiStyle.ITEM_HIGHLIGHT_X_OFFSET, 0f)

                renderColoredTagLabel(
                    itemPanel,
                    tag,
                    metrics.itemWidth - 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    metrics.itemHeight - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    textColor = if (unavailable) CampaignGuiStyle.DISABLED_TAG_TEXT_COLOR else null,
                    enableKeywordColors = !unavailable
                )

                if (selectedTags.contains(tag)) {
                    suggestedButton.setCheckedFromPersistence(true)
                }
            }

            toReturn.forEach { it.sameGroupButtons = toReturn }
            toReturn.forEach { it.updateDisabledButtons() }
            return toReturn
        }
    }

    private fun setCheckedFromPersistence(checked: Boolean) {
        active = checked
        button.isChecked = checked
    }

    override fun onActivate() {
        val st = Settings.getCurrentSuggestedTags().toMutableMap()
        st[weaponId] = ((st[weaponId] ?: listOf()) + listOf(associatedValue)).toSet().toList()
        Settings.customSuggestedTags = st
        suggestedTagSelectionVersion++
    }

    private fun onDeactivate() {
        val st = Settings.getCurrentSuggestedTags().toMutableMap()
        val l = (st[weaponId] ?: listOf())
        val l2 = l.toMutableList()
        l2.remove(associatedValue)
        st[weaponId] = l2
        Settings.customSuggestedTags = st
    }

    override fun executeCallbackIfChecked() {
        if (!active && button.isChecked) {
            check()
            updateDisabledButtons()
        } else if (active && !button.isChecked) {
            onDeactivate()
            uncheck()
            suggestedTagSelectionVersion++
            updateDisabledButtons()
        }
        button.isChecked = active
    }

    private fun updateDisabledButtons(){
        val tags = Settings.getCurrentSuggestedTags()[weaponId] ?: emptyList()
        enable()
        val selfOtherTags = tags.toMutableList().apply { remove(associatedValue) }
        if (isIncompatibleWithExistingTags(associatedValue, selfOtherTags)) {
            disable()
            button.isChecked = false
            active = false
        }
        sameGroupButtons.forEach {
            val tagButton = it as? SuggestedTagButton ?: return@forEach
            tagButton.enable()
            val otherTags = tags.toMutableList().apply { remove(it.associatedValue) }
            if (isIncompatibleWithExistingTags(it.associatedValue, otherTags)) {
                tagButton.disable()
                tagButton.button.isChecked = false
                tagButton.active = false
            }
        }
    }
}
