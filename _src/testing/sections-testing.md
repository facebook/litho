---
id: sections-testing
title: Testing Sections
---

The [SectionsTestHelper](pathname:///javadoc/com/facebook/litho/testing/sections/SectionsTestHelper.html) provides easy-to-use helper functions to test the output of GroupSectionSpecs and state updates.

To demonstrate the testing functionality, consider the following simple group section with a list of text and an optional image header:

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

The `SectionComponentTestHelper` helps you test a group section spec's `@OnCreateChildren` method by returning the children of a section as a list of [SubSection](pathname:///javadoc/com/facebook/litho/testing/sections/SubSection.html):

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

:::tip
`SubSections` can either be matched by exact props or by section class and are best used as existence checks.
:::

## Testing State

Sometimes, a section's behaviour is based on both external props and internal state. You can change state variables and test new behaviour by performing state updates on the scoped context:

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

## Testing Events

Sections, which heavily uses the [event handling system](/codegen/events-for-specs.md), and the `SectionComponentTestHelper`, which has helper methods, both make testing events easier. Simply create an event handler using the scoped context and dispatch your event to execute the event handler:

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

## Testing Other Lifecycle Methods

Testing other lifecycle methods is as simple as calling the lifecycle method with the section under test and its corresponding scoped context:

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

For the full GitHub source of this example, see the [VerySimpleGroupSectionSpecTest](https://github.com/facebook/litho/blob/master/litho-it/src/test/java/com/facebook/litho/sections/common/VerySimpleGroupSectionSpecTest.java).

Testing for sections is still basic so, if you run into any missing functionality, [raise a GitHub Facebook issue](https://github.com/facebook/litho/issues/new) and describe your use case.
