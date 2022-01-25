---
id: diff-sections
title: 'Advanced: Writing your own DiffSection'
---
In this page, you will learn how to build your own `DiffSection`. The Sections API already provides two implementations that cover the most frequent use cases of `SingleComponentSection` and `DataDiffSection`. If the given implementation is not sufficient for your use case, then you should **write your own `DiffSection`**, as the complexity and chances of introducing subtle errors are both high.

## DiffSectionSpec

A *diff section spec* defines a section that explicitly outputs 'insert', 'update', and 'remove' changes on the section hierarchy.

Diff section specs explicitly manage insertions, removals, and updates that a section performs whenever its states and props change.  You will find Diff Sections at the leaves of every section tree as they are the sections that actually specify the changes to be made to a list.

One example of where you might want a custom diff section is if you receive the data you want to display in the form of incremental updates or diffs, which may occur if you're using a specialised diffing algorithm to process your data.

:::note
`DataDiffSection` utilises a familiar Android's [DiffUtil](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html).
:::

The following code provides an example of using [SingleComponentSection](pathname:///javadoc/com/facebook/litho/sections/common/SingleComponentSection.html) to describe how to write diff section specs:

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

As shown at the top of the above code, diff section specs use the `@DiffSectionSpec` annotation. Implementing a diff section spec requires little boilerplate, you only have to write one method annotated with `@OnDiff`.

The method annotated with `@OnDiff` must have, as its first and second argument, a [SectionContext](pathname:///javadoc/com/facebook/litho/sections/SectionContext.html) and a [ChangeSet](pathname:///javadoc/com/facebook/litho/sections/ChangeSet.html) respectively. Following these two arguments, your `@OnDiff` method can also accept any number of arguments annotated with `@Prop` or `@State`.

These props and state have a special type: `Diff<T>`.  If your prop is defined in another annotated method like `@Prop String prop1`, it must be defined as `@Prop Diff<String> prop1` when being used in the `@OnDiff` method. The reason for this `Diff<T>` type wrapper is so we can compare previous prop values with new prop values when computing changes.

## Making Changes to the List with ChangeSet

The `ChangeSet` argument of the `@OnDiff` method is used by the Diff section spec to specify how the section changes in response to new data.  The `@OnDiff` method will always be called with both the current *and previous* props and the state values (hence the `Diff<T>` type). The expectation is that you'd be able to use the current and previous values to determine how to update the items being rendered.

When you've determined the changes to be made, you should call the corresponding method on the `ChangeSet` object. These methods correspond to `RecyclerView.Adapter`'s `notifyItem*` methods. You can get an  idea of how this works in [SingleComponentSectionSpec#onDiff](https://github.com/facebook/litho/blob/d766e3b4965edf84eda0090f58d0020aa302d650/litho-sections-core/src/main/java/com/facebook/litho/sections/common/SingleComponentSectionSpec.java#L25):

  * If you don't have a new `Component` (`component.getNext() == null`) then change the list by removing that row.
  * Else if you have a new `Component` and no previous component, insert a new row.
  * If both an old component and a new component exist, update that row to the new component.

:::note
The indexes used in the `ChangeSet` method calls are *relative to the current section*.  For example, index 0 in `SingleComponentSectionSpec` may be index 100 in the final list, depending on the section hierarchy. The framework will take care of translating local indexes to global indexes when processing the `ChangeSet`.
:::
