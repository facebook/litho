---
id: building-lists
title: Building Lists
---

import useBaseUrl from '@docusaurus/useBaseUrl';

In this section, you'll learn how to build a list of items of the `PostStyledKComponent` that was created previously.
You'll be using the Lazy Collections API for this - refer to the [Building List](../lazycollections/lazycollections.mdx) page for more info.

## Component with a lazy list
As we defined the simple `Post` class to hold information regarding an item that's in turn passed to `PostStyledKComponent`, we will initialize our component with lazy collections with a `List<Post>`.

### Adding children
The list of our data models of `Post` can be mapped to create children for our `LazyList` as seen in below.

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/UserFeedKComponent.kt start=start_example end=end_example
```

You can see the implemented behaviour in the video clip below. Notice how the heart icon persists the state even when the item is scrolled out.

<video loop="true" autoplay="true" class="video" width="100%" height="500px" muted="true">
  <source type="video/webm" src={useBaseUrl("/videos/lazy-list.mov")}></source>
  <p>Your browser does not support the video element.</p>
</video>

### Key Points

* `LazyList` - For items to be arranged as a sequential list.
* `child()` - To add children to the lazy list.
