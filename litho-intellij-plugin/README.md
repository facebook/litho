# Litho IntelliJ Plugin

## Overview
Litho IntelliJ Plugin supports better integration with a Litho framework in IntelliJ IDEA.
Provided features are described in the release notes of
[plugin.xml](src/main/resources/META-INF/plugin.xml) file.

## Installation
Plugin is currently under development. You can manually build and install it by following these steps:

1. From the Litho project root directory run `$./gradlew :litho-intellij-plugin:jar`
This will produce a *litho-intellij-plugin.jar* file in the **litho-intellij-plugin/build/libs** directory.
2. In the IDE open **Preferences** and choose **Plugins** option in the menu on the left.
Click button **"Install plugin from disk..."** and set the path to the jar file built in the previous step.
3. You will be prompted to restart the IDE so changes will apply.
4. Verify that **Preferences - Plugins** contains Litho now. Done.