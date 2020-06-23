---
id: using-components
title: Using Components
---
Generated component classes provide a simple builder with the props you defined in your component *spec*. In order to use the generated component in your UI, you'll need a [LithoView](javadoc/com/facebook/litho/LithoView.html), which is an Android `ViewGroup` that is able to render components.

You can assign a component to be rendered by a `LithoView` as follows:

```java
final Component component = MyComponent.create(c)
    .title("My title")
    .imageUri(Uri.parse("http://example.com/myimage"))
    .build();
LithoView view = LithoView.create(c, component);
```

In this example, `MyComponent` will be laid out by the hosting `LithoView`, which you can use in your application as you would normally use an Android View. See the [tutorial](tutorial) for an example on how to use it in an Activity.

:::info
The `LithoView` from this example, if directly used in your view hierarchy as is, will perform layout synchronously on the main thread.
:::

For more information about performing layout off the main thread, see [Async Layout](asynchronous-layout).


