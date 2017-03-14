---
title: Barebones Tutorial Part 5/5
layout: tutorial_post
author: rspencer
category: tutorial
---

## Properties

Feeds are no good if they only contain repetitive copies of a single component.  In this tutorial, we will look at _properties_: attributes you can set on components you define in order to change their behavior or appearance.

<!--truncate-->

Adding properties to a component is very simple.  Properties are simply parameters to methods of the specification, annotated with the `@Prop` annotation.  Lets add some properties to our `FeedItem` that will change the appearance of the component.  We'll add a `message` and `color` property.

``` java
@OnCreateLayout
static ComponentLayout onCreateLayout(
    ComponentContext c,
    @Prop int color,
    @Prop String message) {
  return Container.create(c)
      .paddingDip(ALL, 16)
      .backgroundColor(color)
      .child(
          Text.create(c)
              .text(message)
              .textSizeSp(40)
              .build())
      .build();
}
```

The magic is in the `@Prop` annotations and the annotation processor.  The processor produces methods on the builder that correspond to the properties in a smart way.  Thus, we simply change the binder's construction of the component to

``` java
private void addContent(RecyclerBinder recyclerBinder, ComponentContext context) {
  for (int i = 0; i < 32; i++) {
    ComponentInfo.Builder componentInfoBuilder = ComponentInfo.create();
    componentInfoBuilder.component(
        FeedItem.create(context)
            .color(i % 2 == 0 ? Color.WHITE : Color.LTGRAY)
            .message("Hello, world!")
            .build());
    recyclerBinder.insertItemAt(i, componentInfoBuilder.build());
  }
}
```

The only change is in lines 5 to 8.  This gives

<img src="/static/images/barebones4.png" style="width: 300px;">

You can specify more options to the `@Prop` annotation.  For example, consider the property

``` java
@Prop(optional = true, resType = ResType.DIMEN_OFFSET) int shadowRadius,
```

This tells the annotation processor to construct a number of functions, such as `shadowRadiusPx`, `shadowRadiusDip`, `shadowRadiusSp` as well as `shadowRadiusRes`.  For more information, see the full documentation.

This concludes the bare bones tutorial, and the code for the finished product can be found [here][barebones-sample].  For more in-depth code, check out the [sample][sample], as well as the documentation elsewhere on this website.

[sample]: https://github.com/facebookincubator/c4a/blob/master/sample/
[barebones-sample]: https://github.com/facebookincubator/c4a/blob/master/sample-barebones/
