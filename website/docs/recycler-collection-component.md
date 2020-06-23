---
id: recycler-collection-component
title: RecyclerCollectionComponent
---

[RecyclerView](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html) is one of the fundamental building blocks for any Android application that contain a scrolling list of items.
Litho recommends using [RecyclerCollectionComponent](/javadoc/com/facebook/litho/sections/widget/RecyclerCollectionComponent) and [Sections](sections-intro) to build scrolling lists easily.  With these apis you can builds everything from simple, homogeneous lists to complex, heterogeneous lists backed by multiple data sources while taking advantage of features such as background layout and incremental mount.

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
You can learn more about how to create sections by checking out some of the [building blocks](sections-building-blocks) included in the library.

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
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
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
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .build();
```

Other snapping options are SNAP_NONE, SNAP_TO_END, SNAP_TO_CENTER.


### Setting the height of a horizontal RecyclerCollectionComponent
You can set the height of horizontally scrolling `RecyclerCollectionComponent` in three ways:
1) The most performant way: A fixed height is set on the H-Scroll component.
In this case, the client knows the height of the h-scroll when it creates it. The height cannot be changed once the h-scroll gets measured. Children of this h-scroll are measured with at most the height of the h-scroll and positioned at the start of the h-scroll. In Litho this is the most efficient way to set the height of an h-scroll and it's advisable to use this option whenever possible.
To do this, just set the height through the `height` prop on your `RecyclerCollectionComponent`:
```java
final Component component =
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
        .height(heightValue)
        .build();
 ```
2) Height is not known when component is created: Let the h-scroll set its height to the height of the first item.
In cases where the height of the h-scroll is not known at the time it is created, the height will be determined by measuring the first child of the h-scroll and setting that as the height of the h-scroll. This measurement happens once only, when the h-scroll is first measured, and the height cannot be changed after that. All other children heights will be measured with at most the height of the h-scroll and position at the start of the h-scroll.
To enable this, instead of passing a `height` prop on the `RecyclerCollectionComponent`, tell it through the `canMeasureRecycler` prop it should measure itself.

```java
final Component component =
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
        .canMeasureRecycler(true)
        .build();
 ```

Note that if you don't set a non-zero height on the `RecyclerCollectionComponent` and `canMeasureRecycler` is not enabled, your RecyclerCollectionComponent will end up with a height of 0.

3) The underperformant way: Let the h-scroll dynamically change its height to fit the tallest item
H-Scrolls can be configured to support items of different heights or remeasuring the height if the height of the children could change after the initial measurement. In this case, the initial height of the h-scroll is determined by the height of the tallest child.
Initial height: The initial height of the h-scroll is determined by the height of the tallest child.
Expanding more than the height of the h-scroll: If a child wants to expand to become taller than the current height of the h-scroll, the h-scroll will be remeasured with the new height of the child. Other items will not be remeasured.
Collapsing the highest child: If the child with the biggest height collapses, then the h-scroll will again determine what its height should be by remeasuring all the items.

> Enabling this option should be done only if absolutely needed and should especially be avoided for lists with infinite scrolling.

Measuring all the children to determine the tallest comes with a high performance cost, especially for infinite loading h-scrolls when the height needs to be remeasured every time new items are inserted.
If you must do this, you can pass your own [RecyclerConfiguration](/javadoc/com/facebook/litho/sections/widget/RecyclerConfiguration.html) to the `RecyclerCollectionComponent` and enable this on the [RecyclerBinderConfigurationer](/javadoc/com/facebook/litho/sections/widget/RecyclerBinderConfiguration.html) that is used to create the `RecyclerConfiguration`.
Here's an example of enabling that on a horizontal linear list:

```java
final RecyclerBinderConfiguration configuration = new RecyclerBinderConfiguration(rangeRatio);
configuration.setHasDynamicItemHeight((true);
RecyclerConfiguration recyclerConfiguration =
    new ListRecyclerConfiguration(
        LinearLayoutManager.HORIZONTAL,
        reverseLayout,
        snapMode,
        configuration);

final Component component =
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .canMeasureRecycler(true)
        .build();
```

### Pull to refresh
`RecyclerCollectionComponent` enables pull-to-refresh by default and sends an event handler to the underlying `Recycler` that will trigger a refresh on the SectionTree.
 To disable this functionality you need to set the `disablePTR` prop to true:

```java
final Component component =
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
        .recyclerConfiguration(recyclerConfiguration)
        .disablePTR(true)
        .build();
 ```

### Loading, Empty, and error screens

With the sections API you can also integrate your data fetching through [Loading events](communicating-with-the-ui#null__loadingstate-loadingstate) and [Services](services).  `RecyclerCollectionComponent` can listen to these [loading events](/javadoc/com/facebook/litho/sections/LoadingEvent.html) and will respond accordingly.  Through the props `loadingComponent`, `emptyComponent`, and `errorComponent`, you can specify what to show when certain things happen when fetching data:
 - `loadingComponent`: data is being loaded and there's nothing in the list
 - `emptyComponent`: data has finished loading and there's nothing to show.
 - `errorComponent`: data loading has failed and there's nothing in the list.

```java
final Component component =
    RecyclerCollectionComponent.create(c)
        .section(FooSection.create(new SectionContext(c)).build())
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
