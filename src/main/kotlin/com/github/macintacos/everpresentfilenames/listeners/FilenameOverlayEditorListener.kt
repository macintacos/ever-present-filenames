package com.github.macintacos.everpresentfilenames.listeners

import com.github.macintacos.everpresentfilenames.services.FilenameDisplayService
import com.github.macintacos.everpresentfilenames.ui.FilenameOverlay
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import javax.swing.Icon

/**
 * Listens for editor creation events and adds a filename overlay to each editor.
 * The overlay displays the current file's name with its icon at the bottom-right corner.
 */
class FilenameOverlayEditorListener : EditorFactoryListener {

    private val overlayMap = mutableMapOf<Editor, FilenameOverlay>()

    // Track pending diff editors to determine which is rightmost
    private val pendingDiffEditors = mutableListOf<Pair<Editor, VirtualFile>>()

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor

        // Only add overlay to main editors and diff editors, not to other editor types
        // like commit message fields, console editors, etc.
        if (editor.editorKind != EditorKind.MAIN_EDITOR && editor.editorKind != EditorKind.DIFF) return

        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return

        // For diff editors, collect them and determine rightmost after UI is ready
        if (editor.editorKind == EditorKind.DIFF) {
            pendingDiffEditors.add(editor to file)

            // Defer processing with multiple invokeLater to ensure UI is fully ready
            ApplicationManager.getApplication().invokeLater {
                ApplicationManager.getApplication().invokeLater {
                    processPendingDiffEditor(editor, file)
                }
            }
            return
        }

        // For main editors, create overlay immediately
        createOverlay(editor, file, showGitStats = true)
    }

    /**
     * Process a diff editor - only show overlay if it's the rightmost in its diff panel.
     */
    private fun processPendingDiffEditor(editor: Editor, file: VirtualFile) {
        // Skip if already processed or not showing
        if (overlayMap.containsKey(editor)) return
        if (!editor.component.isShowing) return

        try {
            val editorX = editor.component.locationOnScreen.x

            // Find other diff editors that share a common ancestor (same diff view)
            val siblingEditors = findSiblingDiffEditors(editor)

            // Check if this editor is the rightmost among its siblings
            var isRightmost = true
            for (sibling in siblingEditors) {
                if (sibling != editor && sibling.component.isShowing) {
                    try {
                        val siblingX = sibling.component.locationOnScreen.x
                        if (siblingX > editorX) {
                            isRightmost = false
                            break
                        }
                    } catch (e: Exception) {
                        // Skip
                    }
                }
            }

            if (isRightmost) {
                createOverlay(editor, file, showGitStats = false, isDiffMode = true)
            }
        } catch (e: Exception) {
            // If we can't determine position, don't show overlay
        }

        // Remove from pending list
        pendingDiffEditors.removeAll { it.first == editor }
    }

    /**
     * Find other diff editors that share a common ancestor with this editor.
     */
    private fun findSiblingDiffEditors(editor: Editor): List<Editor> {
        val siblings = mutableListOf<Editor>()

        // Get all pending diff editors
        for ((otherEditor, _) in pendingDiffEditors) {
            if (otherEditor == editor) continue
            if (!otherEditor.component.isShowing) continue

            // Check if they share a common ancestor within reasonable depth
            if (shareCommonAncestor(editor, otherEditor, maxDepth = 10)) {
                siblings.add(otherEditor)
            }
        }

        return siblings
    }

    /**
     * Check if two editors share a common ancestor within the given depth.
     */
    private fun shareCommonAncestor(editor1: Editor, editor2: Editor, maxDepth: Int): Boolean {
        val ancestors1 = mutableSetOf<java.awt.Container>()
        var parent1: java.awt.Container? = editor1.component.parent
        var depth = 0
        while (parent1 != null && depth < maxDepth) {
            ancestors1.add(parent1)
            parent1 = parent1.parent
            depth++
        }

        var parent2: java.awt.Container? = editor2.component.parent
        depth = 0
        while (parent2 != null && depth < maxDepth) {
            if (parent2 in ancestors1) {
                return true
            }
            parent2 = parent2.parent
            depth++
        }

        return false
    }

    /**
     * Creates and registers an overlay for the given editor.
     */
    private fun createOverlay(editor: Editor, file: VirtualFile, showGitStats: Boolean, isDiffMode: Boolean = false) {
        // Get the file icon
        val icon = getFileIcon(editor, file)

        // Create the overlay component
        val overlay = FilenameOverlay(editor, file, icon, showGitStats, isDiffMode)

        // Add the overlay directly to the editor's content component
        val editorComponent = editor.contentComponent
        editorComponent.add(overlay)

        // Store the overlay for cleanup later
        overlayMap[editor] = overlay

        // Register with the display service to handle duplicate filename logic
        val displayService = FilenameDisplayService.getInstance()
        displayService.registerEditor(editor, file) { displayName ->
            overlay.updateDisplayName(displayName)
        }
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        val editor = event.editor
        val overlay = overlayMap.remove(editor) ?: return

        // Unregister from the display service
        val displayService = FilenameDisplayService.getInstance()
        displayService.unregisterEditor(editor)

        // Clean up the overlay's resources
        overlay.dispose()

        // Remove the overlay from the editor
        overlay.parent?.remove(overlay)
    }

    /**
     * Gets the icon for the given file.
     */
    private fun getFileIcon(editor: Editor, file: VirtualFile): Icon? {
        val project = editor.project ?: return file.fileType.icon

        // Try to get the PSI file for better icon resolution
        val psiFile = PsiManager.getInstance(project).findFile(file)
        return psiFile?.getIcon(0) ?: file.fileType.icon
    }
}
