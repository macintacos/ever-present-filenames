<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

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
