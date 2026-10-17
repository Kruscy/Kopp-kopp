package hu.apk.kidsapp.game.pour

/**
 * A tube holds up to [capacity] color units. [segments] runs bottom -> top,
 * so segments.last() is the color currently poured out first.
 */
data class Tube(val capacity: Int, val segments: List<Int>)

fun Tube.top(): Int? = segments.lastOrNull()
fun Tube.isFull(): Boolean = segments.size >= capacity
fun Tube.isSolved(): Boolean = segments.isEmpty() || (segments.size == capacity && segments.all { it == segments[0] })

/** How many units would move if pouring [src] into [dst]; 0 means the move is illegal. */
fun pourAmount(src: Tube, dst: Tube): Int {
    val topColor = src.top() ?: return 0
    if (dst.isFull()) return 0
    if (dst.segments.isNotEmpty() && dst.top() != topColor) return 0
    val run = src.segments.takeLastWhile { it == topColor }.size
    val space = dst.capacity - dst.segments.size
    return minOf(run, space)
}

fun pour(src: Tube, dst: Tube): Pair<Tube, Tube> {
    val amount = pourAmount(src, dst)
    if (amount == 0) return src to dst
    val color = src.top()!!
    val newSrc = src.copy(segments = src.segments.dropLast(amount))
    val newDst = dst.copy(segments = dst.segments + List(amount) { color })
    return newSrc to newDst
}

fun List<Tube>.isSolved(): Boolean = all { it.isSolved() }

private fun List<Tube>.stateKey(): String = joinToString("|") { it.segments.joinToString(",") }

/**
 * Breadth-first search over legal moves, capped so a stuck/rare-unsolvable
 * generated board never hangs the app. Only used at level-generation time.
 */
fun isSolvable(start: List<Tube>, maxStates: Int = 40_000): Boolean {
    if (start.isSolved()) return true
    val visited = HashSet<String>()
    visited.add(start.stateKey())
    // Depth-first (not breadth-first): a solvable deal is found after a few hundred states,
    // where BFS would exhaust its cap and freeze the UI thread on the bigger levels.
    val stack = ArrayDeque<List<Tube>>()
    stack.addLast(start)
    var explored = 0

    while (stack.isNotEmpty() && explored < maxStates) {
        val state = stack.removeLast()
        explored++

        val moves = ArrayList<Pair<Int, Int>>()
        for (i in state.indices) {
            for (j in state.indices) {
                if (i == j || pourAmount(state[i], state[j]) == 0) continue
                val src = state[i]
                val srcUniform = src.segments.all { it == src.segments[0] }
                // Relocating a one-color tube into an empty tube never gets us closer to solved.
                if (state[j].segments.isEmpty() && srcUniform) continue
                moves.add(i to j)
            }
        }
        // Moves onto a non-empty tube are pushed last, so they are explored first.
        moves.sortBy { (_, j) -> if (state[j].segments.isEmpty()) 0 else 1 }
        for ((i, j) in moves) {
            val (newI, newJ) = pour(state[i], state[j])
            val newState = state.toMutableList().apply {
                this[i] = newI
                this[j] = newJ
            }
            if (newState.isSolved()) return true
            if (visited.add(newState.stateKey())) {
                stack.addLast(newState)
            }
        }
    }
    return false
}
