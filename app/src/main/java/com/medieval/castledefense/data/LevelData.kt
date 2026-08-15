package com.medieval.castledefense.data

import kotlin.random.Random

/** Eine Spawn-Gruppe innerhalb einer Welle. */
data class SpawnGroup(
    val type: EnemyType,
    val count: Int,
    val interval: Float,     // Sekunden zwischen zwei Spawns
    val startDelay: Float = 0f
)

data class WaveDef(val groups: List<SpawnGroup>)

data class LevelConfig(
    val index: Int,                 // 1-basiert
    val theme: MapTheme,
    val layoutIndex: Int,
    val startGold: Int,
    val castleHp: Int,
    val waves: List<WaveDef>,
    val isBossLevel: Boolean,
    val difficultyLabel: String
) {
    val layout: PathLayout get() = MapData.LAYOUTS[layoutIndex]
}

/**
 * Datengetriebene Level-Fabrik.
 * Die ersten Level sind handabgestimmt, danach erzeugt ein deterministischer
 * Budget-Algorithmus beliebig viele weitere Level. Aktuell: 50 Level.
 */
object LevelData {

    const val LEVEL_COUNT = 50
    const val BOSS_EVERY = 5

    private val cache = HashMap<Int, LevelConfig>()

    fun level(index: Int): LevelConfig = cache.getOrPut(index) { build(index) }

    fun themeFor(index: Int): MapTheme = when {
        index <= 8 -> MapTheme.KINGDOM
        index <= 16 -> MapTheme.MOUNTAIN
        index <= 25 -> MapTheme.WINTER
        index <= 33 -> MapTheme.DESERT
        index <= 41 -> MapTheme.DARK_FOREST
        else -> MapTheme.VOLCANO
    }

    fun isBossLevel(index: Int) = index % BOSS_EVERY == 0

    private fun difficulty(index: Int) = when {
        index <= 10 -> "Leicht"
        index <= 25 -> "Mittel"
        index <= 40 -> "Schwer"
        else -> "Episch"
    }

    // Spawn-Intervalle pro Gegnertyp (Sekunden)
    private fun interval(t: EnemyType) = when (t) {
        EnemyType.SOLDIER -> 0.9f
        EnemyType.SCOUT -> 0.55f
        EnemyType.KNIGHT -> 1.1f
        EnemyType.ARCHER -> 1.0f
        EnemyType.HEAVY_KNIGHT -> 1.6f
        EnemyType.SIEGE_UNIT -> 2.4f
        EnemyType.BOSS -> 1f
    }

    // "Bedrohungskosten" pro Gegner für das Wellen-Budget
    private fun threat(t: EnemyType) = when (t) {
        EnemyType.SOLDIER -> 1.0f
        EnemyType.SCOUT -> 1.4f
        EnemyType.KNIGHT -> 3.0f
        EnemyType.ARCHER -> 3.5f
        EnemyType.HEAVY_KNIGHT -> 6.5f
        EnemyType.SIEGE_UNIT -> 11f
        EnemyType.BOSS -> 60f
    }

    /** Ab welchem Level ein Gegnertyp in Wellen auftaucht. */
    private fun availableFrom(t: EnemyType) = when (t) {
        EnemyType.SOLDIER -> 1
        EnemyType.SCOUT -> 2
        EnemyType.KNIGHT -> 3
        EnemyType.ARCHER -> 6
        EnemyType.HEAVY_KNIGHT -> 7
        EnemyType.SIEGE_UNIT -> 9
        EnemyType.BOSS -> Int.MAX_VALUE
    }

    private fun g(t: EnemyType, count: Int, delay: Float = 0f) =
        SpawnGroup(t, count, interval(t), delay)

    private fun build(index: Int): LevelConfig {
        val theme = themeFor(index)
        val layoutIndex = ((index - 1) * 7 + theme.ordinal) % MapData.LAYOUTS.size
        val waves = if (index <= 5) handTunedWaves(index) else generatedWaves(index)
        return LevelConfig(
            index = index,
            theme = theme,
            layoutIndex = layoutIndex,
            startGold = 200 + minOf(index * 8, 250),
            castleHp = 20,
            waves = waves,
            isBossLevel = isBossLevel(index),
            difficultyLabel = difficulty(index)
        )
    }

