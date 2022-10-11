---
id: transition-definitions
title: 'More Ways to Define Transitions'
---

### @State Arguments

If the Transition definition needs to take the value of the state into account, Litho enables `@OnCreateTransition` methods to take `@State` arguments, as shown in the following snippet:

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
In order to make this happen, the framework needs to postpone the collection of `Transition`s until `MountState` (always on UI thread). Normally, this would be done on `LayoutState`, which may be processed on a background thread.
:::

### @Prop `Diff<T>` and @State `Diff<T>`

If a transition depends not only on the actual (updated) value of a `@Prop` or a `@State`, but also on the value change, a generic [Diff](pathname:///javadoc/com/facebook/litho/Diff.html) can be used for arguments, which provides access to both previous and next `@State`/`@Prop` values.

The following snippet shows how one of the samples could be changed to only animate the expanding action (not the collapsing action):

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

To define transitions that run conditionally under certain circumstances, use the `@OnUpdateStateWithTranstion` method:

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

The method functions as the regular `@OnUpdateState` method but also returns `Transition`.
