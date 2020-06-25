---
id: onattached-ondetached
title: OnAttached/OnDetached
---
import useBaseUrl from '@docusaurus/useBaseUrl';

Sometimes we would like the component to subscribe a listener when it's available and unsubscribe the listener when the component is unavailable. Usually we can use `VisibleEvent` and `InvisibleEvent` handlers to subscribe/unsubscribe listeners.

```java
@OnEvent(VisibleEvent.class)
static void onVisible(
    ComponentContext c, @Prop DataSource dataSource, @State SomeListener listener) {
  dataSource.subscribe(listener);
}

@OnEvent(InvisibleEvent.class)
static void onInvisible(
    ComponentContext c, @Prop DataSource dataSource, @State SomeListener listener) {
  dataSource.unsubscribe(listener);
}
```

However, there're a few drawbacks with this approach:

- `VisibleEvent` handler might not be called because the component isn't visible in the viewport, or might be called multiple times without changing the component. Similar issue with `InvisibleEvent` handler. It's hard to use these event handlers to manage listeners/resources.
- There's no guarantee that `VisibleEvent`/`InvisibleEvent` handlers are executed in order.

Method annotated with `@OnAttached` is called when the component is attached to the `ComponentTree`, and method annotated with `@OnDetached` is called when either it's removed from the `ComponentTree` or the `ComponentTree` is released. For each component, both the methods are guaranteed to be called at most once.

## Introduce OnAttached/OnDetached lifecycle methods

By introducing these two methods, now we can subscribe the listener in `@OnAttached` method and unsubscribe it in `@OnDetached` method.

```java
@OnAttached
protected void onAttached(
    ComponentContext c, @Prop DataSource dataSource, @State SomeListener listener) {
  dataSource.subscribe(listener);
}

@OnDetached
protected void onDetached(
    ComponentContext c, @Prop DataSource dataSource, @State SomeListener listener) {
  dataSource.unsubscribe(listener);
}
```

<p align="center">
<img src={useBaseUrl("/images/layout-spec-delegate-moethods.svg")} alt="Image" width="70%" height="70%" />
</p>

## When is @OnAttached method called?

`@OnAttached` is called when `LayoutState` is finalized and applied to the `ComponentTree`. For each component in each `ComponentTree`, `@OnAttached` is guaranteed to be called only once.

## When is @OnDetached method called?

`@OnDetached` is called either when

- `LithoView#release()` or `ComponentTree#release()` is called. Usually you would need to release `LithoView` or `ComponentTree` manually in Activity/Fragment `onDestroy()` to trigger `@OnDetached` method.
- A new root is assigned to `ComponentTree`, i.e `LithoView#setComponent()`, `ComponentTree#setRoot()` or one of their async variants is called, and the old component doesn't exist in the root. For example:

<p align="center">
  <img src={useBaseUrl("/images/set-new-root.png")} alt="Image" width="60%" height="60%" />
</p>

When a new root is applied, `@OnDetached` methods for components `C`, `D`, `E` are called. You can see that even if component `E` still exists in the new root, its `@OnDetached` is called as well, because its position has changed.

## Sample app

Check out [Component Lifecycle Example](https://github.com/facebook/litho/blob/master/sample/src/main/java/com/facebook/samples/litho/lifecycle/LifecycleDelegateActivity.java) in our sample app to understand the component lifecycle methods better!
