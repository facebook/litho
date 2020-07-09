---
id: transition-definitions
title: More Ways to Define Transitions
---

### @State Arguments

Litho allows `@OnCreateTransition` methods to take `@State` arguments if the Transition definition should take value of the state into account.

```java
@OnCreateTransition
static Transition onCreateTransition(
      ComponentContext c, @State boolean animationsDisabled) {
  if (animationsDisabled) {
    return null;
  }

  return Transition.create("square").animate(AnimatedProperties.X);
}
```

:::note
In order to make this happen, the framework needs to postpone collection of `Transition`s until `MountState` (always on UI thread), while normally this would be done on `LayoutState` (which may be processed on a background thread).
:::

### @Prop `Diff<T>` and @State `Diff<T>`

If your transitions should depend not only on the actual (updated) value of a `@Prop` or a `@State`, but rather ont the value change, you could use a generic [Diff](/javadoc/com/facebook/litho/Diff.html) for arguments, so you get access to both previous and next `@State`/`@Prop` values.

Here's how we could change one of our samples to only animate expanding, but not collapsing.

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c, @Prop Diff<Boolean> expanded) {
  boolean isExpanding = !expanded.getPrevious() && expanded.getNext();
  if (isExpanding) {
    return Transition.allLayout().animator(ANIMATOR);
  } else {
    return null;
  }
}
```

### @OnUpdateStateWithTransition

To define transitions that should run conditionally under certain circumstances, you can use `@OnUpdateStateWithTranstion` method.

It works as regular `@OnUpdateState` methods, but also returns `Transition` that should be run whenever the method was invoked.
Another implementation for the above sample using `@OnUpdateStateWithTransition` may look like this:

```java
@OnEvent(ClickEvent.class)
static void onClick(ComponentContext c) {
  MyComponent.toggleWithTransition(c);
}

@OnUpdateStateWithTransition
static Transition toggle(StateValue<Boolean> expanded) {
  expanded.set(!expanded.get()); // Updating state value

  boolean isExpanding = expanded.get();
  if (isExpanding) {
    return Transition.allLayout().animator(ANIMATOR);
  } else {
    return null;
  }
}
```
