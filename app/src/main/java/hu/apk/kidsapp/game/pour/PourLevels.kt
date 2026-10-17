package hu.apk.kidsapp.game.pour

import kotlin.random.Random

data class LevelSpec(val numColors: Int, val capacity: Int, val emptyTubes: Int)

/**
 * Hand-picked ramp for a 4-year-old: level 1 is a single, obvious move; each
 * step after that adds only one more color, one more capacity unit, or one
 * more empty tube at a time so the difficulty climbs gradually.
 */
val levelSpecs = listOf(
    LevelSpec(numColors = 2, capacity = 2, emptyTubes = 1),
    LevelSpec(numColors = 2, capacity = 3, emptyTubes = 1),
    LevelSpec(numColors = 3, capacity = 3, emptyTubes = 1),
    LevelSpec(numColors = 3, capacity = 3, emptyTubes = 2),
    LevelSpec(numColors = 3, capacity = 4, emptyTubes = 1),
    LevelSpec(numColors = 4, capacity = 4, emptyTubes = 1),
    LevelSpec(numColors = 4, capacity = 4, emptyTubes = 2),
    LevelSpec(numColors = 5, capacity = 4, emptyTubes = 2),
    LevelSpec(numColors = 5, capacity = 4, emptyTubes = 2),
    LevelSpec(numColors = 6, capacity = 4, emptyTubes = 2),
    LevelSpec(numColors = 6, capacity = 4, emptyTubes = 2),
    LevelSpec(numColors = 6, capacity = 4, emptyTubes = 3),
)

fun levelSpecFor(levelIndex: Int): LevelSpec =
    levelSpecs[levelIndex.coerceIn(0, levelSpecs.lastIndex)]

private fun randomDeal(spec: LevelSpec, rng: Random): List<Tube> {
    val bag = (0 until spec.numColors).flatMap { color -> List(spec.capacity) { color } }.shuffled(rng)
    val tubes = MutableList(spec.numColors) { i ->
        Tube(spec.capacity, bag.subList(i * spec.capacity, (i + 1) * spec.capacity))
    }
    repeat(spec.emptyTubes) { tubes.add(Tube(spec.capacity, emptyList())) }
    tubes.shuffle(rng)
    return tubes
}

/** Deals colors at random and keeps retrying (new shuffle) until the result is provably solvable. */
fun generateSolvableLevel(spec: LevelSpec, seed: Long): List<Tube> {
    var attemptSeed = seed
    repeat(25) { attempt ->
        val rng = Random(attemptSeed)
        val deal = randomDeal(spec, rng)
        if (!deal.isSolved() && isSolvable(deal)) {
            return deal
        }
        attemptSeed = attemptSeed * 31 + attempt + 1
    }
    // Practically unreachable for these small level sizes, but never leave the player stuck.
    return randomDeal(spec, Random(attemptSeed))
}
