---
docid: saving-state
title: Saving state on rotation
layout: docs
permalink: /docs/saving-state
---

## Saving state on app configuration changes
When the app goes through configuration changes, such as rotation, which cause the Activity to be torn down and recreated, the LithoView that the Activity was displaying will also be destroyed, along with the ComponentTree it was hosting.
This means that all the Component [state](/docs/state) values maintained by the ComponentTree will be reset when the Activity is recreated, unless we do explicitly save and restore these values.

A full example on how to save and restore the state values when the Activity is destroyed can be found [here](https://github.com/facebook/litho/tree/master/codelabs/save-state-rotation).

The ComponentTree holds a StateHandler with the most recently updated Component state values.
You can obtain a copy of that state handler by calling `acquireStateHandler` on a ComponentTree instance.
When the Activity is recreated and a new ComponentTree is created and associated with the LithoView, all you need to do to restore the previously known state values is to pass this StateHandler instance which we saved before the Activity was destroyed when the ComponentTree is created.
```java
ComponentTree.create(
        componentContext,
        RootComponent.create(componentContext).build())
          .stateHandler(mStateHandlerViewModel.getStateHandler())
          .build()
```

> It is not safe to do this if any of the components in your hierarchy were holding a reference to the Activity/Context or a View through a state value, because a configuration change will destroy the Activity and Views.
