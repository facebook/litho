---
docid: glossary
title: Glossary
layout: docs
permalink: /docs/glossary
---

Spec? Prop? State? Tree? What do all of these terms mean when it comes to Litho and Section Components?

## Component
A single logical unit describing a piece of UI.  Components are either comprised of other components or contain logic to draw an Android View or Android Drawable on screen. Section Components components are sometimes referred to Sections.

## Spec
Spec classes (files that end in Spec.java) are classes you write to *specify* the behavior of a custom component.  You can think of spec classes as a collection of functions that the framework will call to interact with your component. 

Litho and Section Components' code-generation framework reads your spec to auto-generate code specific to each spec.

## @Prop
An annotation added to function parameters in your Spec class to describe **immutable** values that will be passed into the component during creation. Props can represent values that can be changed from outside the framework.  You can update a prop value from outside the framework calling `LithoView#setComponent(Component)` or `ComponentTree#setRoot(Component)`. 

When a new component is set, the framework will compare the new props with previous props and, if they differ, update that component. This behavior recurses down the component hierarchy until every component in the tree has had the chance to update.

## @State
An annotation added to function parameters in your Spec class to describe **immutable** values that are stored *inside this component* and *only matter for this component and its children*.

State values are initially set in a function annotated with @OnCreateInitialState and should only be updated by functions annotated with @UpdateState. Whenever a state update is made, the framework will update that component and recurse down its children until every component in the sub-tree has had the chance to update.

## @LayoutSpec
A Litho Annotation added to a spec class that tells the framework "This component is comprised of other components arranged in a specific layout". The majority of specs you write will be LayoutSpecs. 

## @MountSpec
A Litho Annotation added to a spec class that tells the framework "This component describes an Android View or Drawable to be drawn on screen". This is the escape hatch from Litho into traditional Android rendering. 

## @GroupSectionSpec
The equivalent of @LayoutSpec for Section Components

## @DiffSectionSpec
The equivalent of @MountSpec for Section Components. This annotation tells the
framework "This component will produce a changeset for changing the items in a
recycler view".

## ChangeSet

A list of insert / update / delete / move "commands" that describe how to update
the underlying recycler view.
