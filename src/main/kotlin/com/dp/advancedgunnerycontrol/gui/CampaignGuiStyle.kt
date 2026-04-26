package com.dp.advancedgunnerycontrol.gui

import java.awt.Color
import com.fs.starfarer.api.ui.TooltipMakerAPI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

enum class CampaignBorderMode {
    NONE,
    FULL,
    SIDES,
    SIDES_AND_BOTTOM,
}

enum class CampaignContainerType(val debugColor: Color) {
    MAIN(Color(0, 220, 120)),
    MISC(Color.GRAY),
    WEAPON_GROUPS(Color(70, 220, 220)),
    WEAPON_GROUP(Color.GRAY),
    WEAPON_LIST(Color(255, 220, 120)),
    WEAPON_ENTRY(Color(255, 190, 90)),
    TAG_LIST(Color(160, 255, 180)),
    SHIP_MODE(Color(90, 150, 255)),
    ITEM(Color.WHITE),
    HEADER(Color.GRAY),
    PICTURE(Color(190, 110, 255)),
    OPTIONS(Color(255, 110, 110)),
}

val campaignBorderModeByType = mapOf(
    CampaignContainerType.ITEM to CampaignBorderMode.FULL,
    CampaignContainerType.HEADER to CampaignBorderMode.FULL,
    CampaignContainerType.MISC to CampaignBorderMode.SIDES_AND_BOTTOM,
    CampaignContainerType.WEAPON_GROUP to CampaignBorderMode.SIDES,
).withDefault { CampaignBorderMode.NONE }

object CampaignGuiStyle {
    val TOOLTIP_TEXT_COLOR: Color = Color(245, 230, 150)
    val UNAVAILABLE_TAG_BACKGROUND_COLOR: Color = Color(255, 0, 0)
    val UNAVAILABLE_TAG_DARK_COLOR: Color = Color(170, 0, 0)
    val UNAVAILABLE_TAG_BRIGHT_COLOR: Color = Color(255, 90, 90)
    val UNAVAILABLE_TAG_TEXT_COLOR: Color = Color.WHITE
    val ACTIVE_GREEN_BACKGROUND_COLOR: Color = Color(70, 150, 75)
    val ACTIVE_GREEN_DARK_COLOR: Color = Color(25, 80, 35)
    val ACTIVE_GREEN_BRIGHT_COLOR: Color = Color(125, 225, 130)
    data class TagKeywordColor(val keyword: String, val color: Color)
    data class TagTextSegment(val text: String, val color: Color?)
    val TAG_KEYWORD_COLORS: List<TagKeywordColor> = listOf(
        TagKeywordColor("shielding", Color(95, 160, 255)),
        TagKeywordColor("shielded", Color(95, 160, 255)),
        TagKeywordColor("shields", Color(95, 160, 255)),
        TagKeywordColor("shield", Color(95, 160, 255)),
        TagKeywordColor("phasing", Color(190, 120, 255)),
        TagKeywordColor("phased", Color(190, 120, 255)),
        TagKeywordColor("phase", Color(190, 120, 255)),
        TagKeywordColor("fighters", Color(90, 220, 110)),
        TagKeywordColor("fighter", Color(90, 220, 110)),
        TagKeywordColor("missiles", Color(90, 220, 110)),
        TagKeywordColor("missile", Color(90, 220, 110)),
        TagKeywordColor("pd", Color(90, 220, 110)),
        TagKeywordColor("armoured", Color(235, 90, 80)),
        TagKeywordColor("armored", Color(235, 90, 80)),
        TagKeywordColor("amoured", Color(235, 90, 80)),
        TagKeywordColor("armour", Color(235, 90, 80)),
        TagKeywordColor("armor", Color(235, 90, 80)),
        TagKeywordColor("amour", Color(235, 90, 80)),
        TagKeywordColor("ammunition", Color(245, 220, 80)),
        TagKeywordColor("opportunist", Color(245, 220, 80)),
        TagKeywordColor("ammo", Color(245, 220, 80)),
        TagKeywordColor("holding", Color(255, 135, 205)),
        TagKeywordColor("held", Color(255, 135, 205)),
        TagKeywordColor("hold", Color(255, 135, 205)),
        TagKeywordColor("forcing", Color(255, 155, 65)),
        TagKeywordColor("forced", Color(255, 155, 65)),
        TagKeywordColor("force", Color(255, 155, 65)),
    ).sortedByDescending { it.keyword.length }

    const val MAIN_PADDING = 0f
    const val PANEL_PADDING = 4f
    const val SECTION_GAP = 0f
    const val GROUP_GAP = 0f
    const val GROUP_MIN_WIDTH = 185f
    const val TAG_ITEM_HEIGHT = 20f
    const val TAG_ITEM_HGAP = 0f
    const val TAG_ITEM_VGAP = 2f
    const val TAG_ITEM_MIN_WIDTH = 96f
    const val SHIP_MODE_ITEM_HEIGHT = 20f
    const val SHIP_MODE_ITEM_HGAP = 0f
    const val SHIP_MODE_ITEM_VGAP = 2f
    const val SHIP_MODE_ITEM_MIN_WIDTH = 78f
    const val ITEM_TEXT_HORIZONTAL_PADDING = 4f
    const val ITEM_TEXT_TOP_PADDING = 2f
    const val ITEM_HIGHLIGHT_X_OFFSET = -3f
}

