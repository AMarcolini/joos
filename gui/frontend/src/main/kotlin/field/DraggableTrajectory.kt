package field

import GUIApp
import GUIApp.Companion.focusManager
import GUIApp.Companion.menus
import GUIApp.Companion.modalManager
import GUIApp.Companion.popupManager
import com.amarcolini.joos.geometry.Angle
import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.path.Path
import com.amarcolini.joos.path.heading.SplineHeading
import com.amarcolini.joos.path.heading.ValueHeading
import com.amarcolini.joos.serialization.*
import com.amarcolini.joos.trajectory.Trajectory
import com.amarcolini.joos.trajectory.constraints.*
import com.amarcolini.joos.util.deg
import io.nacular.doodle.controls.modal.ModalManager
import io.nacular.doodle.controls.popupmenu.MenuCreationContext
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Stroke
import io.nacular.doodle.event.PointerListener
import io.nacular.doodle.layout.constraints.Strength.Companion.Strong
import io.nacular.doodle.system.SystemPointerEvent
import io.nacular.doodle.utils.addOrAppend
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import settings.KnotMenu
import util.NumberField
import util.TrajectoryMetadata
import util.TrajectoryMetadata.Companion.with
import util.TrajectoryMetadata.Companion.withData
import kotlin.math.max
import kotlin.properties.Delegates

object DraggableTrajectory : EntityGroup() {
    private val knots: MutableList<SplineKnot> = mutableListOf()

    enum class Mode {
        View,
        EditHeading,
        EditPath
    }

    var mode: Mode by Delegates.observable(Mode.EditPath) { _, _, new ->
        when (new) {
            Mode.View -> disableEditing()
            Mode.EditHeading -> initializeHeadingEditing()
            Mode.EditPath -> initializePathEditing()
        }
    }

    private var trajectory: TrajectoryMetadata = TrajectoryMetadata.fromTrajectory(
        window.localStorage.getItem(GUIApp.trajectoryKey)?.let {
            try {
                SerializableTrajectory.fromJSON(it)
            } catch (_: Exception) {
                null
            }
        }
            ?: SerializableTrajectory(
                StartPiece(Pose2d()),
                mutableListOf(
                    LinePiece(Vector2d(30.0, 30.0), SplineHeading(45.deg)),
                    SplinePiece(Vector2d(40.0, 0.0), 30.deg)
                )
            )
    )

    val numPieces get() = trajectory.pieceData.size

    private val pathEntity: PathEntity = PathEntity(trajectory.serializableTrajectory().createPath().path, Stroke(Color.Green))

    val currentPath: Path get() = pathEntity.path

    data class TempGenericConstraints constructor(
        var maxVel: Double = 30.0,
        var maxAccel: Double = 30.0,
        override var maxAngVel: Angle = 180.deg,
        override var maxAngAccel: Angle = 180.deg,
        override var maxAngJerk: Angle = 0.deg
    ) : TrajectoryConstraints {
        @Transient
        override val velConstraint
            get() = MinVelocityConstraint(
                listOf(
                    TranslationalVelocityConstraint(maxVel),
                    AngularVelocityConstraint(maxAngVel),
                )
            )

        @Transient
        override val accelConstraint
            get() = MinAccelerationConstraint(
                listOf(
                    TranslationalAccelerationConstraint(maxAccel)
                )
            )
    }

    val constraints: TempGenericConstraints = TempGenericConstraints()

    var currentTrajectory: Trajectory? = null
        private set
        get() {
            if (!trajectoryIsUpdated) {
                field = trajectory.serializableTrajectory().createTrajectory(
                    constraints
                ).trajectory
                trajectoryIsUpdated = true
            }
            return field
        }
    private var trajectoryIsUpdated = false

    fun recomputeTrajectory() {
        trajectoryIsUpdated = false
    }

    private fun update() {
        pathEntity.path = trajectory.serializableTrajectory().createPath().path
        trajectoryIsUpdated = false
    }

    fun serializableTrajectory() = trajectory.serializableTrajectory()

    fun toJSON(): String {
        return serializableTrajectory().toJSON().replace(Regex("(?<=\\d\\.\\d)\\d+"), "")
    }

    fun disableEditing() {
        children.removeAll(knots)
        knots.clear()
    }

    private fun delete(index: Int) {
        if (index < 0 || index >= trajectory.pieceData.size) return
        trajectory.pieceData.removeAt(index)
        update()
        when (mode) {
            Mode.EditHeading -> initializeHeadingEditing()
            Mode.EditPath -> initializePathEditing()
            Mode.View -> {}
        }
    }

