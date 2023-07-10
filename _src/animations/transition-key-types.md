---
id: transition-key-types
title: Transition Key Scoping
---

Within the other pages of the 'Animations' section, a common approach is used when defining transitions: assigning transition keys and creating transitions within a single component declaration.

There are situations where this approach is not suitable, such as when the transition key is assigned within one component while the transitions themselves are defined in another.

If an attempt to try to do everything using the same approach contained in the other pages of the Animations section is taken, it won't work. It will look like there are no transitions defined, as shown in the following two components:

```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyParentComponentSpec.java start=not_working_start end=not_working_end
```

```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyTransitionComponentSpec.java start=not_working_start end=not_working_end
```

The reason it won't work is that, **by default, transition keys are only visible within the scope of the component declaration in which they are used**.

The scope of transition keys is determined by the [TransitionKeyType](pathname:///javadoc/com/facebook/litho/Transition.TransitionKeyType.html), for which there are two options:

* [LOCAL](pathname:///javadoc/com/facebook/litho/Transition.TransitionKeyType.html#LOCAL) - the default type; only visible within the component where it's used.
* [GLOBAL](pathname:///javadoc/com/facebook/litho/Transition.TransitionKeyType.html#GLOBAL) - makes a transition key visible through the whole tree of components. The drawback here is that the keys should be unique within the tree. It usually takes extra effort to use several components of the same type that assign the `GLOBAL` transition keys within one tree to avoid transition key collisions.

:::note
Litho throws an exception when a transition key collision occurs, which may not be trivial to debug and resolve in the case of `GLOBAL` transition keys. It's recommended that you use `LOCAL` transition keys and assign those keys within the same component that defined them.
:::

There are two ways to change the transition key type:

1. Use [Component.Builder#transitionKeyType()](pathname:///javadoc/com/facebook/litho/Component.Builder.html#transitionKeyType-com.facebook.litho.Transition.TransitionKeyType-) when assigning a key to a `Component`.
2. When creating a `Transition`, use a version of [Transition.create()](pathname:///javadoc/com/facebook/litho/Transition.html#create-com.facebook.litho.Transition.TransitionKeyType-java.lang.String-) that takes the `TransitionKeyType` argument along with the key itself.

The following components are the ones shown at the top of this page but amended to include `TransitionKeyType.GLOBAL`:

```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyParentComponentSpec.java start=start_working end=end_working
```

```java file=sample/src/main/java/com/facebook/samples/litho/java/animations/docs/keyscope/GlobalKeyTransitionComponentSpec.java start=start_working end=end_working
```
