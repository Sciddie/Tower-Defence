package com.medieval.castledefense.game

import android.graphics.Color
import com.medieval.castledefense.data.EnemyData
import com.medieval.castledefense.data.EnemyType
import com.medieval.castledefense.data.LevelConfig
import com.medieval.castledefense.data.TowerData
import com.medieval.castledefense.data.TowerType

/**
 * Zentrale Spielsimulation eines Level-Durchlaufs.
 * Verwaltet Gegner, Türme, Projektile, Gold, Burgleben und Wellenablauf.
 * UI-/Audio-Reaktionen laufen über den [Listener] (lose Kopplung).
 */
class GameWorld(val level: LevelConfig, private val listener: Listener) {

    interface Listener {
        fun onSfx(sfx: Sfx)
        fun onBossSpawned(name: String)
        fun onVictory(stars: Int)
        fun onDefeat()
        fun onEnemyKilled(type: EnemyType, gold: Int)
        fun onTowerBuilt(type: TowerType)
        fun onTowerMaxLevel()
    }

    enum class Phase { PREPARE, WAVE, VICTORY, DEFEAT }

    var phase = Phase.PREPARE
        private set
    var gold = level.startGold
        private set
    var castleHp = level.castleHp
        private set
    var countdown = FIRST_COUNTDOWN
        private set

    // Statistiken für den Endbildschirm
    var enemiesKilled = 0; private set
    var goldEarned = 0; private set

    val waveManager = WaveManager(level)
    val towers = ArrayList<Tower>()
    val effects = Effects()

    private val enemyPool = Array(POOL_ENEMIES) { Enemy() }
    private val projectilePool = Array(POOL_PROJECTILES) { Projectile() }
    private var nextProjectile = 0

    val enemies: List<Enemy> get() = enemyPool.filter { it.active }
    val projectiles: Array<Projectile> get() = projectilePool

    val waypoints = level.layout.waypoints
    val buildSpots = level.layout.buildSpots

    companion object {
        const val FIRST_COUNTDOWN = 8f
        const val WAVE_COUNTDOWN = 5f
        const val POOL_ENEMIES = 192
        const val POOL_PROJECTILES = 160
    }

    // ---------------- Öffentliche Aktionen (Touch-UI) ----------------

    fun towerAt(spotIndex: Int): Tower? = towers.firstOrNull { it.spotIndex == spotIndex }

    fun canAfford(cost: Int) = gold >= cost

    /** Bonusgold, z. B. für frühen Wellenstart. */
    fun addGold(amount: Int) {
        gold += amount
        goldEarned += amount
    }

    fun buildTower(spotIndex: Int, type: TowerType): Boolean {
        if (phase == Phase.VICTORY || phase == Phase.DEFEAT) return false
        if (towerAt(spotIndex) != null) return false
        val cost = TowerData.CONFIGS.getValue(type).costs[0]
        if (!canAfford(cost)) return false
        gold -= cost
        val spot = buildSpots[spotIndex]
        towers.add(Tower(type, spotIndex, spot.x, spot.y))
        effects.spawnBurst(spot.x, spot.y, Color.rgb(200, 190, 160), 14, 1.6f, 0.06f)
        listener.onSfx(Sfx.BUILD)
        listener.onTowerBuilt(type)
        return true
    }

    fun upgradeTower(tower: Tower, chosenPath: Int = tower.pathIndex): Boolean {
        val cost = TowerData.upgradeCost(tower.type, tower.level) ?: return false
        if (!canAfford(cost)) return false
        gold -= cost
        tower.upgrade(chosenPath)
        effects.spawnBurst(tower.x, tower.y, Color.rgb(255, 225, 110), 18, 1.8f, 0.05f)
        listener.onSfx(Sfx.UPGRADE)
        if (tower.level >= tower.config.maxLevel) listener.onTowerMaxLevel()
        return true
    }

