package com.github.macintacos.everpresentfilenames.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.Icon
import javax.swing.JComponent

/**
 * A custom component that displays the filename with an icon in a rounded rectangle overlay.
 * This component is positioned at the bottom-right corner of the editor.
 */
class FilenameOverlay(private val editor: Editor, private val file: VirtualFile, private val icon: Icon?) : JComponent() {

    private val padding = JBUI.scale(5)
    private val cornerRadius = JBUI.scale(8)
    private val margin = JBUI.scale(10) // Distance from the edge of the editor

    init {
        isOpaque = false
        updatePosition()

        // Listen for editor size changes to update position
        editor.component.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent?) {
                updatePosition()
            }
        })
    }

    /**
     * Updates the position and size of the overlay to stick to the bottom-right corner
     */
    private fun updatePosition() {
        val preferredSize = calculatePreferredSize()
        val editorComponent = editor.component

        // Position at bottom-right with margin
        val x = editorComponent.width - preferredSize.width - margin
        val y = editorComponent.height - preferredSize.height - margin

        bounds = Rectangle(x, y, preferredSize.width, preferredSize.height)
        revalidate()
        repaint()
    }

    /**
     * Calculates the preferred size based on text and icon dimensions
     */
    private fun calculatePreferredSize(): Dimension {
        val metrics = getFontMetrics(editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN))
        val textWidth = metrics.stringWidth(file.name)
        val textHeight = metrics.height

        val iconWidth = icon?.iconWidth ?: 0
        val iconHeight = icon?.iconHeight ?: 0
        val iconSpacing = if (icon != null) JBUI.scale(4) else 0

        val width = padding * 2 + iconWidth + iconSpacing + textWidth
        val height = padding * 2 + maxOf(textHeight, iconHeight)

        return Dimension(width, height)
    }

    override fun getPreferredSize(): Dimension {
        return calculatePreferredSize()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        val g2d = g.create() as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Draw rounded rectangle background
        g2d.color = JBColor(
            Color(255, 255, 255, 230), // Light mode: white with slight transparency
            Color(60, 63, 65, 230)      // Dark mode: dark gray with slight transparency
        )
        g2d.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius)

        // Draw border
        g2d.color = JBColor(
            Color(200, 200, 200, 255), // Light mode: light gray border
            Color(100, 100, 100, 255)  // Dark mode: medium gray border
        )
        g2d.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius)

        // Set font to editor's font
        g2d.font = editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
        g2d.color = JBColor.foreground()

        val metrics = g2d.fontMetrics
        var currentX = padding

        // Draw icon if present
        if (icon != null) {
            val iconY = (height - icon.iconHeight) / 2
            icon.paintIcon(this, g2d, currentX, iconY)
            currentX += icon.iconWidth + JBUI.scale(4)
        }

        // Draw filename text
        val textY = (height - metrics.height) / 2 + metrics.ascent
        g2d.drawString(file.name, currentX, textY)

        g2d.dispose()
    }
}
