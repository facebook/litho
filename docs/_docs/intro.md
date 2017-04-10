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
thread](/docs/architecture#asynchronous-layout), automatically [flattening your view
hierarchy](/docs/intro#view-flattening), and [incrementally
rendering](/docs/intro#incremental-mount) complex components.

Have a look at our [Tutorial](/docs/tutorial) for a step-by-step guide on using
Litho in your app. You can also read the quick start guide on how to
[write](/docs/writing-components) and [use](/docs/using-components) your own
Litho components.

## Incremental mount

Even though components provide flatter view hierarchies and perform [layout off the main thread](/docs/architecture#asynchronous-layout), the mount operation (creating, recycling and attaching views and drawables) can still have a cost in the UI thread for very complex components, especially for the ones containing many views.

Litho can transparently spread the cost of mounting components across UI frames to avoid jank.

With incremental mount enabled (which it is by default), the `ComponentView` will only mount enough content to fill its visible region and unmount (and recycle) components that are no longer visible.

![Incremental Mount Diagram](/static/images/incremental-mount.png)

If you are using the Litho [async RecyclerView](/docs/recycler-component) support, the framework will seamlessly perform incremental mount.

## View flattening
Let's take a look at the layout in the example below. It contains an image, a title and subtitle. With the traditional Android View system, you would have a view for all these elements, wrapped in a few view groups for arranging the items. 
![View Flattening](/static/images/viewflatteningnobounds.jpeg)

Litho automatically reduces the number of views your final UI hierarchy will contain. The layout tree that results from the layout calculation step is just a blueprint of your UI with no direct coupling with Android views. This allows the framework to process the layout tree for optimal rendering performance before the component is mounted.  
We do this in two ways.

First, Litho can completely skip containers after layout calculation because they are not used in the mount step. For our example, there won't be a view group wrapping title and subtitle when it is mounted.

Second, Litho can mount either a view or a drawable. In fact, most of the core widgets in the framework, such as Text and Image, mount drawables, not views.

As a result of these optimizations, the component for the UI in the example would actually be rendered as a single, completely flat, view. You can see this in the following screenshot with the [Show layout bounds developer option enabled](/docs/debugging#null__debughighlightmountbounds).

![View Flattening](/static/images/viewflattening.png)

While flatter views have important benefits for memory usage and drawing times, they are not a silver bullet for everything. Litho has a very general system to automatically "unflatten" the view hierarchy of mounted components when we want to lean on non-trivial features from Android views such as touch event handling, accessibility, or confining invalidations. For instance, if you wanted to enable clicking on the image or text in the example, the framework would automatically wrap them in a view if they have [click handlers](/docs/events-overview#callbacks).
