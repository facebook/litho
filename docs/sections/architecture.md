---
id: architecture
title: Sections Implementation Architecture
---

At its core, the Sections framework is responsible for producing a [ChangeSet](pathname:///javadoc/com/facebook/litho/sections/ChangeSet.html) from immutable props and a hierarchy of [Sections](pathname:///javadoc/com/facebook/litho/sections/Section.html). The framework produces these `ChangeSets` by creating a new section hierarchy whenever a `SectionTree` is set with a Section with new props, or whenever a Section in the hierarchy updates its internal state when comparing the new hierarchy with the old hierarchy.

## What is a SectionTree?

Using the Sections framework begins with creating a [SectionTree](pathname:///javadoc/com/facebook/litho/sections/SectionTree.html).

`SectionTree` instances are responsible for:

* Computing/recomputing changes whenever state & props values change.
* Communicating with a [Target](pathname:///javadoc/com/facebook/litho/sections/SectionTree.Target.html) implementation that can update the UI (including telling the `Target` about new changes).

SectionTrees must be created with a `Target` implementation. The [Target](pathname:///javadoc/com/facebook/litho/sections/SectionTree.Target.html) interface is the API between `SectionTree` and the UI.

After computing a ChangeSet from a section hierarchy, a `SectionTree` instance will relay the changes to its `Target`. You can create a `Target` for whatever custom UI you want, but the Sections framework has already implemented some `Targets` for you. [SectionBinderTarget](pathname:///javadoc/com/facebook/litho/sections/widget/SectionBinderTarget.html) is a `Target` implementation that relays changes to a `RecyclerBinder` for rendering.

## Updating the SectionTree

The framework can perform incremental and conditional updates on the structure of Sections whenever any props or state values change. The infrastructure also calculates the minimal operations it needs to perform on the existing hierarchy to update the list to reflect the new data.

To update a section tree to reflect new props, create a section with the new prop values and call [SectionTree#setRoot()](pathname:///javadoc/com/facebook/litho/sections/SectionTree.html#setRoot-com.facebook.litho.sections.Section-). This is also how you set an initial root section on a tree since it's essentially diffing a new section hierarchy with an empty hierarchy.

To update a section tree when a state value changes, perform a regular state update, as described in the [State for Specs](/codegen/state-for-specs.md) page of the Litho documentation.

You may notice that the `setRoot()` and `updateState()` methods also have 'async' implementations, which are (`setRootAsync()` and `updateStateAsync()`) respectively.  The `*async()` methods will ensure that the resulting ChangeSet calculation is performed on a background thread.  Otherwise, the resulting ChangeSet calculation will be done synchronously on whatever thread `setRoot()` or `updateState()` was called.

## Computing ChangeSets

`SectionTree` instances compute changes in two steps: [generating trees](#generating-trees) based on props/state values, then [generating a changeset](#generating-a-changeset) by comparing two trees.

### Generating Trees

A tree is generated from a single root section by recursively calling `@OnCreateChildren` on group section specs until it reaches the leaf sections, diff section specs.  As it visits a new section, `SectionTree` will:

* Create a new `SectionContext` scoped to this new section.
* Check if there's a corresponding section in the current hierarchy, via [key](/codegen/state-for-specs.md#keys-and-identifying-components)) and transfer any state and service values over to the new section.
* Check if there's any pending state updates for the new section, via [key](/codegen/state-for-specs.md#keys-and-identifying-components)) and perform the updates if they exist.
* Create the new child sections by calling `SectionLifecycle#createChildren` then recursively visit those child sections.

### Generating a Changeset

After generating a new tree, `SectionTree` will recursively traverse the new tree and compare it against the current tree to generate a `ChangeSet`. This is where you call `SectionLifecycle#generateChangeSet` on Diff Sections. When traversing the new tree, the framework translates local indexes to global indexes as it merges all `ChangeSet`s into a single `ChangeSet` for the whole hierarchy.

:::note
[SectionContext](pathname:///javadoc/com/facebook/litho/sections/SectionContext.html) is an object that is used to associate each `Section` instance in a hierarchy with its `SectionTree`. `SectionContext` instances are released and recreated every time a `SectionTree` re-calculates its changeset (anytime props or state change). This means you should not rely on the `SectionContext` passed into your spec delegate methods to always be associated with a valid `Section` instance. As a general rule, a `SectionContext` object is only valid between the `@OnBindService` and `@OnUnbindService` methods. You should not keep an instance of `SectionContext` alive outside this window.
:::

### SectionTree and the RecyclerCollectionComponent

[RecyclerCollectionComponent](recycler-collection-component.md) is a Litho component that creates and binds a `SectionTree` to a `Recycler` behind the scenes to make it incredibly easy to use the Sections framework with Litho. `RecyclerCollectionComponent` creates and holds onto a `SectionTree` instance as state and exposes a prop to accept new sections.  Updating the SectionTree when using `RecyclerCollectionComponent` is as simple as updating the section prop passed into it.
