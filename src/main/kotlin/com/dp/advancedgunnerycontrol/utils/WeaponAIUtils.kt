package com.dp.advancedgunnerycontrol.utils

import com.dp.advancedgunnerycontrol.settings.Settings
import com.dp.advancedgunnerycontrol.typesandvalues.*
import com.dp.advancedgunnerycontrol.weaponais.TagBasedAI
import com.dp.advancedgunnerycontrol.weaponais.times_
import com.dp.advancedgunnerycontrol.weaponais.vectorFromAngleDeg
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.AutofireAIPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import org.lazywizard.lazylib.ext.minus
import org.lwjgl.util.vector.Vector2f
import kotlin.math.abs

fun applyTagsToWeaponGroup(ship: ShipAPI, groupIndex: Int, tags: List<String>) : Boolean {
    val weaponGroup = ship.weaponGroupsCopy[groupIndex]
    val plugins = weaponGroup.aiPlugins
    if(tags.isEmpty()){
        for (i in 0 until plugins.size) {
            if (plugins[i] is TagBasedAI){
                plugins[i] = (plugins[i] as? TagBasedAI)?.baseAI ?: plugins[i]
            }
        }
    }
    for (i in 0 until plugins.size) {
        if (plugins[i] !is TagBasedAI) {
            plugins[i] = TagBasedAI(plugins[i])
        }
        (plugins[i] as? TagBasedAI)?.tags = createTags(tags, plugins[i].weapon).toMutableList()
    }
    return plugins.all { (it as? TagBasedAI)?.tags?.all { t -> t.isValid() } ?: true }
}

fun applyTagsToWeapon(weapon: WeaponAPI, tags: List<String>){
    val weaponGroup = weapon.ship.getWeaponGroupFor(weapon)
    val plugin = weaponGroup.getAutofirePlugin(weapon)

    if(plugin !is TagBasedAI){
        setAutofirePlugin(weapon, TagBasedAI(plugin, createTags(tags, weapon).toMutableList()))
    }else{
        val combinedTags = plugin.tags.toMutableSet()
        combinedTags.addAll(createTags(tags, weapon))
        plugin.tags = combinedTags.toMutableList()
    }

}

fun setAutofirePlugin(weapon: WeaponAPI, plugin: AutofireAIPlugin){
    val weaponGroup = weapon.ship.getWeaponGroupFor(weapon)
    val index = weaponGroup.aiPlugins.indexOf(weaponGroup.getAutofirePlugin(weapon))
    weaponGroup.aiPlugins[index] = plugin
}

fun reloadAllShips(storageIndex: Int){
    reloadShips(storageIndex, Global.getCombatEngine()?.ships)
}

fun reloadShips(storageIndex: Int, ships: List<ShipAPI?>?) {
    ships?.filter { it?.owner == 0 }?.filterNotNull().let{
        it?.forEach { ship->
            var atLeastOneTagExist = false
            for(i in 0 until ship.weaponGroupsCopy.size){
                val tags = loadTags(ship, i, storageIndex)
                atLeastOneTagExist = tags.isNotEmpty() || atLeastOneTagExist
                applyTagsToWeaponGroup(ship, i, tags)
            }
            val shipModes = loadShipModes(ship, storageIndex)
            assignShipMode(shipModes, ship)
        }
    }

}

fun persistTemporaryShipData(storageIndex: Int, ships: List<ShipAPI?>?){
    ships?.filter { it?.owner == 0 }?.filterNotNull().let {
        it?.forEach { ship ->
            for(i in 0 until ship.weaponGroupsCopy.size){
                val tags = loadTags(ship, i, storageIndex)
                persistTags(ship.id, i, storageIndex, tags)
            }
            val modes = loadShipModes(ship, storageIndex)
            persistShipModes(ship.id, storageIndex, modes)
        }
    }
}

fun loadTags(ship: ShipAPI, index: Int, storageIndex: Int) : List<String>{
    if(Settings.enableCombatChangePersistance() || !doesShipHaveLocalTags(ship, storageIndex)){
        return ship.fleetMemberId?.let { loadPersistentTags(it, index, storageIndex) } ?: emptyList()
    }
    return loadTagsFromShip(ship, index, storageIndex)
}

fun loadAllTags(ship: FleetMemberAPI) : List<String>{
    val tags = mutableSetOf<String>()
    for(si in 0 until Settings.maxLoadouts()){
        for(i in 0 until ship.variant.weaponGroups.size){
            tags.addAll(loadPersistentTags(ship.id, i, si))
        }
    }
    return tags.toList()
}

