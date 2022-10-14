---
id: trigger-events
title: Triggering Events with Handles
---

:::info
A `Handle` is a unique identifier that can be shared with other components by passing it as a `@Prop`.

Use `new Handle()` to create a new Handle and assign it to a component using the `.handle(..)` method in the component's builder.
:::

The following LayoutSpec applies a Handle (passed in as a @Prop) to a `TextInput` component:

```java
@LayoutSpec
public class TextInputContainerComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Handle textInputHandle) {
    return Column.create(c).child(TextInput.create(c).handle(textInputHandle)).build();
  }
}
```

Components with a reference to the TextInput's handle can now manipulate it directly, as follows:

```java
@LayoutSpec
public class ClearTextTriggerExampleComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    final Handle textInputHandle = new Handle();
    return Column.create(c)
        .child(
            Text.create(c)
                .text("Clear")
                .clickHandler(ClearTextTriggerExampleComponent.onClearClick(c, textInputHandle)))
        .child(TextInputContainerComponent.create(c).textInputHandle(textInputHandle))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClearClick(ComponentContext c, @Param Handle textInputHandle) {
    // Clear the TextInput inside TextInputContainerComponent
    TextInput.setText(c, textInputHandle, "");
  }
}
```

## Trigger custom events

Custom events can be triggered by taking the following three steps:

### 1. Declare the event class

```java
@Event
public class CustomEvent {
  public int increaseBy;
}
```

### 2. Create a trigger for the event inside the spec using the `@OnTrigger` annotation

This method has access to the event's parameters using @FromTrigger, as well as the component's Props and State:

```java
@LayoutSpec
public class ComponentWithCustomEventTriggerComponentSpec {

  ...

  @OnTrigger(CustomEvent.class)
  static void triggerCustomEvent(ComponentContext c, @FromTrigger int increaseBy) {
    // Add custom event behavior
  }
}
```

### 3. Assign a handle

Uses of the component must be assigned a Handle. The event can be triggered anywhere there is a reference to the `Handle`:

```java
@LayoutSpec
public class CustomEventTriggerExampleComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    final Handle textInputHandle = new Handle();
    return Column.create(c)
        .child(
            Text.create(c)
                .text("Trigger custom event")
                .clickHandler(CustomEventTriggerExampleComponent.onClick(c, textInputHandle)))
        .child(ComponentWithCustomEventTriggerComponent.create(c).handle(textInputHandle))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c, @Param Handle textInputHandle) {
    ComponentWithCustomEventTriggerComponent.triggerCustomEvent(c, textInputHandle, 2);
  }
}
```
