---
id: transition-choreography
title: Choreography
---
import useBaseUrl from '@docusaurus/useBaseUrl';

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

<video loop="true" autoplay="true" class="video" width="100%" height="500px">
  <source type="video/webm" src={useBaseUrl("/static/videos/transitions/stagger.webm")}></source>
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


### Transition end callback

A listener can be added to receive a callback when an individual transition has ended. This is done through the Litho event dispatcher. See [Events overview](events-overview).
The `TransitionEndEvent` will be called with the transition key and the specific `AnimatedProperty` that has been animated for that key. If multiple `AnimatedProperty`s are added to the same transition, and all of them run at the same time, a callback will be excecuted for each one of those.

```java
@LayoutSpec
public class ThisComponentSpec {
    ...
    @OnEvent(TransitionEndEvent.class)
    static void onTransitionEndEvent(
        ComponentContext c,
        @FromEvent String transitionKey,
        @FromEvent AnimatedProperty property) {
        // Handle transition end here
    }
    @OnCreateTransition
    static Transition onCreateTransition(ComponentContext c) {
        return Transition.stagger(
            100,
            Transition.create("yellow")
                .animate(AnimatedProperties.Y)
                .transitionEndHandler(ThisComponent.onTransitionEndEvent(c)),
            Transition.create("blue")
                .animate(AnimatedProperties.Y)
                .transitionEndHandler(ThisComponent.onTransitionEndEvent(c)),
            Transition.create("purple")
                .animate(AnimatedProperties.Y)
                .transitionEndHandler(ThisComponent.onTransitionEndEvent(c)));
    }
}
```

You can also add the transition end handler to the `Transition.allLayout()` and the same logic applies.

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
    return Transition.allLayout().transitionEndHandler(ThisComponent.onTransitionEndEvent(c));
}
```
