package com.amarcolini.joos.gui.rendering

import com.amarcolini.joos.geometry.Pose2d
import com.amarcolini.joos.geometry.Vector2d
import com.amarcolini.joos.gui.Global
import com.amarcolini.joos.gui.style.Theme
import com.amarcolini.joos.gui.trajectory.Vector2dStringConverter
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.text.FontWeight
import tornadofx.*

internal class PosIndicator : FixedEntity() {
    private val converter = Vector2dStringConverter()
    var pos = Vector2d()
        set(value) {
            node.text = converter.toString(value)
            field = value
        }
    override val topAnchor: Double = 10.0
    override val bottomAnchor = null
    override val leftAnchor: Double = 10.0
    override val rightAnchor = null
    override val node: Label = Label(converter.toString(pos))

    init {
        node.addClass(Theme.padding)
        Global.theme.onChange { style() }
        style()
    }

    private fun style() {
        node.style {
            fontFamily = "Jetbrains Mono"
            backgroundColor += Global.theme.value.background
            borderColor += box(Global.theme.value.editor)
            borderRadius += box(0.3.em)
            backgroundRadius += box(0.3.em)
            borderWidth += box(0.13.em)
            fontWeight = FontWeight.EXTRA_BOLD
            textFill = Global.theme.value.text
        }
    }
}