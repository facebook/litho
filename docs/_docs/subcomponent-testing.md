---
docid: subcomponent-testing
title: Sub-Component Testing
layout: docs
permalink: /docs/subcomponent-testing.html
---

> IMPORTANT: These APIs are currently considered experimental and subject to
  change. If you want to play safe, stick with the APIs documented under [Unit
  Testing](/docs/unit-testing).

This document outlines fluid APIs for testing assertions about the
sub-components created by your layout specs. We make heavy use of
[AssertJ](https://joel-costigliola.github.io/assertj/) and
[Hamcrest](http://hamcrest.org/JavaHamcrest/). If you have used the two before,
this should feel very familiar. If you haven't, don't worry. This guide will
tell you everything there is to know.

In a rush? Jump straight to the [Resources](#resources) at the end of this page.
More code, fewer words. Beep boop.

## Prerequisites

The package is shipped as a separate module. It is available via maven as
`com.facebook.litho:litho-testing`. To include it in your gradle build, add this
line to your `dependencies` block:

```groovy
testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
```

## Basic Sub-Component Matching

Suppose you have a simple layout spec that truncates the text passed in
at an arbitrary length and appends an ellipsis.

```java
@LayoutSpec
class TruncatingComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop String text) {
    // A unicode-aware implementation is left as an exercise to the reader.
    final String s = text.length() > 16 ? text.substring(0, 16) + "..." : text;
    return Text.create(c).text(s).build();
  }
}
```

> NOTE: As a quick aside, please note that for trivial components like these it
  is often more appropriate to exploit the fact that these are pure functions
  that can be statically invoked. Whenever possible, test your business logic in
  isolation.
  
Now we want to test that the two cases the component is set out to handle are
correctly implemented. Some people may prefer to write the tests before
implementing the business logic, but technical documentation is not the place to
start religious wars.

```java
@RunWith(ComponentsTestRunner.class)
public class TruncatingComponentSpecTest {
```

We start with telling JUnit to run our test with the `ComponentTestRunner`.
Depending on your project setup, a vanilla `RobolectricTestRunner` may also
suffice. Just ensure that your test runner correctly loads Yoga and the Android
platform libraries.

```java
  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();
```

This JUnit rule serves two main purposes: It a) gives you easy access to a
`ComponentContext` via `getContext()` and b) registers improved representations
for `Component`, `Component.Builder` and `LithoView` in AssertJ error messages.

As next step, we ensure that the tests are executed in debug mode.

```java
  @Before
  public void assumeInDebugMode() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }
```

This set of APIs makes use of features that are only available if the
`IS_INTERNAL_BUILD` flag in `ComponentsConfiguration` is enabled. This is
because we allocate some data structures for testing purposes that are
unnecessary in production builds. This means that this test suite will run for
`:testDebugUnitTest` runs but will be skipped for `:testReleaseUnitTest`.

Now to the actual test! Let's test first that short text (i.e. strings with fewer
than 16 characters) is passed on unmodified.

```java
  @Test
  public void testWorksForShortText() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<TruncatingComponent> component = TruncatingComponent.create(c).text("Mr. Meeseeks").build();

    LithoAssertions.assertThat(c, component)
        .has(subComponentWith(c, textEquals("Mr. Meeseeks")));
```

Let's take a look at this in detail. `LithoAssertions.assertThat` is an
overloaded static method that you can use in any place you would normally use a
standard AssertJ `assertThat`. It provides all the normal bells and whistles but
adds matchers for various Litho-related types. For instance `Component` and
`LithoView`. If you want to assert on a constructed Component, you also need to
pass in the `ComponentContext` that was used to build it.

