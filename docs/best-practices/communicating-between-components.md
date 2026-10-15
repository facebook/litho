---
id: communicating-between-components
title: Communicating Between Components
---

This page covers patterns for communicating between Litho components, including parent-child, sibling, and external communication.

## Overview

Litho components communicate through several patterns:

| Pattern | Direction | Use Case |
|---------|-----------|----------|
| [Callback props](#dispatching-events-from-child-to-parent) | Child → Parent | Notify parent of events |
| [Props updates](#passing-new-data-from-parent-to-child) | Parent → Child | Pass new data to children |
| [Controllers](#triggering-an-action-on-a-child-from-a-parent) | Parent → Child | Trigger imperative actions |
| [Parent mediation](#communicating-between-siblings) | Sibling ↔ Sibling | Coordinate between siblings |
| [StateFlow/Listeners](#updating-a-component-from-outside-litho) | External → Component | React to external changes |
| [Observer callbacks](#communicating-externally-from-a-component) | Component → External | Notify external systems |

:::note Thread Safety
A Component's `render` method may run on a background thread. It's important that any communication mechanisms do not perform side-effects from `render`. Side-effects can instead be performed from main thread events like `onClick` and `useEffect`.
:::

### Dispatching events from child to parent

Sometimes a parent needs to know when an event happens in a child component. For example, a `ProductCard` needs to know when its `AddToCartButton` is clicked. The parent passes a lambda callback to the child:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ChildToParentCommunication.kt start=start_parent_receives_event end=end_parent_receives_event
```

The child component invokes the callback to notify the parent that the action occurred:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ChildToParentCommunication.kt start=start_child_sends_event end=end_child_sends_event
```

### Passing new data from parent to child

When a parent component needs to update a child, it passes new props. For example, a `CartScreen` updates a `CartTotal` component when items are added. The parent triggers a [state update](../state-and-side-effects/use-state.mdx), and the child receives the latest value through its props:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ParentToChildCommunication.kt start=start_parent_updates_child end=end_parent_updates_child
```

### Triggering an action on a child from a parent

Sometimes a parent needs to trigger an imperative action on a child rather than just passing new props. For example, a `CheckoutForm` might need to trigger validation on a `PaymentCardInput`. Use the [Controllers Pattern](/docs/primitives/primitive-controllers/) for this:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ControllerPatternCommunication.kt start=start_controller_definition end=end_controller_definition
```

The parent creates the controller and triggers validation when the user submits:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ControllerPatternCommunication.kt start=start_parent_with_controller end=end_parent_with_controller
```

The child registers a listener with the controller:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ControllerPatternCommunication.kt start=start_child_with_controller end=end_child_with_controller
```

### Communicating between siblings

Two sibling components cannot communicate directly. Instead, use a **parent as mediator** pattern:

1. Child A dispatches an event to the parent
2. Parent updates state or triggers an action
3. Child B receives the update through props or a controller

This ensures a unidirectional data flow that's easier to debug and maintain.

For example, a `PromoCodeInput` applies a discount that updates an `OrderSummary`. The `PromoCodeInput` sends the discount to the parent:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/SiblingCommunication.kt start=start_sibling_sender end=end_sibling_sender
```

The `OrderSummary` receives the discount as a prop and displays the updated total:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/SiblingCommunication.kt start=start_sibling_receiver end=end_sibling_receiver
```

The `CheckoutScreen` mediates between the siblings by holding the shared state:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/SiblingCommunication.kt start=start_parent_mediator end=end_parent_mediator
```

### Updating a component from outside Litho

A component tree can be updated by creating a new root component with new props and calling `ComponentTree#setRootAsync`.

There are two main approaches for external systems to communicate with components:

#### With a StateFlow (Recommended)

Use `StateFlow` when you have reactive data streams. This is the preferred approach for Kotlin as it integrates naturally with coroutines and provides automatic lifecycle management. For example, a `CartBadge` can observe the cart item count from a repository:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ExternalCommunication.kt start=start_component_with_flow end=end_component_with_flow
```

#### With a listener pattern

Use a listener pattern when working with callback-based APIs or when you need more control over subscription lifecycle:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ExternalCommunication.kt start=start_external_observer_interface end=end_external_observer_interface
```

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ExternalCommunication.kt start=start_component_with_listener end=end_component_with_listener
```

### Communicating externally from a Component

To send events from a component to a listener outside of the Litho hierarchy, define an observer interface and invoke it from a component event handler. For example, a `PurchaseButton` can notify an `AnalyticsService` when clicked:

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ExternalCommunication.kt start=start_component_event_observer end=end_component_event_observer
```

```kotlin file=sample-kotlin/src/main/java/com/facebook/samples/litho/kotlin/documentation/communicating/ExternalCommunication.kt start=start_component_with_callback end=end_component_with_callback
```
