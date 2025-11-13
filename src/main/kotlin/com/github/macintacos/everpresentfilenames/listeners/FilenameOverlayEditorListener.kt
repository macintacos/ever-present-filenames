package com.github.macintacos.everpresentfilenames.listeners

import com.github.macintacos.everpresentfilenames.services.FilenameDisplayService
import com.github.macintacos.everpresentfilenames.ui.FilenameOverlay
import com.intellij.openapi.editor.Editor
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

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return

        // Get the file icon
        val icon = getFileIcon(editor, file)

        // Create the overlay component
        val overlay = FilenameOverlay(editor, file, icon)

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
