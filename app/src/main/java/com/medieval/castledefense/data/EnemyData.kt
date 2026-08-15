package com.medieval.castledefense.data

import android.graphics.Color

enum class EnemyType { SOLDIER, SCOUT, KNIGHT, HEAVY_KNIGHT, ARCHER, SIEGE_UNIT, BOSS }

/** Bewegungstyp – Späher weichen leicht aus (Zickzack), Bosse stampfen. */
enum class MovementType { WALK, DART, STOMP }

data class EnemyConfig(
    val type: EnemyType,
    val name: String,
    val health: Float,
    val speed: Float,          // Welteinheiten pro Sekunde
    val castleDamage: Int,     // Schaden an der Burg beim Durchbruch
    val goldReward: Int,
    val armor: Float,          // flache Schadensreduktion pro Treffer (physisch)
    val size: Float,           // Radius in Welteinheiten
    val movement: MovementType,
    val bodyColor: Int,
    val accentColor: Int,
    /** Bogenschütze: deaktiviert in Intervallen nahe Türme. */
    val disablesTowers: Boolean = false
)

/** Boss-Varianten – pro Map-Thema ein eigener Boss mit eigenem Namen und Farbschema. */
data class BossVariant(val name: String, val bodyColor: Int, val accentColor: Int, val healthMul: Float)

object EnemyData {

    val CONFIGS: Map<EnemyType, EnemyConfig> = listOf(
        EnemyConfig(
            EnemyType.SOLDIER, "Fußsoldat",
            health = 32f, speed = 1.0f, castleDamage = 1, goldReward = 5,
            armor = 0f, size = 0.17f, movement = MovementType.WALK,
            bodyColor = Color.rgb(150, 60, 50), accentColor = Color.rgb(200, 180, 150)
        ),
        EnemyConfig(
            EnemyType.SCOUT, "Schneller Späher",
            health = 22f, speed = 2.3f, castleDamage = 1, goldReward = 8,
            armor = 0f, size = 0.14f, movement = MovementType.DART,
            bodyColor = Color.rgb(70, 120, 60), accentColor = Color.rgb(160, 190, 120)
        ),
        EnemyConfig(
            EnemyType.KNIGHT, "Ritter",
            health = 95f, speed = 0.9f, castleDamage = 1, goldReward = 15,
            armor = 3f, size = 0.20f, movement = MovementType.WALK,
            bodyColor = Color.rgb(120, 130, 150), accentColor = Color.rgb(70, 80, 110)
        ),
        EnemyConfig(
            EnemyType.HEAVY_KNIGHT, "Schwerer Ritter",
            health = 280f, speed = 0.55f, castleDamage = 2, goldReward = 30,
            armor = 9f, size = 0.24f, movement = MovementType.WALK,
            bodyColor = Color.rgb(80, 85, 100), accentColor = Color.rgb(190, 160, 60)
        ),
        EnemyConfig(
            EnemyType.ARCHER, "Bogenschütze",
            health = 60f, speed = 1.0f, castleDamage = 1, goldReward = 18,
            armor = 0f, size = 0.17f, movement = MovementType.WALK,
            bodyColor = Color.rgb(90, 70, 40), accentColor = Color.rgb(60, 130, 70),
            disablesTowers = true
        ),
        EnemyConfig(
            EnemyType.SIEGE_UNIT, "Belagerungseinheit",
            health = 520f, speed = 0.4f, castleDamage = 3, goldReward = 50,
            armor = 6f, size = 0.30f, movement = MovementType.STOMP,
            bodyColor = Color.rgb(100, 75, 50), accentColor = Color.rgb(55, 45, 35)
        ),
        EnemyConfig(
            EnemyType.BOSS, "Boss",
            health = 2400f, speed = 0.45f, castleDamage = 5, goldReward = 250,
            armor = 12f, size = 0.42f, movement = MovementType.STOMP,
            bodyColor = Color.rgb(40, 40, 45), accentColor = Color.rgb(200, 40, 40)
        )
    ).associateBy { it.type }

    val BOSS_VARIANTS: Map<MapTheme, BossVariant> = mapOf(
        MapTheme.KINGDOM to BossVariant("Der Schwarze Ritter", Color.rgb(35, 35, 42), Color.rgb(200, 40, 40), 1.0f),
        MapTheme.MOUNTAIN to BossVariant("Orkkönig", Color.rgb(70, 100, 55), Color.rgb(220, 190, 70), 1.15f),
        MapTheme.WINTER to BossVariant("Eisgolem", Color.rgb(150, 200, 230), Color.rgb(70, 120, 190), 1.3f),
        MapTheme.DESERT to BossVariant("Wikingerhäuptling", Color.rgb(160, 110, 60), Color.rgb(210, 60, 40), 1.45f),
        MapTheme.DARK_FOREST to BossVariant("Dunkler König", Color.rgb(45, 30, 60), Color.rgb(150, 60, 220), 1.6f),
        MapTheme.VOLCANO to BossVariant("Lava-Titan", Color.rgb(60, 30, 25), Color.rgb(255, 120, 30), 1.8f)
    )

    /** Skalierung der Gegnerwerte mit dem Level (datengetrieben anpassbar). */
    fun scaledHealth(type: EnemyType, level: Int): Float =
        CONFIGS.getValue(type).health * (1f + 0.13f * (level - 1))

    fun scaledGold(type: EnemyType, level: Int): Int =
        (CONFIGS.getValue(type).goldReward * (1f + 0.02f * (level - 1))).toInt()

    fun scaledArmor(type: EnemyType, level: Int): Float =
        CONFIGS.getValue(type).armor * (1f + 0.05f * (level - 1))
}
