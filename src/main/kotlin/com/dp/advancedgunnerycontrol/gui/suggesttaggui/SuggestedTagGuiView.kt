package com.dp.advancedgunnerycontrol.gui.suggesttaggui

import com.dp.advancedgunnerycontrol.gui.ButtonBase
import com.dp.advancedgunnerycontrol.gui.CampaignContainerType
import com.dp.advancedgunnerycontrol.gui.CampaignGuiStyle
import com.dp.advancedgunnerycontrol.gui.CustomView
import com.dp.advancedgunnerycontrol.gui.DebugBorderPanelPlugin
import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.getSuggestedModesForWeaponId
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SuggestedTagGuiView(
    private val weaponListView: WeaponListView,
    private val initialTagScrollOffsets: Map<String, Int> = emptyMap(),
) : CustomView() {
    companion object {
        private const val TOTAL_COLUMNS = 7
        private const val MISC_WIDTH_FRACTION = 0.1667f
        private const val MISC_WIDTH_MIN = 185f
        private const val MISC_WIDTH_MAX = 240f
        private const val SECTION_HEADER_HEIGHT = 20f
        private const val WEAPON_INFO_HEIGHT = 240f
        private const val WEAPON_IMAGE_TOP_GAP = 5f
        private const val WEAPON_IMAGE_BOTTOM_GAP = 6f
        private const val WEAPON_IMAGE_MAX = 52f
        private const val TAG_SCROLL_STEP = 1
        private const val TAG_ELLIPSIS_HEIGHT = CampaignGuiStyle.TAG_ITEM_HEIGHT
    }

    private data class TagScrollRegion(
        val weaponId: String,
        val left: Float,
        val right: Float,
        val bottom: Float,
        val top: Float,
        val maxOffset: Int,
    )

    private data class VisibleTagSlice(
        val tags: List<String>,
        val hasAbove: Boolean,
        val hasBelow: Boolean,
        val maxOffset: Int,
    )

    private val buttons: MutableList<ButtonBase<*>> = mutableListOf()
    private val tagScrollOffsets = initialTagScrollOffsets.toMutableMap()
    private val tagScrollRegions = mutableListOf<TagScrollRegion>()
    private var scrollDirty = false
    private var observedTagSelectionVersion = SuggestedTagButton.suggestedTagSelectionVersion

    override fun advance(p0: Float) {
        buttons.forEach { it.executeCallbackIfChecked() }
    }

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
            val current = tagScrollOffsets[region.weaponId] ?: 0
            val delta = if (event.eventValue > 0) -TAG_SCROLL_STEP else TAG_SCROLL_STEP
            val updated = (current + delta).coerceIn(0, region.maxOffset)
            if (updated != current) {
                tagScrollOffsets[region.weaponId] = updated
                scrollDirty = true
                event.consume()
            }
        }
    }

    override fun buttonPressed(p0: Any?) {}

    fun captureTagScrollOffsets(): Map<String, Int> = tagScrollOffsets.toMap()

    fun shouldRegenerate(): Boolean =
        scrollDirty || observedTagSelectionVersion != SuggestedTagButton.suggestedTagSelectionVersion

    private fun addSectionHeading(panel: CustomPanelAPI, title: String, top: Float = CampaignGuiStyle.PANEL_PADDING) {
        val headerPanel = panel.createCustomPanel(
            panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING,
            SECTION_HEADER_HEIGHT,
            DebugBorderPanelPlugin(CampaignContainerType.HEADER)
        )
        panel.addComponent(headerPanel)
        headerPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, top)
        val header = headerPanel.createUIElement(headerPanel.position.width, SECTION_HEADER_HEIGHT, false)
        header.addSectionHeading(title, Alignment.MID, 0f)
        headerPanel.addUIElement(header).inTL(0f, 0f)
    }

    private fun fitSprite(spriteName: String, maxWidth: Float, maxHeight: Float): Pair<Float, Float> {
        if (spriteName.isBlank()) return 0f to 0f
        return try {
            val sprite = Global.getSettings().getSprite(spriteName)
            if (sprite.width <= 0f || sprite.height <= 0f) {
                min(maxWidth, maxHeight) to min(maxWidth, maxHeight)
            } else {
                val scale = min(maxWidth / sprite.width, maxHeight / sprite.height)
                sprite.width * scale to sprite.height * scale
            }
        } catch (_: Throwable) {
            min(maxWidth, maxHeight) to min(maxWidth, maxHeight)
        }
    }

    private fun formatStat(value: Float): String {
        if (!value.isFinite() || value < 0f) return "0"
        return if (kotlin.math.abs(value - value.roundToInt()) < 0.05f) {
            value.roundToInt().toString()
        } else {
            "%.2f".format(value)
        }
    }

    private fun buildWeaponInfo(panel: CustomPanelAPI, weaponId: String?) {
        addSectionHeading(panel, if (weaponId == null) "Weapon" else "Weapon")
        if (weaponId == null) return

        val spec = Global.getSettings().getWeaponSpec(weaponId)
        val innerWidth = panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING
        val imageMax = min(WEAPON_IMAGE_MAX, innerWidth - 8f)
        val bodyTop = CampaignGuiStyle.PANEL_PADDING + SECTION_HEADER_HEIGHT + WEAPON_IMAGE_TOP_GAP
        val (imageWidth, imageHeight) = fitSprite(spec.turretSpriteName, imageMax, imageMax)
        if (imageWidth > 0f && imageHeight > 0f) {
            val imagePanel = panel.createUIElement(imageWidth, imageHeight, false)
            imagePanel.addImage(spec.turretSpriteName, imageWidth, imageHeight, 0f)
            panel.addUIElement(imagePanel).inTL((panel.position.width - imageWidth) / 2f, bodyTop)
        }

        val statsTop = bodyTop + imageMax + WEAPON_IMAGE_BOTTOM_GAP
        val statPanel = panel.createUIElement(innerWidth, max(20f, panel.position.height - statsTop), false)
        statLines(spec).forEachIndexed { index, line ->
            statPanel.addPara(line, if (index == 0) 0f else 1f)
        }
        panel.addUIElement(statPanel).inTL(CampaignGuiStyle.PANEL_PADDING, statsTop)
    }

    private fun statLines(spec: WeaponSpecAPI): List<String> {
        val derived = spec.derivedStats
        val damage = max(derived.burstDamage, derived.damagePerShot)
        val emp = max(derived.empPerSecond, derived.empPerShot)
        return listOf(
            "Name: ${spec.weaponName}",
            "Size: ${spec.size}",
            "Type: ${spec.type}",
            "Damage: ${formatStat(damage)}",
            "EMP: ${formatStat(emp)}",
            "Range: ${formatStat(spec.maxRange)}",
            "Flux/Second: ${formatStat(derived.fluxPerSecond)}",
            "Flux/Damage: ${formatStat(derived.fluxPerDam)}",
        )
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

    private fun computeVisibleTagSlice(tagsToRender: List<String>, tagContainerHeight: Float, weaponId: String): VisibleTagSlice {
        val tagCount = tagsToRender.size
        val perRow = CampaignGuiStyle.TAG_ITEM_HEIGHT + CampaignGuiStyle.TAG_ITEM_VGAP
        val totalSlots = max(1, floor((tagContainerHeight + CampaignGuiStyle.TAG_ITEM_VGAP) / perRow).toInt())
        if (tagCount <= totalSlots) {
            tagScrollOffsets[weaponId] = 0
            return VisibleTagSlice(tagsToRender, false, false, 0)
        }

        var offset = (tagScrollOffsets[weaponId] ?: 0).coerceAtLeast(0)
        val maxOffset = max(0, tagCount - 1)
        offset = offset.coerceAtMost(maxOffset)
        var hasAbove = offset > 0
        var visibleSlots = totalSlots - if (hasAbove) 1 else 0
        var hasBelow = offset + visibleSlots < tagCount
        if (hasBelow) visibleSlots -= 1
        if (visibleSlots <= 0) visibleSlots = 1
        val finalMaxOffset = max(0, tagCount - visibleSlots)
        offset = offset.coerceAtMost(finalMaxOffset)
        hasAbove = offset > 0
        val endExclusive = min(tagCount, offset + visibleSlots)
        hasBelow = endExclusive < tagCount
        tagScrollOffsets[weaponId] = offset
        return VisibleTagSlice(tagsToRender.subList(offset, endExclusive), hasAbove, hasBelow, finalMaxOffset)
    }

    private fun buildTagContainer(
        panel: CustomPanelAPI,
        weaponId: String?,
        relativeLeft: Float,
        relativeBottom: Float,
    ) {
        if (weaponId == null) return

        val allTags = Settings.getCurrentWeaponTagList()
        val selectedTags = getSuggestedModesForWeaponId(weaponId)
            .distinct()
            .filter { allTags.contains(it) }
            .sortedBy { allTags.indexOf(it) }
        val unselectedTags = allTags.filterNot { selectedTags.contains(it) }
        val perRow = CampaignGuiStyle.TAG_ITEM_HEIGHT + CampaignGuiStyle.TAG_ITEM_VGAP
        val totalSlots = max(1, floor((panel.position.height + CampaignGuiStyle.TAG_ITEM_VGAP) / perRow).toInt())
        val pinnedTags = selectedTags.take(max(0, totalSlots - 1))
        val pinnedHeight = if (pinnedTags.isEmpty()) {
            0f
        } else {
            pinnedTags.size * CampaignGuiStyle.TAG_ITEM_HEIGHT +
                max(0, pinnedTags.size - 1) * CampaignGuiStyle.TAG_ITEM_VGAP
        }
        val normalHeight = max(CampaignGuiStyle.TAG_ITEM_HEIGHT, panel.position.height - pinnedHeight)
        val slice = computeVisibleTagSlice(unselectedTags, normalHeight, weaponId)
        val topIndicatorHeight = if (slice.hasAbove) TAG_ELLIPSIS_HEIGHT else 0f
        val bottomIndicatorHeight = if (slice.hasBelow) TAG_ELLIPSIS_HEIGHT else 0f
        val renderedTagsHeight = if (slice.tags.isEmpty()) {
            0f
        } else {
            slice.tags.size * CampaignGuiStyle.TAG_ITEM_HEIGHT +
                max(0, slice.tags.size - 1) * CampaignGuiStyle.TAG_ITEM_VGAP
        }
        val visibleTagHeight = max(
            CampaignGuiStyle.TAG_ITEM_HEIGHT,
            normalHeight - topIndicatorHeight - bottomIndicatorHeight
        )

        if (pinnedTags.isNotEmpty()) {
            val pinnedPanel = panel.createCustomPanel(panel.position.width, pinnedHeight, null)
            panel.addComponent(pinnedPanel)
            pinnedPanel.position.inTL(0f, 0f)
            buttons.addAll(SuggestedTagButton.createCampaignButtonGroup(weaponId, pinnedPanel, pinnedTags, pinned = true))
        }
        if (slice.hasAbove) buildScrollIndicator(panel, pinnedHeight, "^ ^ ^")

        val tagPanel = panel.createCustomPanel(panel.position.width, min(visibleTagHeight, renderedTagsHeight), null)
        panel.addComponent(tagPanel)
        tagPanel.position.inTL(0f, pinnedHeight + topIndicatorHeight)
        buttons.addAll(SuggestedTagButton.createCampaignButtonGroup(weaponId, tagPanel, slice.tags))

        if (slice.hasBelow) {
            buildScrollIndicator(panel, pinnedHeight + topIndicatorHeight + tagPanel.position.height, "v v v")
        }

        tagScrollRegions.add(
            TagScrollRegion(
                weaponId = weaponId,
                left = relativeLeft,
                right = relativeLeft + panel.position.width,
                bottom = relativeBottom,
                top = relativeBottom + panel.position.height,
                maxOffset = slice.maxOffset,
            )
        )
    }

    private fun buildWeaponColumn(
        panel: CustomPanelAPI,
        weaponId: String?,
        relativeLeft: Float,
        contentHeight: Float,
    ) {
        val infoPanel = panel.createCustomPanel(
            panel.position.width,
            WEAPON_INFO_HEIGHT,
            DebugBorderPanelPlugin(CampaignContainerType.WEAPON_LIST)
        )
        panel.addComponent(infoPanel)
        infoPanel.position.inTL(0f, 0f)
        buildWeaponInfo(infoPanel, weaponId)

        val tagTop = WEAPON_INFO_HEIGHT
        val tagHeight = max(36f, panel.position.height - tagTop)
        val tagPanel = panel.createCustomPanel(
            panel.position.width,
            tagHeight,
            DebugBorderPanelPlugin(CampaignContainerType.TAG_LIST)
        )
        panel.addComponent(tagPanel)
        tagPanel.position.inTL(0f, tagTop)
        buildTagContainer(
            tagPanel,
            weaponId,
            relativeLeft = relativeLeft,
            relativeBottom = contentHeight - tagTop - tagHeight,
        )
    }

    private fun buildWeaponColumns(panel: CustomPanelAPI, contentHeight: Float, miscWidth: Float) {
        val ids = weaponListView.currentIds()
        val cardWidth = panel.position.width / TOTAL_COLUMNS
        repeat(TOTAL_COLUMNS) { index ->
            val effectiveWidth = if (index == TOTAL_COLUMNS - 1) {
                panel.position.width - cardWidth * (TOTAL_COLUMNS - 1)
            } else {
                cardWidth
            }
            val column = panel.createCustomPanel(
                effectiveWidth,
                panel.position.height,
                DebugBorderPanelPlugin(CampaignContainerType.WEAPON_GROUP)
            )
            panel.addComponent(column)
            column.position.inTL(index * cardWidth, 0f)
            buildWeaponColumn(
                column,
                ids.getOrNull(index),
                relativeLeft = miscWidth + index * cardWidth,
                contentHeight = contentHeight
            )
        }
    }

    fun buildIn(
        panel: CustomPanelAPI,
        buildOptionsPanel: (CustomPanelAPI) -> Unit,
    ) {
        buttons.clear()
        tagScrollRegions.clear()
        scrollDirty = false
        observedTagSelectionVersion = SuggestedTagButton.suggestedTagSelectionVersion

        val miscWidth = min(MISC_WIDTH_MAX, max(MISC_WIDTH_MIN, panel.position.width * MISC_WIDTH_FRACTION))
        val columnsWidth = panel.position.width - miscWidth

        val miscPanel = panel.createCustomPanel(
            miscWidth,
            panel.position.height,
            DebugBorderPanelPlugin(CampaignContainerType.MISC)
        )
        panel.addComponent(miscPanel)
        miscPanel.position.inTL(0f, 0f)
        buildOptionsPanel(miscPanel)

        val columnsPanel = panel.createCustomPanel(
            columnsWidth,
            panel.position.height,
            DebugBorderPanelPlugin(CampaignContainerType.WEAPON_GROUPS)
        )
        panel.addComponent(columnsPanel)
        columnsPanel.position.rightOfTop(miscPanel, 0f)
        buildWeaponColumns(columnsPanel, panel.position.height, miscWidth)
    }

    override fun render(alpha: Float) {}
}
