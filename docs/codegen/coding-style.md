---
id: coding-style
title: Coding Style
---

This page contains Best Practice guidelines for styling code.

## Guidelines

* **Components**:
  * Name specs consistently with *NAMEComponentSpec* to generate a component called *NAMEComponent*.
  * The *ComponentContext* argument should be simply called `c` to make the layout code less verbose and more readable.
* **Resource Types** - where appropriate, use resource types, such as `ResType.STRING`, `ResType.COLOR`, `ResType.DIMEN_SIZE`, to make it easier to set prop values from Android resources.
* **Props**:
  * Declare all required props first then the optional ones (`optional = true`).
  * Declare common props (props defined for all Components on `Component.Builder`) after the component's own props.
* **Enums** - use static imports on all layout Enums (such as `YogaEdge`, `YogaAlign`, `YogaJustify`) to reduce layout code and make it more readable.
* **Conditionals** - use inline conditionals on optional children to keep the layout construction code fluent if possible.
* **Child Container** - if constructing a child container, add the container in the following line. This gives the code a layout like construction.
* **Lifecycle methods** - such as `@OnCreateLayout`, should be static and package-private.

The following snippet is an example of an application of the Litho Best Practice coding style:

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
