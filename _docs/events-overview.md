---
docid: events-overview 
title: Overview
layout: docs
permalink: /docs/events-overview
---

The framework provides a general-purpose API to connect components through events.

# Declaring events

Events are declared as a POJO with an @Event annotation. By convention we name suffix Event class names with *Event*. Event declarations may not be inner classes of your ComponentSpec. This is by design as specs are supposed to be a private concept and events can be used across multiple components.

```java
@Event
private class ColorChangedEvent {
  public int color;
}
```

In this example we will assume you have a component called `ColorComponent`. To indicate that a `ColorComponent` can dispatch a `ColorChangedEvent` your 'ColorComponentSpec' must be annotated with that information. This is done with the `events` parameter of the `@MountSpec` and `@LayoutSpec` annotations. A component may be annotated to dispatch multiple events.

This will auto-generate a matching //dispatch// method and an event identifier that will used by event callbacks (see //Handling events// section below).

The //dispatch// method takes an `EventHandler` as the first argument followed by the list of attributes defined in your `@Event` class. An `EventHandler` is essentially a generic //listener// interface to connect components through events. The convention is to have an `EventHandler` prop for each event exposed by your component. 

```java
@MountSpec(events = { ColorChangedEvent.class })
public class ColorComponentSpec {
  ...
  @OnMount
  static ColorDrawable onMount(
      Context c,
      ColorDrawable convertDrawable,
      @Prop EventHandler colorChangedHandler,
      @FromPrepare int color) {
    ...
    ColorComponent.dispatchColorChangedEvent(
        colorChangedHandler,
        color);
    ...
  }
}
```

In the example above, `ColorComponent` takes an `colorChangedHandler` as prop and dispatches the `ColorChangedEvent` to it with the generated `dispatchColorChangedEvent()` method. 

# Handling events

In order to handle events dispatched by other components, you'll need an `EventHandler` instance and a matching callback.

You can create `EventHandler` instances by using your generated component's corresponding event handler factory method. This method will have the same name as your event callback method.

You define the event callback using the `@OnEvent` annotation. `@OnEvent` takes one argument: the event class.
The first parameter of a method annotated with @OnEvent has to be a ComponentContext that the framework will populate for you.

For example, here's how a //layout spec// component would define a handler for the `ColorChangedEvent` declared above:

```java
@LayoutSpec
public class MyComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      LayoutContext c,
      @Prop String someColor) {

    return Container.create(c)
        ...
        .child(
            ColorComponent.create(c)
                .color(someColor)
                .colorChangedHandler(MyComponent.onColorChanged(c))
        ...
        .build();

  }

  @OnEvent(ColorChangedEvent.class)
  static void onColorChanged(
      ComponentContext c,
      @FromEvent int color,
      @Prop String someProp) {
    Log.d("MyComponent", "Color changed: " + color);
  }
}
```

Using the `@Param` annotation on one or more of the parameters of the callback method you can define dynamic event parameters. This is useful if you would like to define a callback for a certain type of event e.g. `onAvatarClicked()` but would like to know what avatar was clicked. The avatar parameter in this case would be passed to the event handler factory method.

As you can see, `@OnEvent` callbacks have access to all component props just like the other //spec// methods.

```java
@LayoutSpec
public class FacePileComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      LayoutContext c,
      @Prop Uri[] faces) {


    ComponentLayout.Builder builder = Container.create(c);
    for (Uri face : avatarUrls) {
      builder.child(
          FrescoComponent.create(c)
              .uri(face)
              .withLayout()
              .clickHandler(FacePileComponent.onFaceClicked(c, face));
    }
    return builder.build();
  }

  @OnEvent(ClickEvent.class)
  static void onFaceClicked(
      ComponentContext c,
      @Param Uri face) {
    Log.d("FacePileComponent", "Face clicked: " + face);
  }
}
```
