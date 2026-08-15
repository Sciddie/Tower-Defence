package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.medieval.castledefense.data.LevelData

/** Levelkarte: 50 Level in einem Raster, mit Sternen, Boss-Kronen und Schlössern. */
class LevelSelectScreen(app: GameApp) : Screen(app) {

    private val backButton = Button(RectF(app.w * 0.02f, app.h * 0.03f, app.w * 0.14f, app.h * 0.13f), "ZURÜCK")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val cols = 10
    private val rows = 5
    private val cells = ArrayList<RectF>()

    init {
        val gridW = app.w * 0.9f
        val gridH = app.h * 0.72f
        val x0 = (app.w - gridW) / 2
        val y0 = app.h * 0.2f
        val cw = gridW / cols
        val ch = gridH / rows
        for (i in 0 until LevelData.LEVEL_COUNT) {
            val col = i % cols
            val row = i / cols
            val pad = cw * 0.08f
            cells.add(RectF(x0 + col * cw + pad, y0 + row * ch + pad, x0 + (col + 1) * cw - pad, y0 + (row + 1) * ch - pad))
        }
    }

    override fun draw(c: Canvas) {
        c.drawColor(Color.rgb(34, 26, 18))
        Ui.text.textSize = app.h * 0.062f
        Ui.text.color = Ui.GOLD
        c.drawText("LEVELAUSWAHL", app.w / 2, app.h * 0.115f, Ui.text)
        Ui.text.color = Ui.TEXT
        backButton.draw(c, app.h * 0.032f)

        for (i in 0 until LevelData.LEVEL_COUNT) {
            val level = i + 1
            val r = cells[i]
            val unlocked = app.save.isLevelUnlocked(level)
            val stars = app.save.stars(level)
            val theme = LevelData.themeFor(level)
            val boss = LevelData.isBossLevel(level)

            val fill = if (unlocked) blend(Ui.PANEL, theme.grassColor, 0.25f) else Color.rgb(40, 34, 28)
            Ui.drawPanel(c, r, r.height() * 0.2f, fill,
                if (boss && unlocked) Ui.GOLD else Ui.BORDER, if (boss && unlocked) 4f else 2.5f)

            if (!unlocked) {
                Ui.drawLock(c, r.centerX(), r.centerY() - r.height() * 0.05f, r.height() * 0.22f)
                continue
            }
            Ui.text.textSize = r.height() * 0.38f
            c.drawText("$level", r.centerX(), r.centerY() + r.height() * 0.02f, Ui.text)
            if (boss) Ui.drawCrown(c, r.centerX(), r.top + r.height() * 0.16f, r.height() * 0.13f)
            // Sterne
            val sr = r.height() * 0.11f
            for (s in 0 until 3) {
                Ui.drawStar(c, r.centerX() + (s - 1) * sr * 2.4f, r.bottom - r.height() * 0.18f, sr, s < stars)
            }
        }

        Ui.text.textSize = app.h * 0.03f
        Ui.text.color = Ui.TEXT_DIM
        c.drawText("Schließe ein Level ab, um das nächste freizuschalten. Kronen markieren Boss-Level.",
            app.w / 2, app.h * 0.965f, Ui.text)
        Ui.text.color = Ui.TEXT
    }

    override fun onTouch(e: MotionEvent) {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return
        if (backButton.contains(e.x, e.y)) { app.click(); app.setScreen(MainMenuScreen(app)); return }
        for (i in 0 until LevelData.LEVEL_COUNT) {
            val level = i + 1
            if (cells[i].contains(e.x, e.y) && app.save.isLevelUnlocked(level)) {
                app.click()
                app.setScreen(GameScreen(app, level))
                return
            }
        }
    }

    override fun onBack(): Boolean {
        app.setScreen(MainMenuScreen(app)); return true
    }

    private fun blend(a: Int, b: Int, t: Float): Int = Color.rgb(
        (Color.red(a) * (1 - t) + Color.red(b) * t).toInt(),
        (Color.green(a) * (1 - t) + Color.green(b) * t).toInt(),
        (Color.blue(a) * (1 - t) + Color.blue(b) * t).toInt()
    )
}
