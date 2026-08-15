package com.medieval.castledefense.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.medieval.castledefense.data.EnemyConfig
import com.medieval.castledefense.data.EnemyData
import com.medieval.castledefense.data.EnemyType
import com.medieval.castledefense.data.MovementType
import com.medieval.castledefense.data.WPoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ein Gegner auf dem Weg zur Burg. Instanzen werden gepoolt und
 * über [reset] wiederverwendet.
 */
class Enemy {

    lateinit var config: EnemyConfig
    var active = false

    var x = 0f; var y = 0f
    var hp = 0f; var maxHp = 0f
    var armor = 0f
    var goldReward = 0
    private var waypointIndex = 1
    var progress = 0f          // zurückgelegte Distanz (für Zielpriorität "vorderster Gegner")
        private set
    var reachedEnd = false
        private set

    // Statuseffekte
    private var slowTimer = 0f
    private var slowFactor = 0f
    var hitFlash = 0f
    private var walkPhase = 0f
    private var dartPhase = 0f
    var disableCooldown = 0f   // Gegner-Bogenschütze: Turm-Deaktivierung

    // Boss-Erscheinung
    var bossName: String? = null
    private var bodyColor = 0
    private var accentColor = 0

    fun reset(type: EnemyType, level: Int, waypoints: List<WPoint>, bossVariant: com.medieval.castledefense.data.BossVariant?) {
        config = EnemyData.CONFIGS.getValue(type)
        maxHp = EnemyData.scaledHealth(type, level) * (bossVariant?.healthMul ?: 1f)
        hp = maxHp
        armor = EnemyData.scaledArmor(type, level)
        goldReward = EnemyData.scaledGold(type, level)
        x = waypoints[0].x; y = waypoints[0].y
        waypointIndex = 1
        progress = 0f
        reachedEnd = false
        slowTimer = 0f; slowFactor = 0f; hitFlash = 0f
        walkPhase = 0f; dartPhase = (x * 13 + y * 7) % 6.28f
        disableCooldown = 4f
        bossName = bossVariant?.name
        bodyColor = bossVariant?.bodyColor ?: config.bodyColor
        accentColor = bossVariant?.accentColor ?: config.accentColor
        active = true
    }

    val isAlive get() = active && hp > 0f && !reachedEnd

    fun applySlow(factor: Float, duration: Float) {
        if (factor >= slowFactor) { slowFactor = factor; slowTimer = duration }
    }

    /** Liefert echten Schaden (nach Rüstung); magischer Schaden ignoriert Rüstung. */
    fun takeDamage(amount: Float, magic: Boolean): Float {
        val dealt = if (magic) amount else (amount - armor).coerceAtLeast(1f)
        hp -= dealt
        hitFlash = 0.12f
        return dealt
    }

