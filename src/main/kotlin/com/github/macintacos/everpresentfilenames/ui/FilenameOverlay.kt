package com.github.macintacos.everpresentfilenames.ui

import com.github.macintacos.everpresentfilenames.settings.FilenameOverlaySettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent

/**
 * A custom component that displays the filename with an icon in a rounded rectangle overlay.
 * This component is positioned at the bottom-right corner of the editor.
 *
 * Features:
 * - Blue dot indicator and italic text when file has unsaved changes
 * - Cyan border when editor is focused, gray when unfocused
 * - Left click: Copy absolute file path to clipboard
 * - Right click: Context menu with options to copy file name, relative path, or absolute path
 */
class FilenameOverlay(
    private val editor: Editor,
    private val file: VirtualFile,
    private val icon: Icon?
) : JComponent() {

    private val padding = JBUI.scale(5)
    private val cornerRadius = JBUI.scale(8)
    private val margin = JBUI.scale(20) // Distance from the edge of the editor
    private val modifiedDotSize = JBUI.scale(6) // Size of the blue dot indicator for unsaved changes
    private val modifiedDotSpacing = JBUI.scale(4) // Space between dot and icon
    private var messageBusConnection: com.intellij.util.messages.MessageBusConnection? = null
    private var isEditorFocused = false

    init {
        isOpaque = false
        updatePosition()

        // Listen for editor size changes to update position
        editor.component.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent?) {
                updatePosition()
            }
        })

        // Listen for scroll events to update position when scrolling
        editor.scrollingModel.addVisibleAreaListener { _ ->
            updatePosition()
        }

        // Listen for focus changes to update border color
        editor.contentComponent.addFocusListener(object : java.awt.event.FocusListener {
            override fun focusGained(e: java.awt.event.FocusEvent?) {
                isEditorFocused = true
                repaint()
            }

            override fun focusLost(e: java.awt.event.FocusEvent?) {
                isEditorFocused = false
                repaint()
            }
        })

        // Add mouse listener for click actions
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.button == MouseEvent.BUTTON1) {
                    // Left click: Copy absolute path
                    copyToClipboard(file.path)
                    showCopiedToast("Absolute Path", file.path)
                } else if (e.button == MouseEvent.BUTTON3) {
                    // Right click: Show context menu
                    showContextMenu(e)
                }
            }
        })

        // Listen for document changes to update font style (italic for unsaved changes)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                updatePosition() // Update position in case size changes with italic font
            }
        })

        // Listen for file save events to update the UI when file is saved
        messageBusConnection = editor.project?.messageBus?.connect()
        messageBusConnection?.subscribe(FileDocumentManagerListener.TOPIC, object : FileDocumentManagerListener {
            override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) {
                // Check if this is our document
                if (document == editor.document) {
                    // Defer the update until after the save completes, so isDocumentUnsaved returns false
                    ApplicationManager.getApplication().invokeLater {
                        updatePosition() // Update to remove italic and blue dot
                    }
                }
            }
        })
    }

    /**
     * Cleans up resources when the overlay is removed
     */
    fun dispose() {
        messageBusConnection?.disconnect()
        messageBusConnection = null
    }

    /**
     * Copies text to the system clipboard and shows a toast notification
     */
    private fun copyToClipboard(text: String) {
        val stringSelection = StringSelection(text)
        CopyPasteManager.getInstance().setContents(stringSelection)
    }

    /**
     * Shows a toast notification with the copied content
     */
    private fun showCopiedToast(label: String, content: String) {
        val message = "Copied '$content' to clipboard"

        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(message, null, JBColor.background(), null)
            .setFadeoutTime(2000)
            .setHideOnClickOutside(true)
            .setHideOnAction(true)
            .setHideOnKeyOutside(true)
            .createBalloon()

        // Show the balloon above the overlay component
        balloon.show(RelativePoint(this, Point(width / 2, 0)), Balloon.Position.above)
    }

    /**
     * Darkens a color by the specified factor (0.0 = black, 1.0 = original color)
     */
    @Suppress("UseJBColor") // Returns calculated color based on theme-aware input
    private fun darkenColor(color: Color, factor: Float): Color {
        val clampedFactor = factor.coerceIn(0f, 1f)
        return Color(
            (color.red * clampedFactor).toInt().coerceIn(0, 255),
            (color.green * clampedFactor).toInt().coerceIn(0, 255),
            (color.blue * clampedFactor).toInt().coerceIn(0, 255),
            color.alpha
        )
    }

    /**
     * Shows a context menu with copy options
     */
    private fun showContextMenu(e: MouseEvent) {
        val options = listOf("Copy File Name", "Copy Relative Path", "Copy Absolute Path")

        val popup = JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<String>("Copy", options) {
                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                    if (finalChoice) {
                        when (selectedValue) {
                            "Copy File Name" -> {
                                copyToClipboard(file.name)
                                showCopiedToast("File Name", file.name)
                            }
                            "Copy Relative Path" -> {
                                val project = editor.project
                                val relativePath = if (project != null) {
                                    val projectBaseDir = project.baseDir
                                    if (projectBaseDir != null) {
                                        VfsUtil.getRelativePath(file, projectBaseDir, '/') ?: file.path
                                    } else {
                                        file.path
                                    }
                                } else {
                                    file.path
                                }
                                copyToClipboard(relativePath)
                                showCopiedToast("Relative Path", relativePath)
                            }
                            "Copy Absolute Path" -> {
                                copyToClipboard(file.path)
                                showCopiedToast("Absolute Path", file.path)
                            }
                        }
                    }
                    return null
                }
            }
        )

        // Show the popup at the mouse position
        popup.show(RelativePoint(this, Point(e.x, e.y)))
    }

    /**
     * Updates the position and size of the overlay to stick to the bottom-right corner
     */
    private fun updatePosition() {
        val preferredSize = calculatePreferredSize()
        val visibleArea = editor.scrollingModel.visibleArea

        // Position at bottom-right of the visible area with margin
        val x = visibleArea.x + visibleArea.width - preferredSize.width - margin
        val y = visibleArea.y + visibleArea.height - preferredSize.height - margin

        bounds = Rectangle(x, y, preferredSize.width, preferredSize.height)
        revalidate()
        repaint()
    }

    /**
     * Calculates the preferred size based on text and icon dimensions
     */
    private fun calculatePreferredSize(): Dimension {
        // Use italic font for size calculation if document is modified, to ensure enough space
        val baseFont = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
        val isModified = FileDocumentManager.getInstance().isDocumentUnsaved(editor.document)
        val font = if (isModified) baseFont.deriveFont(Font.ITALIC) else baseFont

        val metrics = getFontMetrics(font)
        val textWidth = metrics.stringWidth(file.name)
        val textHeight = metrics.height

        val iconWidth = icon?.iconWidth ?: 0
        val iconHeight = icon?.iconHeight ?: 0
        val iconSpacing = if (icon != null) JBUI.scale(4) else 0

        // Add space for the blue dot indicator if document is modified
        val dotWidth = if (isModified) modifiedDotSize + modifiedDotSpacing else 0

        val width = padding * 2 + dotWidth + iconWidth + iconSpacing + textWidth
        val height = padding * 2 + maxOf(textHeight, iconHeight, if (isModified) modifiedDotSize else 0)

        return Dimension(width, height)
    }

    override fun getPreferredSize(): Dimension {
        return calculatePreferredSize()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )

        // Draw rounded rectangle background - use editor background but darker
        val editorBackground = editor.colorsScheme.defaultBackground
        val darkerBackground = darkenColor(editorBackground, 0.80f) // 80% of original brightness (20% darker)
        @Suppress("UseJBColor") // Color is dynamically calculated from theme-aware editor background
        val backgroundColor = Color(
            darkerBackground.red,
            darkerBackground.green,
            darkerBackground.blue,
            230 // Slight transparency
        )
        g2d.color = backgroundColor
        g2d.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius)

        // Draw border - use user-configured color if focused, gray otherwise
        g2d.color = if (isEditorFocused) {
            val settings = FilenameOverlaySettings.getInstance()
            JBColor(
                settings.getFocusedBorderColorLight(),
                settings.getFocusedBorderColorDark()
            )
        } else {
            JBColor(
                Color(200, 200, 200, 255), // Light mode: light gray border
                Color(100, 100, 100, 255)  // Dark mode: medium gray border
            )
        }
        g2d.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius)

        // Check if document has unsaved changes
        val isModified = FileDocumentManager.getInstance().isDocumentUnsaved(editor.document)

        var currentX = padding

        // Draw blue dot indicator if document is modified
        if (isModified) {
            g2d.color = JBColor(
                Color(41, 128, 185), // Light mode: blue
                Color(100, 181, 246) // Dark mode: lighter blue
            )
            val dotY = (height - modifiedDotSize) / 2
            g2d.fillOval(currentX, dotY, modifiedDotSize, modifiedDotSize)
            currentX += modifiedDotSize + modifiedDotSpacing
        }

        // Draw icon if present
        if (icon != null) {
            val iconY = (height - icon.iconHeight) / 2
            icon.paintIcon(this, g2d, currentX, iconY)
            currentX += icon.iconWidth + JBUI.scale(4)
        }

        // Set font to editor's font, make it italic if document has unsaved changes
        val baseFont = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
        g2d.font = if (isModified) {
            baseFont.deriveFont(Font.ITALIC)
        } else {
            baseFont
        }
        g2d.color = JBColor.foreground()

        val metrics = g2d.fontMetrics

        // Draw filename text
        val textY = (height - metrics.height) / 2 + metrics.ascent
        g2d.drawString(file.name, currentX, textY)

        g2d.dispose()
    }
}
