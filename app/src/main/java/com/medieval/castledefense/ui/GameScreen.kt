package com.medieval.castledefense.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.medieval.castledefense.data.Achievement
import com.medieval.castledefense.data.EnemyType
import com.medieval.castledefense.data.LevelData
import com.medieval.castledefense.data.TowerData
import com.medieval.castledefense.data.TowerType
import com.medieval.castledefense.game.GameWorld
import com.medieval.castledefense.game.Sfx
import com.medieval.castledefense.game.Tower
import com.medieval.castledefense.render.MapRenderer
import kotlin.math.ceil
import kotlin.math.min

/**
 * Gameplay-Bildschirm: Weltrendering, HUD, Bauen/Upgraden per Touch,
 * Wellensteuerung, Boss-Banner, Sieg-/Niederlage-Overlays.
 */
class GameScreen(app: GameApp, private val levelIndex: Int) : Screen(app), GameWorld.Listener {

    private val level = LevelData.level(levelIndex)
    private val world = GameWorld(level, this)
    private val mapRenderer = MapRenderer(level)

    // Welt-Transformation (Weltfeld ist 16 x 9 Einheiten)
    private val scale = min(app.w / 16f, app.h / 9f)
    private val offX = (app.w - 16f * scale) / 2f
    private val offY = (app.h - 9f * scale) / 2f

    private var mapBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // UI-Zustand
    private var selectedType: TowerType? = null
    private var selectedTower: Tower? = null
    private var paused = false
    private var gameSpeed = 1f
    private var bossBanner: String? = null
    private var bossBannerTimer = 0f
    private var toast: Achievement? = null
    private var toastTimer = 0f
    private var resultHandled = false

    // Sitzungstatistik (wird am Levelende in den Speicher übernommen)
    private var sessionKills = 0
    private var sessionGold = 0L

    // Layout
    private val hudH = app.h * 0.085f
    private val barH = app.h * 0.15f
    private val barRect = RectF(0f, app.h - barH, app.w, app.h)
    private val towerButtons = LinkedHashMap<TowerType, RectF>()
    private val pauseBtn = Button(RectF(app.w - app.h * 0.1f, app.h * 0.012f, app.w - app.h * 0.012f, app.h * 0.1f), "II")
    private val speedBtn = Button(RectF(app.w - app.h * 0.2f, app.h * 0.012f, app.w - app.h * 0.112f, app.h * 0.1f), "1x")
    private val startWaveBtn = Button(RectF(app.w / 2 - app.w * 0.11f, hudH * 1.9f, app.w / 2 + app.w * 0.11f, hudH * 2.9f), "JETZT STARTEN")

    // Info-Panel für ausgewählten Turm
    private val panelRect = RectF(app.w * 0.66f, app.h * 0.14f, app.w * 0.985f, app.h * 0.66f)
    private var upgradeBtn = Button(RectF(0f, 0f, 0f, 0f), "UPGRADE")
    private var pathABtn = Button(RectF(0f, 0f, 0f, 0f), "")
    private var pathBBtn = Button(RectF(0f, 0f, 0f, 0f), "")
    private var sellBtn = Button(RectF(0f, 0f, 0f, 0f), "VERKAUFEN")

    // Overlay-Buttons (Sieg / Niederlage / Pause)
    private val overlayButtons = ArrayList<Pair<Button, () -> Unit>>()

    init {
        val slotW = min(app.w * 0.135f, barH * 1.5f)
        val types = TowerType.entries
        val totalW = slotW * types.size + 8f * (types.size - 1)
        var x = (app.w - totalW) / 2
        for (t in types) {
            towerButtons[t] = RectF(x, barRect.top + barH * 0.09f, x + slotW, barRect.bottom - barH * 0.09f)
            x += slotW + 8f
        }
        layoutPanelButtons()
        app.audio.startMusic(boss = false)
    }

    private fun layoutPanelButtons() {
        val p = panelRect
        val bh = p.height() * 0.16f
        upgradeBtn = Button(RectF(p.left + 12, p.bottom - bh * 2.3f, p.right - 12, p.bottom - bh * 1.3f), "UPGRADE")
        pathABtn = Button(RectF(p.left + 12, p.bottom - bh * 3.45f, p.right - 12, p.bottom - bh * 2.45f), "")
        pathBBtn = Button(RectF(p.left + 12, p.bottom - bh * 2.3f, p.right - 12, p.bottom - bh * 1.3f), "")
        sellBtn = Button(RectF(p.left + 12, p.bottom - bh * 1.15f, p.right - 12, p.bottom - bh * 0.15f), "VERKAUFEN")
    }

