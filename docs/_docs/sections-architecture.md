---
docid: sections-architecture
title: Sections architecture
layout: docs
permalink: /docs/sections-architecture
metatags: noindex, follow
---

At it's core, the Sections framework is responsible for producing a [ChangeSet](javadocs/com/facebook/litho/sections/ChangeSet.java) from immutable props and a hierarchy of [Sectons](javadoc/com/facebook/litho/sections/Section.java). The framework produces these `ChangeSets` by creating a new section hierarchy whenever a `SectionTree` is set with a Section with new props or whenever a Section in the hierarchy updates it's internal state and comparing the new hierarchy with the old hierarchy.


## [SectionTree](/javadoc/com/facebook/litho/sections/SectionTree)

Using the Sections framework begins with creating a [SectionTree](/javadoc/com/facebook/litho/sections/SectionTree). `SectionTree` instances are responsible for:
   - Computing/Recomputing changes whenever state & props values change.
   - Communicat with a [Target](/javadoc/com/facebook/litho/sections/SectionTree.Target) implementation that can update UI (including telling the `Target` about new changes).

SectionTrees must be created with a `Target` implementation. The [Target](/javadoc/com/facebook/litho/sections/SectionTree.Target) interface is the API between `SectionTree` and UI. After computing a ChangeSet from a section hierarchy, a `SectionTree` instance will relay the changes to it's `Target`. You can create a target for whatever custom UI you want but the Sections framework has already implemented some `Targets` for you. [SectionBinderTarget](/javadoc/com/facebook/litho/widget/SectionBinderTarget) is one a `Target` implementation that relays changes to a `RecyclerBinder` for rendering.


## Section Hierarchies and SectionTree

`SectionTree` begins computing changes whenever props and state change.  To update the props of a SectionTree, create a section with the new prop values and call [SectionTree#setRoot()](/javadoc/com/facebook/litho/sections/SectionTree#setRoot).

<....>


   Similar to Litho components, each `Section` in the hierarchy has a unique global key that's typically the path to the section (i.e. if C's a child of B's a child of A, C.key() = "ABC").  This means if two instances of the same `Section` are direct siblings, you'll need to manually set the key for one of them when creating the `Section`.

## Computing ChangeSets

`SectionTree` instances compute changesets in two steps: generating trees based on props/state values, and creating changesets by comparing two trees. (See `SectionTree#calculateNewChangeSet`)

A tree is generated from a single root section by recursively calling `@OnCreateChildren` on group section specs until it reaches the leaf sections, diff section specs.  As it visits a new section, `SectionTree` will:
 - Create a new `SectionContext` scoped to this new section
 - Check if there's a corresponding section in the current hierarchy (via key) and transfer any state and service values over to the new section.
 - Check if there's any pending state updates for the new section (via key) and perform the updates if they exist.
 - Create the new child sections by calling `SectionLifecycle#createChildren` and recursively visit those child sections.

(See `SectionTree#createNewTreeAndApplyStateUpdates`)

After generating a new tree, `SectionTree` will recursively traverse the new tree and compare it against the current tree to generate a `ChangeSet`. This is where we call `SectionLifecycle#generateChangeSet` on Diff Sections. When traversing the new tree, the framework translates local indexes to global indexes as it merges all `ChangeSet`s into a single `ChangeSet` for the whole hierarchy.  (See `ChangeSetState.generateChangeSet`)


NOTE: `SectionContext` an object that is used to associate each `Section` instance in a hierarchy with the `SectionTree` it's a part of.  `SectionContext` instances are released and recreated every time a `SectionTree` re-calculates it's changeset (anytime props or state change). This means you should not rely on the `SectionContext` passed into your spec delegate methods to always be associated with a valid `Section` instance.  As a general rule, a `SectionContext` object is only valid between the `@OnBindService` and `@OnUnbindService` methods. You should not keep an instance of `SectionContext` alive outside this window.


# RecyclerCollectionComponent

RecyclerCollectionComponent is a Litho component that creates and binds a `SectionTree` to a `Recycler` behind the scenes to make it incredibly easy to use the Sections framework with Litho. `RecyclerCollectionComponent` creates and holds onto a `SectionTree` instance as state and exposes a prop to accept new sections. This is the easiest way to creates scrollable UI powered by Sections.  `RecyclerCollectionComponent` also provides additional functionality that's useful for lists such as vertical & horizontal scroll support, default empty & loading components, and pull-to-refresh support.

