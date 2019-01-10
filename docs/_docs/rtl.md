---
docid: rtl
title: RTL
layout: docs
permalink: /docs/rtl
---

Support for right-to-left (RTL) layouts in Litho follows a similar pattern to Android's RTL support. In order to make your component RTL-ready, simply use the `START` and `END` variants of padding and margin parameters instead of `LEFT` and `RIGHT`. All the rest will be handled by the layout system automatically.

For example, here's a layout:

```java
Column.create(c)
    .paddingDip(START, 10)
    .marginDip(END, 5)
    .child(...)
    .child(...)
    .build();
```

The layout system will automatically follow the layout direction defined by Android's resource system. You can also make absolute positions RTL-aware by using the analogous start/end variants for position attributes:

```java
Image.create(c)
    .drawableRes(R.drawable.my_image)
    .positionType(ABSOLUTE)
    .positionDip(START, 10)
    .build();
```

In the example above, the image component will be automatically placed 10 pixels from the right edge of its parent when RTL is enabled.
