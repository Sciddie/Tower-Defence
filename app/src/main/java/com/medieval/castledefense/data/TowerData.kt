package com.medieval.castledefense.data

import android.graphics.Color

/** Art des Projektils – bestimmt Optik, Sound und Trefferverhalten. */
enum class ProjectileKind { ARROW, BOLT, CANNONBALL, MAGIC, FROST, ROCK }

enum class TowerType { ARCHER, CANNON, CROSSBOW, MAGE, FROST, SIEGE }

/**
 * Werte einer einzelnen Turmstufe.
 * Schaden pro Schuss, Reichweite in Welteinheiten, Feuerrate in Schuss/Sekunde.
 */
data class TowerStats(
    val damage: Float,
    val range: Float,
    val fireRate: Float,
    val splashRadius: Float = 0f,       // >0: Flächenschaden
    val slowFactor: Float = 0f,         // 0..1: Anteil, um den Gegner verlangsamt werden
    val slowDuration: Float = 0f,
    val chainTargets: Int = 0,          // Magier: zusätzliche Kettenziele
    val magic: Boolean = false          // magischer Schaden ignoriert Rüstung
)

/** Ein Upgrade-Pfad: vollständige Werte für Stufe 1..5 (Stufen 1-2 sind pfadunabhängig identisch). */
data class UpgradePath(val name: String, val levels: List<TowerStats>)

data class TowerConfig(
    val type: TowerType,
    val name: String,
    val description: String,
    val color: Int,
    val accentColor: Int,
    val projectile: ProjectileKind,
    val projectileSpeed: Float,
    /** costs[0] = Baukosten, costs[1..4] = Upgradekosten auf Stufe 2..5 */
    val costs: List<Int>,
    val paths: List<UpgradePath>,
    /** Level (1-basiert), nach dessen Abschluss der Turm freigeschaltet wird. 0 = sofort verfügbar. */
    val unlockAfterLevel: Int
) {
    val maxLevel get() = paths[0].levels.size
    fun stats(level: Int, pathIndex: Int): TowerStats =
        paths[pathIndex.coerceIn(0, paths.size - 1)].levels[(level - 1).coerceIn(0, maxLevel - 1)]
}

object TowerData {

    private fun lvls(vararg s: TowerStats) = s.toList()

