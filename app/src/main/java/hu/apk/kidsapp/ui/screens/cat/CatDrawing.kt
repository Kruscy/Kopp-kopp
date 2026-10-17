package hu.apk.kidsapp.ui.screens.cat

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.lighten
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

// ---- A cica "tulajdonságai" ----

enum class FurType(val label: String) {
    SILKY("Selymes"),
    FLUFFY("Bolyhos"),
    STRIPED("Csíkos"),
    SPOTTED("Foltos"),
    DOTTED("Pöttyös"),
}

enum class Extra(val emoji: String, val label: String) {
    NONE("🚫", "Semmi"),
    BOW("🎀", "Masni"),
    COLLAR("🔔", "Csengő"),
    FLOWER("🌸", "Virág"),
    CROWN("👑", "Korona"),
    GLASSES("👓", "Szemüveg"),
}

class NamedColor(val color: Color, val label: String)

val furColors = listOf(
    NamedColor(Color(0xFFF2A154), "narancs"),
    NamedColor(Color(0xFF9AA3AE), "szürke"),
    NamedColor(Color(0xFF3F3F4A), "fekete"),
    NamedColor(Color(0xFFFAF7F2), "fehér"),
    NamedColor(Color(0xFFF3DDB5), "krém"),
    NamedColor(Color(0xFF9C6B47), "barna"),
    NamedColor(Color(0xFF7F93A8), "kékes"),
    NamedColor(Color(0xFFFFA6C9), "rózsaszín"),
)

/** null = ugyanolyan színű, mint a cica bundája */
val pawColors: List<NamedColor?> = listOf(
    null,
    NamedColor(Color(0xFFFFFFFF), "fehér zokni"),
    NamedColor(Color(0xFFFFB3C7), "rózsaszín"),
    NamedColor(Color(0xFF3F3F4A), "fekete"),
    NamedColor(Color(0xFF8EC7F5), "kék"),
)

val eyeColors = listOf(
    NamedColor(Color(0xFF5CBF6B), "zöld"),
    NamedColor(Color(0xFF4A9BE8), "kék"),
    NamedColor(Color(0xFFF0B429), "borostyán"),
    NamedColor(Color(0xFF8A5A3A), "barna"),
    NamedColor(Color(0xFF9B6BE0), "lila"),
)

data class CatLook(
    val fur: Int = 0,
    val type: FurType = FurType.SILKY,
    val paw: Int = 0,
    val eye: Int = 0,
    val extra: Extra = Extra.NONE,
)

/** A játék animációs állapota (mind 0..1, kivéve ahol jelezve van). */
class CatAnim(
    val breathe: Float = 0f,
    val tail: Float = 0f, // -1..1
    val blink: Float = 0f, // 0 nyitva, 1 csukva
    val ear: Float = 0f, // 0..1
    val tilt: Float = 0f, // fok
    val hop: Float = 0f, // 0..1
    val pop: Float = 0f, // 0..1 (rugós "pattanás" változtatáskor)
)

// ---- segédek ----

private fun pseudo(i: Int): Float {
    val x = sin(i * 12.9898f) * 43758.5453f
    return x - floor(x)
}

private fun roundedTriangle(a: Offset, b: Offset, c: Offset): Path {
    fun mid(p: Offset, q: Offset) = Offset((p.x + q.x) / 2f, (p.y + q.y) / 2f)
    return Path().apply {
        val m1 = mid(a, b)
        moveTo(m1.x, m1.y)
        quadraticTo(b.x, b.y, mid(b, c).x, mid(b, c).y)
        quadraticTo(c.x, c.y, mid(c, a).x, mid(c, a).y)
        quadraticTo(a.x, a.y, m1.x, m1.y)
        close()
    }
}

private fun ovalPath(cx: Float, cy: Float, rx: Float, ry: Float) = Path().apply {
    addOval(Rect(cx - rx, cy - ry, cx + rx, cy + ry))
}

private fun luminanceIsDark(c: Color) = c.luminance() < 0.18f

/**
 * Bunda-minta egy tetszőleges területen (a `clip` alakzatra vágva). Minden pozíció a terület
 * méretéhez igazodik, így a test, a fej, a lábak és a kis előnézeti minták is ugyanazt a kódot
 * használják.
 */
