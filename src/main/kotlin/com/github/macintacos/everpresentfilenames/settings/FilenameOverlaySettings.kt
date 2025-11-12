package com.github.macintacos.everpresentfilenames.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

@State(
    name = "FilenameOverlaySettings",
    storages = [Storage("FilenameOverlaySettings.xml")]
)
class FilenameOverlaySettings : PersistentStateComponent<FilenameOverlaySettings.State> {

    private var myState = State()

    class State {
        // Store RGB values separately since Color is not directly serializable
        var focusedBorderColorRed: Int = 0
        var focusedBorderColorGreen: Int = 188
        var focusedBorderColorBlue: Int = 212

        var focusedBorderColorDarkRed: Int = 0
        var focusedBorderColorDarkGreen: Int = 229
        var focusedBorderColorDarkBlue: Int = 255
    }

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getFocusedBorderColorLight(): Color {
        return Color(myState.focusedBorderColorRed, myState.focusedBorderColorGreen, myState.focusedBorderColorBlue)
    }

    fun setFocusedBorderColorLight(color: Color) {
        myState.focusedBorderColorRed = color.red
        myState.focusedBorderColorGreen = color.green
        myState.focusedBorderColorBlue = color.blue
    }

    fun getFocusedBorderColorDark(): Color {
        return Color(myState.focusedBorderColorDarkRed, myState.focusedBorderColorDarkGreen, myState.focusedBorderColorDarkBlue)
    }

    fun setFocusedBorderColorDark(color: Color) {
        myState.focusedBorderColorDarkRed = color.red
        myState.focusedBorderColorDarkGreen = color.green
        myState.focusedBorderColorDarkBlue = color.blue
    }

    companion object {
        fun getInstance(): FilenameOverlaySettings {
            return ApplicationManager.getApplication().getService(FilenameOverlaySettings::class.java)
        }
    }
}
