package com.dp.advancedgunnerycontrol.gui

import java.awt.Color
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.lang.reflect.Method
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

enum class CampaignContainerType(val outlineColor: Color) {
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
    private val log = Global.getLogger(CampaignGuiStyle::class.java)

    data class CheckboxColors(
        val base: Color,
        val bg: Color,
        val bright: Color,
    )
    val TOOLTIP_TEXT_COLOR: Color = Color(245, 230, 150)
    val INACTIVE_ROW_BACKGROUND_COLOR: Color = Color(115, 115, 115, 225)
    val DISABLED_TAG_BACKGROUND_COLOR: Color = Color(28, 28, 28, 235)
    val DISABLED_TAG_DARK_COLOR: Color = Color(18, 18, 18, 235)
    val DISABLED_TAG_BRIGHT_COLOR: Color = Color(245, 95, 85, 240)
    val DISABLED_TAG_TEXT_COLOR: Color = Color(120, 120, 120)
    val DISABLED_TAG_BORDER_COLOR: Color = Color(95, 95, 95)
    val UNAVAILABLE_TAG_BACKGROUND_COLOR: Color = Color(245, 95, 85, 240)
    val UNAVAILABLE_TAG_DARK_COLOR: Color = Color(205, 70, 65)
    val UNAVAILABLE_TAG_BRIGHT_COLOR: Color = Color(255, 155, 145)
    val UNAVAILABLE_TAG_TEXT_COLOR: Color = Color(220, 220, 220)
    val ACTIVE_GREEN_BACKGROUND_COLOR: Color = Color(86, 145, 92)
    val ACTIVE_GREEN_DARK_COLOR: Color = Color(48, 92, 54)
    val ACTIVE_GREEN_BRIGHT_COLOR: Color = Color(140, 205, 145)
    val DEFAULT_HEADING_BACKGROUND_COLOR: Color = Color(40, 40, 40, 225)
    val ACTION_SAVE_BACKGROUND_COLOR: Color = Color(95, 55, 125, 225)
    val ACTION_SAVE_DARK_COLOR: Color = Color(62, 34, 82, 225)
    val ACTION_SAVE_BRIGHT_COLOR: Color = Color(150, 105, 190, 225)
    val ACTION_LOAD_BACKGROUND_COLOR: Color = Color(145, 125, 25, 225)
    val ACTION_LOAD_DARK_COLOR: Color = Color(95, 80, 14, 225)
    val ACTION_LOAD_BRIGHT_COLOR: Color = Color(205, 180, 70, 225)
    val NEUTRAL_BUTTON_IDLE_COLOR: Color = Color(70, 70, 70, 225)
    val NEUTRAL_BUTTON_HOVER_COLOR: Color = Color(180, 180, 180, 225)
    val SHARED_TAG_MODE_UNSELECTED_IDLE_COLOR: Color = Color(0, 0, 0, 225)
    val SHARED_TAG_MODE_UNSELECTED_HOVER_COLOR: Color = NEUTRAL_BUTTON_HOVER_COLOR
    val SHARED_TAG_MODE_SELECTED_IDLE_COLOR: Color = Color(0, 127, 170, 225)
    val SHARED_TAG_MODE_SELECTED_HOVER_COLOR: Color = Color(0, 127, 170, 225)
    val TOGGLE_UNSELECTED_IDLE_COLOR: Color = SHARED_TAG_MODE_UNSELECTED_IDLE_COLOR
    val TOGGLE_UNSELECTED_HOVER_COLOR: Color = SHARED_TAG_MODE_UNSELECTED_HOVER_COLOR
    val TOGGLE_SELECTED_IDLE_COLOR: Color = SHARED_TAG_MODE_SELECTED_IDLE_COLOR
    val TOGGLE_SELECTED_HOVER_COLOR: Color = SHARED_TAG_MODE_SELECTED_HOVER_COLOR

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
    const val HEADING_CHAR_WIDTH_ESTIMATE = 7.2f
    private const val TAG_MODE_BORDER_THICKNESS = -1f

    /**
     * Decompiled addAreaCheckbox(base, bg, bright) behavior:
     * - base controls the hover/glow color.
     * - bg controls checked fill and border.
     * - bright controls the built-in label text color, which AGC leaves blank here.
     *
     * The Starsector area-checkbox renderer draws a black base, checked fill, border,
     * and then an inset glow while hovered. Negative border thickness is intentional
     * here: it expands the black/glow rectangles to reduce the bright-ring/black-center
     * appearance that otherwise shows on blank-label tag and ship-mode buttons.
     */
    fun toggleableCheckboxColors(): CheckboxColors {
        return CheckboxColors(
            base = SHARED_TAG_MODE_UNSELECTED_HOVER_COLOR,
            bg = SHARED_TAG_MODE_SELECTED_IDLE_COLOR,
            bright = SHARED_TAG_MODE_SELECTED_HOVER_COLOR
        )
    }

