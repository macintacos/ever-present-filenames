<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

## [0.3.0] - 2025-11-14
### Added
- Font customization settings: choose between UI font, Editor font, or custom font family
- Font size adjustment setting in Appearance preferences
- File icon scaling to match selected font size for consistent visual proportions
- Tooltip display when hovering over filename showing "Reveal file in Project Outline"
- Horizontal scrolling support for long filenames that don't fit in the overlay
- Gradient indicators on left and right edges to show scrollable content direction
- Interactive close button: file icon transforms to "X" symbol on hover

### Fixed
- Vertical and horizontal spacing improvements for better visual balance
- Clipping issue where text could overflow behind the icon
- Gradient visibility: hides when hovering over text for better readability
- Gradient behavior: left gradient hidden when scrolled all the way left, right gradient hidden when scrolled all the way right

### Changed
- Close button relocated to file icon position with hover-based visibility
- Documentation updates for improved clarity

## [0.2.1] - 2025-11-13
### Added
- New plugin icon with colorful syntax-highlighted code visualization and prominent filename display

### Fixed
- Deprecated API usage: replaced `Document.addDocumentListener()` with `EditorFactory.eventMulticaster.addDocumentListener()`
- Deprecated API usage: replaced `project.baseDir` with `project.guessProjectDir()`
- Plugin icon dimensions corrected to 40x40px

### Changed
- Plugin description relocated to README for better maintainability

## [0.2.0] - 2025-11-13
### Added
- Duplicate filename detection: when multiple files with the same name are open in visible editor splits, displays distinguishing paths
- Smart path prefixes: shows "ROOT/" for files at project root, no prefix for direct children of project root, and ".../" for deeper nested paths
- Close button (X) on filename overlay to close files directly from the overlay
- Unsaved changes prompt when closing files via close button with options to Save, Don't Save, or Cancel
- Hover effects on close button: cursor changes to hand pointer and close button is highlighted with rounded background
- Close button opacity increases when hovering for better visual feedback

### Fixed
- Visibility detection to only compare visible editor splits rather than all open files
- ROOT/ prefix logic to properly exclude project directory name from displayed path
- File move and rename handling to automatically recalculate display names
- Double slash issue in ROOT paths (ROOT//test.txt → ROOT/test.txt)

## [0.1.1] - 2025-11-13
### Fixed
- Text color contrast issue in light mode by implementing dynamic text color based on editor background luminance
- Compilation error caused by duplicate variable declaration

## [0.1.0] - 2025-11-13
### Added
- Filename overlay displayed at bottom-right corner of editor with file icon
- Overlay stays visible when scrolling through the file
- Blue dot indicator and italic text when file has unsaved changes
- Border color indication: customizable color for focused editor (default: cyan), gray for unfocused editors
- Customizable focused border color via Settings → Appearance → Ever Present Filenames
- Left-click to reveal file in Project view
- Right-click context menu with options to copy filename, relative path, or absolute path
- Toast notifications when copying file paths
- Dynamic background color that adapts to editor theme (20% darker than editor background)

### Fixed
- Save event handling to properly update UI when file is saved
- Linting issues and IDE startup errors
