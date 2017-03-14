---
title: Barebones Tutorial Part 4/5
layout: tutorial_post
author: rspencer
category: tutorial
---

## Feed Me!

Feeds in Litho are based upon the `Recycler` component.  This component is similar, conceptually, to the Android `RecyclerView`.  However, with Litho, all the layout is performed in a separate thread, giving a substantial performance boost.  In this tutorial, we'll use a `RecyclerBinder` that provides component to a `Recycler`, in the same way an `LayoutMangager` and `Adapter` combination provides `View`s to a `RecyclerView`.

<!--truncate-->

The first thing we need to do is add the android support recyclerview to the libs, much as we did for SoLoader in the first tutorial:

``` python
java_library(
    name = "android-support-recyclerview",
    exported_deps = [
        ":android-support-recyclerview.jar",
    ],
)

prebuilt_jar(
    name = "android-support-recyclerview.jar",
    binary_jar = "android-support-v7-recyclerview.jar",
)
```

Then, add it as a dependency of `/src/main/java/com/company/tutorial:tutorial`.  Now we will construct a `RecyclerBinder` and attach it to a `Recycler`.  A `RecyclerBinder` takes a component context, a range ratio and a layout info as constructor parameters.  For this example, simply set the range ratio to 4, and construct a `LinearLayoutInfo` for the layout info.  For more information see the detailed docs.

Adding a `Recycler` component to the component tree is as simple as any other component, and setting the binder is straightforward.

``` java
final RecyclerBinder recyclerBinder = new RecyclerBinder(
    context,
    4.0f,
    new LinearLayoutInfo(this, OrientationHelper.VERTICAL, false));

final ComponentTree componentTree = ComponentTree.create(
    context,
    Recycler.create(context)
        .binder(recyclerBinder))
    .build();
```

Now we need to populate the binder.  For this, we will define a helper function in `SampleActivity`.  Binders take `ComponentInfo` classes that describe the components to be rendered by the `Recycler`.  In this case, we want a simple `ComponentInfo` that simply presents our `FeedItem` component.

``` java
private void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
  for (int i = 0; i < 32; i++) {
    ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
    componentInfoBuilder.component(FeedItem.create(context).build());
    recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
  }
}
```

It's that simple.  Call `addContent` somewhere in the main activity `onCreate` and running the app gives a scrollable list of 32 "Hello World" components:

<img src="/static/images/barebones3.png" style="width: 300px;">

