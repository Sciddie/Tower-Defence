package com.medieval.castledefense.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.medieval.castledefense.data.LevelConfig
import com.medieval.castledefense.data.MapTheme
import kotlin.random.Random

/**
 * Zeichnet die komplette Map (Untergrund, Deko, Weg, Spawn, Burg, Bauplätze)
 * in Welt-Koordinaten (16 x 9). Der GameScreen cached das Ergebnis als Bitmap,
 * sodass pro Frame nur ein Blit nötig ist.
 */
class MapRenderer(private val level: LevelConfig) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val theme = level.theme

    fun drawWorld(c: Canvas) {
        drawGround(c)
        drawDecorations(c)
        drawPath(c)
        drawSpawn(c)
        drawCastle(c)
        drawBuildSpots(c)
        if (theme.skyTint != 0) {
            paint.color = theme.skyTint
            c.drawRect(0f, 0f, 16f, 9f, paint)
        }
    }

    private fun drawGround(c: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = theme.grassColor
        c.drawRect(0f, 0f, 16f, 9f, paint)
        // Schachbrett-Struktur für Tiefe
        paint.color = theme.grassColor2
        val rnd = Random(level.index * 31L)
        for (gx in 0 until 16) for (gy in 0 until 9) {
            if ((gx + gy) % 2 == 0 || rnd.nextFloat() < 0.1f) {
                c.drawRect(gx.toFloat(), gy.toFloat(), gx + 1f, gy + 1f, paint)
            }
        }
    }

    private fun drawPath(c: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        val wp = level.layout.waypoints
        path.reset()
        path.moveTo(wp[0].x, wp[0].y)
        for (i in 1 until wp.size) path.lineTo(wp[i].x, wp[i].y)
        paint.strokeWidth = 0.86f
        paint.color = theme.pathEdgeColor
        c.drawPath(path, paint)
        paint.strokeWidth = 0.7f
        paint.color = theme.pathColor
        c.drawPath(path, paint)
        // Trittspuren
        paint.style = Paint.Style.FILL
        paint.color = theme.pathEdgeColor
        val rnd = Random(level.index * 17L)
        for (i in 1 until wp.size) {
            val a = wp[i - 1]; val b = wp[i]
            val segs = (Math.hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()) * 2).toInt()
            for (s in 0..segs) {
                val t = s / segs.coerceAtLeast(1).toFloat()
                c.drawCircle(
                    a.x + (b.x - a.x) * t + rnd.nextFloat() * 0.3f - 0.15f,
                    a.y + (b.y - a.y) * t + rnd.nextFloat() * 0.3f - 0.15f,
                    0.035f, paint
                )
            }
        }
    }

    private fun drawSpawn(c: Canvas) {
        val s = level.layout.waypoints.first()
        paint.style = Paint.Style.FILL
        // dunkles Tor, aus dem die Gegner kommen
        paint.color = Color.rgb(50, 40, 34)
        rect.set(s.x - 0.55f, s.y - 0.7f, s.x + 0.55f, s.y + 0.7f)
        c.drawRoundRect(rect, 0.2f, 0.2f, paint)
        paint.color = Color.rgb(20, 16, 14)
        rect.set(s.x - 0.35f, s.y - 0.45f, s.x + 0.35f, s.y + 0.7f)
        c.drawRoundRect(rect, 0.3f, 0.3f, paint)
        paint.color = Color.rgb(200, 60, 50)
        c.drawCircle(s.x, s.y - 0.85f, 0.12f, paint)
    }

    private fun drawCastle(c: Canvas) {
        val e = level.layout.waypoints.last()
        val col = theme.castleColor
        val dark = darken(col, 0.7f)
        paint.style = Paint.Style.FILL
        // Schatten
        paint.color = Color.argb(70, 0, 0, 0)
        c.drawOval(RectF(e.x - 1.05f, e.y + 0.55f, e.x + 1.05f, e.y + 0.95f), paint)
        // Seitentürme
        paint.color = dark
        rect.set(e.x - 1.0f, e.y - 0.9f, e.x - 0.45f, e.y + 0.75f)
        c.drawRect(rect, paint)
        rect.set(e.x + 0.45f, e.y - 0.9f, e.x + 1.0f, e.y + 0.75f)
        c.drawRect(rect, paint)
        // Hauptturm
        paint.color = col
        rect.set(e.x - 0.55f, e.y - 1.3f, e.x + 0.55f, e.y + 0.75f)
        c.drawRect(rect, paint)
        // Zinnen
        paint.color = dark
        for (i in -2..2) {
            c.drawRect(e.x + i * 0.22f - 0.07f, e.y - 1.5f, e.x + i * 0.22f + 0.07f, e.y - 1.3f, paint)
        }
        for (ox in floatArrayOf(-0.725f, 0.725f)) {
            for (i in -1..1) {
                c.drawRect(e.x + ox + i * 0.18f - 0.05f, e.y - 1.06f, e.x + ox + i * 0.18f + 0.05f, e.y - 0.9f, paint)
            }
        }
        // Tor
        paint.color = Color.rgb(60, 44, 28)
        rect.set(e.x - 0.28f, e.y + 0.05f, e.x + 0.28f, e.y + 0.75f)
        c.drawRoundRect(rect, 0.22f, 0.22f, paint)
        // Fahne
        paint.color = Color.rgb(60, 50, 46)
        c.drawRect(e.x - 0.02f, e.y - 2.05f, e.x + 0.02f, e.y - 1.5f, paint)
        paint.color = Color.rgb(198, 50, 60)
        path.reset()
        path.moveTo(e.x + 0.02f, e.y - 2.05f)
        path.lineTo(e.x + 0.45f, e.y - 1.93f)
        path.lineTo(e.x + 0.02f, e.y - 1.8f)
        path.close()
        c.drawPath(path, paint)
    }

    private fun drawBuildSpots(c: Canvas) {
        for (s in level.layout.buildSpots) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(70, 0, 0, 0)
            c.drawCircle(s.x + 0.03f, s.y + 0.05f, 0.42f, paint)
            paint.color = darken(theme.grassColor, 0.8f)
            c.drawCircle(s.x, s.y, 0.42f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.045f
            paint.color = Color.argb(160, 230, 220, 190)
            c.drawCircle(s.x, s.y, 0.36f, paint)
            paint.pathEffect = null
            // Hammer-Symbol
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(150, 230, 220, 190)
            c.drawRect(s.x - 0.04f, s.y - 0.14f, s.x + 0.04f, s.y + 0.18f, paint)
            c.drawRect(s.x - 0.16f, s.y - 0.2f, s.x + 0.16f, s.y - 0.08f, paint)
        }
    }

    private fun drawDecorations(c: Canvas) {
        val rnd = Random(level.index * 101L)
        val spots = level.layout.buildSpots
        val wp = level.layout.waypoints
        var placed = 0
        var tries = 0
        while (placed < 26 && tries < 300) {
            tries++
            val x = rnd.nextFloat() * 15f + 0.5f
            val y = rnd.nextFloat() * 8f + 0.5f
            // Abstand zu Weg und Bauplätzen einhalten
            if (spots.any { Math.hypot((it.x - x).toDouble(), (it.y - y).toDouble()) < 0.95 }) continue
            var nearPath = false
            for (i in 1 until wp.size) {
                if (segmentDist(wp[i - 1].x, wp[i - 1].y, wp[i].x, wp[i].y, x, y) < 0.85f) { nearPath = true; break }
            }
            if (nearPath) continue
            if (Math.hypot((wp.last().x - x).toDouble(), (wp.last().y - y).toDouble()) < 1.7) continue
            drawDeco(c, x, y, rnd)
            placed++
        }
    }

    private fun drawDeco(c: Canvas, x: Float, y: Float, rnd: Random) {
        paint.style = Paint.Style.FILL
        val s = 0.55f + rnd.nextFloat() * 0.5f
        when (theme) {
            MapTheme.KINGDOM, MapTheme.DARK_FOREST -> {
                // Nadelbaum
                paint.color = Color.rgb(80, 56, 34)
                c.drawRect(x - 0.05f * s, y, x + 0.05f * s, y + 0.22f * s, paint)
                paint.color = if (rnd.nextBoolean()) theme.decoColor else theme.decoColor2
                path.reset()
                path.moveTo(x, y - 0.75f * s)
                path.lineTo(x - 0.34f * s, y + 0.05f * s)
                path.lineTo(x + 0.34f * s, y + 0.05f * s)
                path.close()
                c.drawPath(path, paint)
                path.reset()
                path.moveTo(x, y - 1.05f * s)
                path.lineTo(x - 0.26f * s, y - 0.35f * s)
                path.lineTo(x + 0.26f * s, y - 0.35f * s)
                path.close()
                c.drawPath(path, paint)
            }
            MapTheme.MOUNTAIN -> {
                paint.color = if (rnd.nextBoolean()) theme.decoColor else theme.decoColor2
                path.reset()
                path.moveTo(x - 0.4f * s, y + 0.2f * s)
                path.lineTo(x - 0.1f * s, y - 0.5f * s)
                path.lineTo(x + 0.15f * s, y - 0.15f * s)
                path.lineTo(x + 0.42f * s, y + 0.2f * s)
                path.close()
                c.drawPath(path, paint)
                paint.color = Color.rgb(235, 240, 245)
                path.reset()
                path.moveTo(x - 0.16f * s, y - 0.36f * s)
                path.lineTo(x - 0.1f * s, y - 0.5f * s)
                path.lineTo(x - 0.02f * s, y - 0.33f * s)
                path.close()
                c.drawPath(path, paint)
            }
            MapTheme.WINTER -> {
                // verschneiter Baum
                paint.color = theme.decoColor
                path.reset()
                path.moveTo(x, y - 0.9f * s)
                path.lineTo(x - 0.3f * s, y + 0.1f * s)
                path.lineTo(x + 0.3f * s, y + 0.1f * s)
                path.close()
                c.drawPath(path, paint)
                paint.color = theme.decoColor2
                path.reset()
                path.moveTo(x, y - 0.9f * s)
                path.lineTo(x - 0.18f * s, y - 0.3f * s)
                path.lineTo(x + 0.18f * s, y - 0.3f * s)
                path.close()
                c.drawPath(path, paint)
            }
            MapTheme.DESERT -> {
                if (rnd.nextFloat() < 0.5f) {
                    // Kaktus
                    paint.color = theme.decoColor2
                    rect.set(x - 0.07f * s, y - 0.55f * s, x + 0.07f * s, y + 0.15f * s)
                    c.drawRoundRect(rect, 0.07f, 0.07f, paint)
                    rect.set(x - 0.28f * s, y - 0.4f * s, x - 0.07f * s, y - 0.28f * s)
                    c.drawRoundRect(rect, 0.05f, 0.05f, paint)
                    rect.set(x - 0.28f * s, y - 0.55f * s, x - 0.16f * s, y - 0.28f * s)
                    c.drawRoundRect(rect, 0.05f, 0.05f, paint)
                } else {
                    paint.color = theme.decoColor
                    c.drawCircle(x, y, 0.22f * s, paint)
                    c.drawCircle(x + 0.2f * s, y + 0.05f, 0.15f * s, paint)
                }
            }
            MapTheme.VOLCANO -> {
                if (rnd.nextFloat() < 0.4f) {
                    // Lavabecken
                    paint.color = theme.decoColor2
                    c.drawOval(RectF(x - 0.4f * s, y - 0.22f * s, x + 0.4f * s, y + 0.22f * s), paint)
                    paint.color = Color.rgb(255, 200, 60)
                    c.drawOval(RectF(x - 0.2f * s, y - 0.1f * s, x + 0.2f * s, y + 0.1f * s), paint)
                } else {
                    paint.color = theme.decoColor
                    path.reset()
                    path.moveTo(x - 0.35f * s, y + 0.2f * s)
                    path.lineTo(x, y - 0.45f * s)
                    path.lineTo(x + 0.35f * s, y + 0.2f * s)
                    path.close()
                    c.drawPath(path, paint)
                }
            }
        }
    }

    private fun segmentDist(x1: Float, y1: Float, x2: Float, y2: Float, px: Float, py: Float): Float {
        val dx = x2 - x1; val dy = y2 - y1
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0f) 0f else (((px - x1) * dx + (py - y1) * dy) / len2).coerceIn(0f, 1f)
        val cx = x1 + t * dx; val cy = y1 + t * dy
        return Math.hypot((px - cx).toDouble(), (py - cy).toDouble()).toFloat()
    }

    private fun darken(color: Int, f: Float): Int = Color.rgb(
        (Color.red(color) * f).toInt(), (Color.green(color) * f).toInt(), (Color.blue(color) * f).toInt()
    )
}
