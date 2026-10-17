package hu.apk.kidsapp.ui.screens.car

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import hu.apk.kidsapp.util.darken
import hu.apk.kidsapp.util.lighten
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private val LIGHT_DIR = V3(-0.5f, 0.75f, 0.55f).norm()

/** Kamera: a járművet a pivot körül elforgatja (yaw), megdönti (pitch), majd vetíti. */
private class Proj(
    yawDeg: Float,
    pitchDeg: Float,
    private val pivotY: Float,
    private val unit: Float,
    private val cx: Float,
    private val cy: Float,
    private val camD: Float,
    private val lift: Float,
) {
    private val cyaw = cos(Math.toRadians(yawDeg.toDouble())).toFloat()
    private val syaw = sin(Math.toRadians(yawDeg.toDouble())).toFloat()
    private val cp = cos(Math.toRadians(pitchDeg.toDouble())).toFloat()
    private val sp = sin(Math.toRadians(pitchDeg.toDouble())).toFloat()

    fun dir(n: V3): V3 {
        val x1 = n.x * cyaw + n.z * syaw
        val z1 = -n.x * syaw + n.z * cyaw
        return V3(x1, n.y * cp - z1 * sp, n.y * sp + z1 * cp)
    }

    fun view(v: V3): V3 = dir(V3(v.x, v.y - pivotY + lift, v.z))

    fun screen(p: V3): Offset {
        val d = camD - p.z
        val k = unit * camD / max(d, 0.2f)
        return Offset(cx + p.x * k, cy - p.y * k)
    }

    fun scale(p: V3): Float = unit * camD / max(camD - p.z, 0.2f)

    val camera = V3(0f, 0f, camD)

    /** A kamera helye a jármű saját koordinátarendszerében (a rajzolási sorrendhez). */
    fun cameraModel(): V3 {
        val y1 = camD * sp
        val z1 = camD * cp
        return V3(-z1 * syaw, y1 + pivotY, z1 * cyaw)
    }
}

fun slotColor(slot: Slot, look: CarLook, lamp: Float): Color {
    val body = bodyColors[look.body].color
    return when (slot) {
        Slot.BODY -> body
        Slot.BODY_DARK -> body.darken(0.38f)
        Slot.GLASS -> Color(0xFF314A66)
        Slot.CHROME -> Color(0xFFCDD3DC)
        Slot.DARK -> Color(0xFF2A2C35)
        Slot.LAMP -> lerp(Color(0xFF8A8F99), lampColors[look.lamp].color.lighten(0.2f), lamp)
        Slot.TAIL -> Color(0xFFD62828)
        Slot.TYRE -> tyreColors[look.tyre].color
        Slot.RIM -> rimColors[look.rimColor].color
        Slot.STICKER -> stickerColors[look.stickerColor].color
        Slot.STICKER2 -> stickerSecondary(stickerColors[look.stickerColor].color, look)
        Slot.SIGN -> Color(0xFFFFB300)
    }
}

fun stickerSecondary(primary: Color, look: CarLook): Color {
    if (look.sticker == Sticker.FLAME) {
        return if (primary.luminance() > 0.55f) Color(0xFFE53935) else Color(0xFFFFE066)
    }
    return if (primary.luminance() > 0.6f) Color(0xFF2B2D36) else Color.White
}

private fun shade(c: Color, ndl: Float, spec: Boolean, extra: Float): Color {
    val k = 0.48f + 0.62f * ndl
    var out = if (k < 1f) c.darken(1f - k) else c.lighten((k - 1f) * 0.9f)
    if (extra > 0f) out = out.lighten(extra)
    if (spec && ndl > 0.88f) out = out.lighten((ndl - 0.88f) * 2.2f)
    return out
}

private class RFace(val face: Face, val z: Float, val ndl: Float, val pts: List<Offset>)

private val reusePath = Path()

private fun DrawScope.fillPoly(pts: List<Offset>, color: Color, outline: Color? = null, w: Float = 1.2f) {
    reusePath.reset()
    reusePath.moveTo(pts[0].x, pts[0].y)
    for (i in 1 until pts.size) reusePath.lineTo(pts[i].x, pts[i].y)
    reusePath.close()
    drawPath(reusePath, color)
    if (outline != null) drawPath(reusePath, outline, style = Stroke(width = w, join = StrokeJoin.Round))
}

