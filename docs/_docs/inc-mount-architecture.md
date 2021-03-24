---
docid: inc-mount-architecture
title: Incremental Mount
layout: docs
permalink: /docs/inc-mount-architecture
---

Even though components provide flatter view hierarchies and perform [layout off the main thread](/docs/asynchronous-layout), the mount operation (creating, recycling and attaching views and drawables) can still have a cost in the UI thread for very complex components, especially for the ones containing many views.

Litho can transparently spread the cost of mounting components across UI frames to avoid jank.

With incremental mount enabled (which it is by default), the `LithoView` will only mount enough content to fill its visible region and unmount (and recycle) components that are no longer visible.
  
![Incremental Mount Diagram](/static/images/incremental-mount.png)

If you are using the Litho [async RecyclerView](/docs/recycler-component) support, the framework will seamlessly perform incremental mount.
