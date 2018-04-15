---
docid: sections-working-ranges
title: Working Ranges
layout: docs
permalink: /docs/sections-working-ranges
---

The purpose of the working ranges API is to provide flexible appearance events for a component entering and exiting a given range to perform certain custom operations. The common useful case is for data prefetching or cache warming.

Comparing to the working ranges in [ComponentKit](https://componentkit.org/), the granularity in ComponentKit is pixels whereas in Litho the granularity is positions. This means we can only tell the component if it's in the range instead of how many pixels are in the range.

To use the working range API, you first need to define your own working range class like this:

```java
public class MyWorkingRange implements WorkingRange {

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
}
```

The working range class needs to implement the [WorkingRange](/javadoc/com/facebook/litho/WorkingRange.html) interface. `shouldEnterRange` is used to check if the item at the given position is within a user-defined the range and `shouldExitRange` is used to check if the item is outside the range. The parameter `position` is the position of the item in the list, and the `firstVisibleIndex` / `lastVisibleIndex` / `firstFullyVisibleIndex` / `lastFullyVisibleIndex` parameters are current visible range of the viewport.

After defining a working range, now we implement the callback methods inside the components that should receive the exit and enter range events.

```java
@LayoutSpec
class MyComponentSpec {
  @OnEnteredRange(name = "prefetch")
  static void onEnteredWorkingRange(
      ComponentContext c,
      @Prop Object object) {
    ...
  }

  @OnExitedRange(name = "prefetch")
  static void onExitedWorkingRange(
      ComponentContext c,
      @Prop Object object) {
    ....
  }

  @OnRegisterRanges
  static void registerWorkingRanges(
      ComponentContext c,
      @Prop WorkingRange myRange) {
    MyComponent.registerPrefetchWorkingRange(c, myRange);
  }
}
```

The `@OnEnteredRange` method is triggered when the component enters the range, and the `@OnExitedRange` method is triggered when the component exits it.

The name field in the annotation is used by the Annotation Processor Tool (APT) to generate register methods. For example, for the name "prefetch", the APT would generate a register method called "registerPrefetchWorkingRange", and in the `@OnRegisterRanges` method, we can register our working range with these register methods. The register method binds the working range and `@OnEnteredRange` / `@OnExitedRange` methods.

When items in the list are inserted/removed/scrolled, the [RecyclerBinder](https://fblitho.com/docs/recycler-component#recyclerbinder) would take care of checking if the its working range state changed (in range → out of range and vice versa) and trigger the corresponding `@OnEnteredRange` / `@OnExitedRange` methods on the components that registered the callbacks.