/** A teljes jelenet: forgótányér, árnyék, fénykévék, jármű, kerekek, matrica, fénypászma. */
fun DrawScope.drawCarScene(
    model: VehicleModel,
    look: CarLook,
    yawDeg: Float,
    pitchDeg: Float,
    spinDeg: Float,
    lamp: Float,
    bounce: Float,
    popScale: Float,
    top: Float,
) {
    val fit = model.fitRadius
    val availW = size.width * 0.94f
    val availH = size.height - top - size.height * 0.04f
    // A forgótányér (1,12 x fit sugarú) és a perspektíva miatt közelebb lévő rész is elférjen.
    val unit = min(availW / (2f * fit * 1.12f * 1.22f), availH / (fit * 1.75f)) * popScale
    val cx = size.width / 2f
    val cy = top + availH * 0.46f
    val proj = Proj(yawDeg, pitchDeg, model.pivotY, unit, cx, cy, fit * 4.2f, bounce)
    val outlineCol = Color.Black.copy(alpha = 0.22f)

    // ---- forgótányér ----
    val rt = fit * 1.12f
    val n = 56
    fun ground(x: Float, z: Float, y: Float = 0f) = proj.view(V3(x, y, z))
    // oldalfal
    val thick = 0.2f
    for (i in 0 until n) {
        val a0 = 2.0 * PI * i / n
        val a1 = 2.0 * PI * (i + 1) / n
        val p = listOf(
            V3(rt * cos(a0).toFloat(), 0f, rt * sin(a0).toFloat()),
            V3(rt * cos(a1).toFloat(), 0f, rt * sin(a1).toFloat()),
            V3(rt * cos(a1).toFloat(), -thick, rt * sin(a1).toFloat()),
            V3(rt * cos(a0).toFloat(), -thick, rt * sin(a0).toFloat()),
        )
        val am = (a0 + a1) / 2.0
        val nv = proj.dir(V3(cos(am).toFloat(), 0f, sin(am).toFloat()))
        val c = proj.view(V3(rt * cos(am).toFloat(), -thick / 2f, rt * sin(am).toFloat()))
        if (nv.dot(proj.camera - c) > 0f) {
            val ndl = max(0f, nv.dot(LIGHT_DIR))
            fillPoly(p.map { proj.screen(proj.view(it)) }, shade(Color(0xFF8FA0B8), ndl, false, 0f))
        }
    }
    // felső lap
    fillPoly(
        (0 until n).map { i ->
            val a = 2.0 * PI * i / n
            proj.screen(ground(rt * cos(a).toFloat(), rt * sin(a).toFloat()))
        },
        Color(0xFFE3E9F3),
        Color(0xFFB4BFD0),
        2f,
    )
    // belső gyűrű + jelölések (a tányérral együtt forognak)
    fillPoly(
        (0 until n).map { i ->
            val a = 2.0 * PI * i / n
            proj.screen(ground(rt * 0.93f * cos(a).toFloat(), rt * 0.93f * sin(a).toFloat(), 0.004f))
        },
        Color(0xFFD3DBE9),
    )
    for (i in 0 until 24) {
        val a = 2.0 * PI * i / 24
        val r0 = rt * 0.94f
        val r1 = rt * 0.985f
        val w = 0.035
        val pa = a - w
        val pb = a + w
        fillPoly(
            listOf(
                proj.screen(ground(r0 * cos(pa).toFloat(), r0 * sin(pa).toFloat(), 0.006f)),
                proj.screen(ground(r1 * cos(pa).toFloat(), r1 * sin(pa).toFloat(), 0.006f)),
                proj.screen(ground(r1 * cos(pb).toFloat(), r1 * sin(pb).toFloat(), 0.006f)),
                proj.screen(ground(r0 * cos(pb).toFloat(), r0 * sin(pb).toFloat(), 0.006f)),
            ),
            if (i % 6 == 0) Color(0xFFFF8F00) else Color(0xFF9FB0C8),
        )
    }

    // ---- árnyék ----
    val hl = model.halfLength
    val hw = model.halfWidth
    for ((grow, alpha) in listOf(1.12f to 0.10f, 1.0f to 0.14f, 0.86f to 0.16f)) {
        fillPoly(
            listOf(
                proj.screen(ground(-hl * grow, -hw * grow, 0.008f)),
                proj.screen(ground(hl * grow, -hw * grow, 0.008f)),
                proj.screen(ground(hl * grow, hw * grow, 0.008f)),
                proj.screen(ground(-hl * grow, hw * grow, 0.008f)),
            ),
            Color.Black.copy(alpha = alpha),
        )
    }

    // ---- fénykévék a talajon ----
    if (lamp > 0.02f) {
        val lc = lampColors[look.lamp].color
        val len = fit * 1.35f
        for (l in model.lamps) {
            val x0 = l.pos.x
            val z0 = l.pos.z
            val spread = 0.10f + 0.26f * len / fit
            val p0 = proj.screen(ground(x0, z0 - 0.11f, 0.012f))
            val p1 = proj.screen(ground(x0, z0 + 0.11f, 0.012f))
            val p2 = proj.screen(ground(x0 + len, z0 + 0.11f + spread * len * 0.5f, 0.012f))
            val p3 = proj.screen(ground(x0 + len, z0 - 0.11f - spread * len * 0.5f, 0.012f))
            reusePath.reset()
            reusePath.moveTo(p0.x, p0.y)
            reusePath.lineTo(p3.x, p3.y)
            reusePath.lineTo(p2.x, p2.y)
            reusePath.lineTo(p1.x, p1.y)
            reusePath.close()
            drawPath(
                reusePath,
                Brush.linearGradient(
                    listOf(lc.copy(alpha = 0.42f * lamp), lc.copy(alpha = 0f)),
                    start = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f),
                    end = Offset((p2.x + p3.x) / 2f, (p2.y + p3.y) / 2f),
                ),
            )
        }
    }

    // ---- a jármű alkatrészei ----
    val wheelAngle = -Math.toRadians(spinDeg.toDouble()).toFloat()
    val wheelByPart = model.wheels.associateBy { it.part }
    val stickerFaces = stickerFaces(model, look)
    val faceLists = model.parts.map { part ->
        var faces = part.faces
        wheelByPart[part]?.let { faces = faces + wheelDecals(it, look, wheelAngle) }
        if (part === model.stickerHost && stickerFaces.isNotEmpty()) faces = faces + stickerFaces
        faces
    }
    val order = orderParts(model, proj, proj.cameraModel())

    fun drawFaces(faces: List<Face>) {
        val vis = ArrayList<RFace>(faces.size)
        for (f in faces) {
            val nv = proj.dir(f.normal)
            val c = proj.view(f.center)
            if (nv.dot(proj.camera - c) <= 0f) continue
            vis += RFace(f, c.z, max(0f, nv.dot(LIGHT_DIR)), f.pts.map { proj.screen(proj.view(it)) })
        }
        vis.sortWith(compareBy<RFace>({ it.face.layer }, { it.z }))
        for (r in vis) {
            val f = r.face
            val base = slotColor(f.slot, look, lamp)
            val unlit = f.unlit && (f.slot != Slot.LAMP || lamp > 0.5f)
            val spec = f.slot == Slot.BODY || f.slot == Slot.CHROME
            val color = if (unlit) base.lighten(f.light) else shade(base, r.ndl, spec, f.light)
            fillPoly(r.pts, color, if (f.layer == 0) outlineCol else null)
        }
    }

    for (i in order) drawFaces(faceLists[i])

    // ---- fény-pászma a lámpák körül ----
    if (lamp > 0.02f) {
        val lc = lampColors[look.lamp].color
        for (l in model.lamps) {
            val nv = proj.dir(l.normal)
            val c = proj.view(l.pos)
            val toCam = (proj.camera - c).norm()
            val facing = nv.dot(toCam)
            if (facing <= 0.05f) continue
            val p = proj.screen(c)
            val r = proj.scale(c) * l.radius * (0.55f + 0.7f * facing)
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.9f * facing * lamp), lc.copy(alpha = 0.55f * facing * lamp), lc.copy(alpha = 0f)),
                    center = p,
                    radius = r,
                ),
                radius = r,
                center = p,
            )
        }
    }
}

