---
docid: sections-testing
title: Unit Testing Sections
layout: docs
permalink: /docs/sections-testing
---


[SectionComponentTestHelper](/javadoc/com/facebook/litho/testing/sections/SectionComponentTestHelper) provides helper functions to easily test the output of GroupSectionSpecs and state updates.

To demonstrate the testing functionality, consider this simple group section with a a list of text and an optional image header.

```java
/**
 * Dummy {@link GroupSectionSpec} to illustrate how to test sections.
 */
@GroupSectionSpec
public class VerySimpleGroupSectionSpec {

  @OnCreateInitialState
  protected static void onCreateInitialState(
      SectionContext c,
      StateValue<Integer> extra) {
    extra.set(0);
  }

  @OnCreateChildren
  protected static Children onCreateChildren(
      SectionContext c,
      @State int extra,
      @Prop int numberOfDummy) {
    Children.Builder builder = Children.create();

    if (extra > 0) {
      builder.child(SingleComponentSection.create(c)
          .component(Image.create(c).drawable(new ColorDrawable()).build()));
    }

    for (int i = 0; i < numberOfDummy+extra; i++) {
      builder.child(SingleComponentSection.create(c)
          .component(Text.create(c).text("Lol hi " + i).build())
          .key("key" + i)
          .build());
    }
    return builder.build();
  }

  @OnDataBound
  static void onDataBound(
      SectionContext c,
      @Prop int numberOfDummy,
      @State(canUpdateLazily = true) int extra) {
    VerySimpleGroupSection.lazyUpdateExtra(c, extra - numberOfDummy);
  }

  @OnUpdateState
  static void onUpdateState(
      StateValue<Integer> extra,
      @Param int newExtra) {
    extra.set(newExtra);
  }

  @OnEvent(ClickEvent.class)
  static void onImageClick(
      SectionContext c) {
    VerySimpleGroupSection.onUpdateState(3);
  }
}
```

## Testing Children

`SectionComponentTestHelper` helps you test a group section spec's `@OnCreateChildren` method by returning
the children of a section as a list of [SubSection](/javadoc/com/facebook/litho/testing/sections/SubSection). SubSections can either be matched by exact props or by section class.  These are best used as existence checks.

```java

  @Test
  public void testInitialChildren() throws Exception {

    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    List<SubSection> subSections = mTester.getChildren(s);

    assertThat(subSections)
        .isEqualTo(
            ImmutableList.of(
                SubSection.of(
                    SingleComponentSection.create(mTester.getContext())
                        .key("key0")
                        .component(Text.create(mTester.getContext()).text("Lol hi 0"))
                        .build()),
                SubSection.of(SingleComponentSection.class),
                SubSection.of(SingleComponentSection.class),
                SubSection.of(SingleComponentSection.class)));
  }

```


## Testing State

Sometimes your section's behavior is based on both external props and internal state.  You can change state variables and test new behavior by performing state updates on the scoped context.

```java

  @Test
  public void testStateUpdate() throws Exception {

    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    assertThat(mTester.getChildren(s).size()).isEqualTo(4);

    VerySimpleGroupSection.onUpdateState(mTester.getScopedContext(s), 5);

    assertThat(mTester.getChildren(s).size()).isGreaterThan(4);
  }

```


## Testing events

Sections heavily uses the [event handling system](/docs/events-overview) and `SectionComponentTestHelper` has helper methods to make testing events easier. Simply create an event handler using the scoped context and dispatch your event to execute the event handler.

```java

  @Test
  public void testClickHandler() throws Exception {
    Section s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());


    SectionComponentTestHelper.dispatchEvent(
        s,
        VerySimpleGroupSection.onImageClick(mTester.getScopedContext(s)),
        new ClickEvent());

    VerySimpleGroupSection.VerySimpleGroupSectionStateContainerImpl stateContainer =
        mTester.getStateContainer(s);

    assertThat(stateContainer.extra).isEqualTo(3);
  }
```

## Testing other lifecycle methods

Testing other lifecycle methods is as simple as calling the lifecycle method with the section under test and it's corresponding scoped context.

```java
  @Test
  public void testDataBound() throws Exception {
    Section<VerySimpleGroupSection> s = mTester.prepare(
        VerySimpleGroupSection.create(mTester.getContext()).numberOfDummy(4).build());

    s.dataBound(mTester.getScopedContext(s), s);

    VerySimpleGroupSection.VerySimpleGroupSectionStateContainerImpl stateContainer =
        mTester.getStateContainer(s);

    assertThat(stateContainer.extra).isEqualTo(-4);
  }
```

See [VerySimpleGroupSectionSpecTest](https://github.com/facebook/litho/blob/90d4fb176a6209371f58a68d9cd00bb214ffd54e/litho-it/src/test/java/com/facebook/litho/sections/common/VerySimpleGroupSectionSpecTest.java) for the full source of this example!  Testing for sections is still very basic so if you run into any missing functionality please [reach out to us](https://github.com/facebook/litho/issues/new) and describe your use case.