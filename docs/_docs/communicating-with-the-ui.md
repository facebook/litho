---
docid: communicating-with-the-ui
title: Communicating with the UI
layout: docs
permalink: /docs/communicating-with-the-ui
---
## Introduction
Data flows through the section hierarchy before they are eventually represented on the UI by components.
Section provides a set of functionalities to allow you to respond to the data and interact with the UI.

## @OnRefresh
A method annotated with this annotation will be called when the the UI rendered by the
section's hierarchy is requesting for a refresh of the content.

### API
Call `SectionTree#refresh()` to propagate your refresh request to all the sections in the hierarchy. Then you can handle it in your section like this:

```java
class SectionSpec {

  @OnRefresh
  static void onRefresh(SectionContext c, @Prop yourProp, @State yourState) {
    // Handle your refresh request
  }
}
```

## @OnViewportChanged
A method annotated with this annotation will be called when there is a change in the visible viewport.

### API
Call `SectionTree#viewPortChanged()` or `SectionTree#viewPortChangedFromScrolling()` to allow your sections to know that something on the viewport is now different.

```java
class SectionSpec {

  @OnViewportChanged
  static void onViewportChanged(
    SectionContext c,
    int firstVisiblePosition,
    int lastVisiblePosition,
    int firstFullyVisibleIndex,
    int lastFullyVisibleIndex,
    int totalCount,
    @Prop YourProp prop,
    @State YourState state) {
  }
}
```

1) `firstVisiblePosition`
Position of the first visible components in the viewport. Components are partially
hidden from the visible viewport.

2) `lastVisiblePosition`
Position of the last visible components in the viewport. Components are partially
hidden from the visible viewport.

3) `firstFullyVisibleIndex`
Position of the first fully visible components in the viewport.

4) `lastFullyVisibleIndex`
Position of the last fully visible components in the viewport.

5) `totalCount`
Total number of items in the section's hierarchy, with the section that contains the
annotated method as its root.

### Change in Viewport
A viewport change could occur due to any number of the following reasons:

1) Components added to the visible viewport.
2) Components removed from the visible viewport.
3) Scrolling.
4) Components in the visible viewport are updated.
5) Components have moved in or out of the visible viewport.

### Positions and Counts

Positions and total count returned are with respect to the number of components this section has.
For example:

```
       Section A
      /         \
 Section B   Section C
    |           |
  10 items    10 items
  (visible)   (hidden)
```

 When the first item of Section C comes into the viewport due to scrolling, `firstVisiblePosition` of `Section C` is 0 while the `lastVisiblePosition` of `Section B` is 10.

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

Use this method to give focus to one of the components in a section. If the component is hidden from the visible viewport, the section will be scrolled to reveal it, thereby calling the `@OnViewportChanged` annotated method.

The data that renders the component being requested for focus has to be available before the method can work. Hence, only use `requestFocus()` after the `@OnDataBound` annotated method has been called.

### API
There a few variations of the `requestFocus()` method.

#### SectionLifecycle.requestFocus(SectionContext c, int index)
Calls focus by the index of the `Component` in the `Section` scoped by the given `SectionContext`

> As with `@OnViewportChanged`, the index is with respect to the number of components this section has.

#### SectionLifecycle.requestFocus(SectionContext c, String sectionKey)
Calls focus to the first index of the component in the section represented by the section's key you provided.

#### SectionLifecycle.requestFocusWithOffset(SectionContext c, int index, int offset)
Same as `SectionLifecycle.requestFocus(SectionContext c, int index)` but with an offset.

#### SectionLifecycle.requestFocusWithOffset(SectionContext c, String sectionKey, int offset)
Same as `SectionLifecycle.requestFocus(SectionContext c, String sectionKey)` but with an offset.

#### SectionLifecycle.requestFocus(SectionContext c, String sectionKey, FocusType focusType)
`FocusType` is either:
1) `FocusType.START`
Calls focus to the first index of the component in the section

2) `FocusType.END`
Calls focus to the last index of the component in the section

## @OnEvent(LoadingEvent.class)

Sections should use this annotation to declare a method to receive events about its children loading
state.

### API
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

#### LoadingState loadingState
1) `INITIAL_LOAD`
Loading has started

2) `LOADING`
Loading is still ongoing

3) `SUCCEEDED`
Loading is successful

4) `FAILED`
Loading has failed.

#### boolean isEmpty
Returns true if the data set is empty after the loading event

#### Throwable t
Returns the reason for a `LOAD_FAILED` event

### Dispatch up the hierarchy
The loading event will be passed up the hierarchy until there is a section that has chosen to handle
it. If your section handles the loading event, it has to dispatch the event up its hierarchy if
there are parent sections looking to handle it as well.

```java
SectionLifecycle.dispatchLoadingEvent(c, isEmpty, loadingState, t);
```
