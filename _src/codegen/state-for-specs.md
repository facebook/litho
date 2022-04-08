---
id: state-for-specs
title: State in Specs
---

:::note
This page covers the old Java Spec API. If you aren't using the Spec API, please refer to [Introduction to Hooks](mainconcepts/hooks-intro.mdx).
:::

This page introduces the concept of state in a component written using the Java Spec API, namely a `@LayoutSpec` or `@MountSpec`.

A component can have two types of data:

* **Props** - passed down from the parent and cannot change during a component's lifecycle.
* **State** - State data is encapsulated and managed within the component and is transparent to the parent.

This page uses the example of a `Counter` component, in which you can click a button to increase or decrease a value. The example helps you to learn how to use State to make the `Counter` component reusable and encapsulated; it provides an overview of adding state to a component.

You can start by encapsulating how the `Counter` looks:

```java
@LayoutSpec
class CounterComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop int count) {
    return Row.create(c)
        .child(Text.create(c).text("+"))
        .child(Text.create(c).text("" + count))
        .child(Text.create(c).text("-"))
        .build();
  }
}
```

The `Counter` component is missing a crucial feature, which is interacting with the buttons to update the count value.

Ideally, you'd want this component to encapsulate all this behaviour in its internal implementation, which would mean you'd write it once then reuse it anywhere you need a counter, as follows:

```java
final CounterComponent counter = CounterComponent.create().build();
```

To implement this, you need to add 'state' to the `Counter` component.

## Adding Local State to a Component

You can change the `counter` from prop to state in three steps:

### 1. Replace the `counter` prop declaration with a state declaration

```java
@LayoutSpec
class CounterComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int count) {
    return Row.create(c)
        //...
        .build();
  }
}
```

### 2. Set an initial value for the `count` state

```java
@OnCreateInitialState
static void onCreateInitialState(ComponentContext c, StateValue<Integer> count) {
  count.set(1);
}
```

### 3. Use the state value

```java
@OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int count) {
    return Row.create(c)
        .child(Text.create(c).text("+"))
        .child(Text.create(c).text("" + count))
        .child(Text.create(c).text("-"))
        .build();
  }
```

## Updating State

Next, you'll make the Counter component update the count value when the increase or decrease buttons are clicked, in two steps:

### 1. Set click handlers on the buttons

```java
@OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int count) {
    return Row.create(c)
        .child(Text.create(c).text("+").clickHandler(CounterComponent.onClickIncrease(c)))
        .child(Text.create(c).text("" + count))
        .child(Text.create(c).text("-").clickHandler(CounterComponent.onClickDecrease(c)))
        .build();
  }

@OnEvent(ClickEvent.class)
static void onClickIncrease(ComponentContext c) {}

@OnEvent(ClickEvent.class)
static void onClickDecrease(ComponentContext c) {}
```

### 2. Update the state value in the click handlers

```java file=sample/src/main/java/com/facebook/samples/litho/java/identity/CounterComponentSpec.java start=start_counter end=end_counter
```

## State API Reference and Considerations

### Data Immutability

Due to background layout, state can be accessed anytime by multiple threads. To ensure thread safety, state objects should be immutable (if for some rare reason this is not possible, then at least thread safe). The simplest solution is to express your state in terms of primitives since primitives are, by definition, immutable.

### Component Identity

Litho uses keys to keep track of component identity between layout changes and correctly identify a component as the target of a state update. For more information on how the component identity functions, see the [Keys and Component Identity](../mainconcepts/coordinate-state-actions/keys-and-identity.md) page.

### Initialising State Values

State initialisation is guaranteed to happen once and only once for a component based on its [identity](../mainconcepts/coordinate-state-actions/keys-and-identity.md), even if there are multiple threads attempting to calculate the layout for the same component in parallel.

In the Java API, the method annotated with `@OnCreateInitialState` is guaranteed to be called just once during a component's lifecycle.

