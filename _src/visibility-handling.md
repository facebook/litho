---
id: visibility-handling
title: Visibility Handling
---

### Types of Visibility Range

The framework currently supports six types of Visibility Event:

- [Visible Event](pathname:///javadoc/com/facebook/litho/VisibleEvent.html): this event is triggered when at least one pixel of the Component is visible.
- [Invisible Event](pathname:///javadoc/com/facebook/litho/InvisibleEvent.html): this event is triggered when the Component no longer has any pixels on the screen.
- [Focused Visible Event](pathname:///javadoc/com/facebook/litho/FocusedVisibleEvent.html): this event is triggered when either the Component occupies at least half of the viewport, or, if the Component is smaller than half the viewport, when it is fully visible.
- [Unfocused Visible Event](pathname:///javadoc/com/facebook/litho/UnfocusedVisibleEvent.html): this event is triggered when the Component is no longer focused, i.e. it is not fully visible and does not occupy at least half the viewport.
- [Full Impression Visible Event](pathname:///javadoc/com/facebook/litho/FullImpressionVisibleEvent.html): If the Component is smaller than the viewport, this event is triggered when the entire Component is visible in the viewport. If the Component is bigger than the viewport, then just covering the viewport won't trigger the event. It will be triggered when all the edges have passed through the viewport once.
- [VisibilityChangedEvent](pathname:///javadoc/com/facebook/litho/VisibilityChangedEvent.html): this event is triggered when the visibility of the Component on the screen changes.

### Usage

To register visibility event handlers for a component you can follow the same [steps](/docs/mainconcepts/coordinate-state-actions/events) as for setting any other event handler.

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
            .visibilityChangedHandler(MyLayout.onComponentVisibilityChanged(c))
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

  @OnEvent(VisibilityChangedEvent.class)
  static void onComponentVisibilityChanged(
      ComponentContext c,
      @FromEvent int visibleTop,
      @FromEvent int visibleLeft,
      @FromEvent int visibleHeight,
      @FromEvent int visibleWidth,
      @FromEvent float percentVisibleHeight,
      @FromEvent float percentVisibleWidth) {
      Log.d("VisibilityRanges", "The component visible size is " + visibleHeight + "h" + visibleWidth + "w");
  }

}
```

:::info
VisibilityChangedEvents should be used with particular care since they will be dispatched on every frame while scrolling. No heavy work should be done inside the VisibilityChangedEvents handlers. Visible, Invisible, Focused, Unfocused and Full Impression events are the recommended over VisibilityChanged events whenever possible.
:::

### Custom visibility percentage
By default, `VisibleEvent` is triggered when at least 1 pixel of the Component is visible. In some cases you may want to listen to custom visibility events and perform an action when the Component is only partially visible.
You can specify a ratio of the Component width or height for when the visibility event is dispatched by using `visibleHeightRatio` and `visibleWidthRatio` props when specifying a visibility handler.

```java
@OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {

    return Column.create(c)
        .alignItems(YogaAlign.STRETCH)
        .child(Text.create(c)
            .text("This is MY layout spec")
            .visibleHandler(MyLayout.onTitleVisible(c))
            .visibleHeightRatio(0.8f)
            .visibleWidthRatio(0.1f)
        .build();
}
```
For the example above, a VisibilityEvent is dispatched when at least 80% of the Component's height and 10% of the Component's width is visible.
When the Component's visible percentage changes to less than 80% of total height, an invisible event will be dispatched.
If not specified, the default width or height ratio is 1f.

### Changing LithoView visibility

There are cases when you need to trigger visibility events on the LithoView components because the UI visibility changed, but the UI did not receive any callback to inform it of this change. An example is when a new activity is added to the back stack, covering the UI. In such cases you need to inform the `LithoView` that its visibility status changed, to tell the UI whether it is visible or not. You may want to do this when `Fragment#setUserVisibleHint` or `onResume/onPause` are called.

#### LithoLifecycleProvider API
The `LithoLifecycleProvider` API can inform LithoViews which are registered to listen to lifecycle state changes that their visibility status changed.

```java
public interface LithoLifecycleProvider {
  void moveToLifecycle(LithoLifecycle lithoLifecycle);

  void addListener(LithoLifecycleListener listener);
}
```

The following states are valid LithoLifecycleProvider states:
- `HINT_INVISIBLE`: this state indicates that the lifecycle provider is considered to be not visible on screen. Lifecycle observers can perform operations which are associated with invisibility status. An example of moving to `HINT_INVISIBLE` state is when a fragment goes from `resumed` to `paused` because the app was backgrounded.
- `HINT_VISIBLE`: this state indicates that the lifecycle provider is considered visible on screen. Lifecycle observers can perform operations which are associated with visibility status. An example of moving to `HINT_VISIBLE` state is when a fragment goes from `paused` to `resumed` because the app was foregrounded.
- `DESTROYED`: this is the final state of a lifecycle provider. Lifecycle observers can perform operations associated with releasing resources. An example of moving to `DESTOYED` state is when the hosting Activity is destroyed.

#### Listening to a `LithoLifecycleProvider` state changes
You can register a `LithoView` to listen to state changes of a `LithoLifecycleProvider` instance when you create it:

```java
final LithoLifecycleProvider lifecycleProvider;
final LithoView lithoView = LithoView.create(c, component, lithoLifecycleProvider);
```

These actions will be performed when moving to the following `LithoLifecycleProvider` states:
- `HINT_INVISIBLE`: `InvisibleEvents` will be dispatched to all Components inside the `LithoView` which were visible.
- `HINT_VISIBLE`: `VisibleEvents` will be dispatched to all Components inside the `LithoView` which meet the visibility criteria.
- `DESTROYED`: The `ComponentTree` associated with the `LithoView` will be released, `InvisibleEvents` will be dispatched to all Components which were visible and all content will be unmounted.

#### `AOSPLithoLifecycleProvider`
This is an implementation of `LithoLifecycleProvider` which has the state tied to that of an AOSP [LifecycleOwner](https://developer.android.com/topic/libraries/architecture/lifecycle#lco).
- `LifecycleOwner` in `ON_PAUSE` state will move the `AOSPLithoLifecycleProvider` to `HINT_INVISIBLE` state
- `LifecycleOwner` in `ON_RESUME` state will move the `AOSPLithoLifecycleProvider` to `HINT_VISIBLE` state
- `LifecycleOwner` in `ON_DESTROY` state will move the `AOSPLithoLifecycleProvider` to `DESTROYED` state

You can use `AOSPLithoLifecycleProvider` when you want to associate a LithoView's visibility status with the lifecycle of a Fragment, Activity or custom LifecycleOwner, when `resumed` means the LithoView is on screen and `paused` means the LithoView is hidden.

```java
final AOSPLithoLifecycleProvider lifecycleProvider = new AOSPLithoLifecycleProvider(fragment);
```

#### Handling custom state changes: LithoLifecycleProviderDelegate
`AOSPLithoLifecycleProvider` covers the most common cases, but there are scenarios where a LifecycleOwner's state doesn't match what we see on screen.
Such examples are:
- Fragments in a ViewPager, where the previous and next visible Fragments are prepared and in a `resumed` state before they're actually visible.
- Adding a Fragment on top of another Fragment doesn't move the first Fragment to a `paused` state, and there's no indication that it's no longer visible to the user.

When you need to handle these state changes manually, you can use `LithoLifecycleProviderDelegate`, a generic `LithoLifecycleProvider` implementation, to change state when appropriate.


```java
final LithoLifecycleProviderDelegate lifecycleProvider = new LithoLifecycleProviderDelegate();
lifecycleProvider.moveToLifecycle(<new LithoLifecycle state>);
```

#### Nested ComponentTrees and `LithoLifecycleProvider`
The Litho APIs for writing Lists (Sections, VerticalScrollSpec, HorizontalScrollSpec) will create hierarchies of nested ComponentTrees:
- a ComponentTree at the root of the hierarchy, encapsulating the entire list (associated with a root LithoView)
- a ComponentTree for each item in the List (associated with a LithoView child of the root LithoView)

If the root LithoView is subscribed to listen to a `LithoLifecycleProvider`, then all nested ComponentTrees/child LithoViews will listen to the outer `LithoLifecycleProvider` too and will receive the correct information about visibility/destroyed state.

Demo apps for showcasing `LithoLifecycleProvider` common case usages are coming soon to our sample app!

:::info
The section below contains information about deprecated APIs. Please consider using `LithoLifecycleProvider` for manually informing a `LithoView` about visibility changes.
:::
#### setVisibilityHint (Deprecated)

Example usage:
```java
LithoView.setVisibilityHint(true); // This will dispatch visible/focused events as necessary on all components inside this LithoView
LithoView.setVisibilityHint(false); // This will dispatch invisible/unfocused events as necessary on all components inside this LithoView
```

After calling `LithoView.setVisibilityHint(false)`, the LithoView will consider itself not visible and will ignore any requests to mount until `setVisibilityHint(true)` is called.
You may still unmount the entire LithoView content by calling `unmountAll` if the visibility hint was set to false.
Resetting the visibility hint to true after it was set to false will also trigger a mount pass, in case the visible bounds changed while the LithoView was ignoring mount requests.

### Troubleshooting
If you are not seeing your visibility event fired when you expect it to be, you can take the following steps:
1. Verify that incremental mount is enabled for your Component. It is enabled by default, but if you turned it off, then visibility events will not be fired.
2. Verify that you actually set the event that you defined in your spec on your Component (i.e. by calling `visibleHandler(MyLayout.onTitleVisible(c))` or similar.
3. Visibility handlers are fired in [`MountState.processVisibilityOutputs()`](https://github.com/facebook/litho/blob/master/litho-core/src/main/java/com/facebook/litho/MountState.java#L489:L657). You can step through this method and see why the event that you expect to be fired is not being fired.