/**
 * Rajzolási sorrend (messzebbi alkatrész előbb). Minden alkatrészpárra megkeressük az őket
 * elválasztó síkot: amelyik alkatrész a kamerával azonos oldalon van, az van elöl. Ha nincs ilyen
 * sík, a középpontok mélysége dönt. A páros viszonyokból topologikus sorrend készül.
 */
private fun orderParts(model: VehicleModel, proj: Proj, cam: V3): IntArray {
    val n = model.parts.size
    val z = FloatArray(n) { proj.view(model.parts[it].centroid).z }
    val before = Array(n) { BooleanArray(n) } // before[i][j]: i-t j előtt kell rajzolni
    val indeg = IntArray(n)
    for (i in 0 until n) {
        for (j in i + 1 until n) {
            val a = model.sep[i][j]
            val b = model.sep[j][i]
            val iFirst = when {
                a != null -> a.n.dot(cam - a.p) > 0f // a kamera kívül: j van elöl, i előbb
                b != null -> b.n.dot(cam - b.p) <= 0f // a kamera kívül: i van elöl, j előbb
                else -> z[i] < z[j]
            }
            if (iFirst) {
                before[i][j] = true
                indeg[j]++
            } else {
                before[j][i] = true
                indeg[i]++
            }
        }
    }
    val done = BooleanArray(n)
    val out = IntArray(n)
    for (k in 0 until n) {
        var pick = -1
        for (i in 0 until n) {
            if (done[i] || indeg[i] != 0) continue
            if (pick < 0 || z[i] < z[pick]) pick = i
        }
        if (pick < 0) { // körkörös függés (ritka): a legtávolabbi marad előbb
            for (i in 0 until n) {
                if (!done[i] && (pick < 0 || z[i] < z[pick])) pick = i
            }
        }
        done[pick] = true
        out[k] = pick
        for (j in 0 until n) if (before[pick][j] && !done[j]) indeg[j]--
    }
    return out
}

