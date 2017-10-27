---
docid: diff-sections
title: DiffSection Specs
layout: docs
permalink: /docs/diff-sections
---

A *diff section spec* defines a section that explicitly outputs insert, update, and remove changes on the section hierarchy.

Diff section specs explicitly manage insertions, removals, and updates that a section performs whenever its states and props change.  You will find Diff Sections at the leaves of every section tree as they are the sections that actually specify the changes to be made to a list.

One example where you might want a custom diff section is if you receive the data you want to display in the form of incremental updates or diffs. This might happen if you're using something similar to [DiffUtil](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html) to process your data. (If you are using `DiffUtil` though, consider using the prebuilt [DataDiffSection](/javadoc/com/facebook/litho/sections/common/DataDiffSection) instead of rolling your own diff section.)

In general, you should not need to write your own diff sections specs.  The `com.facebook.litho.sections.widget` package provides two diff sections that cover almost all use cases.

Let's use the example of [SingleComponentSection](/javadoc/com/facebook/litho/sections/common/SingleComponentSection) to describe how to write diff section specs.  Here is a snippet of `SingleComponentSectionSpec`:

```java
@DiffSectionSpec
class SingleComponentSectionSpec {

  @OnDiff
  static void onCreateChangeSet(
      SectionContext c,
      ChangeSet changeSet,
      @Prop Diff<Component> component,
      ...) {

    if (component.getNext() == null) {
      changeSet.delete(0);
    } else if (component.getPrevious() == null) {
      changeSet.insert(
          0,
          ComponentRenderInfo.create()
              .component(component.getNext())
              ...
              .build());
    } else {
      changeSet.update(
          0,
          ComponentRenderInfo.create()
              .component(component.getNext())
              ...
              .build());
    }
  }
}
```

As you can see, diff section specs use the `@DiffSectionSpec` annotation. Implementing a diff section spec is simple. You only have to write one method annotated with `@OnDiff`.

The method annotated with `@OnDiff` must have as its first and second argument a [SectionContext](/javadoc/com/facebook/litho/sections/SectionContext) and a [ChangeSet](/javadoc/com/facebook/litho/sections/ChangeSet) respectively. Following these two arguments, your `@OnDiff` method can also accept any number of arguments annotated with `@Prop` or `@State`.

These props and state have a special type: `Diff<T>`.  If your prop is defined in another annotated method like `@Prop String prop1`, it must be defined as `@Prop Diff<String> prop1` when being used in the `@OnDiff` method. The reason for this `Diff<T>` type wrapper is so we can compare previous prop values with new prop values when computing changes.


## Making changes to the list with ChangeSet

The `ChangeSet` argument of the `@OnDiff` method is used by the Diff section spec to specify how the section changes in response to new data.  The `@OnDiff` method will always be called with the current *and previous* props and state values (hence the `Diff<T>` type). The expectation is that you'd be able to use the current and previous values to determine how to update the items being rendered.

When you've determined what changes need to be made, you should call the corresponding method on the `ChangeSet` object. These methods correspond to `RecyclerView.Adapter`'s `notifyItem*` methods. You can get a quick idea of how this works in [SingleComponentSectionSpec#onDiff](https://github.com/facebook/litho/blob/d766e3b4965edf84eda0090f58d0020aa302d650/litho-sections-core/src/main/java/com/facebook/litho/sections/common/SingleComponentSectionSpec.java#L25):
  - If we don't have a new `Component` (`component.getNext() == null`) then we want to change the list by removing that row.
  - Else if we have a new `Component` and no previous component, we want to insert a new row.
  - If both an old component and a new component exists, we just want to update that row to the new component.

Note, the indexes used in the `ChangeSet` method calls, *they're relative to the current section*.  Index 0 in `SingleComponentSectionSpec` might actually be index 100 in the final list depending on the section hierarchy. The framework will take care of translating local indexes to global indexes when processing the `ChangeSet`.
