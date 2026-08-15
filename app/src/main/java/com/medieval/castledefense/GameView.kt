package com.medieval.castledefense

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.medieval.castledefense.game.AchievementManager
import com.medieval.castledefense.game.GameAudio
import com.medieval.castledefense.game.SaveManager
import com.medieval.castledefense.game.Sfx
import com.medieval.castledefense.ui.GameApp
import com.medieval.castledefense.ui.MainMenuScreen
import com.medieval.castledefense.ui.Screen

/**
 * SurfaceView mit eigenem Render-/Update-Thread (fester Timestep über
 * variable dt, gekappt gegen Frame-Sprünge). Verwaltet den aktiven Screen.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, GameApp {

    override val save = SaveManager(context)
    override val audio = GameAudio(context, save)
    override val achievements = AchievementManager(save)

    override var w = 1f; private set
    override var h = 1f; private set

    private var screen: Screen? = null
    private var thread: LoopThread? = null

    init {
        holder.addCallback(this)
        audio.initAsync()
        isFocusable = true
    }

    override fun setScreen(screen: Screen) {
        this.screen = screen
    }

    override fun click() = audio.play(Sfx.CLICK)

    fun currentScreen(): Screen? = screen

    // ---------------- Lebenszyklus ----------------

    override fun surfaceCreated(holder: SurfaceHolder) {
        w = width.toFloat(); h = height.toFloat()
        if (screen == null) {
            setScreen(MainMenuScreen(this))
            audio.startMusic(boss = false)
        }
        thread = LoopThread().also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        w = width.toFloat(); h = height.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread?.running = false
        thread?.join(500)
        thread = null
    }

    fun onPauseApp() {
        audio.stopMusic()
    }

    fun onResumeApp() {
        if (screen != null) audio.startMusic(boss = false)
    }

    fun onBackPressed(): Boolean = screen?.onBack() ?: false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        screen?.onTouch(event)
        return true
    }

    // ---------------- Game-Loop ----------------

    private inner class LoopThread : Thread("GameLoop") {
        @Volatile var running = true

        override fun run() {
            var last = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                // dt kappen: verhindert Physik-Sprünge nach Pausen
                val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                last = now

                val s = screen
                s?.update(dt)

                var canvas: Canvas? = null
                try {
                    canvas = holder.lockHardwareCanvas() ?: holder.lockCanvas()
                    if (canvas != null && s != null) s.draw(canvas)
                } finally {
                    canvas?.let { try { holder.unlockCanvasAndPost(it) } catch (_: Exception) {} }
                }

                // ~60 FPS Ziel, ohne busy-waiting
                val frameNs = System.nanoTime() - now
                val sleepMs = (16 - frameNs / 1_000_000).coerceAtLeast(0)
                if (sleepMs > 0) try { sleep(sleepMs) } catch (_: InterruptedException) {}
            }
        }
    }
}