    private data class ButtonOverrideMethods(
        val setGlowOverride: Method,
        val setBorderOverride: Method,
        val setBorderThickness: Method,
    )

    private val buttonOverrideMethodsByClass = mutableMapOf<Class<*>, ButtonOverrideMethods?>()

    private fun buttonOverrideMethods(button: ButtonAPI): ButtonOverrideMethods? {
        return buttonOverrideMethodsByClass.getOrPut(button.javaClass) {
            try {
                ButtonOverrideMethods(
                    setGlowOverride = button.javaClass.getMethod("setGlowOverride", Color::class.java),
                    setBorderOverride = button.javaClass.getMethod("setBorderOverride", Color::class.java),
                    setBorderThickness = button.javaClass.getMethod("setBorderThickness", java.lang.Float.TYPE),
                )
            } catch (ex: Throwable) {
                log.warn("AGC could not cache area-checkbox color override methods for ${button.javaClass.name}", ex)
                null
            }
        }
    }

    private fun invokeColorOverride(button: ButtonAPI, method: Method, color: Color) {
        try {
            method.invoke(button, color)
        } catch (ex: Throwable) {
            log.warn("AGC failed to apply area-checkbox color override ${method.name}", ex)
        }
    }

    private fun invokeFloatOverride(button: ButtonAPI, method: Method, value: Float) {
        try {
            method.invoke(button, value)
        } catch (ex: Throwable) {
            log.warn("AGC failed to apply area-checkbox float override ${method.name}", ex)
        }
    }

    fun applyToggleableCheckboxVisualState(button: ButtonAPI) {
        val methods = buttonOverrideMethods(button) ?: return
        val glowColor = if (button.isChecked) {
            SHARED_TAG_MODE_SELECTED_HOVER_COLOR
        } else {
            SHARED_TAG_MODE_UNSELECTED_HOVER_COLOR
        }
        val borderColor = if (button.isChecked) {
            SHARED_TAG_MODE_SELECTED_IDLE_COLOR
        } else {
            SHARED_TAG_MODE_UNSELECTED_IDLE_COLOR
        }
        invokeColorOverride(button, methods.setGlowOverride, glowColor)
        invokeColorOverride(button, methods.setBorderOverride, borderColor)
        invokeFloatOverride(button, methods.setBorderThickness, TAG_MODE_BORDER_THICKNESS)
    }

    fun applyUnavailableCheckboxVisualState(button: ButtonAPI) {
        val methods = buttonOverrideMethods(button) ?: return
        invokeColorOverride(button, methods.setGlowOverride, DISABLED_TAG_BRIGHT_COLOR)
        invokeColorOverride(button, methods.setBorderOverride, DISABLED_TAG_BORDER_COLOR)
        invokeFloatOverride(button, methods.setBorderThickness, 1f)
    }
}

fun TooltipMakerAPI.applyAgcDefaultTextStyle() {
    // Intentionally no-op: preserve Starsector's native paragraph font/style.
}

