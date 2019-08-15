---
docid: transition-key-types
title: Local and Global Keys
layout: docs
permalink: /docs/transition-key-types
---

In all the samples we have presented so far, we followed very common pattern when defining transitions: assigning transition keys and creating transitions happened within one `ComponentSpec`. However, there are situations where this is not the case: transition key is assigned within `ComponentSpec`, while transitions themselves are defined in another.

If you try to do everything exactly how we did it up until now it will not work. It will simply look like there are no transitions defined.

```java
/**
 * MyComponentSpec.java
 */
@LayoutSpec
class MyComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c) {
        return AnotherComponent.create(c).build();
    }

    @OnCreateLayout
    static Transition onCreateTransition(ComponentContext c) {
        return Transition.create("square")
            .animate(AnimatedProperties.X);
    }
}

/**
 * AnotherComponentSpec.java
 */
@LayoutSpec
class AnotherComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c, @State boolean toRight) {
        return Column.create(c)
            .child(
                Rect.create(c)
                    .color(YELLOW)
                    .widthDip(80)
                    .heightDip(80)
                    .transitionKey("square"))
            .alignItems(toRight ? YogaAlign.FLEX_END : YogaAlign.FLEX_START)
            .build();
    }
}
```

The reason is that, by default, transition keys are only visible within the scope of the component spec where it is used. This “visibility” of transition keys is determined by [`TransitionKeyType`](/javadoc/com/facebook/litho/Transition.TransitionKeyType.html). There are two options:

* [**`LOCAL`**](/javadoc/com/facebook/litho/Transition.TransitionKeyType.html#LOCAL) - the default type, only visible within `ComponentSpec` where it is used
* [**`GLOBAL`**](/javadoc/com/facebook/litho/Transition.TransitionKeyType.html#GLOBAL) - makes a transition key visible through the whole `ComponentTree`. The drawback here is that the keys should be unique within the tree. Thus it usually takes an extra effort to use several component of the same type that assign `GLOBAL` transition keys within one tree and avoid transition keys collisions.

> Note that Litho throws an exception when a transition keys collision occurs, which may not be trivial to debug and resolve in case of `GLOBAL` transition keys. Thus we encourage you to use `LOCAL` transition keys and assign transition keys within the same Spec that defines transitions that target those keys.

There are two steps to take to change transition key type:

1. Use [`Component.Builder#transitionKeyType()`](/javadoc/com/facebook/litho/Component.Builder.html#transitionKeyType-com.facebook.litho.Transition.TransitionKeyType-) when assigning a key to a `Component`.
2. When creating a `Transition` use a version of [`Transition.create()`](/javadoc/com/facebook/litho/Transition.html#create-com.facebook.litho.Transition.TransitionKeyType-java.lang.String-) that takes `TransitionKeyType` argument along with the key itself.

Here is how we would fix the sample using `TransitionKeyType.GLOBAL` (*lines 14, 33*):

```java
/**
 * MyComponentSpec.java
 */
@LayoutSpec
class MyComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c) {
        return AnotherComponent.create(c).build();
    }

    @OnCreateTransition
    static Transition onCreateTransition(ComponentContext c) {
        return Transition.create(Transition.TransitionKeyType.GLOBAL, "square")
            .animate(AnimatedProperties.X);
    }
}

/**
 * AnotherComponentSpec.java
 */
@LayoutSpec
class AnotherComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c, @State boolean toRight) {
        return Column.create(c)
            .child(
                Rect.create(c)
                    .color(YELLOW)
                    .widthDip(80)
                    .heightDip(80)
                    .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                    .transitionKey("square"))
            .alignItems(toRight ? YogaAlign.FLEX_END : YogaAlign.FLEX_START)
            .build();
    }
}
```