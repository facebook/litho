---
title: Barebones Tutorial Part 3/5
layout: tutorial_post
author: rspencer
category: tutorial
---

## 3. A First Component

The goal of these barebones tutorials is some sort of simple, scrollable feed.  This feed will just say "Hello World" a whole lot of times.  In this tutorial, we'll look at defining one of the elements (the "Hello World"s) that appear in the feed.  Naturally, in full scale applications, elements will be substantially more complicated components.

<!--truncate-->

Let us dive in and build this component.  In Litho, these are defined by _Spec_ classes.  We'll call our component `FeedItem`.  So thus we define a class called `FeedItemSpec`.

``` java
import android.graphics.Color;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.widget.Text;
import static com.facebook.yoga.YogaEdge.ALL;

@LayoutSpec
public class FeedItemSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(ComponentContext c) {
    return Container.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(Color.WHITE)
        .child(
            Text.create(c)
                .text("Hello World")
                .textSizeSp(40)
                .build())
        .build();
  }
}
```

Some of this is familiar.  We see the `Text` component from before.  However, now we are passing it as a "child" property of a `Container`.  You can think `Container`s like `<div>`s in HTML.  Its a wrapper, usually simply used for collating things together (and some background styling).  In fact, since components uses [Yoga][yoga], you can add flexbox attributes to layout the children of a `Container`.  Here, we simply set the padding and background color.

How do we use this component?  Its rather simple.  In the `SampleActivity`, simply change the `ComponentTree` definition to

``` java
final ComponentTree componentTree = ComponentTree.create(
    context,
    FeedItem.create(context))
        .build();
```

**Note** That's `FeedItem`, not `FeedItemSpec`.

Where did `FeedItem` come from?  And where are `create` and `build` defined?  This is the magic of Specs.  We need to add to our buck target `/src/main/java/com/company/tutorial:tutorial`

``` python
plugins = [
        INSERT_ANNOTATION_TARGET_HERE,
    ],
```

This runs an annotation processor over our code.  It looks for `(.*)Spec` class names and constructs `(.*)` classes.  These classes will have all the methods required by Litho automatically filled in.  In addition, based upon the specification, there will be extra methods (such as `Text`'s `.textSizeSp` or the `.backgroundColor` method of `Container`).

But that's as simple as it is.  Run and you should see

<img src="/static/images/barebones2.png" style="width: 300px;">

[yoga]: https://facebook.github.io/yoga/
