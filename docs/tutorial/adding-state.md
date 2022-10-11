---
id: adding-state
title: Adding State
---

import useBaseUrl from '@docusaurus/useBaseUrl';

In this section of the tutorial, you'll learn about [useState](../mainconcepts/use-state.mdx), one of Litho's [Hooks](../mainconcepts/hooks-intro.mdx).

`useState` enables a component to persist and update a single value across 'renders'.

:::info
Here, rendering refers to building the layout of the component on which the 'render' was invoked. To 'render' is to build the layout. It does not render anything on the display of the device (no Views are created).
:::

## What you've built so far

The following code shows your first Litho component from the previous section of the tutorial (see the [Introducing Layout](introducing-layout.md) page):

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostStyledKComponent.kt start=start_example end=end_example
```

## Using useState to 'Like' the post

The next step is to enhance the code by giving the user the ability to 'like' the post.

For this, you need to declare a component State using the `useState` hook (you need to declare an initial value):

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostWithActionsKComponent.kt start=start_state_hook end=end_state_hook
```

:::note
`useState` can be only used in the `render()` function
:::

### Managing useState

You can use the val `isLiked` for the following:

* To access the state, use `isLiked.value`.
* To update the state, use a [lambda](../mainconcepts/use-state.mdx#value-vs-lambda-variants) variant: `isLiked.update { isLiked -> !isLiked }`. See the following code for an example of how to update the state.

  * To learn more about the update options, see the [useState](../mainconcepts/use-state.mdx#updating-state) page in the 'Main Concepts' section.

The following code uses a click (.onClick) on the `Image` component to update the state:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/PostWithActionsKComponent.kt start=start_image_button end=end_image_button
```

You can see the implemented behaviour in the video clip below. Notice how the heart icon changes on each click, which matches the state as it switches between false and true.

<video loop="true" autoplay="true" class="video" width="100%" height="500px" muted="true">
  <source type="video/webm" src={useBaseUrl("/videos/useState-tutorial.mov")}></source>
  <p>Your browser does not support the video element.</p>
</video>

### Key Points

* `useState` - a Hook that enables a component to persist and update a single value across renders.
* `render()` - you can use useState Hook only in the `render()` function.

## What next?

You now have a component that is formatted with a Flexbox style and gives the user the ability to express a 'like'.

In the next section of the tutorial, [Building Lists](building-lists.md), you'll learn how to build lists in Litho with the Sections API.
