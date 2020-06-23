---
id: custom-layout
title: Custom Layout
---
Litho relies on [Yoga](https://yogalayout.com/docs/), a powerful layout engine that is able to create very complex UIs, for layout calculations.  However, there are few exceptions where Yoga is not enough and you may need to implement your own measuring and layout. 

Litho provides a custom layout API for accessing size information while the [`ComponentTree`](/javadoc/com/facebook/litho/ComponentTree.html) is being created and measured, as well as the possibility to measure a component in isolation.

:::caution IMPORTANT
 This API comes with a **non-negligible** performance overhead. Therefore, it is advisable to only use it when it is absolutely necessary.
:::

## Use cases

* **A component layout tree depends on its own and/or children's size.** For example, perhaps a component layout should use a child only if it fits within its size constraints. If the child doesn't fit, the layout should instead use another child as a fallback.

* **Children of a container have to be absolutely positioned manually based on their/parent size.** Yoga can absolutely position children in a parent. However, the position itself might depend on the child sizes after being measured using the parent size constraints. Margins or paddings need to be manually taken into account if required.

## Size constraints
Before diving into the API, you should be familiar with how the [`onMeasure`](https://developer.android.com/reference/android/view/View.html#onMeasure(int,%20int)) function works in a regular Android `View` and what a [`MeasureSpec`](https://developer.android.com/reference/android/view/View.MeasureSpec.html) is, since Litho uses an equivalent concept called [`SizeSpec`](/javadoc/com/facebook/litho/SizeSpec.html). 

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
During layout creation, the API can provide information about the `SizeSpecs` a component is going to be measured with. To access this information, the [`@OnCreateLayoutWithSizeSpec`](/javadoc/com/facebook/litho/annotations/OnCreateLayoutWithSizeSpec.html) annotation needs to be used instead of `@OnCreateLayout`. The arguments of the annotated method, besides the standard ComponentContext, are two more integers representing the width spec and the height spec.

In the following example, a `Text` component is measured to check if the given text fits in the available space. An `Image` component is otherwise used.

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayoutWithSizeSpec
  static Component onCreateLayoutWithSizeSpec(ComponentContext c, int widthSpec, int heightSpec) {

    final Component textComponent =
        Text.create(c).textSizeSp(16).text("Some text to measure.").build();

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
    final Component imageComponent = Image.create(c).drawableRes(R.drawable.ic_launcher).build();

    // Assuming SizeSpec.getMode(widthSpec) == EXACTLY or AT_MOST.
    final int layoutWidth = SizeSpec.getSize(widthSpec);
    final boolean textFits = (textOutputSize.width <= layoutWidth);

    return textFits ? textComponent : imageComponent;
  }
}
```

## Optimizing OnCreateLayoutWithSizeSpec

`@CreateLayoutWithSizeSpec` can be called more than once in cases where Yoga calls measure.  If the previous layout can be used for the new size spec this call can be avoided. Implementing the `OnShouldCreateLayoutWithNewSizeSpec` allows the spec to specify when the previous layout can be reused.

[`@OnShouldCreateLayoutWithNewSizeSpec`](/javadoc/com/facebook/litho/annotations/OnShouldCreateLayoutWithNewSizeSpec.html) indicates that the annotated method will be called when the component checks if it can use the previous layout with a new size spec. This is used in conjunction with `@OnCreateLayoutWithSizeSpec`. The annotated method must have the following signature:

```java
@OnShouldCreateLayoutWithNewSizeSpec
static boolean onShouldCreateLayoutWithNewSizeSpec(
    ComponentContext c,
    int newWidthSpec,
    int newHeightSpec, ...)
```

The annotated method should return `true` if and only if the Layout Spec should create a new layout for the new size spec. If the method returns `false` then the Component will use the previous layout. In addition,  outputs can be set in `onCreateLayoutWithSizeSpec` which can be referenced in `onShouldCreateLayoutWithNewSizeSpec` method as follows:

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayoutWithSizeSpec
  static Component onCreateLayoutWithSizeSpec(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      Output<Integer> textWidth,
      Output<Boolean> didItFit) {

    final Component textComponent =
        Text.create(c).textSizeSp(16).text("Some text to measure.").build();

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
    final Component imageComponent = Image.create(c).drawableRes(R.drawable.ic_launcher).build();

    // Assuming SizeSpec.getMode(widthSpec) == EXACTLY or AT_MOST.
    final int layoutWidth = SizeSpec.getSize(widthSpec);
    final boolean textFits = (textOutputSize.width <= layoutWidth);

    // set the outputs
    textWidth.set(textOutputSize.width);
    didItFit.set(textFits);

    return textFits ? textComponent : imageComponent;
  }

  @OnShouldCreateLayoutWithNewSizeSpec
  static boolean onShouldCreateLayoutWithNewSizeSpec(
      ComponentContext c,
      int newWidthSpec,
      int newHeightSpec,
      @FromPreviousCreateLayout int textWidth,
      @FromPreviousCreateLayout boolean didItFit) {

    final int newLayoutWidth = SizeSpec.getSize(newWidthSpec);
    final boolean doesItStillFit = (textWidth <= newLayoutWidth);

    // false if it still fits or if still doesn't fit
    return doesItStillFit ^ didItFit;
  }
}
```

![Layout Spec lifecycle flowchart](/static/images/flow-chart-v0.22.1-layout-with-size-spec.svg)