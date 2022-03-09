---
id: adding-state
title: Adding State
---

:::note
🚧   THIS PAGE IS UNDER CONSTRUCTION
:::


You'll start with the component built in the previous step:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostStyledKComponent.kt start=start_example end=end_example
```

The next step will be adding an ability to like a post.
First of all you need to declare a component State using `useState` hook:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostWithActionsKComponent.kt start=start_state_hook end=end_state_hook
```

Then use this State:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostWithActionsKComponent.kt start=start_image_button end=end_image_button
```

Notice, how instead of reading and writing `isLiked` variable directly you need to use
`isLiked.value` to read the current State value and `isLiked.update()` method to write a new value
into the State.
