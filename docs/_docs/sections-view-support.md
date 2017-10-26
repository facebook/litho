---
docid: sections-view-support
title: Sections and Views
layout: docs
permalink: /docs/sections-view-support
---

Sections work best when combined with the rendering optimizations that Litho Components offer. However, the API also provides support for rendering with Views instead of Components. This makes the transition to Sections easier and you can still take advantage of the performance benefits regardless of your product's UI using traditional Views, Litho Components or a mix of the two.

View support is only offered by `DataDiffSection` at the moment. Let's have another look at the [DataDiffSection](/docs/sections-building-blocks#datadiffsection) example to recap how you declare what the framework should render for a certain item.

```java
@GroupSectionSpec
class MyGroupSection {

  @OnCreateChildren
  Children onCreateChildren(
      SectionContext c,
      @Prop ImmutableList<MyModel> dataModel) {
      return Children.create()
          .child(DataDiffSection.create(c)
              .data(dataModel)
              .renderEventHandler(MyGroupSection.onRenderEdge(c)))
          .build();
    }

  @OnEvent(RenderEvent.class)
  RenderInfo onRenderEdge(
      SectionContext c,
      @FromEvent MyModel model) {
      return ComponentRenderInfo.create(c)
          .component(MyModelItemComponent.create(c).item(model).build())
          .build();
  }
}
```

When an item needs to be rendered on screen, the framework dispatches a `RenderEvent` and it calls the event handler passed as prop to the `DataDiffSection` to create a `RenderInfo` for that item. `RenderInfo` holds information that allows the framework to understand how a certain item should be rendered.

`RenderInfo` has two implementations: [ComponentRenderInfo](/javadoc/com/facebook/litho/widget/ComponentRenderInfo.html) and [ViewRenderInfo](/javadoc/com/facebook/litho/widget/ViewRenderInfo.html).

We've seen in the previous example how to use `ComponentRenderInfo` to declare how an item should be rendered using Litho Components. If you want to render items with Views instead, all you have to do is return a `ViewRenderInfo` instance from the `RenderEvent` handler.

```java
@GroupSectionSpec
class MyGroupSection {

  @OnCreateChildren
  Children onCreateChildren(
      SectionContext c,
      @Prop ImmutableList<MyModel> dataModel) {
      return Children.create()
          .child(DataDiffSection.create(c)
              .data(dataModel)
              .renderEventHandler(MyGroupSection.onRenderEdge(c)))
          .build();
    }

  @OnEvent(RenderEvent.class)
  RenderInfo onRenderEdge(
      SectionContext c,
      @FromEvent MyModel model,
      @FromEvent int index) {
      return ViewRenderInfo.create(c)
          .viewCreator(
              new ViewCreator<MyView>() {
                  @Override
                  public View createView(Context c) {
                      // this call is equivalent to onCreateViewHolder()
                      return new MyView(c);
                  }
               })
          .viewBinder(
              new SimpleViewBinder<MyView>() {
                  @Override
                  public void bind(MyView view) {
                    // this call is equivalent to onBindViewHolder()
                  }
              })
          .build();
  }
}
```

`ViewRenderInfo` has two mandatory props that need to be passed to it: a [ViewCreator](/javadoc/com/facebook/litho/viewcompat/ViewCreator.html) and a [ViewBinder](/javadoc/com/facebook/litho/viewcompat/ViewBinder.html).

`ViewCreator` and `ViewBinder` are the logical equivalent of `onCreateViewHolder` and `onBindViewHolder` methods of the `RecyclerView.Adapter`.

Views created by the same `ViewCreator` will be recycled. You can use the `model` or the `index` to decide amongst multiple view types and return the appropriate View from `ViewCreator#createView()`.

The framework provides a no-op implementation of `ViewBinder`, called [SimpleViewBinder](/javadoc/com/facebook/litho/viewcompat/SimpleViewBinder.html), that you can use if only need to implement one of the `ViewBinder` methods, typically `bind(View)`.

# Mixing Components and Views

If your Section needs to render items partly with Litho Components, partly with Views, you can do that by returning the appropriate `RenderInfo` implementation from the `RenderEvent` handler.
Here's how you could do that:

```java
@GroupSectionSpec
class MyGroupSection {

  @OnCreateChildren
  Children onCreateChildren(
      SectionContext c,
      @Prop ImmutableList<MyModel> dataModel) {
      return Children.create()
          .child(DataDiffSection.create(c)
              .data(dataModel)
              .renderEventHandler(MyGroupSection.onRenderEdge(c)))
          .build();
    }

  @OnEvent(RenderEvent.class)
  RenderInfo onRenderEdge(
      SectionContext c,
      @FromEvent MyModel model) {
      if (model.canRenderWithComponent()) {
        return ComponentRenderInfo.create(c)
            .component(MyModelItemComponent.create(c).item(model).build())
            .build();
      }

      return ViewRenderInfo.create(c)
               .viewCreator(
                   new ViewCreator<MyView>() {
                       @Override
                       public View createView(Context c) {
                           // this call is equivalent to onCreateViewHolder()
                           return new MyView(c);
                       }
                    })
               .viewBinder(
                   new SimpleViewBinder<MyView>() {
                       @Override
                       public void bind(MyView view) {
                         // this call is equivalent to onBindViewHolder()
                       }
                   })
               .build();
  }
}
```

