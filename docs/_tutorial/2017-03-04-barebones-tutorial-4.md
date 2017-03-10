---
title: Barebones Tutorial Part 4/5
layout: tutorial_post
author: rspencer
category: tutorial
---

## Feed Me!

Feeds in Litho are based upon the `Recycler` component.  This component is similar, conceptually, to the Android `RecyclerView`.  However, with Litho, all the layout is performed in a separate thread, giving a substantial performance boost.  In this tutorial, we'll write a `Binder` that provides component to a `Recycler`, in the same way an `LayoutMangager` and `Adapter` combination provides `View`s to a `RecyclerView`.

<!--truncate-->

The first thing we need to do is add the android support recyclerview to the libs, much as we did for soloader in the first tutorial:

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

Then, add it as a dependency of `/src/main/java/com/company/tutorial:tutorial`.  This is required for our `Binder`, which we shall name `FeedBinder` and define as follows.

``` java
package com.company.tutorial;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.LinearComponentBinder;

public class FeedBinder extends LinearComponentBinder {

    FeedBinder(Context c) {
        super(c, new LinearLayoutManager(c));
    }

    @Override
    protected int getCount() {
        return 32;
    }

    @Override
    public Component<?> createComponent(ComponentContext c, int position) {
        return FeedItem.create(c)
                .build();
    }
}
```

The above code is mostly self explanatory.  Refer to the docs if you want a more in-depth explanation of how binders work.

A binder is added to a `Recycler` very simply.  Just add this to the activity:

``` java
final ComponentTree componentTree = ComponentTree.create(context,
        Recycler.create(context)
                .binder(new FeedBinder(context)))
        .build();
```

It's that simple.  Running the app gives a scrollable list of 32 "Hello World" compoennts:

<img src="/static/images/barebones3.png" style="width: 300px;">

