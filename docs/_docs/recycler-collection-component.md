---
docid: recycler-collection-component
title: RecyclerCollectionComponent
layout: docs
permalink: /docs/recycler-collection-component
---

[RecyclerView](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html) is one of the fundamental building blocks for any Android application that contain a scrolling list of items.
Litho recommends using [RecyclerCollectionComponent](/javadoc/com/facebook/litho/sections/widget/RecyclerCollectionComponent) and [Sections](/docs/sections-intro) to build scrolling lists of easily.  With these apis you can builds everything from simple, homogeneous lists to complex, heterogeneous lists backed by multiple data sources while taking advantage of features such as background layout and incremental mount.

### Create a RecyclerCollectionComponent

You can use `RecyclerCollectionComponent` as you would use any other component in the framework by building it and adding it as a child in your layout.

```java
@OnCreateLayout
static Component onCreateLayout(
    final ComponentContext c) {
  return RecyclerCollectionComponent.create(c)
      .section(createSection())
      .build();
}
```

This code will eventually render as a `RecyclerView` who's rows are backed by the contents of the section.
You can learn more about how to create sections by checking out some of the [building blocks](/docs/sections-building-blocks) included in the library.

## Batteries Included

`RecyclerCollectionComponent` includes a number of practical features for working with lists.   You can see the full list of props it accepts in [the javadocs](/javadoc/com/facebook/litho/sections/widget/RecyclerCollectionComponent) but here are some notable features:


### Horizontal lists

`RecyclerCollectionComponent` takes a `RecyclerConfiguration` prop for determining what layout manager to use. By default, if this prop is not specified, it uses an implementation of `RecyclerConfiguration` called `ListRecyclerConfiguration` which will create a [LinearLayoutManager]() with vertical orientation to be used by the `RecyclerCollectionComponent`.

For a horizontal layout, you pass a `ListRecyclerConfiguration` with a horizontal orientation:

```java
final RecyclerCollectionComponentSpec.RecyclerConfiguration
      recyclerConfiguration =
          new ListRecyclerConfiguration(
              LinearLayoutManager.HORIZONTAL, false /* reverse layout */);

final Component component =
    RecyclerCollectionComponent.create(context)
        .section(FooSection.create(new SectionContext(context)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .build();
```

You can also create a grid list by using [GridRecyclerConfiguration](/javadoc/com/facebook/litho/sections/widget/GridRecyclerConfiguration).

### Snapping

In horizontally scrollable lists, the snapping mode for the `RecyclerCollectionComponent` can also be configured through the `ListRecyclerConfiguration`:
```java
final RecyclerCollectionComponentSpec.RecyclerConfiguration
      recyclerConfiguration =
          new ListRecyclerConfiguration(
              LinearLayoutManager.HORIZONTAL, false /* reverse layout */, SNAP_TO_START);

final Component component =
    RecyclerCollectionComponent.create(context)
        .section(FooSection.create(new SectionContext(context)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .build();
```

Other snapping options are SNAP_NONE, SNAP_TO_END, SNAP_TO_CENTER.


### Setting the height of a horizontal RecyclerCollectionComponent

A horizontal `RecyclerCollectionComponent` expects to receive its height through a `height` prop. If no height is specified, it will take a height of 0.
If you don't know the height of your `RecyclerCollectionComponent`, you can configure it to determine its own height by enabling the `canMeasure` prop. This will measure the first child Component in the Sections hierarchy and will set the height of the entire `RecyclerCollectionComponent` to the height of the child.

```java
final Component component =
    RecyclerCollectionComponent.create(context)
        .section(FooSection.create(new SectionContext(context)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .disablePTR(true)
        .canMeasureRecycler(true)
        .build();
 ```

Whenever possible, you should specify an exact height on the `RecyclerCollectionComponent` to avoid an extra measure for determining the height.

### Pull to refresh
`RecyclerCollectionComponent` enables pull-to-refresh by default and sends an event handler to the underlying `Recycler` that will trigger a refresh on the SectionTree.
 To disable this functionality you need to set the `disablePTR` prop to false:

```java
final Component component =
    RecyclerCollectionComponent.create(context)
        .section(FooSection.create(new SectionContext(context)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .disablePTR(true)
        .build();
 ```

### Loading, Empty, and error screens

With the sections API you can also integrate your data fetching through [Loading events](/docs/communicating-with-the-ui#null__loadingstate-loadingstate) and [Services](/docs/services).  `RecyclerCollectionComponent` can listen to these [loading events](/javadoc/com/facebook/litho/sections/LoadingEvent.html) and will respond accordingly.  Through the props `loadingComponent`, `emptyComponent`, and `errorComponent`, you can specify what to show when certain things happen when fetching data:
 - `loadingComponent`: data is being loaded and there's nothing in the list
 - `emptyComponent`: data has finished loading and there's nothing to show.
 - `errorComponent`: data loading has failed and there's nothing in the list.

```java
final Component component =
    RecyclerCollectionComponent.create(context)
        .section(FooSection.create(new SectionContext(context)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .loadingComponent(
            Progress.create(c)
                .build())
        .errorComponent(
            Text.create(c)
                .text("Data Fetch has failed").build())
        .emptyComponent(
            Text.create(c)
                .text("No data to show").build())
        .build();
 ```

You can check what other props `RecyclerCollectionComponent` supports [here](/javadoc/com/facebook/litho/sections/widget/RecyclerCollectionComponent.html).
