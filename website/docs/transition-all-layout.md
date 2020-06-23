---
id: transition-all-layout
title: Animating All Layout
---
import useBaseUrl from '@docusaurus/useBaseUrl';

[`Transition.allLayout()`](/javadoc/com/facebook/litho/Transition.html#allLayout--) allows easily animating all layout changes, without having to assign `transitionKey`s to all `Component`s

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
  return Transition.allLayout().animator(ANIMATOR);
}
```

Note, that `Transition.allLayout()`:

1. Targets `AnimatedProperty.X`, `Y`, `HEIGHT` and `WIDTH` of all `Component`s present in layout tree.
2. Could be used to define transitions of [*change* type](transition-types#change-transitions) only. For defining [*appearing*](transition-types#appear-transitions) and [*disappearing*](transition-types#disappear-transitions) transitions, `transitionKey`s still need to be assigned.
3. Could be used as a part of more [complex choreography](transition-choreography).


```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
  return Transition.parallel(
      Transition.allLayout().animator(ANIMATOR),
      Transition.create(TRANSITION_KEY_TEXT)
          .animate(AnimatedProperties.WIDTH)
          .appearFrom(0f)
          .disappearTo(0f)
          .animator(ANIMATOR),
      Transition.create(TRANSITION_KEY_TEXT)
          .animate(AnimatedProperties.ALPHA)
          .appearFrom(0f)
          .disappearTo(0f)
          .animator(ANIMATOR));
}
```

<video loop="true" autoplay="true" class="video" width="100%" height="500px">
  <source type="video/webm" src={useBaseUrl("/static/videos/transitions/alllayout.webm")}></source>
  <p>Your browser does not support the video element.</p>
</video>