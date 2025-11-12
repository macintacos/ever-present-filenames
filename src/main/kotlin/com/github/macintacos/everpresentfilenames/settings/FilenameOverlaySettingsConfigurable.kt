package com.github.macintacos.everpresentfilenames.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class FilenameOverlaySettingsConfigurable : Configurable {

    private var settingsPanel: JPanel? = null
    private var lightModeColorButton: JButton? = null
    private var darkModeColorButton: JButton? = null
    private var lightModeColor: Color? = null
    private var darkModeColor: Color? = null

    override fun getDisplayName(): String {
        return "Ever Present Filenames"
    }

    override fun createComponent(): JComponent {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor = settings.getFocusedBorderColorLight()
        darkModeColor = settings.getFocusedBorderColorDark()

        settingsPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(5)

        // Light mode color picker
        val lightModeLabel = JBLabel("Focused border color (Light mode):")
        settingsPanel!!.add(lightModeLabel, gbc)

        gbc.gridx = 1
        lightModeColorButton = JButton("Choose Color")
        lightModeColorButton!!.background = lightModeColor
        lightModeColorButton!!.addActionListener {
            val newColor = JColorChooser.showDialog(
                settingsPanel,
                "Choose Focused Border Color (Light Mode)",
                lightModeColor
            )
            if (newColor != null) {
                lightModeColor = newColor
                lightModeColorButton!!.background = newColor
            }
        }
        settingsPanel!!.add(lightModeColorButton!!, gbc)

        // Dark mode color picker
        gbc.gridx = 0
        gbc.gridy = 1
        val darkModeLabel = JBLabel("Focused border color (Dark mode):")
        settingsPanel!!.add(darkModeLabel, gbc)

        gbc.gridx = 1
        darkModeColorButton = JButton("Choose Color")
        darkModeColorButton!!.background = darkModeColor
        darkModeColorButton!!.addActionListener {
            val newColor = JColorChooser.showDialog(
                settingsPanel,
                "Choose Focused Border Color (Dark Mode)",
                darkModeColor
            )
            if (newColor != null) {
                darkModeColor = newColor
                darkModeColorButton!!.background = newColor
            }
        }
        settingsPanel!!.add(darkModeColorButton!!, gbc)

        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        val settings = FilenameOverlaySettings.getInstance()
        return lightModeColor != settings.getFocusedBorderColorLight() ||
                darkModeColor != settings.getFocusedBorderColorDark()
    }

    override fun apply() {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor?.let { settings.setFocusedBorderColorLight(it) }
        darkModeColor?.let { settings.setFocusedBorderColorDark(it) }
    }

    override fun reset() {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor = settings.getFocusedBorderColorLight()
        darkModeColor = settings.getFocusedBorderColorDark()
        lightModeColorButton?.background = lightModeColor
        darkModeColorButton?.background = darkModeColor
    }
}
