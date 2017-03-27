---
docid: layout-specs
title: Layout Specs
layout: docs
permalink: /docs/layout-specs
---

A *layout spec* is the logical equivalent of a composite view on Android. It simply groups existing components together in an immutable layout tree.

Implementing a layout spec is very simple: you only need to write one method annotated with `@OnCreateLayout` which returns an immutable tree of `ComponentLayout` objects. 

Let's start with a simple example:

```java
@LayoutSpec
public class MyComponentSpec {
  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop Uri imageUri,
      @Prop String title) {

      return Container.create(c)
          .direction(ROW)
          .alignItems(CENTER)
          .child(
              FrescoComponent.create(c)
                  .uri(imageUri)
                  .withLayout()
                  .widthDip(40)
                  .heightDip(40))
          .child(
              Text.create(c)
                  .text(title)
                  .textSizeRes(R.dimen.my_text_size)
                  .withLayout()
                  .flexGrow(1f))
          .build();
  }
}
```

As you can see, layout spec classes use the `@LayoutSpec` annotation. 

The method annotated with `@OnCreateLayout` must have `ComponentContext` as its first argument followed by a list of arguments annotated with `@Prop`. The annotation processor will validate this and other invariants in the API at build time.

In the example above, the layout tree has a root *Container* with two children stacked horizontally (`FlexDirection.ROW`) and vertically centered (`Align.CENTER`).

The first child is a `FrescoImage` component that takes an `uri` prop and has a 40dp width and height.

```java
DraweeComponent.create(c)
    .uri(imageUri)
    .withLayout()
    .width(40)
    .height(40)
```

The second child is a `Text` component that takes a prop named `text` and fills the remaining horizontal space available in `MyComponent` by using `grow(1f)` (equivalent to Android's `layoutWeight` from `LinearLayout`). The text size is defined in `my_text_size` dimension resource.

```java
Text.create(c)
    .text(title)
    .textSizeRes(R.dimen.my_text_size)
    .withLayout()
    .grow(1f)
```

The framework exposes many layout features. See <https://facebook.github.io/yoga/docs/learn-more/> for full documentation. 
 
