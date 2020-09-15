---
docid: testing-treeprops
title: Testing Tree Props
layout: docs
permalink: /docs/testing-treeprops.html
---

## Prerequisites

Add `com.facebook.litho:litho-testing` to the `dependencies` block in the gradle build file.

```groovy
testImplementation 'com.facebook.litho:litho-testing:{{site.litho-version}}'
```

## Testing components with @TreeProp

While `@Prop`s are received from the immediate parent component, a [`@TreeProp`](/docs/tree-props.html) can be passed down to a
components from any of its ancestors in the current component hierarchy.

## Testing a Component with @TreeProp

When testing hierarchies with a component which uses `@TreeProp`s, those tree props should be be
passed down to the components as expected.

```java
@LayoutSpec
class ComponentWithTreePropSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop String normalProp,
      @TreeProp UserContext userContext) { // <- Should be passed down by ancestors.
    // ...
  }
}
```

Use `LithoViewRule#setTreeProp(Class, Object)` to set a `@TreeProp`.

```java
@RunWith(LithoTestRunner.class)
public class ComponentWithTreePropTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Test
  public void test() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Component component = ComponentWithTreeProp.create(c).build();

    mLithoViewRule
      .attachToWindow()
      .setTreeProp(UserContext.class, new UserContext()) // setting tree props for the hierarchy.
      .setRoot(component)
      .measure()
      .layout();

    // test you assertions as usual
  }
}
```

## Next

Either head back to the [testing overview](/docs/testing-overview.html) or
continue with the next section, [Testing InjectProps](/docs/injectprops-matching).
