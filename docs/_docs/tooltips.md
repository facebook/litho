---
docid: tooltips
title: Tooltips
layout: docs
permalink: /docs/tooltips
---
Litho tooltip APIs provide methods for displaying a floating view anchored to a component in your hierarchy.

If you want to show an Android [PopupWindow](https://developer.android.com/reference/android/widget/PopupWindow) anchored on a component, you need to have access to the view wrapping that component to use it as an anchor. However, in Litho, most components won't be wrapped in views and even if they are you don't have access to them.

Litho provides a utility class called [LithoTooltipController](https://fblitho.com/javadoc/com/facebook/litho/LithoTooltipController.html) as a Tooltip API that allows you to show a tooltip anchored on a Component without dealing with view search.

Here's how you'd use the Tooltip API to show a tooltip on a component when that component becomes visible:

```java
@LayoutSpec
public class ComponentWithAnchorSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c) {
    return Column.create(c)
        .key("column_key")
        .child(
            Row.create(c)
                .key("row_key")
                .child(
                    Text.create(c)
                    .text("This is an anchor")
                    .key("anchor")))
        .visibleHandler(ComponentWithAnchor.onVisible(c))
        .build();
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(
      ComponentContext c,
      @Prop LithoTooltip tooltip) {
    LithoTooltipController.showTooltip(
        c,
        tooltip,
        ComponentKeyUtils.getKeyWithSeparator("column_key", "row_key","anchor"));
  }
}
```

[LithoTooltip](https://fblitho.com/javadoc/com/facebook/litho/LithoTooltip.html) is an interface that requires you to implement a method for showing a tooltip given a host view and the bounds of the anchor component relative to the host view. This enables you to use a custom tooltip implementation.

The interface forces the implementation of `showLithoTooltip`, a method that shows a tooltip given the bounds of the anchor Component inside a hosting view.

For finding the component that is used as anchor, you need to specify keys not only on the anchor but also on all containers that wrap that anchor component. You'll need to pass these keys in order from parent container to anchor to `ComponentKeyUtils.getKeyWithSeparator` to tell the framework which component the tooltip should be shown on.

For convenience, you can call `LithoTooltipController.showTooltip` on a [PopupWindow](https://developer.android.com/reference/android/widget/PopupWindow) directly without having to create a `LithoTooltip` implementation yourself. By default it shows the tooltip as a dropdown with the specified offsets. The example above would change to:
```java
@OnEvent(VisibleEvent.class)
  static void onVisible(
      ComponentContext c,
      @Prop PopupWindow tooltip) {
    LithoTooltipController.showTooltip(
        c,
        tooltip,
        ComponentKeyUtils.getKeyWithSeparator("column_key", "row_key","anchor"),
        0, /* horizontal offset */
        0 /* vertical offset */);
  }
```

At the moment the API only supports View tooltips. We might add Component tooltip support in the future if there's a need for it - contact us if that's the case for you.
