---
docid: view-flattening
title: View Flattening
layout: docs
permalink: /docs/view-flattening
---

Let's take a look at the layout in the example below. It contains an image, a title and subtitle. With the traditional Android View system, you would have a view for all these elements, wrapped in a few view groups for arranging the items.

![View Flattening](/static/images/viewflatteningnobounds.jpeg)

Litho automatically reduces the number of views your final UI hierarchy will contain. The layout tree that results from the layout calculation step is just a blueprint of your UI with no direct coupling with Android views. This allows the framework to process the layout tree for optimal rendering performance before the component is mounted.  
We do this in two ways.

First, Litho can completely skip containers after layout calculation because they are not used in the mount step. For our example, there won't be a view group wrapping title and subtitle when it is mounted.

Second, Litho can mount either a view or a drawable. In fact, most of the core widgets in the framework, such as Text and Image, mount drawables, not views.

As a result of these optimizations, the component for the UI in the example would actually be rendered as a single, completely flat, view. You can see this in the following screenshot with the [Show layout bounds developer option enabled](/docs/developer-options#debughighlightmountbounds).

![View Flattening](/static/images/viewflattening.png)

While flatter views have important benefits for memory usage and drawing times, they are not a silver bullet for everything. Litho has a very general system to automatically "unflatten" the view hierarchy of mounted components when we want to lean on non-trivial features from Android views such as touch event handling, accessibility, or confining invalidations. For instance, if you wanted to enable clicking on the image or text in the example, the framework would automatically wrap them in a view if they have [click handlers](/docs/events-overview#callbacks).
