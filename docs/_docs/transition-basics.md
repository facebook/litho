---
docid: transition-basics
title: Basics
layout: docs
permalink: /docs/transition-basics
---

There is very little setup needed to start using Litho Transition.

To show that, let's imagine that we have a simple Component that renders a yellow square, and aligns it to either right or left edge of screen based on value of the `@State boolean toRight`.

```java
@LayoutSpec
class MyComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c, @State boolean toLeft) {
        return Column.create(c)
            .child(
                Rect.create(c)
                    .color(YELLOW)
                    .widthDip(80)
                    .heightDip(80))
            .alignItems(toRight ? YogaAlign.FLEX_END : YogaAlign.FLEX_START)
            .build();
    }
}
```

<video loop autoplay class="video" style="width: 100%; height: 500px;">
  <source type="video/webm" src="/static/videos/transitions/basics1.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>

However, when the value of the state changes, we re-render the `ComponentTree` which makes the square simply “jump” from its previous position to the new one. If such transition between states isn't what you want and you wish it would rather “flow” from one place to the other, we have good news: there are just a couple of things you need to add to your code to make it happen.

Here are the key elements you'll need to work with:

* **`@OnCreateTransition`** method. You need to add a method annotated with `@OnCreateTransition` to your Spec, which is what we use to define the transition animations. It should return a [`Transition`](/javadoc/com/facebook/litho/Transition), and its first argument should always be of `ComponentContext` type. As other lifecycle methods in a Spec, it could also have `@Prop` arguments, as well as arguments of `StateValue` type, although this comes at a cost - more on this later.
* **`Transition`** is a description of which Component/Property (mandatory) and how (optional) you want to animate. You will not use a constructor to create `Transition` instances, instead you will use one of the provided `Builder`s.
* **`transitionKey`** is an identifier that you normally assign to a `Component` that you want to animate, and then use it when defining `Transition`.
* **`AnimatedProperties`** are used to target the property of a `Component` that should be animated when its value changes.

To put it all together, here is what it would look like in our case:

```java
@LayoutSpec
class MyComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c, @State boolean toLeft) {
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

    @OnCreateLayout
    static Transition onCreateTransition(ComponentContext c) {
        return Transition.create("square")
            .animate(AnimatedProperties.X);
    }
}
```

<video loop autoplay class="video" style="width: 100%; height: 500px;">
  <source type="video/webm" src="/static/videos/transitions/basics2.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>

Notice that we:
1. On *line 12* we assign a `transitionKey` to the `Rect` component using [`Component.Builder#transitionKey()`](/javadoc/com/facebook/litho/Component.Builder.html#transitionKey-java.lang.String-) method.
2. On *lines 19-20* we create a `Transition` using [`Transition.create()`](/javadoc/com/facebook/litho/Transition.html#create-java.lang.String-) that takes a `transitionKey` and then specify the property of the component using [`.animate()`](/javadoc/com/facebook/litho/Transition.TransitionUnitsBuilder.html#animate-com.facebook.litho.animation.AnimatedProperty-) method that takes an [`AnimatedProperty`](/javadoc/com/facebook/litho/animation/AnimatedProperties).

Both of these methods take [variable number of arguments](/javadoc/com/facebook/litho/Transition.html#create-java.lang.String...-), so the description of the multiple `Transition`s may nicely be collapsed and it may look like this:

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
    return Transition.create("square", "oval", "another_shape")
        .animate(AnimatedProperties.X, AnimatedProperties.Y);
}
```

### Key Features

- *Interruptible*: Animations can be interrupted and driven to a new ending value automatically.
- *Declarative*: The framework handles and drives the animations for you, meaning you get 60fps, interruptible animations without extra work.
- *Animated Properties*: Supports animating `X`, `Y`, `WIDTH`, `HEIGHT`, `SCALE`, `ALPHA`, and `ROTATION`.