This is an important consideration that you should keep in mind when you use prop values to initialize state. Passing new props to a component does not call the initializer again; a state value can only be updated after it was initialized by using the [state update APIs](state-for-specs.md#updating-state).

In the Java API, to set an initial value for a state, you have to write a method annotated with `@OnCreateInitialState` in your spec.
The following are points to keep in mind when writing an `@OnCreateInitialState` method:

* The first parameter must be of type `ComponentContext`.
* `@Prop` parameters are allowed, but `@OnCreateInitialState` methods are not called again if the props change.
* All other parameters must have a corresponding parameter annotated with `@State` in the other lifecycle methods, and their type must be a [StateValue](pathname:///javadoc/com/facebook/litho/StateValue.html) that is parameterized with the type of the matching `@State` element.
* Initializing a state value is not mandatory and implementing an `@OnCreateInitialState` method can be entirely skipped. If a state value is not explicitly initialised, the initial state is assigned the default value of its inferred type. For example, `0` for integer state, `false` for Boolean state or `null` for Objects.
* You should never need to call the `@OnCreateInitialState` method yourself.

### Updating State Values

Every state update triggers a new layout calculation for its `ComponentTree`. Passing new props to a Litho component and updating the state are implemented in the same way in Litho, so there is no performance difference. To understand when you should update a Component using new props or updating state, see the [Props vs. State](../best-practices/props-vs-state.md) page.

However, Litho implements a feature called reconciliation, which attempts to detect what part of the `ComponentTree` is affected by that state update and reuse the layout for the nodes that don't need to change.
State updates can be performed synchronously on the same thread that they were triggered from, or asynchronously from Litho's background thread.

The following points should be kept in mind when updating a state value:

* Avoid calling state update methods in the `@OnCreateLayout` method of a component. Every state update method triggers a new layout calculation, which re-invokes the render method of the component that triggered the state update. This can easily lead to an infinite loop. You should consider whether a [lazy state](#lazy-state) update would be more appropriate for your use case, and only use state updates in a render method if you're absolutely certain that the state update is conditionally called and can only be triggered a limited number of times.
* In [MountSpecs](../codegen/mount-specs.md), state updates are not allowed in `bind` and `mount` methods as they cause a runtime exception if used. If you need to update a state value in those methods, you should instead use a [lazy state](#lazy-state) update.

:::note
In the Java API, you can define how a component's state or states should be updated by declaring methods annotated with `@OnUpdateState` in the specs. You can have as many `@OnUpdateState` methods as you need, according to what states you want to update or what parameters your states depend on.

The following points should be considered when writing an `@OnUpdateState` method:

* Parameters representing the state values must match the name of a parameter annotated with `@State` and used in other lifecycle methods, and their type must be a `StateValue` parameterized with the type of the matching `@State`.
* `@Prop`s are not allowed, but `@Param` parameters are. If the value of your state depends on props, you can pass them as `@Param` params from the lifecycle methods that call the state update methods. For each `@OnUpdateState` method in your spec, the generated component has two methods that delegate to the `@OnUpdateStUpdate the state value in the click handlerate` method under the hood:
* A static method with the same name, which asynchronously applies the state updates.
* A static method with the same name and a *Sync* suffix, which synchronously triggers the state updates. Both methods take a `ComponentContext` as first parameter, followed by all the parameters declared with `@Param` in your `@OnUpdateState` method.

## Lazy State

For situations where you want to update the value of a state but don't need to immediately trigger a new layout calculation, you can use **lazy state updates**. After a lazy state update, the new state value is visible in event handlers, but a new layout is not triggered.
Currently, the value is immediately visible to the event handler but **not** visible to other lifecycle callbacks (such as `onMount`).

Lazy state is useful for updating state values that don't need to be reflected in the UI. For example, say you want to log an analytics event only the first time a component becomes visible. If you use lazy state, you can record whether a log was sent in a lazy state variable without causing the UI to reflow.

Lazy state can still be used for regular state updates.

```java file=sample/src/main/java/com/facebook/samples/litho/java/identity/IdentityRootComponentSpec.java start=start_lazy_state end=end_lazy_state
```
