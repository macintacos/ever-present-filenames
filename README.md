# Ever Present Filenames

![Build](https://github.com/macintacos/ever-present-filenames/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
Displays the current file name at the bottom-right corner of the editor.

## Why?

I don't like tabs, so I [disable them](https://www.jetbrains.com/guide/go/tips/disable-tabs/) via
the `Tab placement | None` settings toggle. However, when you do this, and you also
use [split editor windows](https://www.jetbrains.com/help/idea/using-code-editor.html#split_screen),
JetBrains IDEs do not tell you what file is currently open in a given split unless that split
currently has focus.

This completely resolves this problem by always showing you the filename at the bottom right of the
editor view, even when that editor does not have focus.

## Features

- Smart duplicate filename handling: shows distinguishing paths when multiple files with the same
  name are open
- Blue dot indicator and italic text when file has unsaved changes
- Close files directly from the filename overlap by clicking on the file icon.
- Customize the focused border color and font choices to fit in with your editor.
- Left click on filename to reveal file in Project view
- Right click for context menu with options to copy file name, relative path, or absolute path

## Configuration

Go to "Appearance" > "Ever Present Filenames" to edit the appearance of the overlay.
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  ever-present-filenames"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it
  by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download
  the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains
  Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from
  disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
