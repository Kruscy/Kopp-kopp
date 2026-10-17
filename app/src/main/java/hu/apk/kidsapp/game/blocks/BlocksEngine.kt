package hu.apk.kidsapp.game.blocks

/** Az építőelemek formái. `units` a magasságuk kocka-egységben. */
enum class Shape(val units: Float, val label: String) {
    CUBE(1f, "Kocka"),
    SLAB(0.5f, "Lap"),
    PILLAR(2f, "Oszlop"),
    CYLINDER(1f, "Henger"),
    ROOF(1f, "Tető"),
    BALL(1f, "Gömb"),
    ARCH(1f, "Boltív"),
    STAR(1f, "Csillag"),
}

data class Piece(val shape: Shape, val color: Int)

/** Hány oszlopból áll a széles építési terület (több képernyőnyi, a kamera mozgatható). */
const val WORLD_COLS = 30

/** Egy oszlop maximális magassága kocka-egységben. */
const val MAX_UNITS = 8f

fun List<Piece>.units(): Float = sumOf { it.shape.units.toDouble() }.toFloat()