    fun update(dt: Float, waypoints: List<WPoint>) {
        if (!active) return
        if (hitFlash > 0f) hitFlash -= dt
        if (slowTimer > 0f) { slowTimer -= dt; if (slowTimer <= 0f) slowFactor = 0f }

        val speed = config.speed * (1f - slowFactor)
        walkPhase += dt * (4f + speed * 4f)
        dartPhase += dt * 7f

        var remaining = speed * dt
        while (remaining > 0f && waypointIndex < waypoints.size) {
            val target = waypoints[waypointIndex]
            val dx = target.x - x; val dy = target.y - y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist <= remaining) {
                x = target.x; y = target.y
                progress += dist
                remaining -= dist
                waypointIndex++
            } else {
                x += dx / dist * remaining
                y += dy / dist * remaining
                progress += remaining
                remaining = 0f
            }
        }
        if (waypointIndex >= waypoints.size) reachedEnd = true
        if (disableCooldown > 0f) disableCooldown -= dt
    }

    fun distTo(px: Float, py: Float): Float {
        val dx = px - x; val dy = py - y
        return sqrt(dx * dx + dy * dy)
    }

    // ---------------- Rendering ----------------

    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private val rect = RectF()
    }

    /** Zeichnet den Gegner als klare Vektor-Silhouette mit Lauf-Animation. */
    fun draw(c: Canvas) {
        val r = config.size
        val bob = sin(walkPhase) * r * 0.15f
        // Späher weichen sichtbar seitlich aus
        val sway = if (config.movement == MovementType.DART) sin(dartPhase) * 0.12f else 0f
        val cx = x + sway
        val cy = y + bob - r * 0.3f

        // Schatten
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(70, 0, 0, 0)
        c.drawOval(RectF(cx - r, y + r * 0.55f, cx + r, y + r * 1.05f), paint)

        val body = if (hitFlash > 0f) Color.WHITE else bodyColor

        when (config.type) {
            EnemyType.BOSS -> drawBoss(c, cx, cy, r, body)
            EnemyType.SIEGE_UNIT -> drawSiege(c, cx, cy, r, body)
            else -> drawHumanoid(c, cx, cy, r, body)
        }

        // Lebensbalken
        val hpW = r * 2.4f
        rect.set(cx - hpW / 2, cy - r * 1.9f, cx + hpW / 2, cy - r * 1.65f)
        paint.color = Color.argb(190, 30, 30, 30)
        c.drawRoundRect(rect, 0.03f, 0.03f, paint)
        val frac = (hp / maxHp).coerceIn(0f, 1f)
        rect.right = rect.left + hpW * frac
        paint.color = if (frac > 0.5f) Color.rgb(90, 200, 70) else if (frac > 0.25f) Color.rgb(230, 190, 60) else Color.rgb(220, 70, 50)
        c.drawRoundRect(rect, 0.03f, 0.03f, paint)

        // Frost-Markierung
        if (slowFactor > 0f) {
            paint.color = Color.argb(120, 150, 220, 255)
            c.drawCircle(cx, cy, r * 1.25f, paint)
        }
    }

    private fun drawHumanoid(c: Canvas, cx: Float, cy: Float, r: Float, body: Int) {
        // Beine (animiert)
        paint.color = darken(body)
        val leg = sin(walkPhase) * r * 0.4f
        c.drawRect(cx - r * 0.35f, cy + r * 0.4f, cx - r * 0.1f, cy + r * 1.1f + leg * 0.3f, paint)
        c.drawRect(cx + r * 0.1f, cy + r * 0.4f, cx + r * 0.35f, cy + r * 1.1f - leg * 0.3f, paint)
        // Rumpf
        paint.color = body
        rect.set(cx - r * 0.7f, cy - r * 0.6f, cx + r * 0.7f, cy + r * 0.6f)
        c.drawRoundRect(rect, r * 0.3f, r * 0.3f, paint)
        // Kopf mit Helm/Kapuze
        paint.color = if (hitFlash > 0f) Color.WHITE else accentColor
        c.drawCircle(cx, cy - r * 0.95f, r * 0.45f, paint)

        when (config.type) {
            EnemyType.KNIGHT, EnemyType.HEAVY_KNIGHT -> {
                // Schild
                paint.color = if (hitFlash > 0f) Color.WHITE else accentColor
                rect.set(cx - r * 1.05f, cy - r * 0.5f, cx - r * 0.55f, cy + r * 0.45f)
                c.drawRoundRect(rect, r * 0.18f, r * 0.18f, paint)
                // Helmkamm
                paint.color = if (config.type == EnemyType.HEAVY_KNIGHT) Color.rgb(190, 160, 60) else Color.rgb(200, 60, 50)
                c.drawRect(cx - r * 0.08f, cy - r * 1.5f, cx + r * 0.08f, cy - r * 1.0f, paint)
            }
            EnemyType.ARCHER -> {
                // Bogen
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = r * 0.12f
                paint.color = Color.rgb(120, 80, 40)
                rect.set(cx + r * 0.45f, cy - r * 0.7f, cx + r * 1.25f, cy + r * 0.7f)
                c.drawArc(rect, -70f, 140f, false, paint)
                paint.style = Paint.Style.FILL
            }
            EnemyType.SCOUT -> {
                // Dolch
                paint.color = Color.rgb(200, 200, 210)
                c.drawRect(cx + r * 0.6f, cy - r * 0.5f, cx + r * 0.75f, cy + r * 0.2f, paint)
            }
            else -> {
                // Speer des Fußsoldaten
                paint.color = Color.rgb(150, 110, 60)
                c.drawRect(cx + r * 0.6f, cy - r * 1.4f, cx + r * 0.72f, cy + r * 0.6f, paint)
                paint.color = Color.rgb(190, 190, 200)
                path.reset()
                path.moveTo(cx + r * 0.66f, cy - r * 1.75f)
                path.lineTo(cx + r * 0.5f, cy - r * 1.35f)
                path.lineTo(cx + r * 0.82f, cy - r * 1.35f)
                path.close()
                c.drawPath(path, paint)
            }
        }
    }

    private fun drawSiege(c: Canvas, cx: Float, cy: Float, r: Float, body: Int) {
        // Rammbock auf Rädern
        paint.color = body
        rect.set(cx - r, cy - r * 0.5f, cx + r, cy + r * 0.5f)
        c.drawRoundRect(rect, r * 0.2f, r * 0.2f, paint)
        paint.color = darken(body)
        rect.set(cx - r * 0.8f, cy - r * 1.0f, cx + r * 0.8f, cy - r * 0.35f)
        c.drawRoundRect(rect, r * 0.25f, r * 0.25f, paint)
        // Räder drehen sich
        paint.color = Color.rgb(40, 32, 26)
        val w = walkPhase
        for (ox in floatArrayOf(-0.6f, 0.6f)) {
            c.drawCircle(cx + ox * r, cy + r * 0.6f, r * 0.32f, paint)
            paint.color = Color.rgb(90, 74, 56)
            c.drawLine(cx + ox * r - cos(w) * r * 0.28f, cy + r * 0.6f - sin(w) * r * 0.28f,
                cx + ox * r + cos(w) * r * 0.28f, cy + r * 0.6f + sin(w) * r * 0.28f,
                paint.apply { strokeWidth = r * 0.08f })
            paint.color = Color.rgb(40, 32, 26)
        }
        // Ramme
        paint.color = accentColor
        c.drawRect(cx - r * 1.3f, cy - r * 0.15f, cx + r * 0.2f, cy + r * 0.1f, paint)
    }

    private fun drawBoss(c: Canvas, cx: Float, cy: Float, r: Float, body: Int) {
        val stomp = (sin(walkPhase * 0.6f) * r * 0.06f)
        // massiver Körper
        paint.color = body
        rect.set(cx - r * 0.95f, cy - r * 0.9f + stomp, cx + r * 0.95f, cy + r * 0.8f)
        c.drawRoundRect(rect, r * 0.35f, r * 0.35f, paint)
        // Schulterplatten
        paint.color = darken(body)
        c.drawCircle(cx - r * 0.85f, cy - r * 0.6f + stomp, r * 0.4f, paint)
        c.drawCircle(cx + r * 0.85f, cy - r * 0.6f + stomp, r * 0.4f, paint)
        // Kopf + glühende Augen
        paint.color = body
        c.drawCircle(cx, cy - r * 1.2f + stomp, r * 0.5f, paint)
        paint.color = accentColor
        c.drawCircle(cx - r * 0.18f, cy - r * 1.25f + stomp, r * 0.09f, paint)
        c.drawCircle(cx + r * 0.18f, cy - r * 1.25f + stomp, r * 0.09f, paint)
        // Krone
        path.reset()
        val ky = cy - r * 1.62f + stomp
        path.moveTo(cx - r * 0.42f, ky + r * 0.22f)
        path.lineTo(cx - r * 0.42f, ky)
        path.lineTo(cx - r * 0.21f, ky + r * 0.14f)
        path.lineTo(cx, ky - r * 0.08f)
        path.lineTo(cx + r * 0.21f, ky + r * 0.14f)
        path.lineTo(cx + r * 0.42f, ky)
        path.lineTo(cx + r * 0.42f, ky + r * 0.22f)
        path.close()
        paint.color = Color.rgb(235, 195, 70)
        c.drawPath(path, paint)
        // riesige Waffe
        paint.color = Color.rgb(70, 70, 80)
        c.drawRect(cx + r * 1.0f, cy - r * 1.5f + stomp, cx + r * 1.18f, cy + r * 0.6f, paint)
        c.drawCircle(cx + r * 1.09f, cy - r * 1.55f + stomp, r * 0.3f, paint)
    }

    private fun darken(color: Int): Int = Color.rgb(
        (Color.red(color) * 0.65f).toInt(),
        (Color.green(color) * 0.65f).toInt(),
        (Color.blue(color) * 0.65f).toInt()
    )
}
