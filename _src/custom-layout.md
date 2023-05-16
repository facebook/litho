---
id: custom-layout
title: Manual Measurement
---
import useBaseUrl from '@docusaurus/useBaseUrl';

Litho relies on [Yoga](https://yogalayout.com/docs/), a powerful layout engine that can create very complex UIs for layout calculations.  However, there are few exceptions where Yoga is not sufficient and you may need to implement your own measuring and layout logic.

Litho provides a manual component measurement API for determining component sizes during layout creation, which enables Developers to implement dynamic logic based on component sizes.

:::caution IMPORTANT
 This API comes with a **non-negligible** performance overhead.
 Litho is built to optimise when a measure occurs for any component. Measuring a component with this API ends up performing additional measurements to the ones intrinsic to Litho's lifecycle. Measurement can become a heavy operation, especially with more complex layouts, so be sure to only utilize this API when absolutely necessary.
:::

## Use Cases

* **A component layout tree depends on its own and/or children's size.** For example, perhaps a component layout should use a child only if it fits within its size constraints. If the child doesn't fit, the layout should instead use another child as a fallback.

* **Children of a container must be absolutely positioned manually based on their/parent size.** Yoga can absolutely position children in a parent. However, the position itself might depend on the child sizes after being measured using the parent size constraints. Margins or paddings need to be manually considered if required.

* **This API should only be used during layout creation.** Using the API elsewhere may cause unintended behaviour.

## Size Specs

Before diving into the API, it's recommended that you familiarise yourself with how the [onMeasure](https://developer.android.com/reference/android/view/View.html#onMeasure(int,%20int)) function works in a regular Android `View`.  Also,  what a [MeasureSpec](https://developer.android.com/reference/android/view/View.MeasureSpec.html) is, since Litho uses an analogous concept called [SizeSpec](pathname:///javadoc/com/facebook/litho/SizeSpec.html).

Similar to the Android `MeasureSpec` equivalent, Litho's `SizeSpec` is composed of a size and a mode. The possible modes, same as for `MeasureSpec`, are: `UNSPECIFIED`, `EXACTLY`, and `AT_MOST`.

## Measuring a Component

A component can be measured in isolation for a given `SizeSpec`. A `Size` object, passed as an argument, will be populated with the resulting size.

In the following example, a `Text` component is measured with unspecified `SizeSpec`, implying a single line of text indefinitely long.

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

## SizeSpec Information During Layout

During layout creation, the API can provide information about the `SizeSpec`s with which the component is going to be measured. To access this information, the [@OnCreateLayoutWithSizeSpec](pathname:///javadoc/com/facebook/litho/annotations/OnCreateLayoutWithSizeSpec.html) annotation needs to be used instead of `@OnCreateLayout`. The arguments of the annotated method, besides the standard `ComponentContext`, are two more integers representing the width spec and the height spec.

Similar to Android's `MeasureSpec`, you can resolve the exact size of a width or height spec integers by using `SizeSpec.getSize(widthSpec)`, and the mode with `SizeSpec.getMode(widthSpec)`.

In the following example, a `Text` component is measured to check if the given text fits in the available space. An `Image` component is otherwise used.

``` java file=sample/src/main/java/com/facebook/samples/litho/java/documentation/LongTextReplacerComponentSpec.java start=start_example end=end_example
```

## Kotlin Integration

Kotlin equivalent of `@OnCreateLayoutWithSizeSpec` is called `SizeConstraintsAwareComponent`. `SizeConstraintsAwareComponent` is a Component that defines its own content according to the available space, based on the incoming `SizeConstraints`. It can be used in situations when a different content needs to be displayed depending on the available space.

Below, there is an example that uses `SizeConstraints` provided by `SizeConstraintsAwareComponent`:

``` kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/sizeconstraintsawarecomponent/SizeConstraintsAwareComponentKComponent.kt start=start_sizeconstraintsawarecomponent_example end=end_sizeconstraintsawarecomponent_example
```

:::note
In Kotlin the `SizeSpec` has been replaced with `SizeConstraints`. It's an object that provides the minimum and maximum width and height available for a Component. SizeConstraints are provided by parent Component to a child component. A child Component should define its size within those constraints.
:::
