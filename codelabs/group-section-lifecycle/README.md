# 1. Overview

In this codelab, you'll learn about lifecycle methods used in GroupSections: we'll show you the execution order visually, and use the doc to introduce the purpose of each methods.

# 2. Setup the environment
Clone the following repo: [https://github.com/facebook/litho/tree/master/codelabs/group-section-lifecycle](https://github.com/facebook/litho/tree/master/codelabs/group-section-lifecycle)

# 3. Build the app and operate with it
Build the app with command `./gradlew app:installDebug` and launch it. From the bottom grid layout view, you can observe lifecycle methods are called.

# 4. Lifecycle method annotations
### @OnCreateInitialState
A method with this annotation is responsible to initialize State values. This method isn't mandatory if you don't have @​State parmaters or don't need to initialize their values. Unlike other lifecycle methods, this method is called only once for the GroupSection.
```kotlin
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnCreateInitialState
  fun onCreateInitialState(
      c: SectionContext,
      startTime: StateValue<Long>,
  ) {
    val timestamp = SystemClock.uptimeMillis()
    startTime.set(timestamp)
  }
}
```

### @OnCreateTreeProp
A method with this annotation is responsible to generate [TreeProp](https://fblitho.com/docs/tree-props#declaring-a-treeprop). TreeProp is stored in the SectionTree and can be accessed by all child sections.
```kotlin
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnCreateTreeProp
  fun onCreateTreeProp(c: SectionContext, ...): YourTreeProp {
    return YourTreeProp(...)
  }
}
```

### @OnCreateChildren
A method with this annotation is responsible to return a [Children](https://fblitho.com/javadoc/com/facebook/litho/sections/Children.html) object, which is a container to contain the child sections. A child section can be a [GroupSectionSpec](https://fblitho.com/docs/group-sections) or a [DiffSectionSpec](https://fblitho.com/docs/diff-sections).
For @DiffSectionSpec, usually we send a `RenderEvent` event for each item, and the handler is responsible for rendering the component with given data. You can check [here](https://fblitho.com/docs/events-overview) for details about how to handle events.
```kotlin
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnCreateChildren
  fun onCreateChildren(
      c: SectionContext,
      @Prop items: List<Item>,
  ): Children {
    return Children.create()
        .child(
            DataDiffSection.create<Item>(c)
                .data(items)
                .renderEventHandler(LifecycleGroupSection.onRender(c))
        )
        .build()
  }

  @OnEvent(RenderEvent::class)
  fun onRender(c: SectionContext, @FromEvent model: Item): RenderInfo {
    return ComponentRenderInfo.create()
        .component(
            Text.create(c)
                .text(model.animal)
                .textSizeSp(20f)
                .paddingDip(YogaEdge.ALL, 8f))
        .build()
  }
}
```

### @OnDataBound
A method with this annotation will be called when the data changes corresponding to this section's hierarchy has been made available to the `SectionTree.Target.` Data changes could occur due to any number of the following:
  - Insert
  - Update
  - Delete
  - Move

```kotlin
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnDataBound
  fun onDataBound(c: SectionContext, ...) {
    ...
  }
}
```

### @OnDataRendered
A method with this annotation will be called when the dataset corresponding to this section is now rendered completely on viewport.

Parameters in the method:
- *isDataChanged* - True if the dataset is changed, false otherwise.
- *isMounted* - True if the section is shown on the viewport, false otherwise.
- *uptimeMillis* - The timestamp when the method is called.
- *firstVisibleIndex* - First visible item index in the section.
- *lastVisibleIndex* - Last visible item index in the section.
- *changesInfo*  - A collection of Changes between previous/current sections, each change represents a operation(insert, update, delete, or move).
```
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnDataRendered
  fun onDataRendered(
      c: SectionContext,
      isDataChanged: Boolean,
      isMounted: Boolean,
      uptimeMillis: Long,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      changesInfo: ChangesInfo
  ) {
    ...
  }
}
```

### @OnViewportChanged
A method annotated with this annotation will be called when first/last visible index of the section in the viewport changes.
```kotlin
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnViewportChanged
  fun onViewportChanged(
      c: SectionContext,
      firstVisibleIndex: Int,
      lastVisibleIndex: Int,
      totalCount: Int,
      firstFullyVisibleIndex: Int,
      lastFullyVisibleIndex: Int
  ) {
    ...
  }
}
```

### @OnRefresh
A method annotated with this annotation will be called when the section requests a refresh of its content.
```
@GroupSectionSpec
object LifecycleGroupSectionSpec {

  @OnRefresh
  fun onDataRendered(c: SectionContext, ...) {
    ...
  }
}
```
