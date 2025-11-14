package com.github.macintacos.everpresentfilenames.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class FilenameOverlaySettingsConfigurable : Configurable {

    private var settingsPanel: JPanel? = null
    private var lightModeColorButton: JButton? = null
    private var darkModeColorButton: JButton? = null
    private var lightModeColor: Color? = null
    private var darkModeColor: Color? = null

    // Font settings UI components
    private var fontSourceCombo: JComboBox<String>? = null
    private var fontFamilyCombo: JComboBox<String>? = null
    private var fontSizeSpinner: JSpinner? = null

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

        // Font settings section
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(20, 5, 5, 5)
        val fontSectionLabel = JBLabel("Font Settings")
        fontSectionLabel.font = fontSectionLabel.font.deriveFont(java.awt.Font.BOLD)
        settingsPanel!!.add(fontSectionLabel, gbc)

        // Font source combo box
        gbc.gridy = 3
        gbc.gridwidth = 1
        gbc.insets = JBUI.insets(5)
        val fontSourceLabel = JBLabel("Font source:")
        settingsPanel!!.add(fontSourceLabel, gbc)

        gbc.gridx = 1
        val fontSourceOptions = arrayOf("UI Font (Default)", "Editor Font", "Custom Font")
        fontSourceCombo = JComboBox(fontSourceOptions)
        settingsPanel!!.add(fontSourceCombo!!, gbc)

        // Font family combo box
        gbc.gridx = 0
        gbc.gridy = 4
        val fontFamilyLabel = JBLabel("Font family:")
        settingsPanel!!.add(fontFamilyLabel, gbc)

        gbc.gridx = 1
        val availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
        fontFamilyCombo = JComboBox(availableFonts)
        fontFamilyCombo!!.isEnabled = false
        settingsPanel!!.add(fontFamilyCombo!!, gbc)

        // Font size spinner
        gbc.gridx = 0
        gbc.gridy = 5
        val fontSizeLabel = JBLabel("Font size:")
        settingsPanel!!.add(fontSizeLabel, gbc)

        gbc.gridx = 1
        val fontSizeModel = SpinnerNumberModel(14, 8, 72, 1)
        fontSizeSpinner = JSpinner(fontSizeModel)
        settingsPanel!!.add(fontSizeSpinner!!, gbc)

        // Add listener to enable/disable custom font combo based on combo box selection
        val updateFontComboState = {
            fontFamilyCombo!!.isEnabled = fontSourceCombo!!.selectedIndex == 2 // "Custom Font" is index 2
        }
        fontSourceCombo!!.addActionListener { updateFontComboState() }

        // Load current settings
        fontSourceCombo!!.selectedIndex = when (settings.getFontSource()) {
            FontSource.UI_FONT -> 0
            FontSource.EDITOR_FONT -> 1
            FontSource.CUSTOM_FONT -> 2
        }

        val customFont = settings.getCustomFontFamily()
        if (customFont.isNotEmpty()) {
            fontFamilyCombo!!.selectedItem = customFont
        }

        fontSizeSpinner!!.value = settings.getFontSize()
        updateFontComboState()

        return settingsPanel!!
    }

    override fun isModified(): Boolean {
        val settings = FilenameOverlaySettings.getInstance()

        val currentFontSource = when (fontSourceCombo!!.selectedIndex) {
            0 -> FontSource.UI_FONT
            1 -> FontSource.EDITOR_FONT
            2 -> FontSource.CUSTOM_FONT
            else -> FontSource.UI_FONT
        }

        return lightModeColor != settings.getFocusedBorderColorLight() ||
                darkModeColor != settings.getFocusedBorderColorDark() ||
                currentFontSource != settings.getFontSource() ||
                fontFamilyCombo!!.selectedItem as String != settings.getCustomFontFamily() ||
                fontSizeSpinner!!.value as Int != settings.getFontSize()
    }

    override fun apply() {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor?.let { settings.setFocusedBorderColorLight(it) }
        darkModeColor?.let { settings.setFocusedBorderColorDark(it) }

        val fontSource = when (fontSourceCombo!!.selectedIndex) {
            0 -> FontSource.UI_FONT
            1 -> FontSource.EDITOR_FONT
            2 -> FontSource.CUSTOM_FONT
            else -> FontSource.UI_FONT
        }
        settings.setFontSource(fontSource)
        settings.setCustomFontFamily(fontFamilyCombo!!.selectedItem as String)
        settings.setFontSize(fontSizeSpinner!!.value as Int)

        // Notify all overlays that settings have changed
        com.intellij.openapi.application.ApplicationManager.getApplication().messageBus
            .syncPublisher(SettingsChangeListener.TOPIC)
            .settingsChanged()
    }

    override fun reset() {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor = settings.getFocusedBorderColorLight()
        darkModeColor = settings.getFocusedBorderColorDark()
        lightModeColorButton?.background = lightModeColor
        darkModeColorButton?.background = darkModeColor

        fontSourceCombo!!.selectedIndex = when (settings.getFontSource()) {
            FontSource.UI_FONT -> 0
            FontSource.EDITOR_FONT -> 1
            FontSource.CUSTOM_FONT -> 2
        }

        val customFont = settings.getCustomFontFamily()
        if (customFont.isNotEmpty()) {
            fontFamilyCombo!!.selectedItem = customFont
        }

        fontSizeSpinner!!.value = settings.getFontSize()
        fontFamilyCombo!!.isEnabled = fontSourceCombo!!.selectedIndex == 2
    }
}
