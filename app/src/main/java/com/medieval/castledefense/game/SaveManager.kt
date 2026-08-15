package com.medieval.castledefense.game

import android.content.Context
import android.content.SharedPreferences
import com.medieval.castledefense.data.Achievement
import com.medieval.castledefense.data.LevelData
import com.medieval.castledefense.data.TowerData
import com.medieval.castledefense.data.TowerType

/**
 * Lokale Persistenz über SharedPreferences:
 * Sterne, freigeschaltete Level/Türme, Erfolge, Statistiken, Einstellungen.
 */
class SaveManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mcd_save", Context.MODE_PRIVATE)

    // ---- Sterne & Levelfortschritt ----

    fun stars(level: Int): Int = prefs.getInt("stars_$level", 0)

    fun recordStars(level: Int, stars: Int) {
        if (stars > stars(level)) prefs.edit().putInt("stars_$level", stars).apply()
    }

    fun isLevelCompleted(level: Int) = stars(level) > 0

    fun isLevelUnlocked(level: Int) = level == 1 || isLevelCompleted(level - 1)

    fun completedLevelCount(): Int =
        (1..LevelData.LEVEL_COUNT).count { isLevelCompleted(it) }

    fun totalStars(): Int = (1..LevelData.LEVEL_COUNT).sumOf { stars(it) }

    // ---- Türme ----

    fun isTowerUnlocked(type: TowerType): Boolean {
        val after = TowerData.CONFIGS.getValue(type).unlockAfterLevel
        return after == 0 || isLevelCompleted(after)
    }

    // ---- Statistiken ----

    var totalGoldEarned: Long
        get() = prefs.getLong("totalGold", 0)
        set(v) = prefs.edit().putLong("totalGold", v).apply()

    var totalKills: Long
        get() = prefs.getLong("totalKills", 0)
        set(v) = prefs.edit().putLong("totalKills", v).apply()

    var bossesKilled: Int
        get() = prefs.getInt("bossKills", 0)
        set(v) = prefs.edit().putInt("bossKills", v).apply()

    fun markTowerBuilt(type: TowerType) =
        prefs.edit().putBoolean("built_${type.name}", true).apply()

    fun builtAllTowerTypes(): Boolean =
        TowerType.entries.all { prefs.getBoolean("built_${it.name}", false) }

    var reachedTowerLevel5: Boolean
        get() = prefs.getBoolean("towerLvl5", false)
        set(v) = prefs.edit().putBoolean("towerLvl5", v).apply()

    // ---- Erfolge ----

    fun hasAchievement(a: Achievement) = prefs.getBoolean("ach_${a.name}", false)

    fun grantAchievement(a: Achievement): Boolean {
        if (hasAchievement(a)) return false
        prefs.edit().putBoolean("ach_${a.name}", true).apply()
        return true
    }

    // ---- Einstellungen ----

    var musicOn: Boolean
        get() = prefs.getBoolean("musicOn", true)
        set(v) = prefs.edit().putBoolean("musicOn", v).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean("soundOn", true)
        set(v) = prefs.edit().putBoolean("soundOn", v).apply()
}
