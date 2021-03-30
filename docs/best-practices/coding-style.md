---
id: coding-style
title: Coding Style
---

:::caution Content will be updated
This page was moved from the old website without any change and might be updated
:::

#### Guidelines:

 * Name your specs consistently with *NAMEComponentSpec* to generate a component called *NAMEComponent*.
 * The *ComponentContext* argument should be simply called `c` to make your layout code less verbose and more readable.
 * Use resource types (`ResType.STRING`, `ResType.COLOR`, `ResType.DIMEN_SIZE`, etc) where appropriate to make it easier to set prop values from Android resources.
 * Declare all required props first then optional ones (`optional = true`).
 * Declare common props (props defined for all Components on `Component.Builder`) after the component's own props.
 * Use static imports on all layout enums (`YogaEdge`, `YogaAlign`, `YogaJustify`, etc) to reduce your layout code and make it more readable.
 * Lifecycle methods, such as `@OnCreateLayout`, are static and package-private.
 * Use inline conditionals on optional children to keep the layout construction code fluent if possible.
 * If you are constructing a child container, add the container in the following line. This gives the code a layout like construction.

Here is some sample code with our styling guidelines:

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String title,
      @Prop(optional = true) Drawable image) {

  return Row.create(c)
      .alignItems(CENTER)
      .paddingRes(R.dimen.some_dimen)
      .child(
          Image.create(c)
              .drawable(image)
              .width(40)
              .height(40)
              .marginRes(RIGHT, R.dimen.my_margin))
      .child(TextUtils.isEmpty(title) ? null :
          Text.create(c)
              .text(title)
              .textColorAttr(R.attr.textColorTertiary)
              .marginDip(5)
              .flexGrow(1f))
      .build();
  }
}
```
