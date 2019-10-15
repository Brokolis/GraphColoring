package graphColoring

import kotlin.math.sqrt

/**
 * A simple 2d vector implementation
 */
data class Vector2D(val x: Double = 0.0, val y: Double = 0.0) {

    // using manual lazy property rather than kotlin's lazy() to improve performance and memory footprint
    private var _length: Double = 0.0
    private var _lengthInitialized = false
    val length: Double get() {
        if (!_lengthInitialized) {
            _length = sqrt(x * x + y * y)
            _lengthInitialized = true
        }
        return _length
    }

    constructor(other: Vector2D) : this(other.x, other.y)

    fun normalize(): Vector2D {
        if (length == 0.0)
            return Vector2D(this)

        return this / length
    }

    operator fun unaryMinus(): Vector2D {
        return Vector2D(-x, -y)
    }

    operator fun plus(other: Vector2D): Vector2D {
        return Vector2D(x + other.x, y + other.y)
    }

    operator fun minus(other: Vector2D): Vector2D {
        return Vector2D(x - other.x, y - other.y)
    }

    operator fun times(amount: Double): Vector2D {
        return Vector2D(x * amount, y * amount)
    }

    operator fun div(amount: Double): Vector2D {
        return Vector2D(x / amount, y / amount)
    }

    companion object {
        val Zero: Vector2D = Vector2D(0.0, 0.0)
    }

}