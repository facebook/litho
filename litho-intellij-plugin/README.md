# Litho IntelliJ Plugin

## Overview
Litho IntelliJ Plugin supports better integration with Litho framework in IntelliJ IDEA. Check the
<a href='https://github.com/facebook/litho/blob/master/litho-intellij-plugin/src/main/resources/META-INF/plugin.xml'>
release notes</a> for the overview of features and
<a href='https://github.com/facebook/litho/blob/master/litho-intellij-plugin/CHANGELOG.md'>changelog</a> for full version history.
## Installation
The plugin is currently under development. You could manually build and install it by following the next steps:
### BUCK
1. From the Litho project root directory run `$ buck build //litho-intellij-plugin --out output_path`
where `output_path` is an IDE plugin folder.  For example, in MacOS it could be
`~/Library/Application\ Support/AndroidStudioX.X/litho-intellij-plugin.jar`
2. You will be prompted to restart the IDE so that changes will apply.
3. Verify that  **Preferences - Plugins**  contains Litho now. Done.

### Gradle
1. From the Litho project root directory run `$./gradlew :litho-intellij-plugin:jar`
This will produce a *litho-intellij-plugin.jar* file in the **litho-intellij-plugin/build/libs** directory.
2. In the IDE open **Preferences** and choose **Plugins** option in the menu on the left.
Click button **"Install plugin from disk..."** and set the path to the jar file built in the previous step.
3. You will be prompted to restart the IDE so that changes will apply.
4. Verify that **Preferences - Plugins** contains Litho now. Done.