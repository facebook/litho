---
docid: custom-layout
title: Custom Layout
layout: docs
permalink: /docs/custom-layout.html
---
[Yoga](https://facebook.github.io/yoga/) is Litho's powerful layout engine, able to create very complex UIs. However, there are few exceptions where Yoga is not enough. Litho provides a custom layout API to access size information while the tree is being created and measured, as well as the possibility to measure a component in isolation.

**IMPORTANT**: This API comes with a **not negligible** performance overhead. Therefore, is advisable to use it only when absolutely necessary.

## Use cases

* **A component layout tree depends on its own and/or children's size.** For example, perhaps a component layout should use a child only if it fits within its size constraints. If the child doesn't fit, the layout should instead use another child as a fallback.

* **Children of a container have to be absolutely positioned manually based on their/parent size.** Yoga can absolutely position children in a parent. However, the position itself might depend on the child sizes after being measured using the parent size constraints. Margins/paddings need to be manually taken into account if required.

## Size constraints
Before diving into the API, the reader should be familiar with how the [`onMeasure`](https://developer.android.com/reference/android/view/View.html#onMeasure(int,%20int)) function works in a normal Android `View` and what a [`MeasureSpec`](https://developer.android.com/reference/android/view/View.MeasureSpec.html) is, since Litho uses an equivalent concept called `SizeSpec`. Same as the Android `MeasureSpec` equivalent, Litho's `SizeSpec` is composed of a size and a mode. The possible modes, as in `MeasureSpec`, are: `UNSPECIFIED`, `EXACTLY` and `AT_MOST`.

## Measuring a component

A component can be measured in isolation for some given `SizeSpec`. A `Size` object, passed as argument, will be populated with the resulting size.

In the following example, a `Text` component is measured with unspecified `SizeSpec` implying a single line of text indefinitely long.

``` java
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

## `SizeSpec` information during layout
During layout creation, the API can provide information about the `SizeSpecs` a component is going to be measured with. To access this information, a new `@OnCreateLayoutWithSizeSpec` annotation needs to be used instead of `@OnCreateLayout`. The arguments of the annotated method, besides the standard ComponentContext, are two more integers representing the width spec and the height spec.

In the following example, a `Text` component is measured to check if the given text fits in the available space. An `Image` component is otherwise used.

``` java
@LayoutSpec
public class MyComponentSpec {

  @OnCreateLayoutWithSizeSpec
  static ComponentLayout onCreateLayoutWithSizeSpec(
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
    Component<Image> imageComponent = Image.create(c)
        .srcRes(R.drawable.some_icon)
        .build();

    // Assuming SizeSpec.getMode(widthSpec) == EXACTLY or AT_MOST.
    final int layoutWidth = SizeSpec.getSize(widthSpec);
    final boolean textFits = (textOutputSize.width <= layoutWidth);
    
    return Layout.create(c, textFits ? textComponent : imageComponent)
        .build();
  }
}
```