    private fun insert(
        new: TrajectoryMetadata.PieceWithData,
        relativeTo: Int = trajectory.pieceData.lastIndex,
        indexTransform: (Int) -> Int,
    ) {
        trajectory.pieceData.addOrAppend(
            max(0, indexTransform(relativeTo)),
            new
        )
        update()
        when (mode) {
            Mode.EditHeading -> initializeHeadingEditing()
            Mode.EditPath -> initializePathEditing()
            Mode.View -> {}
        }
    }

    private fun addAfter(index: Int, new: TrajectoryMetadata.PieceWithData) =
        insert(relativeTo = index, new = new) {
            it + 1
        }

    private fun addBefore(index: Int, new: TrajectoryMetadata.PieceWithData) =
        insert(relativeTo = index, new = new) {
            it - 1
        }

    private fun makeSpline(position: Vector2d) =
        SplinePiece(position, 0.deg) with SplineKnot.LengthMode.FIXED_LENGTH

    private fun makeLine(position: Vector2d) =
        LinePiece(position, SplineHeading(0.deg)).withData()

    private fun makeMenu(
        knot: PathKnot,
        customMenu: MenuCreationContext.() -> Unit
    ) {
        val listener = PointerListener.pressed { event ->
            if (SystemPointerEvent.Button.Button2 in event.buttons && SystemPointerEvent.Button.Button1 !in event.buttons) {
                GUIApp.appScope.launch {
                    modalManager {
                        val innerMenu = menus({ completed(Unit) }, customMenu)
                        val menu = KnotMenu.createPathKnotMenu(
                            listOf(innerMenu),
                            knot,
                            knot.startVisible || knot.endVisible
                        )
                        pointerOutsideModalChanged += PointerListener.pressed {
                            completed(Unit)
                        }
                        ModalManager.RelativeModal(menu, knot) { modal, knot ->
                            (modal.top eq knot.bottom + 10)..Strong
                            (modal.top greaterEq 5)..Strong
                            (modal.left greaterEq 5)..Strong
                            (modal.centerX eq knot.center.x)..Strong

                            modal.right lessEq parent.right - 5

                            when {
                                parent.height.readOnly - knot.bottom > modal.height.readOnly + 15 -> modal.bottom lessEq parent.bottom - 5
                                else -> modal.bottom lessEq knot.y - 10
                            }

                            modal.width.preserve
                            modal.height.preserve
                        }
                    }
                }
            }
        }
        knot.pointerChanged += listener
    }

    private fun makeMenu(
        knot: PathKnot,
        pieceIndex: Int,
        insertAfter: Boolean = true,
        insertBefore: Boolean = true,
        deletable: Boolean = true,
        nonPathPieces: List<TrajectoryMetadata.PieceWithData> = emptyList()
    ) = makeMenu(knot) {
        val knotPosition = { knot.position.toVector2d() }
        if (insertAfter) menu("Insert After") {
            action("Spline") { addAfter(pieceIndex, makeSpline(knotPosition())) }
            action("Line") { addAfter(pieceIndex, makeLine(knotPosition())) }
            action("Turn") {
                addAfter(
                    pieceIndex,
                    TrajectoryMetadata.PieceWithData(TurnPiece(0.deg), knotPosition)
                )
            }
            action("Wait") {
                addAfter(
                    pieceIndex,
                    TrajectoryMetadata.PieceWithData(WaitPiece(0.0), knotPosition)
                )
            }
        }
        if (insertBefore) menu("Insert Before") {
            action("Spline") { addBefore(pieceIndex, makeLine(knotPosition())) }
            action("Line") { addBefore(pieceIndex, makeLine(knotPosition())) }
        }
        if (nonPathPieces.isNotEmpty()) menu("Edit Pieces") {
            nonPathPieces.forEach { data ->
                when (val piece = data.trajectoryPiece) {
                    is TurnPiece -> {
                        val decimals = 0
                        val initialValue = piece.angle.degrees
                        action("Turn ${initialValue.format(decimals)}°") { _ ->
                            val field = NumberField(
                                initialValue,
                                decimals,
                                true,
                                { piece.angle = it.deg },
                                format = { "${it}°" }
                            )
                            popupManager.show(field, relativeTo = knot) { parent, bounds ->
                                parent.center eq bounds.center
                            }
                            field.focusChanged += { _, _, new ->
                                if (!new) popupManager.hide(field)
                                else field.selectAll()
                            }
                            focusManager.requestFocus(field)
                        }
                    }
                    is WaitPiece -> {
                        val decimals = 1
                        val initialValue = piece.duration
                        action("Wait ${initialValue.format(decimals)}s") { _ ->
                            val field =
                                NumberField(initialValue, decimals, false, piece::duration::set, format = { "${it}s" })
                            popupManager.show(field, relativeTo = knot) { parent, bounds ->
                                parent.center eq bounds.center
                            }
                            field.focusChanged += { _, _, new ->
                                if (!new) popupManager.hide(field)
                                else field.selectAll()
                            }
                            focusManager.requestFocus(field)
                        }
                    }
                    else -> {}
                }
            }
        }
        if (!(!deletable || pieceIndex < 0 || pieceIndex >= trajectory.pieceData.size)) action("Delete") {
            delete(pieceIndex)
        }
    }

