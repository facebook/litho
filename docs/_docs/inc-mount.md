---
docid: inc-mount
title: Incremental Mount
layout: docs
permalink: /docs/inc-mount
---

## Manual incremental mount

If you're not using `RecyclerComponent`, you can still integrate incremental mount in your existing UI implementation. You'll have to explicitly notify the framework every time the `ComponentView`'s visible region changes, by calling:

```java
myComponentView.performIncrementalMount();
```

For example, you'd call `performIncrementalMount()` in an `OnScrollListener` if you're using components in a `ListView`.
