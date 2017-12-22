---
docid: visibility-handling
title: Visibility Handling
layout: docs
permalink: /docs/visibility-handling
---

### Types of Visibility Range ###

The framework currently supports four types of Visibility Event:

- [Visible Event](/javadoc/com/facebook/litho/VisibleEvent): this event is triggered when at least one pixel of the Component is visible.
- [Invisible Event](/javadoc/com/facebook/litho/InvisibleEvent): this event is triggered when the Component no longer has any pixels on the screen.
- [Focused Visible Event](/javadoc/com/facebook/litho/FocusedVisibleEvent): this event is triggered when either the Component occupies at least half of the viewport, or, if the Component is smaller than half the viewport, when it is fully visible.
- [Full Impression Visible Event](/javadoc/com/facebook/litho/FullImpressionVisibleEvent): this event is triggered when the entire Component has passed through the viewport at some point.

### Usage ###

Visibility ranges require [incremental mount](/docs/inc-mount#manual-incremental-mount) to be enabled on the relevant Component.

To register visibility event handlers for a component you can follow the same [steps](/docs/events-overview) as for setting any other event handler. 

Here is an example:

```java
@LayoutSpec
class MyLayoutSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {

    return Column.create(c)
        .alignItems(YogaAlign.STRETCH)
        .child(Text.create(c)
            .text("This is MY layout spec")
            .visibleHandler(MyLayout.onTitleVisible(c))
            .invisibleHandler(MyLayout.onTitleInvisible(c))
            .focusedHandler(MyLayout.onComponentFocused(c, "someStringParam"))
            .fullImpressionHandler(MyLayout.onComponentFullImpression(c)))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onTitleVisible(ComponentContext c) {
    Log.d("VisibilityRanges", "The title entered the Visible Range");
  }

  @OnEvent(InvisibleEvent.class)
  static void onTitleInvisible(ComponentContext c) {
    Log.d("VisibilityRanges", "The title is no longer visible");
  }

  @OnEvent(FocusedVisibleEvent.class)
  static void onComponentFocused(
      ComponentContext c,
      @Param String stringParam) {
    Log.d(
        "VisibilityRanges",
        "The component is focused with param: " + stringParam);
  }

  @OnEvent(FullImpressionVisibleEvent.class)
  static void onComponentFullImpression(ComponentContext c) {
    Log.d("VisibilityRanges", "The component has logged a full impression");
  }
}
```
