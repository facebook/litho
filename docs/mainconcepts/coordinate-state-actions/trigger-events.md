---
id: trigger-events
title: Triggering events with Handles
---

:::danger Content needs to be updated
Moved from old website without any change.
:::

You can trigger Events on components using a `Handle`. A `Handle` is a unique identifier that can be shared can with other components by passing it as a `@Prop`. Use `new Handle()` to create a new `Handle` and assign it to a component using the `.handle(..)` method in the component's builder.

In the following LayoutSpec we apply a `Handle` (passed in as a @Prop) to a `TextInput` component.
```java
@LayoutSpec
public class TextInputContainerComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop Handle textInputHandle) {
    return Column.create(c).child(TextInput.create(c).handle(textInputHandle)).build();
  }
}
```

Components with a reference to the `TextInput`'s handle can now manipulate it directly.

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

You can trigger your own custom events.

Declare your event class.

```java
@Event
public class CustomEvent {
  public int increaseBy;
}
```

Create a trigger for your event inside your spec using the `@OnTrigger` annotation. This method will have access to event's parameters using @FromTrigger, as well as the components Props and State.

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

Usages of your component must be assigned a Handle. The event can be triggered anywhere we have a reference to the `Handle`.

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
