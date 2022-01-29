import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object Variables {
    // Note: On Linux, if you installed Starsector into ~/something, you have to write /home/<user>/ instead of ~/
    val starsectorDirectory = "/home/jannes/software/starsector"
    val modVersion = "0.13.0"
    val jarFileName = "AdvancedGunneryControl.jar"

    val modId = "advanced_gunnery_control_dbeaa06e"
    val modName = "AdvancedGunneryControl"
    val author = "DesperatePeter"
    const val description = "A Starsector mod that adds more autofire modes for weapon groups. On the campaign map, press J to open a GUI. In combat, with NUMLOCK enabled, press the NUMPAD keys to cycle weapon modes."
    val gameVersion = "0.95.1a-RC6"
    val jars = arrayOf("jars/$jarFileName")
    val modPlugin = "com.dp.advancedgunnerycontrol.WeaponControlBasePlugin"
    val isUtilityMod = true
    val masterVersionFile = "https://raw.githubusercontent.com/DesperatePeter/starsector-advanced-weapon-control/master/$modId.version"
    val modThreadId = "21280"

    val modFolderName = modName.replace(" ", "-")

// Scroll down and change the "dependencies" part of mod_info.json, if needed
// LazyLib is needed to use Kotlin, as it provides the Kotlin Runtime
}
//////////////////////

// Note: On Linux, use "${Variables.starsectorDirectory}" as core directory
val starsectorCoreDirectory = Variables.starsectorDirectory
val starsectorModDirectory = "${Variables.starsectorDirectory}/mods"
val modInModsFolder = File("$starsectorModDirectory/${Variables.modFolderName}")

plugins {
    kotlin("jvm") version "1.3.60"
    java
}

version = Variables.modVersion

repositories {
    maven(url = uri("$projectDir/libs"))
    jcenter()
}

dependencies {
    implementation("org.junit.jupiter:junit-jupiter:5.7.0")
    implementation("junit:junit:4.13.1")
    val kotlinVersionInLazyLib = "1.4.21"

    implementation(fileTree("libs") { include("*.jar") })
    testImplementation(kotlin("test"))

    // Get kotlin sdk from LazyLib during runtime, only use it here during compile time
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersionInLazyLib")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersionInLazyLib")

    implementation(fileTree("$starsectorModDirectory/LazyLib/jars") { include("*.jar") })
    implementation(fileTree("$starsectorModDirectory/MagicLib/jars") { include("*.jar") })
    //compileOnly(fileTree("$starsectorModDirectory/Console Commands/jars") { include("*.jar") })

    // Starsector jars and dependencies
    implementation(fileTree(starsectorCoreDirectory) {
        include(
            "starfarer.api.jar",
            "starfarer.api-sources.jar",
            "starfarer_obf.jar",
            "fs.common_obf.jar",
            "json.jar",
            "xstream-1.4.10.jar",
            "log4j-1.2.9.jar",
            "lwjgl.jar",
            "lwjgl_util.jar"
        )
    })
}


