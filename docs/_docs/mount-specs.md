---
docid: mount-specs 
title: Mount Specs
layout: docs
permalink: /docs/mount-specs
---

A *mount spec* defines a component that can render views or drawables.

Mount specs should only be created when you need to integrate your own views/drawables with the Components framework. "Mount" here means the operation performed by all components in a layout tree to extract their rendered state (a `View` or a `Drawable`) to be displayed.

Mount spec classes should be annotated with `@MountSpec` and implement at least an `@OnCreateMountContent` method. The other methods listed below are optional. 

The life cycle of mount spec components is as follows:

- Run `@OnPrepare` once, before layout calculation.
- Run `@OnMeasure` optionally during layout calculation.
- Run `@OnBoundsDefined` once, after layout calculation.
- Run `@OnCreateMountContent` before the component is attached to a hosting view.
- Run `@OnMount` before the component is attached to a hosting view.
- Run `@OnBind` after the component is attached to a hosting view.
- Run `@OnUnbind` before the component is detached from a hosting view.
- Run `@OnUnmount` optionally after the component is detached from a hosting view. 

## Mounting

Let's start with a simple `ColorComponent` that takes a color name as a prop and mounts its respective `ColorDrawable`.

```java
@MountSpec
public class ColorComponentSpec {
  @OnCreateMountContent
  static ColorDrawable onCreateMountContent(ComponentContext c) {
    return new ColorDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      ColorDrawable colorDrawable,
      @Prop String colorName) {
    colorDrawable.setColor(Color.parseColor(colorName));
  }
}
```

- The mount operation has an API very similar to Android's [RecyclerView Adapters](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.Adapter.html). It has a `onCreateMountContent` method to create and initialize the `View`/`Drawable` content if the recycling pool is empty, and an `onMount` method to update the recycled content with the current information.

- The return type from `onCreateMountContent` should always match the type of the second argument of `onMount`. They are required to be a `View` or a `Drawable` subclass. This is validated by the annotation processor at build time.

- Mounting always happens in the main thread as it might have to deal with Android Views (which are bound to the main thread).

- `onCreateMountContent` cannot take a `@Prop` or any other annotated parameter.

- Given that the `@OnMount` method always runs in the UI thread, expensive operations should not be performed in it. 

## Inter-stage inputs and outputs

You can move heavy operations off the UI thread by performing them in the `@OnPrepare` method, which runs only once before the layout calculation is performed and can be executed in a background thread.

Let's say we want to perform the color name parsing off the UI thread in the `ColorComponent` above. In order to do this, we need a way to pass values generated in the `@OnPrepare` method to the `@OnMount` implementation. The Components framework provides *inter-stage inputs and outputs* to allow you to do exactly that.

Let's have a look at `ColorComponent` with the described `@OnPrepare` method.

```java
@MountSpec
public class ColorComponentSpec {
  @OnPrepare
  static void onPrepare(
      Context context,
      @Prop String colorName,
      Output<Integer> color) {
    color.set(Color.parseColor(colorName));
  }

  @OnCreateMountContent
  static ColorDrawable onCreateMountContent(ComponentContext c) {
    return new ColorDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      ColorDrawable colorDrawable,
      @FromPrepare int color) {
    convertDrawable.setColor(color);
  }
}
```

Using `Output<?>` in any of the `@MountSpec` methods automatically creates an input for the following stages. In this case, an `@OnPrepare` output creates an input for `@OnMount`.

The annotation processor will ensure inter-stage invariants are respected at build time e.g. you cannot use outputs from `@OnMeasure` in `@OnPrepare` as `@OnPrepare` always runs before `@OnMeasure`.

## Measurement

You should implement an `@OnMeasure` method whenever you want to define how your component should be measured during the layout calculation.

Now, let's suppose we want our `ColorComponent` to have a default width and enforce a certain aspect ratio when its height is undefined.

```java
@OnMeasure
static void onMeasure(
    ComponentContext context,
    ComponentLayout layout,
    int widthSpec,
    int heightSpec,
    Size size) {

  // If width is undefined, set default size.
  if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
    size.width = 40;
  } else {
    size.width = SizeSpec.getSize(widthSpec);
  }

  // If height is undefined, use 1.5 aspect ratio.
  if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
    size.height = width * 1.5;
  } else {
    size.height = SizeSpec.getSize(heightSpec);
  }
}
```

You can access component props with the `@Prop` annotation as usual in `@OnMeasure`. SizeSpec's API is analogous to Android's [MeasureSpec](http://developer.android.com/reference/android/view/View.MeasureSpec.html).

Just like `@OnPrepare`, the `@OnMeasure` method can also generate inter-stage outputs (accessible via the `@FromMeasure` argument annotation) and may be performed in a background thread.
