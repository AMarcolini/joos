package com.amarcolini.joos.hardware.drive

import com.amarcolini.joos.command.Component
import com.amarcolini.joos.drive.AbstractMecanumDrive
import com.amarcolini.joos.hardware.Motor
import com.amarcolini.joos.hardware.MotorGroup
import com.amarcolini.joos.localization.AngleSensor
import com.amarcolini.joos.trajectory.constraints.MecanumConstraints

/**
 * A [Component] implementation of a mecanum drive.
 */
open class MecanumDrive @JvmOverloads constructor(
    override val motors: MotorGroup,
    trackWidth: Double = 1.0,
    wheelBase: Double = trackWidth,
    lateralMultiplier: Double = 1.0,
    externalHeadingSensor: AngleSensor? = null,
) : AbstractMecanumDrive(
    trackWidth, wheelBase, lateralMultiplier,
    externalHeadingSensor
), DriveComponent {
    /**
     * Constructs a mecanum drive using [constraints].
     */
    @JvmOverloads
    constructor(
        motors: MotorGroup,
        constraints: MecanumConstraints,
        externalHeadingSensor: AngleSensor? = null,
    ) : this(
        motors,
        constraints.trackWidth,
        constraints.wheelBase,
        constraints.lateralMultiplier,
        externalHeadingSensor,
    )

    /**
     * Constructs a mecanum drive from its individual motors.
     */
    @JvmOverloads
    constructor(
        frontLeft: Motor,
        backLeft: Motor,
        backRight: Motor,
        frontRight: Motor,
        externalHeadingSensor: AngleSensor? = null,
        constraints: MecanumConstraints = MecanumConstraints(1.0, 1.0, 1.0, 1.0),
    ) : this(
        MotorGroup(frontLeft, backLeft, backRight, frontRight),
        constraints.trackWidth,
        constraints.wheelBase,
        constraints.lateralMultiplier,
        externalHeadingSensor,
    )

    override fun getWheelPositions() = motors.map { it.distance }

    override fun getWheelVelocities() = motors.map { it.distanceVelocity }

    override fun setMotorPowers(frontLeft: Double, backLeft: Double, backRight: Double, frontRight: Double) {
        motors.zip(listOf(frontLeft, backLeft, backRight, frontRight))
            .forEach { (motor, power) ->
                motor.power = power
            }
    }

    override fun setWheelVelocities(velocities: List<Double>, accelerations: List<Double>) {
        motors.zip(velocities.zip(accelerations))
            .forEach { (motor, power) ->
                motor.setDistanceVelocity(power.first, power.second)
            }
    }

    override fun update() {
        updatePoseEstimate()
        motors.forEach { it.update() }
    }
}