---
id: debugging-tips
title: Debugging Tips
---

# Flipper

## Layout Inspector Plugin

To setup the Layout Inspector Plugin in your project, refer to the [Layout Plugin Setup](https://fbflipper.com/docs/setup/plugins/inspector/) page, which is part of the official Flipper website.

Thanks to Layout Inspector Plugin, you can inspect what views the hierarchy consists of as well as the properties of each view.

The Layout Tab presents components in the hierarchy as if they were native views: on the right hand side you can see all of the properties and state of the given view (see the following image). If you hover over a view or a component in Flipper, the corresponding item will be highlighted in your app. This is perfect for debugging the bounds of your views and making sure you have the correct visual padding.

![Highlight in Flipper](/images/debugging-flipper-highlight.png)

### Quick Edits

The Layout Inspector enbles you to view the hierarchy and inspect each item's properties. In addition, it also enables you to edit things such as layout attributes, background colors, props, and state. With hese features, you can quickly tweak paddings, margins, and colors until you are happy with them, all without re-compiling.  This can save you many hours when implementing a new design.

To edit a property, simply find the view through the layout hierarchy and edit it's properties on the right hand panel, as shown in the following video.

<div display="block">
  <video src="https://lookaside.internalfb.com/intern/pixelcloudnew/asset/?id=585510272692142" controls="1" preload="auto" width="100%"></video>
</div>

### Target Mode

Enable Target Mode by clicking on the crosshairs icon. Now, you can touch any view on the device and the Layout Inspector will jump to the correct position within your layout hierarchy, as shown in the following video.

<div display="block">
  <video src="https://lookaside.internalfb.com/intern/pixelcloudnew/asset/?id=232484772246926" controls="1" preload="auto" width="100%"></video>
</div>

### Accessibility Mode

Enable Accessibility Mode by clicking on the accessibility icon. This shows the accessibility view hierarchy next to the normal hierarchy (see the image, below).

In the accessibility view hierarchy, the currently accessibility-focused view is highlighted in green and any accessibility-focusable elements have a green icon next to their name.

When Accessibility Mode is enabled, the sidebar will show special properties that are used by accessibility services to determine their functionality.
This includes items such as content-description, clickable, focusable, and long-clickable, among others.

![Accessibility in Flipper](/images/debugging-flipper-accessibility.png)

### Talkback

The Accessibility Mode sidebar also includes a panel with properties derived specifically to show Talkback's interpretation of a view (with logic ported over from Google's Talkback source). While generally accurate, this is not guaranteed to be accurate for all situations: it is always better to turn Talkback on for verification.

## Sections Plugin

The Sections plugin provides a view into sections tree generations and lifecycle events that render content on the screen.

With Sections Plugin you can:

* Track Sections lifecycle events.
* Show a stack trace for each state update that occurs in the section hierarchy.
* Visualize the Sections tree hierarchies.
* Inspect Render Sections hierarchies.
* Inspect Sections generated and applied changesets.

Once you open the plugin, it will start listening for all sections events that are triggered by the Sections. This might slow down the interaction with the app at first but it should be smooth out after the first event has been tracked.

The plugin rationale is to create a collection of events that come with an attached stack trace. Every event can hold different information that is rendered on screen through either the Tree Data port view or the left Sidebar.

![Sections in Flipper](/images/debugging-flipper-sections.png)

### Events Dashboard

The Events Dashboard (see the following image) collects all the relevant events that are sent by the following:

* Section component tracker.
* Object responsible to manage the lifecycle events of a Sections hierarchy.
* Sections Tree Analytics Listener, which is responsible for delivering information about the build steps for each tree generation.

Depending on the event that is selected, different information becomes available to the user.

![Events dashboard](/images/debugging-flipper-sections-dashboard.png)

### Stack Trace

For each event that gets selected, a stack trace is attached enabling you trace back who triggered the selected event.

### Tree Data

Tree Data visualises the available tree generation that comes with the selected event (see the following image).

You can analyse each item state between events and find information if the item was:

* removed
* inserted
* updated
* reused

![TreeData for Sections](/images/debugging-flipper-sections-tree.png)

### Changeset Information

After a completed tree generation, every Section Component part of the Sections tree is queried for returning its changeset.

Changesets are reported on the right sidebar (as shwon in the following image) with information about the type of change; they can be particularly useful for analysing what is happening with your Section:

* Insert
* Insert Range
* Update
* Update Range
* Delete
* Delete Range
* Move

![Sections Changeset](/images/debugging-flipper-sections-changeset.png)

## Enable Debugging Logs

Currently there are three places where you can change the boolean value to see more useful logs in logcat:

1. [SectionsDebug](pathname:///javadoc/com/facebook/litho/widget/SectionsDebug.html) -
turning on the `ENABLED` flag will enable the logs with the changesets that were calculated and expose which items were inserted, removed, updated or moved in your list.
This can help answer questions such as "*Why was my item re-rendered when nothing has changed?*".

2. [ComponentTree](pathname:///javadoc/com/facebook/litho/ComponentTree.html) -
   turning on the `DEBUG_LOGS` flag will enable the logs from the `ComponentTree` class responsible for calculating, resolving, measuring and flattening the layout.  This can help answer questions such as  "*Why is the UI not updating?*" (Possibly, a layout calculation issue), "*Why isn't my component visible, but is showing up in Flipper?*" (Possibly, a Mounting problem where `mount` wasn’t called for the layout result).

3. [AnimationsDebug](pathname:///javadoc/com/facebook/litho/AnimationsDebug.html) -
   turning on the `ENABLED` flag will enable logs related to the Animations and Transitions.
