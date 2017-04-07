---
docid: unit-testing
title: Unit Testing
layout: docs
permalink: /docs/unit-testing.html
---

The Components framework provides testing helpers exposed through fluid
AssertJ methods. They are available as:

- `ComponentAssert` for assertions that are run against either Component builders
  or Components.
- `ComponentViewAssert` for assertions against mounted Components.

Under the hood, these asserts are implemented through these helpers,
that are also available in case that more complicated use cases need
to be reconstructed:

- `ComponentTestHelper`: Allows simple and short creation of views that are
  created and mounted in a similar way to how they are in real apps.
- `ComponentQueries`: Utility class to query the state of components.

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
public class LikersComponentSpec {

  @OnCreateLayout
  protected static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop List<User> likers) {
    return Container.create(c)
        .direction(ROW)
        .alignItems(FLEX_START)
        .child(
            Image.create(c)
                .srcRes(R.drawable.like))
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

The Components testing framework provides a JUnit
[`@Rule`](https://github.com/junit-team/junit4/wiki/Rules)]] which
sets up overrides for
[Styleables](https://developer.android.com/reference/android/R.styleable.html)]]
and allows easy access to a `ComponentContext`.

```java
/**
 * Tests {@link LikersComponent}
 */
@RunWith(WithTestDefaultsRunner.class)
public class LikersComponentTest {
  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();
```

## Testing Component Rendering
The Components framework includes a set of AssertJ-style helpers for verifying
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

Generally, this should be enough, but if you need more control,
you can also manually mount a Component with `ComponentTestHelper`
and verify the state of it through `ComponentQueries`.

```java
  @Test
  public void testManuallyMountTwoLikers() {
    ComponentContext c = mComponentsRule.getContext();
    ImmutableList<User> likers =
        ImmutableList.of(new User("Jane"), new User("Mike"));

    ComponentView view = ComponentTestHelper
        .mountComponent(
            c,
            LikersComponent.create(c)
                .likers(likers));

    assertThat(ComponentQueries.hasText(view, "Jane, Mike")).isTrue();
  }
}
```

## Testing Sub-Component Rendering
Instead of performing assertions on the content rendered by your Component, it
might be useful to test for the rendering of sub-components instead.
`SubComponent` is a convenience class that allows for easier comparison of Component
types. You can, again, use AssertJ to verify the presence or absence of
the subcomponents.

```java
public class StoryTest {
  ...

  @Test
  public void testStoryLayout() {
    ComponentContext c = mComponentsRule.getContext();
    Story story = ...

    Component<StoryComponent> component =
        StoryComponent.create(c)
            .story(story);

    assertThat(subComponents).hasSubComponents(
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

    assertThat(component)
        .doesNotContainSubComponent(SubComponent.of(LikersComponent.class));
  }
}
```

## Additional Asserts

There are several more assertions available for `Component`s and
`ComponentView`s. They all operate on the tree created by your `Component`.
So asserting the presence of a `Drawable` in your `Component` will traverse
the view hierarchy from the provided starting point.
