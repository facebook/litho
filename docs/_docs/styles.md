---
docid: styles
title: Styles
layout: docs
permalink: /docs/styles
---

Components can have their props come from regular Android style resources in a similar way to an Android View's AttributeSet constructor. It enables developers to define static prop values or prop defaults directly from style resources.

Style support can be implemented using the `@OnLoadStyle` method in your component spec. The first argument is a ComponentContext which you can use to retrieve a TypedArray with the style resource values. The rest of the arguments should be Outputs matching the name and type of the props to which you want to set values.

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop String prop1,
      @Prop int prop2) {

    return ...;
  }
}
```

For example, in order to implement style support for the two props of the MyComponentSpec above, you'd first define the styleable attributes as usual:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>

  <attr name="prop1" format="string" />
  <attr name="prop2" format="integer" />

  <declare-styleable name="MyComponent">
    <attr name="prop1" />
    <attr name="prop2" />
  </declare-styleable>

</resources>
```

Then you can collect the values of these attributes in your `@OnLoadStyle` method as follows:

```java
@OnLoadStyle
void onLoadStyle(
    ComponentContext c,
    Output<String> prop1,
    Output<Integer> prop2) {

  final DataBoundTypedArray a =
      c.obtainDataBoundAttributes(R.styleable.Text, 0);

  for (int i = 0, size = a.getIndexCount(); i < size; i++) {
    final int attr = a.getIndex(i);

    if (attr == R.styleable.MyComponent_prop1) {
      prop1.set(a.getString(attr));
    } else if (attr == R.styleable.MyComponent_prop2) {
      prop2.set(a.getInteger(attr));
    }
  }

  a.recycle();
}
```

With this, you'll be able to define `prop1` and `prop2` in a style:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="SomeStyle">
        <item name="prop1">@string/some_string</item>
    </style>
</resources>
```

And use it in `MyComponent`:

```java
MyComponent.create(c, 0, R.style.SomeStyle)
    .prop2(10)
    .build();
```

In which case, `prop1` would get the value from the `@string/some_string` resource.
