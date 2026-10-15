---
id: transition-key-types
title: Transition Key Scoping
keywords:
    - transition key type
    - local key
    - global key
    - key scoping
---

Transition keys control which components should be animated when layout changes occur. Understanding transition key scoping is essential when coordinating animations across different components in your hierarchy.

## Understanding Key Scopes

Within the other pages of the 'Animations' section, a common approach is used when defining transitions: assigning transition keys and creating transitions within a single component declaration.

There are situations where this approach is not suitable, such as when the transition key is assigned within one component while the transitions themselves are defined in another.

If you attempt to define transitions for keys assigned in child components, it won't work by default. The transitions will appear to be ignored, as shown in the following two components:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/animations/docs/GlobalKeyNotWorkingComponents.kt start=start_not_working_parent end=end_not_working_parent
```

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/animations/docs/GlobalKeyNotWorkingComponents.kt start=start_not_working_child end=end_not_working_child
```

The reason it won't work is that, **by default, transition keys are only visible within the scope of the component declaration in which they are used**.

## TransitionKeyType

The scope of transition keys is determined by the `TransitionKeyType`, for which there are two options:

### LOCAL (Default)

The default type; only visible within the component where it's used. This is the **recommended approach** for most use cases as it prevents key collision issues.

```kotlin
import com.facebook.litho.Style
import com.facebook.litho.Transition.TransitionKeyType
import com.facebook.litho.transition.transitionKey

// Explicitly specify LOCAL type (though it's the default)
val style = Style.transitionKey(
    context = context,
    transitionKey = "my_key",
    transitionKeyType = TransitionKeyType.LOCAL
)
```

**Use LOCAL when:**

- Animating components within a single component's render method
- You want to avoid key collision issues
- The animation only involves components that are siblings or direct descendants

### GLOBAL

Makes a transition key visible through the whole tree of components. Use this when you need to animate between components defined in different parts of the tree.

```kotlin
import com.facebook.litho.Style
import com.facebook.litho.Transition.TransitionKeyType
import com.facebook.litho.transition.transitionKey

// Global transition key - visible throughout the tree
val style = Style.transitionKey(
    context = context,
    transitionKey = "shared_element",
    transitionKeyType = TransitionKeyType.GLOBAL
)
```

**Use GLOBAL when:**

- You need to animate between components defined in separate component classes
- Creating shared element transitions between different parts of the UI
- Coordinating animations across parent and child components defined in different files

**Important considerations for GLOBAL keys:**

- Keys must be unique across the entire component tree to avoid collisions
- It usually takes extra effort to use several components of the same type that assign `GLOBAL` transition keys within one tree
- Debugging key collisions can be challenging

:::caution Key Collision Warning
Litho throws an exception when a transition key collision occurs, which may not be trivial to debug and resolve in the case of `GLOBAL` transition keys. It's recommended that you use `LOCAL` transition keys and assign those keys within the same component that defined them.
:::

## Setting the Transition Key Type

There are two ways to change the transition key type:

### 1. When assigning the key to a component

```kotlin
import com.facebook.litho.Style
import com.facebook.litho.Transition.TransitionKeyType
import com.facebook.litho.transition.transitionKey

// Set the key type when assigning to the component
val style = Style.transitionKey(
    context = context,
    transitionKey = "element_key",
    transitionKeyType = TransitionKeyType.GLOBAL
)
```

### 2. When creating a Transition

```kotlin
import com.facebook.litho.Transition
import com.facebook.litho.Transition.TransitionKeyType
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.transition.useTransition

// Specify the key type when creating the transition
useTransition(
    Transition.create("element_key", TransitionKeyType.GLOBAL)
        .animate(AnimatedProperties.X)
)
```

## Working Example with GLOBAL Keys

The following components show how to properly use `GLOBAL` keys to coordinate animations across parent and child components:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/animations/docs/GlobalKeyWorkingComponents.kt start=start_working_parent end=end_working_parent
```

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/animations/docs/GlobalKeyWorkingComponents.kt start=start_working_child end=end_working_child
```

## Best Practices

1. **Default to LOCAL keys**: Use `LOCAL` keys whenever possible to avoid collision issues
2. **Unique GLOBAL keys**: When using `GLOBAL` keys, ensure they are unique across your entire component tree
3. **Name consistently**: Use descriptive, prefixed names for GLOBAL keys (e.g., `"feature_shared_element"`) to avoid conflicts
4. **Keep animations local**: Try to structure your components so animations can be defined and used within the same component
