---
docid: props
title: Props
layout: docs
permalink: /docs/props
---

Components use a unidirectional data flow with immutable inputs. Following the name established by React, the inputs that a `Component` takes are known as *props*.

## Defining and using Props
The props for a given `Component` are the union of all arguments annotated with `@Prop` in your spec methods. You can access the value of the props in all the methods that declare it as an `@Prop` parameter. 

The same prop can be defined and accessed in multiple lifecycle methods. The annotation processor will ensure you're using consistent prop types and consistent annotation parameters across all the spec methods.

Take the following `Component` spec, for example:

``` java
@MountSpec
class MyComponentSpec {
  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true) String prop1) {
    ...
  }

  @OnMount
  static SomeDrawable onMount(
      ComponentContext c,
      SomeDrawable convertDrawable,
      @Prop(optional = true) String prop1,
      @Prop int prop2) {
    if (prop1 != null) {
    	...
    }
  }
}
```

`MyComponentSpec` defines two props: a *String* prop called `prop1` and an *int* prop named `prop2`. `prop1` is optional and it needs to be marked as such in all the methods that define it, otherwise the annotation processor will throw an exception. 

When the lifecycle methods get called, the `@Prop` parameters will hold the value passed down from the Component's parent when the Component was created (or their default values).

Props are defined and used the same way in `LayoutSpecs` and `MountSpecs`.   

## Setting Props

For each unique prop defined on the spec, the annotation processor creates a builder pattern method on the Component Builder that has the same name as the prop and that takes as only parameter the value to set for that prop. 

You pass down values for these props by calling the appropriate methods on the generated Component Builder:

``` java
MyComponent.create(c)
    .prop1("My prop 1")
    .prop2(256)
    .build();
```

## Prop defaults

You can omit setting a value for an optional prop and it will be initialized to the Java default for its defined type. You'll often want to define explicit default values for your component props instead of simply relying on Java's defaults.

You can define default prop values as static members on the Spec class via the `@PropDefault` annotation. Let's define default values for the props described above:

``` java
@MountSpec
public class MyComponentSpec {
  @PropDefault static final String prop1 = "mydefaultvalue";
  @PropDefault static final int prop2 = -1;

  ...
}
```

## Resource Types
When creating layouts, it's very common to use values from Android's resource system such as dimensions, colors, strings, etc. The Components framework provides convenient ways to set prop values from Android resources using annotations.

Let's consider a simple example:

```java
@LayoutSpec
public class MyComponentSpec {
  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      LayoutContext context,
      @Prop CharSequence someString,
      @Prop int someSize,
      @Prop int someColor) {
    ...
  }
}
```

In the example above, `MyComponent` has props that are expected to take a color integer (`someColor`), a pixel dimension (`someSize`), and a string (`someString`) as value. Very often, you'll want to set the value of these props using resource values:

```java
Resources res = context.getResources();

MyComponent.create(c)
    .someString(res.getString(R.string.my_string))
    .someSize(res.getDimensionPixelSize(R.dimen.my_dimen))
    .someColor(res.getColor(R.color.my_color))
```

The framework allows you to annotate props with resource types so that your component builder has convenience methods to use resource values directly.

```java
@LayoutSpec
public class MyComponentSpec {
  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      LayoutContext context,
      @Prop(resType = ResType.STRING) CharSequence someString,
      @Prop(resType = ResType.DIMEN_SIZE) int someSize,
      @Prop(resType = ResType.COLOR) int someColor) {
    ...
  }
}
```

With the changes above, `MyComponent`'s builder will contain //Res//, //Attr//, //Dip//, and //Px// methods for the annotated props according to their resource types. So you'll be able to do the following:

```java
MyComponent.create(c)
    .someStringRes(R.string.my_string)
    .someSizePx(10)
    .someSizeDip(10)
    .someColorAttr(android.R.attr.textColorTertiary)
```

Other supported resource types are `ResType.STRING_ARRAY`, `ResType.INT`, `ResType.INT_ARRAY`, `ResType.BOOL`, `ResType.COLOR`, `ResType.DIMEN_OFFSET`, `ResType.FLOAT`, and `ResType.DRAWABLE`.

## Immutability
The props of a Component are read-only. The Component's parent passes down values for the props when it creates the Component and they cannot change throughout the lifecycle of the Component. If the props values must be updated, the parent has to create a new Component and pass down new values for the props.
The props objects should be made immutable. Due to background layout, props may be accessed on multiple threads. Props immutability ensures that no thread safety issues enter into your component hierarchy.

### More: 
**Resource Types**: TODO link to Resource Types guide
