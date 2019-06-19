---
docid: transition-choreography
title: Choreography
layout: docs
permalink: /docs/transition-choreography
---

### Staggers, Sequences, and Parallel Sets

In order to make it possible to implement more complex transition animations, that may involve multiple components, Litho supports creating sets of animations that can run in parallel, in sequence, or on a stagger.

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
    return Transition.stagger(
        100,
        Transition.create("yellow").animate(AnimatedProperties.Y),
        Transition.create("blue").animate(AnimatedProperties.Y),
        Transition.create("purple").animate(AnimatedProperties.Y));
}
```

<video loop autoplay class="video" style="width: 100%; height: 500px;">
  <source type="video/webm" src="/static/videos/transitions/stagger.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>

Sequences and staggers also support interrupting behavior, trying to preserve the guarantee that a component will never jump and will always end up in the correct final position.

### Delays

Additionally, Litho provides `.delay()` API that follows the same pattern.
These sets and delayed transitions can also be nested within each other, e.g. you can have a stagger of parallel animation sets.

```java
@OnCreateTransition
static Transition onCreateTransitionDelayed(ComponentContext c) {
    return Transition.parallel(
        Transition.sequence(
            Transition.create("yellow").animate(AnimatedProperties.Y),
            Transition.create("blue").animate(AnimatedProperties.Y)),
        Transition.delay(3000,
            Transition.create("purple").animate(AnimatedProperties.Y)));
}
```

### Animators

By default, all transitions in Litho run by a spring `Animator` with default configurations.
You can tune the parameters of this spring by creating another `Animator` using `Transition.springWith()` or you can choose to use timing-based `Animator`s that could be created with `Transition.timing()`.
To change the `Animator` use `.animator()` builder setting when creating your transition (*lines 6, 9, 12*):

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
    return Transition.parallel(
        Transition.create("yellow")
            .animate(AnimatedProperties.Y)
            .animator(Transition.springWithConfig(120, 12)),
        Transition.create("blue")
            .animate(AnimatedProperties.Y)
            .animator(Transition.timing(1000)),
        Transition.create("purple")
            .animate(AnimatedProperties.Y)
            .animator(Transition.timing(1000, new BounceInterpolator())));
}
```

<video loop autoplay class="video" style="width: 100%; height: 500px;">
  <source type="video/webm" src="/static/videos/transitions/animators.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>