---
docid: sections-intro
title: Sections Tutorial
layout: docs
permalink: /docs/sections-tutorial
---

*NOTE* This part builds on the work you did in [the litho tutorial](/docs/tutorial). Make sure you've read through that tutorial before returning to this one.

Recall in [the litho tutorial](/docs/tutorial) that we left off with a vertically scrolling list with alternating background colors. In this optional part of this tutorial, we will add an horizontal scrolling unit at the top of the list by modifying `ListSectionSpec` to leverage the composability of the Litho and Sections API.


## 1. Refactoring ListSectionSpec
Let us revisit [ListSectionSpec](/docs/tutorial#3-creating-a-list-of-items).  Each `SingleComponentSection` inside the for loop renders almost the same component, only the number changes.  In other words, each of our `ListItem` components are backed by a model object of the same type: `int`.

Litho provides another core section `DataDiffSection` that is specifically used to render components backed by a list of models. In this step we will refactor `ListSectionSpec` to use `DataDiffSection`.

First, lets generate our model objects.  For the sake of this example, we will just add a static method inside `ListSectionSpec` to generate our model objects.  In a real scenario this would be replaced with proper data fetching logic:

```java
private static List<Integer> generateData(int count) {
  final List<Integer> data = new ArrayList<>(count);
  for (int i = 0; i < count; i++) {
    data.add(i);
  }
  return data;
}
```

Next, write a method that creates a `ListItem` component given a model object:

```java
@OnEvent(RenderEvent.class)
static RenderInfo onRender(final SectionContext c, @FromEvent Integer model) {
  return ComponentRenderInfo.create()
      .component(
          ListItem.create(c)
              .color(model % 2 == 0 ? Color.WHITE : Color.LTGRAY)
              .title(model + ". Hello, world!")
              .subtitle("Litho tutorial")
              .build())
      .build();
}
```

Finally combine these two new methods with `DataDiffSection` by replacing our current `onCreateChildren` method with:

```java
@OnCreateChildren
static Children onCreateChildren(final SectionContext c) {
  return Children.create()
      .child(
          DataDiffSection.create(c)
              .data(generateData(32))
              .renderEventHandler(ListSection.onRender(c)))
      .build();
}
```



**Woah, what's this `@OnEvent` stuff??  Where in the world did `ListSection.onRender()` come from??**

Here's a quick explanation of what's going on:
 - `DataDiffSection` emits a `RenderEvent` whenever an item needs to be rendered.
 - When creating a `DataDiffSection` we give it our custom `RenderEventHandler`, `ListSection.onRender(c)`.
 - This custom event handler will call the `onRender` method defined in `ListSectionSpec` with all the right parameters whenever it receives a `RenderEvent`.
 - All of this event handler code is auto-generated from the `@OnEvent` annotation.

 You can read more Events and Litho's event handling system [here](https://fblitho.com/docs/events-overview).

Run the app. You should see the same thing as before.

<img src="/static/images/barebones4.png" style="width: 300px;">

## 2. Adding an H-scroll

Remember how we used `RecyclerCollectionComponent` to create our list? Since `RecyclerCollectionComponent` is itself a Component, we can create one inside a Section and easily nest scrolling lists! While it doesn't quite make sense to nest a vertical list inside another vertical list, it's quite common to nest a horizontal list inside a vertical list.  We will do just that in this step.

Update `onCreateChildren()` to add a `SingleComponentSection` before the `DataDiffSection`:

```java
@OnCreateChildren
static Children onCreateChildren(final SectionContext c) {
  return Children.create()
      .child(
          SingleComponentSection.create(c)
              .component(
                  RecyclerCollectionComponent.create(c)
                      .disablePTR(true)
                      .recyclerConfiguration(new ListRecyclerConfiguration(LinearLayoutManager.HORIZONTAL, /*reverse layout*/ false, SNAP_TO_CENTER))
                      .section(
                          DataDiffSection.create(c)
                              .data(generateData(32))
                              .renderEventHandler(ListSection.onRender(c))
                              .build())
                      .canMeasureRecycler(true))
              .build())
        .child(
            DataDiffSection.create(c)
                .data(generateData(32))
                .renderEventHandler(ListSection.onRender(c)))
        .build();
  }
```

We're seeing a couple new props for `RecyclerCollectionComponent` here.
- `recyclerConfiguration` is a configuration object for setting the layout of the components and the snapping behavior for the `RecyclerCollectionComponent`.
- `canMeasureRecycler` needs to be set to true for a horizontal `RecyclerCollectionComponent` if you can't set a static height on it. The framework will measure the first child of the `RecyclerCollectionComponent` and use that child's height as the height of the entire horizontal scrolling list.

Run the app now, you should see something like this:

<img src="/static/images/barebones5.gif" style="width: 300px;">


## Summary

Congratulations on completing part 2 of this tutorial! This part of the tutorial introduced you to more advanced usages of Sections and should arm you with additional building blocks to help you build your own complex scrolling UI. Some more fun things you can try:

- `SingleComponentSection` has a few interesting props.  See if you can make the horizontal scrolling row stick to the top
- The `RenderEvent` event has other fields that are helpful when creating components for each row.  See if you could make every 3rd row an h-scroll.

You can find the [completed tutorial here](https://github.com/facebook/litho/tree/master/sample-barebones). Be sure to check out [this sample](https://github.com/facebook/litho/tree/master/sample) for more in-depth code as well as the Litho API documentation for additional information.
