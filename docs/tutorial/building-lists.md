---
id: building-lists
title: Building Lists
---

:::note
🚧   THIS PAGE IS UNDER CONSTRUCTION
:::

For now, see our guide to using Sections [starting here](../sections/start.mdx) for an overview of how to build lists in Litho with the Sections API.

## Simple Lists

Simple component with vertical list:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/UserFeedKComponent.kt start=start_example end=end_example
```

## Nested Lists

Let's add Instagram-like stories to your feed.

With the following Story component:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/StoryKComponent.kt start=start_example end=end_example
```

Example of nested horizontal list inside a vertical list:

```kotlin file=sample/src/main/java/com/facebook/samples/litho/onboarding/UserFeedWithStoriesKComponent.kt start=start_example end=end_example
```
