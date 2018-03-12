---
docid: component-tree
title: Creating a ComponentTree
layout: docs
permalink: /docs/component-tree
---

In the [Using Components](/docs/_docs/using-components.md) guide, we saw how you can create a root component and pass it to a `LithoView`, which will take care of creating a [ComponentTree](/docs/javadoc/com/facebook/litho/ComponentTree.html) with the given root. ComponentTree manages your component's lifecycle in a thread-safe way. You can create and make calls to it from any thread.
You shouldn't typically need to do this, but there are situations where you might want to create and manage your own `ComponentTree`.
This is how you can create a `ComponentTree`, pass it a component root and attach it to a 'LithoView'. The `ComponentTree`'s `create()` method returns a [Builder](/docs/javadoc/com/facebook/litho/ComponentTree.Builder.html) which exposes configuration methods for the ComponentTree.  

```java
@Override
public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final LithoView lithoView = new LithoView(this);
    final ComponentContext context = new ComponentContext(this);

    final Component text = Text.create(context)
        .text("Hello World")
        .textSizeDip(50)
        .build();
    final ComponentTree componentTree = ComponentTree.create(context, text).build();

    lithoView.setComponentTree(componentTree);
    setContentView(lithoView);
}
``` 
