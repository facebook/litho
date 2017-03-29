---
docid: using-components
title: Using Components
layout: docs
permalink: /docs/using-components
---
Generated component classes provide a simple builder with the props you defined in your component *spec*. In order to use the generated component in your UI, you'll need two things: a `ComponentTree` and a `ComponentView`.

`ComponentTree` manages your component's lifecycle in a thread-safe way. You can create and make calls to it from any thread. `ComponentView` is an Android `ViewGroup` that is able to render components.

You can display a component by creating a `ComponentTree` and setting it on a `ComponentView` as follows:

```java
ComponentView view = new ComponentView(context);

ComponentTree component = ComponentTree.create(
    context,
    MyComponent.create()
        .title("My title")
        .imageUri(Uri.parse("http://example.com/myimage")))
    .build();

view.setComponent(component);
```

In this example, `MyComponent` will be laid out by the hosting `ComponentView` once it gets attached to a view tree.

IMPORTANT: The ComponentView from this example, if directly used in your view hierarchy as is, will perform layout synchronously on the main thread.

For more information about performing layout off the main thread, see TODO get correct link for this: [[ components-for-android/async-layout | Async Layout ]].

components.md 
