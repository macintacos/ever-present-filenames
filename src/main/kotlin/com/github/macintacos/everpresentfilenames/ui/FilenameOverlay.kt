package com.github.macintacos.everpresentfilenames.ui

import com.github.macintacos.everpresentfilenames.settings.FilenameOverlaySettings
import com.github.macintacos.everpresentfilenames.settings.FontSource
import com.github.macintacos.everpresentfilenames.settings.SettingsChangeListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ide.projectView.ProjectView
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager
import com.intellij.util.Alarm
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.IconUtil
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.ToolTipManager
import javax.swing.UIManager

/**
 * Data class holding git line change statistics
 */
data class GitLineStats(
    val added: Int,
    val removed: Int,
    val modified: Int
) {
    fun hasChanges(): Boolean = added > 0 || removed > 0 || modified > 0
}

/**
 * A custom component that displays the filename with an icon in a rounded rectangle overlay.
 * This component is positioned at the bottom-right corner of the editor.
 *
 * Features:
 * - Blue dot indicator when file has unsaved changes
 * - Cyan border when editor is focused, gray when unfocused
 * - Left click on filename: Reveal file in Project view, or close Project view if already revealed
 * - Left click on icon: Close the file
 * - Right click: Context menu with options to copy file name, relative path, or absolute path
 * - Git line stats showing added/removed/modified line counts
 */
