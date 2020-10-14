# Litho IntelliJ Plugin

## Overview
Litho IntelliJ Plugin supports better integration with Litho framework in IntelliJ IDEA. Check the
<a href='https://github.com/facebook/litho/blob/master/litho-intellij-plugin/src/main/resources/META-INF/plugin.xml'>
release notes</a> for an overview and
<a href='https://github.com/facebook/litho/blob/master/litho-intellij-plugin/CHANGELOG.md'>changelog</a> for full version history.

## Installation
The plugin is currently under development. You can install it from the
<a href='https://www.jetbrains.com/help/idea/managing-plugins.html#open-plugin-settings'>IntelliJ Marketplace</a> by searching for **Litho**.
Or you can manually build and install the latest version by following next steps:

### Gradle
1. From the litho-intellij-plugin directory run `$./buildPlugin.sh -p output_path` where `output_path` is an IDE plugin folder.  For example, in MacOS it could be `~/Library/Application\ Support/AndroidStudioX.X`
2. You will be prompted to restart the IDE so that changes will apply.
3. Verify that **Preferences - Plugins** contains Litho now. Done.
