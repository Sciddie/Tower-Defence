package com.medieval.castledefense.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.medieval.castledefense.data.TowerConfig
import com.medieval.castledefense.data.TowerData
import com.medieval.castledefense.data.TowerStats
import com.medieval.castledefense.data.TowerType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Ein platzierter Turm auf einem Bauplatz. */
class Tower(
    val type: TowerType,
    val spotIndex: Int,
    val x: Float,
    val y: Float
) {
    val config: TowerConfig = TowerData.CONFIGS.getValue(type)
    var level = 1
        private set
    var pathIndex = 0
        private set
    /** true, sobald durch das Upgrade auf Stufe 3 ein Pfad gewählt wurde. */
    var pathChosen = false
        private set

    var cooldown = 0f
    var disabledTimer = 0f       // durch gegnerische Bogenschützen
    var aimAngle = -1.57f
    private var recoil = 0f
    private var upgradeFlash = 0f

    val stats: TowerStats get() = config.stats(level, pathIndex)

    val needsPathChoice: Boolean get() = level == 2 && config.paths.size > 1 && !pathChosen

    fun upgrade(chosenPath: Int = pathIndex) {
        if (level >= config.maxLevel) return
        if (level == 2 && config.paths.size > 1) { pathIndex = chosenPath; pathChosen = true }
        level++
        upgradeFlash = 0.6f
    }

    fun sellValue(): Int = TowerData.sellValue(type, level)

    /** Sucht das Ziel mit dem größten Wegfortschritt innerhalb der Reichweite. */
    fun findTarget(enemies: List<Enemy>): Enemy? {
        var best: Enemy? = null
        var bestProgress = -1f
        val range = stats.range
        for (e in enemies) {
            if (!e.isAlive) continue
            if (e.distTo(x, y) > range) continue
            if (e.progress > bestProgress) { bestProgress = e.progress; best = e }
        }
        return best
    }

    fun update(dt: Float) {
        if (cooldown > 0f) cooldown -= dt
        if (disabledTimer > 0f) disabledTimer -= dt
        if (recoil > 0f) recoil -= dt * 4f
        if (upgradeFlash > 0f) upgradeFlash -= dt
    }

    val canFire get() = cooldown <= 0f && disabledTimer <= 0f

    fun onFired(targetX: Float, targetY: Float) {
        cooldown = 1f / stats.fireRate
        aimAngle = atan2(targetY - y, targetX - x)
        recoil = 1f
    }

    // ---------------- Rendering ----------------

    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val path = Path()
        private val rect = RectF()
    }

    fun draw(c: Canvas) {
        val r = 0.42f
        // Sockel (Stein) – wächst leicht mit Stufe
        val baseR = r * (0.85f + level * 0.03f)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(80, 0, 0, 0)
        c.drawCircle(x + 0.04f, y + 0.06f, baseR, paint)
        paint.color = Color.rgb(122, 116, 108)
        c.drawCircle(x, y, baseR, paint)
        paint.color = Color.rgb(94, 90, 84)
        c.drawCircle(x, y, baseR * 0.8f, paint)

        val k = recoil.coerceIn(0f, 1f) * 0.06f
        val ox = x - cos(aimAngle) * k
        val oy = y - sin(aimAngle) * k

        when (type) {
            TowerType.ARCHER -> drawArcher(c, ox, oy, r)
            TowerType.CANNON -> drawCannon(c, ox, oy, r)
            TowerType.CROSSBOW -> drawCrossbow(c, ox, oy, r)
            TowerType.MAGE -> drawMage(c, ox, oy, r)
            TowerType.FROST -> drawFrost(c, ox, oy, r)
            TowerType.SIEGE -> drawSiege(c, ox, oy, r)
        }

        // Stufen-Pips
        paint.color = Color.rgb(240, 205, 90)
        for (i in 0 until level) {
            c.drawCircle(x - r * 0.6f + i * r * 0.3f, y + baseR + 0.09f, 0.045f, paint)
        }

        // Upgrade-Glanz
        if (upgradeFlash > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.05f
            paint.color = Color.argb((upgradeFlash / 0.6f * 220).toInt(), 255, 230, 120)
            c.drawCircle(x, y, baseR + (0.6f - upgradeFlash) * 0.9f, paint)
            paint.style = Paint.Style.FILL
        }

        // deaktiviert (von Gegner-Bogenschützen getroffen)
        if (disabledTimer > 0f) {
            paint.color = Color.argb(150, 60, 60, 60)
            c.drawCircle(x, y, baseR, paint)
            paint.color = Color.rgb(255, 220, 90)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 0.4f
            c.drawText("!", x, y + 0.14f, paint)
        }
    }

    private fun drawArcher(c: Canvas, ox: Float, oy: Float, r: Float) {
        // Holzplattform mit Bogenschütze
        paint.color = config.color
        rect.set(ox - r * 0.55f, oy - r * 0.55f, ox + r * 0.55f, oy + r * 0.55f)
        c.drawRoundRect(rect, 0.1f, 0.1f, paint)
        // Bogen, in Zielrichtung gedreht
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.05f
        paint.color = config.accentColor
        val deg = Math.toDegrees(aimAngle.toDouble()).toFloat()
        rect.set(ox - r * 0.42f, oy - r * 0.42f, ox + r * 0.42f, oy + r * 0.42f)
        c.drawArc(rect, deg - 55f, 110f, false, paint)
        c.drawLine(ox, oy, ox + cos(aimAngle) * r * 0.5f, oy + sin(aimAngle) * r * 0.5f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(70, 52, 30)
        c.drawCircle(ox, oy, r * 0.16f, paint)
    }

    private fun drawCannon(c: Canvas, ox: Float, oy: Float, r: Float) {
        paint.color = config.accentColor
        c.drawCircle(ox, oy, r * 0.5f, paint)
        // Rohr
        paint.color = config.color
        val len = r * 0.75f
        paint.strokeWidth = r * 0.34f
        paint.strokeCap = Paint.Cap.ROUND
        c.drawLine(ox, oy, ox + cos(aimAngle) * len, oy + sin(aimAngle) * len, paint.apply { style = Paint.Style.STROKE })
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(30, 30, 34)
        c.drawCircle(ox + cos(aimAngle) * len, oy + sin(aimAngle) * len, r * 0.15f, paint)
    }

    private fun drawCrossbow(c: Canvas, ox: Float, oy: Float, r: Float) {
        paint.color = config.color
        c.drawCircle(ox, oy, r * 0.45f, paint)
        // großes Armbrust-Kreuz
        paint.strokeWidth = 0.07f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.color = config.accentColor
        val a = aimAngle
        c.drawLine(ox - cos(a) * r * 0.3f, oy - sin(a) * r * 0.3f, ox + cos(a) * r * 0.75f, oy + sin(a) * r * 0.75f, paint)
        val perp = a + 1.5708f
        val bx = ox + cos(a) * r * 0.35f; val by = oy + sin(a) * r * 0.35f
        c.drawLine(bx - cos(perp) * r * 0.45f, by - sin(perp) * r * 0.45f, bx + cos(perp) * r * 0.45f, by + sin(perp) * r * 0.45f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawMage(c: Canvas, ox: Float, oy: Float, r: Float) {
        // Turmspitze mit schwebendem Kristall
        paint.color = config.color
        path.reset()
        path.moveTo(ox - r * 0.5f, oy + r * 0.5f)
        path.lineTo(ox, oy - r * 0.65f)
        path.lineTo(ox + r * 0.5f, oy + r * 0.5f)
        path.close()
        c.drawPath(path, paint)
        val pulse = (sin(System.nanoTime() / 3.0e8) * 0.05f).toFloat()
        paint.color = config.accentColor
        c.drawCircle(ox, oy - r * 0.75f, r * 0.2f + pulse, paint)
        paint.color = Color.argb(90, 168, 120, 255)
        c.drawCircle(ox, oy - r * 0.75f, r * 0.34f + pulse, paint)
    }

    private fun drawFrost(c: Canvas, ox: Float, oy: Float, r: Float) {
        paint.color = config.color
        c.drawCircle(ox, oy, r * 0.5f, paint)
        // Eiskristall (6 Strahlen)
        paint.strokeWidth = 0.05f
        paint.style = Paint.Style.STROKE
        paint.color = config.accentColor
        for (i in 0 until 6) {
            val a = i * 1.0472f
            c.drawLine(ox, oy, ox + cos(a) * r * 0.45f, oy + sin(a) * r * 0.45f, paint)
        }
        paint.style = Paint.Style.FILL
        c.drawCircle(ox, oy, r * 0.12f, paint)
    }

    private fun drawSiege(c: Canvas, ox: Float, oy: Float, r: Float) {
        // Trebuchet-Silhouette
        paint.color = config.color
        rect.set(ox - r * 0.6f, oy - r * 0.2f, ox + r * 0.6f, oy + r * 0.5f)
        c.drawRoundRect(rect, 0.08f, 0.08f, paint)
        paint.strokeWidth = 0.09f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(80, 56, 38)
        val armA = aimAngle - 0.6f + recoil * 0.5f
        c.drawLine(ox, oy, ox + cos(armA) * r * 0.95f, oy + sin(armA) * r * 0.95f, paint)
        paint.style = Paint.Style.FILL
        paint.color = config.accentColor
        c.drawCircle(ox + cos(armA) * r * 0.95f, oy + sin(armA) * r * 0.95f, r * 0.16f, paint)
    }
}
