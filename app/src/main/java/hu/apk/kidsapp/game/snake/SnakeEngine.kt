package hu.apk.kidsapp.game.snake

import kotlin.random.Random

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

    fun isOpposite(other: Direction): Boolean = dx == -other.dx && dy == -other.dy
}

data class GridPos(val x: Int, val y: Int)

private fun GridPos.wrapped(cols: Int, rows: Int): GridPos =
    GridPos(((x % cols) + cols) % cols, ((y % rows) + rows) % rows)

data class SnakeState(val snake: List<GridPos>, val fruit: GridPos, val score: Int)

fun randomEmptyCell(occupied: List<GridPos>, cols: Int, rows: Int, rng: Random): GridPos {
    // Retry-until-free would spin forever (freezing the UI thread) if a very long snake ever
    // covered the whole grid, so pick from the explicitly computed free cells instead.
    val taken = occupied.toHashSet()
    val free = ArrayList<GridPos>()
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val cell = GridPos(x, y)
            if (cell !in taken) free.add(cell)
        }
    }
    return if (free.isEmpty()) GridPos(rng.nextInt(cols), rng.nextInt(rows)) else free[rng.nextInt(free.size)]
}

fun initialSnakeState(cols: Int, rows: Int, rng: Random): SnakeState {
    val startX = cols / 2
    val startY = rows / 2
    val snake = listOf(GridPos(startX, startY), GridPos(startX - 1, startY), GridPos(startX - 2, startY))
        .map { it.wrapped(cols, rows) }
    return SnakeState(snake, randomEmptyCell(snake, cols, rows, rng), score = 0)
}

/**
 * No game-over on purpose: this is for a 4-year-old, so the snake just wraps at the
 * edges and can harmlessly cross its own tail. Eating the fruit grows it and scores a point.
 */
fun stepSnake(state: SnakeState, direction: Direction, cols: Int, rows: Int, rng: Random): SnakeState {
    val head = state.snake.first()
    val newHead = GridPos(head.x + direction.dx, head.y + direction.dy).wrapped(cols, rows)
    val ate = newHead == state.fruit
    val newSnake = if (ate) listOf(newHead) + state.snake else listOf(newHead) + state.snake.dropLast(1)
    val newFruit = if (ate) randomEmptyCell(newSnake, cols, rows, rng) else state.fruit
    val newScore = if (ate) state.score + 1 else state.score
    return SnakeState(newSnake, newFruit, newScore)
}