/**
 * @return approximate angular distance of target from current weapon facing in rad
 * note: approximation works well for small values and is off by a factor of PI/2 for 180°
 * @param entity: Relative coordinates (velocity-compensated)
 */
fun angularDistanceFromWeapon(entity: Vector2f, weapon: WeaponAPI): Float {
    val weaponDirection = vectorFromAngleDeg(weapon.currAngle)
    val distance = entity - weapon.location
    val entityDirection = distance times_ (1f / distance.length())
    return (weaponDirection - entityDirection).length()
}
fun linearDistanceFromWeapon(entity: Vector2f, weapon: WeaponAPI): Float {
    return (weapon.location - entity).length()
}
/**
 * @param entity: In relative coordinates
 * @param collRadius: Include any tolerances in here
 * @param aimPoint: Point the weapon is aiming at, deduced from current weapon facing if not provided
 */
fun determineIfShotWillHit(entity: Vector2f, collRadius: Float, weapon: WeaponAPI, aimPoint: Vector2f? = null) : Boolean{
    val apd = aimPoint?.let { angularDistanceFromWeapon(it, weapon) } ?: 0f
    val lateralOffset = abs(angularDistanceFromWeapon(entity, weapon) - apd) * linearDistanceFromWeapon(entity, weapon)
    return lateralOffset < collRadius
}

fun saveTags(ship: ShipAPI, groupIndex: Int, loadoutIndex: Int, tags: List<String>){
    if(Settings.enableCombatChangePersistance()){
        persistTags(ship.fleetMemberId?: "", groupIndex, loadoutIndex, tags)
    }
    saveTagsInShip(ship, groupIndex, tags, loadoutIndex)
}

fun persistTags(shipId: String, groupIndex: Int, loadoutIndex: Int, tags: List<String>){
    if(!Settings.tagStorage[loadoutIndex].modesByShip.containsKey(shipId)){
        Settings.tagStorage[loadoutIndex].modesByShip[shipId] = mutableMapOf()
    }
    Settings.tagStorage[loadoutIndex].modesByShip[shipId]?.set(groupIndex, tags.toSet().toList())
}

fun saveTagsInShip(ship: ShipAPI, groupIndex: Int, tags: List<String>, storageIndex: Int){
    if(!ship.customData.containsKey(Values.CUSTOM_SHIP_DATA_WEAPONS_TAG_KEY)){
        ship.setCustomData(Values.CUSTOM_SHIP_DATA_WEAPONS_TAG_KEY, InShipTagStorage())
    }
    (ship.customData[Values.CUSTOM_SHIP_DATA_WEAPONS_TAG_KEY] as? InShipTagStorage)?.tagsByIndex?.get(storageIndex)?.set(groupIndex, tags.toSet().toList())
}

fun loadPersistentTags(shipId: String, groupIndex: Int, loadoutIndex: Int) : List<String>{
    return Settings.tagStorage[loadoutIndex].modesByShip[shipId]?.get(groupIndex) ?: emptyList()
}

fun getWeaponGroupIndex(weapon: WeaponAPI) : Int {
    return weapon.ship.weaponGroupsCopy.indexOf(weapon.ship.getWeaponGroupFor(weapon))
}

fun loadTagsFromShip(ship: ShipAPI, groupIndex: Int, storageIndex: Int) : List<String>{
    return (ship.customData[Values.CUSTOM_SHIP_DATA_WEAPONS_TAG_KEY] as? InShipTagStorage)?.tagsByIndex?.get(storageIndex)?.get(groupIndex) ?: emptyList()
}

fun doesShipHaveLocalTags(ship: ShipAPI, storageIndex: Int) : Boolean{
    return ship.customData.containsKey(Values.CUSTOM_SHIP_DATA_WEAPONS_TAG_KEY)
            && (ship.customData[Values.CUSTOM_SHIP_DATA_WEAPONS_TAG_KEY] as? InShipTagStorage)?.tagsByIndex?.containsKey(storageIndex) ?: false
}

fun doesShipHaveLocalShipModes(ship: ShipAPI, storageIndex: Int) : Boolean{
    return ship.customData.containsKey(Values.CUSTOM_SHIP_DATA_SHIP_MODES_KEY)
            && (ship.customData[Values.CUSTOM_SHIP_DATA_SHIP_MODES_KEY] as? InShipShipModeStorage)?.modes?.containsKey(storageIndex) ?: false
}