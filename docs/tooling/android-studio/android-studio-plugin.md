---
id: android-studio-plugin
title: Litho Android Studio Plugin
---

## Litho Kotlin API

The Litho Kotlin API does not rely on annotation processing and code generation - its components are plain Kotlin classes. This means you don't need any special IDE plugins to work with them effectively. Standard Android Studio features like code navigation, completion, and refactoring work out of the box.

:::note
At this time, we recommend adopting the [Litho Kotlin API](../../migrating-to-kcomponents/kotlin-intro.mdx) for your development needs.
:::

## File Templates

You can still use the Litho IDE plugin to get file templates for new Kotlin API components. The plugin is available from the [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/14468-litho).

## Legacy Spec API Plugin

For the legacy Spec API IDE plugin capabilities (code navigation for generated code, editor warnings for Specs, etc.), see [Litho IDE Plugin (Spec API)](../../legacy-spec-api/tooling/android-studio-plugin.md).

