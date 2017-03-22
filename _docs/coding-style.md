---
docid: code-style
title: Code Style 
layout: docs
permalink: /docs/code-style
---

#### Guidelines: ####

 * Name your specs consistently with *NAMEComponentSpec* to generate a component called *NAMEComponent*.
 * The *ComponentContext* argument should be simply called `c` to make your layout code less verbose and move readable.
 * Use resource types (`ResType.STRING`, `ResType.COLOR`, `ResType.DIMEN_SIZE`, etc) where appropriate to make it easier to set prop values from Android resources.  
 * Declare all required props first then optional ones (`optional = true`).
 * Use static imports on all layout enums (`YogaFlexDirection`, `YogaAlign`, `YogaJustify`, etc) to reduce your layout code and make it more readable.
 * No extra indentation level for props under `withLayout()`.
 * Lifecycle methods, such as `@OnCreateLayout`, are static and package-private.
 * Use inline conditionals on optional children to keep the layout construction code fluent if possible.
 * If you are constructing a child container, add the container in the following line. This gives the code a layout like construction.

Here is some sample code with our styling guidelines:

``` java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop(resType = STRING) String title,
      @Prop(optional = true) Drawable image) {

  return Container.create(c)
      .flexDirection(ROW)
      .alignItems(CENTER)
      .paddingRes(R.dimen.some_dimen)
      .child(
          Image.create(c)
              .src(image)
              .withLayout()
              .width(40)
              .height(40)
              .marginRes(RIGHT, R.dimen.my_margin))
      .child(TextUtils.isEmpty(title) ? null :
          Text.create(c)
              .text(title)
              .textColorAttr(R.attr.textColorTertiary)
              .withLayout()
              .marginDip(5)
              .flexGrow(1f))
      .build();
  }
}
```
