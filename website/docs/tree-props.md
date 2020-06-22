---
id: tree-props
title: TreeProps
---
import useBaseUrl from '@docusaurus/useBaseUrl';

A TreeProp is a special type of [prop](/docs/props) which transparently passed
from a parent component to its children. It provides a convenient way to share
contextual data or utilities in a tree without having to explicitly pass `@Prop`
to every component in your hierarchy.

A good candidate, for example, is a prefetcher which fetches network images
ahead of render time. The prefetcher is widely used since images are common. The
prefetcher implementation might be something we define for any Component that
needs to use it without having to pass it as `@Prop` in the entire tree.

## Declaring a TreeProp

Each TreeProp is declared and created from a method annotated with `@OnCreateTreeProp`.

```java
@LayoutSpec
public class ParentComponentSpec {
  @OnCreateTreeProp
  static Prefetcher onCreatePrefetcher(
      ComponentContext c,
      @Prop Prefetcher prefetcher) {

    return prefetcher;
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Uri imageUri) {

    return ChildComponent.create(c)
        .imageUri(imageUri)
        .build();
  }
}
```

You can only declare one TreeProp for any one given type. If a child of ParentComponent also defines a TreeProp of type Prefetcher, it will override the value of that TreeProp for all its children (but not for itself).

## Using a TreeProp

The child component can access the TreeProp value through a param annotated with TreeProp that has the same type as that which was declared in the parents @OnCreateTreeProp method.

``` java
@LayoutSpec
class ChildComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @TreeProp Prefetcher prefetcher,
      @Prop Uri imageUri) {
    if (prefetcher != null) {
      prefetcher.prefetch(imageUri);
    }
    // ...
  }
}
```

> IMPORTANT: Once created, the TreeProp value will be passed down to all children, but will not be accessible from the component that created this TreeProp.

If you want to access a TreeProp from the component that created this TreeProp, you can transform it into [`@State`](https://fblitho.com/docs/state) value like this:

```java
@LayoutSpec
public class ParentComponentSpec {

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<ImportantHelper> helper) {

    helper.set(new ImportantHelper());
  }

  @OnCreateTreeProp
  static ImportantHelper onCreateHelper(
      ComponentContext c,
      @State ImportantHelper helper) {

    return helper;
  }
```

And now `ImportantHelper` instance is accessible as `@State` as usual:

```java
@OnCreateLayout
static Component onCreateLayout(
    ComponentContext c,
    @State ImportantHelper helper) {

	//...
}
```

## TreeProps and Sections

TreeProps can be used in both Components and Sections and even shared and
modified between them. Let's consider the example of a logging datastructure we
pass down from the root component to capture information about the hierarchy.

```java
public class LogContext {
  public final String s;

  public LogContext(String s) {
    this.s = s;
  }

  public static LogContext append(@Nullable LogContext t, String s) {
    if (t == null) {
      return new LogContext(s);
    }
    return new LogContext(t.s + ":" + s);
  }

  public String toString() {
    return s;
  }
}

```

Immutable TreeProps are easier to reason about, so try to follow that design
pattern whenever possible.

We now create a component hierarchy that looks like this:

<img src={useBaseUrl("/static/images/treeprop-sections.png")} />

We start by setting up the RootComponent and the RecyclerComponent sitting
inside:

```java
@LayoutSpec
public class RootComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .child(LeafComponent.create(c))
        .child(
            RecyclerCollectionComponent.create(c)
                .section(TopGroupSection.create(new SectionContext(c)).build())
                .flexGrow(1f)
                .build())
        .build();
  }

  @OnCreateTreeProp
  static LogContext onCreateTestTreeProp(ComponentContext c) {
    return new LogContext("root");
  }
}
```

The TopGroupSection takes in the root TreeProp and adds its "top" tag to it.

```java
@GroupSectionSpec
public class TopGroupSectionSpec {

  @OnCreateChildren
  protected static Children onCreateChildren(SectionContext c) {
    return Children.create()
        .child(
            BottomGroupSection.create(c).build()
        )
        .child(
            SingleComponentSection.create(c).component(LeafComponent.create(c))
        )
        .build();
  }

  @OnCreateTreeProp
  static LogContext onCreateTestTreeProp(SectionContext c, @TreeProp LogContext t) {
    return LogContext.append(t, "top");
  }
}
```

We're omitting the bottom part here for brevity, but you can find it in the
repository under [instrumentation-tests](https://github.com/facebook/litho/tree/master/litho-instrumentation-tests/src/main/java/com/facebook/litho/sections/treeprops).

The leaf node simply renders the TreeProp as text in our example case here, but
would normally perform some sort of logging based on the context.

```java
@LayoutSpec
public class LeafComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @TreeProp LogContext treeProp) {
    return Text.create(c)
        .text(LogContext.append(treeProp, "leaf").toString())
        .textSizeDip(24)
        .build();
  }
}
```

The result on screen will be three rows of text that read

- `"root:leaf"`
- `"root:top:leaf"`
- `"root:top:bottom:leaf"`

This illustrates how TreeProps propagate through both component and section
trees and can be used to selectively share information with their children.
