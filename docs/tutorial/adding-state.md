---
id: adding-state
title: Adding State
---

import useBaseUrl from '@docusaurus/useBaseUrl';

In this part of the tutorial, you'll learn about [useState](../mainconcepts/use-state.mdx), one of Litho's [Hooks](../mainconcepts/hooks-intro.mdx).

`useState` enables a component to persist and update a single value across renders.
You'll start with the component built in the previous step:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostStyledKComponent.kt start=start_example end=end_example
```

The next step will be adding an ability to like a post.
First, you need to declare a component State using the `useState` hook, in which you always need to declare an initial value: `useState` can be only used in the `render()` function:
```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostWithActionsKComponent.kt start=start_state_hook end=end_state_hook
```

To access a state, use `isLiked.value`. To update it, use a lambda variant `isLiked.update { isLiked -> !isLiked }`. To learn more about update options, see the [useState](../mainconcepts/use-state.mdx#value-vs-lambda-variants)) page.
You'll update the state after clicking on the `Image` component:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostWithActionsKComponent.kt start=start_image_button end=end_image_button
```

You can see the implemented behaviour in the video below. Notice how the heart icon changes when clicked.

<video loop="true" autoplay="true" class="video" width="100%" height="500px" muted="true">
  <source type="video/webm" src={useBaseUrl("/videos/useState-tutorial.mov")}></source>
  <p>Your browser does not support the video element.</p>
</video>

### Key Points:

* `useState` - a Hook that enables a component to persist and update a single value across renders.
- You can use useState Hook only in the `render()` function.
