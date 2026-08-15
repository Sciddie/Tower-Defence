package com.medieval.castledefense.data

import android.graphics.Color

/** Welt-Koordinate (Spielfeld ist 16 x 9 Einheiten groß). */
data class WPoint(val x: Float, val y: Float)

enum class MapTheme(
    val title: String,
    val grassColor: Int,
    val grassColor2: Int,
    val pathColor: Int,
    val pathEdgeColor: Int,
    val decoColor: Int,
    val decoColor2: Int,
    val castleColor: Int,
    val skyTint: Int,
    val fog: Boolean = false
) {
    KINGDOM(
        "Königreich",
        Color.rgb(96, 138, 68), Color.rgb(84, 126, 60),
        Color.rgb(178, 148, 98), Color.rgb(140, 112, 70),
        Color.rgb(46, 92, 44), Color.rgb(66, 112, 56),
        Color.rgb(168, 168, 178), Color.argb(0, 0, 0, 0)
    ),
    MOUNTAIN(
        "Bergpass",
        Color.rgb(128, 118, 100), Color.rgb(112, 104, 88),
        Color.rgb(90, 82, 70), Color.rgb(70, 62, 52),
        Color.rgb(96, 92, 88), Color.rgb(140, 134, 126),
        Color.rgb(120, 120, 130), Color.argb(30, 120, 130, 150)
    ),
    WINTER(
        "Winterreich",
        Color.rgb(222, 232, 240), Color.rgb(204, 218, 230),
        Color.rgb(160, 176, 194), Color.rgb(130, 146, 166),
        Color.rgb(70, 110, 90), Color.rgb(240, 248, 255),
        Color.rgb(190, 200, 220), Color.argb(40, 200, 220, 255)
    ),
    DESERT(
        "Wüste",
        Color.rgb(222, 192, 128), Color.rgb(208, 176, 112),
        Color.rgb(176, 140, 88), Color.rgb(148, 114, 66),
        Color.rgb(180, 140, 80), Color.rgb(120, 150, 90),
        Color.rgb(200, 170, 120), Color.argb(28, 255, 200, 90)
    ),
    DARK_FOREST(
        "Dunkler Wald",
        Color.rgb(44, 62, 44), Color.rgb(36, 52, 38),
        Color.rgb(80, 70, 58), Color.rgb(58, 50, 40),
        Color.rgb(24, 40, 28), Color.rgb(34, 54, 40),
        Color.rgb(60, 58, 74), Color.argb(70, 40, 50, 55), fog = true
    ),
    VOLCANO(
        "Vulkanland",
        Color.rgb(70, 52, 48), Color.rgb(58, 42, 40),
        Color.rgb(46, 36, 34), Color.rgb(30, 24, 22),
        Color.rgb(40, 30, 28), Color.rgb(255, 110, 30),
        Color.rgb(56, 44, 46), Color.argb(36, 255, 80, 20)
    )
}

/**
 * Ein Streckenlayout: Wegpunkte (Spawn → ... → Burg) und feste Bauplätze.
 * Gegner laufen die Wegpunkte strikt nacheinander ab; gebaut wird nur auf Bauplätzen.
 */
data class PathLayout(
    val waypoints: List<WPoint>,
    val buildSpots: List<WPoint>
)

object MapData {

    private fun p(x: Float, y: Float) = WPoint(x, y)

