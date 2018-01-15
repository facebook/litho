---
docid: unit-testing
title: Unit Testing
layout: docs
permalink: /docs/unit-testing.html
---

Litho provides testing helpers exposed through fluid
AssertJ methods. They are available as:

- [ComponentAssert](/javadoc/com/facebook/litho/testing/assertj/ComponentAssert) for assertions that are run against either Component builders
  or Components.
- [LithoViewAssert](/javadoc/com/facebook/litho/testing/assertj/LithoViewAssert) for assertions against mounted Components.

As a convenience, a [LithoAssertions.assertThat](/javadoc/com/facebook/litho/testing/assertj/LithoAssertions) method is provided that
can be statically imported. It provides access to all matchers provided by `ComponentAssert`, `LithoViewAssert` as well as the
regular core AssertJ matchers:

```java
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
```

In order to use any of the testing capabilities, you need include the optional
`litho-testing` package in your build. It is available as
`com.facebook.litho:litho-testing`. To include it in your gradle build, add this
line to your `dependencies` block:

```groovy
testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
```

To demonstrate the usage of these classes, below is an example of a component
that displays a like icon and a short description.

```java
/**
 * Displays who liked the post.
 *
 * 1 - 3 likers => Comma separated names (e.g. Jane, Mike, Doug)
 * > 3 likers => Comma separated number denoting the like count
 */
@LayoutSpec
class LikersComponentSpec {

  @OnCreateLayout
  protected static Component onCreateLayout(
      ComponentContext c,
      @Prop List<User> likers) {

    return Row.create(c)
        .alignItems(FLEX_START)
        .child(
            Image.create(c)
                 .drawableRes(R.drawable.like))
        .child(
            Text.create(c)
                .text(formatLikers(likers))
                .textSizeSp(12)
                .ellipsize(TruncateAt.END))
        .build();
  }

  private static String formatLikers(List<User> likers) {
    ...
  }
}
```

For our test, we want to verify the rendering of the text and the icon.

## Setup

The Litho testing framework provides a JUnit
[`@Rule`](https://github.com/junit-team/junit4/wiki/Rules) which
sets up overrides for
[Styleables](https://developer.android.com/reference/android/R.styleable.html)
and allows easy access to a `ComponentContext`.

```java
/**
 * Tests {@link LikersComponent}
 */
@RunWith(RobolectricTestRunner.class)
public class LikersComponentTest {
  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();
```

## Testing Component Rendering
The Litho framework includes a set of AssertJ-style helpers for verifying
properties of your Components. Behind the scenes, this will mount the
Component for you.

You can either assert on the pair of `ComponentContext` and `Component`
or on the `ComponentBuilder` before it is consumed by `build()`.

```java
  @Test
  public void testTwoLikers() {
    ComponentContext c = mComponentsRule.getContext();
    ImmutableList<User> likers =
       ImmutableList.of(new User("Jane"), new User("Mike"));

    Component<LikersComponent> component =
       LikersComponent
           .create(c)
           .likers(likers)
           .build();

    assertThat(c, component).hasText("Jane, Mike");
  }

  @Test
  public void testLikeIcon() {
    ComponentContext c = mComponentsRule.getContext();
    Drawable likeIcon = c.getResources().getDrawable(R.drawable.like);

    ImmutableList<User> likers =
        ImmutableList.of(new User("Jane"), new User("Mike"));

    LikersComponent.Builder componentBuilder =
        LikersComponent
            .create(c)
            .likers(likers);

    assertThat(componentBuilder).hasDrawable(likeIcon);
  }
```


## Testing Sub-Component Rendering

Instead of performing assertions on the content rendered by your Component, it
might be useful to test for the rendering of sub-components instead.
[SubComponent](/javadoc/com/facebook/litho/testing/SubComponent) is a convenience class that allows for easier comparison of Component
types. You can, again, use AssertJ to verify the presence or absence of
the sub-Components.

```java
public class StoryTest {
  ...

  @Test
  public void testStoryLayout() {
    ComponentContext c = mComponentsRule.getContext();
    Story story = ...

    StoryComponent.Builder componentBuilder =
        StoryComponent.create(c)
            .story(story);

    assertThat(componentBuilder).hasSubComponents(
        SubComponent.of(HeaderComponent.class),
        SubComponent.of(MessageComponent.class),
        SubComponent.of(LikersComponent.class),
        SubComponent.of(FeedbackComponent.class));
  }

  @Test
  public void testStoryWithZeroLikes() {
    ComponentContext c = mComponentsRule.getContext();
    Story storyWithZeroLikes = ...;

    Component<StoryComponent> component = StoryComponent.create(c)
        .story(storyWithZeroLikes)
        .build();

    assertThat(c, component)
        .doesNotContainSubComponent(SubComponent.of(LikersComponent.class));
  }
}
```

## Additional Asserts

There are several more assertions available for `Component`s and
`LithoView`s. They all operate on the tree created by your `Component`.
So asserting the presence of a `Drawable` in your `Component` will traverse
the view hierarchy from the provided starting point.

## Caveats

When running Litho unit tests, be aware that the native library for Yoga must be loaded
which can pose some challenges depending on your build system of choice. With Gradle and
Robolectric, for instance, you may run into issues as Robolectric spins up new
[ClassLoaders](https://docs.oracle.com/javase/7/docs/api/java/lang/ClassLoader.html)
for every test suite with a different configuration. The same goes for PowerMock, which
prepares the ClassLoaders on a per-suite basis and leaves them in a non-reusable state.

The JVM has two important limitations that are relevant to this:

1. A shared library can only ever be loaded once per process.
2. `ClassLoader`s do not share information about the libraries loaded.

Because of that, using multiple ClassLoaders for test runs is highly problematic
as every instance will attempt to load Yoga and every but the first will fail with
a `libyoga.so already loaded in another classloader` exception.

The only way to avoid this is by either preventing the use of multiple ClassLoaders
or forking the process whenever a new ClassLoader is necessary.

Gradle allows you to limit the number of test classes a process can execute before
it is discarded. If you set the number to one, we avoid the ClassLoader reuse:

```groovy
android {
    [...]

    testOptions {
        unitTests.all {
            forkEvery = 1
            maxParallelForks = Math.ceil(Runtime.runtime.availableProcessors() * 1.5)
        }
    }
}
```

With Buck, this behavior can be achieved by assigning test targets separate names
as those will result in a parallel process being spun up. Alternatively, you can
set the `fork_mode` to `per_test` as described
[here](https://buckbuild.com/rule/java_test.html#fork_mode).

Ultimately, depending on your build system and the existing constraints of your
project, you may need to adjust the way in which your test runner utilizes
ClassLoaders. This is, however, not a problem unique to Litho but an unfortunate
consequence of mixing native and Java code in Android projects.
