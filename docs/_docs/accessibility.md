---
docid: accessibility 
title: Accessibility
layout: docs
permalink: /docs/accessibility
---

## Content Description

All components support content description by default. This means all layout builders have a `CharSequence` prop named `contentDescription`.

Setting a content description on any component is as simple as:

```java
Image.create(c)
    .imageRes(R.drawable.some_image)
    .withLayout()
    .contentDescription("This is an image")
    .build())
```

The content description set here has the same semantics as when it is set on an Android view. 

## Custom accessibility

Mount Specs can implement their own accessibility support by implementing an `@OnPopulateAccessibilityNode` method. This method accepts an `AccessibilityNodeInfoCompat` argument as well as any props that are specified on the spec method. 

For example, accessibility for `Text` is specified using the following method: 

```java
@OnPopulateAccessibilityNode
static void onPopulateAccessibilityNode(
    AccessibilityNodeInfoCompat accessibilityNode,
    @Prop CharSequence text) {
  accessibilityNode.setText(text);
}
```

This is only applicable for components which mount drawables, since if the component mounts a view, the support is built-in.

## Extra accessibility nodes 

On more complex mount specs that need to expose extra nodes to the accessibility framework, you'll have to implement three extra methods:

- *GetExtraAccessibilityNodesCount*: Returns number of extra accessibility nodes exposed by the component.
- *OnPopulateExtraAccessibilityNode*: Populates the extra accessibility node with the given bounds. 
- *GetExtraVirtualViewAt*: Returns the index of the extra accessibility node for the given position within the component.
