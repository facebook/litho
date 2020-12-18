---
id: view-support
title: 'Advanced: Mixing with Android Views'
---

Sections work best when combined with the rendering optimizations that Litho Components offer. However, the API also provides **support for rendering Android `View`s** instead of (or along with) Components. This makes the transition to Sections easier and you can still take advantage of the performance benefits regardless of your product's UI using traditional Android Views, Litho Components or a mix of the two.

View support is available only through `DataDiffSection`. Let's recap how you declare what the framework should render for a certain item:

```java
@GroupSectionSpec
class MyGroupSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(
      SectionContext c,
      @Prop ImmutableList<MyModel> dataModel) {
      return Children.create()
          .child(
              DataDiffSection.create(c)
                .data(dataModel)
                .renderEventHandler(MyGroupSection.onRenderEvent(c)))
          .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRenderEvent(
      SectionContext c,
      @FromEvent MyModel model) {
      return ComponentRenderInfo.create()
          .component(MyModelItemComponent.create(c).item(model).build())
          .build();
  }
}
```

When an item needs to be rendered on the screen, the framework dispatches a `RenderEvent` and it calls the event handler passed as prop to the `DataDiffSection` to create a `RenderInfo` for that item. `RenderInfo` holds information that allows the framework to understand how a certain item should be rendered.

### ViewRenderInfo

Most commonly used implementation of `RenderInfo` is [`ComponentRenderInfo`](pathname:///javadoc/com/facebook/litho/widget/ComponentRenderInfo.html) and we have seen in the previous example how it can be used to declare an item to be rendered using Litho Components. If you want to render items with Views instead, all you have to do is to return a different `RenderInfo` implementation - [`ViewRenderInfo`](pathname:///javadoc/com/facebook/litho/widget/ViewRenderInfo.html) instance from the `RenderEvent` handler.

```java
@OnEvent(RenderEvent.class)
static RenderInfo onRenderEvent(
    SectionContext c,
    @FromEvent MyModel model,
    @FromEvent int index) {
    return ViewRenderInfo.create(c)
        .viewCreator(VIEW_CREATOR)
        .viewBinder(VIEW_BINDER)
        .build();
}
```

`ViewRenderInfo` has two mandatory props that need to be passed to it: a [ViewCreator](pathname:///javadoc/com/facebook/litho/viewcompat/ViewCreator.html) and a [ViewBinder](pathname:///javadoc/com/facebook/litho/viewcompat/ViewBinder.html). `ViewCreator` and `ViewBinder` are the logical equivalent of `onCreateViewHolder()` and `onBindViewHolder()` methods of the `RecyclerView.Adapter`.

The framework provides a no-op implementation of `ViewBinder`, called [SimpleViewBinder](pathname:///javadoc/com/facebook/litho/viewcompat/SimpleViewBinder.html), that you can use if you only need to implement one of the `ViewBinder` methods, typically `bind(View)`.

```java
private static SimpleViewBinder VIEW_BINDER =
    new SimpleViewBinder<MyView>() {
        @Override
        public void bind(MyView view) {
        // this call is equivalent to onBindViewHolder()
        }
    }
```

Views created by the same `ViewCreator` instance will be recycled in the same pool in `RecyclerView`. You can create a static instance of `ViewCreator` for different view types which you will use in the sections and pass static instance to `ViewRenderInfo.Builder#viewCreator()` method to ensure efficient recycling. You can use the `model` or the `index` params in `RenderEvent` handler to decide amongst multiple view types and return the appropriate `ViewCreator` instance.

```java
private static ViewCreator VIEW_CREATOR =
    new ViewCreator<MyView>() {
        @Override
        public MyView createView(Context c, ViewGroup parent) {
            // this call is equivalent to onCreateViewHolder()
            return new MyView(c);
        }
    };
```

### Mixing Components and Views

If your Section needs to render items partly with Litho Components, partly with Views, you can do that by returning the appropriate `RenderInfo` implementation from the `RenderEvent` handler.
Here's how you could do that:

```java
@OnEvent(RenderEvent.class)
static RenderInfo onRenderEvent(
    SectionContext c,
    @FromEvent MyModel model) {
    if (model.canRenderWithComponent()) {
        return ComponentRenderInfo.create()
            .component(MyModelItemComponent.create(c).item(model).build())
            .build();
    }

    return ViewRenderInfo.create(c)
            .viewCreator(VIEW_CREATOR)
            .viewBinder(
                new SimpleViewBinder<MyView>() {
                    @Override
                    public void bind(MyView view) {
                        // this call is equivalent to onBindViewHolder()
                    }
                })
            .build();
  }
```
