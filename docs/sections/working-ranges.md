---
id: working-ranges
title: 'Advanced: Prefetch and pagination'
---

The Working Ranges API provides the means to perform certain complex operations such as data prefetching or cache warming. It extends Sections with a set of appearance and visibility events when a Section enters and exits a given range of positions inside and outside the screen viewport. For example, you can perform a network request to start prefetching data as the last element of a list approaches the viewport.

The API is split in two parts: one defines what a range is, and the other are the Events triggered when Sections interact with the defined ranges.

## Defining a range

To use the working range API, you first need to define your own working range class that implements the [`WorkingRange`](pathname:///javadoc/com/facebook/litho/WorkingRange.html) interface. This interface provides two main functions: `shouldEnterRange` and `shouldExitRange`.

```java
public class AtLeastPartiallyVisibleRange implements WorkingRange { ... }
```

For both functions the parameter `position` is the position of the item in the list, and the `firstVisibleIndex` / `lastVisibleIndex` / `firstFullyVisibleIndex` / `lastFullyVisibleIndex` parameters are current visible range of the viewport.

`shouldEnterRange` is used to check if the **item is within a user-defined range**. In this example we check that means that the position is at least partially visible on screen.

```java
@Override
public boolean shouldEnterRange(
    int position,
    int firstVisibleIndex,
    int lastVisibleIndex,
    int firstFullyVisibleIndex,
    int lastFullyVisibleIndex) {
  return position >= firstVisibleIndex
    && position <= lastVisibleIndex;
}
```

`shouldExitRange` is used to check if the **item is outside a user-defined the range**. In this example we check that means that the position completely not visible, not even partially.

```java
@Override
public boolean shouldExitRange(
    int position,
    int firstVisibleIndex,
    int lastVisibleIndex,
    int firstFullyVisibleIndex,
    int lastFullyVisibleIndex) {
  return position < firstVisibleIndex
    || position > lastVisibleIndex;
}
```

:::caution
Compared to the working ranges in [ComponentKit](https://componentkit.org/), the granularity in ComponentKit is pixels whereas in Litho the granularity is positions. This means we can only tell the component if it's in the range instead of how many pixels are in the range.
:::


## Receiving Range events

After defining a working range, now we implement the callback functions inside the components that should receive the exit and enter range events on the `@LayoutSpec` that contains our `RecyclerComponent`.

```java
@LayoutSpec
class ListContainerComponentSpec {

  @OnRegisterRanges
  static void registerWorkingRanges(
      ComponentContext c,
      @Prop AtLeastPartiallyVisibleRange myRange) {
    ListContainerComponent.registerPrefetchWorkingRange(c, myRange);
  }
}
```

The `@OnEnteredRange` event is triggered **when the component enters the range**:

```java
@OnEnteredRange(name = "prefetch")
static void onEnteredWorkingRange(
    ComponentContext c,
    @Prop MyService service) {
  service.startPrefetch();
}
```

And the `@OnExitedRange` event is triggered **when the component exits the range**:

```java
@OnExitedRange(name = "prefetch")
static void onExitedWorkingRange(
    ComponentContext c,
    @Prop MyService service) {
  service.cancelAllPrefetches();
}
```

:::tip
The name field in the annotation is used to allow multiple range events for a single layout. For example "paginate" becomes `registerPaginateWorkingRange`.
:::
