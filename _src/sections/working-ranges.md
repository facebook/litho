---
id: working-ranges
title: 'Advanced: Prefetch and Pagination'
---

The Working Ranges API provides the means to perform complex operations such as data prefetching and cache warming.

The API extends Sections with a set of appearance and visibility events when a Section enters and exits a given range of positions inside and outside the screen viewport. For example, you can perform a network request to start prefetching data as the last element of a list approaches the viewport.

The API is split into two parts:

1. [Defining a range](#defining-a-range) - uses a WorkingRange class.
2. [Receiving range events](#receiving-range-events) - events triggered when Sections interact with the defined ranges.

## Defining a range

To use the Working Range API, you first need to define your own WorkingRange class that implements the [WorkingRange](pathname:///javadoc/com/facebook/litho/WorkingRange.html) interface. This is achieved with the following:

```java
public class AtLeastPartiallyVisibleRange implements WorkingRange { ... }
```

This interface provides two main functions: [shouldEnterRange](#using-the-shouldenterrange-function) and [shouldExitRange](#using-the-shouldexitrange-function).

For both functions, the parameter `position` is the position of the item in the list. The `firstVisibleIndex` / `lastVisibleIndex` / `firstFullyVisibleIndex` / `lastFullyVisibleIndex` parameters are for the current visible range of the viewport.

### Using the shouldEnterRange Function

`shouldEnterRange` is used to check if the item is **within a user-defined range**. The following example checks if that means the position is at least partially visible on screen:

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

### Using the shouldExitRange Function

`shouldExitRange` is used to check if the item is **outside of a user-defined range**. The following example checks if that means the position is not visible, not even partially:

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
For working ranges, the granularity in the [ComponentKit](https://componentkit.org/) is measured with pixels whereas with Litho its positions. This means you can only tell if the component is within a range of positions instead of how many pixels are in the range.
:::

## Receiving Range Events

After defining a working range, you implement the callback functions inside the components that should receive the exit and enter range events on the `@LayoutSpec` that contains your `RecyclerComponent`, as follows:

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

The `@OnEnteredRange` event is triggered when the component **enters** the range:

```java
@OnEnteredRange(name = "prefetch")
static void onEnteredWorkingRange(
    ComponentContext c,
    @Prop MyService service) {
  service.startPrefetch();
}
```

The `@OnExitedRange` event is triggered when the component **exits** the range:

```java
@OnExitedRange(name = "prefetch")
static void onExitedWorkingRange(
    ComponentContext c,
    @Prop MyService service) {
  service.cancelAllPrefetches();
}
```

:::tip
The name field in the annotation is used to enable multiple range events for a single layout. For example, 'paginate' becomes `registerPaginateWorkingRange`.
:::