    /** Mehrere handgebaute Layouts; werden von den Leveln referenziert. */
    val LAYOUTS: List<PathLayout> = listOf(
        // 0 – klassische S-Kurve
        PathLayout(
            waypoints = listOf(p(-0.5f, 4.5f), p(4.5f, 4.5f), p(4.5f, 2.0f), p(9.5f, 2.0f), p(9.5f, 6.5f), p(14.2f, 6.5f)),
            buildSpots = listOf(
                p(3.0f, 3.2f), p(3.0f, 5.8f), p(5.9f, 3.3f), p(5.9f, 1.0f),
                p(8.2f, 3.3f), p(8.2f, 1.0f), p(10.9f, 5.2f), p(10.9f, 7.6f), p(12.6f, 5.2f), p(7.0f, 5.6f)
            )
        ),
        // 1 – langer Zickzack von oben
        PathLayout(
            waypoints = listOf(p(2.0f, -0.5f), p(2.0f, 6.8f), p(6.5f, 6.8f), p(6.5f, 1.6f), p(11.0f, 1.6f), p(11.0f, 6.0f), p(14.2f, 6.0f)),
            buildSpots = listOf(
                p(0.8f, 3.0f), p(3.4f, 3.6f), p(3.4f, 5.6f), p(5.2f, 5.4f),
                p(5.2f, 2.9f), p(7.9f, 2.9f), p(7.9f, 5.3f), p(9.6f, 3.0f), p(12.4f, 4.6f), p(9.6f, 7.3f)
            )
        ),
        // 2 – große U-Schleife
        PathLayout(
            waypoints = listOf(p(-0.5f, 1.8f), p(12.0f, 1.8f), p(12.0f, 7.0f), p(3.5f, 7.0f), p(3.5f, 4.4f), p(14.2f, 4.4f)),
            buildSpots = listOf(
                p(2.5f, 0.7f), p(5.5f, 0.7f), p(8.5f, 0.7f), p(10.6f, 3.1f),
                p(8.0f, 3.1f), p(5.6f, 3.1f), p(5.5f, 5.7f), p(8.5f, 5.7f), p(2.2f, 5.7f), p(13.2f, 2.9f)
            )
        ),
        // 3 – Serpentine (schmaler Pass)
        PathLayout(
            waypoints = listOf(p(-0.5f, 7.2f), p(5.0f, 7.2f), p(5.0f, 4.6f), p(1.8f, 4.6f), p(1.8f, 1.8f), p(9.0f, 1.8f), p(9.0f, 5.4f), p(14.2f, 5.4f)),
            buildSpots = listOf(
                p(2.6f, 6.0f), p(3.6f, 3.3f), p(0.7f, 3.2f), p(3.6f, 0.8f),
                p(6.4f, 3.2f), p(6.4f, 6.2f), p(7.7f, 0.7f), p(10.6f, 3.6f), p(10.6f, 6.8f), p(12.6f, 4.1f)
            )
        ),
        // 4 – Doppelkurve, Spawn rechts oben
        PathLayout(
            waypoints = listOf(p(13.0f, -0.5f), p(13.0f, 3.6f), p(7.0f, 3.6f), p(7.0f, 7.0f), p(2.2f, 7.0f), p(2.2f, 3.8f), p(-0.3f, 3.8f)),
            buildSpots = listOf(
                p(11.6f, 1.8f), p(14.3f, 2.2f), p(9.4f, 2.4f), p(9.4f, 5.0f),
                p(5.6f, 5.2f), p(5.6f, 2.3f), p(3.7f, 5.6f), p(0.9f, 5.6f), p(3.7f, 8.2f), p(11.6f, 5.0f)
            )
        ),
        // 5 – Spirale zur Mitte, Burg zentral
        PathLayout(
            waypoints = listOf(p(-0.5f, 0.9f), p(14.0f, 0.9f), p(14.0f, 7.6f), p(2.0f, 7.6f), p(2.0f, 3.2f), p(10.8f, 3.2f), p(10.8f, 5.4f)),
            buildSpots = listOf(
                p(3.5f, 2.0f), p(7.0f, 2.0f), p(10.5f, 2.0f), p(12.7f, 4.4f),
                p(9.0f, 4.4f), p(5.5f, 4.6f), p(3.6f, 5.6f), p(6.6f, 6.4f), p(9.5f, 6.5f), p(0.8f, 4.9f)
            )
        )
    )
}
