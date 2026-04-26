package com.dp.advancedgunnerycontrol.gui.suggesttaggui

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI

abstract class WeaponFilter {
    abstract fun matches(weaponSpec: WeaponSpecAPI): Boolean
    abstract fun type(): FilterType
    abstract fun name(): String

    enum class FilterType{SIZE, WEAPON_TYPE, FIRE_FORM, RANGE}

    fun matches(weapon: String): Boolean{
        return matches(Global.getSettings().getWeaponSpec(weapon))
    }

    companion object{
        object ballisticsFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.type == WeaponAPI.WeaponType.BALLISTIC
            override fun type(): FilterType = FilterType.WEAPON_TYPE
            override fun name(): String = "Ballistics"

        }
        object energyFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.type == WeaponAPI.WeaponType.ENERGY
            override fun type(): FilterType = FilterType.WEAPON_TYPE
            override fun name(): String = "Energy"

        }
        object missileFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.type == WeaponAPI.WeaponType.MISSILE
            override fun type(): FilterType = FilterType.WEAPON_TYPE
            override fun name(): String = "Missiles"

        }
        object smallFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.size == WeaponAPI.WeaponSize.SMALL
            override fun type(): FilterType = FilterType.SIZE
            override fun name(): String = "Small"
        }
        object mediumFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.size == WeaponAPI.WeaponSize.MEDIUM
            override fun type(): FilterType = FilterType.SIZE
            override fun name(): String = "Medium"
        }
        object largeFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.size == WeaponAPI.WeaponSize.LARGE
            override fun type(): FilterType = FilterType.SIZE
            override fun name(): String = "Large"
        }
        object beamFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.isBeam
            override fun type(): FilterType = FilterType.FIRE_FORM
            override fun name(): String = "Beam"
        }
        object projectileFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = !weaponSpec.isBeam
            override fun type(): FilterType = FilterType.FIRE_FORM
            override fun name(): String = "Projectile"
        }
        object range0To500Filter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.maxRange in 0f..500f
            override fun type(): FilterType = FilterType.RANGE
            override fun name(): String = "Range [0, 500]"
        }
        object range500To1000Filter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.maxRange in 500f..1000f
            override fun type(): FilterType = FilterType.RANGE
            override fun name(): String = "Range [500, 1000]"
        }
        object range1000To1500Filter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.maxRange in 1000f..1500f
            override fun type(): FilterType = FilterType.RANGE
            override fun name(): String = "Range [1000, 1500]"
        }
        object range1500PlusFilter : WeaponFilter() {
            override fun matches(weaponSpec: WeaponSpecAPI): Boolean = weaponSpec.maxRange >= 1500f
            override fun type(): FilterType = FilterType.RANGE
            override fun name(): String = "Range [1500+]"
        }
        public val allFilters = listOf(
            ballisticsFilter,
            energyFilter,
            missileFilter,
            smallFilter,
            mediumFilter,
            largeFilter,
            beamFilter,
            projectileFilter,
            range0To500Filter,
            range500To1000Filter,
            range1000To1500Filter,
            range1500PlusFilter
        )
    }
}
