<block class="buck" />

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `BUCK` file:

``` python
android_prebuilt_aar(
    name = "litho",
    aar = ":litho.aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho.aar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho:aar:0.1.0",
)

prebuilt_jar(
    name = "litho-annotation",
    binary_jar = ":litho-annotation.jar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-annotation.jar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-annotation:jar:0.1.0",
)

android_prebuilt_aar(
    name = "litho-widget",
    aar = ":litho-widget.aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-widget.aar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-widget:aar:0.1.0",
)

android_library(
    ...
    # Your target here
    ...
    annotation_processor_deps = [
        ":litho-annotation",
    ],
    annotation_processors = [
        "com.facebook.litho.processor.ComponentsProcessor",
    ],
    deps = [
        ':litho',
        ':litho-widget',
        ...
    ]
)
```
