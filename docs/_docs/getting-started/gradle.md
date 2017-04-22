<block class="gradle" />

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `build.gradle` file:

```java
dependencies {
  // ...
  // Litho
  compile 'com.facebook.litho:litho-core:0.2.0'
  compile 'com.facebook.litho:litho-widget:0.2.0'
  provided 'com.facebook.litho:litho-annotations:0.2.0'

  annotationProcessor 'com.facebook.litho:litho-processor:0.2.0'
  
  // SoLoader 
  compile 'com.facebook.soloader:soloader:0.2.0'

  // Optional
  // For debugging
  debugCompile 'com.facebook.litho:litho-stetho:0.2.0'

  // For integration with Fresco
  compile 'com.facebook.litho:litho-fresco:0.2.0'

  // For testing
  testCompile 'com.facebook.litho:litho-testing:0.2.0'
}
```
