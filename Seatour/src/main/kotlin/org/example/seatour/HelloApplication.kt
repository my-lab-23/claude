package org.example.seatour

// BusAffollamentoApp.kt (Main Application)
import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage

class BusAffollamentoApp : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Annotazione Affollamento Bus"

        val controller = MainController()
        val root = controller.creaInterfaccia()

        val scene = Scene(root, 500.0, 400.0)
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main() {
    Application.launch(BusAffollamentoApp::class.java)
}
