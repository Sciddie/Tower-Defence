package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.medieval.castledefense.data.Achievement

class AchievementsScreen(app: GameApp) : Screen(app) {

    private val backButton = Button(RectF(app.w * 0.02f, app.h * 0.03f, app.w * 0.14f, app.h * 0.13f), "ZURÜCK")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(c: Canvas) {
        c.drawColor(Color.rgb(34, 26, 18))
        Ui.text.textSize = app.h * 0.062f
        Ui.text.color = Ui.GOLD
        c.drawText("ERFOLGE", app.w / 2, app.h * 0.115f, Ui.text)
        Ui.text.color = Ui.TEXT
        backButton.draw(c, app.h * 0.032f)

        val entries = Achievement.entries
        val cols = 2
        val gridW = app.w * 0.9f
        val x0 = (app.w - gridW) / 2
        val y0 = app.h * 0.19f
        val cw = gridW / cols
        val ch = app.h * 0.74f / ((entries.size + 1) / cols)

        for ((i, a) in entries.withIndex()) {
            val col = i % cols
            val row = i / cols
            val r = RectF(x0 + col * cw + 8, y0 + row * ch + 8, x0 + (col + 1) * cw - 8, y0 + (row + 1) * ch - 8)
            val done = app.save.hasAchievement(a)
            Ui.drawPanel(c, r, r.height() * 0.18f, if (done) Ui.PANEL_LIGHT else Color.rgb(42, 36, 30),
                if (done) Ui.GOLD else Ui.BORDER)

            // Medaille
            paint.style = Paint.Style.FILL
            paint.color = if (done) Ui.GOLD else Ui.DISABLED
            c.drawCircle(r.left + r.height() * 0.5f, r.centerY(), r.height() * 0.28f, paint)
            paint.color = if (done) Color.rgb(180, 140, 40) else Color.rgb(70, 64, 54)
            c.drawCircle(r.left + r.height() * 0.5f, r.centerY(), r.height() * 0.19f, paint)

            Ui.text.textAlign = Paint.Align.LEFT
            Ui.text.textSize = r.height() * 0.27f
            Ui.text.color = if (done) Ui.GOLD else Ui.TEXT_DIM
            c.drawText(a.title, r.left + r.height() * 0.95f, r.centerY() - r.height() * 0.05f, Ui.text)
            Ui.text.textSize = r.height() * 0.2f
            Ui.text.color = if (done) Ui.TEXT else Ui.TEXT_DIM
            c.drawText(a.description, r.left + r.height() * 0.95f, r.centerY() + r.height() * 0.24f, Ui.text)
            Ui.text.textAlign = Paint.Align.CENTER
        }
        Ui.text.color = Ui.TEXT
    }

    override fun onTouch(e: MotionEvent) {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return
        if (backButton.contains(e.x, e.y)) { app.click(); app.setScreen(MainMenuScreen(app)) }
    }

    override fun onBack(): Boolean {
        app.setScreen(MainMenuScreen(app)); return true
    }
}
