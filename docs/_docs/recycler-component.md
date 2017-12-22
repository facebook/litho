---
docid: recycler-component
title: Recycler
layout: docs
permalink: /docs/recycler-component
---

If you choose to use Litho without using Sections, you can still use the
[Recycler](/javadoc/com/facebook/litho/widget/Recycler.html) component to create lists.  `RecyclerCollectionComponent` uses `Recycler` under the hood.

> Using the Recycler directly is not encouraged. Litho provides a utility component called [RecyclerCollectionComponent](/docs/recycler-collection-component) for writing lists, which abstracts all the complexity of using `Recycler` and `RecyclerBinder`.

### Create a Recycler component

You can use `Recycler` as you would use any other component in the framework by building it and adding it as a child in your layout.

``` java
@OnCreateLayout
static Component onCreateLayout(
    final ComponentContext c,
    @Prop RecyclerBinder recyclerBinder) {
    
  return Recycler.create(c)
      .binder(recyclerBinder)
      .build();
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
final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
    .build(c);
```
This will create the simplest possible `RecyclerBinder` that will layout the content of the `Recycler` as a vertical list.

To have `Recycler` use a grid layout we set it on the Builder:

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder.Builder()
    .layoutInfo(new GridLayoutInfo(c, spanCount))
    .build(c);
```

`RecyclerBinder` exposes a set of APIs to manipulate the items that will be displayed in the `Recycler`.

The most commonly used are:

``` java
recyclerBinder.appendItem(component)
recyclerBinder.insertItemAt(position, component);
recyclerBinder.updateItemAt(position, component);
recyclerBinder.removeItemAt(position);
recyclerBinder.moveItem(fromPosition, toPosition);
```

`RecyclerBinder`'s API works directly with components. Since a component is only a collection of **props**, we can build any component ahead of time and leave the layout management to the `RecyclerBinder`.

`RecyclerBinder` also supports receiving extra information about the way a component should be laid out. These extra information can be passed in through a `ComponentRenderInfo`. Here's what the code looks like:

``` java
recyclerBinder.insertItemAt(
  position,
  ComponentRenderInfo.create()
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
    ComponentRenderInfo render(Data data, int idx) {
      return ComponentRenderInfo.create()
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
