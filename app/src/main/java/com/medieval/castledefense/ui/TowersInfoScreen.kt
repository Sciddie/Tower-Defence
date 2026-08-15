package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.medieval.castledefense.data.TowerData
import com.medieval.castledefense.data.TowerType

/** Übersicht aller Türme mit Werten, Kosten und Freischalt-Status. */
class TowersInfoScreen(app: GameApp) : Screen(app) {

    private val backButton = Button(RectF(app.w * 0.02f, app.h * 0.03f, app.w * 0.14f, app.h * 0.13f), "ZURÜCK")
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cards = ArrayList<RectF>()

    init {
        val cols = 3
        val gridW = app.w * 0.92f
        val gridH = app.h * 0.74f
        val x0 = (app.w - gridW) / 2
        val y0 = app.h * 0.19f
        val cw = gridW / cols
        val ch = gridH / 2
        for (i in TowerType.entries.indices) {
            val col = i % cols
            val row = i / cols
            val pad = cw * 0.035f
            cards.add(RectF(x0 + col * cw + pad, y0 + row * ch + pad, x0 + (col + 1) * cw - pad, y0 + (row + 1) * ch - pad))
        }
    }

    override fun draw(c: Canvas) {
        c.drawColor(Color.rgb(34, 26, 18))
        Ui.text.textSize = app.h * 0.062f
        Ui.text.color = Ui.GOLD
        c.drawText("TÜRME", app.w / 2, app.h * 0.115f, Ui.text)
        Ui.text.color = Ui.TEXT
        backButton.draw(c, app.h * 0.032f)

        for ((i, type) in TowerType.entries.withIndex()) {
            val cfg = TowerData.CONFIGS.getValue(type)
            val r = cards[i]
            val unlocked = app.save.isTowerUnlocked(type)
            Ui.drawPanel(c, r, r.height() * 0.08f, if (unlocked) Ui.PANEL else Color.rgb(42, 36, 30))

            // Farbsymbol
            paint.style = Paint.Style.FILL
            paint.color = if (unlocked) cfg.color else Ui.DISABLED
            c.drawCircle(r.left + r.height() * 0.18f, r.top + r.height() * 0.18f, r.height() * 0.1f, paint)
            paint.color = if (unlocked) cfg.accentColor else Ui.DISABLED
            c.drawCircle(r.left + r.height() * 0.18f, r.top + r.height() * 0.18f, r.height() * 0.05f, paint)

            val ts = r.height() * 0.105f
            Ui.text.textSize = ts
            Ui.text.textAlign = Paint.Align.LEFT
            Ui.text.color = if (unlocked) Ui.GOLD else Ui.TEXT_DIM
            c.drawText(cfg.name, r.left + r.height() * 0.34f, r.top + r.height() * 0.23f, Ui.text)

            Ui.text.textSize = ts * 0.82f
            Ui.text.color = if (unlocked) Ui.TEXT else Ui.TEXT_DIM
            val s1 = cfg.stats(1, 0)
            val lines = if (unlocked) listOf(
                cfg.description,
                "Kosten: ${cfg.costs[0]} Gold",
                "Schaden: ${s1.damage.toInt()}   Reichweite: ${s1.range}",
                "Angriff: ${s1.fireRate}/s   Stufen: ${cfg.maxLevel}",
                if (cfg.paths.size > 1) "Pfade: " + cfg.paths.joinToString(" / ") { it.name } else specialText(type)
            ) else listOf(
                cfg.description,
                "Gesperrt.",
                "Schließe Level ${cfg.unlockAfterLevel} ab,",
                "um diesen Turm freizuschalten."
            )
            var y = r.top + r.height() * 0.42f
            for (line in lines) {
                c.drawText(line, r.left + r.height() * 0.1f, y, Ui.text)
                y += ts * 1.05f
            }
            Ui.text.textAlign = Paint.Align.CENTER
            if (!unlocked) Ui.drawLock(c, r.right - r.height() * 0.16f, r.top + r.height() * 0.16f, r.height() * 0.09f)
        }
        Ui.text.color = Ui.TEXT
    }

    private fun specialText(type: TowerType): String = when (type) {
        TowerType.CANNON -> "Spezial: Flächenschaden"
        TowerType.FROST -> "Spezial: Verlangsamung"
        TowerType.SIEGE -> "Spezial: Extreme Reichweite"
        TowerType.CROSSBOW -> "Spezial: Präzise Einzelziele"
        else -> ""
    }

    override fun onTouch(e: MotionEvent) {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return
        if (backButton.contains(e.x, e.y)) { app.click(); app.setScreen(MainMenuScreen(app)) }
    }

    override fun onBack(): Boolean {
        app.setScreen(MainMenuScreen(app)); return true
    }
}
