---
docid: inc-mount
title: Incremental Mount
layout: docs
permalink: /docs/inc-mount
---

## Manual incremental mount

If you're not using the [Recycler](/javadoc/com/facebook/litho/widget/Recycler) component, you can still integrate incremental mount in your existing UI implementation. You'll have to explicitly notify the framework every time the `LithoView`'s visible region changes, by calling:

```java
myLithoView.performIncrementalMount();
```

For example, you'd call `performIncrementalMount()` in an `OnScrollListener` if you're using components in a `ListView`.
