package com.github.macintacos.everpresentfilenames.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

@State(
    name = "com.github.macintacos.everpresentfilenames.settings.FilenameOverlaySettings",
    storages = [Storage("FilenameOverlaySettings.xml")]
)
class FilenameOverlaySettings : PersistentStateComponent<FilenameOverlaySettings> {

    // Store RGB values separately since Color is not serializable
    var focusedBorderColorRed: Int = 0
    var focusedBorderColorGreen: Int = 188
    var focusedBorderColorBlue: Int = 212

    var focusedBorderColorDarkRed: Int = 0
    var focusedBorderColorDarkGreen: Int = 229
    var focusedBorderColorDarkBlue: Int = 255

    override fun getState(): FilenameOverlaySettings {
        return this
    }

    override fun loadState(state: FilenameOverlaySettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getFocusedBorderColorLight(): Color {
        return Color(focusedBorderColorRed, focusedBorderColorGreen, focusedBorderColorBlue)
    }

    fun setFocusedBorderColorLight(color: Color) {
        focusedBorderColorRed = color.red
        focusedBorderColorGreen = color.green
        focusedBorderColorBlue = color.blue
    }

    fun getFocusedBorderColorDark(): Color {
        return Color(focusedBorderColorDarkRed, focusedBorderColorDarkGreen, focusedBorderColorDarkBlue)
    }

    fun setFocusedBorderColorDark(color: Color) {
        focusedBorderColorDarkRed = color.red
        focusedBorderColorDarkGreen = color.green
        focusedBorderColorDarkBlue = color.blue
    }

    companion object {
        fun getInstance(): FilenameOverlaySettings {
            return ApplicationManager.getApplication().getService(FilenameOverlaySettings::class.java)
        }
    }
}