fun TooltipMakerAPI.addAgcText(
    text: String,
    pad: Float = 0f,
    textColor: Color? = null,
): LabelAPI {
    applyAgcDefaultTextStyle()
    return if (textColor == null) {
        addPara(text, pad)
    } else {
        addPara(text, textColor, pad)
    }
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

data class WrappedLabelLayout(
    val wrappedText: String,
    val lineCount: Int,
    val rowHeight: Float,
)

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

private fun wrapLongToken(token: String, maxCharsPerLine: Int): List<String> {
    if (token.length <= maxCharsPerLine) return listOf(token)
    if (maxCharsPerLine <= 1) return token.map { it.toString() }
    val chunks = mutableListOf<String>()
    var index = 0
    while (index < token.length) {
        val end = min(token.length, index + maxCharsPerLine)
        chunks.add(token.substring(index, end))
        index = end
    }
    return chunks
}

private fun wrapTextLineWordAware(line: String, maxCharsPerLine: Int): List<String> {
    if (line.isBlank()) return listOf("")
    if (!line.contains(" ")) return wrapLongToken(line, maxCharsPerLine)

    val wrapped = mutableListOf<String>()
    var current = ""
    line.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { word ->
        val segments = if (word.length > maxCharsPerLine) wrapLongToken(word, maxCharsPerLine) else listOf(word)
        segments.forEachIndexed { segmentIndex, segment ->
            if (current.isBlank()) {
                current = segment
            } else if ((current.length + 1 + segment.length) <= maxCharsPerLine) {
                current += " $segment"
            } else {
                wrapped.add(current)
                current = segment
            }
            if (segmentIndex < segments.lastIndex) {
                wrapped.add(current)
                current = ""
            }
        }
    }
    if (current.isNotBlank()) wrapped.add(current)
    return if (wrapped.isEmpty()) listOf("") else wrapped
}

fun computeWrappedLabelLayout(
    text: String,
    rowWidth: Float,
    minButtonHeight: Float = 18f,
    horizontalPadding: Float = 8f,
    verticalPadding: Float = 8f,
    approxCharWidthPx: Float = 6.8f,
    lineHeightPx: Float = 15f,
    maxLines: Int = 3,
): WrappedLabelLayout {
    val safeMaxLines = max(1, maxLines)
    val effectiveTextWidth = max(8f, rowWidth - horizontalPadding)
    val maxCharsPerLine = max(1, (effectiveTextWidth / max(1f, approxCharWidthPx)).toInt())

    val wrappedLines = mutableListOf<String>()
    text.split("\n").forEach { explicitLine ->
        wrappedLines.addAll(wrapTextLineWordAware(explicitLine, maxCharsPerLine))
    }
    if (wrappedLines.isEmpty()) wrappedLines.add("")

    val cappedLines = if (wrappedLines.size > safeMaxLines) {
        val visible = wrappedLines.take(safeMaxLines).toMutableList()
        val overflow = wrappedLines.drop(safeMaxLines - 1).joinToString(" ")
        visible[visible.lastIndex] = truncateLabelByLength(overflow, maxCharsPerLine)
        visible
    } else {
        wrappedLines
    }
    val lineCount = max(1, cappedLines.size)
    val rowHeight = max(minButtonHeight, verticalPadding + lineCount * lineHeightPx)
    return WrappedLabelLayout(
        wrappedText = cappedLines.joinToString("\n"),
        lineCount = lineCount,
        rowHeight = rowHeight,
    )
}

fun TooltipMakerAPI.addTagLabelPara(text: String, pad: Float = 0f) {
    addAgcText(text, pad)
}

fun renderTagLabel(
    panel: com.fs.starfarer.api.ui.CustomPanelAPI,
    text: String,
    width: Float,
    height: Float,
    x: Float,
    y: Float,
    textColor: Color? = null,
) {
    val element = panel.createUIElement(width, height, false)
    element.addAgcText(text, 0f, textColor)
    panel.addUIElement(element).inTL(x, y)
}

fun renderCenteredTagLabel(
    panel: CustomPanelAPI,
    text: String,
    width: Float,
    height: Float,
    top: Float = CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
    centerRegionOffsetX: Float = 0f,
    centerRegionWidth: Float = width,
    textColor: Color? = null,
) {
    val estimatedLabelWidth = text.length * 6.8f
    val centerX = centerRegionOffsetX + centerRegionWidth / 2f
    val minLeft = min(0f, centerRegionOffsetX) + CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING
    val left = max(minLeft, centerX - estimatedLabelWidth / 2f)
    val renderWidth = (width - left - CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING).coerceAtLeast(16f)
    renderTagLabel(
        panel = panel,
        text = text,
        width = renderWidth,
        height = height,
        x = left,
        y = top,
        textColor = textColor
    )
}

fun addCustomContainerHeading(
    panel: CustomPanelAPI,
    title: String,
    top: Float = CampaignGuiStyle.PANEL_PADDING,
    fillColor: Color = CampaignGuiStyle.DEFAULT_HEADING_BACKGROUND_COLOR,
    textColor: Color? = null,
    headingHeight: Float = 20f,
) {
    val headerPanel = panel.createCustomPanel(
        panel.position.width - 2f * CampaignGuiStyle.PANEL_PADDING,
        headingHeight,
        CampaignPanelPlugin(CampaignContainerType.HEADER, fillColor = fillColor)
    )
    panel.addComponent(headerPanel)
    headerPanel.position.inTL(CampaignGuiStyle.PANEL_PADDING, top)

    val estimatedLabelWidth = title.length * CampaignGuiStyle.HEADING_CHAR_WIDTH_ESTIMATE
    val left = ((headerPanel.position.width - estimatedLabelWidth) / 2f).coerceAtLeast(CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING)
    val width = (headerPanel.position.width - left - CampaignGuiStyle.ITEM_TEXT_HORIZONTAL_PADDING).coerceAtLeast(16f)
    renderTagLabel(
        headerPanel,
        title,
        width,
        headingHeight - CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
        left,
        CampaignGuiStyle.ITEM_TEXT_TOP_PADDING,
        textColor = textColor
    )
}
