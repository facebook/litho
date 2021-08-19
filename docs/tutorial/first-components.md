---
id: first-components
title: Components and Props
---

In this part of the tutorial, you'll learn the basic Litho building blocks, write a simple component,
extend it to accept **props** and get familiar with building layouts with Flexbox in Litho.

## Hello World!

To display a "Hello World!" text on the screen with Litho you need to integrate Litho component
hierarchy into your View hierarchy.

```java file=sample/src/main/java/com/facebook/samples/litho/java/onboarding/HelloWorldActivity.java start=start_example end=end_example
```

#### Key Points:
- `LithoView`: A hierarchy of Litho components is rendered using a LithoView
- `Text.create(c)`: This is how you create a component (using a generated builder), both built-in
  ones and components you define yourself.
- `.text('Hello World!')`: `text` is a **prop** of the Text component – an input parameter to
  configure the component.

Ignore `ComponentContext` for now, just know you need one to create a component.

## First Component Spec

Welcome to your first Litho component spec! Previously you used a built-in `Text` component but now
you'll define your own. Like Text, your new component can also take **props**. Props configure
a component.

```java file=sample/src/main/java/com/facebook/samples/litho/java/onboarding/FirstComponentSpec.java start=start end=end
```

To use your component, you can just replace the Text component from above with your component,
setting the `name` prop:
```java file=sample/src/main/java/com/facebook/samples/litho/java/onboarding/FirstComponentSpecActivity.java start=start_example end=end_example
```

#### Key Points:
- `@LayoutSpec`: This annotation tells us that the class defines a component. **The name of your
  component is the name of the spec minus 'Spec'.**
- `@OnCreateLayout`: The static method marked with this annotation returns what your component
  should render. `@LayoutSpec` components delegate to other components, combining, configuring, and
  putting them into layouts. **Composition of components is a key Litho concept!**
- `@Prop`: This defines a **prop** for your component. It will automatically be added to your
  component builder and is required for your component to be built.

:::tip
Your component class is generated at build time via the annotation processor. To get proper IDE
autocompletion without having to rebuild, make sure you've installed the [Litho Android Studio plugin](/docs/devtools/android-studio-plugin)!
:::

## Introducing Layout

Enough of HelloWorlds – let's get to building our Instagram app!

You'll start with creating a component for an Instagram post and that requires layouting 2 images
and a text in a particular way. Layouts in Litho are defined via the Flexbox API. You can read more
about different layouting options in [Layout with Flexbox](docs/mainconcepts/uicomposition/flexbox-yoga)
doc, but for now it's enough to know that the main Flexbox primitives are **Column** and **Row**,
and they are used to arrange children vertically and horizontally, respectively.

With their help a `Post` component that will render the UI of an Instagram post will look like that:

```java file=sample/src/main/java/com/facebook/samples/litho/java/onboarding/PostSpec.java start=start_example end=end_example
```

Though hierarchically components are placed correctly, this doesn't look nice, so, as a final touch
let's apply some flexbox styles:

```java file=sample/src/main/java/com/facebook/samples/litho/java/onboarding/PostStyledSpec.java start=start_example end=end_example
```

#### Key Points:
- `Column` and `Row`: The key container types in Litho. They stack children vertically and
  horizontally, respectively.
- `paddingDip`: This prop sets padding in Android dp units. `padding` along with `margin`, `width`
  and `height` are some of many props where you will find variants like `paddingRes`, `paddingPx`,
  and `paddingAttr` that allow you to define it in different ways.
- `Image`: Image is another built-in component that can render a drawable, and `drawableRes` is
  another prop like padding that can accept a drawable in different ways.
