---
docid: sections-intro
title: What are Sections?
layout: docs
permalink: /docs/sections-intro
---

Litho is primarily a rendering API, but efficient UI rendering is only part of the challenge you have to deal with to write a performant surface.
If you take a closer look at the apps on your device, you'll notice that a lot of them center around a scrollable surface that fetches and displays lists of data. When you build a list with RecyclerView you have to think about how to maintain the adapters in sync with your data and notify the adapters of any changes. This typically requires a lot of manual index handling and it results in stateful, imperative code that is difficult to maintain and reason about as your product grows. RecyclerView adapters are also difficult to compose and integrating multiple data sources into the same surface is not trivial.

Sections are built on top of Litho to provide a declarative and composable API for writing highly-optimized list surfaces.
While Litho Components are used for displaying pieces of UI, Sections are a way of structuring the data and translating it into Litho Components. If you visualize your surface as being a tree of components, the nodes for the root of the tree and the subtrees are Sections, while the leaves are Litho Components that represent individual items that will be displayed on screen.

<img src="/static/images/sections-intro.png" style="width: 800px;">

Sections use the same declarative data model as Litho and under the hood transparently handles things like calculating minimal sets of changes for data updates and doing granular UI refreshes.
As part of Litho, the Sections API shares the same main concepts such as annotation-based code generation, event handling, props and state updates.

For easy integration with Litho, the framework provides a built-in Component that can render a hierarchy of Sections, called [RecyclerCollectionComponent](/javadoc/com/facebook/litho/sections/widget/RecyclerCollectionComponent.html). The Sections hierarchy becomes a “data source” for the RecyclerCollectionComponent, and the Components that render your data will become items in the RecyclerView adapter under the hood.  All the complexity of handling operations on your adapter, such as inserts or removes, is hidden away and handled by the infrastructure.

The [Litho tutorial](/docs/tutorial) shows a glimpse on how to integrate a Section to render inside a LithoView. Follow our more detailed [Sections tutorial](/docs/sections-tutorial) for a guide on how to create and compose Sections.
