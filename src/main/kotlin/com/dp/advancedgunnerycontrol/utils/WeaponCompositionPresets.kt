package com.dp.advancedgunnerycontrol.utils

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.Values
import com.dp.advancedgunnerycontrol.typesandvalues.canonicalizeWeaponTagNames
import com.dp.advancedgunnerycontrol.typesandvalues.isIncompatibleWithExistingTags
import com.dp.advancedgunnerycontrol.typesandvalues.shouldTagBeDisabled
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.json.JSONArray
import org.json.JSONObject
import org.lazywizard.lazylib.JSONUtils

private const val PRESET_SCHEMA_VERSION = 1

enum class WeaponCompositionPresetSaveStatus {
    SAVED,
    NO_WEAPON_GROUP_KEY,
    FAILED
}

enum class WeaponCompositionPresetLoadStatus {
    LOADED,
    NO_WEAPON_GROUP_KEY,
    NO_PRESET_FOUND,
    FAILED
}

data class WeaponCompositionPresetSaveResult(
    val status: WeaponCompositionPresetSaveStatus,
    val weaponKey: String? = null,
    val tags: List<String> = emptyList(),
    val error: String? = null
)

data class WeaponCompositionPresetLoadResult(
    val status: WeaponCompositionPresetLoadStatus,
    val weaponKey: String? = null,
    val tags: List<String> = emptyList(),
    val error: String? = null
)

private data class WeaponCompositionPresetFileData(
    val schemaVersion: Int = PRESET_SCHEMA_VERSION,
    val presetsByLoadout: MutableMap<Int, MutableMap<String, List<String>>> = mutableMapOf()
)

fun getWeaponCompositionPresetKey(member: FleetMemberAPI, groupIndex: Int): String? {
    val group = member.variant?.weaponGroups?.getOrNull(groupIndex) ?: return null
    val weaponIds = group.slots
        .mapNotNull { member.variant?.getWeaponId(it)?.trim() }
        .filter { it.isNotEmpty() }
        .toSortedSet()
    if (weaponIds.isEmpty()) return null
    return weaponIds.joinToString(separator = "|")
}

fun saveExternalWeaponCompositionPreset(
    member: FleetMemberAPI,
    groupIndex: Int,
    loadoutIndex: Int,
    file: String = Values.WEAPON_COMP_TAG_PRESETS_JSON_FILE_NAME
): WeaponCompositionPresetSaveResult {
    val key = getWeaponCompositionPresetKey(member, groupIndex)
        ?: return WeaponCompositionPresetSaveResult(WeaponCompositionPresetSaveStatus.NO_WEAPON_GROUP_KEY)
    return try {
        val shipId = generateUniversalFleetMemberId(member)
        val selectedTags = loadPersistentTags(shipId, member, groupIndex, loadoutIndex)
        val sanitized = sanitizeTagsForWeaponGroup(member, groupIndex, selectedTags)

        val fileData = loadPresetData(file)
        val byLoadout = fileData.presetsByLoadout.getOrPut(loadoutIndex) { mutableMapOf() }
        byLoadout[key] = sanitized
        savePresetData(file, fileData)

        WeaponCompositionPresetSaveResult(
            status = WeaponCompositionPresetSaveStatus.SAVED,
            weaponKey = key,
            tags = sanitized
        )
    } catch (ex: Exception) {
        logPresetWarn("Failed saving external weapon preset for key=$key, loadout=$loadoutIndex", ex)
        WeaponCompositionPresetSaveResult(
            status = WeaponCompositionPresetSaveStatus.FAILED,
            weaponKey = key,
            error = ex.message
        )
    }
}

fun loadExternalWeaponCompositionPreset(
    member: FleetMemberAPI,
    groupIndex: Int,
    loadoutIndex: Int,
    file: String = Values.WEAPON_COMP_TAG_PRESETS_JSON_FILE_NAME
): WeaponCompositionPresetLoadResult {
    val key = getWeaponCompositionPresetKey(member, groupIndex)
        ?: return WeaponCompositionPresetLoadResult(WeaponCompositionPresetLoadStatus.NO_WEAPON_GROUP_KEY)
    return try {
        val fileData = loadPresetData(file)
        val savedTags = fileData.presetsByLoadout[loadoutIndex]?.get(key)
            ?: return WeaponCompositionPresetLoadResult(
                status = WeaponCompositionPresetLoadStatus.NO_PRESET_FOUND,
                weaponKey = key
            )

        val sanitized = sanitizeTagsForWeaponGroup(member, groupIndex, savedTags)
        val shipId = generateUniversalFleetMemberId(member)
        persistTags(shipId, member, groupIndex, loadoutIndex, sanitized)

        WeaponCompositionPresetLoadResult(
            status = WeaponCompositionPresetLoadStatus.LOADED,
            weaponKey = key,
            tags = sanitized
        )
    } catch (ex: Exception) {
        logPresetWarn("Failed loading external weapon preset for key=$key, loadout=$loadoutIndex", ex)
        WeaponCompositionPresetLoadResult(
            status = WeaponCompositionPresetLoadStatus.FAILED,
            weaponKey = key,
            error = ex.message
        )
    }
}

