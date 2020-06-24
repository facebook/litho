---
id: transition-types
title: Change, Appear and Disappear
---
import useBaseUrl from '@docusaurus/useBaseUrl'

Litho APIs allow you to create animated transitions between two consecutive states of the UI - `LayoutStates`. We'll be addressing these states as *before* and *after* respectively.

When defining transitions you need to indicate to the framework what `Component`/`AnimatedProperty` pairs you want it to animate. Based on presence of the `Component` in *before* and *after* states we can define 3 types of transitions: *changing*, *appearing* and *disappearing*.

### Change Transitions

A transition where the target `Component` is present in both *before* and *after* layouts is called a *change* transition.

The sample from the [Basics section](transition-basics) features a *change* transition, since the `"square"` `Component` is always present in the layout, but it changes its position - `X` property.

It is the simplest type from a user's perspective, so following is the explanation of how change type transitions work internally.

Let's say, layout *A* (*before*) is mounted (rendered on the screen) and we have a blueprint of the layout *B* (*after*), which the framework is ready to render. The transition is defined for a  `AnimatedProperty.P` of a `ComponentC`, where `ComponentC` is present in both layouts.

The framework needs to do the following steps in order to run *change* animations:

1. Pick the current value of the `AnimatedProperty.P` from the mounted UI element (normally, a `View`) that represents `ComponentC` on the screen — this is going to be the *start* value for animation.
2. Pick the animation *end* value of `AnimatedProperty.P` from the blueprint of layout *B*.
3. When the layout *B* is mounted to the screen, we do not apply the new value of `P` right away, as we normally would, instead, we keep the previous value in place, but subscribe to the [`Choreographer`](/javadoc/com/facebook/litho/choreographercompat/ChoreographerCompat.html), and start changing the value of `P` for the new mounted UI element on every frame until it reaches the end value.

### Appear Transitions

In certain situations you may want to animate how a certain UI element appears on screen.

In this case, we are dealing with an *Appearing* transition: a `Component` is present in the *after* `LayoutState`, but wasn't seen in the *before* `LayoutState`. In order to run transitions we need *start* and *end* values of the `AnimatedProperty`, while the framework, obviously, can only get the latter from the layout, thus it is the responsibility of the user to supply the *start* value. You can do this using `.appearFrom()` (*lines 28, 30*) method when building a `Transition`.

```java
@LayoutSpec
class MyComponentSpec {

    @OnCreateLayout
    static Component onCreateLayout(ComponentContext c, @State boolean shown) {
        Component child;
        if (shown) {
            child = Rect.create(c)
                .color(YELLOW)
                .widthDip(80)
                .heightDip(80)
                .transitionKey("square")
                .build();
        } else {
            child = null;
        }

        return Column.create(c)
            .child(child)
            .alignItems(YogaAlign.FLEX_END)
            .build();
    }

    @OnCreateTransition
    static Transition onCreateTransition(ComponentContext c) {
        return Transition.create("square")
            .animate(AnimatedProperties.X)
            .appearFrom(0f)
            .animate(AnimatedProperties.ALPHA)
            .appearFrom(0f);
    }
}
```

<video loop="true" autoplay="true" class="video" width="100%" height="500px">
  <source type="video/webm" src={useBaseUrl("/static/videos/transitions/appear.webm")}></source>
  <p>Your browser does not support the video element.</p>
</video>

Here's how the framework handles these transitions internally:
1. Pick the `appearFrom` value of the `AnimatedProperty.P` from `Transition` object - this is going to be the *start* value for the animation.
2. As before, pick the animation *end* value of `AnimatedProperty.P` from the blueprint of layout *B*.
3. When the layout *B* is mounted to the screen, instead of applying the *end* value from that layout to the mounted item immediately, we set the user-provided *appearFrom* value and drive that to the *end* value from the layout.

### Disappear Transitions

The opposite of *Appearing* type of transitions is *Disappearing*: when the `Component` is present in *before* layout, but is gone in the *after* layout. Following the same logic as before, now the responsibility of providing the *end* value falls on the user, which is done by using `.disappearTo()` (*lines 8, 10*).

Here is how we could add the disappearing animation to our sample:

```java
@OnCreateTransition
static Transition onCreateTransition(ComponentContext c) {
  return Transition.create("square")
      .animate(AnimatedProperties.X)
      .appearFrom(0f)
      .animate(AnimatedProperties.ALPHA)
      .appearFrom(0f)
      .disappearTo(0f)
      .animate(AnimatedProperties.SCALE)
      .disappearTo(0.5f);
}
```

<video loop autoplay class="video" width="100%" height="500px" >
  <source type="video/webm" src="/static/videos/transitions/disappear.webm"></source>
  <p>Your browser does not support the video element.</p>
</video>

Under the hood, in order to run a disappearing animation, the framework follows this algorithm:

1. Pick up the *start* value from the mounted UI element that represents the `Component`
2. Retrieve the *end* from the user-provided `disappearTo` value of transition definition.
3. Render the *after* layout, but instead of removing the UI element from screen right away, drive the value of `AnimatedProperty` to the *end*, and only then remove the UI element.

> It is important to understand that once the layout has been mounted (on step 3) a disappearing `Component` isn't a part of layout tree anymore. However the drawing order of all the UI element is defined by the layout tree. Hence the UI element for the disappearing `Component` will be drawn the last. In some rare cases (normally involving cross-fading elements), this may result in the rendering being different from what you would expect. In such cases, a change animation should be used instead.