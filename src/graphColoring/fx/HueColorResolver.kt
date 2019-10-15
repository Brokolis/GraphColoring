package graphColoring.fx

import javafx.scene.paint.Color

/**
 * Generates colors based on hue.
 */
class HueColorResolver : ColorResolver {

    val n0: Int = 3
    val step0: Double = 360.0 / n0

    override fun resolve(color: Int): Color {
        if (color < 0) return Color.BLACK

        fun n(i: Int) = if (i == 0) n0 else n0 * pow(2, i - 1)
        fun step(i: Int) = if (i == 0) step0 else step0 / pow(2, i - 1)
        fun offset(i: Int): Double = if (i == 0) 0.0 else offset(i - 1) + pow(-1, i + 1) * step(i) / 2.0

        val i = log2(color / n0)
        val m = color % n(i)

        val hue = offset(i) + step(i) * m

        return Color.hsb(hue, 0.8, 0.5)
    }

    private fun log2(n: Int): Int {
        var i = 0
        var e = 1

        while (n >= e) {
            i++
            e *= 2
        }

        return i
    }

    private fun pow(x: Int, exp: Int): Int {
        var result = 1

        for (i in 1..exp) {
            result *= x
        }

        return result
    }

}