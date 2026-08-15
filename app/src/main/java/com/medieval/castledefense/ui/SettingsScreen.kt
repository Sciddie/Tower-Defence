package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.view.MotionEvent

class SettingsScreen(app: GameApp) : Screen(app) {

    private val backButton = Button(RectF(app.w * 0.02f, app.h * 0.03f, app.w * 0.14f, app.h * 0.13f), "ZURÜCK")
    private val musicButton: Button
    private val soundButton: Button

    init {
        val bw = app.w * 0.36f
        val bh = app.h * 0.13f
        val x = app.w / 2 - bw / 2
        musicButton = Button(RectF(x, app.h * 0.3f, x + bw, app.h * 0.3f + bh), "")
        soundButton = Button(RectF(x, app.h * 0.48f, x + bw, app.h * 0.48f + bh), "")
        refreshLabels()
    }

    private fun refreshLabels() {
        musicButton.label = "MUSIK: " + if (app.save.musicOn) "AN" else "AUS"
        soundButton.label = "SOUND: " + if (app.save.soundOn) "AN" else "AUS"
    }

    override fun draw(c: Canvas) {
        c.drawColor(Color.rgb(34, 26, 18))
        Ui.text.textSize = app.h * 0.062f
        Ui.text.color = Ui.GOLD
        c.drawText("EINSTELLUNGEN", app.w / 2, app.h * 0.115f, Ui.text)
        Ui.text.color = Ui.TEXT
        backButton.draw(c, app.h * 0.032f)
        musicButton.draw(c, app.h * 0.045f)
        soundButton.draw(c, app.h * 0.045f)

        Ui.text.textSize = app.h * 0.032f
        Ui.text.color = Ui.TEXT_DIM
        c.drawText("Fortschritt und Einstellungen werden automatisch gespeichert.", app.w / 2, app.h * 0.75f, Ui.text)
        c.drawText("Medieval Castle Defense · Version 1.0", app.w / 2, app.h * 0.82f, Ui.text)
        Ui.text.color = Ui.TEXT
    }

    override fun onTouch(e: MotionEvent) {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return
        when {
            backButton.contains(e.x, e.y) -> { app.click(); app.setScreen(MainMenuScreen(app)) }
            musicButton.contains(e.x, e.y) -> {
                app.save.musicOn = !app.save.musicOn
                app.audio.onMusicSettingChanged(boss = false)
                app.click(); refreshLabels()
            }
            soundButton.contains(e.x, e.y) -> {
                app.save.soundOn = !app.save.soundOn
                app.click(); refreshLabels()
            }
        }
    }

    override fun onBack(): Boolean {
        app.setScreen(MainMenuScreen(app)); return true
    }
}
