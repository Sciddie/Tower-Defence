package com.medieval.castledefense.game

import com.medieval.castledefense.data.Achievement

/**
 * Prüft Erfolgsbedingungen und meldet neu freigeschaltete Erfolge
 * (für eine Einblendung im Spiel) über [pollUnlocked].
 */
class AchievementManager(private val save: SaveManager) {

    private val newlyUnlocked = ArrayDeque<Achievement>()

    private fun tryGrant(a: Achievement) {
        if (save.grantAchievement(a)) newlyUnlocked.addLast(a)
    }

    /** Nach jedem gewonnenen Level aufrufen. */
    fun onLevelWon(hpLost: Boolean, wasBossLevel: Boolean) {
        tryGrant(Achievement.FIRST_VICTORY)
        if (!hpLost) tryGrant(Achievement.INVINCIBLE)
        if (wasBossLevel) tryGrant(Achievement.BOSS_SLAYER)
        if (save.completedLevelCount() >= 25) tryGrant(Achievement.CASTLE_LORD)
        checkTotals()
    }

    fun checkTotals() {
        if (save.totalGoldEarned >= 10_000) tryGrant(Achievement.GOLD_RUSH)
        if (save.totalKills >= 500) tryGrant(Achievement.SLAYER_500)
        if (save.builtAllTowerTypes()) tryGrant(Achievement.MASTER_BUILDER)
        if (save.reachedTowerLevel5) tryGrant(Achievement.MAX_POWER)
    }

    /** Liefert den nächsten frisch freigeschalteten Erfolg (oder null). */
    fun pollUnlocked(): Achievement? = newlyUnlocked.removeFirstOrNull()
}
