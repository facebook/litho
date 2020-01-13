---
docid: reconciliation
title: Reconciliation
layout: docs
permalink: /docs/reconciliation
---

## Introduction

**Within the framework reconciliation is enabled by default.**

**Note: As of January 2020 reconciliation is only implemented for state updates.**

Reconciliation is an implementation detail in Litho’s layout calculation process. Instead of
recreating the entire layout tree for every update reconciliation enables Litho to clone the
unmodified subtrees and only recreate the subtree which has changed.

When using Litho, the component hierarchy is determined by what is returned by the
OnCreateLayout method. On the next state or prop update, the `OnCreateLayout` may return a different
tree of components. Litho uses reconciliation to figure out which components should be re-created
and which can be re-used (read cloned).

In essence, this means Litho will not call (most) lifecycle methods of components which were
unaffected by the update. Reconciliation doubles down on the requirement that component specs must
be immutable and should not rely on side effects to achieve any desired behaviour.

## Enable or disable reconciliation!

There are various integration points.

### **`ComponentTree`**

The `ComponentTree` builder exposes an API to enable or disable reconciliation.
(As of January 2020) Reconciliation is enabled by default in Litho.

```java
ComponentTree.create(context)
  .isReconciliationEnabled(true)
  .build();
```

**Note: In this integration the config is not passed into any sections within the component
hierarchy. It needs to be explicitly set in the sections (see below). There are plans to enable
propagation over the section boundary in future milestones.**

### **`RecyclerCollectionComponent`**

The `RecyclerBinderConfiguration` and the `RecyclerBinder` builders both expose an API to enable or
disable reconciliation.

```java
RecyclerBinderConfiguration.create()
  .isReconciliationEnabled(false)
  .build();
```
<br>
```java
new RecyclerBinder.Builder()
  .isReconciliationEnabled(true)
  .build(context);
```

### **`Sections`**

The `ComponentRenderInfo` builder exposes an API to enable or disable reconciliation for individual
sections in a GroupSectionSpec.

```java
ComponentRenderInfo.create()
  .component(component)
  .customAttribute(ComponentRenderInfo.RECONCILIATION_ENABLED, false)
  .build();
```

## Tradeoffs

It is important to remember that the reconciliation algorithm is an implementation detail. Litho
could re-create the whole hierarchy on every action; the end result would be the same. Just to be
clear, re-creation in this context means calling `OnCreateLayout` for the modified component, it
doesn’t mean Litho will unmount and remount them. Litho will only mount and unmount items will
which have changed.

In order to reconcile changes, Litho keeps the previous component hierarchy in memory. This
increases memory usage and can potentially increase OOMs.
