---
docid: prop-matching
title: Prop Matching
layout: docs
permalink: /docs/prop-matching.html
---

> IMPORTANT: These APIs are currently considered experimental and subject to
  change. If you want to play safe, stick with the APIs documented under [Unit
  Testing](/docs/unit-testing).

We have already learned about matching [sub-components in the
hierarchy](/docs/subcomponent-testing). In this article, we will drill a bit
deeper and explore TestSpecs as a way to test individual props of those
components, even if we don't know all of them.

Not a fan of many words? Jump to the [TL;DR](#tldr) for just some sample code.

## Prerequisites

The package is shipped as a separate module. It is available via maven as
`com.facebook.litho:litho-testing`. To include it in your gradle build, add this
line to your `dependencies` block:

```groovy
testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
```

## Complex Components

Composability is one of the big strengths Litho has to offer. It allows you to
encapsulate your logic in small components and compose them together
effortlessly into larger ones. But despite all good efforts, sometimes there is
no clear dividing line and your component may grow beyond its original scope.

Having more complex components shouldn't prevent you from using them
confidently. That's why we have a set of powerful APIs to test your components
no matter the size.

For this example, let's consider this LayoutSpec:

```java
@LayoutSpec
public class ComplexComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop StoryProps<ComplexAttachment> storyProps,
      @Prop ImageRequest imageRequest,
      @Prop DraweeController draweeController,
      @Prop String title,
      @Prop(resType = ResType.DIMEN_TEXT) int titleTextSize,
      @Prop int visiblePhotoCount,
      @Prop(optional = true) Artist favoriteArtist,
      @Prop(optional = true) boolean shouldHavePuppies) {
    return Row.create(c).build();
  }
}
```

## Testing a Complex Component

When we look at the props of our `ComplexComponent`, we see a lot of opaque
objects that we may have trouble getting ahold of for our tests. `StoryProps`
might be something we obtain through some dependency injection mechanism. A
`DraweeController` is an implementation detail we shouldn't have to worry about
for ensuring that the component tree has the right shape.

However, if you remember the `SubComponent.of` API, we need to specify all
non-optional props for it to succeed. Let's see what this would look like:

```java
@RunWith(ComponentsTestRunner.class)
public class FeedWithComplexItemsTest {
  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();
```

<img src="/static/images/complex-component-0.svg" style="float: right; width: 200px;" />

As always, we create a standard JUnit test suite and run it with a
`RobolectricTestRunner`-compatible implementation like `ComponentsTestRunner`.

For the purpose of this article, we assume that we have a `FeedItemComponent`
that contains our `ComplexComponent`. The `FeedItemComponent` contains the logic
for populating our complex props which we want to verify.

<div style="clear: right; margin-bottom: 1em;"></div>

```java
@Test
public void testComplexSubComponent() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<FeedItemComponent> component = makeComponent("Two Brothers");

  assertThat(c, component)
      .has(
          subComponentWith(
              c, legacySubComponent(SubComponent.of(
                  // ERROR: This fails at runtime as we haven't provided all
                  // required parameters.
                  ComplexComponent.create(c)
                      .title("Two Brothers")
                      .build()
              ))));
}

```

Sadly, this test fails with this error message:

```
java.lang.IllegalStateException: The following props are not marked as optional
and were not supplied: [storyProps, imageRequest, draweeController,
titleTextSize, visiblePhotoCount]
```

But what if we can't provide these props in our tests? Or if we don't
want to test implementation details like the image loading controller?

We could simply choose not to test any props at all and decide to verify only
the presence of our component.

```java
@Test
public void testComplexSpecIsPresent() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<FeedItemComponent> component = makeComponent("Rixty Minutes");

  assertThat(c, component)
      .has(
          subComponentWith(
              c, typeIs(ComplexComponent.class)));
}
```

Clearly, having this test is better than nothing. In the same way that
having some Starbucks coffee after a cross-Atlantic flight is better than no
coffee at all ... but I digress.

What if there was a way to match just *some* of our props?

## Partial Props Matching

TestSpecs allow you to match against exactly those props that you choose to
test. Just as LayoutSpecs and MountSpecs, TestSpecs make use of the powerful
annotation processing mechanism Java offers and generate code for you.

We start by creating a new class as part of our testing project and link to the
original spec we want to generate our TestSpec for.

```java
@TestSpec(ComplexComponentSpec.class)
public interface TestComplexComponentSpec {}
```

There are a few things to note here:

- The class you reference in `@TestSpec` must be a LayoutSpec or MountSpec.
- You must link to the Spec and not the generated component, e.g.
  `ComplexComponentSpec.class` not `ComplexComponent.class`.
- In contrast to other specs, TestSpecs are generated from an interface, not a
  class.
- The interface must be empty, i.e. cannot have any members.
- By convention, you prefix your TestSpec with `Test`, followed by the original
  spec name.

And that's it. Those two lines are enough to generate us a powerful matcher that
we can use in our tests.

## Using TestSpecs

Now that we have our TestSpec generated, let's put it into use. Where normal
components have a `create` function, test specs come with a `matcher` function.
It does take the same props as the underlying component but, and this won't come
as a surprise, allows omitting non-optional props.

```java
@Test
public void testComplexTestSpecProps() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<FeedItemComponent> component = makeComponent("Two Brothers");

  assertThat(c, component)
      .has(
          subComponentWith(
              c, TestComplexComponent.matcher(c)
                  .shouldHavePuppies(false)
                  .build()));
}
```

Obviously, this outrageous omission of puppies couldn't possibly pass the test
run and will fail with a helpful error message:

```
java.lang.AssertionError: 
Expecting:
 <FeedItemComponent{0, 0 - 100, 100}
  ComplexComponent{0, 0 - 100, 0}
  Column{0, 0 - 100, 50}
    FeedImageComponent{0, 0 - 100, 50}
      RecyclerCollectionComponent{0, 0 - 100, 50}
        Recycler{0, 0 - 100, 0}
    TitleComponent{4, 46 - 16, 46}
      Text{4, 46 - 16, 46 text="Some Name"}
    ActionsComponent{60, 4 - 96, 40}
      FavouriteButton{2, 2 - 34, 34 [clickable]}
  FooterComponent{0, 50 - 100, 66}
    Text{8, 8 - 92, 8 text="Two Brothers"}>
to have:
 <sub component with <Sub-component of type <ComplexComponent> with
 prop <shouldHavePuppies> is <false> (doesn't match true)>>
```

Here we can see a brief overview of the hierarchy we were matching against and
the matcher that failed.

## Advanced Matchers

But wait, there's more! Instead of just matching against partial props, you can
also provide hamcrest matchers in any place that accepts concrete values. For
props that take resource types, you can make use of all the same matchers you
find in regular components.

```
@Test
public void testComplexTestSpecAdvancedProps() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<FeedItemComponent> component =
      makeComponent("Rixty Minutes");

  assertThat(c, component)
      .has(
          subComponentWith(
              c, TestComplexComponent.matcher(c)
                  // titleTextSizeDip, Sp etc. work too!
                  .titleTextSizeRes(R.dimen.notification_subtext_size)
                  .title(containsString("Minutes"))
                  .build()));
}
```

## Matching Matchers

<img src="/static/images/yo_dawg.jpg" style="float: right; width: 200px;" />
There is one type of prop that requires some special treatment: components.
While we could just match against child components via normal equality (and
there is indeed support for this), it is not particularly helpful. We rarely
know what exact instance of a component is passed down to the props and we face
many of the same problems we discussed before: The props of the Component may
not be known in full or perhaps we don't want to provide them all.

<img src="/static/images/complex-component-0.svg" style="float: left; margin-right: 1em; width: 200px;" />
The solution to this is obvious: We match matchers! For any prop that takes a
Component, the TestSpec generates a matcher that takes another matcher. This
allows for **declarative matching against entire trees of components**.

For our example, let's suppose that our `FeedItemComponent` wraps the
`ComplexComponent` in a `Card`.

<div style="clear: right; margin-bottom: 1em;"></div>

```java
@Test
public void testComplexTestSpecProps() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<FeedItemComponent> component = makeComponent("Ricksy Business");

  assertThat(c, component)
      .has(
          subComponentWith(
              c, TestCard.matcher(c)
                  .content(TestComplexComponent.matcher(c)
                      .title(endsWith("Business"))
                      .build()
                  ).build()));
}
```

Notice the `TestCard` we use to declare our hierarchy here. The `litho-testing`
package comes with TestSpecs for all standard Litho widgets.

## A Note on Buck

If you use gradle, this should Just Work™ and shouldn't require any additional
setup.

With Buck or Blaze/Bazel, however, you may need some additional configuration
for the annotation processing step to work.

In order to save you copy-pasting boilerplate all over your project, it is
recommended keep a rule definition like this in a well-known place
(e.g. `//buck_imports/litho_testspec.bzl`). You would obviously have to adjust
the library paths to the corresponding targets in your code base.

```python
"""Provides macros for working with litho testspec."""

def litho_testspec(
    name,
    deps=None,
    annotation_processors=None,
    annotation_processor_deps=None,
    **kwargs
):
    """Litho testspec."""
    deps = deps or []
    annotation_processors = annotation_processors or []
    annotation_processor_deps = annotation_processor_deps or []

    deps.extend(
        [
            "//java/com/facebook/litho:litho",
            "//third-party/android/support/v4:lib-support-v4",
            "//libraries/components/litho-testing/src/main/java/com/facebook/litho/testing:testing",
            "//libraries/components/litho-testing/src/main/java/com/facebook/litho/testing/assertj:assertj",
            "//third-party/java/jsr-305:jsr-305",
            "//third-party/java/hamcrest:hamcrest",
            "//third-party/java/hamcrest:hamcrest-library",
        ]
    )

    annotation_processor_deps.extend(
        [
            "//libraries/components/litho-processor/src/main/java/com/facebook/litho/specmodels/processor:processor-lib"
        ]
    )

    annotation_processors.extend(
        [
            "com.facebook.litho.specmodels.processor.testing.ComponentsTestingProcessor",
        ]
    )

    return android_library(
        name,
        deps=deps,
        annotation_processors=annotation_processors,
        annotation_processor_deps=annotation_processor_deps,
        **kwargs
    )
```

In the definitions for your test suite, you can then create a separate target
for your test specs:

```python
load("//buck_imports:litho_testspec.bzl", "litho_testspec")

litho_testspec(
    name = "testspecs",
    srcs = glob(["*Spec.java"]),
    deps = [
        "//my/library/dependencies",
        # ...
    ],
)

robolectric_test(
    name = "test",
    srcs = glob(["*Test.java*"]),
    deps = [
        ":testspecs",
        # ...
    ]
)
```

This ensures that test specs are processed by the dedicated
`ComponentsTestingProcessor`.

## TL;DR

**Step 1**

Create a TestSpec for your LayoutSpec or MountSpec.

```java
@TestSpec(MyLayoutSpec.class)
public interface TestMyLayoutSpec {}
```

**Step 2**

Use the generated test matcher in your suite.

```java
@Test
public void testComplexTestSpecAdvancedProps() {
  final ComponentContext c = mComponentsRule.getContext();
  final Component<MyWrapperComponent> component = ...;

  assertThat(c, component)
      .has(
          subComponentWith(
              c, TestMyLayout.matcher(c)
                  .titleTextSizeRes(R.dimen.notification_subtext_size)
                  .title(containsString("Minutes"))
                  .child(TestChildComponent.matcher(c).size(greaterThan(5)).build())
                  .build()));
}

```
