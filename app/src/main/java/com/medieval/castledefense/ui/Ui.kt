package com.medieval.castledefense.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface

/** Zentrale UI-Farben und Zeichenhilfen im mittelalterlichen Stil. */
object Ui {
    val PANEL = Color.rgb(52, 38, 24)
    val PANEL_LIGHT = Color.rgb(74, 55, 34)
    val BORDER = Color.rgb(201, 162, 75)
    val TEXT = Color.rgb(240, 226, 192)
    val TEXT_DIM = Color.rgb(170, 155, 125)
    val GOLD = Color.rgb(255, 215, 90)
    val RED = Color.rgb(220, 80, 60)
    val GREEN = Color.rgb(120, 200, 90)
    val DISABLED = Color.rgb(95, 85, 70)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = TEXT
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val path = Path()

    fun drawPanel(c: Canvas, r: RectF, radius: Float, fill: Int = PANEL, border: Int = BORDER, borderW: Float = 3f) {
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(120, 0, 0, 0)
        c.drawRoundRect(RectF(r.left + 4, r.top + 6, r.right + 4, r.bottom + 6), radius, radius, paint)
        paint.color = fill
        c.drawRoundRect(r, radius, radius, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = borderW
        paint.color = border
        c.drawRoundRect(r, radius, radius, paint)
        paint.style = Paint.Style.FILL
    }

    fun drawStar(c: Canvas, cx: Float, cy: Float, r: Float, filled: Boolean) {
        path.reset()
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = Math.PI / 2 * 3 + i * Math.PI / 5
            val x = cx + (Math.cos(a) * rad).toFloat()
            val y = cy + (Math.sin(a) * rad).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = if (filled) GOLD else Color.argb(90, 0, 0, 0)
        c.drawPath(path, paint)
        if (filled) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.12f
            paint.color = Color.rgb(180, 140, 40)
            c.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }
    }

    fun drawHeart(c: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        path.moveTo(cx, cy + r * 0.9f)
        path.cubicTo(cx - r * 1.5f, cy - r * 0.2f, cx - r * 0.7f, cy - r * 1.1f, cx, cy - r * 0.3f)
        path.cubicTo(cx + r * 0.7f, cy - r * 1.1f, cx + r * 1.5f, cy - r * 0.2f, cx, cy + r * 0.9f)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = RED
        c.drawPath(path, paint)
    }

    fun drawCoin(c: Canvas, cx: Float, cy: Float, r: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(190, 145, 40)
        c.drawCircle(cx, cy, r, paint)
        paint.color = GOLD
        c.drawCircle(cx, cy, r * 0.82f, paint)
        paint.color = Color.rgb(190, 145, 40)
        text.textSize = r * 1.3f
        val old = text.color
        text.color = Color.rgb(150, 110, 30)
        c.drawText("G", cx, cy + r * 0.45f, text)
        text.color = old
    }

    fun drawSwords(c: Canvas, cx: Float, cy: Float, r: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.28f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = Color.rgb(210, 210, 220)
        c.drawLine(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f, paint)
        c.drawLine(cx + r * 0.7f, cy - r * 0.7f, cx - r * 0.7f, cy + r * 0.7f, paint)
        paint.strokeWidth = r * 0.34f
        paint.color = Color.rgb(140, 100, 50)
        c.drawLine(cx - r * 0.55f, cy + r * 0.55f, cx - r * 0.75f, cy + r * 0.75f, paint)
        c.drawLine(cx + r * 0.55f, cy + r * 0.55f, cx + r * 0.75f, cy + r * 0.75f, paint)
        paint.style = Paint.Style.FILL
    }

    fun drawLock(c: Canvas, cx: Float, cy: Float, r: Float) {
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(120, 110, 95)
        c.drawRoundRect(RectF(cx - r * 0.7f, cy - r * 0.15f, cx + r * 0.7f, cy + r * 0.85f), r * 0.2f, r * 0.2f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = r * 0.24f
        c.drawArc(RectF(cx - r * 0.45f, cy - r * 0.85f, cx + r * 0.45f, cy + r * 0.1f), 180f, 180f, false, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(60, 52, 42)
        c.drawCircle(cx, cy + r * 0.3f, r * 0.16f, paint)
    }

    fun drawCrown(c: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        path.moveTo(cx - r, cy + r * 0.5f)
        path.lineTo(cx - r, cy - r * 0.3f)
        path.lineTo(cx - r * 0.5f, cy + r * 0.05f)
        path.lineTo(cx, cy - r * 0.6f)
        path.lineTo(cx + r * 0.5f, cy + r * 0.05f)
        path.lineTo(cx + r, cy - r * 0.3f)
        path.lineTo(cx + r, cy + r * 0.5f)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = GOLD
        c.drawPath(path, paint)
    }
}

/** Rechteckiger Button mit mittelalterlichem Look. */
class Button(val rect: RectF, var label: String) {
    var enabled = true
    var highlighted = false
    var subLabel: String? = null

    fun contains(x: Float, y: Float) = enabled && rect.contains(x, y)

    fun draw(c: Canvas, textSize: Float) {
        val fill = when {
            !enabled -> Ui.DISABLED
            highlighted -> Ui.PANEL_LIGHT
            else -> Ui.PANEL
        }
        val border = if (highlighted) Ui.GOLD else Ui.BORDER
        Ui.drawPanel(c, rect, rect.height() * 0.25f, fill, border, if (highlighted) 5f else 3f)
        Ui.text.textSize = textSize
        Ui.text.color = if (enabled) Ui.TEXT else Ui.TEXT_DIM
        val sub = subLabel
        if (sub == null) {
            c.drawText(label, rect.centerX(), rect.centerY() + textSize * 0.35f, Ui.text)
        } else {
            c.drawText(label, rect.centerX(), rect.centerY() - textSize * 0.1f, Ui.text)
            Ui.text.textSize = textSize * 0.72f
            Ui.text.color = Ui.GOLD
            c.drawText(sub, rect.centerX(), rect.centerY() + textSize * 0.78f, Ui.text)
        }
    }
}
