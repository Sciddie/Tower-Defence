package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import com.medieval.castledefense.data.LevelData
import kotlin.math.sin

class MainMenuScreen(app: GameApp) : Screen(app) {

    private val buttons = ArrayList<Pair<Button, () -> Unit>>()
    private var time = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var skyShader: LinearGradient? = null

    init {
        val bw = app.w * 0.3f
        val bh = app.h * 0.104f
        val x = app.w * 0.5f - bw / 2
        var y = app.h * 0.34f
        fun add(label: String, action: () -> Unit) {
            buttons.add(Button(RectF(x, y, x + bw, y + bh), label) to action)
            y += bh * 1.18f
        }
        add("SPIELEN") {
            val next = (1..LevelData.LEVEL_COUNT).firstOrNull { !app.save.isLevelCompleted(it) } ?: 1
            app.setScreen(GameScreen(app, next))
        }
        add("LEVEL") { app.setScreen(LevelSelectScreen(app)) }
        add("TÜRME") { app.setScreen(TowersInfoScreen(app)) }
        add("ERFOLGE") { app.setScreen(AchievementsScreen(app)) }
        add("EINSTELLUNGEN") { app.setScreen(SettingsScreen(app)) }
    }

    override fun update(dt: Float) { time += dt }

    override fun draw(c: Canvas) {
        val w = app.w; val h = app.h
        // Abendhimmel
        if (skyShader == null) {
            skyShader = LinearGradient(0f, 0f, 0f, h,
                Color.rgb(46, 38, 66), Color.rgb(140, 84, 60), Shader.TileMode.CLAMP)
        }
        paint.shader = skyShader
        c.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Sonne
        paint.color = Color.rgb(255, 200, 120)
        c.drawCircle(w * 0.78f, h * 0.32f + sin(time * 0.4f) * 6f, h * 0.07f, paint)

        // Hügel & Burg-Silhouette
        paint.color = Color.rgb(30, 24, 40)
        path.reset()
        path.moveTo(0f, h)
        path.lineTo(0f, h * 0.78f)
        path.quadTo(w * 0.25f, h * 0.65f, w * 0.5f, h * 0.78f)
        path.quadTo(w * 0.75f, h * 0.9f, w, h * 0.74f)
        path.lineTo(w, h)
        path.close()
        c.drawPath(path, paint)
        drawSilhouetteCastle(c, w * 0.145f, h * 0.76f, h * 0.30f)

        // Titel
        Ui.text.textSize = h * 0.085f
        Ui.text.color = Ui.GOLD
        c.drawText("MEDIEVAL", w / 2, h * 0.15f, Ui.text)
        Ui.text.textSize = h * 0.115f
        c.drawText("CASTLE DEFENSE", w / 2, h * 0.27f, Ui.text)
        Ui.text.color = Ui.TEXT

        for ((b, _) in buttons) b.draw(c, app.h * 0.045f)

        // Fortschrittszeile
        Ui.text.textSize = h * 0.032f
        Ui.text.color = Ui.TEXT_DIM
        val stars = app.save.totalStars()
        val done = app.save.completedLevelCount()
        c.drawText("$done/${LevelData.LEVEL_COUNT} Level  ·  $stars Sterne", w / 2, h * 0.965f, Ui.text)
        Ui.text.color = Ui.TEXT
    }

    private fun drawSilhouetteCastle(c: Canvas, cx: Float, baseY: Float, size: Float) {
        paint.color = Color.rgb(22, 18, 32)
        c.drawRect(cx - size * 0.5f, baseY - size * 0.75f, cx + size * 0.5f, baseY, paint)
        c.drawRect(cx - size * 0.75f, baseY - size * 1.0f, cx - size * 0.42f, baseY, paint)
        c.drawRect(cx + size * 0.42f, baseY - size * 1.0f, cx + size * 0.75f, baseY, paint)
        for (i in -2..2) {
            c.drawRect(cx + i * size * 0.2f - size * 0.06f, baseY - size * 0.88f,
                cx + i * size * 0.2f + size * 0.06f, baseY - size * 0.75f, paint)
        }
        // Fahnen wehen
        paint.color = Color.rgb(160, 50, 55)
        val flap = sin(time * 3f) * size * 0.04f
        for (ox in floatArrayOf(-0.585f, 0.585f)) {
            c.drawRect(cx + ox * size - 2f, baseY - size * 1.25f, cx + ox * size + 2f, baseY - size * 1.0f, paint)
            path.reset()
            path.moveTo(cx + ox * size + 2f, baseY - size * 1.25f)
            path.lineTo(cx + ox * size + size * 0.2f + flap, baseY - size * 1.19f)
            path.lineTo(cx + ox * size + 2f, baseY - size * 1.13f)
            path.close()
            c.drawPath(path, paint)
        }
    }

    override fun onTouch(e: MotionEvent) {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return
        for ((b, action) in buttons) {
            if (b.contains(e.x, e.y)) { app.click(); action(); return }
        }
    }
}
