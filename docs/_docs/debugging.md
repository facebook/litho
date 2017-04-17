---
docid: debugging
title: Debugging
layout: docs
permalink: /docs/debugging
---

## Stetho

[Stetho](http://facebook.github.io/stetho/) is a great debugging tool for Android and we have made sure it works with Litho as well.
To enable Litho debugging in Stetho, add the following lines in the `onCreate()` method of your `Application` implementation.

```java
public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, false);
    
    Stetho.initialize(
        Stetho.newInitializerBuilder(this)
              .enableWebKitInspector(new LithoWebKitInspector(this))
              .build());
  }
}
```

This will enable full integration of Litho in stetho. After you have enabled Litho support, just start your app and navigate to `chrome://inspect` in your browser.

![Stetho start](/static/images/stetho-start.png)

Click on the inspect link for the application you would like to inspect (we are using the Litho sample app). This opens a UI inspector where you are able to inspect the `View` and `Component` hierarchy of your application.

![Stetho inspect](/static/images/stetho-inspect.png)

When inspecting a Litho component you are also given the ability to edit the contents of your UI directly from the inspector! This enables quick design iterations by tweaking margins, padding, and many other properties, without needed to re-compile or re-start the application. You can also use this to quickly test that your UI handles different lengths of text properly.

![Stetho edit](/static/images/stetho-edit.png)
