---
id: debugging-lists
title: Debugging Lists
---

This page covers debugging techniques for Litho list components.

## Debugging Lazy Collections

For modern Kotlin-based Lazy Collections (`LazyList`, `LazyGrid`, `LazyStaggeredGrid`), you can use the standard Flipper Layout Inspector to inspect the component hierarchy and debug issues.

The Layout Inspector allows you to:
- View the full UI hierarchy including Lazy Collection items
- Inspect component props and state
- Modify values at runtime for debugging

For more information on using the Flipper Layout Inspector, see the [Flipper Plugins](../developer-tools/flipper-plugins.mdx) page.

### Common debugging techniques

1. **Verify child IDs are stable**: Ensure each child has a consistent and unique `id` to enable proper diffing. Unstable IDs can cause unnecessary re-renders.

2. **Check for unnecessary updates**: If items are re-rendering unexpectedly, verify that props implement proper `equals()` methods. See [Working with Updates](../../lazycollections/lazycollections-working-with-updates.mdx) for guidance.

3. **Use the Layout Inspector**: Inspect the component hierarchy to verify that items are being recycled correctly.

## Debugging Legacy Sections

For debugging legacy Sections-based lists (`RecyclerCollectionComponent`, `GroupSectionSpec`, `DiffSectionSpec`), see the dedicated [Debugging Sections](../../legacy-spec-api/sections/debugging-sections.md) guide in the Legacy Spec API section.

The Sections debugging guide covers:
- Understanding Sections hierarchies and the RecyclerBinder
- Reading changeset operations in the Flipper Sections plugin
- Common issues like state resets, unwanted scrolling, and inefficient re-renders
