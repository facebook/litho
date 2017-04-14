---
docid: using-components
title: Using Components
layout: docs
permalink: /docs/using-components
---
Generated component classes provide a simple builder with the props you defined in your component *spec*. In order to use the generated component in your UI, you'll need two things a [LithoView](/javadoc/com/facebook/litho/LithoView), which is an Android `ViewGroup` that is able to render components.

You can assign a component to be rendered by a `LithoView` as follows:

```java
final Component component = MyComponent.create()
    .title("My title")
    .imageUri(Uri.parse("http://example.com/myimage")
    .build();
LithoView view = LithoView.create(context, component);    
```

In this example, `MyComponent` will be laid out by the hosting `LithoView`, which you can use in your application as you would normally use an Android View. See the [tutorial](/docs/tutorial#hello-world) for an example on how to use it in an Activity.

> IMPORTANT: The LithoView from this example, if directly used in your view hierarchy as is, will perform layout synchronously on the main thread.  
For more information about performing layout off the main thread, see [Async Layout](/docs/architecture#async-layout).

## Extra
We saw how you can create a root component and pass it to a `LithoView`, which will take care of creating a [ComponentTree](/javadoc/com/facebook/litho/ComponentTree) with the given root. ComponentTree manages your component's lifecycle in a thread-safe way. You can create and make calls to it from any thread.
You shouldn't typically need to do this, but there are situations where you might want to create and manage your own `ComponentTree`, such as turning off [incremental mount](/docs/intro#incremental-mount).  
This is how you can create a `ComponentTree`, pass it a component root and attach it to a 'LithoView'. The `ComponentTree`'s `create()` method returns a [Builder](/javadoc/com/facebook/litho/ComponentTree.Builder) which exposes configuration methods for the ComponentTree.  

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