fun DrawScope.drawFurPattern(type: FurType, area: Rect, clip: Path, fur: Color, s: Float, head: Boolean = false) {
    val light = fur.lighten(0.40f)
    val dark = fur.darken(0.22f)
    val stripe = if (luminanceIsDark(fur)) fur.lighten(0.22f) else fur.darken(0.28f)
    clipPath(clip) {
        when (type) {
            FurType.SILKY -> {
                // Selymes: puha, folyó szőrszálak és egy fényes, lágy csillanás.
                // Néhány hosszú, lágyan ívelő, véletlenszerűen elhelyezett szálat rajzolunk,
                // hogy ne legyen szabályos ("fakéreg") mintázat.
                val n = 9
                for (i in 0 until n) {
                    val x = area.left + area.width * (0.08f + 0.84f * pseudo(i * 5 + 1))
                    val y0 = area.top + area.height * (0.05f + 0.35f * pseudo(i * 5 + 2))
                    val len = area.height * (0.35f + 0.40f * pseudo(i * 5 + 3))
                    val sway = (pseudo(i * 5 + 4) - 0.5f) * area.width * 0.22f
                    val hair = Path().apply {
                        moveTo(x, y0)
                        quadraticTo(x + sway, y0 + len * 0.5f, x - sway * 0.5f, y0 + len)
                    }
                    drawPath(
                        hair,
                        light.copy(alpha = 0.26f),
                        style = Stroke(width = s * 0.0055f + 1f, cap = StrokeCap.Round),
                    )
                }
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.34f), Color.Transparent),
                        start = Offset(area.left, area.top + area.height * 0.15f),
                        end = Offset(area.left + area.width * 0.85f, area.top + area.height * 0.75f),
                    ),
                    topLeft = area.topLeft,
                    size = area.size,
                )
            }
            FurType.FLUFFY -> {
                // Bolyhos: sok apró, pihés szőrcsomó a felszínen.
                val n = 46
                for (i in 0 until n) {
                    val px = area.left + area.width * pseudo(i * 2 + 1)
                    val py = area.top + area.height * pseudo(i * 2 + 2)
                    val r = s * (0.014f + 0.010f * pseudo(i + 40))
                    val tuft = Path().apply {
                        moveTo(px - r, py + r * 0.6f)
                        quadraticTo(px - r * 0.2f, py - r * 1.4f, px, py - r * 1.6f)
                        quadraticTo(px + r * 0.2f, py - r * 1.2f, px + r, py + r * 0.6f)
                    }
                    drawPath(
                        tuft,
                        (if (i % 3 == 0) dark else light).copy(alpha = 0.30f),
                        style = Stroke(width = s * 0.0055f + 1f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
            FurType.STRIPED -> if (head) {
                // Fej: csíkok a homlokon és az arc szélén, az arc közepe (szem, orr) szabad marad.
                val w = s * 0.026f
                for (dx in listOf(-0.24f, 0f, 0.24f)) {
                    val x = area.left + area.width * (0.5f + dx)
                    drawLine(
                        stripe,
                        Offset(x, area.top - 2f),
                        Offset(x - dx * area.width * 0.12f, area.top + area.height * (if (dx == 0f) 0.30f else 0.24f)),
                        strokeWidth = w,
                        cap = StrokeCap.Round,
                    )
                }
                for (side in listOf(-1f, 1f)) {
                    for (k in 0..2) {
                        val y = area.top + area.height * (0.46f + 0.13f * k)
                        val x0 = if (side < 0f) area.left - 4f else area.right + 4f
                        val p = Path().apply {
                            moveTo(x0, y)
                            quadraticTo(x0 - side * area.width * 0.09f, y + area.height * 0.02f, x0 - side * area.width * 0.17f, y)
                        }
                        drawPath(p, stripe, style = Stroke(width = w, cap = StrokeCap.Round))
                    }
                }
            } else {
                val bands = 6
                for (i in 0 until bands) {
                    val y = area.top + area.height * (i + 0.5f) / bands
                    val len = area.width * (0.34f + 0.1f * pseudo(i + 7))
                    for (side in listOf(-1f, 1f)) {
                        val x0 = if (side < 0f) area.left - 4f else area.right + 4f
                        val p = Path().apply {
                            moveTo(x0, y)
                            quadraticTo(x0 - side * len * 0.5f, y + area.height * 0.03f, x0 - side * len, y - area.height * 0.02f)
                        }
                        drawPath(p, stripe, style = Stroke(width = area.height / bands * 0.46f, cap = StrokeCap.Round))
                    }
                }
            }
            FurType.SPOTTED -> {
                // Foltos (háromszínű cica): fehér és sötét foltok.
                val patchA = if (fur.luminance() > 0.8f) furColors[0].color else Color(0xFFFFFFFF)
                val patchB = if (fur.luminance() < 0.2f) Color(0xFFF2A154) else Color(0xFF3F3F4A)
                val blobs = listOf(
                    Triple(0.22f, 0.25f, 0.30f), Triple(0.78f, 0.62f, 0.34f), Triple(0.30f, 0.82f, 0.24f),
                    Triple(0.70f, 0.15f, 0.20f), Triple(0.50f, 0.50f, 0.16f),
                )
                // Keskeny részeken (láb) kevesebb és kisebb folt van, hogy ne torlódjanak.
                val narrow = area.width < area.height * 0.5f || area.height < area.width * 0.5f
                val unit = sqrt(area.width * area.height)
                blobs.take(if (narrow) 2 else blobs.size).forEachIndexed { i, (fx, fy, fr) ->
                    val c = Offset(area.left + area.width * fx, area.top + area.height * fy)
                    val rr = unit * fr * (if (narrow) 0.8f else 1f)
                    drawOval(
                        if (i % 2 == 0) patchA else patchB,
                        topLeft = Offset(c.x - rr, c.y - rr * 0.8f),
                        size = Size(rr * 2f, rr * 1.6f),
                    )
                }
            }
            FurType.DOTTED -> {
                val step = maxOf(area.width, area.height) / 6.5f
                val r = step * 0.22f
                var row = 0
                var y = area.top + step * 0.5f
                while (y < area.bottom + step) {
                    var x = area.left + (if (row % 2 == 0) step * 0.5f else step)
                    while (x < area.right + step) {
                        drawCircle(stripe, radius = r, center = Offset(x, y))
                        x += step
                    }
                    y += step * 0.85f
                    row++
                }
            }
        }
    }
}

/** Kis bolyhos szőrcsúcsok egy alakzat körvonala mentén (a test mögé rajzoljuk). */
private fun DrawScope.drawTufts(path: Path, center: Offset, fur: Color, s: Float, spacing: Float = 0.022f, length: Float = 0.028f) {
    val pm = PathMeasure()
    pm.setPath(path, false)
    val total = pm.length
    if (total <= 0f) return
    val n = (total / (s * spacing)).toInt().coerceAtLeast(8)
    for (i in 0 until n) {
        val d = total * i / n
        val pos = pm.getPosition(d)
        val tan = pm.getTangent(d)
        var nx = tan.y
        var ny = -tan.x
        if ((pos.x - center.x) * nx + (pos.y - center.y) * ny < 0f) {
            nx = -nx
            ny = -ny
        }
        val len = s * length * (0.6f + 0.8f * pseudo(i + 11))
        val half = s * 0.011f
        val tip = Offset(pos.x + nx * len + tan.x * len * 0.35f, pos.y + ny * len + tan.y * len * 0.35f)
        val tuft = Path().apply {
            moveTo(pos.x - tan.x * half, pos.y - tan.y * half)
            quadraticTo(pos.x + nx * len * 0.5f, pos.y + ny * len * 0.5f, tip.x, tip.y)
            quadraticTo(pos.x + nx * len * 0.35f + tan.x * half, pos.y + ny * len * 0.35f + tan.y * half, pos.x + tan.x * half, pos.y + tan.y * half)
            close()
        }
        drawPath(tuft, if (i % 2 == 0) fur else fur.lighten(0.10f))
    }
}

private fun DrawScope.paintPart(
    path: Path,
    area: Rect,
    look: CatLook,
    fur: Color,
    outline: Color,
    s: Float,
    center: Offset,
    pattern: Boolean = true,
) {
    if (look.type == FurType.FLUFFY) drawTufts(path, center, fur, s)
    drawPath(
        path,
        Brush.horizontalGradient(
            listOf(fur.lighten(0.10f), fur, fur.darken(0.12f)),
            startX = area.left,
            endX = area.right,
        ),
    )
    if (pattern) drawFurPattern(look.type, area, path, fur, s)
    drawPath(path, outline, style = Stroke(width = s * 0.007f, join = StrokeJoin.Round))
}

/**
 * Az egész cica. `cx` a vízszintes közép, `gy` a talaj, `s` a cica magassága (fülekkel kb. 1,05 s).
 */
fun DrawScope.drawCat(look: CatLook, cx: Float, gy: Float, s: Float, a: CatAnim) {
    val fur = furColors[look.fur].color
    val light = fur.lighten(0.40f)
    val outline = fur.darken(0.50f).copy(alpha = 0.55f)
    val pawCol = pawColors[look.paw]?.color ?: fur
    val dark = luminanceIsDark(fur)
    val sx = 1f + 0.06f * a.pop
    val sy = 1f + 0.06f * a.pop + 0.012f * a.breathe

    // Talajárnyék
    drawOval(
        Color.Black.copy(alpha = 0.16f),
        topLeft = Offset(cx - s * 0.36f, gy - s * 0.02f),
        size = Size(s * 0.72f, s * 0.09f),
    )

    withTransform({
        translate(left = 0f, top = -a.hop * s * 0.12f)
        scale(sx, sy, pivot = Offset(cx, gy))
    }) {
        val fluffy = look.type == FurType.FLUFFY

        // ---- Farok ----
        val sw = a.tail
        val tailPath = Path().apply {
            moveTo(cx + s * 0.20f, gy - s * 0.07f)
            cubicTo(
                cx + s * 0.55f, gy - s * 0.01f,
                cx + s * 0.58f + sw * s * 0.05f, gy - s * 0.40f,
                cx + s * 0.40f + sw * s * 0.11f, gy - s * 0.47f,
            )
        }
        val tailW = if (fluffy) s * 0.125f else s * 0.088f
        val pm = PathMeasure()
        pm.setPath(tailPath, false)
        val tailLen = pm.length
        if (fluffy) {
            // pihés farok: a szélén apró szőrszálak
            val n = 44
            for (i in 0 until n) {
                val d = tailLen * i / n
                val pos = pm.getPosition(d)
                val tan = pm.getTangent(d)
                for (side in listOf(-1f, 1f)) {
                    val nx = tan.y * side
                    val ny = -tan.x * side
                    val l = s * (0.012f + 0.014f * pseudo(i * 3 + (if (side < 0) 0 else 1)))
                    drawLine(
                        fur.lighten(if (i % 2 == 0) 0f else 0.12f),
                        Offset(pos.x + nx * tailW * 0.4f, pos.y + ny * tailW * 0.4f),
                        Offset(pos.x + nx * (tailW * 0.5f + l) + tan.x * l * 0.4f, pos.y + ny * (tailW * 0.5f + l) + tan.y * l * 0.4f),
                        strokeWidth = s * 0.013f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        drawPath(tailPath, outline, style = Stroke(width = tailW + s * 0.012f, cap = StrokeCap.Round))
        drawPath(tailPath, fur, style = Stroke(width = tailW, cap = StrokeCap.Round))
        drawPath(
            tailPath,
            Color.White.copy(alpha = if (look.type == FurType.SILKY) 0.30f else 0.12f),
            style = Stroke(width = tailW * 0.28f, cap = StrokeCap.Round),
        )
        when (look.type) {
            FurType.STRIPED -> {
                val rings = 7
                for (i in 1..rings) {
                    val d = tailLen * i / (rings + 1)
                    val pos = pm.getPosition(d)
                    val tan = pm.getTangent(d)
                    val nx = tan.y
                    val ny = -tan.x
                    drawLine(
                        if (dark) fur.lighten(0.22f) else fur.darken(0.28f),
                        Offset(pos.x - nx * tailW * 0.5f, pos.y - ny * tailW * 0.5f),
                        Offset(pos.x + nx * tailW * 0.5f, pos.y + ny * tailW * 0.5f),
                        strokeWidth = s * 0.026f,
                        cap = StrokeCap.Butt,
                    )
                }
            }
            FurType.DOTTED, FurType.SPOTTED -> {
                for (i in 1..5) {
                    val d = tailLen * i / 6
                    val pos = pm.getPosition(d)
                    drawCircle(
                        if (look.type == FurType.SPOTTED) (if (i % 2 == 0) Color.White else Color(0xFF3F3F4A)) else fur.darken(0.28f),
                        radius = tailW * 0.28f,
                        center = pos,
                    )
                }
            }
            else -> {}
        }

        // ---- Test ----
        val bodyPath = Path().apply {
            moveTo(cx - s * 0.15f, gy - s * 0.50f)
            cubicTo(cx - s * 0.31f, gy - s * 0.40f, cx - s * 0.35f, gy - s * 0.14f, cx - s * 0.25f, gy - s * 0.04f)
            quadraticTo(cx, gy + s * 0.012f, cx + s * 0.25f, gy - s * 0.04f)
            cubicTo(cx + s * 0.35f, gy - s * 0.14f, cx + s * 0.31f, gy - s * 0.40f, cx + s * 0.15f, gy - s * 0.50f)
            close()
        }
        val bodyArea = Rect(cx - s * 0.35f, gy - s * 0.50f, cx + s * 0.35f, gy - s * 0.02f)
        paintPart(bodyPath, bodyArea, look, fur, outline, s, Offset(cx, gy - s * 0.26f))

        // Combok (hátsó lábak)
        for (side in listOf(-1f, 1f)) {
            val haunch = ovalPath(cx + side * s * 0.20f, gy - s * 0.13f, s * 0.115f, s * 0.115f)
            val area = Rect(cx + side * s * 0.20f - s * 0.115f, gy - s * 0.245f, cx + side * s * 0.20f + s * 0.115f, gy - s * 0.015f)
            paintPart(haunch, area, look, fur, outline, s, Offset(cx + side * s * 0.20f, gy - s * 0.13f))
            // hátsó mancs
            val hp = ovalPath(cx + side * s * 0.215f, gy - s * 0.032f, s * 0.088f, s * 0.044f)
            drawPath(hp, pawCol)
            drawPath(hp, outline, style = Stroke(width = s * 0.006f))
            for (k in -1..1) {
                drawLine(
                    pawCol.darken(0.35f).copy(alpha = 0.45f),
                    Offset(cx + side * s * 0.215f + k * s * 0.028f, gy - s * 0.045f),
                    Offset(cx + side * s * 0.215f + k * s * 0.028f, gy - s * 0.022f),
                    strokeWidth = s * 0.005f + 1f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Has (világosabb folt)
        drawOval(
            Brush.radialGradient(
                listOf(light.copy(alpha = 0.95f), light.copy(alpha = 0f)),
                center = Offset(cx, gy - s * 0.24f),
                radius = s * 0.19f,
            ),
            topLeft = Offset(cx - s * 0.15f, gy - s * 0.44f),
            size = Size(s * 0.30f, s * 0.40f),
        )

        // Mellső lábak + mancsok
        for (side in listOf(-1f, 1f)) {
            val x0 = cx + side * s * 0.10f
            val leg = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        Rect(x0 - s * 0.058f, gy - s * 0.37f, x0 + s * 0.058f, gy - s * 0.04f),
                        androidx.compose.ui.geometry.CornerRadius(s * 0.058f),
                    ),
                )
            }
            val legArea = Rect(x0 - s * 0.058f, gy - s * 0.37f, x0 + s * 0.058f, gy - s * 0.04f)
            paintPart(leg, legArea, look, fur, outline, s, Offset(x0, gy - s * 0.2f))
            val paw = ovalPath(x0, gy - s * 0.04f, s * 0.072f, s * 0.044f)
            drawPath(paw, pawCol)
            drawPath(paw, outline, style = Stroke(width = s * 0.006f))
            for (k in -1..1) {
                drawLine(
                    pawCol.darken(0.35f).copy(alpha = 0.5f),
                    Offset(x0 + k * s * 0.024f, gy - s * 0.052f),
                    Offset(x0 + k * s * 0.024f, gy - s * 0.028f),
                    strokeWidth = s * 0.005f + 1f,
                    cap = StrokeCap.Round,
                )
            }
        }

        // ---- Nyaki kiegészítők (a fej alatt) ----
        val neckY = gy - s * 0.44f
        when (look.extra) {
            Extra.COLLAR -> {
                val band = Path().apply {
                    moveTo(cx - s * 0.17f, neckY - s * 0.015f)
                    quadraticTo(cx, neckY + s * 0.075f, cx + s * 0.17f, neckY - s * 0.015f)
                }
                drawPath(band, Color(0xFF2E6DB4), style = Stroke(width = s * 0.04f, cap = StrokeCap.Round))
                drawPath(band, Color(0xFFFFFFFF).copy(alpha = 0.35f), style = Stroke(width = s * 0.01f, cap = StrokeCap.Round))
                val bc = Offset(cx, neckY + s * 0.065f)
                drawCircle(Color(0xFFFFC93C), radius = s * 0.032f, center = bc)
                drawCircle(Color(0xFFB88A00), radius = s * 0.032f, center = bc, style = Stroke(width = s * 0.006f))
                drawLine(Color(0xFFB88A00), Offset(bc.x - s * 0.02f, bc.y + s * 0.006f), Offset(bc.x + s * 0.02f, bc.y + s * 0.006f), strokeWidth = s * 0.006f)
                drawCircle(Color(0xFFB88A00), radius = s * 0.007f, center = Offset(bc.x, bc.y + s * 0.02f))
            }
            Extra.BOW -> {
                val c = Offset(cx, neckY + s * 0.045f)
                val red = Color(0xFFE53950)
                for (side in listOf(-1f, 1f)) {
                    val wing = Path().apply {
                        moveTo(c.x, c.y)
                        quadraticTo(c.x + side * s * 0.09f, c.y - s * 0.09f, c.x + side * s * 0.14f, c.y - s * 0.02f)
                        quadraticTo(c.x + side * s * 0.13f, c.y + s * 0.07f, c.x, c.y)
                        close()
                    }
                    drawPath(wing, red)
                    drawPath(wing, red.darken(0.3f), style = Stroke(width = s * 0.006f, join = StrokeJoin.Round))
                }
                drawCircle(red.darken(0.12f), radius = s * 0.028f, center = c)
                drawCircle(Color.White.copy(alpha = 0.35f), radius = s * 0.009f, center = Offset(c.x - s * 0.008f, c.y - s * 0.008f))
            }
            else -> {}
        }

        // ---- Fej (kissé megdől koppintáskor) ----
        val headY = gy - s * 0.66f
        rotate(a.tilt, pivot = Offset(cx, gy - s * 0.46f)) {
            val earTw = a.ear * 7f
            val earInner = Color(0xFFFFB3BE)
            for (side in listOf(-1f, 1f)) {
                val tw = if (side < 0f) -earTw else 0f
                rotate(tw, pivot = Offset(cx + side * s * 0.17f, headY - s * 0.12f)) {
                    val p0 = Offset(cx + side * s * 0.275f, headY - s * 0.05f)
                    val p1 = Offset(cx + side * s * 0.215f, headY - s * 0.395f)
                    val p2 = Offset(cx + side * s * 0.055f, headY - s * 0.21f)
                    val ear = roundedTriangle(p0, p1, p2)
                    if (fluffy) drawTufts(ear, Offset(cx + side * s * 0.17f, headY - s * 0.2f), fur, s, 0.03f, 0.024f)
                    drawPath(ear, fur)
                    drawPath(ear, outline, style = Stroke(width = s * 0.007f, join = StrokeJoin.Round))
                    val c = Offset((p0.x + p1.x + p2.x) / 3f, (p0.y + p1.y + p2.y) / 3f + s * 0.02f)
                    fun shrink(p: Offset) = Offset(c.x + (p.x - c.x) * 0.52f, c.y + (p.y - c.y) * 0.52f)
                    drawPath(roundedTriangle(shrink(p0), shrink(p1), shrink(p2)), earInner)
                }
            }

            val headPath = ovalPath(cx, headY, s * 0.295f, s * 0.245f)
            val headArea = Rect(cx - s * 0.295f, headY - s * 0.245f, cx + s * 0.295f, headY + s * 0.245f)
            if (fluffy) {
                drawTufts(headPath, Offset(cx, headY), fur, s, 0.022f, 0.032f)
            }
            drawPath(
                headPath,
                Brush.radialGradient(
                    listOf(fur.lighten(0.14f), fur, fur.darken(0.10f)),
                    center = Offset(cx - s * 0.08f, headY - s * 0.10f),
                    radius = s * 0.40f,
                ),
            )
            drawFurPattern(look.type, headArea, headPath, fur, s, head = true)
            drawPath(headPath, outline, style = Stroke(width = s * 0.007f))

            // Pofa
            drawOval(
                Brush.radialGradient(
                    listOf(light, light.copy(alpha = 0f)),
                    center = Offset(cx, headY + s * 0.09f),
                    radius = s * 0.16f,
                ),
                topLeft = Offset(cx - s * 0.16f, headY + s * 0.0f),
                size = Size(s * 0.32f, s * 0.20f),
            )
            // Pír az arcon
            for (side in listOf(-1f, 1f)) {
                drawCircle(
                    Color(0xFFFF8FA3).copy(alpha = 0.42f),
                    radius = s * 0.04f,
                    center = Offset(cx + side * s * 0.20f, headY + s * 0.075f),
                )
            }

            // Szemek
            val eyeCol = eyeColors[look.eye].color
            for (side in listOf(-1f, 1f)) {
                val ec = Offset(cx + side * s * 0.115f, headY - s * 0.015f)
                val rx = s * 0.064f
                val ry = s * 0.072f
                val eyeShape = ovalPath(ec.x, ec.y, rx, ry)
                drawPath(eyeShape, Color.White)
                clipPath(eyeShape) {
                    drawCircle(eyeCol, radius = s * 0.058f, center = Offset(ec.x, ec.y + s * 0.005f))
                    drawCircle(eyeCol.darken(0.35f), radius = s * 0.058f, center = Offset(ec.x, ec.y + s * 0.005f), style = Stroke(width = s * 0.008f))
                    drawOval(
                        Color(0xFF1F1F2B),
                        topLeft = Offset(ec.x - s * 0.022f, ec.y - s * 0.038f),
                        size = Size(s * 0.044f, s * 0.086f),
                    )
                    drawCircle(Color.White, radius = s * 0.014f, center = Offset(ec.x - s * 0.018f, ec.y - s * 0.028f))
                    drawCircle(Color.White.copy(alpha = 0.8f), radius = s * 0.007f, center = Offset(ec.x + s * 0.02f, ec.y + s * 0.025f))
                    // pislogás: szemhéj a bunda színével
                    if (a.blink > 0.02f) {
                        drawRect(
                            fur,
                            topLeft = Offset(ec.x - rx - 2f, ec.y - ry - 2f),
                            size = Size(rx * 2f + 4f, (ry * 2f + 4f) * a.blink),
                        )
                        if (a.blink > 0.7f) {
                            drawLine(
                                outline,
                                Offset(ec.x - rx, ec.y - ry + ry * 2f * a.blink),
                                Offset(ec.x + rx, ec.y - ry + ry * 2f * a.blink),
                                strokeWidth = s * 0.008f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
                drawPath(eyeShape, outline, style = Stroke(width = s * 0.006f))
            }

            // Orr, száj
            val noseTop = headY + s * 0.04f
            val nose = roundedTriangle(
                Offset(cx - s * 0.032f, noseTop),
                Offset(cx + s * 0.032f, noseTop),
                Offset(cx, noseTop + s * 0.034f),
            )
            drawPath(nose, Color(0xFFFF7A97))
            drawPath(nose, Color(0xFFC94B6B), style = Stroke(width = s * 0.004f))
            val mouth = Path().apply {
                moveTo(cx, noseTop + s * 0.034f)
                lineTo(cx, noseTop + s * 0.052f)
                moveTo(cx, noseTop + s * 0.052f)
                quadraticTo(cx - s * 0.015f, noseTop + s * 0.085f, cx - s * 0.055f, noseTop + s * 0.062f)
                moveTo(cx, noseTop + s * 0.052f)
                quadraticTo(cx + s * 0.015f, noseTop + s * 0.085f, cx + s * 0.055f, noseTop + s * 0.062f)
            }
            drawPath(mouth, Color(0xFF6B3F3F), style = Stroke(width = s * 0.006f, cap = StrokeCap.Round))

            // Bajusz
            val whisker = if (dark) Color(0xFFF2F2F8) else Color(0xFF6B5B55)
            for (side in listOf(-1f, 1f)) {
                for (k in -1..1) {
                    drawLine(
                        whisker.copy(alpha = 0.85f),
                        Offset(cx + side * s * 0.10f, headY + s * (0.075f + k * 0.012f)),
                        Offset(cx + side * s * 0.255f, headY + s * (0.045f + k * 0.045f)),
                        strokeWidth = s * 0.004f + 1f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // Fej-kiegészítők
            when (look.extra) {
                Extra.CROWN -> {
                    val by = headY - s * 0.225f
                    val w = s * 0.17f
                    val crown = Path().apply {
                        moveTo(cx - w, by)
                        lineTo(cx - w * 1.05f, by - s * 0.10f)
                        lineTo(cx - w * 0.5f, by - s * 0.045f)
                        lineTo(cx, by - s * 0.13f)
                        lineTo(cx + w * 0.5f, by - s * 0.045f)
                        lineTo(cx + w * 1.05f, by - s * 0.10f)
                        lineTo(cx + w, by)
                        close()
                    }
                    drawPath(crown, Brush.verticalGradient(listOf(Color(0xFFFFE066), Color(0xFFF2B01E)), startY = by - s * 0.13f, endY = by))
                    drawPath(crown, Color(0xFFB88A00), style = Stroke(width = s * 0.007f, join = StrokeJoin.Round))
                    drawCircle(Color(0xFFE53950), radius = s * 0.018f, center = Offset(cx, by - s * 0.035f))
                    drawCircle(Color(0xFF4A90E2), radius = s * 0.013f, center = Offset(cx - w * 0.55f, by - s * 0.02f))
                    drawCircle(Color(0xFF6FCF97), radius = s * 0.013f, center = Offset(cx + w * 0.55f, by - s * 0.02f))
                }
                Extra.FLOWER -> {
                    val fc = Offset(cx - s * 0.185f, headY - s * 0.19f)
                    for (i in 0 until 5) {
                        val ang = Math.toRadians((i * 72 - 90).toDouble())
                        val pc = Offset(fc.x + (kotlin.math.cos(ang) * s * 0.042f).toFloat(), fc.y + (sin(ang) * s * 0.042f).toFloat())
                        drawCircle(Color(0xFFFF8FB8), radius = s * 0.036f, center = pc)
                        drawCircle(Color(0xFFD9558A), radius = s * 0.036f, center = pc, style = Stroke(width = s * 0.004f))
                    }
                    drawCircle(Color(0xFFFFD93D), radius = s * 0.026f, center = fc)
                }
                Extra.GLASSES -> {
                    val frame = Color(0xFF3B3B50)
                    val r = s * 0.093f
                    for (side in listOf(-1f, 1f)) {
                        val c = Offset(cx + side * s * 0.115f, headY - s * 0.015f)
                        drawCircle(Color(0xFFBFE3F5).copy(alpha = 0.22f), radius = r, center = c)
                        drawCircle(frame, radius = r, center = c, style = Stroke(width = s * 0.013f))
                        drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x - r * 0.55f, c.y - r * 0.45f), Offset(c.x - r * 0.2f, c.y - r * 0.75f), strokeWidth = s * 0.007f, cap = StrokeCap.Round)
                    }
                    drawLine(frame, Offset(cx - s * 0.115f + r, headY - s * 0.025f), Offset(cx + s * 0.115f - r, headY - s * 0.025f), strokeWidth = s * 0.012f)
                }
                else -> {}
            }
        }
    }
}

/** Kis cicafej színválasztó-előnézethez. */
fun DrawScope.drawMiniCatHead(center: Offset, r: Float, fur: Color) {
    val outline = fur.darken(0.5f).copy(alpha = 0.55f)
    for (side in listOf(-1f, 1f)) {
        val ear = roundedTriangle(
            Offset(center.x + side * r * 0.95f, center.y - r * 0.1f),
            Offset(center.x + side * r * 0.72f, center.y - r * 1.25f),
            Offset(center.x + side * r * 0.12f, center.y - r * 0.7f),
        )
        drawPath(ear, fur)
        drawPath(ear, outline, style = Stroke(width = r * 0.05f, join = StrokeJoin.Round))
    }
    drawCircle(fur, radius = r, center = center)
    drawCircle(outline, radius = r, center = center, style = Stroke(width = r * 0.05f))
    for (side in listOf(-1f, 1f)) {
        drawCircle(Color(0xFF2B2B3A), radius = r * 0.13f, center = Offset(center.x + side * r * 0.38f, center.y - r * 0.05f))
        drawCircle(Color.White, radius = r * 0.045f, center = Offset(center.x + side * r * 0.38f - r * 0.04f, center.y - r * 0.09f))
    }
    drawCircle(Color(0xFFFF7A97), radius = r * 0.09f, center = Offset(center.x, center.y + r * 0.22f))
}