    val CONFIGS: Map<TowerType, TowerConfig> = listOf(

        TowerConfig(
            type = TowerType.ARCHER,
            name = "Bogenschützenturm",
            description = "Günstig, schnell, ideal gegen flinke Gegner.",
            color = Color.rgb(126, 145, 92), accentColor = Color.rgb(222, 197, 132),
            projectile = ProjectileKind.ARROW, projectileSpeed = 11f,
            costs = listOf(50, 40, 70, 110, 160),
            paths = listOf(
                UpgradePath("Schnellfeuer", lvls(
                    TowerStats(8f, 2.6f, 1.6f),
                    TowerStats(11f, 2.7f, 1.9f),
                    TowerStats(12f, 2.8f, 2.6f),
                    TowerStats(14f, 2.9f, 3.4f),
                    TowerStats(16f, 3.0f, 4.4f)
                )),
                UpgradePath("Präzision", lvls(
                    TowerStats(8f, 2.6f, 1.6f),
                    TowerStats(11f, 2.7f, 1.9f),
                    TowerStats(18f, 3.3f, 1.9f),
                    TowerStats(26f, 3.7f, 2.0f),
                    TowerStats(38f, 4.2f, 2.1f)
                ))
            ),
            unlockAfterLevel = 0
        ),

        TowerConfig(
            type = TowerType.CANNON,
            name = "Kanonenturm",
            description = "Langsam, aber verheerender Flächenschaden.",
            color = Color.rgb(94, 94, 102), accentColor = Color.rgb(60, 60, 66),
            projectile = ProjectileKind.CANNONBALL, projectileSpeed = 7.5f,
            costs = listOf(100, 80, 130, 200, 300),
            paths = listOf(
                UpgradePath("Sprengkraft", lvls(
                    TowerStats(26f, 2.3f, 0.55f, splashRadius = 0.75f),
                    TowerStats(36f, 2.4f, 0.60f, splashRadius = 0.85f),
                    TowerStats(50f, 2.5f, 0.65f, splashRadius = 0.95f),
                    TowerStats(70f, 2.7f, 0.70f, splashRadius = 1.10f),
                    TowerStats(96f, 2.9f, 0.75f, splashRadius = 1.30f)
                ))
            ),
            unlockAfterLevel = 0
        ),

        TowerConfig(
            type = TowerType.CROSSBOW,
            name = "Armbrustturm",
            description = "Hohe Reichweite, hoher Einzelzielschaden.",
            color = Color.rgb(140, 104, 66), accentColor = Color.rgb(90, 62, 34),
            projectile = ProjectileKind.BOLT, projectileSpeed = 14f,
            costs = listOf(150, 110, 170, 260, 380),
            paths = listOf(
                UpgradePath("Durchschlag", lvls(
                    TowerStats(30f, 3.6f, 0.9f),
                    TowerStats(42f, 3.8f, 0.95f),
                    TowerStats(58f, 4.0f, 1.0f),
                    TowerStats(80f, 4.3f, 1.05f),
                    TowerStats(110f, 4.6f, 1.1f)
                ))
            ),
            unlockAfterLevel = 2
        ),

        TowerConfig(
            type = TowerType.MAGE,
            name = "Magierturm",
            description = "Magischer Kettenblitz, ignoriert Rüstung.",
            color = Color.rgb(96, 78, 160), accentColor = Color.rgb(168, 120, 255),
            projectile = ProjectileKind.MAGIC, projectileSpeed = 9f,
            costs = listOf(200, 140, 220, 330, 480),
            paths = listOf(
                UpgradePath("Kettenblitz", lvls(
                    TowerStats(18f, 2.8f, 0.8f, chainTargets = 1, magic = true),
                    TowerStats(24f, 2.9f, 0.85f, chainTargets = 2, magic = true),
                    TowerStats(30f, 3.0f, 0.9f, chainTargets = 3, magic = true),
                    TowerStats(38f, 3.1f, 0.95f, chainTargets = 4, magic = true),
                    TowerStats(48f, 3.2f, 1.0f, chainTargets = 5, magic = true)
                )),
                UpgradePath("Arkanfokus", lvls(
                    TowerStats(18f, 2.8f, 0.8f, chainTargets = 1, magic = true),
                    TowerStats(24f, 2.9f, 0.85f, chainTargets = 2, magic = true),
                    TowerStats(52f, 3.2f, 0.9f, chainTargets = 1, magic = true),
                    TowerStats(78f, 3.4f, 0.95f, chainTargets = 1, magic = true),
                    TowerStats(115f, 3.6f, 1.0f, chainTargets = 1, magic = true)
                ))
            ),
            unlockAfterLevel = 7
        ),

        TowerConfig(
            type = TowerType.FROST,
            name = "Frostturm",
            description = "Wenig Schaden, verlangsamt Gegner stark.",
            color = Color.rgb(110, 170, 200), accentColor = Color.rgb(200, 240, 255),
            projectile = ProjectileKind.FROST, projectileSpeed = 10f,
            costs = listOf(175, 120, 190, 280, 400),
            paths = listOf(
                UpgradePath("Eiseskälte", lvls(
                    TowerStats(6f, 2.5f, 1.0f, slowFactor = 0.30f, slowDuration = 1.5f, magic = true),
                    TowerStats(8f, 2.6f, 1.05f, slowFactor = 0.35f, slowDuration = 1.8f, magic = true),
                    TowerStats(10f, 2.7f, 1.1f, slowFactor = 0.40f, slowDuration = 2.1f, magic = true),
                    TowerStats(13f, 2.9f, 1.15f, slowFactor = 0.45f, slowDuration = 2.4f, magic = true),
                    TowerStats(16f, 3.1f, 1.2f, slowFactor = 0.55f, slowDuration = 2.8f, magic = true)
                ))
            ),
            unlockAfterLevel = 4
        ),

        TowerConfig(
            type = TowerType.SIEGE,
            name = "Belagerungsturm",
            description = "Extrem hoher Schaden auf sehr große Distanz.",
            color = Color.rgb(120, 84, 60), accentColor = Color.rgb(200, 60, 50),
            projectile = ProjectileKind.ROCK, projectileSpeed = 6.5f,
            costs = listOf(400, 260, 400, 600, 850),
            paths = listOf(
                UpgradePath("Zerschmetterer", lvls(
                    TowerStats(140f, 5.2f, 0.28f, splashRadius = 0.6f),
                    TowerStats(190f, 5.5f, 0.30f, splashRadius = 0.7f),
                    TowerStats(260f, 5.8f, 0.32f, splashRadius = 0.8f),
                    TowerStats(350f, 6.1f, 0.34f, splashRadius = 0.9f),
                    TowerStats(470f, 6.5f, 0.36f, splashRadius = 1.0f)
                ))
            ),
            unlockAfterLevel = 11
        )
    ).associateBy { it.type }

    /** Verkaufswert: 70 % der investierten Kosten. */
    fun sellValue(type: TowerType, level: Int): Int {
        val c = CONFIGS.getValue(type).costs
        var invested = 0
        for (i in 0 until level.coerceAtMost(c.size)) invested += c[i]
        return (invested * 0.7f).toInt()
    }

    fun upgradeCost(type: TowerType, currentLevel: Int): Int? {
        val c = CONFIGS.getValue(type).costs
        return if (currentLevel < c.size) c[currentLevel] else null
    }
}
