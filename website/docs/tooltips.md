---
id: tooltips
title: Tooltips
---
Litho tooltip APIs provide methods for displaying a floating view anchored to a component in your hierarchy.

If you want to show an Android [PopupWindow](https://developer.android.com/reference/android/widget/PopupWindow) anchored on a component, you need to have access to the view wrapping that component to use it as an anchor. However, in Litho, most components won't be wrapped in views and even if they are you don't have access to them.

Litho provides a utility class called [LithoTooltipController](pathname:///javadoc/com/facebook/litho/LithoTooltipController.html) as a Tooltip API that allows you to show a tooltip anchored on a Component without dealing with view search.

Here's how you'd use the Tooltip API to show a tooltip on a component 1. when that component becomes visible and 2. on a click event:

```java
@LayoutSpec
public class TooltipTriggerExampleComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    final Handle anchorHandle = new Handle();
    return Column.create(c)
        .child(
            Text.create(c)
                .text("Click to Trigger show tooltip")
                .clickHandler(TooltipTriggerExampleComponent.onClick(c, anchorHandle)))
        .child(Text.create(c).text("Tooltip anchor").handle(anchorHandle))
        .visibleHandler(TooltipTriggerExampleComponent.onVisible(c, anchorHandle))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @Param Handle anchorHandle) {
    TooltipTriggerExampleComponentSpec.showToolTip(c, anchorHandle);
  }

  @OnEvent(VisibleEvent.class)
  static void onVisible(ComponentContext c, @Param Handle anchorHandle) {
    // Show a tooltip when the component becomes visible.
    // NB: Incremental mount must be enabled for the component to receive visibility callbacks.
    TooltipTriggerExampleComponentSpec.showToolTip(c, anchorHandle);
  }

  static void showToolTip(ComponentContext c, Handle anchorHandle) {
    final LithoTooltip tooltip = /* Provide an implementation of LithoTooltip or PopupWindow */;
    LithoTooltipController.showTooltipOnHandle(c, tooltip, anchorHandle);
  }
}
```

[LithoTooltip](pathname:///javadoc/com/facebook/litho/LithoTooltip.html) is an interface that requires you to implement a method for showing a tooltip given a host view and the bounds of the anchor component relative to the host view. This enables you to use a custom tooltip implementation.

The interface forces the implementation of `showLithoTooltip`, a method that shows a tooltip given the bounds of the anchor Component inside a hosting view.

For finding the component that is used as anchor, you need to specify a handle on the anchor component.

For convenience, you can call `LithoTooltipController.showTooltip` on a [PopupWindow](https://developer.android.com/reference/android/widget/PopupWindow) directly without having to create a `LithoTooltip` implementation yourself. By default it shows the tooltip as a dropdown with the specified offsets. The example above would change to:
```java
@OnEvent(VisibleEvent.class)
static void onVisible(
    ComponentContext c,
    @Prop PopupWindow tooltip,
    @State Handle anchorHandle) {
  LithoTooltipController.showTooltipOnHandle(
      c,
      tooltip,
      anchorHandle);
}
```

At the moment the API only supports View tooltips. We might add Component tooltip support in the future if there's a need for it - contact us if that's the case for you.
