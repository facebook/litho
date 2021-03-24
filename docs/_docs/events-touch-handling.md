---
docid: events-touch-handling
title: Touch Handling
layout: docs
permalink: /docs/events-touch-handling
---

All components support touch handling through the framework's event system. All components can handle the following events by default: `ClickEvent`, `LongClickEvent`, and `TouchEvent`.

This means all layout builders have an `EventHandler` prop named `clickHandler`, `longClickHandler`, and `touchHandler` respectively. Specify the event class you want to handle as an argument to the `@OnEvent` annotation.

For example, setting a click handler on any component is as simple as:

```java
@LayoutSpec
class MyComponentSpec {

	@OnCreateLayout
	static Component onCreateLayout(
	    ComponentContext c,
	    @Prop String title) {
	    
	  return Text.create(c)
	      .text(title)
	      .clickHandler(MyComponent.onClick(c))
	      .build();
    }
}
```

And the callback within MyComponentSpec would look like this:

```java
@LayoutSpec
class MyComponentSpec {
... 
    @OnEvent(ClickEvent.class)
    static void onClick(
        ComponentContext c,
        @FromEvent View view,
        @Prop String someProp) {
        // Handle click here.
    }
}
```

## Touch area expansion
You can expand the interactive bounds of a component by using the touch expansion APIs in the layout builder:

```java
Text.create(c)
    .text(title)
    .clickHandler(MyComponent.onClick(c))
    .touchExpansionDip(ALL, 10);
```
In this example, the clickable bounds of the text component will be 10 dips larger on all edges (left, top, right, bottom).
