---
id: incremental-mount
title: Incremental Mount
---

:::danger Content needs to be updated
Moved from old website without any change.
:::

Even though components provide flatter view hierarchies and perform [layout off the main thread](/docs/asynchronous-layout), the mount operation (creating, recycling and attaching views and drawables) can still have a cost in the UI thread for very complex components, especially for the ones containing many views.

Litho can transparently spread the cost of mounting components across UI frames to avoid jank.

With incremental mount enabled (which it is by default), the `LithoView` will only mount enough content to fill its visible region and unmount (and recycle) components that are no longer visible.

![Incremental Mount Diagram](/images/incremental-mount.png)

If you are using the Litho [async RecyclerView](/docs/recycler-component) support, the framework will seamlessly perform incremental mount.

## Manual incremental mount

If you're not using the [Recycler](pathname:///javadoc/com/facebook/litho/widget/Recycler.html) component, you can still integrate incremental mount in your existing UI implementation. You'll have to explicitly notify the framework every time the `LithoView`'s visible region changes, by calling:

```java
myLithoView.performIncrementalMount();
```

For example, you'd call `performIncrementalMount()` in an `OnScrollListener` if you're using components in a `ListView`.
