---
docid: inc-mount
title: Incremental Mount
layout: docs
permalink: /docs/inc-mount
---

Even though components provide flatter view hierarchies and perform [layout off the main thread](/docs/async-layout), the mount operation (creating, recycling and attaching views and drawables) can still have a cost in the UI thread for very complex components, especially ones containing many views.

The Components framework can transparently spread the cost of mounting components across UI frames to avoid jank.

With incremental mount enabled (which it is by default), the `ComponentView` will only mount enough content to fill its visible region and unmount (and recycle) components that are no longer visible.

![Incremental Mount Diagram](/static/images/incremental-mount.png)

If you are using Components' [async RecyclerView](/docs/recycler-component) support, the framework will seamlessly perform incremental mount.

## Manual incremental mount

If you're not using `RecyclerComponent`, you can still integrate incremental mount in your existing UI implementation. You'll have to explicitly notify the framework every time the `ComponentView`'s visible region changes, by calling:

```java
myComponentView.performIncrementalMount();
```

For example, you'd call `performIncrementalMount()` in an `OnScrollListener` if you're using components in a `ListView`.
