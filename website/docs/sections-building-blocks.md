---
id: sections-building-blocks
title: Sections Building Blocks
---

The Sections API provides a few built-in Sections that can be used as building blocks for almost any type of surface.

Most scrolling surfaces can usually be expressed as groups of homogeneous items interleaved with one-off items. As an example, imagine a list of contacts sorted alphabetically and separated by headers indicating the first letter of the contact's name.
Following this assumption, the Sections API packages two `DiffSectionSpec` implementations that can be combined to represent the structure of nearly any surface: `SingleComponentSection` and `DataDiffSection`.

## SingleComponentSection
The `SingleComponentSection` is the simplest Section you can have within a Sections hierarchy and it can be used to represent a one-off row in a complex list. As the name suggests, you can use this Section to render a single Component which is passed to this Section as its only prop.

One of the typical use cases of a `SingleComponentSection` is to add a loading spinner at the end of a list:

```java
final Section loadingSection = SingleComponentSection.create(c)
    .component(Progress.create(c).build())
    .build();
```

## DataDiffSection

If you're new to using `DataDiffSection`, we recommend you watch this Litho Lesson which covers the basics of how to do diffing:

<iframe padding-top="10px" width="560" height="315" src="https://www.youtube-nocookie.com/embed/-Ahskig2Lw0" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>

### How to Use DataDiffSection

**NOTE**: Because Sections diffing can occur on a background thread, mutations to the list or its elements from another thread can cause `DiffUtils` to emit an invalid diff! The reason for this is that `OnCheckIsSameItem` and/or `OnCheckIsSameContent` may return inconsistent results for the same pairs of indices over the course of diffing the lists. **This will show up as an `IndexOutOfBoundsException` thrown from `RecyclerBinder`**.
 
:::tip 
To fix this, you need to prevent concurrent modification of the data with diffing on another thread: the easiest way to ensure this is to use immutable lists and immutable data with `DataDiffSection` and the Sections API.
:::

A `DataDiffSection` is used to represent a homogeneous list of data. The minimal information that you have to pass to a `DataDiffSection` is the list of items that it needs to render and a callback for rendering each item in this list.

```java
@GroupSectionSpec
class MyGroupSection {

  @OnCreateChildren
  static Children onCreateChildren(
      SectionContext c,
      @Prop ImmutableList<MyModel> dataModel) {
      return Children.create()
          .child(DataDiffSection.create(c)
              .data(dataModel)
              .renderEventHandler(MyGroupSection.onRenderEdge(c)))
          .build();
    }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRenderEdge(
      SectionContext c,
      @FromEvent MyModel model) {
      return ComponentRenderInfo.create()
          .component(MyModelItemComponent.create(c).item(model).build())
          .build();
  }
}
```

`DataDiffSection` is designed to efficiently render the parts of a surface that handle large flows of data. When an item at a certain position needs to be displayed on screen, the framework will check whether the model we have received in the new list of data changed since the last time we rendered it. If the data changed for the item in that position, the framework will dispatch a `RenderEvent` for that item and the `DataDiffSection` will use the `RenderEvent` handler we passed as prop to create a Component for that item and display it.

The framework implementation of the DataDiffSection uses the Android [DiffUtil](https://developer.android.com/reference/android/support/v7/util/DiffUtil.html) class for determining the minimal set of operations that is needed to update the UI given the current and new lists of data.

By default, DataDiffSection will detect data changes by checking instance equality and subsequently calling `equals()` on the objects in the data list. Most of the times however, your data model is more complex and you need a custom way of declaring how two items should be compared and based on that, deciding when to update them.
This can easily be done by implementing and passing to the `DataDiffSection` handlers for two events: `OnCheckIsSameItemEvent` and `OnCheckIsSameContentEvent`.

When the `DataDiffSection` calculates the delta between its current and new list of data, it will check whether two items represent the same piece of data, and only if that's true it will check whether the content of that item is unchanged.

This is how the example above would change if you provided your own item comparison methods to the DataDiffSection:

```java
@OnCreateChildren
Children onCreateChildren(
    SectionContext c,
    @Prop ImmutableList<MyModel> dataModel) {
  return Children.create()
    .child(DataDiffSection.create(c)
        .data(dataModel)
        .onCheckIsSameItemEventHandler(MyGroupSection.onCheckIsSameItem(c))
        .onCheckIsSameContentEventHandler(MyGroupSection.onCheckIsSameContent(c))
        .renderEventHandler(MyGroupSection.onRenderEdge(c)))
    .build();
}


@OnEvent(OnCheckIsSameItemEvent.class)
boolean onCheckIsSameItem(
    SectionContext c,
    @FromEvent MyModel previousItem,
    @FromEvent MyModel nextItem) {
  return previousItem.getId() == nextItem.getId();
}

@OnEvent(OnCheckIsSameContentEvent.class)
boolean onCheckIsSameContent(
    SectionContext c,
    @FromEvent MyModel previousItem,
    @FromEvent MyModel nextItem) {
  return MyModel.compareContent(previousItem, nextItem);
}
```

## Litho integration: RecyclerCollectionComponent

For easy integration with Litho, the framework provides a built-in Component that can render a hierarchy of Sections, called `RecyclerCollectionComponent`.

This is a regular Litho Component that takes a Section prop and has logic for creating the infrastructure for rendering the Components encapsulated in the Section hierarchy in a [Recycler](recycler-component) managed by a [RecyclerBinder](javadoc/com/facebook/litho/widget/RecyclerBinder.html).

The Sections hierarchy becomes a “data source” for the `RecyclerCollectionComponent`, and the complexity of handling operations on your list, such as inserts or removes, is hidden away and handled by the infrastructure.

```java
final Component listComponent = RecyclerCollectionComponent.create(c)
    .section(MyGroupSection.create(new SectionContext(c))
        .dataModel(...)
        .build())
    .build();
```
