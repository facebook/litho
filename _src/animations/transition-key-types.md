---
id: transition-key-types
title: 'Advanced: Transitions key scoping'
---

In all the samples we have presented so far, we followed very common pattern when defining transitions: assigning transition keys and creating transitions happened within one `ComponentSpec`. However, there are situations where this is not the case: transition key is assigned within `ComponentSpec`, while transitions themselves are defined in another.

If you try to do everything exactly how we did it up until now it will not work. It will simply look like there are no transitions defined.

```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyParentComponentSpec.java start=not_working_start end=not_working_end
```
```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyTransitionComponentSpec.java start=not_working_start end=not_working_end
```

The reason is that, by default, transition keys are only visible within the scope of the component spec where it is used. This “visibility” of transition keys is determined by [`TransitionKeyType`](pathname:///javadoc/com/facebook/litho/Transition.TransitionKeyType.html). There are two options:

* [**`LOCAL`**](pathname:///javadoc/com/facebook/litho/Transition.TransitionKeyType.html#LOCAL) - the default type, only visible within `ComponentSpec` where it is used
* [**`GLOBAL`**](pathname:///javadoc/com/facebook/litho/Transition.TransitionKeyType.html#GLOBAL) - makes a transition key visible through the whole `ComponentTree`. The drawback here is that the keys should be unique within the tree. Thus it usually takes an extra effort to use several component of the same type that assign `GLOBAL` transition keys within one tree and avoid transition keys collisions.

:::note
Litho throws an exception when a transition keys collision occurs, which may not be trivial to debug and resolve in case of `GLOBAL` transition keys. Thus we encourage you to use `LOCAL` transition keys and assign transition keys within the same Spec that defines transitions that target those keys.
:::

There are two steps to take to change transition key type:

1. Use [`Component.Builder#transitionKeyType()`](pathname:///javadoc/com/facebook/litho/Component.Builder.html#transitionKeyType-com.facebook.litho.Transition.TransitionKeyType-) when assigning a key to a `Component`.
2. When creating a `Transition` use a version of [`Transition.create()`](pathname:///javadoc/com/facebook/litho/Transition.html#create-com.facebook.litho.Transition.TransitionKeyType-java.lang.String-) that takes `TransitionKeyType` argument along with the key itself.

Here is how we would fix the sample using `TransitionKeyType.GLOBAL`:

```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyParentComponentSpec.java start=start_working end=end_working
```
```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyTransitionComponentSpec.java start=start_working end=end_working
```
