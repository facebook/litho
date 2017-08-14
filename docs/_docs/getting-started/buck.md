<block class="buck" />

## Adding Litho to your Project

You can include Litho to your Android project via Buck by adding the following to your `BUCK` file:

```python
android_prebuilt_aar(
    name = "litho",
    aar = ":litho-core.aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-core.aar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-core:aar:{{site.litho-version}}",
)

prebuilt_jar(
    name = "litho-annotation",
    binary_jar = ":litho-annotation.jar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-processor.jar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-processor:jar:{{site.litho-version}}",
)

prebuilt_jar(
    name = "litho-processor",
    binary_jar = ":litho-processor.jar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-annotation.jar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-annotation:jar:{{site.litho-version}}",
)

android_prebuilt_aar(
    name = "litho-widget",
    aar = ":litho-widget.aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-widget.aar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-widget:aar:{{site.litho-version}}",
)

litho_android_library(
    ...
    # Your target here
    ...
    annotation_processor_deps = [
        ":litho-annotation",
        ":litho-processor",
    ],
    annotation_processors = [
        "com.facebook.litho.specmodels.processor.ComponentsProcessor",
    ],
    deps = [
        ":litho",
        ":litho-widget",
        ...
    ]
)
```
