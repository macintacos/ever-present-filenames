package com.github.macintacos.everpresentfilenames.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

enum class FontSource {
    UI_FONT,
    EDITOR_FONT,
    CUSTOM_FONT,
}

enum class OverlayPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

interface SettingsChangeListener {
    fun settingsChanged()

    companion object {
        val TOPIC = Topic.create("FilenameOverlaySettingsChanged", SettingsChangeListener::class.java)
    }
}

@State(
    name = "FilenameOverlaySettings",
    storages = [Storage("FilenameOverlaySettings.xml")],
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

        // Font settings
        var fontSource: String = FontSource.UI_FONT.name
        var customFontFamily: String = ""
        var fontSize: Int = 14

        // Behavior settings
        var enableProjectViewToggle: Boolean = true
        var enableGitLineStats: Boolean = true

        // Position settings
        var overlayPosition: String = OverlayPosition.BOTTOM_RIGHT.name
        var horizontalMargin: Int = 20
        var verticalMargin: Int = 20
    }

    override fun getState(): State = myState

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getFocusedBorderColorLight(): Color =
        Color(myState.focusedBorderColorRed, myState.focusedBorderColorGreen, myState.focusedBorderColorBlue)

    fun setFocusedBorderColorLight(color: Color) {
        myState.focusedBorderColorRed = color.red
        myState.focusedBorderColorGreen = color.green
        myState.focusedBorderColorBlue = color.blue
    }

    fun getFocusedBorderColorDark(): Color =
        Color(myState.focusedBorderColorDarkRed, myState.focusedBorderColorDarkGreen, myState.focusedBorderColorDarkBlue)

    fun setFocusedBorderColorDark(color: Color) {
        myState.focusedBorderColorDarkRed = color.red
        myState.focusedBorderColorDarkGreen = color.green
        myState.focusedBorderColorDarkBlue = color.blue
    }

    fun getFontSource(): FontSource =
        try {
            FontSource.valueOf(myState.fontSource)
        } catch (e: IllegalArgumentException) {
            FontSource.UI_FONT
        }

    fun setFontSource(source: FontSource) {
        myState.fontSource = source.name
    }

    fun getCustomFontFamily(): String = myState.customFontFamily

    fun setCustomFontFamily(family: String) {
        myState.customFontFamily = family
    }

    fun getFontSize(): Int = myState.fontSize

    fun setFontSize(size: Int) {
        myState.fontSize = size
    }

    fun isProjectViewToggleEnabled(): Boolean = myState.enableProjectViewToggle

    fun setProjectViewToggleEnabled(enabled: Boolean) {
        myState.enableProjectViewToggle = enabled
    }

    fun isGitLineStatsEnabled(): Boolean = myState.enableGitLineStats

    fun setGitLineStatsEnabled(enabled: Boolean) {
        myState.enableGitLineStats = enabled
    }

    fun getOverlayPosition(): OverlayPosition =
        try {
            OverlayPosition.valueOf(myState.overlayPosition)
        } catch (e: IllegalArgumentException) {
            OverlayPosition.BOTTOM_RIGHT
        }

    fun setOverlayPosition(position: OverlayPosition) {
        myState.overlayPosition = position.name
    }

    fun getHorizontalMargin(): Int = myState.horizontalMargin

    fun setHorizontalMargin(margin: Int) {
        myState.horizontalMargin = margin
    }

    fun getVerticalMargin(): Int = myState.verticalMargin

    fun setVerticalMargin(margin: Int) {
        myState.verticalMargin = margin
    }

    companion object {
        fun getInstance(): FilenameOverlaySettings = ApplicationManager.getApplication().getService(FilenameOverlaySettings::class.java)
    }
}
