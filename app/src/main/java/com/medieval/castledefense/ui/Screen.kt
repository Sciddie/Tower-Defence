package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.view.MotionEvent
import com.medieval.castledefense.game.AchievementManager
import com.medieval.castledefense.game.GameAudio
import com.medieval.castledefense.game.SaveManager

/** Vom GameView bereitgestellter Kontext für alle Screens. */
interface GameApp {
    val w: Float
    val h: Float
    val save: SaveManager
    val audio: GameAudio
    val achievements: AchievementManager
    fun setScreen(screen: Screen)
    fun click()
}

/** Basisklasse aller Bildschirme (Menüs und Gameplay). */
abstract class Screen(protected val app: GameApp) {
    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas)
    open fun onTouch(e: MotionEvent) {}
    /** @return true, wenn der Back-Button verarbeitet wurde. */
    open fun onBack(): Boolean = false
}