    // ---------------- Update ----------------

    override fun update(dt: Float) {
        if (!paused) world.update(dt * gameSpeed)
        if (bossBannerTimer > 0f) bossBannerTimer -= dt
        if (toastTimer > 0f) {
            toastTimer -= dt
            if (toastTimer <= 0f) toast = null
        }
        if (toast == null) {
            app.achievements.pollUnlocked()?.let {
                toast = it; toastTimer = 3.2f
                app.audio.play(Sfx.ACHIEVEMENT)
            }
        }
        // Turm verkauft/entfernt? Auswahl aufräumen
        selectedTower?.let { if (it !in world.towers) selectedTower = null }
    }

    // ---------------- Rendering ----------------

    override fun draw(c: Canvas) {
        c.drawColor(Color.rgb(20, 16, 12))

        // Map einmalig als Bitmap cachen
        val bmp = mapBitmap ?: Bitmap.createBitmap(
            (16 * scale).toInt().coerceAtLeast(1), (9 * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        ).also {
            val bc = Canvas(it)
            bc.scale(scale, scale)
            mapRenderer.drawWorld(bc)
            mapBitmap = it
        }
        c.drawBitmap(bmp, offX, offY, null)

        // Welt-Entities
        c.save()
        c.translate(offX, offY)
        c.scale(scale, scale)
        drawWorldOverlays(c)
        for (t in world.towers) t.draw(c)
        world.enemies.sortedBy { it.y }.forEach { it.draw(c) }
        for (p in world.projectiles) p.draw(c)
        world.effects.draw(c)
        c.restore()

        drawHud(c)
        drawBuildBar(c)
        selectedTower?.let { drawTowerPanel(c, it) }
        if (world.phase == GameWorld.Phase.PREPARE && !paused) drawCountdown(c)
        if (bossBannerTimer > 0f) drawBossBanner(c)
        toast?.let { drawToast(c, it) }

        when {
            paused -> drawPauseOverlay(c)
            world.phase == GameWorld.Phase.VICTORY -> drawResultOverlay(c, victory = true)
            world.phase == GameWorld.Phase.DEFEAT -> drawResultOverlay(c, victory = false)
            else -> {}
        }
    }

    /** Reichweitenkreise und Bauplatz-Hervorhebung (in Weltkoordinaten). */
    private fun drawWorldOverlays(c: Canvas) {
        val sel = selectedTower
        if (sel != null) {
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(40, 255, 255, 255)
            c.drawCircle(sel.x, sel.y, sel.stats.range, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.05f
            paint.color = Color.argb(160, 255, 255, 255)
            c.drawCircle(sel.x, sel.y, sel.stats.range, paint)
        }
        if (selectedType != null) {
            // freie Bauplätze pulsierend markieren
            val pulse = (System.nanoTime() / 1e9 % 1.0).toFloat()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.06f
            paint.color = Color.argb((150 * (1f - pulse)).toInt(), 130, 230, 110)
            for ((i, s) in world.buildSpots.withIndex()) {
                if (world.towerAt(i) == null) c.drawCircle(s.x, s.y, 0.45f + pulse * 0.15f, paint)
            }
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawHud(c: Canvas) {
        // obere Leiste
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(170, 26, 20, 12)
        c.drawRoundRect(RectF(app.w * 0.012f, app.h * 0.012f, app.w * 0.42f, hudH + app.h * 0.012f), hudH * 0.3f, hudH * 0.3f, paint)

        val cy = app.h * 0.012f + hudH / 2
        val icon = hudH * 0.3f
        Ui.text.textAlign = Paint.Align.LEFT
        Ui.text.textSize = hudH * 0.52f

        Ui.drawHeart(c, app.w * 0.035f, cy, icon)
        Ui.text.color = Ui.TEXT
        c.drawText("${world.castleHp}", app.w * 0.055f, cy + hudH * 0.19f, Ui.text)

        Ui.drawCoin(c, app.w * 0.125f, cy, icon)
        Ui.text.color = Ui.GOLD
        c.drawText("${world.gold}", app.w * 0.148f, cy + hudH * 0.19f, Ui.text)

        Ui.drawSwords(c, app.w * 0.24f, cy, icon)
        Ui.text.color = Ui.TEXT
        val waveShown = (world.waveManager.currentWave + 1).coerceAtLeast(if (world.phase == GameWorld.Phase.PREPARE) world.waveManager.currentWave + 2 else 1)
            .coerceAtMost(world.waveManager.totalWaves)
        c.drawText("WELLE $waveShown/${world.waveManager.totalWaves}", app.w * 0.263f, cy + hudH * 0.19f, Ui.text)
        Ui.text.textAlign = Paint.Align.CENTER

        speedBtn.label = if (gameSpeed > 1f) "2x" else "1x"
        speedBtn.draw(c, hudH * 0.42f)
        pauseBtn.draw(c, hudH * 0.42f)
    }

    private fun drawBuildBar(c: Canvas) {
        paint.color = Color.argb(200, 30, 23, 14)
        c.drawRect(barRect, paint)
        paint.color = Ui.BORDER
        c.drawRect(barRect.left, barRect.top, barRect.right, barRect.top + 3f, paint)

        for ((type, r) in towerButtons) {
            val cfg = TowerData.CONFIGS.getValue(type)
            val unlocked = app.save.isTowerUnlocked(type)
            val affordable = world.canAfford(cfg.costs[0])
            val isSel = selectedType == type

            paint.color = when {
                !unlocked -> Color.rgb(45, 38, 30)
                isSel -> Ui.PANEL_LIGHT
                else -> Ui.PANEL
            }
            c.drawRoundRect(r, r.height() * 0.15f, r.height() * 0.15f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (isSel) 5f else 2.5f
            paint.color = if (isSel) Ui.GOLD else Ui.BORDER
            c.drawRoundRect(r, r.height() * 0.15f, r.height() * 0.15f, paint)
            paint.style = Paint.Style.FILL

            if (!unlocked) {
                Ui.drawLock(c, r.centerX(), r.centerY() - r.height() * 0.08f, r.height() * 0.16f)
                Ui.text.textSize = r.height() * 0.16f
                Ui.text.color = Ui.TEXT_DIM
                c.drawText("Level ${cfg.unlockAfterLevel}", r.centerX(), r.bottom - r.height() * 0.09f, Ui.text)
                continue
            }
            // Mini-Icon: Farbring des Turms
            paint.color = cfg.color
            c.drawCircle(r.centerX(), r.centerY() - r.height() * 0.14f, r.height() * 0.2f, paint)
            paint.color = cfg.accentColor
            c.drawCircle(r.centerX(), r.centerY() - r.height() * 0.14f, r.height() * 0.1f, paint)

            Ui.text.textSize = r.height() * 0.155f
            Ui.text.color = if (affordable) Ui.TEXT else Ui.TEXT_DIM
            c.drawText(shortName(type), r.centerX(), r.bottom - r.height() * 0.28f, Ui.text)
            Ui.text.color = if (affordable) Ui.GOLD else Ui.RED
            c.drawText("${cfg.costs[0]} G", r.centerX(), r.bottom - r.height() * 0.08f, Ui.text)
        }
        Ui.text.color = Ui.TEXT
    }

    private fun shortName(type: TowerType) = when (type) {
        TowerType.ARCHER -> "Bogen"
        TowerType.CANNON -> "Kanone"
        TowerType.CROSSBOW -> "Armbrust"
        TowerType.MAGE -> "Magier"
        TowerType.FROST -> "Frost"
        TowerType.SIEGE -> "Belager."
    }

    private fun drawTowerPanel(c: Canvas, tower: Tower) {
        Ui.drawPanel(c, panelRect, 18f)
        val p = panelRect
        val ts = p.height() * 0.075f
        Ui.text.textSize = ts * 1.15f
        Ui.text.color = Ui.GOLD
        c.drawText(tower.config.name, p.centerX(), p.top + ts * 1.7f, Ui.text)
        Ui.text.textSize = ts * 0.9f
        Ui.text.color = Ui.TEXT
        val s = tower.stats
        val pathInfo = if (tower.config.paths.size > 1 && tower.pathChosen)
            "  ·  " + tower.config.paths[tower.pathIndex].name else ""
        c.drawText("Stufe ${tower.level}/${tower.config.maxLevel}$pathInfo", p.centerX(), p.top + ts * 3.0f, Ui.text)

        Ui.text.textAlign = Paint.Align.LEFT
        var y = p.top + ts * 4.6f
        val lines = ArrayList<String>()
        lines.add("Schaden: ${s.damage.toInt()}")
        lines.add("Reichweite: ${"%.1f".format(s.range)}")
        lines.add("Angriff: ${"%.1f".format(s.fireRate)}/s")
        if (s.splashRadius > 0f) lines.add("Fläche: ${"%.1f".format(s.splashRadius)}")
        if (s.slowFactor > 0f) lines.add("Verlangsamung: ${(s.slowFactor * 100).toInt()} %")
        if (s.chainTargets > 0) lines.add("Kettenziele: ${s.chainTargets}")
        for (line in lines) {
            c.drawText(line, p.left + 16f, y, Ui.text)
            y += ts * 1.25f
        }
        Ui.text.textAlign = Paint.Align.CENTER

        val upCost = TowerData.upgradeCost(tower.type, tower.level)
        if (tower.needsPathChoice) {
            // Pfadwahl beim Upgrade auf Stufe 3
            val a = tower.config.paths[0]; val b = tower.config.paths[1]
            pathABtn.label = a.name
            pathABtn.subLabel = "$upCost Gold"
            pathABtn.enabled = upCost != null && world.canAfford(upCost)
            pathABtn.draw(c, ts * 0.85f)
            pathBBtn.label = b.name
            pathBBtn.subLabel = "$upCost Gold"
            pathBBtn.enabled = upCost != null && world.canAfford(upCost)
            pathBBtn.draw(c, ts * 0.85f)
        } else if (upCost != null) {
            upgradeBtn.label = "UPGRADE"
            upgradeBtn.subLabel = "$upCost Gold"
            upgradeBtn.enabled = world.canAfford(upCost)
            upgradeBtn.draw(c, ts * 0.95f)
        } else {
            Ui.text.color = Ui.GOLD
            c.drawText("MAXIMALE STUFE", p.centerX(), upgradeBtn.rect.centerY(), Ui.text)
            Ui.text.color = Ui.TEXT
        }
        sellBtn.subLabel = "+${tower.sellValue()} Gold"
        sellBtn.draw(c, ts * 0.95f)
    }

    private fun drawCountdown(c: Canvas) {
        val secs = ceil(world.countdown).toInt()
        Ui.text.textSize = app.h * 0.05f
        Ui.text.color = Ui.TEXT
        c.drawText("NÄCHSTE WELLE", app.w / 2, hudH * 1.2f, Ui.text)
        Ui.text.textSize = app.h * 0.075f
        Ui.text.color = Ui.GOLD
        c.drawText("$secs", app.w / 2, hudH * 1.85f, Ui.text)
        startWaveBtn.subLabel = "+${earlyBonus()} Gold"
        startWaveBtn.draw(c, app.h * 0.03f)
        Ui.text.color = Ui.TEXT
    }

    private fun earlyBonus() = (world.countdown.toInt() * 3).coerceAtLeast(0)

    private fun drawBossBanner(c: Canvas) {
        val alpha = (bossBannerTimer / 3.5f).coerceIn(0f, 1f)
        paint.color = Color.argb((alpha * 200).toInt(), 60, 10, 10)
        val r = RectF(0f, app.h * 0.36f, app.w, app.h * 0.52f)
        c.drawRect(r, paint)
        Ui.text.textSize = app.h * 0.065f
        Ui.text.color = Color.argb((alpha * 255).toInt(), 255, 90, 70)
        c.drawText("⚔ ${bossBanner ?: "BOSS"} ⚔", app.w / 2, r.centerY() + app.h * 0.022f, Ui.text)
        Ui.text.color = Ui.TEXT
    }

    private fun drawToast(c: Canvas, a: Achievement) {
        val r = RectF(app.w * 0.3f, app.h * 0.02f, app.w * 0.7f, app.h * 0.135f)
        Ui.drawPanel(c, r, 14f, Ui.PANEL_LIGHT, Ui.GOLD)
        Ui.text.textSize = r.height() * 0.36f
        Ui.text.color = Ui.GOLD
        c.drawText("Erfolg: ${a.title}", r.centerX(), r.centerY() - r.height() * 0.05f, Ui.text)
        Ui.text.textSize = r.height() * 0.27f
        Ui.text.color = Ui.TEXT
        c.drawText(a.description, r.centerX(), r.centerY() + r.height() * 0.3f, Ui.text)
    }

    // ---------------- Overlays ----------------

    private fun rebuildOverlayButtons(vararg entries: Pair<String, () -> Unit>) {
        overlayButtons.clear()
        val bw = app.w * 0.26f
        val bh = app.h * 0.1f
        var y = app.h * 0.62f
        for ((label, action) in entries) {
            overlayButtons.add(Button(RectF(app.w / 2 - bw / 2, y, app.w / 2 + bw / 2, y + bh), label) to action)
            y += bh * 1.16f
        }
    }

    private fun drawPauseOverlay(c: Canvas) {
        paint.color = Color.argb(180, 10, 8, 6)
        c.drawRect(0f, 0f, app.w, app.h, paint)
        Ui.text.textSize = app.h * 0.09f
        Ui.text.color = Ui.GOLD
        c.drawText("PAUSE", app.w / 2, app.h * 0.28f, Ui.text)
        Ui.text.textSize = app.h * 0.04f
        Ui.text.color = Ui.TEXT
        c.drawText("Level $levelIndex · ${level.theme.title} · ${level.difficultyLabel}", app.w / 2, app.h * 0.38f, Ui.text)
        rebuildOverlayButtons(
            "FORTSETZEN" to { paused = false },
            "NEUSTART" to { app.setScreen(GameScreen(app, levelIndex)) },
            "LEVELAUSWAHL" to { app.setScreen(LevelSelectScreen(app)) }
        )
        // Buttons höher platzieren
        repositionOverlayButtons(app.h * 0.45f)
        for ((b, _) in overlayButtons) b.draw(c, app.h * 0.038f)
    }

    private fun repositionOverlayButtons(startY: Float) {
        val bh = app.h * 0.1f
        var y = startY
        for ((b, _) in overlayButtons) {
            b.rect.offsetTo(app.w / 2 - b.rect.width() / 2, y)
            y += bh * 1.16f
        }
    }

    private fun drawResultOverlay(c: Canvas, victory: Boolean) {
        paint.color = Color.argb(190, 8, 6, 4)
        c.drawRect(0f, 0f, app.w, app.h, paint)

        Ui.text.textSize = app.h * 0.1f
        Ui.text.color = if (victory) Ui.GOLD else Ui.RED
        c.drawText(if (victory) "SIEG!" else "NIEDERLAGE", app.w / 2, app.h * 0.19f, Ui.text)

        // Sterne
        val stars = if (victory) world.stars() else 0
        for (i in 0 until 3) {
            Ui.drawStar(c, app.w / 2 + (i - 1) * app.h * 0.12f, app.h * 0.29f, app.h * 0.045f, i < stars)
        }

        Ui.text.textSize = app.h * 0.038f
        Ui.text.color = Ui.TEXT
        val wave = (world.waveManager.currentWave + 1).coerceAtLeast(1)
        c.drawText("Erreichte Welle: $wave/${world.waveManager.totalWaves}", app.w / 2, app.h * 0.4f, Ui.text)
        c.drawText("Besiegte Gegner: ${world.enemiesKilled}", app.w / 2, app.h * 0.46f, Ui.text)
        Ui.text.color = Ui.GOLD
        c.drawText("Verdientes Gold: ${world.goldEarned}", app.w / 2, app.h * 0.52f, Ui.text)
        Ui.text.color = Ui.TEXT

        if (victory && levelIndex < LevelData.LEVEL_COUNT) {
            rebuildOverlayButtons(
                "WEITER" to { app.setScreen(GameScreen(app, levelIndex + 1)) },
                "ERNEUT SPIELEN" to { app.setScreen(GameScreen(app, levelIndex)) },
                "LEVELAUSWAHL" to { app.setScreen(LevelSelectScreen(app)) }
            )
        } else {
            rebuildOverlayButtons(
                "ERNEUT SPIELEN" to { app.setScreen(GameScreen(app, levelIndex)) },
                "LEVELAUSWAHL" to { app.setScreen(LevelSelectScreen(app)) }
            )
        }
        repositionOverlayButtons(app.h * 0.58f)
        for ((b, _) in overlayButtons) b.draw(c, app.h * 0.036f)
    }

    // ---------------- Touch ----------------

    override fun onTouch(e: MotionEvent) {
        if (e.actionMasked != MotionEvent.ACTION_DOWN) return
        val x = e.x; val y = e.y

        // Overlays haben Vorrang
        if (paused || world.phase == GameWorld.Phase.VICTORY || world.phase == GameWorld.Phase.DEFEAT) {
            for ((b, action) in overlayButtons) {
                if (b.contains(x, y)) { app.click(); action(); return }
            }
            return
        }

        if (pauseBtn.contains(x, y)) { app.click(); paused = true; return }
        if (speedBtn.contains(x, y)) { app.click(); gameSpeed = if (gameSpeed > 1f) 1f else 2f; return }

        if (world.phase == GameWorld.Phase.PREPARE && startWaveBtn.contains(x, y)) {
            app.click()
            world.addGold(earlyBonus())
            world.startWaveEarly()
            return
        }

        // Info-Panel
        val sel = selectedTower
        if (sel != null && panelRect.contains(x, y)) {
            val upCost = TowerData.upgradeCost(sel.type, sel.level)
            if (sel.needsPathChoice) {
                if (pathABtn.contains(x, y)) { if (world.upgradeTower(sel, 0)) app.click(); return }
                if (pathBBtn.contains(x, y)) { if (world.upgradeTower(sel, 1)) app.click(); return }
            } else if (upCost != null && upgradeBtn.contains(x, y)) {
                if (world.upgradeTower(sel)) app.click()
                return
            }
            if (sellBtn.contains(x, y)) {
                app.click(); world.sellTower(sel); selectedTower = null
                return
            }
            return
        }

        // Bauleiste
        if (barRect.contains(x, y)) {
            for ((type, r) in towerButtons) {
                if (r.contains(x, y)) {
                    if (!app.save.isTowerUnlocked(type)) return
                    app.click()
                    selectedType = if (selectedType == type) null else type
                    selectedTower = null
                    return
                }
            }
            return
        }

        // Welt-Tap: Bauplatz suchen
        val wx = (x - offX) / scale
        val wy = (y - offY) / scale
        var nearestSpot = -1
        var nearestDist = 0.6f
        for ((i, s) in world.buildSpots.withIndex()) {
            val d = Math.hypot((s.x - wx).toDouble(), (s.y - wy).toDouble()).toFloat()
            if (d < nearestDist) { nearestDist = d; nearestSpot = i }
        }
        if (nearestSpot >= 0) {
            val existing = world.towerAt(nearestSpot)
            if (existing != null) {
                app.click()
                selectedTower = if (selectedTower == existing) null else existing
                selectedType = null
            } else if (selectedType != null) {
                if (!world.buildTower(nearestSpot, selectedType!!)) {
                    app.audio.play(Sfx.CLICK, 0.5f)
                }
            }
            return
        }
        // Tap ins Leere: Auswahl aufheben
        selectedTower = null
        selectedType = null
    }

    override fun onBack(): Boolean {
        when {
            paused -> paused = false
            selectedTower != null || selectedType != null -> { selectedTower = null; selectedType = null }
            else -> paused = true
        }
        return true
    }

    // ---------------- GameWorld.Listener ----------------

    override fun onSfx(sfx: Sfx) = app.audio.play(sfx)

    override fun onBossSpawned(name: String) {
        bossBanner = name
        bossBannerTimer = 3.5f
        app.audio.play(Sfx.BOSS_HORN)
        app.audio.startMusic(boss = true)
    }

    override fun onVictory(stars: Int) {
        if (resultHandled) return
        resultHandled = true
        val hpLost = world.castleHp < level.castleHp
        app.save.recordStars(levelIndex, stars)
        flushSessionStats()
        app.achievements.onLevelWon(hpLost = hpLost, wasBossLevel = level.isBossLevel)
        app.audio.play(Sfx.VICTORY)
        app.audio.startMusic(boss = false)
    }

    override fun onDefeat() {
        if (resultHandled) return
        resultHandled = true
        flushSessionStats()
        app.achievements.checkTotals()
        app.audio.play(Sfx.DEFEAT)
        app.audio.startMusic(boss = false)
    }

    private fun flushSessionStats() {
        app.save.totalKills += sessionKills
        app.save.totalGoldEarned += sessionGold
        sessionKills = 0; sessionGold = 0
    }

    override fun onEnemyKilled(type: EnemyType, gold: Int) {
        sessionKills++
        sessionGold += gold
        if (type == EnemyType.BOSS) app.save.bossesKilled += 1
    }

    override fun onTowerBuilt(type: TowerType) {
        app.save.markTowerBuilt(type)
    }

    override fun onTowerMaxLevel() {
        app.save.reachedTowerLevel5 = true
    }
}
