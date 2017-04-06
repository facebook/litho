---
docid: recycler-component
title: RecyclerComponent
layout: docs
permalink: /docs/recycler-component
---
#Recycler Component

RecyclerView is one of the fundamental building blocks for any Android application that contain a scrolling list of items.
Recycler Component exposes very similar functionalities while taking advantage of features such as background layout and incremental mount.

### Create a Recycler Component

You can use Recycler as any other component in the framework by building it and adding it as a child in your layout.

``` java
@OnCreateLayout
static ComponentLayout onCreateLayout(
    final ComponentContext c,
    @Prop RecyclerBinder recyclerBinder) {
   	  return Container.create(c)
        .child(Recycler.create(c)
          .binder(recyclerBinder))
        .build();
}
```
This code will render a Recycler component that will display the content of recyclerBinder.

### RecyclerBinder
RecyclerBinder is the entry point to manipulate list-like UIs with Components.
It keeps a list of all the components contained in the list and as the user scrolls through the list it computes layouts for items that are about to become visible.

RecyclerBinder is the part of Litho that:

 - Serves as an Adapter for a RecyclerView
 - Defines the layout to use in the RecyclerView (ex Linear, Grid)
 - Manages all the complexity of computing layouts ahead of time in a background thread.

![Layout range in action](static/range_small.gif "Layout range in action")

Let's start creating a RecyclerBinder:

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder(c);
```
This will create the simplest possible RecyclerBinder that will layout the content of the Recycler as a vertical list.

To have Recycler use a grid layout we can use this constructor instead:

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder(c, new GridLayoutInfo(c, spanCount);
```
RecyclerBinder exposes a set of APIs to manipulate the items that will be displayed in the Recycler.

The most commonly used are:

``` java
recyclerBinder.insertItemAt(position, component);
recyclerBinder.updateItemAt(position, component);
recyclerBinder.removeItemAt(position);
recyclerBinder.moveItem(fromPosition, toPosition);
```

RecyclerBinder's API works directly with Components, since a Component is only a collection of Props, we can build any component ahead of time and leave the layout management to the RecyclerBinder.

RecyclerBinder also supports receiving extra information about the way a Component should be laid out. These extra information can be passed in through a ComponentInfo. Here's what the code looks like:

``` java
recyclerBinder.insertItemAt(
	position,
	ComponentInfo.create().component(component).isSticky().build());
```

#### RecyclerBinder Async operations

RecyclerBinder also supports async operations.
Inserting or updating an item asynchronously, means that if the item is in the range for which we would need to compute a layout, the Binder will wait until the layout is ready before notifying the RecyclerView of any change.
To use async operations you would write something like:

``` java
 recyclerBinder.insertItemAtAsync(position, component);
 recyclerBinder.updateItemAtAsync(position, component);
 recyclerBinder.removeItemAsync(position);
 recyclerBinder.moveItemAsync(fromPosition, toPosition);
```
RecyclerBinder is guaranteed to respect the ordering in which the async operations were invoked so something like:

``` java
 recyclerBinder.insertItemAtAsync(0, component);
 recyclerBinder.removeItemAsync(1);
```
would mean that remove is only executed after component has been inserted at position 0 and it will actually remove the item that was in position 0 before component was inserted.

Operating with synchronous operations on the other hand, means that an item will be immediately inserted, even if its insertion position is inside the range of active components. This potentially means that the framework would be forced to compute a layout synchronously on the UI thread when that component needs to be put on screen.

###### Sync insertion:
![Layout range in action](static/insertion_sync_small.gif "Layout range in action")
###### Async insertion:
![Layout range in action](static/insertion_async_small.gif "Layout range in action")


Operating with sync operations is usually useful when the result of an operation should be visible without any delay. Triggering a sync operation while there is an active queue of async operations will flush all the pending async operations synchronously.
