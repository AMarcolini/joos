package com.amarcolini.joos.localization

import com.amarcolini.joos.drive.Drive
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.kinematics.Kinematics
import com.amarcolini.joos.kinematics.SwerveKinematics
import kotlin.jvm.JvmOverloads

/**
 * Default localizer for swerve drives based on the drive encoder positions, module orientations, and (optionally) a
 * heading sensor.
 *
 * @param wheelPositions wheel positions in linear distance units
 * @param wheelVelocities wheel velocities in linear distance units
 * @param moduleOrientations module orientations
 * @param trackWidth lateral distance between pairs of wheels on different sides of the robot
 * @param wheelBase distance between pairs of wheels on the same side of the robot
 * @param drive the drive this localizer is using
 * @param useExternalHeading whether to use [drive]'s external heading sensor
 */
class SwerveLocalizer @JvmOverloads constructor(
    private val wheelPositions: () -> List<Double>,
    private val wheelVelocities: () -> List<Double>? = { null },
    private val moduleOrientations: () -> List<Angle>,
    private val trackWidth: Double,
    private val wheelBase: Double = trackWidth,
    private val drive: Drive,
    private val useExternalHeading: Boolean = true
) : Localizer {
    private var _poseEstimate = Pose2d()
    override var poseEstimate: Pose2d
        get() = _poseEstimate
        set(value) {
            lastWheelPositions = emptyList()
            lastExtHeading = null
            if (useExternalHeading) drive.externalHeading = value.heading
            _poseEstimate = value
        }
    override var poseVelocity: Pose2d? = null
        private set
    private var lastWheelPositions = emptyList<Double>()
    private var lastExtHeading: Angle? = null

    override fun update() {
        val wheelPositions = wheelPositions()
        val moduleOrientations = moduleOrientations()
        val extHeading: Angle? = if (useExternalHeading) drive.externalHeading else null
        if (lastWheelPositions.isNotEmpty()) {
            val wheelDeltas = wheelPositions
                .zip(lastWheelPositions)
                .map { it.first - it.second }
            val robotPoseDelta = SwerveKinematics.wheelToRobotVelocities(
                wheelDeltas,
                moduleOrientations,
                wheelBase,
                trackWidth
            )
            val lastExtHeading = lastExtHeading
            val finalHeadingDelta = if (extHeading != null && lastExtHeading != null)
                (extHeading - lastExtHeading).normDelta()
            else robotPoseDelta.heading
            _poseEstimate = Kinematics.relativeOdometryUpdate(
                _poseEstimate,
                Pose2d(robotPoseDelta.vec(), finalHeadingDelta)
            )
        }

        val wheelVelocities = wheelVelocities()
        val extHeadingVel = if (useExternalHeading) drive.getExternalHeadingVelocity() else null
        poseVelocity =
            if (wheelVelocities != null)
                SwerveKinematics.wheelToRobotVelocities(
                    wheelVelocities,
                    moduleOrientations,
                    wheelBase,
                    trackWidth
                ).let {
                    if (extHeadingVel != null) Pose2d(it.vec(), extHeadingVel)
                    else it
                }
            else null

        lastWheelPositions = wheelPositions
        lastExtHeading = extHeading
    }
}