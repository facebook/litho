---
docid: sections-events
title: Sections Events
layout: docs
permalink: /docs/sections-events
metatags: noindex, follow
---

## @OnRefresh
A method annotated with this annotation will be called when the the UI rendered by the
section's hierarchy is requesting for a refresh of the content.

```java
class SectionSpec {

  @OnRefresh
  static void onRefresh(SectionContext c, @Prop yourProp, @State yourState) {
  }
}
```

## @OnViewportChanged
A method annotated with this annotation will be called when the `Component`s rendered by the
section's hierarchy encounters a change in its visible viewport.

The method can receive the following parameters:

`firstFullyVisibleIndex`
Returns the position of the first fully visible components in the viewport.

`lastFullyVisibleIndex`
Returns the position of the last fully visible components in the viewport.

`firstVisiblePosition`
Returns the position of the first visible components in the viewport. Components is partially
hidden from the visible viewport.

`lastVisiblePosition`
Returns the position of the last visible components in the viewport. Components is partially
hidden from the visible viewport.

`totalCount`
Returns the total number of items in the section's hierarchy, with the `Section` that contains the
annotated method as its root.

A viewport change could occur due to any number of the following reasons:

1) Components added to the visible viewport.
2) Components removed from the visible viewport.
3) Scrolling.
4) Components in the visible viewport are updated.
5) Components have moved in or out of the visible viewport.

```java
class SectionSpec {

  @OnViewportChanged
  static void onViewportChanged(
    SectionContext c,
    int firstVisiblePosition,
    int lastVisiblePosition,
    int totalCount,
    int firstFullyVisibleIndex,
    int lastFullyVisibleIndex
    @Prop YourProp prop
    @State YourState state) {
  }
}
```

A point to remember here is that the positions and total count returned are with respect to the
number of `Component`s this `Section` has.

For example:

     Section A
     /      \
Section B   Section C
   |           |
 10 items    10 items
 (visible)   (hidden)

 When the first item of Section C comes into the viewport due to scrolling,
 `firstVisiblePosition` of `Section C` is 0 while the `lastVisiblePosition` of `Section B` is 10.

 Section A has a total of 20 items while Section B and C have 10 each.

## @OnDataBound

A method annotated with this annotation will be called when the data changes corresponding to this
section's hierarchy is made available to the `SectionTree.Target`.

Data changes could occur due to any number of the following:
1) Insertion
2) Update
3) Delete
4) Move

Availability of these data changes do not mean they are visible on the viewport.
To detect visibility, use `@OnViewportChanged`.

```java
class SectionSpec {

  @OnDataBound
  static void OnDataBound(
    SectionContext c,
    @Prop YourProp prop
    @State YourState state) {
    // Handle data changes
  }
}
```

## requestFocus()

Use this method to give focus to one of the `Components` in a section. If the `Component` is
hidden from the visible viewport, the section will be scrolled to reveal it, thereby calling the
`@OnViewportChanged` annotated method.

The data that renders the `Component` being requested for focus has to be available before the
method can work. Hence, only use `requestFocus()` after the `@OnDataBound` annotated method has
been called.

There a few variations of the `requestFocus()` method, but there are two basic ones you should be
familiar with:

1) `SectionLifecycle.requestFocus(SectionContext c, int index)`
Calls focus by the index of the `Component` in the `Section` scoped by the given `SectionContext`


NOTE: As with `@OnViewportChanged`, the index is with respect to the number of `Component`s
this `Section` has.

2) `SectionLifecycle.requestFocus(SectionContext c, String sectionKey)`
Calls focus to the first index of the `Component` in the `Section` represented by the section's
key you provided.


The other two variations of the method are calling focus with an offset:

3) `SectionLifecycle.requestFocusWithOffset(SectionContext c, int index, int offset)`

4) `SectionLifecycle.requestFocusWithOffset(SectionContext c, String sectionKey, int offset)`

There is one last variation to the request focus calling method using the `Section`'s key:

5) `SectionLifecycle.requestFocus(SectionContext c, String sectionKey, FocusType focusType)`
`FocusType` is either `FocusType.START` or `FocusType.END`. The former calls focus to the first
index of the `Component` in the `Section` while the latter does to the last index.


## @OnEvent(LoadingEvent.class)

Sections should use this annotation to declare a method to receive events about its children loading
state.

There are 3 parameters to handle:

`LoadingState loadingState`
There are 4 `loadingState` which can be returned from the loading event.

1) INITIAL_LOAD
Loading has started

2) LOADING
Loading is still ongoing

3) SUCCEEDED
Loading is successful

4) FAILED
Loading has failed.

`boolean isEmpty`
Returns true if the data set is empty after the loading event

`Throwable t`
Returns the reason for a `LOAD_FAILED` event

The loading event will be passed up the hierarchy until there is a section that has chosen to handle
it. If your section handles the loading event, it has to dispatch the event up its hierarchy if
there are parent sections looking to handle it as well.

Code sample below shows how a section can handle the loading event from its child:

```java
class YourSectionSpec {

  @OnCreateChildren
  protected static Children onCreateChildren(SectionContext c) {
    return Children.create()
        .child(
          ChildSection
            .create(c)
            .loadingEventHandler(YourSection.onLoadingEvent(c)))
        .build()
    }
  }

  @OnEvent(LoadingEvent.class)
  static void onLoadingEvent(
    SectionContext c,
    @FromEvent LoadingState loadingState,
    @FromEvent boolean isEmpty,
    @FromEvent Throwable t) {

    switch (loadingState) {
        case INITIAL_LOAD:
        case LOADING:
          // Handle loading
          break;
        case FAILED:
          // Handle failure
          break;
        case SUCCEEDED:
          // Handle success
          break;
    }

    // Dispatch the same loading event up the hierarchy.
    SectionLifecycle.dispatchLoadingEvent(
        c,
        isEmpty,
        loadingState,
        t);
  }
}
```

