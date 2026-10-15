---
id: immutability
title: Immutability
keywords:
    - immutability
    - immutable
    - thread safety
    - mutable objects
    - data class
    - props immutability
    - state immutability
api_category: best-practices
related_docs: [props-vs-state, use-state]
---

Components are essentially functions that receive data as parameters and are immutable (cannot be changed). When the props or state of a component change, the framework will create a new component instance with the updated information, because the previous component cannot be mutated.

While the component itself is immutable, it is easy to make it not thread safe by using mutable objects for props and state. Litho computes layout on a background thread, and if the objects that make up a component's props or state are mutated from another thread, then this may result in rendering different outputs for the same component.

:::note Best Practice
Props and state must be kept as either primitive types (which are inherently immutable), or, if it's not possible to use a primitive type, an alternative that is also immutable.
:::

## Common Mistakes

| Mistake | Problem | Fix |
|---------|---------|-----|
| Using `MutableList` as prop | Can be modified from another thread | Use `List` (immutable interface) |
| Using `var` in data class | State can be mutated | Use `val` only in data classes |
| Modifying object after passing as prop | Race conditions | Create new copy with changes |

## Examples

:::caution Anti-Pattern: Mutable Props
```kotlin
// ❌ Incorrect - MutableList can be modified
class BadComponent(private val items: MutableList<String>) : KComponent()
```
:::

:::tip Pattern: Immutable Props
```kotlin
// ✅ Correct - List is immutable interface
class GoodComponent(private val items: List<String>) : KComponent()
```
:::

:::caution Anti-Pattern: Mutable Data Class Properties
```kotlin
// ❌ Incorrect - var properties can be mutated
data class BadUserData(
    var name: String,  // Can be changed!
    var age: Int       // Can be changed!
)
```
:::

:::tip Pattern: Immutable Data Classes
```kotlin
// ✅ Correct - all properties are val
data class UserData(
    val name: String,
    val age: Int
)
```
:::

## See Also

- [Props vs State](props-vs-state.md) - When to use props vs state
- [useState](../state-and-side-effects/use-state.mdx) - State management in Litho
