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
import kotlin.math.max

class TagButton(var ship: FleetMemberAPI, var group: Int, tag: String, button: ButtonAPI) :
    ButtonBase<String>(tag, button, false) {

    private var visualStateChecked: Boolean? = null

    companion object {
        private var storage = Settings.tagStorage[AGCGUI.storageIndex]
        var campaignTagSelectionVersion = 0
            private set

        private fun sanitizePersistedTags(ship: FleetMemberAPI, group: Int): MutableList<String> {
            val allTags = Settings.getCurrentWeaponTagList()
            val loaded = loadPersistentTags(ship.id, ship, group, AGCGUI.storageIndex)
            val sanitized = loaded
                .filter { allTags.contains(it) }
                .toMutableList()
            var shouldPersist = sanitized != loaded
            var changed = true
            while (changed) {
                changed = false
                sanitized.toList().forEach { persistedTag ->
                    val otherTags = sanitized.toMutableList().apply { remove(persistedTag) }
                    if (isIncompatibleWithExistingTags(persistedTag, otherTags) || shouldTagBeDisabled(group, ship, persistedTag)) {
                        sanitized.remove(persistedTag)
                        changed = true
                        shouldPersist = true
                    }
                }
            }
            if (shouldPersist) {
                persistTags(ship.id, ship, group, AGCGUI.storageIndex, sanitized)
            }
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
            val itemHeight = tags.map { tag ->
                computeWrappedLabelLayout(
                    text = tag,
                    rowWidth = panel.position.width - 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    minButtonHeight = CampaignGuiStyle.TAG_ITEM_HEIGHT,
                    horizontalPadding = 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    verticalPadding = 2f * CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    maxLines = 1
                ).rowHeight
            }.maxOrNull() ?: CampaignGuiStyle.TAG_ITEM_HEIGHT
            val metrics = computeWrapGridMetrics(
                itemCount = max(tags.size, 1),
                availableWidth = panel.position.width,
                availableHeight = panel.position.height,
                minItemWidth = CampaignGuiStyle.TAG_ITEM_MIN_WIDTH,
                itemHeight = itemHeight,
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
                val shell = addCampaignTagModeToggleShell(
                    parent = panel,
                    data = tag,
                    x = metrics.xFor(index),
                    y = metrics.yFor(index),
                    width = metrics.itemWidth,
                    height = metrics.itemHeight,
                    tooltip = getTagTooltip(tag),
                    unavailable = unavailable
                )
                toReturn.add(
                    TagButton(
                        ship,
                        group,
                        tag,
                        shell.button
                    )
                )
                renderTagLabel(
                    shell.panel,
                    tag,
                    metrics.itemWidth - 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    metrics.itemHeight - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
                    CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                    textColor = if (unavailable) CampaignGuiStyle.DISABLED_TAG_TEXT_COLOR else null
                )
                if (sanitizedTags.contains(tag)) {
                    toReturn.last().setCheckedFromPersistence(true)
                }
            }
            toReturn.forEach {
                it.sameGroupButtons = toReturn
            }
            return toReturn
        }
    }

    private fun setCheckedFromPersistence(checked: Boolean) {
        active = checked
        button.isChecked = checked
        applyToggleableVisualState(force = true)
    }

    private fun applyToggleableVisualState(force: Boolean = false) {
        if (!force && visualStateChecked == button.isChecked) return
        CampaignGuiStyle.applyToggleableCheckboxVisualState(button)
        visualStateChecked = button.isChecked
    }

    private fun applyUnavailableVisualState() {
        CampaignGuiStyle.applyUnavailableCheckboxVisualState(button)
        visualStateChecked = null
    }

    private fun updateDisabledButtons() {
        val tags = sanitizePersistedTags(ship, group)
        enable()
        val selfOtherTags = tags.toMutableList().apply { remove(associatedValue) }
        if (isIncompatibleWithExistingTags(associatedValue, selfOtherTags) || shouldTagBeDisabled(group, ship, associatedValue)) {
            // TODO: If Starsector exposes a stable disabled-click sound API, trigger it when unavailable tags are clicked.
            disable()
            button.isChecked = false
            active = false
            applyUnavailableVisualState()
        } else {
            applyToggleableVisualState()
        }
        sameGroupButtons.forEach {
            val tagButton = it as? TagButton ?: return@forEach
            val otherTags = tags.toMutableList().apply { remove(it.associatedValue) }
            tagButton.enable()
            if (isIncompatibleWithExistingTags(it.associatedValue, otherTags) || shouldTagBeDisabled(
                    group,
                    ship,
                    it.associatedValue
                )
            ) {
                tagButton.disable()
                tagButton.button.isChecked = false
                tagButton.active = false
                tagButton.applyUnavailableVisualState()
            } else {
                tagButton.applyToggleableVisualState()
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
        applyToggleableVisualState()
    }

    override fun onActivate() {
        val tags = loadPersistentTags(ship.id, ship, group, AGCGUI.storageIndex).toMutableList()
        tags.add(associatedValue)
        persistTags(ship.id, ship, group, AGCGUI.storageIndex, tags)
        campaignTagSelectionVersion++
    }
}
