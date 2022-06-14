---
id: first-components
title: Components and Props
---
import useBaseUrl from '@docusaurus/useBaseUrl';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';

In this part of the tutorial, you'll learn the basic Litho building blocks, write a simple component, extend it to accept [props](../kotlin/basics.mdx#props) and get familiar with building layouts with [Flexbox](../kotlin/flexbox-containers.mdx) in Litho.

## Hello World!

To display a "Hello World!" text on the screen with Litho, you need to integrate Litho component hierarchy into your View hierarchy.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/MyActivity.kt start=start_example end=end_example
```

### Key Points

* `LithoView` - a hierarchy of Litho components is rendered using a LithoView.
* `Text(...)` - this is how you create a component, both built-in and components you define yourself.
* `text = "Hello, World!", textSize = 50.dp` -  `text` and `textSize` are **props** ((an input parameters to configure the component)) of the Text component.

## First KComponent

Welcome to your first Litho component! Previously, you used a built-in `Text` component but now you'll define your own. Like Text, your new component can also take **props**.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/HelloComponent.kt start=start_simple_example end=end_simple_example
```

To use your component, you can just replace the Text component from above with your component, setting the `name` prop:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/FirstComponentActivity.kt start=start_example end=end_example
```

### Key Points

* `KComponent` - a class needed to extend in order to create components.
* `render` - function override that returns what your component should render.

:::tip
Lots of code autocompletion and class templates can be found in the [Litho Android Studio plugin](../devtools/android-studio-plugin.md)!
:::

## Introducing Layout

Enough of HelloWorlds, it's time to build an Instagram app!

Before you start, add two simple classes to represent `User` and `Post` data models:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/model/models.kt start=start_example end=end_example
```

Now it's time for UI work!

First, you'll create a component for an Instagram post that requires layouting two images and a text in a particular way. Layouts in Litho are defined via the Flexbox API. To learn more about different layouting options, see the [Layout with Flexbox](../mainconcepts/flexbox-yoga.mdx) page. For now, it's enough to know that the main Flexbox primitives are **Column** and **Row**, which are used to arrange children vertically and horizontally, respectively.

With their help, a `Post` component that will render the UI of an Instagram post will look like that:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostComponent.kt start=start_example end=end_example
```

Though components are placed hierarchically correct, they don't look nice. So, as a final touch you can some Flexbox styles:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostStyledKComponent.kt start=start_example end=end_example
```

Your component should look like the following image.

<img src={useBaseUrl("/images/simple_component_tutorial.png")} width="200px" className="white-background"/>

### Key Points

* `Column` and `Row` - the key container types in Litho. They stack children vertically and horizontally, respectively.
* `padding` and `margin` - check the props that set padding and margin using `Dimen` (a class that represents dimension values in different Android units: `Px`, `Dp`, and `Sp`).
* `drawableRes` - returns an Android Drawable from resource ID.
* `Image` - image is another built-in component that can render a drawable.
