<block class="gradle" />

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `build.gradle` file:

```java
dependencies {
  // ...
  // Litho
  compile 'com.facebook.litho:litho:0.1.0'
  compile 'com.facebook.litho:litho-widget:0.1.0'
  compileOnly 'com.facebook.litho:litho-annotation:0.1.0'
  apt 'com.facebook.litho:litho-annotation:0.1.0'
}
```
