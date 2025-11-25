# Ever Present Filenames

![Build](https://github.com/macintacos/ever-present-filenames/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->
Displays the current file name at the bottom-right corner of the editor.

## Why?

I don't like tabs, so I [disable them](https://www.jetbrains.com/guide/go/tips/disable-tabs/) via the `Tab placement | None` settings toggle. However, when you do this, and you also use [split editor windows](https://www.jetbrains.com/help/idea/using-code-editor.html#split_screen), JetBrains IDEs do not tell you what file is currently open in a given split unless that split currently has focus.

This completely resolves this problem by always showing you the filename at the bottom right of the editor view, even when that editor does not have focus.

Basically - I made this [because this issue never got traction](https://youtrack.jetbrains.com/issue/PY-78087/With-Editor-Tabs-set-to-None-there-is-no-good-way-to-see-the-names-of-all-files-in-all-splits).

## Features

- Show the filename for each visible "split" in the IDE, even when tabs have been disabled.
- Smart duplicate filename handling: shows distinguishing paths when multiple files with the same name are open
- The UI shows when there are pending unsaved changes, as well as how many lines have been added / removed based on `git diff --numstat HEAD`.
- Take the following actions using just your mouse:
  - Click on the file icon to close the file.
  - Click on the filename itself to reveal the file in the Project Outline
  - Click on the numbered diff to open the diff view for that file
  - Right-click on the filename to copy the path to the file

## Configuration

Go to "Appearance" > "Ever Present Filenames" to edit the settings for the plugin.
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
