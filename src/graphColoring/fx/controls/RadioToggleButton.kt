package graphColoring.fx.controls

import javafx.scene.control.ToggleButton

class RadioToggleButton : ToggleButton() {

    override fun fire() {
        if (toggleGroup == null || !isSelected) {
            super.fire()
        }
    }

}