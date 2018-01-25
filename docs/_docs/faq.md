---
docid: faq
title: FAQ
layout: docs
permalink: /docs/faq
---

## Frequently Asked Questions

### Using Litho with React Native

React Native ships with its own version of Yoga which can cause conflicts when merging the 
dex files. In order to avoid this, you can instruct Gradle to exclude one of the Yoga modules.

To do this, add a section like this to your Gradle file after the dependency declaration:

```gradle
configurations.all {
  exclude group: 'com.facebook.yoga', module: 'yoga'
  exclude group: 'com.facebook.litho', module: 'litho-annotations'
  resolutionStrategy.force 'com.google.code.findbugs:jsr305:1.3.9'
}
```

For more information, check out [issue #224](https://github.com/facebook/litho/issues/224).

### Forcing newer versions of the Support Library

If you want to override the version of the support library Litho requires, you can set
the overrides in your `build.gradle`:

```gradle
configurations.all {
  resolutionStrategy {
    force 'com.android.support:appcompat-v7:26.+'
    force 'com.android.support:support-compat:26.+'
    force 'com.android.support:support-core-ui:26.+'
    force 'com.android.support:support-annotations:26.+'
    force 'com.android.support:recyclerview-v7:26.+'
  }
}
```

### Could not initialize class com.facebook.yoga.YogaNode

If you are getting this error when running a Litho unit test, go through these steps:

- Ensure Java 8 is correctly set up. If you are on a Mac, make sure that `which java`
  points to something like `/Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home/bin/java`
  and *not* `/usr/bin/java`. Otherwise, update your `$PATH` accordingly.

**For Buck**

- Make sure your tests use the `components_robolectric_test` which sets up the necessary dependencies on the native libraries.
- If your tests use PowerMock, use the `components_robolectric_powermock_test` or set the `fork_mode` manually to `per_test` which
  ensures that class loaders aren't reused across threads.
- Try `buck kill` and `buck clean`.
- If everything else fails, reboot.

**For Gradle**

- Follow the instructions under [Unit Testing - Caveats](https://fblitho.com/docs/unit-testing.html#caveats) for your setup.
- Relaunch the gradle daemon with `./gradlew --stop`.

### `@InjectProp` fails for generated components

When using parallel build systems like Buck, it can be difficult for the build
system to determine the correct order to generate sources in. This can lead to
essential type information being unavailable, making it impossible to determine
the fully qualified name. If a component A tries to use `@InjectProp` for
another generated component B, this can fail if B is part of the same
compilation unit, but sits in a different package.

The easiest workaround for this is to help the compiler by moving
either the referencing or the referenced component into a separate build module.
Splitting build modules by package is considered a good practice with Buck.
