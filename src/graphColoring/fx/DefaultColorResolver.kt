package graphColoring.fx

import javafx.scene.paint.Color

class DefaultColorResolver : ColorResolver {

    override fun resolve(color: Int): Color {
        return when (color) {
            0 -> Color.GREEN
            1 -> Color.BLUE
            2 -> Color.RED
            3 -> Color.FORESTGREEN
            4 -> Color.BLUEVIOLET
            5 -> Color.BROWN
            else -> Color.BLACK
        }
    }

}