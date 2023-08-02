---
id: first-components
title: Components and Props
---

In this section of the tutorial, you'll learn the basic Litho building blocks then create your first component that uses [props](../mainconcepts/props.mdx).

## Basic Litho building blocks

:::info Basic Terminology

* **Component** - all user-interactable elements in the UI (such as buttons, checkboxes, scrollbars, lists, menus, and text fields) that you see in the application are components. To be used, a component must be placed in a container.  For more information, see the [Components](../mainconcepts/components-basics.mdx) page in the 'Main Concepts' section.
* **Container component** - arranges groups of components in a [layout](introducing-layout.md).
* **Prop** - an item of data that cannot be changed (making it 'immutable') during the associated component's lifecycle. For more information, see the [Types of Props](../mainconcepts/props.mdx) page in the 'Main Concepts' section.
:::

To display the classic "Hello, World!" text on the screen with Litho, you need to integrate the Litho component hierarchy into your View hierarchy.
To illustrate this, the "Hello, World" code (`MyActivity.kt`), from the [Setting up the Project](project-setup.mdx) section of the tutorial, is shown below:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/MyActivity.kt start=start_example end=end_example
```

### Key Points in MyActivity.kt

* `LithoView` - a hierarchy of Litho components is rendered using a LithoView.
* `Text(...)` - the 'Text' component (this is how you create an instance of a component (both built-in and those you define yourself).
* `Text(...)` - assigns values to the props `text` and `textsize`.

## Create your first component

Previously, you used a built-in `Text` component. Now, you'll define your own using the following 'HelloComponent.kt' code. As with the above, your 'first' component can also declare **props**:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/HelloComponent.kt start=start_simple_example end=end_simple_example
```

### Key Points in HelloComponent.kt

* `KComponent` - a class needed to extend in order to create components.
* `val name` - holds an immutable `String` prop named `name` that cannot be changed during the lifecycle of the component.
* `render` - function override that returns what your component should render.
* `Text(...)` - returns an instance of the `Text` component with its `text` prop set to the string `"Hello $name'`.

:::tip
Lots of code autocompletion and class templates can be found in the [Litho Android Studio plugin](../devtools/android-studio-plugin.md)!
:::

### Use you first component

To use your component, just replace the Text component in the "Hello, World!" example with an instance of your `HelloComponent`:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/FirstComponentActivity.kt start=start_example end=end_example
```

## What next?

The next section of the tutorial [Introducing Layout](introducing-layout.md) helps you become familiar with building layouts using Flexbox.

For more information, see the [Components](../mainconcepts/components-basics.mdx) and [Props](../mainconcepts/props.mdx) pages of the 'Main Concepts' section.
