package hu.apk.kidsapp.ui.screens.car

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ================= Kis 3D-s alapok =================

data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = V3(x * k, y * k, z * k)
    fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun len() = sqrt(dot(this))
    fun norm(): V3 {
        val l = len()
        return if (l < 1e-6f) this else V3(x / l, y / l, z / l)
    }
}

fun mix(a: V3, b: V3, t: Float) = a + (b - a) * t

fun rotY(v: V3, deg: Float): V3 {
    if (deg == 0f) return v
    val r = Math.toRadians(deg.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    return V3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
}

fun rotX(v: V3, deg: Float): V3 {
    if (deg == 0f) return v
    val r = Math.toRadians(deg.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    return V3(v.x, v.y * c - v.z * s, v.y * s + v.z * c)
}

private fun avg(pts: List<V3>): V3 {
    var x = 0f
    var y = 0f
    var z = 0f
    pts.forEach { x += it.x; y += it.y; z += it.z }
    val n = pts.size.toFloat()
    return V3(x / n, y / n, z / n)
}

private fun newell(pts: List<V3>): V3 {
    var nx = 0f
    var ny = 0f
    var nz = 0f
    for (i in pts.indices) {
        val a = pts[i]
        val b = pts[(i + 1) % pts.size]
        nx += (a.y - b.y) * (a.z + b.z)
        ny += (a.z - b.z) * (a.x + b.x)
        nz += (a.x - b.x) * (a.y + b.y)
    }
    return V3(nx, ny, nz).norm()
}

// ================= Színhelyek (slot) és beállítások =================

enum class Slot { BODY, BODY_DARK, GLASS, CHROME, DARK, LAMP, TAIL, TYRE, RIM, STICKER, STICKER2, SIGN }

class NamedColor(val color: Color, val label: String)

val bodyColors = listOf(
    NamedColor(Color(0xFFE53935), "piros"),
    NamedColor(Color(0xFFFF8F00), "narancs"),
    NamedColor(Color(0xFFFDD835), "sárga"),
    NamedColor(Color(0xFF66BB6A), "zöld"),
    NamedColor(Color(0xFF26C6DA), "türkiz"),
    NamedColor(Color(0xFF1E88E5), "kék"),
    NamedColor(Color(0xFF8E44AD), "lila"),
    NamedColor(Color(0xFFFF7EB6), "rózsaszín"),
    NamedColor(Color(0xFFF5F5F7), "fehér"),
    NamedColor(Color(0xFF2B2D36), "fekete"),
    NamedColor(Color(0xFFB0B7C3), "ezüst"),
)

val lampColors = listOf(
    NamedColor(Color(0xFFFFE082), "sárga"),
    NamedColor(Color(0xFFFFFFFF), "fehér"),
    NamedColor(Color(0xFF80D8FF), "jégkék"),
    NamedColor(Color(0xFFFF80AB), "rózsaszín"),
    NamedColor(Color(0xFF69F0AE), "zöld"),
    NamedColor(Color(0xFFFFAB40), "narancs"),
    NamedColor(Color(0xFFB388FF), "lila"),
    NamedColor(Color(0xFFFF5252), "piros"),
)

val tyreColors = listOf(
    NamedColor(Color(0xFF2B2B33), "fekete"),
    NamedColor(Color(0xFF6B6F7A), "szürke"),
    NamedColor(Color(0xFFF1F1F4), "fehér"),
    NamedColor(Color(0xFFE53935), "piros"),
    NamedColor(Color(0xFF1E88E5), "kék"),
    NamedColor(Color(0xFF43A047), "zöld"),
    NamedColor(Color(0xFFFDD835), "sárga"),
    NamedColor(Color(0xFFFF7EB6), "rózsaszín"),
)

val rimColors = listOf(
    NamedColor(Color(0xFFD0D5DD), "ezüst"),
    NamedColor(Color(0xFFFFC93C), "arany"),
    NamedColor(Color(0xFF3A3A44), "fekete"),
    NamedColor(Color(0xFFE53935), "piros"),
    NamedColor(Color(0xFF1E88E5), "kék"),
    NamedColor(Color(0xFF43A047), "zöld"),
    NamedColor(Color(0xFFFF7EB6), "rózsaszín"),
    NamedColor(Color(0xFFB388FF), "lila"),
)

val stickerColors = listOf(
    NamedColor(Color(0xFFE53935), "piros"),
    NamedColor(Color(0xFFFDD835), "sárga"),
    NamedColor(Color(0xFFFFFFFF), "fehér"),
    NamedColor(Color(0xFF2B2D36), "fekete"),
    NamedColor(Color(0xFF1E88E5), "kék"),
    NamedColor(Color(0xFF43A047), "zöld"),
    NamedColor(Color(0xFFFF7EB6), "rózsaszín"),
    NamedColor(Color(0xFFFF8F00), "narancs"),
)

enum class VehicleType(val label: String) {
    SPORTS("Sportkocsi"),
    OFFROAD("Terepjáró"),
    TRUCK("Kamion"),
    BUS("Busz"),
}

enum class RimStyle(val label: String) {
    STAR("Csillag"),
    MANY("Küllős"),
    DISC("Tárcsa"),
    TURBINE("Turbina"),
    OFFROAD("Terepes"),
    FLOWER("Virág"),
}

enum class Sticker(val label: String) {
    NONE("Semmi"),
    STAR("Csillag"),
    BOLT("Villám"),
    FLAME("Láng"),
    HEART("Szív"),
    FLOWER("Virág"),
    SMILEY("Mosoly"),
    STRIPES("Csíkok"),
    CHECKER("Kockás"),
}

data class CarLook(
    val vehicle: VehicleType = VehicleType.SPORTS,
    val body: Int = 0,
    val lamp: Int = 0,
    val tyre: Int = 0,
    val rimStyle: RimStyle = RimStyle.STAR,
    val rimColor: Int = 0,
    val sticker: Sticker = Sticker.STAR,
    val stickerColor: Int = 1,
)

// ================= Felületek és alkatrészek =================

class Face(
    val pts: List<V3>,
    val normal: V3,
    val slot: Slot,
    val layer: Int,
    val unlit: Boolean,
    val light: Float,
) {
    val center: V3 = avg(pts)
}

class Part(val faces: List<Face>, val centroid: V3)

class PartBuilder {
    private class Raw(val pts: List<V3>, val slot: Slot, val layer: Int, val normal: V3?, val unlit: Boolean, val light: Float)

    private val raw = mutableListOf<Raw>()

    fun poly(pts: List<V3>, slot: Slot, layer: Int = 0, normal: V3? = null, unlit: Boolean = false, light: Float = 0f) {
        raw += Raw(pts, slot, layer, normal, unlit, light)
    }

    fun build(): Part {
        val centroid = avg(raw.flatMap { it.pts })
        val faces = raw.map { f ->
            var n = f.normal ?: newell(f.pts)
            if (f.normal == null && n.dot(avg(f.pts) - centroid) < 0f) n = n * -1f
            Face(f.pts, n, f.slot, f.layer, f.unlit, f.light)
        }
        return Part(faces, centroid)
    }
}

/** Csonka hasáb: alul és felül más-más téglalap (lejtős szélvédő, ferde orr stb.). */
class Hexa(val b: List<V3>, val t: List<V3>) {
    val center: V3 = avg(b + t)
    private val yb = b[0].y
    private val yt = t[0].y
    private val zb = b[2].z
    private val zt = t[2].z

    // négyszögek: [bal-alul, jobb-alul, jobb-felül, bal-felül]
    fun right() = listOf(b[3], b[2], t[2], t[3])
    fun left() = listOf(b[0], b[1], t[1], t[0])
    fun front() = listOf(b[1], b[2], t[2], t[1])
    fun back() = listOf(b[0], b[3], t[3], t[0])

    /** a jobb oldali (+z) fal z koordinátája adott magasságban */
    fun zAt(y: Float): Float {
        val k = ((y - yb) / (yt - yb)).coerceIn(0f, 1f)
        return zb + (zt - zb) * k
    }
}

fun hexaOf(
    xb0: Float, xb1: Float, xt0: Float, xt1: Float,
    yb: Float, yt: Float, zb: Float, zt: Float, zc: Float = 0f,
): Hexa {
    val b = listOf(
        V3(xb0, yb, zc - zb), V3(xb1, yb, zc - zb), V3(xb1, yb, zc + zb), V3(xb0, yb, zc + zb),
    )
    val t = listOf(
        V3(xt0, yt, zc - zt), V3(xt1, yt, zc - zt), V3(xt1, yt, zc + zt), V3(xt0, yt, zc + zt),
    )
    return Hexa(b, t)
}

fun PartBuilder.addHexa(h: Hexa, slot: Slot) {
    poly(listOf(h.b[0], h.b[1], h.b[2], h.b[3]), slot)
    poly(listOf(h.t[0], h.t[1], h.t[2], h.t[3]), slot)
    poly(h.right(), slot)
    poly(h.left(), slot)
    poly(h.front(), slot)
    poly(h.back(), slot)
}

fun boxPart(x0: Float, x1: Float, y0: Float, y1: Float, z0: Float, z1: Float, slot: Slot): Part {
    val h = (z1 - z0) / 2f
    val hx = hexaOf(x0, x1, x0, x1, y0, y1, h, h, (z0 + z1) / 2f)
    return PartBuilder().apply { addHexa(hx, slot) }.build()
}

fun rectUV(u0: Float, u1: Float, v0: Float, v1: Float) =
    listOf(Offset(u0, v0), Offset(u1, v0), Offset(u1, v1), Offset(u0, v1))

fun trapUV(u0: Float, u1: Float, v0: Float, v1: Float, inset: Float) =
    listOf(Offset(u0, v0), Offset(u1, v0), Offset(u1 - inset, v1), Offset(u0 + inset, v1))

fun ellipseUV(cu: Float, cv: Float, ru: Float, rv: Float, n: Int = 14) =
    (0 until n).map { i ->
        val a = 2.0 * PI * i / n
        Offset(cu + ru * cos(a).toFloat(), cv + rv * sin(a).toFloat())
    }

private fun bilerp(q: List<V3>, u: Float, v: Float) = mix(mix(q[0], q[1], u), mix(q[3], q[2], u), v)

/** Matrica-szerű lapka egy négyszögön (ablak, lámpa, rács). Visszaadja a közepét és a normálvektorát. */
fun PartBuilder.decal(
    quad: List<V3>,
    ref: V3,
    uv: List<Offset>,
    slot: Slot,
    layer: Int = 1,
    off: Float = 0.016f,
    unlit: Boolean = false,
    light: Float = 0f,
): Pair<V3, V3> {
    var n = newell(quad)
    if (n.dot(avg(quad) - ref) < 0f) n = n * -1f
    val pts = uv.map { bilerp(quad, it.x, it.y) + n * off }
    poly(pts, slot, layer, n, unlit, light)
    return avg(pts) to n
}

fun cylinderPart(
    c: V3, r: Float, hw: Float, yawDeg: Float, rxDeg: Float,
    sideSlot: Slot, capSlot: Slot, n: Int = 20,
): Part {
    val pb = PartBuilder()
    fun w(l: V3) = c + rotY(rotX(l, rxDeg), yawDeg)
    fun wn(l: V3) = rotY(rotX(l, rxDeg), yawDeg)
    for (i in 0 until n) {
        val a0 = 2.0 * PI * i / n
        val a1 = 2.0 * PI * (i + 1) / n
        val am = (a0 + a1) / 2.0
        val p0x = r * cos(a0).toFloat()
        val p0y = r * sin(a0).toFloat()
        val p1x = r * cos(a1).toFloat()
        val p1y = r * sin(a1).toFloat()
        pb.poly(
            listOf(w(V3(p0x, p0y, -hw)), w(V3(p1x, p1y, -hw)), w(V3(p1x, p1y, hw)), w(V3(p0x, p0y, hw))),
            sideSlot,
            normal = wn(V3(cos(am).toFloat(), sin(am).toFloat(), 0f)),
        )
    }
    for (sgn in listOf(-1f, 1f)) {
        val pts = (0 until n).map { i ->
            val a = 2.0 * PI * i / n
            w(V3(r * cos(a).toFloat(), r * sin(a).toFloat(), sgn * hw))
        }
        pb.poly(pts, capSlot, normal = wn(V3(0f, 0f, sgn)))
    }
    return pb.build()
}

// ================= Felnik =================

enum class RimKind { BACK_DARK, BACK_RIM, SPOKE, HOLE, HUB }

class RimPoly(val pts: List<Offset>, val kind: RimKind)

private fun circlePts(cx: Float, cy: Float, r: Float, n: Int = 14) =
    (0 until n).map { i ->
        val a = 2.0 * PI * i / n
        Offset(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
    }

private fun ringPts(r0: Float, r1: Float, n: Int = 24): List<Offset> {
    val outer = (0..n).map { i ->
        val a = 2.0 * PI * i / n
        Offset(r1 * cos(a).toFloat(), r1 * sin(a).toFloat())
    }
    val inner = (n downTo 0).map { i ->
        val a = 2.0 * PI * i / n
        Offset(r0 * cos(a).toFloat(), r0 * sin(a).toFloat())
    }
    return outer + inner
}

private fun spokePts(angleDeg: Float, r0: Float, r1: Float, w0: Float, w1: Float): List<Offset> {
    val a = Math.toRadians(angleDeg.toDouble())
    val dx = cos(a).toFloat()
    val dy = sin(a).toFloat()
    val px = -dy
    val py = dx
    return listOf(
        Offset(dx * r0 + px * w0, dy * r0 + py * w0),
        Offset(dx * r1 + px * w1, dy * r1 + py * w1),
        Offset(dx * r1 - px * w1, dy * r1 - py * w1),
        Offset(dx * r0 - px * w0, dy * r0 - py * w0),
    )
}

private fun polar(r: Float, deg: Float): Offset {
    val a = Math.toRadians(deg.toDouble())
    return Offset(r * cos(a).toFloat(), r * sin(a).toFloat())
}

private fun rotatePts(pts: List<Offset>, rad: Float): List<Offset> {
    val c = cos(rad)
    val s = sin(rad)
    return pts.map { Offset(it.x * c - it.y * s, it.x * s + it.y * c) }
}

/** A felni mintája egységsugarú koordinátákban; `spin` radiánban forgatja. */
fun rimPolys(style: RimStyle, spin: Float): List<RimPoly> {
    val out = mutableListOf<RimPoly>()
    fun add(pts: List<Offset>, kind: RimKind) {
        out += RimPoly(if (spin == 0f) pts else rotatePts(pts, spin), kind)
    }
    when (style) {
        RimStyle.STAR -> {
            add(circlePts(0f, 0f, 1f, 24), RimKind.BACK_DARK)
            add(ringPts(0.86f, 1f), RimKind.SPOKE)
            for (k in 0 until 5) add(spokePts(k * 72f + 90f, 0.1f, 0.92f, 0.20f, 0.11f), RimKind.SPOKE)
            add(circlePts(0f, 0f, 0.22f), RimKind.HUB)
        }
        RimStyle.MANY -> {
            add(circlePts(0f, 0f, 1f, 24), RimKind.BACK_DARK)
            add(ringPts(0.9f, 1f), RimKind.SPOKE)
            for (k in 0 until 12) add(spokePts(k * 30f, 0.12f, 0.92f, 0.05f, 0.04f), RimKind.SPOKE)
            add(circlePts(0f, 0f, 0.18f), RimKind.HUB)
        }
        RimStyle.DISC -> {
            add(circlePts(0f, 0f, 1f, 24), RimKind.BACK_RIM)
            add(ringPts(0.72f, 0.77f), RimKind.HOLE)
            for (k in 0 until 5) {
                val p = polar(0.46f, k * 72f + 90f)
                add(circlePts(p.x, p.y, 0.075f, 10), RimKind.HOLE)
            }
            add(circlePts(0f, 0f, 0.24f), RimKind.HUB)
        }
        RimStyle.TURBINE -> {
            add(circlePts(0f, 0f, 1f, 24), RimKind.BACK_DARK)
            add(ringPts(0.88f, 1f), RimKind.SPOKE)
            for (k in 0 until 9) {
                val a = k * 40f
                add(listOf(polar(0.18f, a), polar(0.18f, a + 16f), polar(0.94f, a + 58f), polar(0.94f, a + 38f)), RimKind.SPOKE)
            }
            add(circlePts(0f, 0f, 0.17f), RimKind.HUB)
        }
        RimStyle.OFFROAD -> {
            add(circlePts(0f, 0f, 1f, 24), RimKind.BACK_RIM)
            for (k in 0 until 6) {
                val p = polar(0.56f, k * 60f + 30f)
                add(circlePts(p.x, p.y, 0.17f, 12), RimKind.HOLE)
            }
            for (k in 0 until 14) {
                val p = polar(0.9f, k * (360f / 14f))
                add(circlePts(p.x, p.y, 0.045f, 8), RimKind.HOLE)
            }
            add(circlePts(0f, 0f, 0.2f), RimKind.HUB)
        }
        RimStyle.FLOWER -> {
            add(circlePts(0f, 0f, 1f, 24), RimKind.BACK_DARK)
            add(ringPts(0.88f, 1f), RimKind.SPOKE)
            for (k in 0 until 6) {
                val a = Math.toRadians((k * 60.0))
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                val petal = (0 until 10).map { i ->
                    val t = 2.0 * PI * i / 10
                    val along = 0.5f + 0.36f * cos(t).toFloat()
                    val across = 0.19f * sin(t).toFloat()
                    Offset(dx * along - dy * across, dy * along + dx * across)
                }
                add(petal, RimKind.SPOKE)
            }
            add(circlePts(0f, 0f, 0.15f), RimKind.HUB)
        }
    }
    return out
}

// ================= Matricák =================

class StickerPoly(val pts: List<Offset>, val second: Boolean)

/** A matrica szélességi szorzója (csíkok, kockás sáv hosszúkás). */
fun stickerWidthMul(s: Sticker) = when (s) {
    Sticker.STRIPES -> 2.4f
    Sticker.CHECKER -> 2.4f
    else -> 1f
}

fun stickerPolys(s: Sticker): List<StickerPoly> {
    val out = mutableListOf<StickerPoly>()
    when (s) {
        Sticker.NONE -> {}
        Sticker.STAR -> {
            out += StickerPoly((0 until 10).map { i ->
                val r = if (i % 2 == 0) 1f else 0.42f
                polar(r, -90f + i * 36f).let { Offset(it.x, -it.y) }
            }, false)
        }
        Sticker.BOLT -> {
            out += StickerPoly(
                listOf(
                    Offset(0.15f, 1f), Offset(-0.62f, -0.08f), Offset(-0.1f, -0.08f),
                    Offset(-0.36f, -1f), Offset(0.62f, 0.18f), Offset(0.1f, 0.18f), Offset(0.62f, 1f),
                ),
                false,
            )
        }
        Sticker.FLAME -> {
            out += StickerPoly(
                listOf(
                    Offset(0f, 1f), Offset(0.22f, 0.62f), Offset(0.5f, 0.3f), Offset(0.62f, -0.1f),
                    Offset(0.52f, -0.55f), Offset(0.25f, -0.9f), Offset(0f, -1f), Offset(-0.25f, -0.9f),
                    Offset(-0.52f, -0.55f), Offset(-0.62f, -0.1f), Offset(-0.45f, 0.15f), Offset(-0.38f, 0.45f),
                    Offset(-0.18f, 0.35f), Offset(-0.12f, 0.65f),
                ),
                false,
            )
            out += StickerPoly(
                listOf(
                    Offset(0f, 0.2f), Offset(0.18f, -0.05f), Offset(0.28f, -0.4f), Offset(0.15f, -0.68f),
                    Offset(0f, -0.74f), Offset(-0.15f, -0.68f), Offset(-0.28f, -0.4f), Offset(-0.18f, -0.1f),
                ),
                true,
            )
        }
        Sticker.HEART -> {
            out += StickerPoly((0 until 32).map { i ->
                val t = 2.0 * PI * i / 32
                val x = 16.0 * sin(t).let { it * it * it }
                val y = 13.0 * cos(t) - 5.0 * cos(2 * t) - 2.0 * cos(3 * t) - cos(4 * t)
                Offset((x / 17.0).toFloat(), ((y + 2.0) / 17.0).toFloat())
            }, false)
        }
        Sticker.FLOWER -> {
            for (k in 0 until 5) {
                val a = Math.toRadians(k * 72.0 - 90.0)
                val dx = cos(a).toFloat()
                val dy = sin(a).toFloat()
                out += StickerPoly((0 until 10).map { i ->
                    val t = 2.0 * PI * i / 10
                    val along = 0.58f + 0.38f * cos(t).toFloat()
                    val across = 0.30f * sin(t).toFloat()
                    Offset(dx * along - dy * across, dy * along + dx * across)
                }, false)
            }
            out += StickerPoly(circlePts(0f, 0f, 0.3f, 14), true)
        }
        Sticker.SMILEY -> {
            out += StickerPoly(circlePts(0f, 0f, 1f, 24), false)
            out += StickerPoly(ellipseUV(-0.36f, 0.3f, 0.12f, 0.18f, 10), true)
            out += StickerPoly(ellipseUV(0.36f, 0.3f, 0.12f, 0.18f, 10), true)
            val smile = (0..10).map { i ->
                val t = PI * (0.12 + 0.76 * i / 10.0)
                Offset(0.62f * cos(t).toFloat(), -0.1f - 0.62f * sin(t).toFloat())
            }
            val outer = smile
            val inner = smile.reversed().map { Offset(it.x * 0.86f, it.y * 0.86f + 0.06f) }
            out += StickerPoly(outer + inner, true)
        }
        Sticker.STRIPES -> {
            out += StickerPoly(rectUV(-1f, 1f, 0.28f, 0.62f), false)
            out += StickerPoly(rectUV(-1f, 1f, -0.62f, -0.28f), false)
        }
        Sticker.CHECKER -> {
            val cols = 12
            val rows = 3
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val u0 = -1f + 2f * c / cols
                    val u1 = -1f + 2f * (c + 1) / cols
                    val v0 = -0.5f + 1f * r / rows
                    val v1 = -0.5f + 1f * (r + 1) / rows
                    out += StickerPoly(rectUV(u0, u1, v0, v1), (r + c) % 2 == 0)
                }
            }
        }
    }
    return out
}

// ================= Járművek =================

class WheelInst(val center: V3, val radius: Float, val hw: Float, val yawDeg: Float, val part: Part)

class Lamp(val pos: V3, val normal: V3, val radius: Float)

class StickerAnchor(val cx: Float, val cy: Float, val half: Float, val zOut: Float)

/** Egy alkatrész síkja, amely a másik alkatrészt teljesen a külső oldalán hagyja (elválasztó sík). */
class SepPlane(val n: V3, val p: V3)

class VehicleModel(
    val parts: List<Part>,
    val wheels: List<WheelInst>,
    val lamps: List<Lamp>,
    val sticker: StickerAnchor,
    val stickerHost: Part,
    val fitRadius: Float,
    val pivotY: Float,
    val halfLength: Float,
    val halfWidth: Float,
    val frontX: Float,
) {
    /** sep[i][j]: az i alkatrész egy lapjának síkja, amely a j alkatrészt teljesen a külső oldalán hagyja. */
    val sep: Array<Array<SepPlane?>> by lazy { computeSeparation(parts) }
}

private const val SEP_EPS = 0.03f

private fun computeSeparation(parts: List<Part>): Array<Array<SepPlane?>> {
    val pts = parts.map { part -> part.faces.flatMap { it.pts } }
    return Array(parts.size) { i ->
        Array<SepPlane?>(parts.size) { j ->
            if (i == j) {
                null
            } else {
                var found: SepPlane? = null
                for (f in parts[i].faces) {
                    if (f.layer != 0) continue
                    val p0 = f.pts[0]
                    if (pts[j].all { f.normal.dot(it - p0) >= -SEP_EPS }) {
                        found = SepPlane(f.normal, p0)
                        break
                    }
                }
                found
            }
        }
    }
}

private class Builder {
    val parts = mutableListOf<Part>()
    val wheels = mutableListOf<WheelInst>()
    val lamps = mutableListOf<Lamp>()

    /** A kerekek a karosszérián KÍVÜL ülnek (nem lógnak bele), így a takarás pontos. */
    fun wheel(c: V3, r: Float, hw: Float, yaw: Float = 0f) {
        val p = cylinderPart(c, r, hw, yaw, 0f, Slot.TYRE, Slot.TYRE)
        parts += p
        wheels += WheelInst(c, r, hw, yaw, p)
    }

    fun lampAt(res: Pair<V3, V3>, radius: Float) {
        lamps += Lamp(res.first, res.second, radius)
    }

    fun axleBox(x: Float, y0: Float, y1: Float, zHalf: Float) {
        parts += boxPart(x - 0.08f, x + 0.08f, y0, y1, -zHalf, zHalf, Slot.DARK)
    }
}

private val cache = HashMap<VehicleType, VehicleModel>()

fun vehicleModel(type: VehicleType): VehicleModel = cache.getOrPut(type) {
    when (type) {
        VehicleType.SPORTS -> buildSports()
        VehicleType.OFFROAD -> buildOffroad()
        VehicleType.TRUCK -> buildTruck()
        VehicleType.BUS -> buildBus()
    }
}

private fun buildSports(): VehicleModel {
    val b = Builder()
    val body = hexaOf(-2.2f, 2.25f, -2.1f, 1.7f, 0.30f, 0.78f, 0.80f, 0.76f)
    val cab = hexaOf(-1.1f, 0.55f, -0.7f, 0.05f, 0.78f, 1.28f, 0.70f, 0.52f)

    val bp = PartBuilder()
    bp.addHexa(body, Slot.BODY)
    bp.decal(body.front(), body.center, rectUV(0.32f, 0.68f, 0.18f, 0.5f), Slot.DARK)
    for (c in listOf(0.17f, 0.83f)) {
        b.lampAt(bp.decal(body.front(), body.center, trapUV(c - 0.11f, c + 0.11f, 0.52f, 0.82f, 0.04f), Slot.LAMP, unlit = true), 0.30f)
    }
    for (c in listOf(0.14f, 0.86f)) {
        bp.decal(body.back(), body.center, rectUV(c - 0.12f, c + 0.12f, 0.45f, 0.78f), Slot.TAIL, unlit = true)
    }
    val bodyPart = bp.build()
    b.parts += bodyPart

    val cp = PartBuilder()
    cp.addHexa(cab, Slot.BODY)
    for (q in listOf(cab.right(), cab.left())) {
        cp.decal(q, cab.center, trapUV(0.10f, 0.92f, 0.18f, 0.86f, 0.14f), Slot.GLASS)
    }
    cp.decal(cab.front(), cab.center, trapUV(0.06f, 0.94f, 0.12f, 0.9f, 0.07f), Slot.GLASS)
    cp.decal(cab.back(), cab.center, trapUV(0.1f, 0.9f, 0.18f, 0.82f, 0.06f), Slot.GLASS)
    b.parts += cp.build()

    // légterelő + tartók, lökhárítók, tükrök (mind a karosszéria FELÜLETÉN ülnek, nem lógnak bele)
    b.parts += boxPart(-2.18f, -1.78f, 1.0f, 1.06f, -0.76f, 0.76f, Slot.BODY_DARK)
    for (s in listOf(-1f, 1f)) b.parts += boxPart(-2.05f, -1.95f, 0.78f, 1.0f, s * 0.5f - 0.04f, s * 0.5f + 0.04f, Slot.DARK)
    b.parts += boxPart(2.25f, 2.37f, 0.28f, 0.40f, -0.78f, 0.78f, Slot.DARK)
    b.parts += boxPart(-2.32f, -2.2f, 0.28f, 0.46f, -0.78f, 0.78f, Slot.DARK)
    b.parts += boxPart(0.15f, 0.34f, 0.79f, 0.92f, 0.70f, 0.95f, Slot.BODY)
    b.parts += boxPart(0.15f, 0.34f, 0.79f, 0.92f, -0.95f, -0.70f, Slot.BODY)

    for (x in listOf(1.38f, -1.38f)) for (z in listOf(0.95f, -0.95f)) b.wheel(V3(x, 0.37f, z), 0.37f, 0.15f)

    return VehicleModel(
        b.parts, b.wheels, b.lamps,
        StickerAnchor(-0.25f, 0.53f, 0.19f, body.zAt(0.53f) + 0.014f), bodyPart,
        fitRadius = 2.9f, pivotY = 0.55f, halfLength = 2.4f, halfWidth = 1.12f, frontX = 2.37f,
    )
}

private fun buildOffroad(): VehicleModel {
    val b = Builder()
    val body = hexaOf(-2.1f, 2.05f, -2.0f, 1.95f, 0.62f, 1.30f, 0.98f, 0.94f)
    val cab = hexaOf(-1.75f, 0.85f, -1.7f, 0.60f, 1.30f, 2.0f, 0.92f, 0.86f)

    val bp = PartBuilder()
    bp.addHexa(body, Slot.BODY)
    bp.decal(body.front(), body.center, rectUV(0.30f, 0.70f, 0.30f, 0.62f), Slot.DARK)
    for (c in listOf(0.14f, 0.86f)) {
        b.lampAt(bp.decal(body.front(), body.center, ellipseUV(c, 0.64f, 0.085f, 0.17f), Slot.LAMP, unlit = true), 0.32f)
    }
    for (c in listOf(0.07f, 0.93f)) bp.decal(body.back(), body.center, rectUV(c - 0.04f, c + 0.04f, 0.45f, 0.8f), Slot.TAIL, unlit = true)
    val bodyPart = bp.build()
    b.parts += bodyPart

    val cp = PartBuilder()
    cp.addHexa(cab, Slot.BODY)
    for (q in listOf(cab.right(), cab.left())) {
        cp.decal(q, cab.center, trapUV(0.06f, 0.48f, 0.18f, 0.86f, 0.05f), Slot.GLASS)
        cp.decal(q, cab.center, trapUV(0.52f, 0.94f, 0.18f, 0.86f, 0.05f), Slot.GLASS)
    }
    cp.decal(cab.front(), cab.center, trapUV(0.06f, 0.94f, 0.14f, 0.9f, 0.04f), Slot.GLASS)
    cp.decal(cab.back(), cab.center, trapUV(0.1f, 0.9f, 0.2f, 0.82f, 0.03f), Slot.GLASS)
    b.parts += cp.build()

    // tetőcsomagtartó: sínek a tetőn, keresztrudak a síneken
    for (s in listOf(-1f, 1f)) b.parts += boxPart(-1.5f, 0.55f, 2.0f, 2.08f, s * 0.76f - 0.04f, s * 0.76f + 0.04f, Slot.DARK)
    for (x in listOf(-1.4f, -0.5f, 0.4f)) b.parts += boxPart(x - 0.04f, x + 0.04f, 2.08f, 2.16f, -0.8f, 0.8f, Slot.DARK)
    // lökhárítók, védőrács
    b.parts += boxPart(2.05f, 2.28f, 0.62f, 0.82f, -0.96f, 0.96f, Slot.DARK)
    b.parts += boxPart(-2.3f, -2.1f, 0.62f, 0.82f, -0.96f, 0.96f, Slot.DARK)
    for (s in listOf(-1f, 1f)) b.parts += boxPart(2.1f, 2.2f, 0.82f, 1.14f, s * 0.6f - 0.04f, s * 0.6f + 0.04f, Slot.CHROME)
    b.parts += boxPart(2.1f, 2.2f, 1.14f, 1.22f, -0.64f, 0.64f, Slot.CHROME)
    // tengelyek, sárvédők (a kerekek tetején)
    for (x in listOf(1.35f, -1.35f)) {
        b.axleBox(x, 0.46f, 0.62f, 1.0f)
        b.parts += boxPart(x - 0.72f, x + 0.72f, 1.16f, 1.26f, 0.98f, 1.42f, Slot.DARK)
        b.parts += boxPart(x - 0.72f, x + 0.72f, 1.16f, 1.26f, -1.42f, -0.98f, Slot.DARK)
    }
    for (x in listOf(1.35f, -1.35f)) for (z in listOf(1.2f, -1.2f)) b.wheel(V3(x, 0.58f, z), 0.58f, 0.22f)
    // pótkerék a hátulján (tengelye az x irányba néz), tartóval
    b.parts += boxPart(-2.3f, -2.08f, 1.0f, 1.3f, -0.12f, 0.12f, Slot.DARK)
    b.wheel(V3(-2.5f, 1.15f, 0f), 0.52f, 0.2f, 90f)

    return VehicleModel(
        b.parts, b.wheels, b.lamps,
        StickerAnchor(-0.1f, 0.98f, 0.30f, body.zAt(0.98f) + 0.014f), bodyPart,
        fitRadius = 3.4f, pivotY = 1.0f, halfLength = 2.7f, halfWidth = 1.45f, frontX = 2.3f,
    )
}

private fun buildTruck(): VehicleModel {
    val b = Builder()
    b.parts += boxPart(-3.5f, 3.4f, 0.55f, 0.95f, -0.9f, 0.9f, Slot.DARK)
    for (x in listOf(2.5f, -1.6f, -2.9f)) b.axleBox(x, 0.45f, 0.55f, 1.0f)
    // fülke padlója, raktér padlója
    b.parts += boxPart(1.35f, 3.45f, 0.95f, 1.12f, -0.9f, 0.9f, Slot.DARK)
    b.parts += boxPart(-3.4f, 1.1f, 0.95f, 1.15f, -0.9f, 0.9f, Slot.DARK)

    val cab = hexaOf(1.35f, 3.45f, 1.35f, 3.1f, 1.12f, 3.0f, 1.12f, 1.05f)
    val cp = PartBuilder()
    cp.addHexa(cab, Slot.BODY)
    for (q in listOf(cab.right(), cab.left())) {
        cp.decal(q, cab.center, trapUV(0.14f, 0.88f, 0.55f, 0.9f, 0.06f), Slot.GLASS)
        cp.decal(q, cab.center, rectUV(0.5f, 0.52f, 0.08f, 0.5f), Slot.DARK)
    }
    cp.decal(cab.front(), cab.center, rectUV(0.08f, 0.92f, 0.52f, 0.92f), Slot.GLASS)
    cp.decal(cab.front(), cab.center, rectUV(0.22f, 0.78f, 0.1f, 0.38f), Slot.DARK)
    for (c in listOf(0.1f, 0.9f)) {
        b.lampAt(cp.decal(cab.front(), cab.center, rectUV(c - 0.06f, c + 0.06f, 0.12f, 0.3f), Slot.LAMP, unlit = true), 0.34f)
    }
    b.parts += cp.build()
    // légterelő a tetőn
    b.parts += PartBuilder().apply { addHexa(hexaOf(1.4f, 2.9f, 1.4f, 1.95f, 3.0f, 3.5f, 0.92f, 0.92f), Slot.BODY) }.build()
    // raktér
    val cargo = hexaOf(-3.5f, 1.2f, -3.5f, 1.2f, 1.15f, 3.5f, 1.15f, 1.15f)
    val gp = PartBuilder()
    gp.addHexa(cargo, Slot.BODY)
    gp.decal(cargo.back(), cargo.center, rectUV(0.495f, 0.505f, 0.04f, 0.96f), Slot.DARK)
    for (c in listOf(0.08f, 0.92f)) gp.decal(cargo.back(), cargo.center, rectUV(c - 0.05f, c + 0.05f, 0.04f, 0.1f), Slot.TAIL, unlit = true)
    val cargoPart = gp.build()
    b.parts += cargoPart
    // lökhárító, kipufogók, tükrök
    b.parts += boxPart(3.45f, 3.67f, 0.6f, 0.95f, -1.08f, 1.08f, Slot.DARK)
    for (s in listOf(-1f, 1f)) {
        b.parts += cylinderPart(V3(1.28f, 2.1f, s * 1.24f), 0.09f, 0.8f, 0f, 90f, Slot.CHROME, Slot.CHROME)
        b.parts += boxPart(3.2f, 3.36f, 1.9f, 2.5f, if (s > 0) 1.12f else -1.34f, if (s > 0) 1.34f else -1.12f, Slot.DARK)
    }
    for (x in listOf(2.5f, -1.6f, -2.9f)) for (z in listOf(1.15f, -1.15f)) b.wheel(V3(x, 0.55f, z), 0.55f, 0.2f)

    return VehicleModel(
        b.parts, b.wheels, b.lamps,
        StickerAnchor(-1.15f, 2.3f, 0.85f, 1.15f + 0.014f), cargoPart,
        fitRadius = 4.2f, pivotY = 1.7f, halfLength = 3.8f, halfWidth = 1.4f, frontX = 3.67f,
    )
}

private fun buildBus(): VehicleModel {
    val b = Builder()
    val body = hexaOf(-4.2f, 4.2f, -4.1f, 3.85f, 0.55f, 3.35f, 1.0f, 0.96f)
    val bp = PartBuilder()
    bp.addHexa(body, Slot.BODY)
    for (q in listOf(body.right(), body.left())) {
        for (i in 0 until 6) {
            val u0 = 0.06f + i * 0.13f
            bp.decal(q, body.center, rectUV(u0, u0 + 0.10f, 0.55f, 0.88f), Slot.GLASS)
        }
        bp.decal(q, body.center, rectUV(0.84f, 0.95f, 0.1f, 0.88f), Slot.GLASS)
        bp.decal(q, body.center, rectUV(0.0f, 1.0f, 0.2f, 0.27f), Slot.BODY_DARK)
    }
    bp.decal(body.front(), body.center, rectUV(0.06f, 0.94f, 0.42f, 0.9f), Slot.GLASS)
    bp.decal(body.front(), body.center, rectUV(0.3f, 0.7f, 0.92f, 0.99f), Slot.SIGN, unlit = true)
    bp.decal(body.front(), body.center, rectUV(0.34f, 0.66f, 0.08f, 0.28f), Slot.DARK)
    for (c in listOf(0.07f, 0.93f)) {
        b.lampAt(bp.decal(body.front(), body.center, rectUV(c - 0.05f, c + 0.05f, 0.1f, 0.24f), Slot.LAMP, unlit = true), 0.34f)
    }
    bp.decal(body.back(), body.center, rectUV(0.1f, 0.9f, 0.5f, 0.88f), Slot.GLASS)
    for (c in listOf(0.05f, 0.95f)) bp.decal(body.back(), body.center, rectUV(c - 0.04f, c + 0.04f, 0.1f, 0.3f), Slot.TAIL, unlit = true)
    val bodyPart = bp.build()
    b.parts += bodyPart
    b.parts += boxPart(-2.0f, 0.5f, 3.35f, 3.62f, -0.6f, 0.6f, Slot.BODY_DARK)
    b.parts += boxPart(4.2f, 4.36f, 0.55f, 0.82f, -0.97f, 0.97f, Slot.DARK)
    for (s in listOf(-1f, 1f)) b.parts += boxPart(3.95f, 4.1f, 2.3f, 2.85f, if (s > 0) 1.0f else -1.2f, if (s > 0) 1.2f else -1.0f, Slot.DARK)
    for (x in listOf(2.7f, -2.5f)) b.axleBox(x, 0.45f, 0.55f, 1.0f)
    for (x in listOf(2.7f, -2.5f)) for (z in listOf(1.2f, -1.2f)) b.wheel(V3(x, 0.55f, z), 0.55f, 0.2f)

    return VehicleModel(
        b.parts, b.wheels, b.lamps,
        StickerAnchor(-0.6f, 1.35f, 0.55f, body.zAt(1.35f) + 0.014f), bodyPart,
        fitRadius = 4.7f, pivotY = 1.9f, halfLength = 4.5f, halfWidth = 1.5f, frontX = 4.36f,
    )
}
