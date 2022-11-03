package com.amarcolini.joos.gui

import com.amarcolini.joos.gui.rendering.Backgrounds
import com.amarcolini.joos.gui.style.Dark
import com.amarcolini.joos.gui.style.Light
import com.amarcolini.joos.gui.trajectory.TrajectoryEditor
import javafx.scene.control.MenuItem
import javafx.scene.control.TabPane
import javafx.scene.control.TextInputDialog
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Path
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import tornadofx.*
import java.io.File
import java.nio.file.Paths

internal class MainApp : App(MainView::class, Dark::class) {
    init {
        reloadStylesheetsOnFocus()
    }

    override fun start(stage: Stage) {
        stage.isMaximized = true
        stage.maxWidth = Screen.getPrimary().visualBounds.width
        stage.maxHeight = Screen.getPrimary().visualBounds.height
        stage.width = Screen.getPrimary().visualBounds.width
        stage.height = Screen.getPrimary().visualBounds.height
        stage.title = "Joos GUI"
        when (parameters.named["theme"]?.lowercase()) {
            "light" -> Global.theme.value = Light()
            else -> {
            }
        }
        importStylesheet(Global.theme.value::class)
        super.start(stage)
    }
}

internal class MainView : View() {
    val background = when (app.parameters.named["joos/gui/background"]?.lowercase()) {
        "freightfrenzy" -> Backgrounds.FreightFrenzy.image
        "ultimategoal" -> Backgrounds.UltimateGoal.image
        else -> Global.background
    }
    private val editor = TrajectoryEditor()

    init {
        editor.renderer.trajectory = Global.trajectory
        editor.renderer.constraints.set(Global.constraints)
        Global.background = background
    }

    override val root = vbox {
        menubar {
            menu(null, ImageView(run {
                val url = "joos/gui/logo.png"
                Global.resources?.get(url)?.get(0)?.open()?.let { Image(it, 60.0, 100.0, true, true) } ?: Image(
                    url,
                    60.0,
                    100.0,
                    true,
                    true
                )
            })).style {
                backgroundColor += Color.TRANSPARENT
            }
            menu("_Theme") {
                item("_Dark").action {
                    Global.theme.value = Dark()
                    scene.stylesheets.clear()
                    importStylesheet(Global.theme.value.externalForm)
                }
                item("_Light").action {
                    Global.theme.value = Light()
                    scene.stylesheets.clear()
                    importStylesheet(Global.theme.value.externalForm)
                }
                for (item in Global.extraThemes) {
                    item(item.key).action {
                        Global.theme.value = item.value
                        scene.stylesheets.clear()
                        importStylesheet(item.value.externalForm)
                    }
                }
            }
            menu("_Background") {
                item("_Generic").action {
                    val field = editor.renderer.fieldRenderer
                    Global.background = Backgrounds.Generic.image
                    field.draw(field.width, field.height)
                }
                item("_Freight Frenzy").action {
                    val field = editor.renderer.fieldRenderer
                    Global.background = Backgrounds.FreightFrenzy.image
                    field.draw(field.width, field.height)
                }
                item("_Ultimate Goal").action {
                    val field = editor.renderer.fieldRenderer
                    Global.background = Backgrounds.UltimateGoal.image
                    field.draw(field.width, field.height)
                }
                for (item in Global.extraBackgrounds) {
                    item(item.key).action {
                        val field = editor.renderer.fieldRenderer
                        Global.background = lazy { item.value }
                        field.draw(field.width, field.height)
                    }
                }
                item("_New +").action {
                    val file = chooseFile(
                        "Choose a Background",
                        arrayOf(
                            FileChooser.ExtensionFilter(
                                "Image Files (*.png, *.jpg, *.jpeg, *.gif, *.tif, *.tiff)",
                                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.tif", "*.tiff"
                            )
                        ), File(Paths.get("").toAbsolutePath().toUri()), FileChooserMode.Single, currentWindow
                    )
                    if (file.isEmpty()) return@action
                    val image = Image(file[0].toURI().toString())
                    val dialog = TextInputDialog(file[0].nameWithoutExtension)
                    dialog.title = "New Background"
                    dialog.headerText = "Choose a name for this background"
                    dialog.dialogPane.stylesheets += scene.stylesheets
                    dialog.dialogPane.graphic = Path()
                    val name = dialog.showAndWait()
                    if (!name.isPresent) return@action
                    Global.extraBackgrounds[name.get()] = image
                    val item = MenuItem()
                    item.text = name.get()
                    item.setOnAction {
                        val field = editor.renderer.fieldRenderer
                        Global.background = lazy { image }
                        field.draw(field.width, field.height)
                    }
                    items.add(items.size - 1, item)
                }
            }
        }
        splitpane {
            tabpane {
                tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
                tab("Trajectory") {
                    add(editor)
                }
            }
            add(editor.renderer)
            vgrow = Priority.ALWAYS
        }
    }
}

fun main(args: Array<String>) {
    launch<MainApp>(args + "--add-modules javafx.controls, javafx.graphics, javafx.base")
}