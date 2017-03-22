---
docid: getting-started
title: Getting Started
layout: docs
permalink: /docs/getting-started.html
---

## Adding Litho to your Project

You can include Litho to your Android project via Gradle by adding the following to your `build.gradle` file:

```java 
dependencies { 
  // ...

  // SoLoader dependency
  compile 'com.facebook.soloader:soloader:0.1.0+'
  // Litho
  compile 'com.facebook.litho:litho:1.0.0' 
} 
```

Litho has a dependency on [SoLoader](https://github.com/facebook/SoLoader) to help load native libraries provided by the underlying layout engine, [Yoga](https://facebook.github.io/yoga/).

## Testing your Installation

You can test your install by adding a view created with Litho to an activity.

First, initialize `SoLoader`. Your `Application` class is a good place to do this:

```java
[MyApplication.java]
public class MyApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
  }
}
```

Then, add a predefined Litho `Text` widget to an activity that displays "Hello World!":

```java
[MyActivity.java]
public class MyActivity extends Activity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentView componentView = new ComponentView(this);
    final ComponentContext context = new ComponentContext(this);

    final Component text = Text.create(context)
        .text("Hello, World!")
        .build();
    final ComponentTree componentTree = ComponentTree.create(context, text).build();

    componentView.setComponent(componentTree);

    setContentView(componentView);
  }
}
```

Run the app, you should see "Hello World!" displayed on the screen.
