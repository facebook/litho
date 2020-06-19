---
id: dev-options
title: Developer Options
---
For debugging Litho layouts please have a look at [Flipper](https://fbflipper.com/), a general purpose debugging tool which has first class support for Litho.

There are two compile time flags for visualizing the component hierarchy of your application. These are similar to Android's show view bounds internal setting but because Litho does not always use Android Views we have implemented our own to make it more helpful.

Within the [ComponentsConfiguration](/javadoc/com/facebook/litho/config/ComponentsConfiguration) class there are two fields which control this.

### debugHighlightInteractiveBounds
Highlight the interactive bounds of components as well as their expanded touch bounds, if present.

### debugHighlightMountBounds
Highlight the bounds of mounted drawables and views. Views automatically added by the framework (e.g. when a component is clickable) are highlighted with a different color.

These are turned off by default. If you want to turn them for your application, you can override them anywhere in your app.

```java
ComponentsConfiguration.debugHighlightInteractiveBounds = true;
ComponentsConfiguration.debugHighlightMountBounds = true;
```

For Sections debugging, you can turn on a flag in [SectionsDebug](/javadoc/com/facebook/litho/widget/SectionsDebug.html) to see logs with the Changesets that were calculated and to see which items were inserted, removed, updated or moved in your list. This can help answer questions like 'Why was my item re-rendered when nothing changed?'.
```java
SectionsDebug.ENABLED = true;
```

