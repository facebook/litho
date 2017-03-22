---
docid: accessibility-handling
title: Accessibility Handling
layout: docs
permalink: /docs/accessibility-handling
---

All components support a set of events corresponding to [`AccessibilityDelegateCompat`](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html)'s methods. These events have attributes for each parameter of the corresponding `AccessibilityDelegateCompat` method *and* an additional parameter of type `AccessibilityDelegateCompat` called `superDelegate` which allows you to explicitly call `View`'s default implementation of accessibility methods where necessary. The supported events are:


| Event | AccessibilityDelegate method
| ----- | ----------------------------
| DispatchPopulateAccessibilityEventEvent | [dispatchPopulateAccessibilityEvent](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#dispatchPopulateAccessibilityEvent(android.view.View, android.view.accessibility.AccessibilityEvent))
| OnInitializeAccessibilityEventEvent | [onInitializeAccessibilityEvent](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#onInitializeAccessibilityEvent(android.view.View, android.view.accessibility.AccessibilityEvent))
| OnInitializeAccessibilityNodeInfoEvent | [onInitializeAccessibilityNodeInfo](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#onInitializeAccessibilityNodeInfo(android.view.View, android.support.v4.view.accessibility.AccessibilityNodeInfoCompat))
| OnPopulateAccessibilityEventEvent | [onPopulateAccessibilityEvent](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#onPopulateAccessibilityEvent(android.view.View, android.view.accessibility.AccessibilityEvent))
| OnRequestSendAccessibilityEventEvent | [onRequestSendAccessibilityEvent](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#onRequestSendAccessibilityEvent(android.view.ViewGroup, android.view.View, android.view.accessibility.AccessibilityEvent))
| PerformAccessibilityActionEvent | [performAccessibilityAction](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#performAccessibilityAction(android.view.View, int, android.os.Bundle))
| SendAccessibilityEventEvent | [sendAccessibilityEvent](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#sendAccessibilityEvent(android.view.View, int))
| SendAccessibilityUncheckedEvent |  [sendAccessibilityEventUnchecked](https://developer.android.com/reference/android/support/v4/view/AccessibilityDelegateCompat.html#sendAccessibilityEventUnchecked(android.view.View, android.view.accessibility.AccessibilityEvent))


Setting a handler for *any* of these events, will result in an `AccessibilityDelegate` being set on the mounted `View` which will call your event handler when the corresponding method is called. Whenever a method for which you haven't supplied an event handler is called, the delegate will defer to `View`'s default implementation (equivalent to calling `super` or `superDelegate`'s implementation).

For example, overriding `onInitializeAccessibilityNodeInfo` for a component can be as simple as:

1. Implementing an event handler

```java
@OnEvent(OnInitializeAccessiblityNodeInfoEvent.class)
static void onInitializeAccessibilityNodeInfoEvent(
    @FromEvent AccessibilityDelegateCompat superDelegate,
    @FromEvent View view,
    @FromEvent AccessibilityNodeInfoCompat node) {
  // Equivalent to calling super on a regular AccessibilityDelegate, not required
  superDelegate.onInitializeAccessibilityNodeInfo(view, node);
  // My implementation
}
```

{:start="2"}
2. Setting that event handler on a component:

```
Text.create(c)
    .text(title)
    .withLayout()
    .onInitializeAccessiblityNodeInfoHandler(MyComponent.onInitializeAccessibilityNodeInfo)
```  

One of the best features of `AccessibilityDelegate`s in general is their reusability even across different types of `View`s. This can also be achieved within the Components framework by creating a wrapper spec that takes in a component and adds the desired event handlers. For example, let's say we want to have a Component that appends "please" to every `AccessibilityEvent` that it announces.

```java
@LayoutSpec
class PoliteComponentWrapper {
    
  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop Component<?> content) {
    return Layout.create(c, content)
        .onPopulateAccessibilityEventHandler(PoliteComponentWrapper.onPopulateAccessibilityEvent(c))
        .build();
  }

  @OnEvent(OnPopulateAccessibilityEvent.class)
  static void onPopulateAccessibilityEvent(
      ComponentContext c,
      @FromEvent AccessibilityDelegateCompat superDelegate,
      @FromEvent View view
      @FromEvent AccessibilityEvent event) {
    superDelegate.onPopulateAccessibilityEvent(view, event);
    event.getText().add("please");
  }
}
```

Now you can replace any usages of your component with `PoliteComponentWrapper`

```java
@OnCreateLayout
static ComponentLayout onCreateLayout(
    ComponentContext c,
    @Prop CharSequence text) {
  return PoliteComponentWrapper.create(c)
      .content(
           Text.create(c)
               .text(text))
      .build();
}
```
