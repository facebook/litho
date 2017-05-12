<block class="gradle" />

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `build.gradle` file:

```groovy
dependencies {
  // ...
  // Litho
  compile 'com.facebook.litho:litho-core:{{site.litho-version}}'
  compile 'com.facebook.litho:litho-widget:{{site.litho-version}}'
  provided 'com.facebook.litho:litho-annotations:{{site.litho-version}}'

  annotationProcessor 'com.facebook.litho:litho-processor:{{site.litho-version}}'

  // SoLoader
  compile 'com.facebook.soloader:soloader:{{site.litho-version}}'

  // Optional
  // For debugging
  debugCompile 'com.facebook.litho:litho-stetho:{{site.litho-version}}'

  // For integration with Fresco
  compile 'com.facebook.litho:litho-fresco:{{site.litho-version}}'

  // For testing
  testCompile 'com.facebook.litho:litho-testing:{{site.litho-version}}'
}
```

## Using Snapshot releases

> IMPORTANT: This will break and may set your house on fire. Snapshots are unsigned and
  automatically published by our CI system. Use them for testing purposes only.

First, add the Sonatype Snapshots repository to your gradle config:

```groovy
repositories {
  maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}
```

```
dependencies {
  // ...
  // Litho
  compile 'com.facebook.litho:litho-core:{{site.litho-snapshot-version}}'
  compile 'com.facebook.litho:litho-widget:{{site.litho-snapshot-version}}'
  provided 'com.facebook.litho:litho-annotations:{{site.litho-snapshot-version}}'

  annotationProcessor 'com.facebook.litho:litho-processor:{{site.litho-snapshot-version}}'

  // SoLoader
  compile 'com.facebook.soloader:soloader:{{site.litho-snapshot-version}}'

  // Optional
  // For debugging
  debugCompile 'com.facebook.litho:litho-stetho:{{site.litho-snapshot-version}}'

  // For integration with Fresco
  compile 'com.facebook.litho:litho-fresco:{{site.litho-snapshot-version}}'

  // For testing
  testCompile 'com.facebook.litho:litho-testing:{{site.litho-snapshot-version}}'
}
```
