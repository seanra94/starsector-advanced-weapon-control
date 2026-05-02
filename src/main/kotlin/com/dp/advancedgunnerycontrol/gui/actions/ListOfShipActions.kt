package com.dp.advancedgunnerycontrol.gui.actions

import com.dp.advancedgunnerycontrol.gui.GUIAttributes
import com.dp.advancedgunnerycontrol.settings.Settings

fun generateShipActions(attributes: GUIAttributes): List<GUIAction> {
    if(Settings.isAdvancedMode) return listOf(
        CycleLoadoutAction(attributes),
        NextShipAction(attributes),
        CopyLoadoutAction(attributes),
        ApplySuggestedModeAction(attributes),
        ResetAction(attributes),
        ReloadSettingsAction(attributes),
        CopyToSameVariantAction(attributes),
        GoToSuggestedTagsAction(attributes),
        ToggleSimpleAdvancedAction(attributes)
    )
    return listOf(
        NextShipAction(attributes),
        ToggleSimpleAdvancedAction(attributes)
    )
}