    /** Level 1–5 exakt nach Design-Vorgabe. */
    private fun handTunedWaves(index: Int): List<WaveDef> = when (index) {
        1 -> listOf(
            WaveDef(listOf(g(EnemyType.SOLDIER, 10))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 15), g(EnemyType.SCOUT, 2, 6f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 12), g(EnemyType.KNIGHT, 5, 4f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 10), g(EnemyType.SCOUT, 5, 5f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 12), g(EnemyType.KNIGHT, 6, 3f), g(EnemyType.HEAVY_KNIGHT, 1, 12f)))
        )
        2 -> listOf(
            WaveDef(listOf(g(EnemyType.SOLDIER, 12))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 10), g(EnemyType.SCOUT, 4, 4f))),
            WaveDef(listOf(g(EnemyType.SCOUT, 8), g(EnemyType.SOLDIER, 10, 3f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 14), g(EnemyType.KNIGHT, 4, 5f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 6), g(EnemyType.SCOUT, 8, 4f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 16), g(EnemyType.KNIGHT, 6, 4f), g(EnemyType.SCOUT, 6, 8f)))
        )
        3 -> listOf(
            WaveDef(listOf(g(EnemyType.SOLDIER, 14), g(EnemyType.SCOUT, 3, 5f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 5), g(EnemyType.SOLDIER, 10, 3f))),
            WaveDef(listOf(g(EnemyType.SCOUT, 10), g(EnemyType.KNIGHT, 4, 4f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 16), g(EnemyType.KNIGHT, 6, 4f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 8), g(EnemyType.SCOUT, 8, 5f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 10), g(EnemyType.SOLDIER, 12, 4f), g(EnemyType.HEAVY_KNIGHT, 2, 14f)))
        )
        4 -> listOf(
            WaveDef(listOf(g(EnemyType.SOLDIER, 14), g(EnemyType.SCOUT, 5, 4f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 6), g(EnemyType.SCOUT, 6, 4f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 18), g(EnemyType.KNIGHT, 5, 5f))),
            WaveDef(listOf(g(EnemyType.SCOUT, 12), g(EnemyType.KNIGHT, 5, 4f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 8), g(EnemyType.HEAVY_KNIGHT, 2, 8f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 16), g(EnemyType.KNIGHT, 8, 4f), g(EnemyType.SCOUT, 8, 8f))),
            WaveDef(listOf(g(EnemyType.HEAVY_KNIGHT, 3), g(EnemyType.KNIGHT, 8, 3f)))
        )
        else -> listOf( // Level 5 – erstes Boss-Level
            WaveDef(listOf(g(EnemyType.SOLDIER, 16), g(EnemyType.SCOUT, 5, 5f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 8), g(EnemyType.SOLDIER, 12, 3f))),
            WaveDef(listOf(g(EnemyType.SCOUT, 12), g(EnemyType.KNIGHT, 6, 4f))),
            WaveDef(listOf(g(EnemyType.KNIGHT, 10), g(EnemyType.HEAVY_KNIGHT, 2, 10f))),
            WaveDef(listOf(g(EnemyType.SOLDIER, 20), g(EnemyType.KNIGHT, 8, 5f))),
            WaveDef(listOf(g(EnemyType.HEAVY_KNIGHT, 3), g(EnemyType.SCOUT, 10, 5f))),
            WaveDef(listOf(g(EnemyType.BOSS, 1), g(EnemyType.KNIGHT, 6, 6f), g(EnemyType.SOLDIER, 10, 10f)))
        )
    }

    /** Deterministische Wellengenerierung über ein Bedrohungsbudget. */
    private fun generatedWaves(index: Int): List<WaveDef> {
        val rng = Random(index * 7919L)
        val waveCount = minOf(6 + index / 3, 15)
        val waves = ArrayList<WaveDef>(waveCount)
        val pool = EnemyType.entries.filter { it != EnemyType.BOSS && availableFrom(it) <= index }

        for (w in 1..waveCount) {
            val isLast = w == waveCount
            // Budget wächst mit Level und Wellennummer
            var budget = (8f + index * 2.2f) * (0.55f + 0.45f * w / waveCount.toFloat())
            if (isLast) budget *= 1.35f

            val groups = ArrayList<SpawnGroup>()
            var delay = 0f
            // schwere Typen zuerst einstreuen, Rest mit Standardgegnern füllen
            val heavies = pool.filter { threat(it) >= 3f }.shuffled(rng).take(2)
            for (h in heavies) {
                if (budget < threat(h) * 2 || rng.nextFloat() < 0.3f) continue
                val maxCount = (budget * 0.45f / threat(h)).toInt()
                if (maxCount <= 0) continue
                val count = 1 + rng.nextInt(maxCount.coerceAtMost(8))
                groups.add(g(h, count, delay))
                budget -= count * threat(h)
                delay += 3f + count * interval(h) * 0.5f
            }
            val fillers = pool.filter { threat(it) < 3f }
            val filler = fillers[rng.nextInt(fillers.size)]
            val count = (budget / threat(filler)).toInt().coerceAtLeast(3)
            groups.add(0, g(filler, count))

            // Boss-Level: finaler Boss mit Eskorte
            if (isLast && isBossLevel(index)) {
                groups.add(SpawnGroup(EnemyType.BOSS, 1 + (index / 25), 8f, 2f))
            }
            waves.add(WaveDef(groups))
        }
        return waves
    }
}
