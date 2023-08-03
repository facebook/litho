---
id: communicating-between-components
title: Communicating Between Components
---

import {FbInternalOnly, OssOnly} from 'docusaurus-plugin-internaldocs-fb/internal';

### Dispatching an Event from a child to its parent

In the Spec API, communicating from a child to a parent is done through an `EventHandler`, which handles a custom event type. The `EventHandler` is defined in the parent component and passed as a Prop to the child component.  For more information on Spec events, see the document [Events for Specs](/docs/codegen/events-for-specs) page.

<FbInternalOnly>

In the Kotlin API, the parent component can simply pass a [lambda](https://kotlinlang.org/docs/lambdas.html) as the callback instead of a reference to a generated `EventHandler`.
For more information on Kotlin events, see [Event Handling](/docs/kotlin/event-handling).

</FbInternalOnly>

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentReceivesEventFromChildSpec.java start=start_demo end=end_demo
```

The child component can invoke the event handler received from the parent to inform the parent that a certain action took place, such as when the child component receives a click event or, in a visibility handler, when it becomes visible.  The following code provides an example.

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ChildComponentSendsEventToParentSpec.java start=start_demo end=end_demo
```

### Passing new Props from a parent to a child

If a parent component needs to pass new data to a child, it can do so by simply passing new props to the child component.
When the data is updated as a result of an action controlled by the parent component (for example, a click event on the parent component), the new data is passed down to the child component by triggering a 'state update', which updates the value of the prop that will be passed to the child component and recreates the child with this new value.  The child component receives the latest value of the state through the prop when it's created.

The following code illustrates this concept with a click event on the parent component.

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentSendsEventToChildSpec.java start=start_update_prop end=end_update_prop
```

### Triggering an Action on a child from a parent

There are cases when a parent needs to trigger an action on a child instead of just passing new data. To do this, the parent needs to keep a reference to the child and trigger an action on it using that reference.

The reference to the child is maintained through a `Handle` instance, which the parent creates and passes to the child component as a prop:

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentSendsEventToChildSpec.java start=start_define_handle end=end_define_handle
```

The parent uses the Handle reference to trigger an action on the child component:

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentSendsEventToChildSpec.java start=start_trigger end=end_trigger
```

The action is defined on the child component using the `@OnTrigger` annotation in Java:

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ChildComponentReceivesEventFromParentSpec.java start=start_define_trigger end=end_define_trigger
```

Defining triggers in `KComponents` is not supported yet, but they can invoke triggers as with Java Components.

### Communicating between siblings

Two sibling components (two child components of the same parent) cannot communicate directly. All communication must flow through the parent component, which intercepts events from a child component and notifies other child components of those events (using the methods detailed above).

A child component that needs to send a signal to a sibling component will dispatch an event to the common parent component:

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ChildComponentSiblingCommunicationSpec.java start=start_dispatch_to_parent end=end_dispatch_to_parent
```

As shown in the following code, the parent component can:

* Perform a state update to recreate the sibling with new data (@OnUpdateState)
* Trigger an event on the sibling using a reference (@OnEvent).

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/ParentComponentMediatorSpec.java start=start_parent_mediator end=end_parent_mediator
```

### Communicating externally to a component

New data can be passed to a component from outside a Litho hierarchy by simply creating a new root component with new props.

There are multiple ways to perform an action on a component from outside a Litho hierarchy. The preferred method to pass new information to a component is by recreating it with new props; sometimes, it's necessary to trigger an action from non-Litho systems.

#### With an observer

An interface callback is invoked externally:

```java file=sample/src/main/java/com/facebook/samples/litho/java/stateupdates/StateUpdateFromOutsideTreeActivity.java start=start_external_observer end=end_external_observer
```

The Component implements the callback and dispatches a state update on itself when the callback is invoked. No props or state should be captured in the callback: the callback will not be updated if they change, as illustrated in the following code.

```java file=sample/src/main/java/com/facebook/samples/litho/java/stateupdates/StateUpdateFromOutsideTreeWithListenerComponentSpec.java start=start_implement_observer end=end_implement_observer
```

#### With a handle

A Handle reference can be created and passed to a Component, then used to invoke a trigger defined in the component:

```java file=sample/src/main/java/com/facebook/samples/litho/java/stateupdates/StateUpdateFromOutsideTreeActivity.java start=start_external_handle end=end_external_handle
```

### Communicating externally from a Component

To send events from a component to a listener outside of the Litho hierarchy, define an observer externally and invoke it from a component lifecycle method.

```java file=sample/src/main/java/com/facebook/samples/litho/java/communicating/CommunicatingFromChildToParent.java start=start_define_observer end=end_define_observer
```

:::tip
Keep in mind that some lifecycle methods of Litho components can be invoked on background threads, so invoking callbacks from these methods might not be thread-safe if the callback produces side-effects.
:::
