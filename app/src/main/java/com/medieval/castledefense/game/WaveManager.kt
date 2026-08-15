package com.medieval.castledefense.game

import com.medieval.castledefense.data.EnemyType
import com.medieval.castledefense.data.LevelConfig

/**
 * Spawnt die Gegner der aktuellen Welle gemäß der datengetriebenen
 * Wellendefinition (Gruppen mit Startverzögerung und Spawn-Intervall).
 */
class WaveManager(private val level: LevelConfig) {

    private class GroupState { var spawned = 0; var timer = 0f }

    private var groupStates: List<GroupState> = emptyList()
    var currentWave = -1     // 0-basiert; -1 = noch keine Welle gestartet
        private set

    val totalWaves get() = level.waves.size
    val isLastWave get() = currentWave >= totalWaves - 1

    fun startNextWave(): Boolean {
        if (isLastWave && currentWave >= 0) return false
        currentWave++
        val wave = level.waves[currentWave]
        groupStates = wave.groups.map { g ->
            GroupState().apply { timer = g.startDelay }
        }
        return true
    }

    /** true, wenn alle Gegner der aktuellen Welle gespawnt wurden. */
    val finishedSpawning: Boolean
        get() = currentWave >= 0 && groupStates.withIndex().all { (i, s) ->
            s.spawned >= level.waves[currentWave].groups[i].count
        }

    fun update(dt: Float, spawn: (EnemyType) -> Unit) {
        if (currentWave < 0) return
        val wave = level.waves[currentWave]
        for ((i, state) in groupStates.withIndex()) {
            val group = wave.groups[i]
            if (state.spawned >= group.count) continue
            state.timer -= dt
            while (state.timer <= 0f && state.spawned < group.count) {
                spawn(group.type)
                state.spawned++
                state.timer += group.interval
            }
        }
    }
}
