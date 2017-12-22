---
docid: transition-animations
title: Transition Animations
layout: docs
permalink: /docs/transition_animations
---

***Note:*** **This API is in beta and will likely change in the future.**

Litho allows declaring layout animations that will be triggered when certain layout changes happen. We can declare animations when a component appears on screen, disappears from screen, or changes some properties. As of now we support animating opacity, scale, and position of components.

<video loop autoplay class="video" style="width: 600px; height: 300px; display: block; margin: auto;">
  <source type="video/mp4" src="/static/videos/transitions_demo1.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

## Declarative Animations

In the same way Litho requires thinking in a different way from standard Android to build UI, declarative animations also require thinking in a different way from standard Android. If you've used CSS transitions on web, this style of animating will be familiar to you.

Animations in Litho define transitions between two different states of the UI. These states of the UI are declared as normal via `@OnCreateLayout`. Animations introduce a second lifecycle method, `@OnCreateTransition`, which defines how the UI should animate from one layout state to the next.

For example, let's say you declare a component that has two states: one where, based on a boolean prop, 1) the red box is rendered on the left and 2) where it's rendered on the right:

<img src="/static/images/transitions_blocks_demo.png" style="width: 600px;">

<br />
The code for this might look something like this:

```java
@OnCreateLayout
static Component onCreateLayout(
    ComponentContext c,
    @Prop boolean left) {
  return Row.create(c)
      .positionPx(YogaEdge.LEFT, left ? 0 : 100)
      .backgroundColor(Color.RED)
      .transitionKey("red")
      .build();
}
```

<br />
Then we can animate the change in `x` as the `left` prop changes using `@OnCreateTransition`:

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
  return Transition.create("red")
      .animate(AnimatedProperties.X);
}
```

<br />
The end result is the following:

<video loop autoplay class="video" style="width: 600px; height: 100%;">
  <source type="video/mp4" src="/static/videos/blocks_demo_animation.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

<br />
**Things to Observe**
- The key `"red"` referenced in `Transition.create` in `@OnCreateTransition` is matched with the `transitionKey` prop from `@OnCreateLayout`: the transition key transitions to be targeted at specific components, and also allows Litho to identify a component as being the same across multiple renders.
- The use of a builder pattern: `Transition.create` allows you to start declaring a transition on a key or set of keys. It supports one or more `.animate()` calls which specify a property or set of properties to animate. See the targeting section below.

## Features

- *Interruptible*: Animations can be interrupted and driven to a new ending value automatically
- *Declarative*: The framework handles and drives the animations for you, meaning you get 60fps, interruptible animations without extra work.
- *Current Properties*: Currently supports animating `X`, `Y`, `SCALE`, and `ALPHA`. We hope to support `WIDTH` and `HEIGHT` in the near future.

<br />
<video loop autoplay class="video" style="width: 600px; height: 100%;">
  <source type="video/mp4" src="/static/videos/transitions_demo2.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

## API Capabilities

### Appearing and Disappearing Components

Transitions for appearing and disappearing components are a little bit different since their properties don't have an implicit source value or destination value, respectively.

In the API, you can specify `appearFrom` and `disappearTo` values for properties after selecting them with `.animate()`. For example, to animate `ALPHA` and `SCALE` to/from 0 when a component with the transition key `"animateMe"` appears/disappears, you can write:

```java
return Transition.create("animateMe")
    .animate(AnimatedProperties.ALPHA)
    .appearFrom(0)
    .disappearTo(0)
    .animate(AnimatedProperties.SCALE)
    .appearFrom(0)
    .disappearTo(0);
```

<br />
`appearFrom` and `disappearTo` values can also be relative to the last known value of some property on a component. For example, to animate a slide-from-right-and-fade-in appearance/slide-out-right-and-fade-out animation, you can write:

```java
return Transition.create("animateMe")
    .animate(AnimatedProperties.ALPHA)
    .appearFrom(0)
    .disappearTo(0)
    .animate(AnimatedProperties.X)
    .appearFrom(DimensionValue.offsetDip(-100))
    .disappearTo(DimensionValue.offsetDip(100));
```

<br />
See [the javadocs for `DimensionValue`](/javadoc/index.html?com/facebook/litho/animation/DimensionValue.html) for more options of relative position values.


### Targeting

Litho transitions support targeting a single component, sets of components, or all components with transition keys. Similarly, transitions can target a single property, a set of properties, or 'all' properties.

1\. Animate 'all' properties on all components with transition keys:

```java
Transition.create(Transition.allKeys())
    .animate(Transition.allProperties())
```

<br />
2\. Animate the `X` property on all components with transition keys:

```java
Transition.create(Transition.allKeys())
    .animate(AnimatedProperties.X)
```

<br />
3\. Animate all properties on the component with transition key `'red'`:

```java
Transition.create('red')
    .animate(Transition.allProperties())
```

<br />
4\. Animate `X` and `Y` on the components with transition keys `'red'` and `'blue'`

```java
Transition.create('red', 'blue')
    .animate(AnimatedProperties.X, AnimatedProperties.Y)
```

<br />
**NOTE**: When we say 'all' properties, right now we just mean the supported properties listed above.

### Staggers, Sequences, and Parallel Sets

Litho transitions support creating sets of animations that can run in parallel, in sequence, or on a stagger. These sets can also be nested within each other, e.g. you can have a stagger of parallel animation sets. More info:

- **`Transition.parallel`**: All child animations start at the same time. This animation is considered finished once all its child animations have finished.

<br />
<video loop autoplay class="video" style="width: 600px; height: 100%;">
  <source type="video/mp4" src="/static/videos/parallel_demo.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

<br />
- **`Transition.stagger`**: All child animations start one after another, on a stagger. You must specify the stagger interval. Like a parallel set, this animation is considered finished once all its child animations have finished.

<br />
<video loop autoplay class="video" style="width: 600px; height: 100%;">
  <source type="video/mp4" src="/static/videos/stagger_demo.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

<br />
- **`Transition.sequence`**: The animations in this set run one after another. Each child animation must finish before the next animation will start.

<br />
<video loop autoplay class="video" style="width: 600px; height: 100%;">
  <source type="video/mp4" src="/static/videos/sequence_demo.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

<br />
**Interrupting Behavior**: Sequences and staggers also support interrupting behavior, trying to preserve the guarantee that a component will 1) never jump 2) will always end up in the correct final position:

<br />
<video loop autoplay class="video" style="width: 600px; height: 100%;">
  <source type="video/mp4" src="/static/videos/stagger_interrupt.mov"></source>
  <p>Your browser does not support the video element.</p>
</video>

### Animators

By default, all transitions in Litho run using a spring with a default configuration. To provide a different animator, or to tune the parameters of this spring, use the `.animator` builder setting when creating your transition:

```java
Transition.create('red')
    .animate(AnimatedProperties.X)
    .animator(Transition.springWithConfig(10, 100))
```

<br />
Springs provide natural movement and can be easily interrupted but you may need a timing-based animation. In this case, you can use `Transition.timing()` as an animator. `Transition.timing()` also supports taking a standard [Android interpolator](https://developer.android.com/reference/android/view/animation/Interpolator.html) to interpolate a timing animation.

## Sample Code

You can find a transitions demo if you install the litho sample app via `./gradlew :sample:installDebug`. Code for these examples can be found [on Github](https://github.com/facebook/litho/tree/master/sample/src/main/java/com/facebook/samples/litho/transitionsdemo).
