package com.amarcolini.joos.geometry

import com.amarcolini.joos.util.Angle
import com.amarcolini.joos.util.epsilonEquals
import org.apache.commons.math3.util.FastMath
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Class for representing 2D vectors (x and y).
 */
data class Vector2d @JvmOverloads constructor(
    @JvmField val x: Double = 0.0,
    @JvmField val y: Double = 0.0
) {
    companion object Static {
        /**
         * Returns a vector in Cartesian coordinates `(x, y)` from one in polar coordinates `(r, theta)`.
         */
        @JvmStatic
        fun polar(r: Double, theta: Double) =
            Vector2d(r * cos(theta), r * sin(theta))

        @JvmStatic
        @JvmOverloads
        fun of(x: Double = 0.0, y: Double = 0.0) = Vector2d(x, y)
    }

    /**
     * Returns the magnitude of this vector.
     */
    fun norm() = sqrt(x * x + y * y)

    /**
     * Returns the angle of this vector (in radians).
     */
    fun angle() = Angle.norm(FastMath.atan2(y, x))

    /**
     * Calculates the angle between two vectors (in radians).
     */
    infix fun angleBetween(other: Vector2d) =
        acos((this dot other) / (norm() * other.norm()))

    /**
     * Adds two vectors.
     */
    operator fun plus(other: Vector2d) =
        Vector2d(x + other.x, y + other.y)

    /**
     * Subtracts two vectors.
     */
    operator fun minus(other: Vector2d) =
        Vector2d(x - other.x, y - other.y)

    /**
     * Multiplies two vectors.
     */
    operator fun times(scalar: Double) = Vector2d(scalar * x, scalar * y)

    /**
     * Divides two vectors.
     */
    operator fun div(scalar: Double) = Vector2d(x / scalar, y / scalar)

    /**
     * Returns the negative of this vector.
     */
    operator fun unaryMinus() = Vector2d(-x, -y)

    /**
     * Returns the dot product of two vectors.
     */
    infix fun dot(other: Vector2d) = x * other.x + y * other.y

    /**
     * Returns the 2D cross product of two vectors.
     */
    infix fun cross(other: Vector2d) = x * other.y - y * other.x

    /**
     * Returns the distance between two vectors.
     */
    infix fun distTo(other: Vector2d) = (this - other).norm()

    /**
     * Returns the projection of this vector onto another.
     */
    infix fun projectOnto(other: Vector2d) = other * (this dot other) / (other dot other)

    /**
     * Rotates this vector by [angle].
     */
    fun rotated(angle: Double): Vector2d {
        val newX = x * cos(angle) - y * sin(angle)
        val newY = x * sin(angle) + y * cos(angle)
        return Vector2d(newX, newY)
    }

    infix fun epsilonEquals(other: Vector2d) =
        x epsilonEquals other.x && y epsilonEquals other.y

    override fun toString() = String.format("(%.3f, %.3f)", x, y)
}

operator fun Double.times(vector: Vector2d) = vector.times(this)

operator fun Double.div(vector: Vector2d) = vector.div(this)
