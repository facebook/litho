---
id: debugging-sections
title: Debugging Sections
---
import useBaseUrl from '@docusaurus/useBaseUrl';

This page acts as a debugging guide that explains how to read the operations performed when an event occurs in a Litho Sections surface with the Flipper Sections plugin. This helps to debug common [issues](#issues) such as unwanted scrolling and items getting re-rendered incorrectly.

## Sections hierarchies

In a Litho Sections hierarchy, the common setup is to have a `RecyclerCollectionComponent` as the root of your `LithoView`. This component is the bridge with the Sections hierarchy. You'll use this setup indirectly if you're using `SectionsHelper`.

The `RecyclerCollectionComponent` wraps a `Recycler` component, which mounts a `RecyclerView`. All sections surfaces are just APIs to make it easier to work with `RecyclerView` and `RecyclerView.Adapter`.

The adapter abstraction used in Sections is called a `RecyclerBinder`.
All adapter operations are dispatched to the `RecyclerBinder`, which use the render information implemented by the user in the GroupSectionSpec to transform them into `LithoViews` hosting `ComponentTrees`.  The `LithoViews` represent the items inserted into the `RecyclerView.Adapter`.

Simply put, under the hood, a Sections hierarchy is represented by a `RecyclerView` with a single view type which is the `LithoView`. Each item in the list is its own `ComponentTree`.

![](/images/debugging-sections-hierarchies.png)

## Sections terminology

### Diffing

Diffing means comparing the existing data on a Section with an updated list passed through props or state. You can accomplish this through special Section Spec types called [DiffSectionSpecs](../sections/start.mdx).

### Changesets

A `Changeset` is a list of operations that are dispatched to the `RecyclerView Adapter` to update the items in a list. A Changeset consists of the same type of operations supported by a `RecyclerView Adapter`, such as insert, remove and update.

Every time you recreate the Sections hierarchy by setting a new root or updating state, the framework calculates a new `Changeset`, which have the minimal list of operations that should be performed by the `RecyclerView Adapter` to reflect the data changes in the UI.

Without using Sections, it’s the Developer’s responsibility to calculate granular operations for efficient updates to send to the Adapter, but the Sections API handles all of that.

## Issues

The following common issues can occur when using Sections, each issue has an accompanying description and walks you through how to debug them.  The code for the issues is located in the [changesetdebug](https://github.com/facebook/litho/tree/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug) file.

To test yourself: `./gradlew installDebug` and navigate to the `Changeset debug` section.

### Issue 1: the state of an entire Section surface is getting reset

**Code**: [StateResettingActivity](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/StateResettingActivity.java)

**Scenario**: the surface displays a `StateResettingRootComponent` which looks like this:

```java
@OnCreateLayout
static Component onCreateLayout(
    ComponentContext c, @Prop List<DataModel> dataModels, @Prop boolean showHeader) {

  final Component listComponent =
      RecyclerCollectionComponent.create(c)
          .disablePTR(true)
          .section(FavouriteGroupSection.create(new SectionContext(c)).dataModels(dataModels))
          .flexGrow(1)
          .build();

  return showHeader
      ? Column.create(c)
          .child(Text.create(c).text("Header").build())
          .child(listComponent)
          .build()
      : listComponent;
}
```

If `StateResettingRootComponent` can show a header, it creates a Column with a header component and RecyclerCollectionComponent. If the header is not ready yet, it immediately delegates to the RecyclerCollectionComponent. Initially, the header is hidden so `StateResettingRootComponent` only displays the RecyclerCollectionComponent, but after some time, `setRoot` is called to indicate to `StateResettingRootComponent` that the header is ready to display. This will make the header component appear but will cause all items in the `RecyclerCollectionComponent` to lose any updated state and the entire list will lose its state, such as the current scroll position.

In a Sections list, when an item in the list loses its state that usually indicates it was treated by the framework as a new item after setting new data. To investigate, refer to the Changeset operations in the Sections Flipper plugin.

The following video shows that when the second `setRoot` is triggered, the resulting Changeset for the section contains an `INSERT_RANGE` operation. Therefore, all items were re-rendered as new items which have just been inserted into the adapter.

<video width="100%" controls="controls">
  <source src={useBaseUrl('videos/debugging-sections-issue1.mov')} />
</video>

The resulting updated list has 20 items, but the Changeset doesn’t have any DELETE operations. This raises the question of what happened to the items that were initially inserted, which indicates that something out of the ordinary is going on with the items the RecyclerBinder knows about. The RecyclerBinder has no knowledge about the items which were inserted on the first render, and that can only mean one thing: a new RecyclerBinder instance was created after the second `setRoot`. This can happen if the RecyclerCollectionComponent’s state is recreated. You can confirm by adding logging in its [`@OnCreateInitialState`](https://github.com/facebook/litho/blob/d23a9406659f27fa9df25d02ae1097a975973bb8/litho-sections-widget/src/main/java/com/facebook/litho/sections/widget/RecyclerCollectionComponentSpec.java#L272) implementation. This means its key is changing after the `setRoot` update.

Looking at the implementation of `StateResettingRootComponent` again, you can see that the `RecyclerCollectionComponent` can get re-parented depending on the presence of the header, which makes the framework treat it as a different component and reset its key.

The fix for this issue is to maintain the state after the `setRoot` update, you need to make the hierarchy of components stable.  This involves making sure that the path from the root to any stateful component is preserved after an update. In this case, always wrapping the children in a column and conditionally adding the header as a child would solve the issue.

### Issue 2: the Section content scrolls away from top after loading

**Code**: [ScrollingToBottomActivity](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/ScrollingToBottomActivity.java)

**Scenario**: when navigating to this Sections surface, the list is not scrolled to top.  Instead, it scrolls to another item automatically.

Check the Flipper Sections Plugin to see what happens when navigating to this surface.

<video width="100%" controls="controls">
  <source src={useBaseUrl('videos/debugging-sections-issue2.mov')} />
</video>

In the video above, the initial `setRoot` call passes to the Section a list of items, starting with item 15. This matches what you see on screen. Later is a `setRoot` call, which inserts at the top of the Section a list of items from 0 to 14. The order in which this data is inserted explains why the list scrolled to item 15.

RecyclerView will try to maintain the current scroll position whenever the adapter is notified of changes. In this scenario, the initial scroll position is at item '15', so when the second insert batch is applied, the RecyclerView will keep this as the first visible item, even if items are inserted above.

This is a commonly occurring scenario that can occur in surfaces that consist of Sections that use different data sources. Imagine a typical feed-like surface that has a header Section followed by the feed stories Section. The two Sections will receive data from different sources, each source being queried independently. This means that the response can return at different times. If the feed stories data is fetched first, these items will be inserted immediately, and the header data will be inserted at the top of the feed items later when the request completes.

If your Section surface is using data queried from multiple sources, and it can be inserted out of order, you can maintain scroll position at the first item in the list by manually scrolling to the top, after the data reaches the adapter, using the [requestFocus APIs](../sections/communicating-with-the-ui.md#scrolling-requestfocus):

```java
@OnDataBound
static void onDataBound(SectionContext c) {
    DelayedLoadingSection.requestFocus(c, 0);
}
```

### Issue 3: all items are being re-rendered after a data update

**Code**: [ItemsRerenderingActivity](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/ItemsRerenderingActivity.java)

**Scenario**: some items in the Sections list were updated after interacting with them. After some time, the surface appears to blink, and the scroll position is reset. All items are reset, losing any updated state.

This issue has similar symptoms to [issue #1](#issue-1-the-state-of-an-entire-section-surface-is-getting-reset), but you can use the Flipper Sections Plugin this time to see what’s different and how to find the cause.

<video width="100%" controls="controls">
  <source src={useBaseUrl('videos/debugging-sections-issue3.mov')} />
</video>

The list is reset after the second setRoot call is triggered, as can be seen from its generated changesets.

All the items that were previously in the list were deleted and inserted again, along with the items that are new. This explains the blinking and the state being reset, since all existing items are removed and treated as completely new items.

The difference between this issue and [issue #1](#issue-1-the-state-of-an-entire-section-surface-is-getting-reset) is that the changeset contains a list of delete operations, which means that this is not a case of the RecyclerBinder being reset, but rather something is not working right when comparing the items in the current and new list and deciding if they are the same or not.

Look at the code for the Section rendering this surface, which you can find in the [InefficientFavouriteGroupSectionSpec](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/InefficientFavouriteGroupSectionSpec.java).

```java
@OnCreateChildren
static Children onCreateChildren(SectionContext c, @Prop List<DataModel> dataModels) {
  return Children.create()
      .child(
          DataDiffSection.<DataModel>create(new SectionContext(c))
              .data(dataModels)
              .renderEventHandler(InefficientFavouriteGroupSection.onRender(c))
              .build())
      .build();
}
```

The Section has a DataDiffSection child which is passed a `renderEventHandler` prop, so it knows how to render the `DataModel` items but not how to compare them for efficient updates. By default, the Sections framework will compare items first by pointer equality and then by calling `equals` if no comparison methods are passed to the `DataDiffSection`. For complex data types, you will always need implement comparison methods to decide when an item should be re-rendered.

```java
@OnCreateChildren
static Children onCreateChildren(SectionContext c, @Prop List<DataModel> dataModels) {
  return Children.create()
      .child(
          DataDiffSection.<DataModel>create(new SectionContext(c))
              .data(dataModels)
              .renderEventHandler(InefficientFavouriteGroupSection.onRender(c))
              .onCheckIsSameItemEventHandler(InefficientFavouriteGroupSection.onCheckIsSameItem(c))
              .onCheckIsSameContentEventHandler(
                  InefficientFavouriteGroupSection.onCheckIsSameContent(c))
              .build())
      .build();
}

@OnEvent(RenderEvent.class)
static RenderInfo onRender(SectionContext c, @FromEvent DataModel model) {
  return ComponentRenderInfo.create()
      .component(
          Row.create(c)
              .child(Text.create(c).text(model.getData()).textSizeDip(30))
              .child(RowItem.create(c))
              .build())
      .build();
}

@OnEvent(OnCheckIsSameItemEvent.class)
static boolean onCheckIsSameItem(
    SectionContext c,
    @FromEvent DataModel previousItem,
    @FromEvent DataModel nextItem) {
  return previousItem.getId() == nextItem.getId();
}
```

After making the above change to [InefficientFavouriteGroupSectionSpec](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/InefficientFavouriteGroupSectionSpec.java), only the new item is inserted and all the existing items are reused and their state is persisted, as shown in the following diagram.

![](/images/debugging-sections-issue3.png)

### Issue 4: the Section is not updating items after a prop update

**Code**: [PropUpdatingActivity](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/PropUpdatingActivity.java)

**Scenario**: you have a list of items which can be in a selected or unselected state. The Section has a selectedItem prop which is the index of the item which is selected - this value can change based on data coming from an external source. When the value changes, a new prop value is passed to the Section.

Initially, item at position 0 is selected:

```java
SelectedItemRootComponent.create(mComponentContext)
    .dataModels(mDataModels)
    .selectedItem(0)
    .build();
```

Then after a while, new data is available and item at position 1 needs to be selected:

```java
SelectedItemRootComponent.create(mComponentContext)
    .dataModels(mDataModels)
    .selectedItem(1)
    .build();
```

However, it looks like nothing is changing: the item at position 0 is still selected and item at position 1 is still unselected.

Again, you go to the Flipper Sections Plugin to understand what’s happening:

<video width="100%" controls="controls">
  <source src={useBaseUrl('videos/debugging-sections-issue4.mov')} />
</video>

Here you see that when you pass a new value for `selectedItem` and `setRoot` is called, the changeset generated for this shows us that all items in the list have been reused and nothing got updated. This is not what is expected: items 0 and 1 should be updated.

As with [issue #3](#issue-3-all-items-are-being-re-rendered-after-a-data-update), this indicates that something is not working right when comparing the items in the current and new list and deciding if they are the same or not. However, looking at [`SelectedItemRootComponent`](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/java/changesetdebug/SelectedItemRootComponentSpec.java), you see that in this case you are passing comparison methods to the DataDiffSection:

```java
@OnCreateLayout
static Component onCreateLayout(ComponentContext c, @Prop List<DataModel> dataModels) {

  return RecyclerCollectionComponent.create(c)
      .disablePTR(true)
      .section(
          DataDiffSection.<DataModel>create(new SectionContext(c))
              .data(dataModels)
              .renderEventHandler(SelectedItemRootComponent.onRender(c))
              .onCheckIsSameContentEventHandler(SelectedItemRootComponent.isSameContent(c))
              .onCheckIsSameItemEventHandler(SelectedItemRootComponent.isSameItem(c))
              .build())
      .flexGrow(1)
      .build();
}

@OnEvent(RenderEvent.class)
static RenderInfo onRender(
    ComponentContext c,
    @Prop int selectedItem,
    @FromEvent DataModel model,
    @FromEvent int index) {
  return ComponentRenderInfo.create()
      .component(
          Row.create(c)
              .child(Text.create(c).text(model.getData()).textSizeDip(30))
              .child(FixedRowItem.create(c).favourited(selectedItem == index))
              .build())
      .build();
}

@OnEvent(OnCheckIsSameItemEvent.class)
static boolean isSameItem(
    ComponentContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
  return previousItem.getId() == nextItem.getId();
}

@OnEvent(OnCheckIsSameContentEvent.class)
static boolean isSameContent(
    ComponentContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
  return previousItem.getData().equals(nextItem.getData());
}
```

The items in the list would only be changed based on the comparison result. However, when a new value is passed for `selectedItem`, that prop is only used in the render function and not in the comparison methods.  This means the items being compared will be considered the same even if the selection status changes.

The fix in this case is to take the `selectedItem` value into account when doing the comparison. Since you cannot compare previous and current props, change this to make the selection state part of the DataModel class then use that field for comparison instead. After making that change, the new comparison method would look like this:

```java
@OnEvent(OnCheckIsSameContentEvent.class)
static boolean isSameContent(
    ComponentContext context, @FromEvent DataModel previousItem, @FromEvent DataModel nextItem) {
  return previousItem.getData().equals(nextItem.getData())
      && previousItem.isSelected() == nextItem.isSelected();
}
```

This will correctly take the selection status into account when deciding whether to re-render items or not, and the new changeset will appear as shown in the following diagram.

![](/images/debugging-sections-issue4.png)
