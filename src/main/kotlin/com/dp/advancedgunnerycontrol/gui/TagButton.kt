package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.TagListView
import com.dp.advancedgunnerycontrol.typesandvalues.getTagTooltip
import com.dp.advancedgunnerycontrol.typesandvalues.isIncompatibleWithExistingTags
import com.dp.advancedgunnerycontrol.typesandvalues.shouldTagBeDisabled
import com.dp.advancedgunnerycontrol.utils.loadPersistentTags
import com.dp.advancedgunnerycontrol.utils.persistTags
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import java.awt.Color
import kotlin.math.max

class TagButton(var ship: FleetMemberAPI, var group: Int, tag: String, button: ButtonAPI) :
    ButtonBase<String>(tag, button, false) {

    companion object {
        private var storage = Settings.tagStorage[AGCGUI.storageIndex]
        var campaignTagSelectionVersion = 0
            private set

        private fun sanitizePersistedTags(ship: FleetMemberAPI, group: Int): MutableList<String> {
            val allTags = Settings.getCurrentWeaponTagList()
            val sanitized = loadPersistentTags(ship.id, ship, group, AGCGUI.storageIndex)
                .filter { allTags.contains(it) }
                .toMutableList()
            var changed = true
            while (changed) {
                changed = false
                sanitized.toList().forEach { persistedTag ->
                    val otherTags = sanitized.toMutableList().apply { remove(persistedTag) }
                    if (isIncompatibleWithExistingTags(persistedTag, otherTags) || shouldTagBeDisabled(group, ship, persistedTag)) {
                        sanitized.remove(persistedTag)
                        changed = true
                    }
                }
            }
            persistTags(ship.id, ship, group, AGCGUI.storageIndex, sanitized)
            return sanitized
        }

        fun createModeButtonGroup(
            ship: FleetMemberAPI,
            group: Int,
            tooltip: TooltipMakerAPI,
            tagView: TagListView
        ): List<TagButton> {
            storage = Settings.tagStorage[AGCGUI.storageIndex]
            val toReturn = mutableListOf<TagButton>()
            tagView.view().forEach {
                toReturn.add(
                    TagButton(
                        ship, group, it, tooltip.addAreaCheckbox(
                            it,
                            it,
                            Misc.getBasePlayerColor(),
                            Misc.getDarkPlayerColor(),
                            Misc.getBrightPlayerColor(),
                            160f,
                            18f,
                            3f
                        )
                    )
                )
                tooltip.addTooltipToPrevious(
                    AGCGUI.makeTooltip(getTagTooltip(it)),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                if (loadPersistentTags(ship.id, ship, group, AGCGUI.storageIndex).contains(it)) {
                    toReturn.last().setCheckedFromPersistence(true)
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

        fun createCampaignModeButtonGroup(
            ship: FleetMemberAPI,
            group: Int,
            panel: CustomPanelAPI,
            visibleTags: List<String> = Settings.getCurrentWeaponTagList(),
            pinned: Boolean = false,
        ): List<TagButton> {
            storage = Settings.tagStorage[AGCGUI.storageIndex]
            val tags = visibleTags
            val sanitizedTags = sanitizePersistedTags(ship, group)
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
            val toReturn = mutableListOf<TagButton>()
            tags.forEachIndexed { index, tag ->
                val otherSelectedTags = sanitizedTags.toMutableList().apply { remove(tag) }
                val unavailable = !pinned && (
                    isIncompatibleWithExistingTags(tag, otherSelectedTags) ||
                        shouldTagBeDisabled(group, ship, tag)
                    )
                val itemPanel = panel.createCustomPanel(
                    metrics.itemWidth,
                    metrics.itemHeight,
                    DebugBorderPanelPlugin(
                        CampaignContainerType.ITEM,
                        fillColor = if (unavailable) CampaignGuiStyle.UNAVAILABLE_TAG_BACKGROUND_COLOR else null
                    )
                )
                panel.addComponent(itemPanel)
                itemPanel.position.inTL(metrics.xFor(index), metrics.yFor(index))
                val inner = itemPanel.createUIElement(
                    metrics.itemWidth,
                    metrics.itemHeight,
                    false
                )
                val label = truncateLabel(tag, metrics.itemWidth, 22f)
                val baseColor = when {
                    pinned -> Color(190, 175, 95)
                    unavailable -> CampaignGuiStyle.UNAVAILABLE_TAG_BACKGROUND_COLOR
                    else -> Misc.getBasePlayerColor()
                }
                val darkColor = when {
                    pinned -> Color(95, 85, 35)
                    unavailable -> CampaignGuiStyle.UNAVAILABLE_TAG_DARK_COLOR
                    else -> Misc.getDarkPlayerColor()
                }
                val brightColor = when {
                    pinned -> Color(230, 215, 135)
                    unavailable -> CampaignGuiStyle.UNAVAILABLE_TAG_BRIGHT_COLOR
                    else -> Misc.getBrightPlayerColor()
                }
                toReturn.add(
                    TagButton(
                        ship,
                        group,
                        tag,
                        inner.addAreaCheckbox(
                            "",
                            tag,
                            baseColor,
                            darkColor,
                            brightColor,
                            metrics.itemWidth,
                            metrics.itemHeight,
                            0f
                        )
                    )
                )
                inner.addTooltipToPrevious(
                    AGCGUI.makeTooltip(getTagTooltip(tag)),
                    TooltipMakerAPI.TooltipLocation.BELOW
                )
                itemPanel.addUIElement(inner).inTL(CampaignGuiStyle.ITEM_HIGHLIGHT_X_OFFSET, 0f)
                renderColoredTagLabel(
                    itemPanel,
                    label,
                    metrics.itemWidth - 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    metrics.itemHeight - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    textColor = if (unavailable) CampaignGuiStyle.UNAVAILABLE_TAG_TEXT_COLOR else null,
                    enableKeywordColors = !unavailable
                )
                if (sanitizedTags.contains(tag)) {
                    toReturn.last().setCheckedFromPersistence(true)
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
    }

    private fun setCheckedFromPersistence(checked: Boolean) {
        active = checked
        button.isChecked = checked
    }

    private fun updateDisabledButtons() {
        val tags = sanitizePersistedTags(ship, group)
        sameGroupButtons.forEach {
            it.enable()
            val otherTags = tags.toMutableList().apply { remove(it.associatedValue) }
            if (isIncompatibleWithExistingTags(it.associatedValue, otherTags) || shouldTagBeDisabled(
                    group,
                    ship,
                    it.associatedValue
                )
            ) {
                it.disable()
                it.button.isChecked = false
            }
        }
    }

    override fun executeCallbackIfChecked() {
        if (!active && button.isChecked) {
            check()
            updateDisabledButtons()
        } else if (active && !button.isChecked) {
            val tags = loadPersistentTags(ship.id, ship, group, AGCGUI.storageIndex).toMutableList()
            tags.remove(associatedValue)
            persistTags(ship.id, ship, group, AGCGUI.storageIndex, tags)
            uncheck()
            campaignTagSelectionVersion++
            updateDisabledButtons()
        }
        button.isChecked = active
    }

    override fun onActivate() {
        val tags = loadPersistentTags(ship.id, ship, group, AGCGUI.storageIndex).toMutableList()
        tags.add(associatedValue)
        persistTags(ship.id, ship, group, AGCGUI.storageIndex, tags)
        campaignTagSelectionVersion++
    }
}
