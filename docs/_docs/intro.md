---
docid: intro
title: What is Litho?
layout: docs
permalink: /docs/intro
---

Litho is a declarative framework to build efficient user interfaces (UI) on
Android. It allows you to write highly-optimized Android views through a simple
functional API based on Java annotations. It was [primarily built](/docs/uses)
to implement complex scrollable UIs based on RecyclerView.

With Litho, you build your UI in terms of *components* instead of interacting
directly with traditional Android views. A *component* is essentially a
function that takes immutable inputs, called *props*, and returns a component
hierarchy describing your user interface.

```java
@LayoutSpec
class HelloComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop String name) {

    return Text.create(c)
        .text("Hello, " + name)
        .textSizeRes(R.dimen.my_text_size)
        .textColor(Color.BLACK)
        .withLayout()
        .paddingDip(ALL, 10)
        .build();
  }
}
```

You simply declare what you want to display and Litho takes care of rendering
it in the most efficient way by computing [layout in a background
thread](/docs/background-layout), automatically [flattening your view
hierarchy](/docs/flatter-views), and [incrementally
rendering](/docs/incremental-mount) complex components.

Have a look at our [Tutorial](/docs/tutorial) for a step-by-step guide on using
Litho in your app. You can also read the quick start guide on how to
[write](/docs/writing-components) and [use](/docs/using-components) your own
Litho components.

