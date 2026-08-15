package com.medieval.castledefense.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.random.Random

/**
 * Gepoolte Partikel und schwebende Texte ("+10 Gold").
 * Object Pooling vermeidet Allokationen im Game-Loop (GC-Pausen).
 */
class Effects {

    class Particle {
        var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
        var life = 0f; var maxLife = 1f
        var color = Color.WHITE; var size = 0.05f
        var gravity = 0f
        var active = false
    }

    class FloatText {
        var x = 0f; var y = 0f; var text = ""
        var color = Color.WHITE; var life = 0f
        var active = false
    }

    private val particles = Array(512) { Particle() }
    private val texts = Array(32) { FloatText() }
    private var pIndex = 0
    private var tIndex = 0
    private val rnd = Random(1)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    fun clear() {
        particles.forEach { it.active = false }
        texts.forEach { it.active = false }
    }

    fun spawnBurst(x: Float, y: Float, color: Int, count: Int, speed: Float = 1.6f, size: Float = 0.05f, life: Float = 0.5f, gravity: Float = 2.5f) {
        repeat(count) {
            val p = particles[pIndex]; pIndex = (pIndex + 1) % particles.size
            p.x = x; p.y = y
            val a = rnd.nextFloat() * 6.2832f
            val s = speed * (0.3f + rnd.nextFloat())
            p.vx = kotlin.math.cos(a) * s; p.vy = kotlin.math.sin(a) * s - speed * 0.4f
            p.maxLife = life * (0.6f + rnd.nextFloat() * 0.7f); p.life = p.maxLife
            p.color = color; p.size = size * (0.6f + rnd.nextFloat() * 0.8f)
            p.gravity = gravity
            p.active = true
        }
    }

    /** Rauch/Feuer-Kombination für Explosionen. */
    fun spawnExplosion(x: Float, y: Float, radius: Float) {
        spawnBurst(x, y, Color.rgb(255, 170, 40), (radius * 26).toInt().coerceIn(10, 40), 2.6f, 0.07f, 0.45f, 1f)
        spawnBurst(x, y, Color.rgb(90, 80, 75), (radius * 16).toInt().coerceIn(6, 24), 1.2f, 0.10f, 0.8f, -0.8f)
    }

    fun spawnText(x: Float, y: Float, text: String, color: Int) {
        val t = texts[tIndex]; tIndex = (tIndex + 1) % texts.size
        t.x = x; t.y = y; t.text = text; t.color = color; t.life = 1.1f; t.active = true
    }

    fun update(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) { p.active = false; continue }
            p.vy += p.gravity * dt
            p.x += p.vx * dt; p.y += p.vy * dt
        }
        for (t in texts) {
            if (!t.active) continue
            t.life -= dt
            if (t.life <= 0f) { t.active = false; continue }
            t.y -= 0.6f * dt
        }
    }

    fun draw(c: Canvas) {
        for (p in particles) {
            if (!p.active) continue
            val a = (255 * (p.life / p.maxLife)).toInt().coerceIn(0, 255)
            paint.color = p.color
            paint.alpha = a
            c.drawCircle(p.x, p.y, p.size, paint)
        }
        textPaint.textSize = 0.34f
        for (t in texts) {
            if (!t.active) continue
            textPaint.color = t.color
            textPaint.alpha = (255 * (t.life / 1.1f).coerceIn(0f, 1f)).toInt()
            c.drawText(t.text, t.x, t.y, textPaint)
        }
    }
}
