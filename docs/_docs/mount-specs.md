---
docid: mount-specs
title: Mount Specs
layout: docs
permalink: /docs/mount-specs
---

A *mount spec* defines a component that can render views or drawables.

Mount specs should only be created when you need to integrate your own views/drawables with Litho. *Mount* here refers to the operation performed by all components in a layout tree to extract their rendered state (a `View` or a `Drawable`) to be displayed.

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

Let's say we want to perform the color name parsing off the UI thread in the `ColorComponent` above. In order to do this, we need a way to pass values generated in the `@OnPrepare` method to the `@OnMount` implementation. Litho provides *inter-stage inputs and outputs* to allow you to do exactly that.

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
    colorDrawable.setColor(color);
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

## ShouldUpdate

A MountSpec can define a method annotated with `@ShouldUpdate` to avoid remeasuring and remounting upon updates.  
Invocations of `@ShouldUpdate` are dependent on whether a Component is a **pure render function**. A Component is a pure render function if the result of the rendering only depends on its props and states. This means that the Component shouldn't be accessing any mutable global variable during `@OnMount`.  
A `@MountSpec` can be defined as pure render by using the pureRender parameter of the `@MountSpec` annotation.
Only pure render Components can assume that when props do not change remounting won't be needed. A `@ShouldUpdate` function can be defined as follows:

``` java
@ShouldUpdate(onMount = true)
public boolean shouldUpdate(Diff<String> someStringProp) {
  return !someStringProp.getPrevious().equals(someStringProp.getNext());
}
```
The parameters taken from `shouldUpdate` are [Diffs](/javadoc/com/facebook/litho/Diff) of Props or State. A Diff is an object containing the value of a `@Prop` or a `@State` in the old components hierarchy and the value of the same `@Prop` or `@State` in the new components hierarchy.
In this example this component was defining **someStringProp** as a String `@Prop`. `shouldUpdate` will receive a `Diff<String>` to be able to compare the old and new value of this `@Prop`.  
`shouldUpdate` has to take into consideration any prop and any states that are used at `@OnMount` time. It can safely ignore props and states that are only used at `@OnMount`/`@OnUnbind` time as these two methods will be executed regardless.

The `onMount` attribute on the `@ShouldUpdate` annotation controls whether this `shouldUpdate` check can happen at mount time. By default, Litho will try to do this reconciliation at layout time, but if layout diffing is turned off it might be useful to set onMount to true in order to execute this check at mount time instead. The `onMount` attribute is set to false by default as the equality check might be heavy itself and make mount performances worse.

`@ShouldUpdate` annotated methods are currently only supported in `@MountSpec`. We have plans to expand the support to complex layouts in the future but at the moment a `@ShouldUpdate` annotated method in a `@LayoutSpec` would have no effect.

## Pre-allocation

When a MountSpec component is being mounted, its `View`/`Drawable` content needs to be either initialized or reused from the recycling pool. If the pool is empty, a new instance will be created at that time, which might keep the UI thread too busy and drop one or more frames. To mitigate that, Litho can pre-allocate a few instances and put in the recycling pool.

``` java
@MountSpec(poolSize = 3, canPreallocate = true)
public class ColorComponentSpec {
  ...
}
```

`canPreallocate` enables pre-allocation for this MountSpec and `poolSize` defines the amount of instances to pre-allocate. For this `ColorComponent` example, three instances of `ColorDrawable` will be created and put in the recycling pool. This option is recommended for MountSpec components that inflate a complex `View`.

