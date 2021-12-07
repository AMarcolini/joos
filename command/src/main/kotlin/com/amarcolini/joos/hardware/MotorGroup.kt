package com.amarcolini.joos.hardware

import com.amarcolini.joos.command.Component
import com.qualcomm.robotcore.hardware.DcMotor

/**
 * A class that runs multiple motors together as a unit.
 */
class MotorGroup(private vararg val motors: Motor) : Component {
    /**
     * The maximum revolutions per minute that all motors in the group can achieve.
     */
    @JvmField
    val maxRPM = motors.minOf { it.maxRPM }

    /**
     * The maximum ticks per second velocity that all motors in the group can achieve.
     */
    @JvmField
    val maxTPS: Double = motors.minOf { it.maxTPS }

    /**
     * The maximum distance travelled that all motors in the group have travelled.
     * @see distanceVelocity
     */
    val distance get() = motors.minOf { it.distance }

    /**
     * The minimum distance velocity out of all motors in the group.
     */
    val distanceVelocity get() = motors.minOf { it.distanceVelocity }

    /**
     * Whether the motor group is reversed.
     */
    var reversed: Boolean = false
        /**
         * Sets whether the direction of the motor group is reversed.
         */
        @JvmName("setReversed")
        set(value) {
            motors.forEach {
                it.reversed = value
            }
            field = value
        }
        @JvmName("isReversed")
        get

    /**
     * Reverses the direction of the motor group.
     */
    fun reversed(): MotorGroup {
        reversed = !reversed
        return this
    }

    /**
     * Sets the speed of the motor.
     *
     * @param velocity the velocity to set
     * @param acceleration the acceleration to set
     * @param unit the units [velocity] and [acceleration] are expressed in (revolutions per minute by default)
     */
    @JvmOverloads
    fun setSpeed(
        velocity: Double,
        acceleration: Double = 0.0,
        unit: Motor.RotationUnit = Motor.RotationUnit.RPM
    ) = motors.forEach { it.setSpeed(velocity, acceleration, unit) }

    /**
     * Sets the percentage of power/velocity of the motor group in the range `[-1.0, 1.0]`.
     *
     * *Note*: Since power is expressed as a percentage, motors may move at different speeds.
     */
    fun setPower(power: Double) = motors.forEach { it.setPower(power) }

    var zeroPowerBehavior: Motor.ZeroPowerBehavior = Motor.ZeroPowerBehavior.FLOAT
        set(value) {
            motors.forEach { it.zeroPowerBehavior = value }
            field = value
        }

    override fun update() {
        motors.forEach { it.update() }
    }
}