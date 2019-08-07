---
docid: cached-values
title: Cached Values
layout: docs
permalink: /docs/cached-values
---

The purpose of the Cached Values API is to provide caching within `Spec` classes, rather than have to repeatedly make an expensive calculation or to use lazy state updates for this purpose.

The API is made up of two annotations: `@CachedValue` and `@OnCalculateCachedValue`. `@CachedValue` is used in the same way as `@Prop`, `@State` etc - it is used to annotate a parameter to a `Spec` method, which the generated code will use to pass the `Spec` method the correct cached value. In particular, the generated code will check to see if the value is already cached, and if not it will calculate the value and cache it.

`@OnCalculateCachedValue` is used to calculate the cached value. It has a method `name()` which is used to identify which cached value the method is calculating. Cached values can only depend upon props and state - any other parameters are not allowed.

```
@OnCreateLayout
static Component onCreateLayout(
    ComponentContext c,
    @Prop Object prop,
    @CachedValue int expensiveValue) {
  return getComponent(prop, expensiveValue);
}

@OnCalculateCachedValue(name = "expensiveValue")
static int onCalculateExpensiveValue(
    @Prop Object prop,
    @State Object state) {
  return doExpensiveCalculation(prop, state);
}
```

`@OnCalculateCachedValue` is called whenever the dependent props or state change - i.e. an equality check on them fails. 

Cached values are stored on the `ComponentTree`, so they will live for the lifetime of the `ComponentTree`.
