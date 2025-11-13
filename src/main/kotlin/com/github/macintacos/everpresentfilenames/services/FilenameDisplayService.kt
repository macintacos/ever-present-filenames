package com.github.macintacos.everpresentfilenames.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

/**
 * Service that tracks all open editors and calculates display names for files.
 * When multiple files with the same name are open, it calculates minimal distinguishing paths.
 */
@Service(Service.Level.APP)
class FilenameDisplayService {

    private val editorFileMap = mutableMapOf<Editor, VirtualFile>()
    private val displayNameListeners = mutableMapOf<Editor, (String) -> Unit>()

    init {
        val connection = ApplicationManager.getApplication().messageBus.connect()

        // Listen for file system changes (moves, renames)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                var needsRecalculation = false

                for (event in events) {
                    // Check if the event affects any of our tracked files
                    when (event) {
                        is VFileMoveEvent -> {
                            // File was moved to a different directory
                            if (editorFileMap.values.contains(event.file)) {
                                needsRecalculation = true
                            }
                        }
                        is VFilePropertyChangeEvent -> {
                            // File property changed (e.g., renamed)
                            if (event.propertyName == VirtualFile.PROP_NAME) {
                                if (editorFileMap.values.contains(event.file)) {
                                    needsRecalculation = true
                                }
                            }
                        }
                    }
                }

                if (needsRecalculation) {
                    recalculateAllDisplayNames()
                }
            }
        })

        // Listen for file editor selection changes (tab switches, split changes)
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                recalculateAllDisplayNames()
            }
        })
    }

    /**
     * Registers an editor with its file and a callback to update the display name
     */
    fun registerEditor(editor: Editor, file: VirtualFile, onDisplayNameChanged: (String) -> Unit) {
        editorFileMap[editor] = file
        displayNameListeners[editor] = onDisplayNameChanged
        recalculateAllDisplayNames()
    }

    /**
     * Unregisters an editor when it's closed
     */
    fun unregisterEditor(editor: Editor) {
        editorFileMap.remove(editor)
        displayNameListeners.remove(editor)
        recalculateAllDisplayNames()
    }

    /**
     * Checks if an editor is currently visible in the UI
     */
    private fun isEditorVisible(editor: Editor): Boolean {
        // Get all open projects
        val projects = ProjectManager.getInstance().openProjects

        for (project in projects) {
            val fileEditorManager = FileEditorManager.getInstance(project)

            // Get all selected editors (visible in splits)
            val selectedEditors = fileEditorManager.selectedEditors
            for (selectedEditor in selectedEditors) {
                if (selectedEditor is TextEditor && selectedEditor.editor == editor) {
                    return true
                }
            }

            // Also check all editors (to handle multiple splits)
            val allEditors = fileEditorManager.allEditors
            for (fileEditor in allEditors) {
                if (fileEditor is TextEditor && fileEditor.editor == editor) {
                    // Check if this editor's component is actually showing
                    if (fileEditor.editor.component.isVisible && fileEditor.editor.component.isShowing) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Recalculates and updates display names for all registered editors
     */
    private fun recalculateAllDisplayNames() {
        // Filter to only visible editors
        val visibleEditors = editorFileMap.entries.filter { (editor, _) ->
            isEditorVisible(editor)
        }

        // Group visible files by their filename
        val filesByName = visibleEditors.groupBy { it.value.name }

        // For each group, calculate display names
        filesByName.forEach { (filename, entries) ->
            if (entries.size == 1) {
                // Only one visible file with this name, just show the filename
                val editor = entries.first().key
                displayNameListeners[editor]?.invoke(filename)
            } else {
                // Multiple visible files with the same name, need to show distinguishing paths
                val files = entries.map { it.value }
                val displayNames = calculateDistinguishingPaths(files, entries.first().key.project)

                entries.forEachIndexed { index, entry ->
                    displayNameListeners[entry.key]?.invoke(displayNames[index])
                }
            }
        }

        // For editors that are registered but not visible, just show the filename
        val invisibleEditors = editorFileMap.entries.filter { (editor, _) ->
            !isEditorVisible(editor)
        }
        invisibleEditors.forEach { (editor, file) ->
            displayNameListeners[editor]?.invoke(file.name)
        }
    }

    /**
     * Calculates distinguishing paths for files with the same name
     */
    private fun calculateDistinguishingPaths(files: List<VirtualFile>, project: Project?): List<String> {
        if (files.isEmpty()) return emptyList()
        if (files.size == 1) return listOf(files.first().name)

        val filename = files.first().name

        // Get path components for each file (excluding the filename itself)
        val pathComponents = files.map { file ->
            val pathParts = mutableListOf<String>()
            var current = file.parent
            while (current != null) {
                pathParts.add(0, current.name) // Add to front to maintain order
                current = current.parent
            }
            pathParts
        }

        // Find the common ancestor path length
        val minPathLength = pathComponents.minOfOrNull { it.size } ?: 0
        var commonPrefixLength = 0

        for (i in 0 until minPathLength) {
            val component = pathComponents.first()[i]
            if (pathComponents.all { it.size > i && it[i] == component }) {
                commonPrefixLength = i + 1
            } else {
                break
            }
        }

        // Check if any file is at the project root
        val projectBasePath = project?.basePath
        val anyAtProjectRoot = files.any { file ->
            projectBasePath != null && file.parent?.path == projectBasePath
        }

        // Build display names
        return files.mapIndexed { index, file ->
            val components = pathComponents[index]

            // Determine how many components to show after the common prefix
            val componentsToShow = components.drop(commonPrefixLength)

            if (componentsToShow.isEmpty()) {
                // File is at or very close to root
                filename
            } else {
                val pathPrefix = if (anyAtProjectRoot) {
                    // Don't use ".../" if any file is at project root
                    componentsToShow.joinToString("/")
                } else {
                    // Use ".../" prefix
                    ".../" + componentsToShow.joinToString("/")
                }
                "$pathPrefix/$filename"
            }
        }
    }

    companion object {
        fun getInstance(): FilenameDisplayService {
            return ApplicationManager.getApplication().getService(FilenameDisplayService::class.java)
        }
    }
}