class FilenameOverlay(
    private val editor: Editor,
    private val file: VirtualFile,
    private val icon: Icon?
) : JComponent(), Disposable {

    companion object {
        // Track the last revealed file to enable toggle behavior
        private var lastRevealedFile: VirtualFile? = null

        // Custom git stats icons
        private val gitStatsAddedIcon = IconLoader.getIcon("/icons/gitStatsAdded.svg", FilenameOverlay::class.java)
        private val gitStatsRemovedIcon = IconLoader.getIcon("/icons/gitStatsRemoved.svg", FilenameOverlay::class.java)
        private val gitStatsModifiedIcon = IconLoader.getIcon("/icons/gitStatsModified.svg", FilenameOverlay::class.java)
    }

    private val padding = JBUI.scale(5)
    private val cornerRadius = JBUI.scale(8)
    private val margin = JBUI.scale(20) // Distance from the edge of the editor
    private val modifiedDotSize =
        JBUI.scale(6) // Size of the blue dot indicator for unsaved changes
    private val modifiedDotSpacing = JBUI.scale(4) // Space between dot and icon
    private val gitStatsSpacing = JBUI.scale(6) // Space before git stats
    private val gitStatsIconSize = JBUI.scale(12) // Size of the scaled icons
    private val gitStatsNumberSpacing = JBUI.scale(2) // Space between icon and number
    private val gitStatsItemSpacing = JBUI.scale(6) // Space between stat items
    private var messageBusConnection: com.intellij.util.messages.MessageBusConnection? = null
    private var currentGitStats: GitLineStats? = null
    private val gitStatsUpdateAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val gitStatsDebounceMs = 500 // Debounce delay in milliseconds
    private var isEditorFocused = false
    private var displayName: String = file.name
    private var iconBounds: Rectangle? = null
    private var isHoveringIcon = false
    private var textBounds: Rectangle? = null
    private var isHoveringText = false
    private var scrollOffset = 0 // Horizontal scroll offset for the text
    private var maxScrollOffset = 0 // Maximum scroll offset

    init {
        isOpaque = false
        // Enable tooltips for this component
        ToolTipManager.sharedInstance().registerComponent(this)
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
                    // Check if click is on icon (which acts as close button)
                    if (iconBounds?.contains(e.point) == true) {
                        closeFile()
                    } else {
                        // Left click on filename area: Reveal file in Project view
                        revealInProjectView()
                    }
                } else if (e.button == MouseEvent.BUTTON3) {
                    // Right click: Show context menu
                    showContextMenu(e)
                }
            }

            override fun mouseExited(e: MouseEvent) {
                // Reset hover state when mouse leaves the component
                if (isHoveringIcon || isHoveringText) {
                    isHoveringIcon = false
                    isHoveringText = false
                    cursor = Cursor.getDefaultCursor()
                    repaint()
                }
            }
        })

        // Add mouse wheel listener for horizontal scrolling
        addMouseWheelListener { e ->
            if (maxScrollOffset > 0) {
                // Scroll horizontally
                val delta = e.wheelRotation * JBUI.scale(10)
                scrollOffset = (scrollOffset + delta).coerceIn(0, maxScrollOffset)
                repaint()
            }
        }

        // Add mouse motion listener for hover effects
        addMouseMotionListener(object : java.awt.event.MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val wasHoveringIcon = isHoveringIcon
                val wasHoveringText = isHoveringText

                isHoveringIcon = iconBounds?.contains(e.point) == true
                isHoveringText = textBounds?.contains(e.point) == true

                // Icon takes precedence over text
                if (isHoveringIcon) {
                    isHoveringText = false
                }

                if (wasHoveringIcon != isHoveringIcon || wasHoveringText != isHoveringText) {
                    // Update cursor
                    cursor = if (isHoveringIcon || isHoveringText) {
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    } else {
                        Cursor.getDefaultCursor()
                    }

                    repaint()
                }
            }
        })

        // Listen for document changes to update modified indicator (blue dot)
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                // Only handle events for our specific document
                if (event.document == editor.document) {
                    updatePosition() // Update position for modified dot indicator
                    // Note: Git stats only update on file save, not on every keystroke
                }
            }
        }, this)

        // Initialize git stats
        updateGitStats()

        // Listen for file save events to update the UI when file is saved
        messageBusConnection = editor.project?.messageBus?.connect()
        val saveListener = object : FileDocumentManagerListener {
            override fun beforeDocumentSaving(document: com.intellij.openapi.editor.Document) {
                // Check if this is our document
                if (document == editor.document) {
                    // Defer the update until after the save completes
                    ApplicationManager.getApplication().invokeLater {
                        updatePosition() // Update to remove blue dot
                        // Schedule git stats update after save (with delay to ensure file is written)
                        scheduleGitStatsUpdate()
                    }
                }
            }
        }
        messageBusConnection?.subscribe(FileDocumentManagerListener.TOPIC, saveListener)

        // Listen for settings changes to update font, git stats, and recalculate sizes
        val settingsListener = object : SettingsChangeListener {
            override fun settingsChanged() {
                ApplicationManager.getApplication().invokeLater {
                    updateGitStats() // Refresh git stats (will clear if feature disabled)
                    updatePosition() // Recalculate size with new font settings
                }
            }
        }
        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(SettingsChangeListener.TOPIC, settingsListener)

        // Listen for git repository changes (commits, pulls, etc.) to update git stats
        editor.project?.messageBus?.connect(this)?.subscribe(
            GitRepository.GIT_REPO_CHANGE,
            GitRepositoryChangeListener { repository ->
                // Check if this change affects our file's repository
                val fileRepo = GitRepositoryManager.getInstance(editor.project!!).getRepositoryForFile(file)
                if (fileRepo == repository) {
                    scheduleGitStatsUpdate()
                }
            }
        )
    }

    /**
     * Cleans up resources when the overlay is removed
     */
    override fun dispose() {
        ToolTipManager.sharedInstance().unregisterComponent(this)
        messageBusConnection?.disconnect()
        messageBusConnection = null
    }

    /**
     * Override to return tooltip text when hovering over text or icon
     */
    override fun getToolTipText(event: MouseEvent?): String? {
        return when {
            isHoveringIcon -> "Close Tab"
            isHoveringText -> "Reveal file in Project Outline"
            else -> null
        }
    }

    /**
     * Gets accurate font metrics for a given font using a Graphics2D context
     * This ensures consistent measurements regardless of the font source
     */
    private fun getAccurateFontMetrics(font: Font): FontMetrics {
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.font = font
        val metrics = g2d.fontMetrics
        g2d.dispose()
        return metrics
    }

    /**
     * Scales the icon to match the font size
     * Returns a scaled version of the icon that's proportional to the text height
     * Uses IntelliJ's icon scaling utilities for sharp, high-quality results
     */
    private fun getScaledIcon(): Icon? {
        if (icon == null) return null

        // Get current font to determine appropriate icon size
        val baseFont = getBaseFont()
        val metrics = getAccurateFontMetrics(baseFont)

        // Scale icon to match text height (use ascent + descent for actual text bounds)
        val targetSize = metrics.ascent + metrics.descent

        // If icon is already the right size, return it as-is
        if (icon.iconWidth == targetSize && icon.iconHeight == targetSize) {
            return icon
        }

        // Use IntelliJ's IconUtil.scale with a Component context for better quality
        // This ensures the icon looks the same as in the Project view
        val scale = targetSize.toFloat() / icon.iconWidth.toFloat()
        return IconUtil.scale(icon, this, scale)
    }

    /**
     * Gets the base font to use for the filename overlay based on user settings
     */
    private fun getBaseFont(): java.awt.Font {
        val settings = FilenameOverlaySettings.getInstance()
        val fontSource = settings.getFontSource()
        val fontSize = settings.getFontSize()

        val baseFont = when (fontSource) {
            FontSource.UI_FONT -> {
                // Use the default UI font
                UIManager.getFont("Label.font") ?: java.awt.Font("Dialog", java.awt.Font.PLAIN, 12)
            }
            FontSource.EDITOR_FONT -> {
                // Use the editor font
                editor.colorsScheme.getFont(com.intellij.openapi.editor.colors.EditorFontType.PLAIN)
            }
            FontSource.CUSTOM_FONT -> {
                // Use custom font
                val fontFamily = settings.getCustomFontFamily()
                val defaultFont = UIManager.getFont("Label.font") ?: java.awt.Font("Dialog", java.awt.Font.PLAIN, 12)
                if (fontFamily.isNotEmpty()) {
                    java.awt.Font(fontFamily, java.awt.Font.PLAIN, defaultFont.size)
                } else {
                    defaultFont
                }
            }
        }

        // Apply font size
        return baseFont.deriveFont(fontSize.toFloat())
    }

    /**
     * Updates the display name for this overlay
     */
    fun updateDisplayName(newDisplayName: String) {
        if (displayName != newDisplayName) {
            displayName = newDisplayName
            updatePosition() // Recalculate size and repaint
        }
    }

    /**
     * Calculates the git line change statistics for the current file
     * Uses git diff --numstat to get actual git statistics
     */
    private fun calculateGitLineStats(): GitLineStats? {
        val project = editor.project ?: return null
        val settings = FilenameOverlaySettings.getInstance()
        if (!settings.isGitLineStatsEnabled()) return null

        try {
            // Get the git repository for this file
            val repositoryManager = GitRepositoryManager.getInstance(project)
            val repository = repositoryManager.getRepositoryForFile(file) ?: return null

            val repoRootPath = repository.root.path

            // Get relative path from repo root
            val absolutePath = file.path
            val relativePath = if (absolutePath.startsWith(repoRootPath)) {
                absolutePath.removePrefix(repoRootPath).removePrefix("/")
            } else {
                absolutePath
            }

            // Run git diff --numstat HEAD using ProcessBuilder
            val processBuilder = ProcessBuilder()
                .command("git", "diff", "--numstat", "HEAD", "--", relativePath)
                .directory(java.io.File(repoRootPath))
                .redirectErrorStream(false)

            val process = processBuilder.start()

            // Read output before waiting (to prevent deadlock)
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            val errorOutput = process.errorStream.bufferedReader().use { it.readText().trim() }

            val exitCode = process.waitFor()

            // If command failed or no output, return null
            if (exitCode != 0 || output.isEmpty()) {
                return null
            }

            // Parse the output: "<added>\t<removed>\t<filename>"
            val line = output.lines().firstOrNull() ?: return null
            val parts = line.split("\t")
            if (parts.size < 2) return null

            // Handle binary files (shown as "-" in git diff --numstat)
            val added = parts[0].trim().toIntOrNull() ?: return null
            val removed = parts[1].trim().toIntOrNull() ?: return null

            // Note: Git doesn't have a concept of "modified" lines
            // A changed line shows as +1 -1 (one add, one remove)
            // We show the raw git numbers without trying to infer modifications
            val modified = 0

            return if (added > 0 || removed > 0) {
                GitLineStats(added, removed, modified)
            } else {
                null
            }
        } catch (e: Exception) {
            // If anything goes wrong, just return null
            return null
        }
    }

    /**
     * Updates the git line stats and repaints if changed
     * Runs the git command in a background thread to avoid blocking the UI
     */
    private fun updateGitStats() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val newStats = calculateGitLineStats()
            ApplicationManager.getApplication().invokeLater {
                if (currentGitStats != newStats) {
                    currentGitStats = newStats
                    updatePosition() // Recalculate size and repaint
                }
            }
        }
    }

    /**
     * Schedules a debounced git stats update.
     * Cancels any pending update and schedules a new one after the debounce delay.
     */
    private fun scheduleGitStatsUpdate() {
        gitStatsUpdateAlarm.cancelAllRequests()
        gitStatsUpdateAlarm.addRequest({
            updateGitStats()
        }, gitStatsDebounceMs)
    }

    /**
     * Calculates the width needed to render a single git stat item (icon + number)
     */
    private fun calculateGitStatItemWidth(value: Int, metrics: FontMetrics): Int {
        if (value == 0) return 0
        val numberWidth = metrics.stringWidth(value.toString())
        return gitStatsIconSize + gitStatsNumberSpacing + numberWidth
    }

    /**
     * Calculates the total width needed for git stats indicator
     * Returns 0 if no changes or feature disabled
     */
    private fun calculateGitStatsWidth(metrics: FontMetrics): Int {
        val stats = currentGitStats ?: return 0
        if (!stats.hasChanges()) return 0

        var width = gitStatsSpacing // Initial spacing before "(..."

        // Opening parenthesis
        width += metrics.stringWidth("(")

        var hasContent = false

        // Added
        if (stats.added > 0) {
            width += calculateGitStatItemWidth(stats.added, metrics)
            hasContent = true
        }

        // Removed
        if (stats.removed > 0) {
            if (hasContent) width += gitStatsItemSpacing
            width += calculateGitStatItemWidth(stats.removed, metrics)
            hasContent = true
        }

        // Modified
        if (stats.modified > 0) {
            if (hasContent) width += gitStatsItemSpacing
            width += calculateGitStatItemWidth(stats.modified, metrics)
        }

        // Closing parenthesis
        width += metrics.stringWidth(")")

        return width
    }

    /**
     * Reveals the file in the Project view, or closes the Project view if the same file is already revealed
     */
    private fun revealInProjectView() {
        val project = editor.project ?: return

        ApplicationManager.getApplication().invokeLater {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val projectToolWindow = toolWindowManager.getToolWindow("Project")
            val settings = FilenameOverlaySettings.getInstance()

            // Check if toggle behavior is enabled and if we're clicking on the same file that was last revealed
            if (settings.isProjectViewToggleEnabled() &&
                lastRevealedFile == file &&
                projectToolWindow?.isVisible == true) {
                // Same file and project window is visible, so close it
                projectToolWindow.hide(null)
                lastRevealedFile = null
            } else {
                // Different file or project window is not visible, so reveal the file
                val projectView = ProjectView.getInstance(project)
                projectView.select(null, file, true)

                // Ensure the Project tool window is visible
                projectToolWindow?.activate(null)

                // Update the last revealed file
                lastRevealedFile = file
            }
        }
    }

    /**
     * Closes the file in the editor
     */
    private fun closeFile() {
        val project = editor.project ?: return

        ApplicationManager.getApplication().invokeLater {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val fileDocumentManager = FileDocumentManager.getInstance()

            // Check if document has unsaved changes
            val isModified = fileDocumentManager.isDocumentUnsaved(editor.document)

            if (isModified) {
                // Show dialog asking user what to do with unsaved changes
                val result = Messages.showYesNoCancelDialog(
                    project,
                    "File '${file.name}' has unsaved changes. Do you want to save them?",
                    "Unsaved Changes",
                    "Save",
                    "Don't Save",
                    "Cancel",
                    Messages.getQuestionIcon()
                )

                when (result) {
                    Messages.YES -> {
                        // Save the file, then close it
                        fileDocumentManager.saveDocument(editor.document)
                        fileEditorManager.closeFile(file)
                    }
                    Messages.NO -> {
                        // Close without saving
                        fileEditorManager.closeFile(file)
                    }
                    Messages.CANCEL -> {
                        // Do nothing, keep file open
                    }
                }
            } else {
                // No unsaved changes, close directly
                fileEditorManager.closeFile(file)
            }
        }
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
     * Calculates the relative luminance of a color to determine if it's light or dark
     * Returns a value between 0 (black) and 255 (white)
     */
    private fun calculateLuminance(color: Color): Double {
        // Use the relative luminance formula (perceived brightness)
        return 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    }

    /**
     * Returns an appropriate text color based on the background brightness
     */
    @Suppress("UseJBColor") // Returns calculated color based on background luminance
    private fun getContrastingTextColor(backgroundColor: Color): Color {
        val luminance = calculateLuminance(backgroundColor)
        // If luminance > 128, background is light, use dark text; otherwise use light text
        return if (luminance > 128) {
            Color(30, 30, 30)    // Dark text for light background
        } else {
            Color(220, 220, 220)  // Light text for dark background
        }
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
                                    val projectBaseDir = project.guessProjectDir()
                                    if (projectBaseDir != null) {
                                        VfsUtil.getRelativePath(file, projectBaseDir, '/')
                                            ?: file.path
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
        var x = visibleArea.x + visibleArea.width - preferredSize.width - margin
        val y = visibleArea.y + visibleArea.height - preferredSize.height - margin

        // Check if the overlay extends beyond the left edge of the visible area
        val leftEdge = visibleArea.x + margin
        val rightEdge = visibleArea.x + visibleArea.width - margin

        var actualWidth = preferredSize.width

        if (x < leftEdge) {
            // Overlay doesn't fit, need to clamp and enable scrolling
            // Calculate the maximum width that fits in the visible area
            val maxWidth = rightEdge - leftEdge
            actualWidth = minOf(preferredSize.width, maxWidth)

            // Calculate how much we need to scroll
            maxScrollOffset = preferredSize.width - actualWidth
            x = leftEdge // Clamp to left edge

            // Start scrolled all the way to the right (showing the filename, not the path)
            scrollOffset = maxScrollOffset
        } else {
            // Overlay fits, no scrolling needed
            maxScrollOffset = 0
            scrollOffset = 0
        }

        // Set both bounds and size to ensure the component actually resizes
        bounds = Rectangle(x, y, actualWidth, preferredSize.height)
        size = Dimension(actualWidth, preferredSize.height)
        revalidate()
        repaint()
    }

    /**
     * Calculates the preferred size based on text and icon dimensions
     */
    private fun calculatePreferredSize(): Dimension {
        val baseFont = getBaseFont()
        val isModified = FileDocumentManager.getInstance().isDocumentUnsaved(editor.document)

        // Use accurate Graphics2D-based font metrics for consistent measurements
        val metrics = getAccurateFontMetrics(baseFont)

        // Use TextLayout for the most accurate text bounds calculation
        // This accounts for all rendering details including kerning, ligatures, etc.
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2d.font = baseFont

        val textLayout = java.awt.font.TextLayout(displayName, baseFont, g2d.fontRenderContext)
        val textBounds = textLayout.bounds
        g2d.dispose()

        // Use advance (full width needed for rendering) plus a small buffer
        val textWidth = kotlin.math.ceil(textLayout.advance).toInt() + 1
        val textHeight = metrics.height

        // Get scaled icon that matches font size
        val scaledIcon = getScaledIcon()
        val iconWidth = scaledIcon?.iconWidth ?: 0
        val iconHeight = scaledIcon?.iconHeight ?: 0

        // Simulate the exact layout logic from paintComponent to ensure consistency
        var calculatedWidth = padding

        // Add blue dot if modified
        if (isModified) {
            calculatedWidth += modifiedDotSize + modifiedDotSpacing
        }

        // Add icon and spacing
        if (scaledIcon != null) {
            calculatedWidth += scaledIcon.iconWidth + JBUI.scale(4)
        }

        // Add text width
        calculatedWidth += textWidth

        // Add git stats width if applicable
        calculatedWidth += calculateGitStatsWidth(metrics)

        // Add right padding
        calculatedWidth += padding

        val height =
            padding * 2 + maxOf(textHeight, iconHeight, if (isModified) modifiedDotSize else 0, gitStatsIconSize)

        return Dimension(calculatedWidth, height)
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
        val darkerBackground =
            darkenColor(editorBackground, 0.80f) // 80% of original brightness (20% darker)

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

        // Save original clip and set clip region to prevent content from overflowing the rounded rectangle
        val originalClip = g2d.clip
        g2d.clip(java.awt.geom.RoundRectangle2D.Float(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            cornerRadius.toFloat(), cornerRadius.toFloat()
        ))

        // Check if document has unsaved changes
        val isModified = FileDocumentManager.getInstance().isDocumentUnsaved(editor.document)

        // Calculate positions first
        var currentX = padding

        // Calculate dot position and width
        val dotX = if (isModified) currentX else 0
        if (isModified) {
            currentX += modifiedDotSize + modifiedDotSpacing
        }

        // Calculate icon position
        val scaledIcon = getScaledIcon()
        val iconX = currentX
        val iconY = if (scaledIcon != null) (height - scaledIcon.iconHeight) / 2 else 0
        if (scaledIcon != null) {
            currentX += scaledIcon.iconWidth + JBUI.scale(4)
        }

        // Set font based on user settings
        val font = getBaseFont()
        g2d.font = font

        // Determine text color based on editor background brightness for optimal contrast
        g2d.color = getContrastingTextColor(editorBackground)

        // Use the same accurate font metrics method as in calculatePreferredSize
        val metrics = getAccurateFontMetrics(font)

        // Calculate text bounds for hover detection
        val textLayout = java.awt.font.TextLayout(displayName, font, g2d.fontRenderContext)
        val textWidth = kotlin.math.ceil(textLayout.advance).toInt()
        val textHeight = metrics.ascent + metrics.descent

        // Apply scroll offset only to text (icon remains fixed)
        val textX = currentX - scrollOffset
        val textY = (height + metrics.ascent - metrics.descent) / 2

        // Store text bounds for hover detection (accounting for scroll)
        textBounds = Rectangle(textX, (height - textHeight) / 2, textWidth, textHeight)

        // Draw hover highlight on text if hovering
        if (isHoveringText) {
            val highlightPadding = JBUI.scale(2)
            g2d.color = getContrastingTextColor(editorBackground).let { textColor ->
                @Suppress("UseJBColor")
                Color(textColor.red, textColor.green, textColor.blue, 40)
            }
            g2d.fillRoundRect(
                textX - highlightPadding,
                (height - textHeight) / 2 - highlightPadding,
                textWidth + highlightPadding * 2,
                textHeight + highlightPadding * 2,
                JBUI.scale(4),
                JBUI.scale(4)
            )
        }

        // Draw filename text - properly center vertically using actual text bounds (not line height)
        // This ensures consistent vertical centering regardless of font family/size
        g2d.color = getContrastingTextColor(editorBackground)
        g2d.drawString(displayName, textX, textY)

        // Draw git stats indicator after the filename (if applicable)
        val gitStats = currentGitStats
        if (gitStats != null && gitStats.hasChanges()) {
            var statsX = textX + textWidth + gitStatsSpacing

            // Draw opening parenthesis
            g2d.color = getContrastingTextColor(editorBackground)
            g2d.drawString("(", statsX, textY)
            statsX += metrics.stringWidth("(")

            var hasDrawnItem = false

            // Helper to draw a stat item (icon + number)
            fun drawStatItem(value: Int, icon: Icon) {
                if (value == 0) return
                if (hasDrawnItem) {
                    statsX += gitStatsItemSpacing
                }

                // Scale icon to match badge size
                val scaledIcon = IconUtil.scale(icon, this@FilenameOverlay, gitStatsIconSize.toFloat() / icon.iconWidth.toFloat())
                val iconY = (height - scaledIcon.iconHeight) / 2

                // Draw the icon
                scaledIcon.paintIcon(this@FilenameOverlay, g2d, statsX, iconY)

                // Draw the number after the icon
                statsX += scaledIcon.iconWidth + gitStatsNumberSpacing
                g2d.color = getContrastingTextColor(editorBackground)
                g2d.font = font
                g2d.drawString(value.toString(), statsX, textY)
                statsX += metrics.stringWidth(value.toString())

                hasDrawnItem = true
            }

            // Draw each stat type with custom icons
            // Green rounded square with + for additions
            drawStatItem(gitStats.added, gitStatsAddedIcon)

            // Red rounded square with - for deletions
            drawStatItem(gitStats.removed, gitStatsRemovedIcon)

            // Blue rounded square with dot for modifications
            drawStatItem(gitStats.modified, gitStatsModifiedIcon)

            // Draw closing parenthesis
            g2d.color = getContrastingTextColor(editorBackground)
            g2d.drawString(")", statsX, textY)
        }

        // Draw gradient overlay to indicate scrollable content (if there's text behind the icon)
        // Hide gradient when hovering over text or when scrolled all the way to the left
        if (scaledIcon != null && maxScrollOffset > 0 && scrollOffset > 0 && !isHoveringText) {
            val gradientWidth = JBUI.scale(50) // Wider gradient for more prominence
            val gradientStart = iconX + scaledIcon.iconWidth + JBUI.scale(2)
            val gradientEnd = gradientStart + gradientWidth

            // Determine the base color for the gradient based on hover state
            val baseColor = if (isHoveringText) {
                // When hovering text, gradient should fade from transparent to the highlight color
                getContrastingTextColor(editorBackground).let { textColor ->
                    @Suppress("UseJBColor")
                    Color(textColor.red, textColor.green, textColor.blue, 80) // Increased opacity
                }
            } else {
                // When not hovering, use a more opaque version of the background
                @Suppress("UseJBColor")
                Color(darkerBackground.red, darkerBackground.green, darkerBackground.blue, 255) // Fully opaque
            }

            val gradient = java.awt.GradientPaint(
                gradientStart.toFloat(), 0f,
                baseColor, // Opaque on the left (near icon)
                gradientEnd.toFloat(), 0f,
                Color(baseColor.red, baseColor.green, baseColor.blue, 0) // Transparent on the right
            )
            g2d.paint = gradient
            g2d.fillRect(
                gradientStart,
                (height - textHeight) / 2,
                gradientWidth,
                textHeight
            )

            // Reset paint
            g2d.paint = null
        }

        // Draw right-side gradient to indicate more content to the right
        // Hide when scrolled all the way to the right or when hovering
        if (maxScrollOffset > 0 && scrollOffset < maxScrollOffset && !isHoveringText) {
            val gradientWidth = JBUI.scale(50)
            val gradientEnd = width - 1 // Right edge (accounting for border)
            val gradientStart = gradientEnd - gradientWidth

            // Use the same base color as the left gradient
            val baseColor = Color(darkerBackground.red, darkerBackground.green, darkerBackground.blue, 255)

            val gradient = java.awt.GradientPaint(
                gradientStart.toFloat(), 0f,
                Color(baseColor.red, baseColor.green, baseColor.blue, 0), // Transparent on the left
                gradientEnd.toFloat(), 0f,
                baseColor // Opaque on the right (at edge)
            )
            g2d.paint = gradient
            g2d.fillRect(
                gradientStart,
                (height - textHeight) / 2,
                gradientWidth,
                textHeight
            )

            // Reset paint
            g2d.paint = null
        }

        // Now draw the dot and icon on top of the text (so they're not overlapped by scrolling text)

        // First, draw a background that extends from the left edge to cover any text
        // This needs to be done before drawing the dot and icon
        val borderWidth = 1
        @Suppress("UseJBColor")
        val opaqueBackground = Color(
            darkerBackground.red,
            darkerBackground.green,
            darkerBackground.blue,
            255 // Fully opaque
        )

        if (scaledIcon != null) {
            // Calculate how far the background should extend to the right of the icon
            val backgroundWidth = iconX + scaledIcon.iconWidth + JBUI.scale(2) + JBUI.scale(2)

            g2d.color = opaqueBackground
            g2d.fillRect(
                borderWidth,
                iconY - JBUI.scale(2),
                backgroundWidth - borderWidth,
                scaledIcon.iconHeight + JBUI.scale(2) * 2
            )
        }

        // Draw blue dot indicator if document is modified (doesn't scroll)
        if (isModified) {
            g2d.color = JBColor(
                Color(41, 128, 185), // Light mode: blue
                Color(100, 181, 246) // Dark mode: lighter blue
            )
            val dotY = (height - modifiedDotSize) / 2
            g2d.fillOval(dotX, dotY, modifiedDotSize, modifiedDotSize)
        }

        // Draw scaled icon if present (acts as close button, doesn't scroll)
        if (scaledIcon != null) {
            // Store icon bounds for click detection
            iconBounds = Rectangle(iconX, iconY, scaledIcon.iconWidth, scaledIcon.iconHeight)

            val highlightPadding = JBUI.scale(2)

            @Suppress("UseJBColor")
            val iconBackground = Color(
                darkerBackground.red,
                darkerBackground.green,
                darkerBackground.blue,
                255 // Fully opaque
            )

            // Draw hover highlight if hovering over icon
            if (isHoveringIcon) {
                // Draw hover highlight overlay
                g2d.color = getContrastingTextColor(editorBackground).let { textColor ->
                    @Suppress("UseJBColor")
                    Color(textColor.red, textColor.green, textColor.blue, 40)
                }
                g2d.fillRoundRect(
                    iconX - highlightPadding,
                    iconY - highlightPadding,
                    scaledIcon.iconWidth + highlightPadding * 2,
                    scaledIcon.iconHeight + highlightPadding * 2,
                    JBUI.scale(4),
                    JBUI.scale(4)
                )

                // Draw X symbol over the icon when hovering
                g2d.stroke = BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g2d.color = getContrastingTextColor(editorBackground)

                val inset = (scaledIcon.iconWidth * 0.25f).toInt()
                val x1 = iconX + inset
                val y1 = iconY + inset
                val x2 = iconX + scaledIcon.iconWidth - inset
                val y2 = iconY + scaledIcon.iconHeight - inset

                g2d.drawLine(x1, y1, x2, y2)
                g2d.drawLine(x2, y1, x1, y2)
            } else {
                // Draw normal icon when not hovering
                scaledIcon.paintIcon(this, g2d, iconX, iconY)
            }
        }

        // Restore the original clip
        g2d.clip = originalClip

        g2d.dispose()
    }
}
