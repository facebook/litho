---
id: communicating-between-components
title: Communicating between Components
---
### Dispatching an event from a child to the parent
In the Spec API, communicating from a child to a parent is done through an `EventHandler` which handles a custom event type. The `EventHandler` is defined in the parent Component and passed as Prop to the child component.
You can read in more detail about events in the [Events Overview](/docs/mainconcepts/coordinate-state-actions/events) page for Specs.

In the Kotlin API, the parent can simply pass a [lambda](https://kotlinlang.org/docs/lambdas.html) as callback instead of a reference to a generated `EventHandler`.
You can read in more detail about events in Kotlin [here](/docs/fb/kotlin/event-handling).

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentReceivesEventFromChildSpec.java start=start_demo end=end_demo
```

The child component can invoke the event handler received from the parent to inform the parent that a certain action happened on the child component - for example when the child component receives a click event or in a visibility handler when it becomes visible.
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ChildComponentSendsEventToParentSpec.java start=start_demo end=end_demo
```

### Passing new Props from parent to child
If a parent component needs to pass new data to a child, it can do so by simply passing new props to the child Component. When the data is updated as a result of an action controlled by the parent component (for example, a click event on the parent component), the new data is passed down to the child Component by triggering a state update which updates the value of the prop that will be passed to the child Component and recreates the child with this new value.
The child component receives the latest value of the state through the prop when it's created.
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentSendsEventToChildSpec.java start=start_update_prop end=end_update_prop
```

### Triggering an action on a child from a parent
There are cases when a parent needs to trigger an action on a child instead of just passing new data. To do this, the parent needs to keep a reference to the child and trigger an action on it using that reference.
The reference to the child is maintained through a `Handle` instance which the parent creates and that is passed to the child component as prop:

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentSendsEventToChildSpec.java start=start_define_handle end=end_define_handle
```

The parent uses the Handle reference to trigger an action on the child component:
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentSendsEventToChildSpec.java start=start_trigger end=end_trigger
```

The action is defined on the child component using the `@OnTrigger` annotation in Java.
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ChildComponentReceivesEventFromParentSpec.java start=start_define_trigger end=end_define_trigger
```

Defining triggers in `KComponents` is not supported yet, but they can invoke triggers same as Java Components.

### Communicating between siblings
Two sibling components cannot communicate directly. All communication must flow through a common parent component which intercepts events from a child component and notifies other children components of these events, by combining the two ways of communicating from above.

A child component that needs to send a signal to a sibling component will dispatch an event to the common parent component.
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ChildComponentSiblingCommunicationSpec.java start=start_dispatch_to_parent end=end_dispatch_to_parent
```

The parent component can either perform a state update to recreate the sibling with new data or trigger an event on the sibling using a reference.
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentMediatorSpec.java start=start_parent_mediator end=end_parent_mediator
```

### Communicating externally to a Component
New data can be passed to a component from outside a Litho hierarchy by simply creating a new root Component with new props.
There are multiple ways you can perform an action on a Component from outside a Litho hierarchy. The preferred method to pass new information to a Component is by recreating it with new props - however sometimes it's necessary to trigger an action from non-Litho systems.
#### With an observer

An interface callback is invoked externally:
```java file=sample/src/main/java/com/facebook/samples/litho/java/stateupdates/StateUpdateFromOutsideTreeActivity.java start=start_external_observer end=end_external_observer
```
The Component implements the callback and dispatches a state update on itself when the callback is invoked. No props or state should be captured in the callback - the callback will not be updated if they change.
```java file=sample/src/main/java/com/facebook/samples/litho/java/stateupdates/StateUpdateFromOutsideTreeWithListenerComponentSpec.java start=start_implement_observer end=end_implement_observer
```

#### With a handle

You can create a Handle reference and pass it to a Component, then use the reference to invoke a trigger defined in the Component.
```java file=sample/src/main/java/com/facebook/samples/litho/java/stateupdates/StateUpdateFromOutsideTreeActivity.java start=start_external_handle end=end_external_handle
```
### Communicating externally from a Component
To send events from a component outside to a listener outside of the Litho hierarchy, we can define an observer externally and invoke it from a Component lifecycle method. Keep in mind that some lifecycle methods of Litho components can be invoked on background threads, so invoking callbacks from these methods might not be thread-safe if the callback produces side effects.
```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/CommunicatingFromChildToParent.java start=start_define_observer end=end_define_observer
```
