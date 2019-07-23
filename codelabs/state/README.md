# Overview

In this code lab we will learn how to add state to a `Component`. First, we will use state to
create a very simple counter component. We will learn how to add state to a component, and we will
learn how to use and update the current state of a `component`.

In the second section we will look at lazy state updates; where state updates do not re-render the
UI. This is especially useful when certain states are not used during render or when updates are
not immediately required.

# Setup the environment

    1. Clone this repository.
    2. Open project in Android Studio.

1. Adding state to a Component

Adding state is similar to adding a prop to the Component; any argument in `OnCreateLayout`
annotated with `@State` declares it as a state value of the component.

```kotlin
@OnCreateLayout
fun onCreateLayout(
    c: ComponentContext,
    @State count: Int   // count is a state value
): Component {
  return Row.create(c)/* counter UI */.build()
}
```

# Setting the initial value for state

Initialize the value of state in the `OnCreateInitialState` lifecycle of the component.

```kotlin
@OnCreateInitialState
fun onCreateInitialState(
  c: ComponentContext,
  count: StateValue<Int>  // StateValue is the container for `count`.
) {
  count.set(0) // set the initial value of count to 0.
}
```

# Adding a click handler

Add an event handler for click events by adding an `@OnEvent` lifecycle to the component.

```kotlin
@OnEvent(ClickEvent::class)
fun onIncrement(c: ComponentContext) {
  // handle the event here.
}
```

This will generate a method in the component which can be used to set the click handler on other
components.

```kotlin
Text.create(c)
  .text("+")
  .clickHandler(RootComponent.onIncrement(c)) /* sets the click handler on the Text Component */
```

# Adding the state update method

Add a state update method to the component by using the `OnUpdateState` lifecycle to the component.

```kotlin
@OnUpdateState
fun increment(count: StateValue<Int>, step: StateValue<Int>) {
  // Actually increment the value of count.
  count.set(count.get()?.plus(1))
}
```

This will generate a method in the component to call this state update method on the component;
use this method in the `onIncrement` event handler of the component.

```kotlin
@OnEvent(ClickEvent::class)
fun onIncrement(c: ComponentContext) {
  RootComponent.increment(c) // Call the increment state update method.
}
```

Dispatching a state update will re-render the component again; i.e. the `OnCreateLayout`
lifecycle will be called again. Run the application and test the counter.

2. Lazy State Updates

For lazy state updates we will add a `TextInput` where the increment/decrement step can be
specified. The text change is not immediately required so we can lazily apply this state
update.

# Adding a lazy state value in the component

To enable a state value to lazily update set its `canUpdateLazily` option to `true`. Also don't
forget to initialize it in `OnCreateInitialState`.

```kotlin
@OnCreateInitialState
fun onCreateInitialState(
  c: ComponentContext,
  @Prop(optional = true) startCount: Int,
  count: StateValue<Int>,
  step: StateValue<Int>
) {
  count.set(0) // set the initial value of count to 0.
  step.set(1) // set the initial value of step to 1.
}

@OnCreateLayout
fun onCreateLayout(
  c: ComponentContext,
  @State count: Int,
  @State(canUpdateLazily = true) step: Int // Set the `canUpdateLazily` option to `true`.
): Component {
  return Row.create(c)/* counter UI */.build()
}

@OnUpdateState
fun changeStep(step: StateValue<Int>, @Param value: Int) {
  step.set(value)
}
```

This will generate a new state update method in the component to dispatch a lazy state update.
Lazy state update methods are prefixed with `lazy`. So the new generated method will
be `lazyUpdateStep`.

## Creating a text change handler

Adding an event handler to listen to text change events.

```kotlin
@OnEvent(TextChangedEvent::class)
fun onChangeStep(c: ComponentContext, @FromEvent text: String) {
  /*
    `text`: is an attribute of the `TextChangedEvent`, which is equal the text in the `TextInput`
    component which dispatched this event.
  */

  /*
    Lazy state update will dispatch the state update, but will only apply it in the next layout
    pass. See the log statement printed in the `OnCreateLayout`.
  */
  RootComponent.lazyUpdateStep(c, value)
}
```

Setting the text changed event handler on the `TextInput`.

```kotlin
TextInput.create(c)
    .initialText(step.toString())
    .minWidthDip(64F)
    .textSizeSp(24F)
    /* Setting the text change handler */
    .textChangedEventHandler(RootComponent.onChangeStep(c))
```
