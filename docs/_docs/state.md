---
docid: state
title: State
layout: docs
permalink: /docs/state
---

A Litho component can also contain two types of data:

*  **props**: passed down from parent and cannot change during a component's lifecycle.
*  **state**: encapsulates implementation details that are managed by the component and is transparent to the parent.

A common example for when State is needed is rendering a checkbox. The component renders different drawables for the checked and unchecked states, but this is an internal detail of the checkbox component that the parent doesn't need to be aware of.

## Declaring a Component State
You can define a State on a Component by using the @State annotation in the spec lifecycle methods, similarly to how you would define a Prop.

Defining state elements is enabled on the lifecycle methods of Layout Specs and Mount Specs.

```java
@LayoutSpec
public class CheckboxSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State boolean isChecked) {

    return Column.create(c)
        .child(Image.create(c)
            .drawableRes(isChecked
                ? R.drawable.is_checked
                : R.drawable.is_unchecked))
        .child(Text.create(c)
            .text("Submit")
            .clickHandler(Checkbox.onClickedText(c))
        .build;
  }

  @OnEvent(ClickEvent.class)
  static void onClickedText(
      ComponentContext c,
      @State boolean isChecked) {
    ...
  }
}
```

## Initializing a State value
To set an initial value for a state, you have to write a method annotated with `@OnCreateInitialState` in your spec.

This is what you need to know when writing an `@OnCreateInitialState` method:

* First parameter must be of type `ComponentContext`.
* `@Prop` parameters are allowed.
* All other parameters must have a corresponding parameter annotated with `@State` in the other lifecycle methods, and their type must be a [StateValue](/javadoc/com/facebook/litho/StateValue) that is parameterized with the type of the matching `@State` element.
* `@OnCreateInitialState` methods are not mandatory. If you do not define one or if you only initialize some states, the uninitialized ones will take Java defaults.
* `@OnCreateInitialState` is called only once for each component, when it first gets added to the `ComponentTree`. Following layout recalculations of the same `ComponentTree` will not call this again if the key of the component doesn't change.
* You should never need to call `@OnCreateInitialState` yourself.

Here's how you would initialize the checkbox state with a value passed down from the parent:

```java
@LayoutSpec
public class CheckboxSpec {

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<Boolean> isChecked,
      @Prop boolean initChecked) {

    isChecked.set(initChecked);
  }
}
```

## Defining State Updates
You can define how a component's state or states should be updated by declaring methods annotated with `@OnUpdateState` in the specs.

You can have as many `@OnUpdateState` methods as you need, according to what states you want to update or what parameters your states depend on.

Each call to an `@OnUpdateState` method will trigger a new layout calculation for its ComponentTree. For better performance, if there are situations that can trigger an update for multiple states, you should define an `@OnUpdateMethod` that updates the value for all those states. Bundling them in the same update call reduces the number of new layout calculations and improves performance.

This is what you need to know when writing an `@OnUpdateState`  method:

* Parameters representing the states must match the name of a parameter annotated with @State and their type must be a StateValue parameterized with the type of the matching @State.
* `@Param` parameters are allowed. If the value of your state depends on props, you can declare them like this and pass the value of the prop when the update call is triggered.
* All other parameters must have a corresponding parameter annotated with `@State` in the other lifecycle methods, and their type must be a `StateValue` parameterized with the type of the matching `@State` element.

Here's how you would define a state update method for the checkbox:

```java
@LayoutSpec
public class CheckboxSpec {

  @OnUpdateState
  static void updateCheckboxState(StateValue<Boolean> isChecked) {
    isChecked.set(!isChecked.get());
  }
}
```

If you want to bundle multiple state updates in a single method, you would just add all those states as parameters to the same `@OnUpdateState` method:

```java
@OnUpdateState
static void updateMultipleStates(
    StateValue<Boolean> stateOne,
    StateValue<String> stateTwo,
    @Param int someParam) {

  final boolean thresholdReached = someParam > 100;
  stateOne.set(thresholdReached);
  stateTwo.set(thresholdReached ? "reached" : "not reached");
}

```

## Calling state updates
For each `@OnUpdateState` method in your spec, the generated component will have two methods that will delegate to the `@OnUpdateState` method under the hood:
* a static method with the same name, which will synchronously apply the state updates.
* a static method with the same name and an *Async* suffix, which will asynchronously trigger the state updates.
Both methods take as first parameter a `ComponentContext` followed by all the parameters declared with `@Param` in your `@OnUpdateState` method.

Here's how you would call the state update method to update your checkbox when a user clicks it:

```java
@LayoutSpec
public class CheckboxSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @State boolean isChecked) {

    return Column.create(c)
        .child(Image.create(c)
        .drawableRes(isChecked
            ? R.drawable.is_checked
            : R.drawable.is_unchecked))
        .clickHandler(Checkbox.onCheckboxClicked(c)))
    .build;
  }

  @OnUpdateState
  static void updateCheckbox(StateValue<Boolean> isChecked) {
    isChecked.set(!isChecked.get());
  }

  @OnEvent(ClickEvent.class)
  static void onCheckboxClicked(ComponentContext c) {
    Checkbox.updateCheckboxAsync(c);
    // Checkbox.updateCheckbox(c); for a sync update
  }
}
```

This is what you need to keep in mind when calling state update methods:

* When calling a state update method, the `ComponentContext` instance passed as first parameter must always be the one that is passed down as parameter in the lifecycle method in which the update state is triggered. This context contains important information about the currently known state values and it's important for transfering these values from the old components to the new ones during new layout calculations.
* In `LayoutSpecs`, You should avoid calling state update methods in `onCreateLayout`, unless you are absolutely certain they will happen only a deterministic, small number of times.
Every call to a state update method will trigger a new layout calculation on the ComponentTree, which in turn will call `onCreateLayout` on all its components, so it's rather easy to go into an infinite loop. You should consider whether a lazy state update (described below) wouldn't be more appropriate for your use case.
* In `MountSpecs`, you should never call update state methods from `bind` and `mount` methods. If you need to update a state value in those methods, you should instead use a lazy state update, described below.

## Keys and identifying components
The framework sets a key on each component, based on its type and the key of its parent. This key is used to determine which component we want to update when calling a state update and finding this component when traversing the tree.

Components of the same type that have the same parent will be assigned the same key, so we need a way of uniquely identifying them.

Moreover, when a Component's state or props are updated and the `ComponentTree` is recreated, there are situations when components are removed, added or rearranged inside the tree. Because components can be dynamic we need a way of keeping track of the components so we know, even after the `ComponentTree` changes, for which component to apply a state update.

Whenever a key collision is detected in a ComponentTree, which can happen when a parent component created multiple children components of the same type, we assign a unique key on those siblings which depends on the order in which they added to the parent.
However, with the current implementation, there's no easy way for us to detect that a component is the same when the order of the components in your hierarchy changes. This means that the keys that is autogenerated is not stable through component moves. If you expect your components to move around, you have to assign manual keys.

The `Component.Builder` class exposes a .key() method that you can call when creating a component to assign a unique key to it that will be used to identify this component.

You should set this key whenever you have multiple children of the same component with the same type or you expect the content of your layout to be dynamic.
The manual key you set on a component using the `key` prop will always take precedence over the autogenerated one.

The most common case when you must manually define keys for your components is creating and adding them as children in a loop:

```java
@OnCreateLayout
static Component onCreateLayout(
    ComponentContext c,
    @State boolean isChecked) {

  final Component.Builder parent = Column.create(c);
  for (int i = 0; i < 10; i++) {
    parent.child(Text.create(c).key("key" +i));
  }
  return parent.build();
}
```


## Lazy State Updates
For situations where you want to update the value of a `State` but don't need to immediately trigger a new layout calculation, you can use **lazy state updates**. After a lazy state update is called, the component will hold the same value for that state until the next layout calculation is triggered by something else (receiving new props or regular state updates) and the value will be updated. This is useful for updating internal Component information and persisting it between ComponentTree re-layouts when an immediate layout calculation is not needed.

To use lazy state updates, you need to set the `canUpdateLazily` parameter on the `@State` annotation to true.

For a state parameter `foo` marked with `canUpdateLazily`, the framework will generate a static state update method named `lazyUpdateFoo` which takes as parameter a new value that will be set as the new value for foo.

States marked as `canUpdateLazily` can still be used for regular state updates.

Let's look at an example:

```java
@OnCreateLayout
static Component onCreateLayout(
    final ComponentContext c,
    @State(canUpdateLazily = true) String foo) {

  FooComponent.lazyUpdateFoo(context, "updated foo");
  return Column.create(c)
      .child(
          Text.create(c)
              .text(foo))
      .build();
}

@OnCreateInitialState
static void onCreateInitialState(
    ComponentContext c,
    StateValue<String> foo) {
  foo.set("first foo");
}
```

The first time FooComponent is rendered, its child `Text` component will display *first foo*, even if `foo` is lazily updated with another value. When a regular state update or receiving new props will trigger a new layout calculation, the lazy state update will be applied and the `Text` will render *updated foo*.

## Immutability
Because of [background layout](/docs/asynchronous-layout), `State` can be accessed at anytime by multiple threads. To ensure thread safety, `State` objects should be immutable (and if for some rare reason this is not possible, then at least thread safe). The simplest solution is to express your state in terms of primitives since primitives are by definition immutable.