data class WrapGridMetrics(
    val columns: Int,
    val rows: Int,
    val itemWidth: Float,
    val itemHeight: Float,
    val horizontalGap: Float,
    val verticalGap: Float,
) {
    fun xFor(index: Int): Float = (index % columns) * (itemWidth + horizontalGap)
    fun yFor(index: Int): Float = (index / columns) * (itemHeight + verticalGap)
}

fun computeWrapGridMetrics(
    itemCount: Int,
    availableWidth: Float,
    availableHeight: Float,
    minItemWidth: Float,
    itemHeight: Float,
    horizontalGap: Float,
    verticalGap: Float,
    maxColumns: Int = itemCount,
): WrapGridMetrics {
    if (itemCount <= 0) {
        return WrapGridMetrics(1, 0, availableWidth, itemHeight, horizontalGap, verticalGap)
    }

    val widthLimitedColumns = max(
        1,
        floor((availableWidth + horizontalGap) / (minItemWidth + horizontalGap)).toInt()
    )
    val upperColumns = min(maxColumns, max(1, widthLimitedColumns))
    var chosen = WrapGridMetrics(
        columns = 1,
        rows = itemCount,
        itemWidth = availableWidth,
        itemHeight = itemHeight,
        horizontalGap = horizontalGap,
        verticalGap = verticalGap,
    )

    for (columns in upperColumns downTo 1) {
        val itemWidth = (availableWidth - horizontalGap * (columns - 1)) / columns
        if (itemWidth <= 0f) continue
        val rows = ceil(itemCount.toFloat() / columns.toFloat()).toInt()
        val requiredHeight = rows * itemHeight + max(0, rows - 1) * verticalGap
        chosen = WrapGridMetrics(columns, rows, itemWidth, itemHeight, horizontalGap, verticalGap)
        if (requiredHeight <= availableHeight) {
            return chosen
        }
    }

    return chosen
}

fun truncateLabel(text: String, width: Float, reservedWidth: Float = 20f): String {
    val estimatedChars = max(4, ((width - reservedWidth) / 5.4f).toInt())
    if (text.length <= estimatedChars) {
        return text
    }
    return text.take(max(1, estimatedChars - 3)) + "..."
}

fun truncateLabelByLength(text: String, maxVisibleLength: Int): String {
    if (maxVisibleLength <= 3) return text.take(max(0, maxVisibleLength))
    if (text.length <= maxVisibleLength) return text
    return text.take(maxVisibleLength - 3) + "..."
}

fun TooltipMakerAPI.addTagLabelPara(text: String, pad: Float = 0f) {
    addPara(text, pad)
}

fun splitTagLabelSegments(text: String): List<CampaignGuiStyle.TagTextSegment> {
    val segments = mutableListOf<CampaignGuiStyle.TagTextSegment>()
    var index = 0
    while (index < text.length) {
        val match = CampaignGuiStyle.TAG_KEYWORD_COLORS.firstOrNull {
            text.regionMatches(index, it.keyword, 0, it.keyword.length, ignoreCase = true)
        }
        if (match == null) {
            val start = index
            while (index < text.length && CampaignGuiStyle.TAG_KEYWORD_COLORS.none {
                    text.regionMatches(index, it.keyword, 0, it.keyword.length, ignoreCase = true)
                }) {
                index++
            }
            segments.add(CampaignGuiStyle.TagTextSegment(text.substring(start, index), null))
        } else {
            segments.add(
                CampaignGuiStyle.TagTextSegment(
                    text.substring(index, index + match.keyword.length),
                    match.color
                )
            )
            index += match.keyword.length
        }
    }
    return segments
}

fun TooltipMakerAPI.addColoredTagLabel(text: String, pad: Float = 0f) {
    val label = addPara(text, pad)
    val highlighted = splitTagLabelSegments(text)
        .filter { it.color != null }
    if (highlighted.isNotEmpty()) {
        label.setHighlight(*highlighted.map { it.text }.toTypedArray())
        label.setHighlightColors(*highlighted.map { it.color }.toTypedArray())
    }
}

fun renderColoredTagLabel(
    panel: com.fs.starfarer.api.ui.CustomPanelAPI,
    text: String,
    width: Float,
    height: Float,
    x: Float,
    y: Float,
    textColor: Color? = null,
    enableKeywordColors: Boolean = true,
) {
    val element = panel.createUIElement(width, height, false)
    if (textColor == null) {
        element.addPara(text, 0f)
    } else {
        element.addPara(text, textColor, 0f)
    }
    panel.addUIElement(element).inTL(x, y)
}