tasks {
    named<Jar>("jar")
    {
        destinationDirectory.set(file("$rootDir/jars"))
        archiveFileName.set(Variables.jarFileName)
    }

    register("create-metadata-files") {
        val version = Variables.modVersion.split(".").let { javaslang.Tuple3(it[0], it[1], it[2]) }
        System.setProperty("line.separator", "\n") // Use LF instead of CRLF like a normal person

        File(projectDir, "mod_info.json")
            .writeText(
                """
                    # THIS FILE IS GENERATED BY build.gradle.kts. (Note that Starsector's json parser permits `#` for comments)
                    {
                        "id": "${Variables.modId}",
                        "name": "${Variables.modName}",
                        "author": "${Variables.author}",
                        "utility": "${Variables.isUtilityMod}",
                        "version": { "major":"${version._1}", "minor": "${version._2}", "patch": "${version._3}" },
                        "description": "${Variables.description}",
                        "gameVersion": "${Variables.gameVersion}",
                        "jars":[${Variables.jars.joinToString() { "\"$it\"" }}],
                        "modPlugin":"${Variables.modPlugin}",
                        "dependencies": [
                            {
                                "id": "lw_lazylib",
                                "name": "LazyLib",
                            },
                            {
                                "id" : "MagicLib",
                                "name" : "MagicLib"
                            }
                        ]
                    }
                """.trimIndent()
            )

        File(projectDir, "data/config/version/version_files.csv")
            .writeText(
                """
                    version file
                    ${Variables.modId}.version

                """.trimIndent()
            )

        File(projectDir, "${Variables.modId}.version")
            .writeText(
                """
                    # THIS FILE IS GENERATED BY build.gradle.kts.
                    {
                        "masterVersionFile":"${Variables.masterVersionFile}",
                        "modName":"${Variables.modName}",
                        "modThreadId":${Variables.modThreadId},
                        "modVersion":
                        {
                            "major":${version._1},
                            "minor":${version._2},
                            "patch":${version._3}
                        },
                        "directDownloadURL": "https://github.com/DesperatePeter/starsector-advanced-weapon-control/releases/download/${version._1}.${version._2}.${version._3}/AdvancedGunneryControl-${version._1}.${version._2}.${version._3}.zip"
                    }
                """.trimIndent()
            )


        File(projectDir, ".github/workflows/mod-folder-name.txt")
            .writeText(Variables.modFolderName)
    }

    register("write-settings-file") {
        System.setProperty("line.separator", "\n")
        File(projectDir, "Settings.editme")
            .writeText(
                """
                   | # By editing this file, you can modify the behaviour of this mod!
                   | # NOTE: If the mod fails to parse these settings, it will fall back to default settings
                   | # NOTE: For bool values, everything but true will be interpreted as false
                   | #       Check starsector.log (in the Starsector folder) for details (ctrl+f for advancedgunnerycontrol)
                   | {
                   |   #                                 #### CYCLE ORDER ####
                   |   # Reorder the entries in this list to change the order in which you cycle through fire modes in game.
                   |   # Delete/add modes as you see fit. Note: "Default" will always be the first mode.
                   |   # Allowed values: "PD", "PD (Flux>50%)", "PD (Ammo<90%)", "Fighters", "Missiles", "NoFighters", "BigShips", "SmallShips", "Mining", "Opportunist", "TargetShields", "AvoidShields", "NoPD"
                   |   # Example: "cycleOrder" : ["PD"] -> Will cycle between Default and PD Mode ( becomes ["Default", "PD"])
                   |   "cycleOrder" : ["PD", "PD (Flux>50%)", "Fighters", "Missiles", "NoFighters", "Opportunist", "TargetShields", "AvoidShields" ] # <---- EDIT HERE ----


                   |   #                                 #### CUSTOM AI ####
                   |   # If you set this to true, if weapons in weapon groups in Fighters/Missiles mode would normally target something else,
                   |   # they will try to acquire a fitting target using custom targeting AI.
                   |   # If you set this to false, they will use exclusively vanilla AI (base AI) simply not fire in that situation.
                   |   # Update: I made quite a lot of improvements to the customAI, so I feel like it's safe to use now.
                   |   # Beware though that enabling it will have a negative effect on game performance.
                   |   # Allowed values: true/false
                   |   ,"enableCustomAI" : true # <---- EDIT HERE ----
                   |   
                   |   # Enabling this will always use the customAI (for applicable modes, refer to mode table)
                   |   # Note that forcing & enabling custom AI should actually be beneficial for performance over just enabling it.
                   |   # Note that setting enableCustomAI to false and this to true is not a brilliant idea and will be overridden :P
                   |   ,"forceCustomAI" : false # <---- EDIT HERE ----


                   |   #                                 #### UI SETTINGS ####
                   |   # Switch this off if you want to reset fire modes every battle
                   |   , "enablePersistentFireModes" : true # <---- EDIT HERE ----
                   |   # Number of frames messages will be displayed before fading. -1 for infinite
                   |   , "messageDisplayDuration" : 180 # <---- EDIT HERE ----
                   |   # X/Y Position (from bottom left) where messages will be displayed (refpoint: top left corner of message)
                   |   # Note: I believe the game calculates everything in 2560 x 1440 and then scales it to your actual resolution 
                   |   , "messagePositionX" : 900 # <---- EDIT HERE ----
                   |   , "messagePositionY" : 150 # <---- EDIT HERE ----
                   |   # When on, all weapon groups will be displayed (same as infoHotkey) rather than just the cycled one.
                   |   , "alwaysShowFullInfo" : false # <---- EDIT HERE ----
                   |   # A key that can be represented by a single character that's not bound to anything in combat in the Starsector settings
                   |   , "saveLoadInfoHotkey" : "j" # <---- EDIT HERE ----
                   |   , "resetHotkey" : "/" # <---- EDIT HERE ----
                   |   , "loadAllShipsHotkey" : "*" # <---- EDIT HERE ----
                   |   , "suffixHotkey" : "-" # <---- EDIT HERE ----
                   |   , "cycleLoadoutHotkey" : "+" # <---- EDIT HERE ----
                   |   , "maxLoadouts" : 3 # <---- EDIT HERE ----
                   |   , "GUIHotkey" : "j" # <---- EDIT HERE ----
                   |   , "helpHotkey" : "?" # <---- EDIT HERE ----
                   |   , "loadoutNames" : [ "Normal", "Special", "AllDefault" ]

                   |   # If you disable this, you will have to use the J-Key to save/load weapon modes (for each ship)
                   |   # This can't be enabled when enablePersistentFireModes is off
                   |   , "enableAutoSaveLoad" : true # <---- EDIT HERE ----
                   |   # When enabled, fire modes where all weapons are invalid (e.g. PD mode for non-PD weapons) are skipped when cycling.
                   |   , "skipInvalidModes" : true # <---- EDIT HERE ----
                   |   # Press the "J"-Key while on the system/hyperspace map with this enabled
                   |   , "enableGUI" : true # <---- EDIT HERE ----


                   |   #                                 #### CUSTOM AI CONFIGURATION  ####
                   |   # NOTE: All the stuff here is mainly here to facilitate testing. But feel free to play around with the settings here!

                   |   # Define the number of calculation steps the AI should perform per time frame to compute firing solutions.
                   |   # higher values -> slightly better AI but worse performance (0 means just aim at current target position).
                   |   # performance cost increases linearly, firing solution accuracy approx. logarithmically (recommended: 1-2)
                   |   # I.e. doubling this value doubles the time required to compute firing solutions but only increases their
                   |   # accuracy a little bit.
                   |   # I believe that 1 is the value used in Vanilla
                   |   ,"customAIRecursionLevel" : 1 # <---- EDIT HERE (maybe)----                   

                   |   # Any positive or negative float possible, reasonable values: between 0.7 ~ 2.0 or so
                   |   # 1.0 means "fire if shot will land within 1.0*(targetHitbox+10)"
                   |   # (the +10 serves to compensate for very small targets such as missiles and fighters)
                   |   ,"customAITriggerHappiness" : 1.1 # <---- EDIT HERE (maybe) ----

                   |   # Set this to true if you want the custom AI to perform better :P
                   |   ,"customAIAlwaysUsesBestTargetLeading" : false # <---- EDIT HERE (maybe) ----

                   |   #                                 #### FRIENDLY FIRE AI CONFIGURATION ####
                   |   # "magic number" to choose how complex the friendly fire calculation should be
                   |   # The number entered here roughly corresponds to the big O notation (i.e. runtime of friendly fire algorithm ~ n^i,
                   |   # where n is the number of entities (ships/missiles) in range of the ship and i is the number chosen here)
                   |   # Valid numbers are:
                   |   #     - 0 : No friendly fire computation, weapons won't care about hitting allies
                   |   #     - 1 : Weapons won't consider friendly fire for target selection, only for deciding whether to fire or not
                   |   #     - 2 : Weapon will only select targets that don't risk friendly fire (potentially high performance cost)
                   |   ,"customAIFriendlyFireAlgorithmComplexity" : 1 # <---- EDIT HERE (maybe) ----

                   |   # Essentially the same as triggerHappiness, but used to prevent firing if ally would be hit
                   |   # 1.0 should be enough to not hit allies if they don't change their course, but it's nice to have a little buffer
                   |   ,"customAIFriendlyFireCaution" : 1.1 # <---- EDIT HERE (maybe) ----                   
                   | 
                   |   #                                 #### MODE/SUFFIX CUSTOMIZATION ####
                   |   # NOTE: Unless stated otherwise, numbers in this section should be positive values between (exclusively) 0 and 1 and represent fractions (i.e. 0.01 to 0.99)
                   |   # NOTE: Using invalid values might cause very odd behaviour and/or crashes!
                   |   
                   |   # Shield thresholds: When not flanking shields and shields are on, the shield factor is simply
                   |   # equal to (1 - fluxLevel) of the target. When flanking shields, shield factor == 0.
                   |   # When shields are off, the shield factor is equal to (1 - fluxLevel)*0.75
                   |   # When omni-shields are off, it's considered as half-flanking (subject to change)
                   |   # For frontal shields, unfold time and projectile travel time are considered to determine flanking
                   |   # For modes that want to hit shields, reducing the threshold makes them more likely to fire
                   |   # For modes that want to avoid shields, the opposite is true
                   |   ,"targetShields_threshold" : 0.2
                   |   ,"avoidShields_threshold" : 0.5
                   |   
                   |   # Opportunist fire mode AND conserveAmmo suffix:                   |  
                   |   ,"opportunist_kineticThreshold" : 0.5 
                   |   ,"opportunist_HEThreshold" : 0.15 
                   |    # increasing this value will increase the likelihood of opportunist/conserveAmmo firing (positive non-zero number)
                   |    # Note: Relatively small changes to this value will have a considerable impact. So I'd recommend values between 0.9 and 1.2 or so
                   |   ,"opportunist_triggerHappinessModifier" : 1.0
                   |   
                   |   # Vent ship modes:
                   |   # Vent (Flux>75%)
                   |   ,"vent_flux" : 0.75 # vent if flux level > X
                   |   ,"vent_safetyFactor" : 2.0 # vent only if ship thinks it will survive venting X times (positive non-zero number)
                   |   
                   |   # VentAggressive (Flux>25%)
                   |   ,"aggressiveVent_flux" : 0.25 # vent if flux level > X
                   |   ,"aggressiveVent_safetyFactor" : 0.25 # (positive non-zero number)
                   |   
                   |   ,"retreat_hull" : 0.5 # retreat if hull level < X
                   |   ,"shieldsOff_flux" : 0.5 # In ShieldsOff (Flux>50%) mode, turn off shields if flux level > X
                   |   
                   |   ,"holdFire50_flux" : 0.5
                   |   ,"holdFire75_flux" : 0.75
                   |   ,"holdFire90_flux" : 0.9
                   |   
                   |   ,"pd50_flux" : 0.5
                   |   ,"pd90_ammo" : 0.5
                   |   ,"conserveAmmo_ammo" : 0.5
                   |   
                   |   ,"panicFire_hull" : 0.5
                   |   
                   |   ,"retreat_shouldDirectRetreat" : false
                   |   # If false, the BigShips/SmallShips modes will still target frigates/capitals etc.
                   |   ,"strictBigSmallShipMode" : true 
                   | }

                """.trimMargin()
            )
    }

    // If enabled, will copy your mod to the /mods directory when run (and whenever gradle syncs).
    // Disabled by default, as it is not needed if your mod directory is symlinked into your /mods folder.
    register<Copy>("install-mod") {
        val enabled = false;

        if (!enabled) return@register

        println("Installing mod into Starsector mod folder...")

        val destinations = listOf(modInModsFolder)

        destinations.forEach { dest ->
            copy {
                from(projectDir)
                into(dest)
                exclude(".git", ".github", ".gradle", ".idea", ".run", "gradle")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

// Compile to Java 6 bytecode so that Starsector can use it
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}