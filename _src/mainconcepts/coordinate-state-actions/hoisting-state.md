---
id: hoisting-state
title: Hoisting State
---

Often, several components need to reflect the same changing value. Rather than a [state](/docs/codegen/state-for-specs) for each component, it is better to host a single state in their closest common ancestor.

## Interface Scenario

Consider an interface that converts temperatures between Celsius and Fahrenheit where modifying one value will cause the other to be updated.

Using separate Celsius and Fahrenheit states would be difficult and error prone: complicated book-keeping would be needed to keep the two values synchronised. Furthermore, these values would represent the same temperature, so there is repetition of data and no single source of truth.

It's preferable to make each temperature input component stateless and to use a single state in the parent. This is called a 'Hoisting State', as the state is lifted up from the children into the parent.

A single state can be introduced in the parent to represent the temperature in Celsius. The latest value and an update callback can be passed down as a prop to the inputs. For the Fahrenheit component, the temperature conversion formula is applied to the prop and in the update callback. Now, all the child components can use and modify the temperature value. The following code provides an example.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/kotlin/documentation/HoistState.kt  start=start_example end=end_example
```
