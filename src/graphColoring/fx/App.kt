package graphColoring.fx

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class App : Application() {

    override fun start(primaryStage: Stage) {
        val loader = FXMLLoader(javaClass.classLoader.getResource("home.fxml"))
        val root = loader.load<Parent>()
        primaryStage.title = "Hello Graph"
        primaryStage.scene = Scene(root, 800.0, 600.0)
        primaryStage.show()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(App::class.java, *args)
        }

    }

}