    fun sellTower(tower: Tower) {
        gold += tower.sellValue()
        towers.remove(tower)
        effects.spawnText(tower.x, tower.y - 0.3f, "+${tower.sellValue()} Gold", Color.rgb(255, 215, 90))
        listener.onSfx(Sfx.SELL)
    }

    /** Nächste Welle sofort starten (Countdown überspringen). */
    fun startWaveEarly() {
        if (phase == Phase.PREPARE) beginWave()
    }

    // ---------------- Simulation ----------------

    fun update(dt: Float) {
        effects.update(dt)
        when (phase) {
            Phase.PREPARE -> {
                countdown -= dt
                if (countdown <= 0f) beginWave()
                updateEntities(dt)
            }
            Phase.WAVE -> {
                waveManager.update(dt) { type -> spawnEnemy(type) }
                updateEntities(dt)
                checkWaveEnd()
            }
            else -> updateEntities(dt * 0.3f) // Endbildschirm: Szene läuft langsam weiter
        }
    }

    private fun beginWave() {
        waveManager.startNextWave()
        phase = Phase.WAVE
        listener.onSfx(Sfx.WAVE_START)
    }

    private fun spawnEnemy(type: EnemyType) {
        val enemy = enemyPool.firstOrNull { !it.active } ?: return
        val bossVariant = if (type == EnemyType.BOSS) EnemyData.BOSS_VARIANTS.getValue(level.theme) else null
        enemy.reset(type, level.index, waypoints, bossVariant)
        if (bossVariant != null) {
            listener.onBossSpawned(bossVariant.name)
            effects.spawnBurst(enemy.x, enemy.y, bossVariant.accentColor, 30, 2.2f, 0.08f, 0.8f)
        }
    }

    private fun updateEntities(dt: Float) {
        // Gegner bewegen, Durchbrüche verarbeiten
        for (e in enemyPool) {
            if (!e.active) continue
            e.update(dt, waypoints)
            if (e.reachedEnd) {
                e.active = false
                if (phase == Phase.WAVE || phase == Phase.PREPARE) damageCastle(e)
            } else if (e.config.disablesTowers && e.disableCooldown <= 0f) {
                // Gegner-Bogenschütze legt nahegelegenen Turm lahm
                val victim = towers.filter { it.disabledTimer <= 0f && e.distTo(it.x, it.y) < 2.6f }
                    .minByOrNull { e.distTo(it.x, it.y) }
                if (victim != null) {
                    victim.disabledTimer = 2.5f
                    e.disableCooldown = 6f
                    effects.spawnBurst(victim.x, victim.y, Color.rgb(200, 60, 50), 8, 1.4f, 0.05f)
                    listener.onSfx(Sfx.HIT)
                }
            }
        }

        // Türme zielen und feuern
        val alive = enemies
        for (t in towers) {
            t.update(dt)
            if (!t.canFire) continue
            val target = t.findTarget(alive) ?: continue
            fire(t, target)
        }

        // Projektile
        for (p in projectilePool) {
            if (!p.active) continue
            if (p.update(dt)) onProjectileImpact(p)
        }
    }

    private fun fire(tower: Tower, target: Enemy) {
        val p = projectilePool[nextProjectile]
        nextProjectile = (nextProjectile + 1) % projectilePool.size
        p.launch(tower.config.projectile, tower.x, tower.y, target, tower.stats, tower.config.projectileSpeed)
        tower.onFired(target.x, target.y)
        listener.onSfx(
            when (tower.type) {
                TowerType.ARCHER -> Sfx.ARROW
                TowerType.CROSSBOW -> Sfx.BOLT
                TowerType.CANNON -> Sfx.CANNON
                TowerType.MAGE -> Sfx.MAGIC
                TowerType.FROST -> Sfx.FROST
                TowerType.SIEGE -> Sfx.SIEGE
            }
        )
    }

