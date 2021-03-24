---
docid: flipper-plugins
title: Flipper Plugins
layout: docs
permalink: /docs/flipper-plugins
---

When you create or debug standard Android Views, you can use Layout Preview and Layout Inspector tools from Android Studio. But since Litho operates with different UI primitives like Components and Sections those standard tools are not very useful for our  case.

Luckily, we have [Flipper](https://fbflipper.com) – an extensible mobile app debugger whose Layout Inspector plugin gives you an ability to tweak and inspect your UI in runtime and supports Litho. You'll be able to access full UI hierarchy, inspect Litho Components as well as Views, and even change values for View attributes or Component props in a currently running app without rebuilding it.

![Flipper Layout plugin](/static/images/flipper-layout-plugin.png)

## Adding Flipper & Layout plugin to your Project

To add Flipper and Litho plugins to you app you need to do the [following steps](https://fbflipper.com/docs/setup/layout-plugin):

```groovy
dependencies {
  debugImplementation 'com.facebook.flipper:flipper:{{site.flipper-version}}'
  debugImplementation 'com.facebook.flipper:flipper-litho-plugin:{{site.flipper-version}}'
}
```

And init `FlipperClient` with Inspector plugin which is typically done in your custom `Application` class:

```java
final FlipperClient client = AndroidFlipperClient.getInstance(mApplicationContext);
final DescriptorMapping descriptorMapping = DescriptorMapping.withDefaults();
LithoFlipperDescriptors.add(descriptorMapping);
client.addPlugin(new InspectorFlipperPlugin(mApplicationContext, descriptorMapping));
client.start();
```

You can read more about Layout Inspector on [Flipper website](https://fbflipper.com/docs/features/layout-plugin).

## Sections plugin

Another Litho plugin that is very useful for debugging Litho is Sections. It can uncover the flow of state changes for the list backed by [Litho Sections](/docs/sections-intro), visualise these changes such as which items were added, reused or deleted, and show the data corresponding to the specific Section.

![Flipper Sections plugin](/static/images/flipper-sections-plugin.png)

To enable Sections plugin in your app you need to add this line in addition to the general Flipper configuration:

```java
client.addPlugin(new SectionsFlipperPlugin(true));
```
