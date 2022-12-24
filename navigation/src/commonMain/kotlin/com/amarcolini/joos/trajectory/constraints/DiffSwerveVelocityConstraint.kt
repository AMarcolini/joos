package com.amarcolini.joos.trajectory.constraints

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.DiffSwerveKinematics
import com.amarcolini.joos.kinematics.Kinematics
import kotlin.math.abs
import kotlin.math.max

/**
 * Differential swerve-specific drive constraints that also limit maximum wheel velocities.
 *
 * @param maxWheelVel maximum wheel velocity
 * @param trackWidth track width
 */
class DiffSwerveVelocityConstraint(val maxWheelVel: Double, val trackWidth: Double) : TrajectoryVelocityConstraint {
    override fun get(pose: Pose2d, deriv: Pose2d, lastDeriv: Pose2d, ds: Double, baseRobotVel: Pose2d): Double {
        val wheel0 = DiffSwerveKinematics.robotToWheelVelocities(baseRobotVel, trackWidth)
        if (wheel0.maxOf(::abs) >= maxWheelVel) {
            throw UnsatisfiableConstraint()
        }

        val robotDeriv = Kinematics.fieldToRobotVelocity(pose, deriv)

        val wheel = DiffSwerveKinematics.robotToWheelVelocities(robotDeriv, trackWidth)
        return wheel0.zip(wheel).minOf {
            max(
                (maxWheelVel - it.first) / it.second,
                (-maxWheelVel - it.first) / it.second
            )
        }
    }
}