/** A kerék felnije az aktuális forgással, mindkét oldalon. */
private fun wheelDecals(w: WheelInst, look: CarLook, angle: Float): List<Face> {
    val polys = rimPolys(look.rimStyle, angle)
    val rimR = w.radius * 0.64f
    val out = ArrayList<Face>(polys.size * 2)
    for (sgn in listOf(-1f, 1f)) {
        val normal = rotY(V3(0f, 0f, sgn), w.yawDeg)
        for (p in polys) {
            val slot: Slot
            val layer: Int
            var light = 0f
            when (p.kind) {
                RimKind.BACK_DARK -> { slot = Slot.DARK; layer = 1 }
                RimKind.BACK_RIM -> { slot = Slot.RIM; layer = 1 }
                RimKind.SPOKE -> { slot = Slot.RIM; layer = 2 }
                RimKind.HOLE -> { slot = Slot.DARK; layer = 3 }
                RimKind.HUB -> { slot = Slot.RIM; layer = 4; light = 0.28f }
            }
            val pts = p.pts.map {
                val lx = (if (sgn < 0f) -it.x else it.x) * rimR
                val ly = it.y * rimR
                w.center + rotY(V3(lx, ly, sgn * (w.hw + 0.012f)), w.yawDeg)
            }
            out += Face(pts, normal, slot, layer, false, light)
        }
    }
    return out
}

/** A matrica az autó két oldalán (a kijelölt síkon). */
private fun stickerFaces(model: VehicleModel, look: CarLook): List<Face> {
    if (look.sticker == Sticker.NONE) return emptyList()
    val polys = stickerPolys(look.sticker)
    val a = model.sticker
    val wMul = stickerWidthMul(look.sticker)
    val out = ArrayList<Face>(polys.size * 2)
    for (sgn in listOf(-1f, 1f)) {
        val normal = V3(0f, 0f, sgn)
        for (p in polys) {
            val pts = p.pts.map {
                // a -z oldalon tükrözünk, hogy kívülről nézve is jó irányú legyen
                val u = (if (sgn < 0f) -it.x else it.x) * a.half * wMul
                V3(a.cx + u, a.cy + it.y * a.half, sgn * a.zOut)
            }
            out += Face(pts, normal, if (p.second) Slot.STICKER2 else Slot.STICKER, 1, false, 0f)
        }
    }
    return out
}

