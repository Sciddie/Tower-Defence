package com.medieval.castledefense.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.medieval.castledefense.data.ProjectileKind
import com.medieval.castledefense.data.TowerStats
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Gepooltes Projektil. Verfolgt sein Ziel (leichtes Homing); stirbt das Ziel,
 * fliegt es zur letzten bekannten Position weiter (Splash wirkt dort trotzdem).
 */
class Projectile {

    var active = false
    lateinit var kind: ProjectileKind
    var x = 0f; var y = 0f
    private var targetX = 0f; private var targetY = 0f
    private var speed = 10f
    var target: Enemy? = null
    lateinit var stats: TowerStats
    private var travel = 0f     // für Kanonen-Bogenflug (visuell)
    private var totalDist = 1f

    fun launch(kind: ProjectileKind, fromX: Float, fromY: Float, target: Enemy, stats: TowerStats, speed: Float) {
        this.kind = kind
        this.x = fromX; this.y = fromY
        this.target = target
        this.targetX = target.x; this.targetY = target.y
        this.stats = stats
        this.speed = speed
        this.travel = 0f
        val dx = targetX - x; val dy = targetY - y
        this.totalDist = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
        this.active = true
    }

    /** @return true, wenn das Projektil in diesem Frame eingeschlagen ist. */
    fun update(dt: Float): Boolean {
        if (!active) return false
        target?.let { if (it.isAlive) { targetX = it.x; targetY = it.y } else target = null }
        val dx = targetX - x; val dy = targetY - y
        val dist = sqrt(dx * dx + dy * dy)
        val step = speed * dt
        travel += step
        if (dist <= step + 0.05f) {
            x = targetX; y = targetY
            active = false
            return true
        }
        x += dx / dist * step
        y += dy / dist * step
        return false
    }

    // ---------------- Rendering ----------------

    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    fun draw(c: Canvas) {
        if (!active) return
        val angle = atan2(targetY - y, targetX - x)
        // Bogenflug für ballistische Geschosse
        val arc = if (kind == ProjectileKind.CANNONBALL || kind == ProjectileKind.ROCK) {
            val t = (travel / totalDist).coerceIn(0f, 1f)
            -sin(t * Math.PI).toFloat() * totalDist * 0.16f
        } else 0f
        val py = y + arc

        when (kind) {
            ProjectileKind.ARROW, ProjectileKind.BOLT -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (kind == ProjectileKind.BOLT) 0.06f else 0.04f
                paint.color = if (kind == ProjectileKind.BOLT) Color.rgb(90, 62, 34) else Color.rgb(120, 90, 50)
                val len = if (kind == ProjectileKind.BOLT) 0.3f else 0.24f
                c.drawLine(px(-len, angle), pyOf(-len, angle, py), px(0f, angle), py, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(220, 220, 230)
                c.drawCircle(x, py, 0.04f, paint)
            }
            ProjectileKind.CANNONBALL -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(35, 35, 40)
                c.drawCircle(x, py, 0.11f, paint)
                paint.color = Color.argb(120, 255, 160, 60)
                c.drawCircle(x - cos(angle) * 0.12f, py - sin(angle) * 0.12f, 0.05f, paint)
            }
            ProjectileKind.ROCK -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.rgb(105, 95, 85)
                c.drawCircle(x, py, 0.16f, paint)
                paint.color = Color.rgb(75, 68, 60)
                c.drawCircle(x + 0.05f, py - 0.04f, 0.07f, paint)
            }
            ProjectileKind.MAGIC -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(110, 168, 120, 255)
                c.drawCircle(x, py, 0.16f, paint)
                paint.color = Color.rgb(210, 180, 255)
                c.drawCircle(x, py, 0.08f, paint)
            }
            ProjectileKind.FROST -> {
                paint.style = Paint.Style.FILL
                paint.color = Color.argb(140, 180, 230, 255)
                c.drawCircle(x, py, 0.13f, paint)
                paint.color = Color.WHITE
                c.drawCircle(x, py, 0.05f, paint)
            }
        }
    }

    private fun px(offset: Float, angle: Float) = x + cos(angle) * offset
    private fun pyOf(offset: Float, angle: Float, base: Float) = base + sin(angle) * offset
}
