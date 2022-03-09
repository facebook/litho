---
id: first-components
title: Components and Props
---

import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

In this part of the tutorial, you'll learn the basic Litho building blocks, write a simple component,
extend it to accept [props](../kotlin/basics.mdx#props) and get familiar with building layouts with [Flexbox](../kotlin/flexbox-containers.mdx) in Litho.

## Hello World!

To display a "Hello World!" text on the screen with Litho you need to integrate Litho component
hierarchy into your View hierarchy.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/MyActivity.kt start=start_example end=end_example
```

### Key Points:

- `LithoView`: A hierarchy of Litho components is rendered using a LithoView
- `Text(...)`: This is how you create a component, both built-in
  ones and components you define yourself.
- `text = "Hello, World!", textSize = 50.dp`: `text` and `textSize` are **props** of the Text component – an input parameters to
  configure the component.

## First KComponent

Welcome to your first Litho component! Previously, you used a built-in `Text` component but now
you'll define your own. Like Text, your new component can also take **props**. Props configure
a component.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/HelloComponent.kt start=start_simple_example end=end_simple_example
```

To use your component, you can just replace the Text component from above with your component,
setting the `name` prop:
```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/FirstComponentActivity.kt start=start_example end=end_example
```

### Key Points:

- `KComponent`: This is a class that we need to extend in order to create components.
- `render`: Function override that returns what your component should render.

:::tip
Lots of code autocompletion and class templates can be found in [Litho Android Studio plugin](../devtools/android-studio-plugin.md)!
:::

## Introducing Layout

Enough of HelloWorlds – let's get to building our Instagram app!

Before we start, let's add 2 simple classes to represent `User` and `Post` data models:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/model/models.kt start=start_example end=end_example
```

Now it's time for UI work!
First you'll create a component for an Instagram post and that requires layouting 2 images
and a text in a particular way. Layouts in Litho are defined via the Flexbox API. You can read more
about different layouting options in [Layout with Flexbox](/mainconcepts/flexbox-yoga.mdx)
doc, but for now it's enough to know that the main Flexbox primitives are **Column** and **Row**,
and they are used to arrange children vertically and horizontally, respectively.

With their help a `Post` component that will render the UI of an Instagram post will look like that:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostComponent.kt start=start_example end=end_example
```

Though hierarchically components are placed correctly, this doesn't look nice, so, as a final touch
let's apply some flexbox styles:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostStyledKComponent.kt start=start_example end=end_example
```

### Key Points:

- `Column` and `Row`: The key container types in Litho. They stack children vertically and
  horizontally, respectively.
- `padding` and `margin`: Are the props that set padding and margin using `Dimen` (a class that represents dimension values in different Android units: `Px`, `Dp`, and `Sp`).
- `drawableRes`: Returns an Android Drawable from resource ID.
- `Image`: Image is another built-in component that can render a drawable.
