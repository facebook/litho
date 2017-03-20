---
docid: props
title: Props
layout: docs
permalink: /docs/props.html
---

Components use an unidirectional data flow with immutable inputs. Following the name established by React, the inputs that a `Component` takes are known as *props*.

## Defining and using Props
The props for a given `Component` are the union of all arguments annotated with `@Prop` in your spec methods. You can access the value of the props in all the methods that declare it as an `@Prop` parameter. 

The same prop can be defined and accessed in multiple lifecycle methods. The annotation processor will ensure you're using consistent prop types and consistent annotation parameters across all the spec methods.

Take the following `Component` spec, for example:

```
@MountSpec
class MyComponentSpec {
  @OnPrepare
  protected static void onPrepare(
      ComponentContext c,
      @Prop(optional = true) String prop1) {
    ...
  }

  @OnMount
  protected static SomeDrawable onMount(
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

```
MyComponent.create(c)
    .prop1("My prop 1")
    .prop2(256)
    .build();
```

## Prop defaults

You can omit setting a value for an optional prop and it will be initialized to the Java default for its defined type. You'll often want to define explicit default values for your component props instead of simply relying on Java's defaults.

You can define default prop values as static members on the Spec class via the `@PropDefault` annotation. Let's define default values for the props described above:

```
@MountSpec
public class MyComponentSpec {
  @PropDefault static final String prop1 = "mydefaultvalue";
  @PropDefault static final int prop2 = -1;

  ...
}
```

## Immutability
The props of a Component are read-only. The Component's parent passes down values for the props when it creates the Component and they cannot change throughout the lifecycle of the Component. If the props values must be updated, the parent has to create a new Component and pass down new values for the props.
The props objects should be made immutable. Due to background layout, props may be accessed on multiple threads. Props immutability ensures that no thread safety issues enter into your component hierarchy.

### More: 
**Resource Types**: TODO link to Resource Types guide
