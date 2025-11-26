@file:Suppress("ktlint:standard:no-wildcard-imports")

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

    // Position settings UI components
    private var positionButtonGroup: ButtonGroup? = null
    private var topLeftRadio: JRadioButton? = null
    private var topRightRadio: JRadioButton? = null
    private var bottomLeftRadio: JRadioButton? = null
    private var bottomRightRadio: JRadioButton? = null
    private var horizontalMarginSpinner: JSpinner? = null
    private var verticalMarginSpinner: JSpinner? = null
    private var stickyLinesWarningLabel: JLabel? = null

    // Color settings UI components
    private var lightModeColorButton: JButton? = null
    private var darkModeColorButton: JButton? = null
    private var lightModeColor: Color? = null
    private var darkModeColor: Color? = null

    // Font settings UI components
    private var fontSourceCombo: JComboBox<String>? = null
    private var fontFamilyCombo: JComboBox<String>? = null
    private var fontSizeSpinner: JSpinner? = null

    // Behavior settings UI components
    private var projectViewToggleCheckbox: JCheckBox? = null
    private var gitLineStatsCheckbox: JCheckBox? = null

    override fun getDisplayName(): String = "Ever Present Filenames"

    override fun createComponent(): JComponent {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor = settings.getFocusedBorderColorLight()
        darkModeColor = settings.getFocusedBorderColorDark()

        settingsPanel = JPanel(GridBagLayout())
        val gbc =
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(5)
            }

        // Position settings section
        createPositionSection(settings, gbc)

        // Color settings section
        gbc.gridy++
        createColorSection(gbc)

        // Font settings section
        gbc.gridy++
        createFontSection(settings, gbc)

        // Behavior settings section
        gbc.gridy++
        createBehaviorSection(settings, gbc)

        return settingsPanel!!
    }

    private fun createSectionHeader(
        title: String,
        gbc: GridBagConstraints,
    ) {
        gbc.gridx = 0
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.insets = JBUI.insets(20, 5, 5, 5)

        val headerPanel = JPanel(GridBagLayout())
        val headerGbc =
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
            }

        val label = JBLabel(title)
        label.font = label.font.deriveFont(java.awt.Font.BOLD)
        headerPanel.add(label, headerGbc)

        headerGbc.gridy = 1
        headerGbc.insets = JBUI.insets(5, 0, 0, 0)
        val separator = JSeparator(SwingConstants.HORIZONTAL)
        headerPanel.add(separator, headerGbc)

        settingsPanel!!.add(headerPanel, gbc)

        // Reset fill and weightx for subsequent components
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
    }

    private fun createPositionSection(
        settings: FilenameOverlaySettings,
        gbc: GridBagConstraints,
    ) {
        createSectionHeader("Position Settings", gbc)

        // Position selector - visual grid with radio buttons at corners
        gbc.gridy++
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(5)
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.WEST

        val positionPanel = JPanel(GridBagLayout())
        positionPanel.border = BorderFactory.createTitledBorder("Overlay Position")

        // Create corner panel with radio buttons
        val cornerPanel = createCornerPanel()

        // Add corner panel to position panel (left side)
        val posGbc =
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                gridheight = 2
                insets = JBUI.insets(10)
                anchor = GridBagConstraints.NORTHWEST
            }
        positionPanel.add(cornerPanel, posGbc)

        // Margin settings panel (right side of corner panel)
        val marginPanel = createMarginPanel()

        // Add margin panel to position panel (right of corner panel)
        posGbc.apply {
            gridx = 1
            gridy = 0
            gridheight = 1
            anchor = GridBagConstraints.NORTHWEST
            insets = JBUI.insets(10, 20, 10, 10)
        }
        positionPanel.add(marginPanel, posGbc)

        settingsPanel!!.add(positionPanel, gbc)

        // Helper text explaining margin behavior
        gbc.gridy++
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(2, 10, 5, 5)
        val helperText =
            JBLabel("Margins are applied from the closest edges of the editor based on the chosen position.")
        helperText.foreground = JBColor.gray
        helperText.font = helperText.font.deriveFont(helperText.font.size2D - 1f)
        settingsPanel!!.add(helperText, gbc)

        // Sticky Lines warning for top positions
        gbc.gridy++
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(10, 10, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL

        // Create warning panel with proper text wrapping
        val warningPanel = JPanel(GridBagLayout())
        val warningGbc =
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
            }

        // Warning line (red text with blue link for "Sticky Lines")
        val warningLine =
            JEditorPane(
                "text/html",
                """<html><body style="font-family: ${
                    UIManager
                        .getFont(
                            "Label.font",
                        )?.family ?: "Dialog"
                }; font-size: ${UIManager.getFont("Label.font")?.size ?: 12}pt; margin: 0; padding: 0;">
            <span style="color: #CC0000; font-weight: bold;">Warning:</span>
            <span style="color: #CC0000;">Top positions may cause visual glitches when </span><a href="https://www.jetbrains.com/help/idea/sticky-lines.html" style="color: #589DF6;">Sticky Lines</a><span style="color: #CC0000;"> is enabled.</span>
            </body></html>""",
            )
        warningLine.isEditable = false
        warningLine.isOpaque = false
        warningLine.addHyperlinkListener { e ->
            if (e.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    java.awt.Desktop
                        .getDesktop()
                        .browse(e.url.toURI())
                } catch (ex: Exception) {
                    // Ignore if browser can't be opened
                }
            }
        }
        warningPanel.add(warningLine, warningGbc)

        // Explanation text (normal color with blue link at end)
        warningGbc.gridy = 1
        warningGbc.insets = JBUI.insets(5, 0, 0, 0)
        val explanationText =
            JEditorPane(
                "text/html",
                """<html><body style="font-family: ${
                    UIManager
                        .getFont(
                            "Label.font",
                        )?.family ?: "Dialog"
                }; font-size: ${
                    UIManager
                        .getFont(
                            "Label.font",
                        )?.size ?: 12
                }pt; margin: 0; padding: 0; width: 450px;">
            The overlay can scroll out of view when scrolling through the editor with this setting on.
            To avoid this, either use a bottom position for the overlay, or disable Sticky Lines in your IDE settings.
            <a href="https://www.jetbrains.com/help/idea/sticky-lines.html" style="color: #589DF6;">Learn how to disable Sticky Lines.</a>
            </body></html>""",
            )
        explanationText.isEditable = false
        explanationText.isOpaque = false
        explanationText.addHyperlinkListener { e ->
            if (e.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    java.awt.Desktop
                        .getDesktop()
                        .browse(e.url.toURI())
                } catch (ex: Exception) {
                    // Ignore if browser can't be opened
                }
            }
        }
        warningPanel.add(explanationText, warningGbc)

        stickyLinesWarningLabel = JLabel() // Dummy label to track visibility state
        stickyLinesWarningLabel!!.isVisible = false

        warningPanel.isVisible = false // Hidden by default
        settingsPanel!!.add(warningPanel, gbc)
        gbc.fill = GridBagConstraints.NONE

        // Store reference to warning panel for visibility toggling
        val warningPanelRef = warningPanel

        // Add listeners to radio buttons to show/hide warning
        val updateWarningVisibility = {
            val isTopPosition = topLeftRadio!!.isSelected || topRightRadio!!.isSelected
            warningPanelRef.isVisible = isTopPosition
        }
        topLeftRadio!!.addActionListener { updateWarningVisibility() }
        topRightRadio!!.addActionListener { updateWarningVisibility() }
        bottomLeftRadio!!.addActionListener { updateWarningVisibility() }
        bottomRightRadio!!.addActionListener { updateWarningVisibility() }

        // Load current position settings
        when (settings.getOverlayPosition()) {
            OverlayPosition.TOP_LEFT -> topLeftRadio!!.isSelected = true
            OverlayPosition.TOP_RIGHT -> topRightRadio!!.isSelected = true
            OverlayPosition.BOTTOM_LEFT -> bottomLeftRadio!!.isSelected = true
            OverlayPosition.BOTTOM_RIGHT -> bottomRightRadio!!.isSelected = true
        }
        horizontalMarginSpinner!!.value = settings.getHorizontalMargin()
        verticalMarginSpinner!!.value = settings.getVerticalMargin()

        // Update warning visibility based on loaded settings
        updateWarningVisibility()
    }

    private fun createCornerPanel(): JPanel {
        val cornerPanel = JPanel(GridBagLayout())
        cornerPanel.preferredSize = java.awt.Dimension(200, 120)
        cornerPanel.border = BorderFactory.createLineBorder(JBColor.border(), 2)
        cornerPanel.background = JBColor.background()

        val cornerGbc = GridBagConstraints()

        // Create radio buttons for each corner
        positionButtonGroup = ButtonGroup()
        topLeftRadio = JRadioButton()
        topRightRadio = JRadioButton()
        bottomLeftRadio = JRadioButton()
        bottomRightRadio = JRadioButton()

        positionButtonGroup!!.add(topLeftRadio)
        positionButtonGroup!!.add(topRightRadio)
        positionButtonGroup!!.add(bottomLeftRadio)
        positionButtonGroup!!.add(bottomRightRadio)

        // Top-left corner
        cornerGbc.apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            weighty = 0.0
            anchor = GridBagConstraints.NORTHWEST
            insets = JBUI.insets(5)
        }
        cornerPanel.add(topLeftRadio, cornerGbc)

        // Top-right corner
        cornerGbc.apply {
            gridx = 2
            anchor = GridBagConstraints.NORTHEAST
        }
        cornerPanel.add(topRightRadio, cornerGbc)

        // Center filler
        cornerGbc.apply {
            gridx = 1
            gridy = 1
            weightx = 1.0
            weighty = 1.0
            fill = GridBagConstraints.BOTH
        }
        val centerLabel = JBLabel("Editor", SwingConstants.CENTER)
        centerLabel.foreground = JBColor.gray
        cornerPanel.add(centerLabel, cornerGbc)

        // Bottom-left corner
        cornerGbc.apply {
            gridx = 0
            gridy = 2
            weightx = 0.0
            weighty = 0.0
            fill = GridBagConstraints.NONE
            anchor = GridBagConstraints.SOUTHWEST
        }
        cornerPanel.add(bottomLeftRadio, cornerGbc)

        // Bottom-right corner
        cornerGbc.apply {
            gridx = 2
            anchor = GridBagConstraints.SOUTHEAST
        }
        cornerPanel.add(bottomRightRadio, cornerGbc)

        return cornerPanel
    }

    private fun createMarginPanel(): JPanel {
        val marginPanel = JPanel(GridBagLayout())
        val marginGbc =
            GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(5)
            }

        marginPanel.add(JBLabel("Horizontal margin:"), marginGbc)

        marginGbc.gridx = 1
        val hMarginModel = SpinnerNumberModel(20, 0, 200, 5)
        horizontalMarginSpinner = JSpinner(hMarginModel)
        marginPanel.add(horizontalMarginSpinner, marginGbc)

        marginGbc.apply {
            gridx = 0
            gridy = 1
        }
        marginPanel.add(JBLabel("Vertical margin:"), marginGbc)

        marginGbc.gridx = 1
        val vMarginModel = SpinnerNumberModel(20, 0, 200, 5)
        verticalMarginSpinner = JSpinner(vMarginModel)
        marginPanel.add(verticalMarginSpinner, marginGbc)

        return marginPanel
    }

    private fun createColorSection(gbc: GridBagConstraints) {
        createSectionHeader("Color Settings", gbc)

        // Light mode color picker
        gbc.gridy++
        gbc.gridwidth = 1
        gbc.insets = JBUI.insets(5)
        val lightModeLabel = JBLabel("Focused border color (Light mode):")
        settingsPanel!!.add(lightModeLabel, gbc)

        gbc.gridx = 1
        lightModeColorButton = JButton("Choose Color")
        lightModeColorButton!!.background = lightModeColor
        lightModeColorButton!!.addActionListener {
            val newColor =
                JColorChooser.showDialog(
                    settingsPanel,
                    "Choose Focused Border Color (Light Mode)",
                    lightModeColor,
                )
            if (newColor != null) {
                lightModeColor = newColor
                lightModeColorButton!!.background = newColor
            }
        }
        settingsPanel!!.add(lightModeColorButton!!, gbc)

        // Dark mode color picker
        gbc.gridx = 0
        gbc.gridy++
        val darkModeLabel = JBLabel("Focused border color (Dark mode):")
        settingsPanel!!.add(darkModeLabel, gbc)

        gbc.gridx = 1
        darkModeColorButton = JButton("Choose Color")
        darkModeColorButton!!.background = darkModeColor
        darkModeColorButton!!.addActionListener {
            val newColor =
                JColorChooser.showDialog(
                    settingsPanel,
                    "Choose Focused Border Color (Dark Mode)",
                    darkModeColor,
                )
            if (newColor != null) {
                darkModeColor = newColor
                darkModeColorButton!!.background = newColor
            }
        }
        settingsPanel!!.add(darkModeColorButton!!, gbc)
    }

    private fun createFontSection(
        settings: FilenameOverlaySettings,
        gbc: GridBagConstraints,
    ) {
        createSectionHeader("Font Settings", gbc)

        // Font source combo box
        gbc.gridy++
        gbc.gridx = 0
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
        gbc.gridy++
        val fontFamilyLabel = JBLabel("Font family:")
        settingsPanel!!.add(fontFamilyLabel, gbc)

        gbc.gridx = 1
        val availableFonts =
            GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames
        fontFamilyCombo = JComboBox(availableFonts)
        fontFamilyCombo!!.isEnabled = false
        settingsPanel!!.add(fontFamilyCombo!!, gbc)

        // Font size spinner
        gbc.gridx = 0
        gbc.gridy++
        val fontSizeLabel = JBLabel("Font size:")
        settingsPanel!!.add(fontSizeLabel, gbc)

        gbc.gridx = 1
        val fontSizeModel = SpinnerNumberModel(14, 8, 72, 1)
        fontSizeSpinner = JSpinner(fontSizeModel)
        settingsPanel!!.add(fontSizeSpinner!!, gbc)

        // Add listener to enable/disable custom font combo based on combo box selection
        val updateFontComboState = {
            fontFamilyCombo!!.isEnabled =
                fontSourceCombo!!.selectedIndex == 2 // "Custom Font" is index 2
        }
        fontSourceCombo!!.addActionListener { updateFontComboState() }

        // Load current settings
        fontSourceCombo!!.selectedIndex =
            when (settings.getFontSource()) {
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
    }

    private fun createBehaviorSection(
        settings: FilenameOverlaySettings,
        gbc: GridBagConstraints,
    ) {
        createSectionHeader("Behavior Settings", gbc)

        // Project view toggle checkbox
        gbc.gridy++
        gbc.gridx = 0
        gbc.gridwidth = 2
        gbc.insets = JBUI.insets(5)
        projectViewToggleCheckbox =
            JCheckBox("Enable Project view toggle (click filename to close if already revealed)")
        projectViewToggleCheckbox!!.isSelected = settings.isProjectViewToggleEnabled()
        settingsPanel!!.add(projectViewToggleCheckbox!!, gbc)

        // Git line stats checkbox
        gbc.gridy++
        gitLineStatsCheckbox = JCheckBox("Show git line change statistics (+added / -removed)")
        gitLineStatsCheckbox!!.isSelected = settings.isGitLineStatsEnabled()
        settingsPanel!!.add(gitLineStatsCheckbox!!, gbc)
    }

    override fun isModified(): Boolean {
        val settings = FilenameOverlaySettings.getInstance()

        val currentFontSource =
            when (fontSourceCombo!!.selectedIndex) {
                0 -> FontSource.UI_FONT
                1 -> FontSource.EDITOR_FONT
                2 -> FontSource.CUSTOM_FONT
                else -> FontSource.UI_FONT
            }

        val currentPosition =
            when {
                topLeftRadio!!.isSelected -> OverlayPosition.TOP_LEFT
                topRightRadio!!.isSelected -> OverlayPosition.TOP_RIGHT
                bottomLeftRadio!!.isSelected -> OverlayPosition.BOTTOM_LEFT
                else -> OverlayPosition.BOTTOM_RIGHT
            }

        return lightModeColor != settings.getFocusedBorderColorLight() ||
            darkModeColor != settings.getFocusedBorderColorDark() ||
            currentFontSource != settings.getFontSource() ||
            fontFamilyCombo!!.selectedItem as String != settings.getCustomFontFamily() ||
            fontSizeSpinner!!.value as Int != settings.getFontSize() ||
            projectViewToggleCheckbox!!.isSelected != settings.isProjectViewToggleEnabled() ||
            gitLineStatsCheckbox!!.isSelected != settings.isGitLineStatsEnabled() ||
            currentPosition != settings.getOverlayPosition() ||
            horizontalMarginSpinner!!.value as Int != settings.getHorizontalMargin() ||
            verticalMarginSpinner!!.value as Int != settings.getVerticalMargin()
    }

    override fun apply() {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor?.let { settings.setFocusedBorderColorLight(it) }
        darkModeColor?.let { settings.setFocusedBorderColorDark(it) }

        val fontSource =
            when (fontSourceCombo!!.selectedIndex) {
                0 -> FontSource.UI_FONT
                1 -> FontSource.EDITOR_FONT
                2 -> FontSource.CUSTOM_FONT
                else -> FontSource.UI_FONT
            }
        settings.setFontSource(fontSource)
        settings.setCustomFontFamily(fontFamilyCombo!!.selectedItem as String)
        settings.setFontSize(fontSizeSpinner!!.value as Int)
        settings.setProjectViewToggleEnabled(projectViewToggleCheckbox!!.isSelected)
        settings.setGitLineStatsEnabled(gitLineStatsCheckbox!!.isSelected)

        // Save position settings
        val position =
            when {
                topLeftRadio!!.isSelected -> OverlayPosition.TOP_LEFT
                topRightRadio!!.isSelected -> OverlayPosition.TOP_RIGHT
                bottomLeftRadio!!.isSelected -> OverlayPosition.BOTTOM_LEFT
                else -> OverlayPosition.BOTTOM_RIGHT
            }
        settings.setOverlayPosition(position)
        settings.setHorizontalMargin(horizontalMarginSpinner!!.value as Int)
        settings.setVerticalMargin(verticalMarginSpinner!!.value as Int)

        // Notify all overlays that settings have changed
        com.intellij.openapi.application.ApplicationManager
            .getApplication()
            .messageBus
            .syncPublisher(SettingsChangeListener.TOPIC)
            .settingsChanged()
    }

    override fun reset() {
        val settings = FilenameOverlaySettings.getInstance()
        lightModeColor = settings.getFocusedBorderColorLight()
        darkModeColor = settings.getFocusedBorderColorDark()
        lightModeColorButton?.background = lightModeColor
        darkModeColorButton?.background = darkModeColor

        fontSourceCombo!!.selectedIndex =
            when (settings.getFontSource()) {
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

        projectViewToggleCheckbox!!.isSelected = settings.isProjectViewToggleEnabled()
        gitLineStatsCheckbox!!.isSelected = settings.isGitLineStatsEnabled()

        // Reset position settings
        when (settings.getOverlayPosition()) {
            OverlayPosition.TOP_LEFT -> topLeftRadio!!.isSelected = true
            OverlayPosition.TOP_RIGHT -> topRightRadio!!.isSelected = true
            OverlayPosition.BOTTOM_LEFT -> bottomLeftRadio!!.isSelected = true
            OverlayPosition.BOTTOM_RIGHT -> bottomRightRadio!!.isSelected = true
        }
        horizontalMarginSpinner!!.value = settings.getHorizontalMargin()
        verticalMarginSpinner!!.value = settings.getVerticalMargin()
    }
}
