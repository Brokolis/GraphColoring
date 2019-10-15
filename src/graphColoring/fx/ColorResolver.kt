package graphColoring.fx

import javafx.scene.paint.Color

interface ColorResolver {

    fun resolve(color: Int) : Color

}