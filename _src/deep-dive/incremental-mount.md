---
id: incremental-mount
title: Incremental Mount
---

Even though components provide flatter view hierarchies and perform [layout off the main thread](/docs/asynchronous-layout), the mount operation (creating, recycling and attaching views and drawables) can still have a cost in the UI thread for very complex components, especially for the ones containing many views.
Since there's no benefit of maintaining views outside the viewport, Incremental Mount plays a pivotal role in boosting overall performance by ensuring such views are excluded from the view hierarchy.

:::note
It is easy to confuse Litho's Incremental Mount feature with similar Android features such as view recycling in a RecyclerView. However, unlike Android's RecyclerView, Incremental Mount operates on a view-by-view resolution, rather than entire list items as in a RecyclerView.
This means that unlike with a RecyclerView, when an individual view has exited the viewport, it will be unmounted from the view hierarchy, even if its container is partially visible within the viewport.
:::

With Incremental Mount enabled (which it is by default), the `LithoView` will only mount enough content to fill its visible region and unmount (and recycle) components that are no longer visible.

![Incremental Mount Diagram](/images/incremental-mount.png)

When using a prebuilt collection component such as `RecyclerCollectionComponent` or a [Lazy Collection](../kotlin/lazycollections/lazycollections.mdx), the framework will seamlessly perform Incremental Mount.
When not using a prebuilt collection component (such as [Recycler](pathname:///javadoc/com/facebook/litho/widget/Recycler.html)), or when manually resizing a `LithoView`'s container, you can still integrate Incremental Mount in your existing UI implementation.

```java
myLithoView.notifyVisibleBoundsChanged();
```

For example, if a `LithoView` is nested within a custom scrolling container, `myLithoView.notifyVisibleBoundsChanged()` should be called within a `OnScrollListener`.
