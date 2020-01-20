---
docid: architecture-overview
title: Architecture Overview
layout: docs
permalink: /docs/architecture-overview
---

## Core Concepts

- **Component**. A bundle of immutable data (Props) and pure functions that can transform the props into UI. 
- **LithoView**. A subclass of `android.view.ViewGroup` that is used to render a `Component`. 
- **ComponentTree**. A class that holds a single root `Component` and controls its lifecycle. 
- **LayoutState**. Responsible for calculating a layout and holding onto the output of the calculation. 
- **MountState**. Responsible for mounting the `Component` into a `LithoView`. Each `LithoView` has exactly one `MountState`. 
- **InternalNode**. Class which represents a single node in the layout of a `Component`. 
- **LayoutOutput**. Lightweight representation of the output of the layout pass. The `LayoutState` produces a List of `LayoutOutput`s that are then mounted by the `MountState`. 

## Litho Lesson: Component to Screen

We recommend you start by watching this video: it covers most of the core concepts above, as well as the fundamentals of how a Litho component gets turned into a `View` on screen.

<iframe style="padding-top: 10px" width="560" height="315" src="https://www.youtube-nocookie.com/embed/t9wTHnCx5RM" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

## Layout

### Initializing Layout
Layout calculation is controlled by the `ComponentTree`. There are two mains ways that a `ComponentTree` will kick off a layout calculation: 
1. If the bounds are known in advance, then the user can call `setSizeSpec`/`setSizeSpecAsync` directly. 
2. When the `ComponentTree` is set on a `LithoView`, then `LithoView#onMeasure()` will cause a layout to be calculated, unless the `ComponentTree` already has a valid layout (e.g. from an earlier `setSizeSpecAsync` call). 

### Calculating Layout
1. `LayoutState#createTree`. The first step in calculating layout is creating the tree of components (a tree of `InternalNode`s). This is done by recursively resolving each Component in the hierarchy, which for `Row` and `Column` means creating a new `InternalNode` and adding each of their children, and for all other Components just means calling `onCreateLayout`. 
2. `LayoutState#measureTree`. The second step in the calculation is measuring the tree. To do this the `InternalNode`s just defer to their underlying Yoga nodes, and Yoga does the calculation for us. 
3. `LayoutState#collectResults`. Now that we have created and measured the tree, we collect the results into a lightweight representation of elements that need mounting and throw away the `InternalNode` tree. 

### Layout Diffing
When calculating new layout on a `ComponentTree` that already has an existing layout, it is common that much of the layout is actually quite similar to before (e.g. you might change the color of an icon when you click on it, but leave the remaining UI the same). In order to take advantage of the similarities in cases such as this, we do something called `layoutDiffing`. Each `LayoutState` that we create has a tree of `DiffNode`s that store the results of the layout that we calculated, including measurements. When creating a new `LayoutState` for a `ComponentTree` that already has one, we use the `DiffNode` tree to skip work where possible. 

## Mount

Mount (`MountState#mount`) takes place for the first time when the `LithoView` is first laid out, and again every time the visible portion of the `LithoView` changes (provided that incremental mount is enabled). The mount step receives a `Rect` indicating the current visible area of the LithoView, and it uses that `Rect` to determine which of the `LayoutOutput`s from the Layout phase need to be either mounted (if they are now on the screen and they weren't before) or unmounted (if they are no longer on the screen). 

For each Component that needs to be mounted, we first acquire the `View`/`Drawable` defined in the `MountSpec`'s `onCreateMountContent` method. If possible, we acquire the mount content from our recycling pool (which is stored on a per-context basis), but if the pool is empty, then we create a new one. After that, we call `mount`, and then `bind`. When unmounting, we call `unbind` and then `unmount`. 

Each time mount is called, we also call `MountState#processVisibilityOutputs`, which determines whether any visibility events need to be fired, and fires them if so. 

Mounting (including firing visibility events) is bound to the UI thread, so operations called here should be as lightweight as possible.

### Mount Diffing
Mount diffing is a technique that Litho uses to avoid unmounting and mounting content when it is not necessary. Before mounting, we look at the currently mounted content and compare it with the content that we want to mount. If the currently mounted content does not need updating, then we don't call `unmount`/`mount`. 
