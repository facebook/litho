---
docid: dev-options
title: Developer Options
layout: docs
permalink: /docs/developer-options
---
We provide two compile time flags for visualizing the component hierarchy of your application. These are similar to Android's show view bounds internal setting but because Litho does not always use Android Views we have implemented our own to make it more helpful.

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