    private fun initializePathEditing() {
        disableEditing()
        knots += PathKnot().apply {
            val startData = trajectory.startData
            val startPos = startData.start.pose::vec
            position = startPos().toPoint()
            tangent = startData.start.tangent
            startVisible = false
            endVisible = true
            onChange += {
                startData.start.pose = Pose2d(it.position.toVector2d())
                startData.start.tangent = it.tangent
                update()
            }
            val nonPathPieces = trajectory.pieceData.takeWhile {
                it.trajectoryPiece !is MovableTrajectoryPiece
            }
            isSpecial = nonPathPieces.isNotEmpty()
            makeMenu(
                this, -1,
                insertAfter = true,
                insertBefore = false,
                deletable = false,
                nonPathPieces = nonPathPieces
            )
        }

        trajectory.pieceData.forEachIndexed { i, pieceData ->
            val piece = pieceData.trajectoryPiece
            val nonPathPieces = trajectory.pieceData.drop(i + 1).takeWhile {
                it.trajectoryPiece !is MovableTrajectoryPiece
            }
            val knot = when (piece) {
                is SplinePiece -> PathKnot().apply {
                    position = piece.end.toPoint()
                    tangent = piece.tangent
                    startTangentMag = piece.endTangentMag
                    startVisible = true
                    val nextSpline = trajectory.pieceData.getOrNull(i + 1)?.trajectoryPiece as? SplinePiece
                    endVisible = nextSpline != null
                    nextSpline?.startTangentMag?.let { endTangentMag = it }
                    onChange += {
                        piece.end = it.position.toVector2d()
                        piece.tangent = it.tangent
                        piece.endTangentMag = it.startTangentMag
                        nextSpline?.startTangentMag = it.endTangentMag
                        update()
                    }
                }
                is LinePiece -> PathKnot().apply {
                    position = piece.end.toPoint()
                    tangentMode = SplineKnot.TangentMode.FIXED
                    startVisible = false
                    val nextSpline = trajectory.pieceData.getOrNull(i + 1)?.trajectoryPiece as? SplinePiece
                    endVisible = lengthMode != SplineKnot.LengthMode.FIXED_LENGTH && nextSpline != null
                    onChange += {
                        piece.end = it.position.toVector2d()
                        nextSpline?.startTangentMag = it.endTangentMag
                        update()
                    }
                }
                else -> null
            }
            if (knot != null) {
                knot.isSpecial = nonPathPieces.isNotEmpty()
                makeMenu(
                    knot,
                    trajectory.pieceData.indexOf(pieceData),
                    nonPathPieces = nonPathPieces
                )
                knots += knot
            }
        }
        children += knots
        recomputeTransforms()
    }

    private fun initializeHeadingEditing() {
        disableEditing()
        knots += HeadingKnot().apply {
            position = trajectory.startData.start.pose.vec().toPoint()
            startVisible = false
            endVisible = true
            onChange += {
                trajectory.startData.start.pose = Pose2d(it.position.toVector2d(), it.tangent)
                update()
            }
        }
        trajectory.pieceData.forEachIndexed { _, pieceData ->
            val piece = pieceData.trajectoryPiece
            if (piece !is MovableTrajectoryPiece) return@forEachIndexed
            val heading = piece.heading as? ValueHeading

            knots += HeadingKnot().apply {
                position = piece.end.toPoint()
                if (heading != null) tangent = heading.target
                else endVisible = false
                onChange += {
                    piece.end = it.position.toVector2d()
                    heading?.target = it.tangent
                    update()
                }
            }
        }
        children += knots
        recomputeTransforms()
    }

    init {
        children += pathEntity
        initializePathEditing()
    }
}