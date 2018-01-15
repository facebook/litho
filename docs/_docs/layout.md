---
docid: layout
title: Layout
layout: docs
permalink: /docs/layout
---

Litho uses [Yoga](https://facebook.github.io/yoga/) which is an implementation of [Flexbox](https://www.w3.org/TR/css-flexbox-1/) to measure and layout components on screen. If you have used Flexbox on the web before this should be very familiar. If you are more familiar with how Android normally performs Layout then Flexbox will remind you a lot of `LinearLayout`.

In Litho you can use a `Row` to achieve a similar layout to a horizontal `LinearLayout`.

```java
Row.create(c)
    .child(...)
    .child(...)
    .build();
```

Or a `Column` to achieve a similar layout to a vertical `LinearLayout`.

```java
Column.create(c)
    .child(...)
    .child(...)
    .build();
```

To achieve an effect similar to a `LinearLayout` with weights Flexbox provides a concept called `flexGrow(<weight>)`.

```java
Row.create(c)
    .child(
        SolidColor.create(c)
            .color(RED)
            .flexGrow(1))
    .child(
        SolidColor.create(c)
            .color(BLUE)
            .flexGrow(1))
    .build();
```

If you would like to overlay one view on top of the other -- similar to a `FrameLayout` -- Flexbox can do that with `positionType(ABSOLUTE)`.

```java
Column.create(c)
    .child(
        Image.create(c)
            .drawableRes(R.drawable.some_big_image)
            .widthDip(100)
            .heightDip(100))
    .child(
        Text.create(c)
            .text("Overlaid text")
            .positionType(ABSOLUTE))
    .build();
```

For more documentation of specific Flexbox properties check out the [Yoga documentation](https://facebook.github.io/yoga/docs/getting-started/) or explore any web resources on how Flexbox works.
