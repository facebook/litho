---
docid: trigger-events
title: Sending events from parent to child
layout: docs
permalink: /docs/trigger-events
---
If you want to pass an event down from the parent component to the child, you can use the Trigger APIs to achieve that.

## API

Your child component will declare that it is able to handle events from its parent.

```java
@LayoutSpec
class ChildComponentSpec {
  @OnTrigger(YourEvent.class)
  static Object yourTriggerFunction(
    ComponentContext c,
    @FromTrigger Object fromTrigger,
    @State Object yourState,
    @Prop Object yourProp) {
    // Handle event
    return new Object();
  }
}
```

Declare your event class.

```java
@Event(returnType = Object.class)
public class YourEvent {
  public Object fromTrigger;
}
```
> Note: YourEvent class is declared in the same way as any other EventHandler class, so you can reuse them.

Your parent component will then assign a key to the child component.
> Note: This step is required because we only allow the direct parent of the child component to trigger it.

```java
@LayoutSpec
class ParentComponentSpec {
 @OnCreateLayout
 Component onCreateLayout(ComponentContext c) {

   return Column.create(c)
       .child(
        ChildComponent.create(c)
           .key("trigger"))
       .build();
 }
}
```

To trigger an event down, use the key your declared in the wiring and pass in the necessary information.

```java
@LayoutSpec
class ParentComponentSpec {
 ...
 static void function(ComponentContext c, Object fromTrigger) {
     Object result = YourChildComponent.yourTriggerFunction(c, "trigger", fromTrigger);
 }
}
```