/** Kis, oldalról nézett jármű-ikon az előnézethez. */
fun DrawScope.drawVehicleIcon(type: VehicleType, color: Color) {
    val w = size.width
    val h = size.height
    val dark = Color(0xFF2A2C35)
    val glass = Color(0xFF9BB8D6)
    fun wheel(cx: Float, cy: Float, r: Float) {
        drawCircle(dark, radius = r, center = Offset(cx, cy))
        drawCircle(Color(0xFFD0D5DD), radius = r * 0.5f, center = Offset(cx, cy))
    }
    when (type) {
        VehicleType.SPORTS -> {
            drawRoundRect(color, Offset(w * 0.06f, h * 0.50f), Size(w * 0.88f, h * 0.2f), androidx.compose.ui.geometry.CornerRadius(h * 0.08f))
            val cab = Path().apply {
                moveTo(w * 0.28f, h * 0.5f); lineTo(w * 0.40f, h * 0.32f); lineTo(w * 0.62f, h * 0.32f); lineTo(w * 0.76f, h * 0.5f); close()
            }
            drawPath(cab, color)
            val win = Path().apply {
                moveTo(w * 0.33f, h * 0.48f); lineTo(w * 0.42f, h * 0.35f); lineTo(w * 0.60f, h * 0.35f); lineTo(w * 0.70f, h * 0.48f); close()
            }
            drawPath(win, glass)
            wheel(w * 0.27f, h * 0.7f, h * 0.11f)
            wheel(w * 0.75f, h * 0.7f, h * 0.11f)
        }
        VehicleType.OFFROAD -> {
            drawRoundRect(color, Offset(w * 0.08f, h * 0.38f), Size(w * 0.84f, h * 0.28f), androidx.compose.ui.geometry.CornerRadius(h * 0.05f))
            drawRoundRect(color, Offset(w * 0.2f, h * 0.18f), Size(w * 0.5f, h * 0.24f), androidx.compose.ui.geometry.CornerRadius(h * 0.04f))
            drawRect(glass, Offset(w * 0.25f, h * 0.22f), Size(w * 0.18f, h * 0.15f))
            drawRect(glass, Offset(w * 0.47f, h * 0.22f), Size(w * 0.18f, h * 0.15f))
            drawRect(dark, Offset(w * 0.22f, h * 0.14f), Size(w * 0.44f, h * 0.03f))
            wheel(w * 0.28f, h * 0.7f, h * 0.15f)
            wheel(w * 0.72f, h * 0.7f, h * 0.15f)
        }
        VehicleType.TRUCK -> {
            drawRoundRect(color, Offset(w * 0.05f, h * 0.22f), Size(w * 0.58f, h * 0.44f), androidx.compose.ui.geometry.CornerRadius(h * 0.04f))
            drawRoundRect(color.darken(0.18f), Offset(w * 0.66f, h * 0.34f), Size(w * 0.29f, h * 0.32f), androidx.compose.ui.geometry.CornerRadius(h * 0.05f))
            drawRect(glass, Offset(w * 0.72f, h * 0.38f), Size(w * 0.18f, h * 0.12f))
            drawRect(dark, Offset(w * 0.05f, h * 0.64f), Size(w * 0.9f, h * 0.05f))
            wheel(w * 0.2f, h * 0.72f, h * 0.1f)
            wheel(w * 0.42f, h * 0.72f, h * 0.1f)
            wheel(w * 0.8f, h * 0.72f, h * 0.1f)
        }
        VehicleType.BUS -> {
            drawRoundRect(color, Offset(w * 0.04f, h * 0.22f), Size(w * 0.92f, h * 0.46f), androidx.compose.ui.geometry.CornerRadius(h * 0.07f))
            for (i in 0 until 5) drawRect(glass, Offset(w * (0.1f + i * 0.145f), h * 0.3f), Size(w * 0.105f, h * 0.15f))
            drawRect(color.darken(0.3f), Offset(w * 0.04f, h * 0.52f), Size(w * 0.92f, h * 0.04f))
            wheel(w * 0.25f, h * 0.72f, h * 0.1f)
            wheel(w * 0.75f, h * 0.72f, h * 0.1f)
        }
    }
}

/** Felni-ikon (előnézethez): gumi + felni minta. */
fun DrawScope.drawWheelIcon(tyre: Color, style: RimStyle, rim: Color) {
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = min(size.width, size.height) * 0.46f
    drawCircle(tyre, radius = r, center = c)
    drawCircle(tyre.darken(0.35f), radius = r, center = c, style = Stroke(width = 2f))
    val rimR = r * 0.7f
    val dark = Color(0xFF23252E)
    for (p in rimPolys(style, 0f)) {
        val color = when (p.kind) {
            RimKind.BACK_DARK, RimKind.HOLE -> dark
            RimKind.BACK_RIM, RimKind.SPOKE -> rim
            RimKind.HUB -> rim.lighten(0.28f)
        }
        fillPoly(p.pts.map { Offset(c.x + it.x * rimR, c.y + it.y * rimR) }, color)
    }
}

/** Matrica-ikon: karosszéria-színű lapon a matrica. */
fun DrawScope.drawStickerIcon(sticker: Sticker, body: Color, look: CarLook) {
    val c = Offset(size.width / 2f, size.height / 2f)
    drawRoundRect(
        body,
        Offset(size.width * 0.04f, size.height * 0.12f),
        Size(size.width * 0.92f, size.height * 0.76f),
        androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.2f),
    )
    val wMul = stickerWidthMul(sticker)
    val half = size.minDimension * 0.30f
    val primary = stickerColors[look.stickerColor].color
    val secondary = stickerSecondary(primary, look)
    for (p in stickerPolys(sticker)) {
        fillPoly(
            p.pts.map { Offset(c.x + it.x * half * min(wMul, 1.45f), c.y - it.y * half) },
            if (p.second) secondary else primary,
        )
    }
}
