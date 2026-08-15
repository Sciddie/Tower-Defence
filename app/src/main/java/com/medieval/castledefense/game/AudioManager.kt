package com.medieval.castledefense.game

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx {
    ARROW, BOLT, CANNON, EXPLOSION, HIT, ENEMY_DEATH, GOLD, BUILD, UPGRADE, SELL,
    CASTLE_HIT, CLICK, BOSS_HORN, FROST, MAGIC, SIEGE, VICTORY, DEFEAT, ACHIEVEMENT, WAVE_START
}

/**
 * Audio ohne Binär-Assets: Alle Soundeffekte und die Musik werden beim ersten
 * Start prozedural synthetisiert (16-bit-PCM), als WAV gecacht und über
 * SoundPool bzw. AudioTrack abgespielt.
 */
class GameAudio(private val context: Context, private val save: SaveManager) {

    companion object { const val RATE = 22050 }

    private val pool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        ).build()

    private val soundIds = HashMap<Sfx, Int>()
    @Volatile private var ready = false

    private var musicTrack: AudioTrack? = null
    private var currentMusicBoss: Boolean? = null

    fun initAsync() {
        Thread {
            val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
            for (sfx in Sfx.entries) {
                val f = File(dir, "${sfx.name}.wav")
                if (!f.exists()) writeWav(f, synth(sfx))
                soundIds[sfx] = pool.load(f.absolutePath, 1)
            }
            ready = true
        }.apply { priority = Thread.MIN_PRIORITY }.start()
    }

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!ready || !save.soundOn) return
        soundIds[sfx]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    // ---------------- Musik ----------------

    /** Startet (oder wechselt) die geloopte Hintergrundmusik. */
    fun startMusic(boss: Boolean) {
        if (!save.musicOn) { stopMusic(); return }
        if (currentMusicBoss == boss && musicTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) return
        stopMusic()
        currentMusicBoss = boss
        Thread {
            val pcm = if (boss) buildBossLoop() else buildNormalLoop()
            synchronized(this) {
                if (currentMusicBoss != boss || !save.musicOn) return@Thread
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder().setSampleRate(RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
                    )
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.size * 2)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.setLoopPoints(0, pcm.size, -1)
                track.setVolume(0.5f)
                track.play()
                musicTrack = track
            }
        }.apply { priority = Thread.MIN_PRIORITY }.start()
    }

    fun stopMusic() {
        synchronized(this) {
            currentMusicBoss = null
            musicTrack?.run { try { stop() } catch (_: Exception) {}; release() }
            musicTrack = null
        }
    }

    fun onMusicSettingChanged(boss: Boolean) {
        if (save.musicOn) { currentMusicBoss = null; startMusic(boss) } else stopMusic()
    }

    fun release() { stopMusic(); pool.release() }

    // ---------------- Synthese ----------------

    private fun env(i: Int, n: Int, attack: Float = 0.01f, decayPow: Float = 3f): Float {
        val t = i / n.toFloat()
        val a = min(1f, t / attack)
        return a * (1f - t).coerceAtLeast(0f).pow(decayPow)
    }

    private fun samples(dur: Float) = (dur * RATE).toInt()

    private fun mix(vararg parts: FloatArray): ShortArray {
        val n = parts.maxOf { it.size }
        val out = ShortArray(n)
        for (i in 0 until n) {
            var v = 0f
            for (p in parts) if (i < p.size) v += p[i]
            out[i] = (v.coerceIn(-1f, 1f) * 30000).toInt().toShort()
        }
        return out
    }

    private fun tone(freq: Float, dur: Float, vol: Float, decayPow: Float = 3f, harmonics: Int = 1, glide: Float = 0f): FloatArray {
        val n = samples(dur)
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val f = freq + glide * (i / n.toFloat())
            phase += 2.0 * PI * f / RATE
            var s = 0f
            for (h in 1..harmonics) s += (sin(phase * h).toFloat()) / h
            out[i] = s * vol * env(i, n, decayPow = decayPow)
        }
        return out
    }

    private fun noise(dur: Float, vol: Float, decayPow: Float = 3f, lowpass: Float = 1f): FloatArray {
        val n = samples(dur)
        val out = FloatArray(n)
        val rnd = Random(42)
        var last = 0f
        for (i in 0 until n) {
            val w = rnd.nextFloat() * 2f - 1f
            last += lowpass * (w - last)
            out[i] = last * vol * env(i, n, decayPow = decayPow)
        }
        return out
    }

    private fun synth(sfx: Sfx): ShortArray = when (sfx) {
        Sfx.ARROW -> mix(noise(0.12f, 0.5f, 2f, 0.6f), tone(900f, 0.1f, 0.15f, 4f, glide = -500f))
        Sfx.BOLT -> mix(noise(0.09f, 0.6f, 2f, 0.8f), tone(500f, 0.12f, 0.3f, 5f, glide = -250f))
        Sfx.CANNON -> mix(noise(0.35f, 0.9f, 4f, 0.25f), tone(80f, 0.3f, 0.6f, 4f, 3, glide = -30f))
        Sfx.EXPLOSION -> mix(noise(0.5f, 1f, 3f, 0.2f), tone(60f, 0.45f, 0.5f, 3f, 4, glide = -25f))
        Sfx.HIT -> mix(noise(0.06f, 0.5f, 4f, 0.9f), tone(300f, 0.05f, 0.3f, 5f))
        Sfx.ENEMY_DEATH -> mix(tone(280f, 0.25f, 0.4f, 3f, 2, glide = -180f), noise(0.15f, 0.3f, 3f, 0.5f))
        Sfx.GOLD -> mix(tone(1180f, 0.09f, 0.35f, 2f), tone(1560f, 0.14f, 0.3f, 2f).delayed(0.05f))
        Sfx.BUILD -> mix(noise(0.15f, 0.5f, 2f, 0.4f), tone(160f, 0.18f, 0.5f, 2f, 3), tone(220f, 0.12f, 0.3f, 2f, 2).delayed(0.1f))
        Sfx.UPGRADE -> mix(tone(440f, 0.12f, 0.35f, 2f), tone(560f, 0.12f, 0.35f, 2f).delayed(0.08f), tone(680f, 0.2f, 0.35f, 2f).delayed(0.16f))
        Sfx.SELL -> mix(tone(600f, 0.1f, 0.35f, 2f), tone(420f, 0.16f, 0.35f, 2f).delayed(0.08f))
        Sfx.CASTLE_HIT -> mix(noise(0.4f, 0.8f, 3f, 0.3f), tone(100f, 0.4f, 0.55f, 3f, 3, glide = -40f))
        Sfx.CLICK -> mix(tone(700f, 0.05f, 0.3f, 5f))
        Sfx.BOSS_HORN -> mix(
            tone(146f, 0.9f, 0.5f, 1.2f, 5), tone(148f, 0.9f, 0.4f, 1.2f, 4),
            tone(220f, 0.7f, 0.3f, 1.5f, 3).delayed(0.25f)
        )
        Sfx.FROST -> mix(tone(1500f, 0.2f, 0.2f, 3f, glide = 500f), noise(0.22f, 0.25f, 3f, 0.9f))
        Sfx.MAGIC -> mix(tone(700f, 0.25f, 0.3f, 2.5f, 2, glide = 500f), tone(1050f, 0.2f, 0.2f, 2.5f, glide = 700f).delayed(0.05f))
        Sfx.SIEGE -> mix(noise(0.3f, 0.7f, 3f, 0.3f), tone(70f, 0.35f, 0.6f, 3f, 4))
        Sfx.VICTORY -> mix(
            tone(523f, 0.22f, 0.35f, 1.6f, 2), tone(659f, 0.22f, 0.35f, 1.6f, 2).delayed(0.18f),
            tone(784f, 0.24f, 0.35f, 1.6f, 2).delayed(0.36f), tone(1046f, 0.5f, 0.4f, 1.5f, 2).delayed(0.54f)
        )
        Sfx.DEFEAT -> mix(
            tone(392f, 0.35f, 0.4f, 1.6f, 3), tone(330f, 0.35f, 0.4f, 1.6f, 3).delayed(0.3f),
            tone(262f, 0.7f, 0.45f, 1.4f, 3).delayed(0.6f)
        )
        Sfx.ACHIEVEMENT -> mix(tone(880f, 0.14f, 0.3f, 2f), tone(1174f, 0.3f, 0.35f, 2f).delayed(0.12f))
        Sfx.WAVE_START -> mix(tone(392f, 0.18f, 0.35f, 2f, 3), tone(494f, 0.28f, 0.35f, 2f, 3).delayed(0.14f))
    }

    private fun FloatArray.delayed(seconds: Float): FloatArray {
        val off = (seconds * RATE).toInt()
        val out = FloatArray(size + off)
        copyInto(out, off)
        return out
    }

    // ---------------- Musik-Loops ----------------

    /** Ruhige mittelalterliche Melodie (dorisch) über Bordun-Bass. */
    private fun buildNormalLoop(): ShortArray {
        val bpm = 92f
        val beat = 60f / bpm
        val melody = intArrayOf(0, 3, 5, 7, 5, 3, 2, 0, -2, 0, 3, 2, 0, -2, -4, 0)
        return buildLoop(melody, beat, 220f, bright = true)
    }

    /** Bedrohlicher, schnellerer Boss-Loop in Moll. */
    private fun buildBossLoop(): ShortArray {
        val bpm = 132f
        val beat = 60f / bpm
        val melody = intArrayOf(0, 0, 1, 0, -2, 0, 1, 3, 0, 0, -4, -2, 1, 0, -2, -4)
        return buildLoop(melody, beat, 174.6f, bright = false)
    }

    private fun buildLoop(melody: IntArray, beat: Float, baseFreq: Float, bright: Boolean): ShortArray {
        val minorScale = intArrayOf(0, 2, 3, 5, 7, 8, 10)
        fun noteFreq(step: Int): Float {
            var idx = step; var oct = 0
            while (idx < 0) { idx += 7; oct-- }
            while (idx >= 7) { idx -= 7; oct++ }
            return baseFreq * 2f.pow(oct + minorScale[idx] / 12f)
        }
        val total = samples(melody.size * beat)
        val out = FloatArray(total)
        // Bordun-Bass (Grundton + Quinte)
        var ph1 = 0.0; var ph2 = 0.0
        for (i in 0 until total) {
            ph1 += 2.0 * PI * (baseFreq / 2f) / RATE
            ph2 += 2.0 * PI * (baseFreq * 0.75f) / RATE
            out[i] += (sin(ph1) * 0.10 + sin(ph2) * 0.06).toFloat()
        }
        // Melodie (gezupfte Saite: Ton mit Obertönen und schnellem Abklingen)
        for ((n, step) in melody.withIndex()) {
            val f = noteFreq(step)
            val start = samples(n * beat)
            val len = samples(beat * if (bright) 0.95f else 0.9f)
            var ph = 0.0
            for (i in 0 until len) {
                if (start + i >= total) break
                ph += 2.0 * PI * f / RATE
                val e = exp(-3.0 * i / len).toFloat()
                var s = sin(ph).toFloat() * 0.16f
                s += sin(ph * 2).toFloat() * 0.07f
                if (!bright) s += sin(ph * 3).toFloat() * 0.05f
                out[start + i] += s * e
            }
        }
        // Leise Trommel auf jedem Beat (Boss: doppelt so dicht)
        val drumEvery = if (bright) 2 else 1
        for (n in melody.indices step drumEvery) {
            val start = samples(n * beat)
            val len = samples(0.09f)
            val rnd = Random(n)
            for (i in 0 until len) {
                if (start + i >= total) break
                val e = (1f - i / len.toFloat()).pow(2)
                out[start + i] += (rnd.nextFloat() * 2f - 1f) * 0.08f * e
            }
        }
        return ShortArray(total) { (out[it].coerceIn(-1f, 1f) * 26000).toInt().toShort() }
    }

    // ---------------- WAV-Datei ----------------

    private fun writeWav(file: File, pcm: ShortArray) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            val dataLen = pcm.size * 2
            val header = ByteArray(44)
            fun putInt(off: Int, v: Int) { for (i in 0..3) header[off + i] = (v shr (8 * i)).toByte() }
            fun putShort(off: Int, v: Int) { header[off] = v.toByte(); header[off + 1] = (v shr 8).toByte() }
            "RIFF".toByteArray().copyInto(header, 0)
            putInt(4, 36 + dataLen)
            "WAVE".toByteArray().copyInto(header, 8)
            "fmt ".toByteArray().copyInto(header, 12)
            putInt(16, 16); putShort(20, 1); putShort(22, 1)
            putInt(24, RATE); putInt(28, RATE * 2); putShort(32, 2); putShort(34, 16)
            "data".toByteArray().copyInto(header, 36)
            putInt(40, dataLen)
            raf.write(header)
            val bytes = ByteArray(dataLen)
            for (i in pcm.indices) {
                bytes[i * 2] = (pcm[i].toInt() and 0xFF).toByte()
                bytes[i * 2 + 1] = (pcm[i].toInt() shr 8).toByte()
            }
            raf.write(bytes)
        }
    }
}
