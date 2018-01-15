---
docid: custom-layout
title: Custom Layout
layout: docs
permalink: /docs/custom-layout.html
---
Litho relies on [Yoga](https://facebook.github.io/yoga/), a powerful layout engine that is able to create very complex UIs, for layout calculations.  However, there are few exceptions where Yoga is not enough and you may need to implement your own measuring and layout. 

Litho provides a custom layout API for accessing size information while the [ComponentTree](/javadoc/com/facebook/litho/ComponentTree) is being created and measured, as well as the possibility to measure a component in isolation.

> **IMPORTANT**: This API comes with a **non-negligible** performance overhead. Therefore, it is advisable to only use it when it is absolutely necessary.

## Use cases

* **A component layout tree depends on its own and/or children's size.** For example, perhaps a component layout should use a child only if it fits within its size constraints. If the child doesn't fit, the layout should instead use another child as a fallback.

* **Children of a container have to be absolutely positioned manually based on their/parent size.** Yoga can absolutely position children in a parent. However, the position itself might depend on the child sizes after being measured using the parent size constraints. Margins or paddings need to be manually taken into account if required.

## Size constraints
Before diving into the API, you should be familiar with how the [`onMeasure`](https://developer.android.com/reference/android/view/View.html#onMeasure(int,%20int)) function works in a regular Android `View` and what a [`MeasureSpec`](https://developer.android.com/reference/android/view/View.MeasureSpec.html) is, since Litho uses an equivalent concept called `SizeSpec`. 

Similar to the Android `MeasureSpec` equivalent, Litho's `SizeSpec` is composed of a size and a mode. The possible modes, same as for `MeasureSpec`, are: `UNSPECIFIED`, `EXACTLY` and `AT_MOST`.

## Measuring a component

A component can be measured in isolation for a given `SizeSpec`. A `Size` object, passed as argument, will be populated with the resulting size.

In the following example, a `Text` component is measured with unspecified `SizeSpec` implying a single line of text indefinitely long.

```java
final Component<Text> textComponent = Text.create(c)
    .textSizeSp(16)
    .text(“Some text to measure.”)
    .build();

final Size outputSize = new Size();
textComponent.measure(
    c, 
    SizeSpec.makeSizeSpec(0, UNSPECIFIED),
    SizeSpec.makeSizeSpec(0, UNSPECIFIED),
    outputSize);

final int textComponentWidth = outputSize.width;
final int textComponentHeight = outputSize.height;
```

## SizeSpec information during layout
During layout creation, the API can provide information about the `SizeSpecs` a component is going to be measured with. To access this information, a new `@OnCreateLayoutWithSizeSpec` annotation needs to be used instead of `@OnCreateLayout`. The arguments of the annotated method, besides the standard ComponentContext, are two more integers representing the width spec and the height spec.

In the following example, a `Text` component is measured to check if the given text fits in the available space. An `Image` component is otherwise used.

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayoutWithSizeSpec
  static Component onCreateLayoutWithSizeSpec(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      @Prop SomeObject someProp) {

    final Component<Text> textComponent = Text.create(c)
        .textSizeSp(16)
        .text(“Some text to measure.”)
        .build();

    // UNSPECIFIED sizeSpecs will measure the text as being one line only,
    // having unlimited width.
    final Size textOutputSize = new Size();
    textComponent.measure(
        c, 
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        SizeSpec.makeSizeSpec(0, UNSPECIFIED),
        textOutputSize);

    // Small component to use in case textComponent doesn’t fit within
    // the current layout.
    final Component<Image> imageComponent = Image.create(c)
        .drawableRes(R.drawable.some_icon)
        .build();

    // Assuming SizeSpec.getMode(widthSpec) == EXACTLY or AT_MOST.
    final int layoutWidth = SizeSpec.getSize(widthSpec);
    final boolean textFits = (textOutputSize.width <= layoutWidth);
    
    return Wrapper.create(c)
        .delegate(textFits ? textComponent : imageComponent)
        .build();
  }
}
```
