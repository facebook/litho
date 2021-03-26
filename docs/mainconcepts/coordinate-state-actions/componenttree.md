---
id: componenttree
title: "ComponentTree"
---

:::danger Content needs to be updated
Moved from old website without any change.
:::

In the [Using Components](/docs/using-components) guide, we saw how you can create a root component and pass it to a `LithoView`, which will take care of creating a [`ComponentTree`](pathname:///javadoc/com/facebook/litho/ComponentTree.html) with the given root. `ComponentTree` manages your component's lifecycle in a thread-safe way. You can create and make calls to it from any thread.

You shouldn't typically need to do this, as you usually provide a component to your LithoView instead as shown in [Using Components](/docs/using-components). However, there are situations where you might want to create and manage your own `ComponentTree`.

In order to create a `ComponentTree`, you pass it a component root and attach it to a `LithoView`. The `ComponentTree`'s `create()` method returns a [Builder](pathname:///javadoc/com/facebook/litho/ComponentTree.Builder.html) which exposes configuration methods for the `ComponentTree`.

```java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final LithoView lithoView = new LithoView(this);
    final ComponentContext c = new ComponentContext(this);

    final Component text = Text.create(c)
        .text("Hello World")
        .textSizeDip(50)
        .build();
    final ComponentTree componentTree = ComponentTree.create(c, text).build();

    lithoView.setComponentTree(componentTree);
    setContentView(lithoView);
}
```