private fun sanitizeTagsForWeaponGroup(member: FleetMemberAPI, groupIndex: Int, tags: List<String>): List<String> {
    val allVisibleTags = Settings.getCurrentWeaponTagList()
    val sanitized = canonicalizeWeaponTagNames(tags)
        .filter { allVisibleTags.contains(it) }
        .toMutableList()
    var changed = true
    while (changed) {
        changed = false
        sanitized.toList().forEach { persistedTag ->
            val otherTags = sanitized.toMutableList().apply { remove(persistedTag) }
            if (isIncompatibleWithExistingTags(persistedTag, otherTags) || shouldTagBeDisabled(groupIndex, member, persistedTag)) {
                sanitized.remove(persistedTag)
                changed = true
            }
        }
    }
    return sanitized
}

private fun loadPresetData(file: String): WeaponCompositionPresetFileData {
    return try {
        val root = JSONUtils.loadCommonJSON(file)
        val schemaVersion = root.optInt("schemaVersion", PRESET_SCHEMA_VERSION)
        val loadoutsObject = root.optJSONObject("loadouts")
        val presetsByLoadout = mutableMapOf<Int, MutableMap<String, List<String>>>()
        if (loadoutsObject != null) {
            loadoutsObject.keys().forEach { loadoutKeyAny ->
                val loadoutKey = loadoutKeyAny as? String ?: return@forEach
                val loadoutIndex = loadoutKey.toIntOrNull() ?: return@forEach
                val perLoadoutObject = loadoutsObject.optJSONObject(loadoutKey) ?: return@forEach
                val perLoadout = mutableMapOf<String, List<String>>()
                perLoadoutObject.keys().forEach { weaponKeyAny ->
                    val weaponKey = weaponKeyAny as? String ?: return@forEach
                    val tagsJson = perLoadoutObject.optJSONArray(weaponKey)
                    if (tagsJson == null) {
                        logPresetWarn("Ignoring malformed preset entry for key=$weaponKey in loadout=$loadoutIndex (not an array).")
                        return@forEach
                    }
                    perLoadout[weaponKey] = jsonArrayToStringList(tagsJson)
                }
                if (perLoadout.isNotEmpty()) {
                    presetsByLoadout[loadoutIndex] = perLoadout
                }
            }
        }
        WeaponCompositionPresetFileData(schemaVersion = schemaVersion, presetsByLoadout = presetsByLoadout)
    } catch (ex: Exception) {
        logPresetWarn("Failed reading preset file '$file'. Falling back to empty presets.", ex)
        WeaponCompositionPresetFileData()
    }
}

private fun savePresetData(file: String, fileData: WeaponCompositionPresetFileData) {
    val root = JSONUtils.loadCommonJSON(file)
    clearJsonObject(root)
    root.put("schemaVersion", fileData.schemaVersion)
    val loadoutsObject = JSONObject()
    fileData.presetsByLoadout.forEach { (loadoutIndex, presets) ->
        val loadoutObject = JSONObject()
        presets.forEach { (weaponKey, tags) ->
            loadoutObject.put(weaponKey, JSONArray(canonicalizeWeaponTagNames(tags)))
        }
        loadoutsObject.put(loadoutIndex.toString(), loadoutObject)
    }
    root.put("loadouts", loadoutsObject)
    root.save()
}

private fun clearJsonObject(jsonObject: JSONObject) {
    val keys = mutableListOf<String>()
    jsonObject.keys().forEach { key ->
        (key as? String)?.let { keys.add(it) }
    }
    keys.forEach { jsonObject.remove(it) }
}

private fun jsonArrayToStringList(jsonArray: JSONArray): List<String> {
    val output = mutableListOf<String>()
    for (i in 0 until jsonArray.length()) {
        val value = jsonArray.opt(i) as? String ?: continue
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) output.add(trimmed)
    }
    return output
}

private fun logPresetWarn(message: String, throwable: Throwable? = null) {
    val logger = Global.getLogger(WeaponCompositionPresetFileData::class.java)
    if (throwable == null) logger.warn(message) else logger.warn(message, throwable)
}
