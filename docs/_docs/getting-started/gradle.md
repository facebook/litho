<block class="gradle" />

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `build.gradle` file:

```java
dependencies {
  // ...
  // Litho
  compile 'com.facebook.litho:litho-core:0.1.0'
  compile 'com.facebook.litho:litho-widget:0.1.0'
  compileOnly 'com.facebook.litho:litho-annotation:0.1.0'

  annotationProcessor 'com.facebook.litho:litho-processor:0.1.0'

  // Optional
  // For debugging
  debugCompile 'com.facebook.litho:litho-stetho:0.1.0'

  // For integration with Fresco
  debugCompile 'com.facebook.litho:litho-fresco:0.1.0'

  // For testing
  testCompile 'com.facebook.litho:litho-testing:0.1.0'
}
```
