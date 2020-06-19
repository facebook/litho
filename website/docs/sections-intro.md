---
id: sections-intro
title: What are Sections?
---

**The Sections API is a declarative, composable, and thread-safe API for writing highly-optimized list screens, built on top of Litho.** It tries to address issues we've had at Facebook when writing complex lists, such as maintaining many view types, handling multiple data sources and composing lists together.

When building a list with RecyclerView, you have to think about how to maintain the adapters in sync with your data and notify the adapters of any changes. This typically requires manual index handling and results in stateful, imperative code that is difficult to maintain and reason about as your product grows. RecyclerView adapters are also difficult to compose and integrating multiple data sources into the same surface is not trivial.

Instead, the Sections API looks a lot like the Components API: your list is defined by a root Section. That root section can have children which are either single rows, a list of rows, or other nested Sections. This tree ends up resolving to a flat list of rows that the RecyclerView can understand. Any updates for your list are sent to the Adapter using the standard granular notification calls (e.g. `notifyItemInserted`) -- Sections basically eliminated `notifyDataSetChanged` except in exceptional circumstances!

While Litho Components are used for displaying pieces of UI, Sections are a way of composing data into a list of Litho Components or Views. If you visualize your screen as being a tree of components, the nodes for the root of the tree and the subtrees are Sections, while the leaves are Litho Components that represent individual items that will be displayed on screen.

<!-- <img src="/static/images/sections-intro.png" style="width: 800px;" /> -->

Sections use the same declarative data model as Litho and under the hood transparently handles things like calculating minimal sets of changes for data updates and doing granular UI refreshes.
As part of Litho, the Sections API shares the same main concepts such as annotation-based code generation, event handling, props and state updates.

For easy integration with Litho, the framework provides a built-in Component that can render a hierarchy of Sections, called [RecyclerCollectionComponent](/javadoc/com/facebook/litho/sections/widget/RecyclerCollectionComponent.html). The Sections hierarchy becomes a “data source” for the RecyclerCollectionComponent, and the Components that render your data will become items in the RecyclerView adapter under the hood.  All the complexity of handling operations on your adapter, such as inserts or removes, is hidden away and handled by the infrastructure.

The [Litho tutorial](/docs/tutorial) shows a glimpse on how to integrate a Section to render inside a LithoView. Follow our more detailed [Sections tutorial](/docs/sections-tutorial) for a guide on how to create and compose Sections.
