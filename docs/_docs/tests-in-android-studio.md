---
docid: tests-in-android-studio
title: Running Tests in Android Studio
layout: docs
permalink: /docs/tests-in-android-studio.html
---

The Litho repository can be used with both Buck and Gradle. The Buck plugin for
Android Studio has support for running tests by simply clicking next to a tests
method and requires no further configuration.

Gradle, however, can be a bit tricky to convince to run our tests due to
our use of native libraries. When running a test, you may see an error
message complaining about `libyoga` like this:

<img src="/static/images/android-studio-tests-0.png" style="width: 800px;">

However, you can easily fix those loading issues by modifying the test settings.
First, add `-Djava.library.path=build/jniLibs` to the "VM Options" of your run
configuration. Then, add the environment variables corresponding to your
platform:

- `DYLD_LIBRARY_PATH=build/jniLibs` for MacOS, or
- `LD_LIBRARY_PATH=build/jniLibs` for Linux.

The end result should look like this:

<img src="/static/images/android-studio-tests-1.png" style="width: 800px;">

Afterwards, you should be able to run your tests and see the results directly in
Android Studio. Added benefit: You can continuously run tests in the background
on every change or manually rerun them by pressing `Ctrl+Shift+F10` by default.

<img src="/static/images/android-studio-tests-2.png" style="width: 800px;">

When this works for you, make sure to update the default settings as well so you
don't need to enter those settings again for every new test you run:

<img src="/static/images/android-studio-tests-3.png" style="width: 800px;">
