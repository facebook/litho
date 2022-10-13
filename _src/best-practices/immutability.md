---
id: immutability
title: Immutability
---

Components are essentially functions that receive data as parameters and are immutable (cannot be changed). When the props or state of a component change, the framework will create a new component instance with the updated information, because the previous component cannot be mutated.

While the component itself is immutable, it is easy to make it not thread safe by using mutable objects for props and state. Litho computes [layout on a background thread](/docs/asynchronous-layout), and if the objects that make up a component's props or state are mutated from another thread, then this may result in rendering different outputs for the same component.

:::note Best Practice
Props and state must be kept as either primitive types (which are inherently immutable), or, if it's not possible to use a primitive type, an alternative that is also immutable.
:::
