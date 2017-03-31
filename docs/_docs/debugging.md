---
docid: debugging
title: Debugging
layout: docs
permalink: /docs/debugging
---

## Stetho

[Stetho](http://facebook.github.io/stetho/) is a great debugging tool for android and we have made sure it works with Litho as well. To enable Litho support in Stetho add the following lines at the start of your `Application` subclass's `onCreate()` method.

```java
Stetho.initialize(
    Stetho.newInitializerBuilder(this)
        .enableWebKitInspector(new LithoWebKitInspector(this))
        .build());
```

This will enable full integration of Litho in stetho. After you have enabled Litho support just start your app and navigate to `chrome://inspect` in your browser.

![Stetho start](/static/images/stetho-start.png)

Click on the inspect link for the application you would like to inspect (we are using the Litho sample app). This opens a UI inspector where you are able to inspect the `View` and `Component` hierarchy of your application.

![Stetho inspect](/static/images/stetho-inspect.png)

When inspecting a Litho component you are also given the ability to edit the contents of your UI directly from the inspector! This enables quick itteration of designs by tweaking margins, padding, and many other properties without needed to re-compile or re-start the application. You can also use this to quickly test that your UI handles different lengths of text properly.

![Stetho edit](/static/images/stetho-edit.png)

## Optional flags

In addition to Stetho we also provide two compile time flags for visualizing the component hierarchy of your application. These are similar to Android's show view bounds internal setting but because Litho does not always use Android Views we have implemented our own to make it more helpful.

Within the `ComponentsConfiguration` class there are two fields which control this.

### DEBUG_HIGHLIGHT_INTERACTIVE_BOUNDS
Highlight the interactive bounds of components as well as their expanded touch bounds, if present.

### DEBUG_HIGHLIGHT_MOUNT_BOUNDS
Highlight the bounds of mounted drawables and views. Views automatically added by the framework (e.g. when a component is clickable) are highlighted with a different color.
