These files are stubs for BUCK target. Copy them from the IDE installation folder.
For example, in MacOS it could be `/Applications/Android Studio.app/Contents/lib`.

To build plugin with BUCK use the following commands:
```
$ buck build //litho-intellij-plugin --out output_path
```

where `output_path` is an IDE plugin folder.
For example, in MacOS it could be `~/Library/Application\ Support/AndroidStudioX.X/litho-intellij-plugin.jar`
