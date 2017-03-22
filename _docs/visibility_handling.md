---
docid: visibility-handling
title: Visibility Handling
layout: docs
permalink: /docs/visibility-handling
---

### Types of Visibility Range ###

The framework currently supports four types of Visibility Event:

- Visible Event: this event is triggered when at least one pixel of the Component is visible. 

- Invisible Event: this event is triggered when the Component no longer has any pixels on the screen. 

- Focused Visible Event: this event is triggered when either the Component occupies at least half of the viewport, or, if the Component is smaller than half the viewport, when it is fully visible. 

- Full Impression Visible Event: this event is triggered when the entire Component has passed through the viewport at some point.

### Usage ###

Visibility Ranges require incremental mount [TODO: add a link here] to be enabled on the relevant Component. 

To register visibility event handlers for a component, the workflow is the same as the one used for setting any event handler. Here is an example:

```java
@LayoutSpec
public class MyLayoutSpec {

  @OnCreateLayout
  public ComponentLayout onCreateLayout(
      ComponentContext c) {
    final ComponentLayout title =
        Text.create(c)
            .text("This is MY layout spec")
            .withLayout()
            .visibleHandler(MyLayoutSpec.onTitleVisible(c))
            .invisibleHandler(MyLayoutSpec.onTitleInvisible(c))
            .build();

    final String contentString = getRandomInt() % 2 == 0 ?
        "Great layout spec" :
        "Bad layout spec";
       
    return Container.create(c)
        .direction(FlexDirection.COLUMN)
        .alignItems(Align.STRETCH)
        .child(title)
        .child(
            Text.create(c)
                .text(contentString)
                .withLayout()
                .focusedHandler(MyLayoutSpec.onContentFocused(c, contentString))
                .fullImpressionHandler(MyLayoutSpec.onContentFullImpression(c)))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  public void onTitleVisible(ComponentContext c) {
    Log.d("VisibilityRanges", "The title entered the Visible Range");
  }

  @OnEvent(InvisibleEvent.class)
  public void onTitleInvisible(ComponentContext c) {
    Log.d("VisibilityRanges", "The title is no longer visible");
  }

  @OnEvent(FocusedVisibleEvent.class)
  public void onContentFocused(
      ComponentContext c, 
      @Param String contentString) {
    Log.d(
      "VisibilityRanges",
      "The content that entered the Focused Range has value: " + contentString);
  }

  @OnEvent(FullImpressionVisibleEvent.class)
  public void onContentFullImpression(ComponentContext c) {
    Log.d("VisibilityRanges", "The content entered the Full Impression Range");
  }
};
```
