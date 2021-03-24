## Adding Litho to your Project

We publish the Litho artifacts to Bintray's JCenter. To include Litho to your
Android project, make sure you include the reference to the repository in your `build.gradle` file:

```groovy
repositories {
  jcenter()
}
```

Then add the dependencies like this:

```groovy
dependencies {
  // ...
  // Litho
  implementation 'com.facebook.litho:litho-core:{{site.litho-version}}'
  implementation 'com.facebook.litho:litho-widget:{{site.litho-version}}'

  annotationProcessor 'com.facebook.litho:litho-processor:{{site.litho-version}}'

  // SoLoader
  implementation 'com.facebook.soloader:soloader:{{site.soloader-version}}'

  // For integration with Fresco
  implementation 'com.facebook.litho:litho-fresco:{{site.litho-version}}'

  // For testing
  testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
}
```

## Adding Sections to your Project

Litho comes with an optional library called Sections for declaratively building lists. You can include Sections by adding the following additional dependencies to your `build.gradle` file:
```groovy
dependencies {

  // Sections
  implementation 'com.facebook.litho:litho-sections-core:{{site.litho-version}}'
  implementation 'com.facebook.litho:litho-sections-widget:{{site.litho-version}}'
  compileOnly 'com.facebook.litho:litho-sections-annotations:{{site.litho-version}}'

  annotationProcessor 'com.facebook.litho:litho-sections-processor:{{site.litho-version}}'
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

Then you can access the snapshot versions of all Litho artifacts that we
publish:

```groovy
dependencies {
  // ...
  // Litho
  implementation 'com.facebook.litho:litho-core:{{site.litho-snapshot-version}}'
  implementation 'com.facebook.litho:litho-widget:{{site.litho-snapshot-version}}'

  annotationProcessor 'com.facebook.litho:litho-processor:{{site.litho-snapshot-version}}'

  // SoLoader
  implementation 'com.facebook.soloader:soloader:{{site.soloader-version}}'

  // For integration with Fresco
  implementation 'com.facebook.litho:litho-fresco:{{site.litho-snapshot-version}}'

  // For testing
  testImplementation 'com.facebook.litho:litho-testing:{{site.litho-snapshot-version}}'
}
```
