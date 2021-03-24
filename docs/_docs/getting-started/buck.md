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

android_library(
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
    provided_deps = [
        "litho-annotation",
    ],
    deps = [
        ":litho",
        ":litho-widget",
        ...
    ]
)
```

## Adding Sections to your Project


Litho comes with an optional library called Sections for declaratively building lists. You can include Sections by adding the following additional dependencies to your `BUCK` file:

```python
android_prebuilt_aar(
    name = "litho-sections",
    aar = ":litho-sections-core.aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-sections-core.aar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-sections-core:aar:{{site.litho-version}}",
)

prebuilt_jar(
    name = "litho-sections-annotation",
    binary_jar = ":litho-sections-annotation.jar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-sections-processor.jar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-sections-processor:jar:{{site.litho-version}}",
)

prebuilt_jar(
    name = "litho-sections-processor",
    binary_jar = ":litho-sections-processor.jar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-sections-annotation.jar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-sections-annotation:jar:{{site.litho-version}}",
)

android_prebuilt_aar(
    name = "litho-sections-widget",
    aar = ":litho-sections-widget.aar",
    visibility = ["PUBLIC"],
)

remote_file(
    name = "litho-sections-widget.aar",
    sha1 = "sha1here",
    url = "mvn:com.facebook.litho:litho-sections-widget:aar:{{site.litho-version}}",
)

```
Then modify your `android_library` target as such:

```python
android_library(
    ...
    # Your target here
    ...
    annotation_processor_deps = [
        ":litho-annotation",
        ":litho-processor",
        ":litho-sections-annotations",
        ":litho-sections-processor",
    ],
    annotation_processors = [
        "com.facebook.litho.specmodels.processor.ComponentsProcessor",
        "com.facebook.litho.specmodels.processor.sections.SectionsComponentProcessor",
    ],
    provided_deps = [
        "litho-annotations",
        "litho-sections-annotations",
    ],
    deps = [
        ":litho",
        ":litho-widget",
        ":litho-sections",
        ":litho-sections-widget",
        ...
    ]
)
```