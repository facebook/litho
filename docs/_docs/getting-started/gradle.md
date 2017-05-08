<block class="gradle" />

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `build.gradle` file:

```java
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
