---
docid: recycler-component
title: Recycler
layout: docs
permalink: /docs/recycler-component
---

[RecyclerView](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html) is one of the fundamental building blocks for any Android application that contain a scrolling list of items. The
[Recycler](/javadoc/com/facebook/litho/widget/Recycler) component exposes very similar functionalities while taking advantage of features such as background layout and incremental mount.

### Create a Recycler component

You can use `Recycler` as you would use any other component in the framework by building it and adding it as a child in your layout.

``` java
@OnCreateLayout
static ComponentLayout onCreateLayout(
    final ComponentContext c,
    @Prop RecyclerBinder recyclerBinder) {
    
  return Recycler.create(c)
      .binder(recyclerBinder)
      .buildWithLayout();
}
```
This code will render a `Recycler` component that will display the content of `recyclerBinder`.

### RecyclerBinder
[RecyclerBinder](/javadoc/com/facebook/litho/widget/RecyclerBinder) is the entry point to manipulate list-like UIs with components.
It keeps a list of all the components contained in the list and as the user scrolls through the list it computes layouts for items that are about to become visible.

`RecyclerBinder` is the part of Litho that:

 - Serves as an `Adapter` for a `RecyclerView`
 - Defines the layout to use in the `RecyclerView` (ex Linear, Grid)
 - Manages all the complexity of computing layouts ahead of time in a background thread.

![Layout range in action](/static/images/range_small.gif "Layout range in action")

Let's start creating a `RecyclerBinder`:

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder(c);
```
This will create the simplest possible `RecyclerBinder` that will layout the content of the `Recycler` as a vertical list.

To have `Recycler` use a grid layout we can use this constructor instead:

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder(c, new GridLayoutInfo(c, spanCount);
```

`RecyclerBinder` exposes a set of APIs to manipulate the items that will be displayed in the `Recycler`.

The most commonly used are:

``` java
recyclerBinder.insertItemAt(position, component);
recyclerBinder.updateItemAt(position, component);
recyclerBinder.removeItemAt(position);
recyclerBinder.moveItem(fromPosition, toPosition);
```

`RecyclerBinder`'s API works directly with components. Since a component is only a collection of **props**, we can build any component ahead of time and leave the layout management to the `RecyclerBinder`.

`RecyclerBinder` also supports receiving extra information about the way a component should be laid out. These extra information can be passed in through a `ComponentInfo`. Here's what the code looks like:

``` java
recyclerBinder.insertItemAt(
  position,
  ComponentInfo.create()
      .component(component)
      .isSticky(true)
      .build());
```

#### Using RecyclerBinder with DiffUtil

RecyclerBinder exposes by default bindings to be used in conjunction with [DiffUtil](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html).
Litho defines in its API a [RecyclerBinderUpdateCallback](/javadoc/com/facebook/litho/widget/RecyclerBinderUpdateCallback.html) that implements `ListUpdateCallback` and therefore can be used to dispatch the `DiffResult` to a `RecyclerBinder`.

Here's an example of how `DiffUtil` can be used with Litho:

``` java

  private final ComponentRenderer<Data> mComponentRenderer = new ComponentRenderer<> {
    ComponentInfo render(Data data, int idx) {
      return ComponentInfo.create()
          .component(
          	DataComponent.create(mComponentContext)
          	    .data(data))
          	    .build();
    }
  }

  public void onNewData(List<Data> newData) {
    final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MyDataDiffCallback(mCurrentData, newData));
    final RecyclerBinderUpdateCallback callback = RecyclerBinderUpdateCallback.acquire(
      mCurrentData.size(),
      newData,
      mComponentRenderer,
      mRecyclerBinder)

    diffResult.dispatchUpdatesTo(callback);
    callback.applyChangeset();
    RecyclerBinderUpdateCallback.release(callback);
}
```

The `ComponentRenderer` will be invoked whenever a new component needs to be created for a new or updated model in the list.