Next, we call `has()` on the assertion object. This is a standard [AssertJ
method](http://joel-costigliola.github.io/assertj/assertj-core-conditions.html)
and comes with the aliases `is()` and `are()` which can sometimes read nicer.
There are also negated forms: `isNot()`, `doesNotHave` and `areNot()`.

These combinators all take a `Condition` and this is exactly what
`subComponentWith` creates for you. You give it a `ComponentContext` and another
`Condition` and it hands a composition of the two `Conditions` back to you that
will succeed if it matches against at least one of the sub-components created by
your component.

In this case, we use `textEquals` which ensures that there exists a Component
that renders the exact string given to it.

If we're honest, this should be enough for our base case, but let's overdo it a
bit to showcase what AssertJ and Hamcrest allow us to do here:

```java
    LithoAssertions.assertThat(c, component)
        .has(allOf(
            subComponentWith(c, textEquals("Mr. Meeseeks")),
            subComponentWith(c, text(startsWith("Mr."))),
            subComponentWith(c, anyOf(
                text(endsWith("Sanchez")),
                text(containsString("Mees")))
            )
        ));
  }
}
```

If you see something like this in your code review, you would without a doubt
reject it. And you should! But it does a pretty good job of illustrating how
powerful the composition of matchers is.

To learn more about composing AssertJ condition, check out [the official
documentation](http://joel-costigliola.github.io/assertj/assertj-core-conditions.html).

## We have to go deeper

Let's take a slightly more complex LayoutSpec this time. It still has the same
text truncation logic, but adds some styling by wrapping the Text in a Column
and a Card.

```java
@LayoutSpec
public class WrappingComponentSpec {
  @OnCreateLayout
  public static Component onCreateLayout(ComponentContext c, @Prop String text) {
    final String s = text.length() > 16 ? text.substring(0, 16) + "..." : text;
    return Column.create(c)
        .backgroundColor(Color.GRAY)
        .child(Card.create(c).content(Text.create(c).text(s)))
        .build();
  }
}
```

> NOTE: Time for another quick aside to remind you that this is a contrived
  example and it would be way better to keep our previous
  `TruncatingComponentSpec` around and use this LayoutSpec to wrap it. But
  tutorials - and sometimes the real world - can be stinkers.
  
The setup boilerplate will look just like in our previous example.

```java
@RunWith(ComponentsTestRunner.class)
public class WrappingComponentSpecTest {
  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void assumeInDebugMode() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

```

When testing components, we care about the business logic and not the styling of
it, so will focus on sub-component containing the text again. Let's start with a
naïve attempt by just copying our previous test:

```java
  @Test
  public void testWorksForShortText() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<WrappingComponent> component =
        WrappingComponent.create(c).text("Szechuan Sauce").build();

    // Wait! This won't work!
    LithoAssertions.assertThat(c, component).has(subComponentWith(c, textEquals("Szechuan Sauce")));
  }
```

So I've got good and bad news for you. The bad news first: This test fails.
However, the good news is that the error message gives us a good hint as to why:

```
Expecting:
 <WrappingComponent{0, 0 - 100, 100}
  Card{0, 0 - 100, 6}
    Column{3, 2 - 97, 2}
      Text{0, 0 - 94, 0 text="Szechuan Sauce"}
      CardClip{0, 0 - 94, 0}
    CardShadow{0, 0 - 100, 6}>
to have:
 <sub component with <text <is "Szechuan Sauce">>>
```

As you can see from the output, the "Text" we're looking for
sits nested in the hierarchy. This means that our `subComponentWith` matcher
won't work here.

Luckily enough, there is another matcher just for this case:
`deepSubComponentWith`. So, let's just replace it in our assertion:

```java
  @Test
  public void testWorksForShortText() {
    final ComponentContext c = mComponentsRule.getContext();
    final Component<WrappingComponent> component =
        WrappingComponent.create(c).text("Szechuan Sauce").build();

    LithoAssertions.assertThat(c, component)
        .has(deepSubComponentWith(c, textEquals("Szechuan Sauce")));
  }
}
```

And boom! Our build is back to green. Everything else remains exactly the same.
We can still use various combinators as well as mix and match with `allOf()` and
`anyOf` if we so desire.

## DIY Conditions

If you look at
[ComponentConditions](/javadoc/com/facebook/litho/testing/assertj/ComponentConditions.html)
you can find a growing collection of matchers that can be used in
`subComponentWith` and `deepSubComponentWith`. But don't let the existing list
limit your creativity! Writing your own conditions is a piece of cake.

All you need to do to write your own condition is implement the
`Condition<InspectableComponent>` interface which consists of a single method:
`matches(InspectableComponent)`.

[InspectableComponent](/javadoc/com/facebook/litho/testing/subcomponents/InspectableComponent.html)
is a wrapper around a `Component` with additional information we have about the
component at test time.

Let's implement a custom matcher that asserts that a given Component has a
background:

```java
public static Condition<InspectableComponent> anyBackground() {
  return new Condition<InspectableComponent>() {
    @Override
    public boolean matches(InspectableComponent value) {
      as("any background");
      return value.getBackground() != null;
    }
  };
}

```

Nothing here should come as a surprise, but it may be worth noting that the `as`
invocation instructs AssertJ to record the use of this condition in a potential
error message as you have seen it above.

If we want to put this to use, we use it in the same place we previously made
our text assertions:

```java
@Test
public void testHasBackground() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<WrappingComponent> component =
      WrappingComponent.create(c).text("I hate Mumunmunundsdays.").build();

  LithoAssertions.assertThat(c, component)
      .has(deepSubComponentWith(c, anyBackground()));
}
```

## Old Meets New

If you have written unit tests for Litho before, you may have used
[SubComponent](/javadoc/com/facebook/litho/testing/subcomponents/SubComponent.html)
in your tests. The goal if these new fluid combinators is to ultimately replace
the need for `SubComponent` altogether. But until we're there, you can use the
[legacySubComponent](/javadoc/com/facebook/litho/testing/subcomponents/SubComponent.html#legacySubComponent-com.facebook.litho.testing.subcomponents.SubComponent-) matcher.

It accepts an existing `SubComponent` and works with both shallow and deep
sub-component traversals. This is great if you want to ensure that a property
with a given set of props exists in the component tree.

```java
@Test
public void testSubComponentLegacyBridge() {
  final ComponentContext c = mComponentsRule.getContext();

  assertThat(c, mComponent)
      .has(
          subComponentWith(
              c,
              legacySubComponent(
                  SubComponent.of(
                      FooterComponent.create(c).subtitle("Gubba nub nub doo rah kah").build()))));
}
```

## Resources

To learn more, check out these resources:

- [LithoViewSubComponentExtractor](/javadoc/com/facebook/litho/testing/assertj/LithoViewSubComponentExtractor.html)
- [LithoViewSubComponentDeepExtractor](/javadoc/com/facebook/litho/testing/assertj/LithoViewSubComponentDeepExtractor.html)
- [ComponentConditions](/javadoc/com/facebook/litho/testing/assertj/ComponentConditions.html)
- [AssertJ Conditions](http://joel-costigliola.github.io/assertj/assertj-core-conditions.html)