    private fun onProjectileImpact(p: Projectile) {
        val stats = p.stats
        if (stats.splashRadius > 0f) {
            // Flächenschaden
            effects.spawnExplosion(p.x, p.y, stats.splashRadius)
            listener.onSfx(Sfx.EXPLOSION)
            for (e in enemyPool) {
                if (!e.isAlive) continue
                if (e.distTo(p.x, p.y) <= stats.splashRadius + e.config.size) {
                    e.takeDamage(stats.damage, stats.magic)
                    afterHit(e)
                }
            }
        } else {
            val primary = p.target?.takeIf { it.isAlive }
                ?: enemyPool.firstOrNull { it.isAlive && it.distTo(p.x, p.y) < 0.35f }
            if (primary != null) {
                primary.takeDamage(stats.damage, stats.magic)
                if (stats.slowFactor > 0f) {
                    primary.applySlow(stats.slowFactor, stats.slowDuration)
                    effects.spawnBurst(primary.x, primary.y, Color.rgb(170, 225, 255), 8, 1.2f, 0.05f)
                }
                listener.onSfx(Sfx.HIT)
                afterHit(primary)
                // Kettenblitz des Magiers
                if (stats.chainTargets > 0 && stats.magic) chainLightning(primary, stats.damage, stats.chainTargets)
            }
        }
    }

    private fun chainLightning(from: Enemy, damage: Float, jumps: Int) {
        var source = from
        var dmg = damage
        val hitSet = HashSet<Enemy>().apply { add(from) }
        repeat(jumps) {
            val next = enemyPool
                .filter { it.isAlive && it !in hitSet && it.distTo(source.x, source.y) < 1.5f }
                .minByOrNull { it.distTo(source.x, source.y) } ?: return
            dmg *= 0.7f
            next.takeDamage(dmg, magic = true)
            // Blitz-Visualisierung entlang der Strecke
            val steps = 5
            for (s in 0..steps) {
                val fx = source.x + (next.x - source.x) * s / steps
                val fy = source.y + (next.y - source.y) * s / steps
                effects.spawnBurst(fx, fy, Color.rgb(190, 150, 255), 2, 0.6f, 0.04f, 0.25f, 0f)
            }
            hitSet.add(next)
            afterHit(next)
            source = next
        }
    }

    private fun afterHit(e: Enemy) {
        if (e.hp > 0f || !e.active) return
        e.active = false
        enemiesKilled++
        gold += e.goldReward
        goldEarned += e.goldReward
        effects.spawnText(e.x, e.y - 0.4f, "+${e.goldReward}", Color.rgb(255, 215, 90))
        effects.spawnBurst(e.x, e.y, e.config.bodyColor, 10, 1.4f, 0.05f)
        listener.onSfx(Sfx.ENEMY_DEATH)
        listener.onSfx(Sfx.GOLD)
        listener.onEnemyKilled(e.config.type, e.goldReward)
    }

    private fun damageCastle(e: Enemy) {
        castleHp -= e.config.castleDamage
        val end = waypoints.last()
        effects.spawnBurst(end.x, end.y, Color.rgb(220, 80, 50), 16, 2f, 0.07f)
        listener.onSfx(Sfx.CASTLE_HIT)
        if (castleHp <= 0) {
            castleHp = 0
            phase = Phase.DEFEAT
            listener.onDefeat()
        }
    }

    private fun checkWaveEnd() {
        if (!waveManager.finishedSpawning) return
        if (enemyPool.any { it.active }) return
        if (waveManager.isLastWave) {
            phase = Phase.VICTORY
            listener.onVictory(stars())
        } else {
            phase = Phase.PREPARE
            countdown = WAVE_COUNTDOWN
        }
    }

    /** 3 Sterne: kaum Leben verloren, 2: einige, 1: gerade so geschafft. */
    fun stars(): Int {
        val frac = castleHp / level.castleHp.toFloat()
        return when {
            frac >= 0.9f -> 3
            frac >= 0.5f -> 2
            else -> 1
        }
    }
}
