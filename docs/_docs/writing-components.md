---
docid: writing-components
title: Writing Components
layout: docs
permalink: /docs/writing-components
---

## Component Specs
A component *spec* generates the actual component class that you'll use in your UI. There are two types of component specs:

- *Layout spec*: combines other components into a specific layout. This is the equivalent of a ViewGroup on Android.
- *Mount spec*: a component that can render a view or a drawable.

For now, let's just have a look at the overall structure of a *layout spec*:

```java
@LayoutSpec
class MyComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop String title,
      @Prop Uri imageUri) {
    ...
  }
}
```

A few things to note:

 - A component spec is just a plain java class with some special annotations.
 - A component spec is completely stateless and doesn't have any class members.
 - The arguments annotated with `@Prop` will automatically become part of the component's builder.
 - For components to be created from your component specs, you need to add the Litho annotation processor to your BUCK or Gradle file. See the [Getting Started](/docs/getting-started) guide on how to do that. You can make the generated class package-private by adding `isPublic = false` to the class annotation.

## Spec, Lifecycle, and Component classes

A component spec class will be processed to generate a [ComponentLifecycle](/javadoc/com/facebook/litho/ComponentLifecycle) subclass with the same name as the spec but without the *Spec* suffix. For example, a `MyComponentSpec` spec generates a `MyComponent` class.

The generated `ComponentLifecycle` class is what you are going to [use in your product](/docs/using-components). The spec class will be used as a delegate in the generated code at runtime.

The only API exposed by the generated class is a `create(...)` method that returns the appropriate [`Component.Builder`](/javadoc/com/facebook/litho/Component.Builder.html) for the `@Prop`s that you declared in your spec class.

At runtime, all component instances of a certain type share the same `ComponentLifecycle` reference. This means that there will only be one spec instance per component type, not per component instance.

