---
id: injectprop-matching
title: Testing InjectProps
---

`@InjectProp` is a pluggable mechanism for letting your dependency injection
container take care of providing the props to your components. Testing them can
be achieved with TestSpecs, which we learned about in the [Prop
Matching](prop-matching) section.

## Prerequisites

The package is shipped as a separate module. It is available via maven as
`com.facebook.litho:litho-testing`. To include it in your gradle build, add this
line to your `dependencies` block:

```groovy
testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
```

## A DI-Enabled Component

A component spec using `@InjectProp` looks just like any other spec and can
contain a mix of normal props, state and injected props.


```java
@LayoutSpec
class MyInjectPropSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop String normalProp,
      @InjectProp UserController injectedProp,
      @InjectProp ProfilePictureComponent profilePicture) {
    // ...
  }
}
```

One difference between regular and injected props after generation is that there
is no builder method for setting any injected props as they are provided by the
DI container.

## Testing a injected props

When testing a hierarchy containing a component containing an injected
component, we want to ensure that the props set on it match our expectations.
TestSpecs, as opposed to regular component specs, generate methods to match
against props for both regular and injected props. 

We generate our TestSpec as always:

```java
@TestSpec(MyInjectPropSpec.class)
public interface TestMyInjectPropSpec {}
```

And can write our test once this is done.

```java
@RunWith(LithoTestRunner.class)
public class InjectPropMatcherGenerationTest {
  @Rule public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setUp() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Test
  public void testInjectPropMatching() {
    final ComponentContext c = mComponentsRule.getContext();
    final MyInjectProp component =
        new MyInjectProp(new MyInjectPropSpec())
          .create(c)
          .normalString("normal string")
          .build();

    // This would normally be done by your DI container.
    component.injectedString = "injected string";
    component.injectedKettle = new Kettle(92f);

    final Condition<InspectableComponent> matcher =
        TestMyInjectProp.matcher(c)
            .normalString("normal string")
            .injectedString("injected string")
            .injectedKettle(new CustomTypeSafeMatcher<MyInjectPropSpec.Kettle>(
                "matches temperature") {
              @Override
              protected boolean matchesSafely(MyInjectPropSpec.Kettle item) {
                return Math.abs(item.temperatureCelsius - 92f) < 0.1;
              }
            })
            .build();

    assertThat(c, component).has(deepSubComponentWith(c, matcher));
  }
}
```

In the example you can see how we test against normal and injected props in just
the same way. The Kettle object is instantiated with a temperature, that we can
verify using a custom matcher.

## Testing injected components

A fairly common type to inject is indeed other Components. Let's consider our
spec from before with an injected `Text` component instead:

```java
@OnCreateLayout
static Component onCreateLayout(
    ComponentContext c,
    @InjectProp Text injectedComponent) {
  return Column.create(c).child(injectedComponent).build();
}
```

While `Text` is clearly too broad of a component to be a reasonable target for
DI, it'll do just fine for this example.

When testing that the injected component behaves as expected, we can use the
previously explored pattern of passing in another TestSpec matcher:

```java
@Test
public void testInjectPropMatching() {
  final ComponentContext c = mComponentsRule.getContext();
  final MyInjectProp component =
      new MyInjectProp(new MyInjectPropSpec()).create(c).build();
  component.injectedComponent = Text.create(c).text("injected text").build();

  final Condition<InspectableComponent> matcher =
      TestMyInjectProp.matcher(c)
          .injectedComponent(TestText.matcher(c).text("injected text").build())
          .build();

  assertThat(c, component).has(deepSubComponentWith(c, matcher));
}
```

As you can see, the appropriate matchers are generated for you even if the
type of the prop is a concrete instance of a component rather than the generic
`Component` type.

## Next

Either head back to the [testing overview](testing-overview) or
continue with the next section, [Testing Event Handlers](event-handler-testing).
