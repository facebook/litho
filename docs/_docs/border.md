---
docid: border
title: Borders
layout: docs
permalink: /docs/borders
---

Litho provides extensive support for stylized borders on component layouts. All of the available options are specified through a `Border` object via a builder pattern.

## Border Widths

Litho supports setting a border width for all edges, or a different width per edge. Specifying a width for a certain edge is done via the various `width` builder methods. 

For example, let's say you wanted to specify a 10 dip border width for all edges:

![Red Border](/static/images/border-all.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      .build())
  .build()
```

Or that you'd like to specify specific widths for a specific edge:

![Red Left Border](/static/images/border-left.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.LEFT, 5)
      .color(YogaEdge.LEFT, 0xfff36b7f)
      .build())
  .build()
```

Maybe you just want something different per edge:

![Varying Border Width](/static/images/border-varying-all.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.START, 5)
      .widthDip(YogaEdge.TOP, 10)
      .widthDip(YogaEdge.RIGHT, 15)
      .widthDip(YogaEdge.BOTTOM, 20)
      .color(YogaEdge.ALL, 0xfff36b7f)
      .build())
  .build()
```

## Border Colors

Setting a color for a certain border edge is set via the various `color` builder methods. 

You can set each border edge to any color you'd like:

![Varying Border Color](/static/images/border-color-all.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.START, 0xfff36b7f)
      .color(YogaEdge.TOP, 0xff7ff36b)
      .color(YogaEdge.END, 0xff6b7ff3)
      .color(YogaEdge.BOTTOM, 0xfff39b6b)
      .build())
  .build();
```

It still works for varying widths:

![Varying Border Color with Varying Width](/static/images/border-color-varying-width.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.START, 5)
      .widthDip(YogaEdge.TOP, 10)
      .widthDip(YogaEdge.RIGHT, 15)
      .widthDip(YogaEdge.BOTTOM, 20)
      .color(YogaEdge.START, 0xfff36b7f)
      .color(YogaEdge.TOP, 0xff7ff36b)
      .color(YogaEdge.END, 0xff6b7ff3)
      .color(YogaEdge.BOTTOM, 0xfff39b6b)
      .build())
  .build();
```

## Border Radius

![Border Radius](/static/images/border-radius-xy.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      // We set a border radius of 10 dip here
      .radiusDip(10f)
      .build())
  .build();
```

You may also set a separate radius value per the X and Y dimensions:

![Varying Border Radii](/static/images/border-radius-separate.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      // We set a border X radius of 10 dip and Y radius of 30
      .radiusXDip(10f)
      .radiusYDip(30f)
      .build())
  .build();
```

## Border Effects

Border effects are powerful tools to help you stylize your borders even further. The following effects are currently available:
- [Dash](#dash)
- [Discrete](#discrete)
- [Path Dash](#path-dash)
- [Composition](#composition)

Under the hood, each effect utilizes a standard [PathEffect](https://developer.android.com/reference/android/graphics/PathEffect.html) supplied by the Android Framework. You may use up to two effects at the same time. If you use two effects, they will both be composed with each other in the order given.

Border effects are specified via the various `*Effect` methods on the `Border.Builder` object.

> IMPORTANT: Currently Litho does not support varying border widths with effects. Each border width must be the same.

### Dash

The dash effect applies a sequence of "on" and "off" sections of the border color. This effect utilizes [DashPathEffect](https://developer.android.com/reference/android/graphics/DashPathEffect.html) internally.

![Dash Effect](/static/images/border-dash-effect.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      // We want "on" segments of length 10, "off" of length 5
      // We also specify 0 for the phase as we do not want to offset the start
      .dashEffect(new float[] {10f, 5f}, 0f)
      .build())
  .build();
```

### Discrete

The discrete effect will divide your border into segments whereby each segment will randomly deviate in the cross axis. This effect utilizes [DiscretePathEffect](https://developer.android.com/reference/android/graphics/DiscretePathEffect.html) internally.

![Discrete Effect](/static/images/border-discrete-effect.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      // We set a segment length of 10 with a maximum deviation of 5
      .discreteEffect(10f, 5f)
      .build())
  .build();
```

### Path Dash

Path dash will take in a custom Android path object and continually stamp the path along the border. This effect utilizes [PathDashPathEffect](https://developer.android.com/reference/android/graphics/PathDashPathEffect.html) internally.

![Path Effect](/static/images/border-pathdash-effect.png)

```java
// Our stamp shape will be a simple circle
Path dot = new Path();
dot.addCircle(0f, 0f, 5f, Path.Direction.CCW);

Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      // We set the spacing between stamps to 15
      .pathDashEffect(dot, 15f, 0f, PathDashPathEffect.Style.ROTATE)
      .build())
  .build();
```

### Composition

You may compose multiple effects by simply specifying more than one. This effect utilizes [ComposePathEffect](https://developer.android.com/reference/android/graphics/ComposePathEffect.html) internally.

> IMPORTANT: Currently Litho does not support composing more than two effects.

![Composed Effect](/static/images/border-composed-effect.png)

```java
Row.create(c)
  .child(
    Text.create(c)
      .text("Hello Litho")
      .textSizeSp(16))
  .border(
    Border.create(c)
      .widthDip(YogaEdge.ALL, 5)
      .color(YogaEdge.ALL, 0xfff36b7f)
      // The effects below will be composed together
      // resulting in a discrete, dashed border
      .dashEffect(new float[] {10f, 5f}, 0f)
      .discreteEffect(20f, 10f)
      .build())
  .build();
```