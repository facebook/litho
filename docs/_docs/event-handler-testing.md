---
docid: event-handler-testing
title: Event Handler Testing
layout: docs
permalink: /docs/event-handler-testing.html
---

This document provides a quick example of how to write tests for your event
handlers. You should be familiar with [TestSpecs](/docs/subcomponent-testing)
before diving into this topic.

## Prerequisites

The package is shipped as a separate module. It is available via maven as
`com.facebook.litho:litho-testing`. To include it in your gradle build, add this
line to your `dependencies` block:

```groovy
testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
```

## What to test for

We are going to work with this spec in our example:

```java
@LayoutSpec
public class LearningStateComponentSpec {

  @PropDefault static final boolean canClick = true;

  @OnCreateInitialState
  static void onCreateInitialState(
    ComponentContext c,
    StateValue<Integer> count) {

    count.set(0);
  }

  @OnCreateLayout
  static Component onCreateLayout(
    ComponentContext c,
    @Prop(optional = true) boolean canClick,
    @State Integer count) {

    return Text.create(c)
        .text("Clicked " + count + " times.")
        .textSizeDip(50)
        .clickHandler(canClick ? LearningStateComponent.onClick(c) : null)
        .backgroundRes(android.R.color.holo_blue_light)
        .alignSelf(STRETCH)
        .paddingDip(BOTTOM, 20)
        .paddingDip(TOP, 40)
        .build();
  }

  @OnUpdateState
  static void incrementClickCount(StateValue<Integer> count) {
    count.set(count.get() + 1);
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    LearningStateComponent.incrementClickCount(c);
  }
}
```

<video loop autoplay class="video" style="float: right; width: 300px;">
  <source type="video/mp4" src="/static/videos/state_taps.mp4"></source>
</video>

When testing event handlers, it is important to remember what you actually want
to validate in your unit test. You may be getting this inkling to ensure that a
click event you issue triggers the callback you pass in as your prop. *When you
do this, you are actually testing the framework.* This is not what you want to
spend your time on. While writing high-level end-to-end tests ensuring that your
touch events have the right effects, this is not what you should concern
yourself with for unit tests.

<div style="clear:both;"></div>

## Testing handler presence

Instead, let's focus on the actual logic that we have in our spec. Whether or
not we have a click handler depends on the prop `canClick`. It is very common
for handlers to be set conditionally based on a prop. In our test, we are going
to limit ourselves to checking if a handler is set or not. For that we can use
the TestSpec matchers we have learned about before.

```java
@RunWith(ComponentsTestRunner.class)
public class LearningStateComponentSpecTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Test
  public void testComponentOnClick() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = LearningStateComponent.create(
        c)
        .canClick(true)
        .build();

    LithoAssertions.assertThat(c, component).has(
        SubComponentExtractor.subComponentWith(
          c,
          TestText.matcher(c)
            .clickHandler(IsNull.<EventHandler<ClickEvent>>notNullValue(null)).build())
    );
  }

  @Test
  public void testNoComponentOnClick() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component component = LearningStateComponent.create(
        c)
        .canClick(false)
        .build();

    LithoAssertions.assertThat(c, component).has(
        SubComponentExtractor.subComponentWith(
          c,
          TestText.matcher(c)
            .clickHandler(IsNull.<EventHandler<ClickEvent>>nullValue(null)).build())
    );
  }
}
```

As you can see here, we can match against a click handler just like any other
prop set on a sub-component. Matching against a specific instance of an
`EventHandler` is currently not supported.

## Testing event handlers

We have now verified that the event handler is properly set, but not what
actually happens when the callback is invoked. How can we do that? Remember that
Component Specs ideally consist of only pure and hence static functions. This
means that you can test every function in isolation. For our example here, we
can directly test the `incrementClickCount` method, completely independent of
its use within the click handler.

```java
@Test
public void testIncrementClickCount() {
  final StateValue<Integer> count = new StateValue<>();
  count.set(0);
  LearningStateComponentSpec.incrementClickCount(count);

  LithoAssertions.assertThat(count).valueEqualTo(1);
}
```

Note how we don't use a component instance or even the generated code from the
spec. You can also see how the `StateValue` can be compared directly with
`LithoAssertions.assertThat`, which has a special method for peeking inside the
container to compare values.

## Next

Either head back to the [testing overview](/docs/testing-overview.html) or
continue with the next section, [Espresso](/docs/espresso-testing).
