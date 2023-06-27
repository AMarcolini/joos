package com.amarcolini.joos.kinematics

import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.util.cos
import com.amarcolini.joos.util.rad
import com.amarcolini.joos.util.sin
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.cos
import kotlin.math.sin

/**
 * Swerve drive kinematic equations. All wheel positions and velocities are given starting with front left and
 * proceeding counter-clockwise (i.e., front left, rear left, rear right, front right). Robot poses are specified in a
 * coordinate system with positive x pointing forward, positive y pointing left, and positive heading measured
 * counter-clockwise from the x-axis.
 */
@JsExport
object SwerveKinematics {

    /**
     * Computes the wheel velocity vectors corresponding to [robotVel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleVelocityVectors(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ): List<Vector2d> {
        val x = wheelBase / 2
        val y = trackWidth / 2

        val vx = robotVel.x
        val vy = robotVel.y
        val omega = robotVel.heading.radians

        return listOf(
            Vector2d(vx - omega * y, vy + omega * x),
            Vector2d(vx - omega * y, vy - omega * x),
            Vector2d(vx + omega * y, vy - omega * x),
            Vector2d(vx + omega * y, vy + omega * x)
        )
    }

    /**
     * Computes the wheel velocities corresponding to [robotVel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToWheelVelocities(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).map(Vector2d::norm)

    /**
     * Computes the module orientations corresponding to [robotVel] given the provided
     * [trackWidth] and [wheelBase].
     *
     * @param robotVel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleOrientations(
        robotVel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).map(Vector2d::angle)

    /**
     * Computes the acceleration vectors corresponding to [robotAccel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleAccelerationVectors(
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ): List<Vector2d> {
        val x = wheelBase / 2
        val y = trackWidth / 2

        val ax = robotAccel.x
        val ay = robotAccel.y
        val alpha = robotAccel.heading.radians

        return listOf(
            Vector2d(ax - alpha * y, ay + alpha * x),
            Vector2d(ax - alpha * y, ay - alpha * x),
            Vector2d(ax + alpha * y, ay - alpha * x),
            Vector2d(ax + alpha * y, ay + alpha * x)
        )
    }

    /**
     * Computes the wheel accelerations corresponding to [robotAccel] given the provided [trackWidth] and
     * [wheelBase].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToWheelAccelerations(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                trackWidth,
                wheelBase
            )
        )
            .map { (vel, accel) ->
                (vel.x * accel.x + vel.y * accel.y) / vel.norm()
            }

    /**
     * Computes the module angular velocities corresponding to [robotAccel] given the provided [trackWidth]
     * and [wheelBase].
     *
     * @param robotAccel velocity of the robot in its reference frame
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun robotToModuleAngularVelocities(
        robotVel: Pose2d,
        robotAccel: Pose2d,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ) =
        robotToModuleVelocityVectors(
            robotVel,
            trackWidth,
            wheelBase
        ).zip(
            robotToModuleAccelerationVectors(
                robotAccel,
                trackWidth,
                wheelBase
            )
        ).map { (vel, accel) ->
            ((vel.x * accel.y - vel.y * accel.x) / (vel.x * vel.x + vel.y * vel.y)).rad
        }

    /**
     * Computes the robot velocities corresponding to [wheelVelocities], [moduleOrientations], and the drive parameters.
     *
     * @param wheelVelocities wheel velocities (or wheel position deltas)
     * @param moduleOrientations wheel orientations (in radians)
     * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
     * @param wheelBase distance between pairs of wheels on the same side of the robot
     */
    @JvmStatic
    @JvmOverloads
    fun wheelToRobotVelocities(
        wheelVelocities: List<Double>,
        moduleOrientations: List<Angle>,
        trackWidth: Double,
        wheelBase: Double = trackWidth
    ): Pose2d {
        val x = wheelBase / 2
        val y = trackWidth / 2

        val vectors = wheelVelocities
            .zip(moduleOrientations)
            .map { (vel, orientation) ->
                Vector2d(
                    vel * cos(orientation),
                    vel * sin(orientation)
                )
            }

        val vx = vectors.sumOf { it.x } / 4
        val vy = vectors.sumOf { it.y } / 4
        val (frontLeft, rearLeft, rearRight, frontRight) = vectors
        val omega = ((
                y * (rearRight.x + frontRight.x - frontLeft.x - rearLeft.x) +
                        x * (frontLeft.y + frontRight.y - rearLeft.y - rearRight.y)
                ) / (4 * (x * x + y * y))).rad

        return Pose2d(vx, vy, omega)
    }
}