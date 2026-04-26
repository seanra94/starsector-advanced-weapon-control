package com.dp.advancedgunnerycontrol.gui

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.TagListView
import com.dp.advancedgunnerycontrol.utils.loadAllTags
import com.dp.advancedgunnerycontrol.utils.loadPersistentTags
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.WeaponGroupSpec
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class ShipView(
    private val tagView: TagListView,
    private val enableTagScroll: Boolean = true,
    private val drawFrame: Boolean = true,
    initialTagScrollOffsets: Map<Int, Int> = emptyMap(),
) : CustomView() {
    companion object {
        private const val TOTAL_WEAPON_GROUPS = 7
        private const val MISC_WIDTH_FRACTION = 0.1667f
        private const val MISC_WIDTH_MIN = 185f
        private const val MISC_WIDTH_MAX = 240f
        private const val PICTURE_HEIGHT_FRACTION = 0.245f
        private const val PICTURE_HEIGHT_MIN = 220f
        private const val PICTURE_HEIGHT_MAX = 240f
        private const val SHIP_MODE_HEIGHT_MIN = 96f
        private const val SHIP_MODE_HEIGHT_MAX = 176f
        private const val SHIP_MODE_SPARE_ROWS = 1
        private const val SECTION_HEADER_HEIGHT = 20f
        private const val WEAPON_ENTRY_HEIGHT = 40f
        private const val WEAPON_IMAGE_WIDTH = 30f
        private const val WEAPON_ENTRY_CONTENT_LEFT_OFFSET = -4f
        private const val WEAPON_ENTRY_TEXT_GAP = 1f
        private const val WEAPON_TEXT_MAX_VISIBLE_CHARS = 31
        private const val WEAPON_TEXT_CHARS_PER_LINE = 18
        private const val WEAPON_ROWS_VISIBLE = 4
        private const val WEAPON_OVERFLOW_ROW_HEIGHT = WEAPON_ENTRY_HEIGHT
        private const val WEAPON_TO_TAG_GAP = 2f
        private const val TAG_ELLIPSIS_HEIGHT = CampaignGuiStyle.TAG_ITEM_HEIGHT
        private const val TAG_SCROLL_STEP = 1
        private const val PICTURE_INFO_ROW_HEIGHT = 32f
        private const val PICTURE_BOTTOM_PADDING = 8f
        private const val PICTURE_IMAGE_TOP_GAP = 2f
        private const val PICTURE_IMAGE_BOTTOM_GAP = 5f
    }

    private data class WeaponEntry(
        val label: String,
        val sprite: String,
    )

    private data class TagScrollRegion(
        val groupIndex: Int,
        val left: Float,
        val right: Float,
        val bottom: Float,
        val top: Float,
        val maxOffset: Int,
    )

    private data class VisibleTagSlice(
        val offset: Int,
        val tags: List<String>,
        val hasAbove: Boolean,
        val hasBelow: Boolean,
        val maxOffset: Int,
    )

    private val log = Global.getLogger(ShipView::class.java)
    private val buttons: MutableList<ButtonBase<*>> = mutableListOf()
    private val groupTagScrollOffsets = initialTagScrollOffsets.toMutableMap()
    private val tagScrollRegions = mutableListOf<TagScrollRegion>()
    private var campaignScrollDirty = false
    private var observedTagSelectionVersion = TagButton.campaignTagSelectionVersion

    private fun addTagButtonGroup(
        group: Int,
        ship: FleetMemberAPI,
        panel: CustomPanelAPI,
        visibleTags: List<String>,
        pinned: Boolean = false,
    ) {
        buttons.addAll(TagButton.createCampaignModeButtonGroup(ship, group, panel, visibleTags, pinned))
    }

    private fun addShipModeButtonGroup(ship: FleetMemberAPI, panel: CustomPanelAPI) {
        buttons.addAll(ShipModeButton.createCampaignModeButtonGroup(ship, panel))
    }

    override fun advance(t: Float) {
        buttons.forEach { it.executeCallbackIfChecked() }
        if (enableTagScroll) {
            tagView.advance()
        }
    }

    fun shouldRegenerate(): Boolean {
        return campaignScrollDirty ||
            observedTagSelectionVersion != TagButton.campaignTagSelectionVersion ||
            (enableTagScroll && tagView.hasChanged())
    }

    fun captureTagScrollOffsets(): Map<Int, Int> = groupTagScrollOffsets.toMap()

    override fun processInput(events: MutableList<InputEventAPI>?) {
        if (tagScrollRegions.isEmpty()) return
        val panelPos = pos ?: return
        events?.forEach { event ->
            if (event.isConsumed || !event.isMouseScrollEvent || event.eventValue == 0) return@forEach
            val mouseX = event.x.toFloat()
            val mouseY = event.y.toFloat()
            val region = tagScrollRegions.firstOrNull {
                mouseX in (panelPos.x + it.left)..(panelPos.x + it.right) &&
                    mouseY in (panelPos.y + it.bottom)..(panelPos.y + it.top)
            } ?: return@forEach

            if (region.maxOffset <= 0) return@forEach
            val current = groupTagScrollOffsets[region.groupIndex] ?: 0
            val delta = if (event.eventValue > 0) -TAG_SCROLL_STEP else TAG_SCROLL_STEP
            val updated = (current + delta).coerceIn(0, region.maxOffset)
            if (updated != current) {
                groupTagScrollOffsets[region.groupIndex] = updated
                campaignScrollDirty = true
                event.consume()
            }
        }
    }

    override fun buttonPressed(buttonId: Any?) {}

    private fun addSectionHeading(
        panel: CustomPanelAPI,
        title: String,
        yOffset: Float = CampaignGuiStyle.PANEL_PADDING,
    ) {
        val headerPanel = panel.createCustomPanel(
            panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING,
            SECTION_HEADER_HEIGHT,
            DebugBorderPanelPlugin(CampaignContainerType.HEADER)
        )
        panel.addComponent(headerPanel)
        headerPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, yOffset)
        val header = headerPanel.createUIElement(
            headerPanel.position.width,
            SECTION_HEADER_HEIGHT,
            false
        )
        header.addSectionHeading(title, Alignment.MID, 0f)
        headerPanel.addUIElement(header).inTL(0f, 0f)
    }

    private fun estimateShipModePanelHeight(miscWidth: Float): Float {
        if (!Settings.isAdvancedMode) {
            return SHIP_MODE_HEIGHT_MIN
        }
        val innerWidth = miscWidth - 2f * CampaignGuiStyle.PANEL_PADDING
        val columns = if (
            Settings.getCurrentShipModes().size > 1 &&
            innerWidth >= (CampaignGuiStyle.SHIP_MODE_ITEM_MIN_WIDTH * 2f + CampaignGuiStyle.SHIP_MODE_ITEM_HGAP)
        ) 2 else 1
        val rows = max(1, kotlin.math.ceil(Settings.getCurrentShipModes().size / columns.toFloat()).toInt()) +
            SHIP_MODE_SPARE_ROWS
        val bodyHeight =
            rows * CampaignGuiStyle.SHIP_MODE_ITEM_HEIGHT + max(0, rows - 1) * CampaignGuiStyle.SHIP_MODE_ITEM_VGAP
        val totalHeight = 2f * CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT + bodyHeight
        return min(SHIP_MODE_HEIGHT_MAX, max(SHIP_MODE_HEIGHT_MIN, totalHeight))
    }

    private fun aggregateWeapons(group: WeaponGroupSpec, ship: FleetMemberAPI): List<WeaponEntry> {
        val counts = linkedMapOf<String, Int>()
        group.slots.mapNotNull { ship.variant.getWeaponId(it) }.forEach { weaponId ->
            counts[weaponId] = (counts[weaponId] ?: 0) + 1
        }
        return counts.entries.map { (weaponId, count) ->
            val spec: WeaponSpecAPI = Global.getSettings().getWeaponSpec(weaponId)
            WeaponEntry(
                label = "$count x ${spec.weaponName}",
                sprite = spec.turretSpriteName
            )
        }.sortedBy { it.label }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): String {
        if (maxCharsPerLine <= 0 || text.length <= maxCharsPerLine) return text
        val wrapped = mutableListOf<String>()
        var current = ""
        text.split(" ").forEach { word ->
            if (current.isBlank()) {
                current = word
            } else if ((current.length + 1 + word.length) <= maxCharsPerLine) {
                current += " $word"
            } else {
                wrapped.add(current)
                current = word
            }
        }
        if (current.isNotBlank()) wrapped.add(current)
        return wrapped.joinToString("\n")
    }

    private fun wrapTextCapped(text: String, maxCharsPerLine: Int, maxLines: Int): String {
        val wrapped = wrapText(text, maxCharsPerLine).split("\n")
        if (wrapped.size <= maxLines) return wrapped.joinToString("\n")
        val visible = wrapped.take(maxLines).toMutableList()
        val lastIndex = visible.lastIndex
        val finalLine = visible[lastIndex]
        visible[lastIndex] = if (finalLine.length > 3) {
            finalLine.take(max(1, maxCharsPerLine - 3)) + "..."
        } else {
            finalLine + "..."
        }
        return visible.joinToString("\n")
    }

    private fun buildPictureInfoRow(
        panel: CustomPanelAPI,
        label: String,
        value: String,
        top: Float,
        width: Float,
    ) {
        val labelWidth = width * 0.34f
        val valueWidth = width - labelWidth

        val labelPanel = panel.createUIElement(labelWidth, PICTURE_INFO_ROW_HEIGHT, false)
        labelPanel.addPara("$label:", 0f)
        panel.addUIElement(labelPanel).inTL(CampaignGuiStyle.PANEL_PADDING, top)

        val valuePanel = panel.createUIElement(valueWidth, PICTURE_INFO_ROW_HEIGHT, false)
        valuePanel.addPara(
            wrapTextCapped(value, max(8, ((valueWidth - 8f) / 6.4f).toInt()), 2),
            0f
        )
        panel.addUIElement(valuePanel).inTL(CampaignGuiStyle.PANEL_PADDING + labelWidth, top)
    }

    private fun buildPictureContainer(panel: CustomPanelAPI, ship: FleetMemberAPI) {
        addSectionHeading(panel, "Ship")

        val bodyTop = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT + PICTURE_IMAGE_TOP_GAP
        val innerWidth = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
        val spriteSize = min(innerWidth - 12f, 84f)

        val imagePanel = panel.createUIElement(spriteSize, spriteSize, false)
        imagePanel.addImage(ship.hullSpec.spriteName, spriteSize, spriteSize, 0f)
        panel.addUIElement(imagePanel).inTL((panel.position.width - spriteSize) / 2f, bodyTop)

        val rowsTop = bodyTop + spriteSize + PICTURE_IMAGE_BOTTOM_GAP
        buildPictureInfoRow(panel, "Hull", ship.hullSpec.hullName, rowsTop, innerWidth)
        buildPictureInfoRow(panel, "Name", ship.shipName, rowsTop + PICTURE_INFO_ROW_HEIGHT, innerWidth)
        buildPictureInfoRow(
            panel,
            "Variant",
            ship.variant?.displayName?.ifBlank { "Default" } ?: "Default",
            rowsTop + 2f * PICTURE_INFO_ROW_HEIGHT,
            innerWidth
        )

        val spacer = panel.createUIElement(innerWidth, PICTURE_BOTTOM_PADDING, false)
        panel.addUIElement(spacer).inTL(CampaignGuiStyle.PANEL_PADDING, rowsTop + 3f * PICTURE_INFO_ROW_HEIGHT)
    }

    private fun buildScrollIndicator(panel: CustomPanelAPI, top: Float, symbol: String) {
        val indicator = panel.createCustomPanel(
            panel.position.width,
            TAG_ELLIPSIS_HEIGHT,
            DebugBorderPanelPlugin(CampaignContainerType.ITEM)
        )
        panel.addComponent(indicator)
        indicator.position.inTL(0f, top)

        val text = indicator.createUIElement(
            indicator.position.width - 2f * CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
            TAG_ELLIPSIS_HEIGHT - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
            false
        )
        text.addPara(symbol, 0f)
        indicator.addUIElement(text).inTL(
            CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING,
            CampaignGuiStyle.ITEM_TEXT_TOP_PADDING
        )
    }

    private fun buildShipModeContainer(panel: CustomPanelAPI, ship: FleetMemberAPI) {
        addSectionHeading(panel, "Ship Modes")
        val bodyTop = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT
        val innerWidth = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
        val bodyHeight = panel.position.height - bodyTop - CampaignGuiStyle.PANEL_PADDING

        if (!Settings.isAdvancedMode) {
            val body = panel.createUIElement(innerWidth, bodyHeight, false)
            body.addPara("Ship AI modes are only available in advanced mode.", 0f)
            panel.addUIElement(body).inTL(CampaignGuiStyle.PANEL_PADDING, bodyTop)
            return
        }

        val itemPanel = panel.createCustomPanel(innerWidth, bodyHeight, null)
        panel.addComponent(itemPanel)
        itemPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, bodyTop)
        addShipModeButtonGroup(ship, itemPanel)
    }

    private fun addWeaponTooltip(
        anchorPanel: CustomPanelAPI,
        entries: List<WeaponEntry>,
    ) {
        val binder = anchorPanel.createUIElement(0f, 0f, false)
        binder.addTooltip(anchorPanel, TooltipMakerAPI.TooltipLocation.BELOW, 260f) { tooltip ->
            tooltip.setParaFontColor(CampaignGuiStyle.TOOLTIP_TEXT_COLOR)
            entries.forEach { entry ->
                if (entry.sprite.isNotBlank()) {
                    val imageText = tooltip.beginImageWithText(entry.sprite, 16f)
                    imageText.setParaFontColor(CampaignGuiStyle.TOOLTIP_TEXT_COLOR)
                    imageText.addPara(entry.label, 0f)
                    tooltip.addImageWithText(2f)
                } else {
                    tooltip.addPara(entry.label, CampaignGuiStyle.TOOLTIP_TEXT_COLOR, 0f)
                }
            }
        }
        anchorPanel.addUIElement(binder).inTL(0f, 0f)
    }

    private fun fitSprite(spriteName: String, maxWidth: Float, maxHeight: Float): Pair<Float, Float> {
        if (spriteName.isBlank()) return 0f to 0f
        return try {
            val sprite = Global.getSettings().getSprite(spriteName)
            val spriteWidth = sprite.width
            val spriteHeight = sprite.height
            if (spriteWidth <= 0f || spriteHeight <= 0f) {
                min(maxWidth, maxHeight) to min(maxWidth, maxHeight)
            } else {
                val scale = min(maxWidth / spriteWidth, maxHeight / spriteHeight)
                spriteWidth * scale to spriteHeight * scale
            }
        } catch (_: Throwable) {
            min(maxWidth, maxHeight) to min(maxWidth, maxHeight)
        }
    }

    private fun buildWeaponEntryContainer(
        panel: CustomPanelAPI,
        entry: WeaponEntry?,
        top: Float,
        imageText: String? = null,
    ) {
        val entryPanel = panel.createCustomPanel(
            panel.position.width,
            WEAPON_ENTRY_HEIGHT,
            DebugBorderPanelPlugin(CampaignContainerType.WEAPON_ENTRY)
        )
        panel.addComponent(entryPanel)
        entryPanel.position.inTL(0f, top)

        if (entry == null && imageText == null) return

        if (imageText != null) {
            val imageTextPanel = entryPanel.createUIElement(
                WEAPON_IMAGE_WIDTH,
                WEAPON_ENTRY_HEIGHT - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
                false
            )
            imageTextPanel.addPara(imageText, 0f)
            entryPanel.addUIElement(imageTextPanel).inTL(0f, CampaignGuiStyle.ITEM_TEXT_TOP_PADDING)
        } else if (entry?.sprite?.isNotBlank() == true) {
            val imageMaxHeight = WEAPON_ENTRY_HEIGHT - 4f
            val (imageWidth, imageHeight) = fitSprite(entry.sprite, WEAPON_IMAGE_WIDTH, imageMaxHeight)
            if (imageWidth > 0f && imageHeight > 0f) {
                val imagePanel = entryPanel.createUIElement(WEAPON_IMAGE_WIDTH, WEAPON_ENTRY_HEIGHT, false)
                imagePanel.addImage(entry.sprite, imageWidth, imageHeight, 0f)
                entryPanel.addUIElement(imagePanel).inTL(
                    WEAPON_ENTRY_CONTENT_LEFT_OFFSET,
                    max(0f, (WEAPON_ENTRY_HEIGHT - imageHeight) / 2f)
                )
            }
        }

        if (entry == null) return

        val textLeft = WEAPON_ENTRY_CONTENT_LEFT_OFFSET + WEAPON_IMAGE_WIDTH + WEAPON_ENTRY_TEXT_GAP
        val textWidth = entryPanel.position.width - textLeft - WEAPON_ENTRY_TEXT_GAP
        val textPanel = entryPanel.createUIElement(
            textWidth,
            WEAPON_ENTRY_HEIGHT - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
            false
        )
        val displayLabel = truncateLabelByLength(entry.label, WEAPON_TEXT_MAX_VISIBLE_CHARS)
        textPanel.addPara(
            wrapTextCapped(displayLabel, WEAPON_TEXT_CHARS_PER_LINE, 2),
            0f
        )
        entryPanel.addUIElement(textPanel).inTL(textLeft, CampaignGuiStyle.ITEM_TEXT_TOP_PADDING)
    }

    private fun buildWeaponContainer(
        panel: CustomPanelAPI,
        entries: List<WeaponEntry>,
    ) {
        repeat(WEAPON_ROWS_VISIBLE) { index ->
            buildWeaponEntryContainer(
                panel,
                entries.getOrNull(index),
                index * WEAPON_ENTRY_HEIGHT
            )
        }

        if (entries.size == WEAPON_ROWS_VISIBLE + 1) {
            buildWeaponEntryContainer(
                panel,
                entries.getOrNull(WEAPON_ROWS_VISIBLE),
                WEAPON_ROWS_VISIBLE * WEAPON_ENTRY_HEIGHT
            )
        } else if (entries.size > WEAPON_ROWS_VISIBLE + 1) {
            buildWeaponEntryContainer(
                panel,
                WeaponEntry("Hover for full weapon list", ""),
                WEAPON_ROWS_VISIBLE * WEAPON_ENTRY_HEIGHT,
                imageText = " ..."
            )
        } else {
            buildWeaponEntryContainer(
                panel,
                null,
                WEAPON_ROWS_VISIBLE * WEAPON_ENTRY_HEIGHT
            )
        }
    }

    private fun computeVisibleTagSlice(tagsToRender: List<String>, tagContainerHeight: Float, groupIndex: Int): VisibleTagSlice {
        val tagCount = tagsToRender.size
        val perRow = CampaignGuiStyle.TAG_ITEM_HEIGHT + CampaignGuiStyle.TAG_ITEM_VGAP
        val totalSlots = max(1, floor((tagContainerHeight + CampaignGuiStyle.TAG_ITEM_VGAP) / perRow).toInt())
        if (tagCount <= totalSlots) {
            groupTagScrollOffsets[groupIndex] = 0
            return VisibleTagSlice(0, tagsToRender, false, false, 0)
        }

        var offset = (groupTagScrollOffsets[groupIndex] ?: 0).coerceAtLeast(0)
        val maxOffset = max(0, tagCount - 1)
        offset = offset.coerceAtMost(maxOffset)
        var hasAbove = offset > 0
        var visibleSlots = totalSlots - if (hasAbove) 1 else 0
        var hasBelow = offset + visibleSlots < tagCount
        if (hasBelow) {
            visibleSlots -= 1
        }
        if (visibleSlots <= 0) {
            visibleSlots = 1
        }
        val finalMaxOffset = max(0, tagCount - visibleSlots)
        offset = offset.coerceAtMost(finalMaxOffset)
        hasAbove = offset > 0
        val endExclusive = min(tagCount, offset + visibleSlots)
        hasBelow = endExclusive < tagCount
        groupTagScrollOffsets[groupIndex] = offset
        val tags = tagsToRender.subList(offset, endExclusive)
        return VisibleTagSlice(offset, tags, hasAbove, hasBelow, finalMaxOffset)
    }

    private fun buildTagContainer(
        panel: CustomPanelAPI,
        ship: FleetMemberAPI,
        groupIndex: Int,
        relativeScrollLeft: Float,
        relativeScrollRight: Float,
        relativeScrollBottom: Float,
        relativeScrollTop: Float,
    ) {
        val allTags = Settings.getCurrentWeaponTagList()
        val selectedTags = loadPersistentTags(ship.id, ship, groupIndex, AGCGUI.storageIndex)
            .distinct()
            .filter { allTags.contains(it) }
            .sortedBy { allTags.indexOf(it) }
        val unselectedTags = allTags.filterNot { selectedTags.contains(it) }
        val perRow = CampaignGuiStyle.TAG_ITEM_HEIGHT + CampaignGuiStyle.TAG_ITEM_VGAP
        val totalSlots = max(1, floor((panel.position.height + CampaignGuiStyle.TAG_ITEM_VGAP) / perRow).toInt())
        val pinnedVisibleTags = selectedTags.take(max(0, totalSlots - 1))
        val pinnedHeight = if (pinnedVisibleTags.isEmpty()) {
            0f
        } else {
            pinnedVisibleTags.size * CampaignGuiStyle.TAG_ITEM_HEIGHT +
                max(0, pinnedVisibleTags.size - 1) * CampaignGuiStyle.TAG_ITEM_VGAP
        }
        val normalTagAreaHeight = max(CampaignGuiStyle.TAG_ITEM_HEIGHT, panel.position.height - pinnedHeight)
        val slice = computeVisibleTagSlice(unselectedTags, normalTagAreaHeight, groupIndex)
        val ellipsisTopHeight = if (slice.hasAbove) TAG_ELLIPSIS_HEIGHT else 0f
        val ellipsisBottomHeight = if (slice.hasBelow) TAG_ELLIPSIS_HEIGHT else 0f
        val renderedTagsHeight = if (slice.tags.isEmpty()) {
            0f
        } else {
            slice.tags.size * CampaignGuiStyle.TAG_ITEM_HEIGHT +
                max(0, slice.tags.size - 1) * CampaignGuiStyle.TAG_ITEM_VGAP
        }
        val visibleTagHeight = max(
            CampaignGuiStyle.TAG_ITEM_HEIGHT,
            normalTagAreaHeight - ellipsisTopHeight - ellipsisBottomHeight
        )

        if (pinnedVisibleTags.isNotEmpty()) {
            val pinnedPanel = panel.createCustomPanel(panel.position.width, pinnedHeight, null)
            panel.addComponent(pinnedPanel)
            pinnedPanel.position.inTL(0f, 0f)
            addTagButtonGroup(groupIndex, ship, pinnedPanel, pinnedVisibleTags, pinned = true)
        }

        if (slice.hasAbove) {
            buildScrollIndicator(panel, pinnedHeight, "^ ^ ^")
        }

        val tagPanel = panel.createCustomPanel(panel.position.width, min(visibleTagHeight, renderedTagsHeight), null)
        panel.addComponent(tagPanel)
        tagPanel.position.inTL(0f, pinnedHeight + ellipsisTopHeight)
        addTagButtonGroup(groupIndex, ship, tagPanel, slice.tags)

        if (slice.hasBelow) {
            buildScrollIndicator(panel, pinnedHeight + ellipsisTopHeight + tagPanel.position.height, "v v v")
        }

        tagScrollRegions.add(
            TagScrollRegion(
                groupIndex = groupIndex,
                left = relativeScrollLeft,
                right = relativeScrollRight,
                bottom = relativeScrollBottom,
                top = relativeScrollTop,
                maxOffset = slice.maxOffset,
            )
        )
    }

    private fun buildWeaponGroupContainer(
        panel: CustomPanelAPI,
        ship: FleetMemberAPI,
        groupIndex: Int,
        relativeGroupLeft: Float,
        relativeGroupTopOffset: Float,
        contentHeight: Float,
    ) {
        addSectionHeading(panel, "Group ${groupIndex + 1}")
        val group = ship.variant.weaponGroups.getOrNull(groupIndex)
        val entries = group?.let { aggregateWeapons(it, ship) }.orEmpty()
        if (entries.isEmpty()) return

        val topContent = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT
        val innerWidth = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
        val weaponContainerHeight =
            WEAPON_ROWS_VISIBLE * WEAPON_ENTRY_HEIGHT +
            WEAPON_OVERFLOW_ROW_HEIGHT
        val tagContainerTop = topContent + weaponContainerHeight + WEAPON_TO_TAG_GAP
        val tagContainerHeight = max(36f, panel.position.height - tagContainerTop)

        val weaponContainer = panel.createCustomPanel(
            innerWidth,
            weaponContainerHeight,
            DebugBorderPanelPlugin(CampaignContainerType.WEAPON_LIST)
        )
        panel.addComponent(weaponContainer)
        weaponContainer.position.inTL(CampaignGuiStyle.PANEL_PADDING, topContent)
        buildWeaponContainer(weaponContainer, entries)
        if (entries.isNotEmpty()) {
            addWeaponTooltip(weaponContainer, entries)
        }

        val tagContainer = panel.createCustomPanel(
            innerWidth,
            tagContainerHeight,
            DebugBorderPanelPlugin(CampaignContainerType.TAG_LIST)
        )
        panel.addComponent(tagContainer)
        tagContainer.position.inTL(CampaignGuiStyle.PANEL_PADDING, tagContainerTop)

        val relativeTopOffset = relativeGroupTopOffset + tagContainerTop
        val relativeBottom = contentHeight - relativeTopOffset - tagContainerHeight
        buildTagContainer(
            tagContainer,
            ship,
            groupIndex,
            relativeScrollLeft = relativeGroupLeft + CampaignGuiStyle.PANEL_PADDING,
            relativeScrollRight = relativeGroupLeft + CampaignGuiStyle.PANEL_PADDING + innerWidth,
            relativeScrollBottom = relativeBottom,
            relativeScrollTop = relativeBottom + tagContainerHeight,
        )
    }

    private fun buildWeaponGroupsContainer(panel: CustomPanelAPI, ship: FleetMemberAPI, contentHeight: Float, miscWidth: Float) {
        val headerBottom = 0f
        val innerWidth = panel.position.width
        val innerHeight = panel.position.height
        val cardWidth = innerWidth / TOTAL_WEAPON_GROUPS

        repeat(TOTAL_WEAPON_GROUPS) { index ->
            val effectiveWidth = if (index == TOTAL_WEAPON_GROUPS - 1) {
                innerWidth - cardWidth * (TOTAL_WEAPON_GROUPS - 1)
            } else {
                cardWidth
            }
            val groupPanel = panel.createCustomPanel(
                effectiveWidth,
                innerHeight,
                DebugBorderPanelPlugin(CampaignContainerType.WEAPON_GROUP)
            )
            panel.addComponent(groupPanel)
            groupPanel.position.inTL(
                index * cardWidth,
                headerBottom
            )
            buildWeaponGroupContainer(
                groupPanel,
                ship,
                index,
                relativeGroupLeft = miscWidth + index * cardWidth,
                relativeGroupTopOffset = headerBottom,
                contentHeight = contentHeight
            )
        }
    }

    fun buildIn(
        panel: CustomPanelAPI,
        ship: FleetMemberAPI,
        buildOptionsPanel: ((CustomPanelAPI) -> Unit)? = null,
        optionsPreferredHeight: Float? = null,
    ) {
        buttons.clear()
        tagScrollRegions.clear()
        campaignScrollDirty = false
        observedTagSelectionVersion = TagButton.campaignTagSelectionVersion
        Settings.hotAddTags(loadAllTags(ship))

        val miscWidth = min(MISC_WIDTH_MAX, max(MISC_WIDTH_MIN, panel.position.width * MISC_WIDTH_FRACTION))
        val weaponGroupsWidth = panel.position.width - miscWidth
        val pictureHeight = min(PICTURE_HEIGHT_MAX, max(PICTURE_HEIGHT_MIN, panel.position.height * PICTURE_HEIGHT_FRACTION))
        val minimumShipModeHeight = estimateShipModePanelHeight(miscWidth)
        val desiredOptionsHeight = optionsPreferredHeight ?: max(120f, panel.position.height - pictureHeight - minimumShipModeHeight)
        val optionsHeight = min(desiredOptionsHeight, panel.position.height - pictureHeight - minimumShipModeHeight)
        val shipModeHeight = max(minimumShipModeHeight, panel.position.height - pictureHeight - optionsHeight)

        log.info(
            "[AGC_CAMPAIGN_UI] ShipView.buildIn ship=${ship.shipName} panel=${panel.position.width}x${panel.position.height} " +
                "miscW=$miscWidth groupsW=$weaponGroupsWidth optionsH=$optionsHeight groups=${ship.variant.weaponGroups.size}"
        )

        val miscPanel = panel.createCustomPanel(
            miscWidth,
            panel.position.height,
            DebugBorderPanelPlugin(CampaignContainerType.MISC)
        )
        panel.addComponent(miscPanel)
        miscPanel.position.inTL(0f, 0f)

        val picturePanel = miscPanel.createCustomPanel(
            miscWidth,
            pictureHeight,
            DebugBorderPanelPlugin(CampaignContainerType.PICTURE)
        )
        miscPanel.addComponent(picturePanel)
        picturePanel.position.inTL(0f, 0f)
        buildPictureContainer(picturePanel, ship)

        val optionsPanel = miscPanel.createCustomPanel(
            miscWidth,
            optionsHeight,
            DebugBorderPanelPlugin(CampaignContainerType.OPTIONS)
        )
        miscPanel.addComponent(optionsPanel)
        optionsPanel.position.belowLeft(picturePanel, 0f)
        buildOptionsPanel?.invoke(optionsPanel)

        val shipModePanel = miscPanel.createCustomPanel(
            miscWidth,
            shipModeHeight,
            DebugBorderPanelPlugin(CampaignContainerType.SHIP_MODE)
        )
        miscPanel.addComponent(shipModePanel)
        shipModePanel.position.belowLeft(optionsPanel, 0f)
        buildShipModeContainer(shipModePanel, ship)

        val weaponGroupsPanel = panel.createCustomPanel(
            weaponGroupsWidth,
            panel.position.height,
            DebugBorderPanelPlugin(CampaignContainerType.WEAPON_GROUPS)
        )
        panel.addComponent(weaponGroupsPanel)
        weaponGroupsPanel.position.rightOfTop(miscPanel, 0f)
        buildWeaponGroupsContainer(weaponGroupsPanel, ship, panel.position.height, miscWidth)
    }

    fun showShipModes(attributes: GUIAttributes) {
        val ship = attributes.ship ?: return
        val screenWidthUi = Global.getSettings().screenWidthPixels / Global.getSettings().screenScaleMult
        val panelHeight = Global.getSettings().screenHeightPixels / Global.getSettings().screenScaleMult - 40f
        attributes.customPanel = attributes.visualPanel?.showCustomPanel(screenWidthUi, panelHeight, this)
        attributes.customPanel?.position?.inTL(0f, 20f)
        attributes.customPanel?.let {
            buildIn(it, ship)
        }
    }

    override fun render(alpha: Float) {
        if (!drawFrame) return
        super.render(alpha)
    }
}
