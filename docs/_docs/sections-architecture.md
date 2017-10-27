---
docid: sections-architecture
title: Sections architecture
layout: docs
permalink: /docs/sections-architecture
---

At it's core, the Sections framework is responsible for producing a [ChangeSet](/javadocs/com/facebook/litho/sections/ChangeSet.java) from immutable props and a hierarchy of [Sections](/javadoc/com/facebook/litho/sections/Section.java). The framework produces these `ChangeSets` by creating a new section hierarchy whenever a `SectionTree` is set with a Section with new props or whenever a Section in the hierarchy updates it's internal state and comparing the new hierarchy with the old hierarchy.

## What is a SectionTree?

Using the Sections framework begins with creating a [SectionTree](/javadoc/com/facebook/litho/sections/SectionTree). `SectionTree` instances are responsible for:
   - Computing/Recomputing changes whenever state & props values change.
   - Communicat with a [Target](/javadoc/com/facebook/litho/sections/SectionTree.Target) implementation that can update UI (including telling the `Target` about new changes).

SectionTrees must be created with a `Target` implementation. The [Target](/javadoc/com/facebook/litho/sections/SectionTree.Target) interface is the API between `SectionTree` and UI. After computing a ChangeSet from a section hierarchy, a `SectionTree` instance will relay the changes to it's `Target`. You can create a target for whatever custom UI you want but the Sections framework has already implemented some `Targets` for you. [SectionBinderTarget](/javadoc/com/facebook/litho/sections/widget/SectionBinderTarget) is one a `Target` implementation that relays changes to a `RecyclerBinder` for rendering.

## Updating the SectionTree

The framework can perform incremental and conditional updates on the structure of Sections whenever any props or state values change. The infrastructure also calculates the minimal operations it needs to perform on the existing hierarchy to update the list to reflect the new data.

To update a section tree to reflect new props, create a section with the new prop values and call [SectionTree#setRoot()](/javadoc/com/facebook/litho/sections/SectionTree.html#setRoot-com.facebook.litho.sections.Section-). This is also how you set an initial root section on a tree since it's essentially diffing a new section hierarchy with an empty hierarchy.

To update a section tree when a state value changes, just perform a regular state update as described in the documentation for litho [State](/docs/state).

You may notice that the `setRoot()` and `updateState()` methods also have "async" implementations (`setRootAsync()` and `updateStateAsync()`).  The `*async()` methods will ensure that the resulting ChangeSet calculation is performed on a background thread.  Otherwise the resulting ChangeSet calculation will be done synchronously on whatever thread `setRoot()` or `updateState()` was called. This is just like Litho's [asychronous layout](/docs/asynchronous-layout#sync-and-async-operations).

## Computing ChangeSets

`SectionTree` instances compute changes in two steps: generating trees based on props/state values, then creating a changeset by comparing two trees.

A tree is generated from a single root section by recursively calling `@OnCreateChildren` on group section specs until it reaches the leaf sections, diff section specs.  As it visits a new section, `SectionTree` will:
 - Create a new `SectionContext` scoped to this new section
 - Check if there's a corresponding section in the current hierarchy (via [key](/docs/state#keys-and-identifying-components)) and transfer any state and service values over to the new section.
 - Check if there's any pending state updates for the new section (via [key](/docs/state#keys-and-identifying-components)) and perform the updates if they exist.
 - Create the new child sections by calling `SectionLifecycle#createChildren` and recursively visit those child sections.

After generating a new tree, `SectionTree` will recursively traverse the new tree and compare it against the current tree to generate a `ChangeSet`. This is where we call `SectionLifecycle#generateChangeSet` on Diff Sections. When traversing the new tree, the framework translates local indexes to global indexes as it merges all `ChangeSet`s into a single `ChangeSet` for the whole hierarchy.

NOTE: [SectionContext](/javadoc/com/facebook/litho/sections/SectionContext) is an object that is used to associate each `Section` instance in a hierarchy with it's `SectionTree`.  `SectionContext` instances are released and recreated every time a `SectionTree` re-calculates it's changeset (anytime props or state change). This means you should not rely on the `SectionContext` passed into your spec delegate methods to always be associated with a valid `Section` instance.  As a general rule, a `SectionContext` object is only valid between the `@OnBindService` and `@OnUnbindService` methods. You should not keep an instance of `SectionContext` alive outside this window.


### SectionTree and RecyclerCollectionComponent

[RecyclerCollectionComponent](/docs/recycler-collection-component) is a Litho component that creates and binds a `SectionTree` to a `Recycler` behind the scenes to make it incredibly easy to use the Sections framework with Litho. `RecyclerCollectionComponent` creates and holds onto a `SectionTree` instance as state and exposes a prop to accept new sections.  Updating the SectionTree when using RecyclerCollectionComponent is as simple as updating the section prop passed into it.



