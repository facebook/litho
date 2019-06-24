# 1. Overview

In this codelab, you'll learn about lifecycle methods used in GroupSections: we'll show you the execution order visually, and use the doc to introduce the purpose of each methods.

# 2. Setup the environment
Clone the following repo: [https://github.com/facebook/litho/tree/master/codelabs/group-section-lifecycle](https://github.com/facebook/litho/tree/master/codelabs/group-section-lifecycle)

# 3. Build the app and operate with it
Build the app with command `./gradlew app:installDebug` and launch it. From the bottom grid layout view, you can observe lifecycle methods are called.

# 4. Lifecycle method annotations
###@​OnCreateInitialState
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

###@​OnCreateChildren
A method with this annotation is responsible to return a [Children](https://fblitho.com/javadoc/com/facebook/litho/sections/Children.html) object, which is a container to contain the child sections. A child section can be a [GroupSectionSpec](/docs/group-sections) or a [DiffSectionSpec](/docs/diff-sections).
For @​DiffSectionSpec, usually we send a `RenderEvent` event for each item, and the handler is responsible for rendering the component with given data. You can check [here](/docs/events-overview) for details about how to handle events.
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
