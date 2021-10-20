---
id: debugging-tips
title: Debugging Tips
---

# Flipper

## Layout Inspector Plugin

To setup the Layout Inspector Plugin in your project, please follow [this guide from the official Flipper docs](https://fbflipper.com/docs/setup/plugins/inspector/).

Thanks to Layout Inspector Plugin you can inspect what views the hierarchy is made up of as well as what properties each view has.
The Layout Tab presents components in the hierarchy just as if they were native views, on the right hand side you can see all of the properties and state of the given view.
If you hover over a view or a component in Flipper, the corresponding item will be highlighted in your app.
This is perfect for debugging the bounds of your views and making sure you have the correct visual padding.

![Highlight in Flipper](/images/debugging-flipper-highlight.png)

### Quick Edits

The Layout Inspector not only allows you to view the hierarchy and inspect each item's properties,
but it also allows you to edit things such as layout attributes, background colors, props, and state.
This allows you to quickly tweak paddings, margins, and colors until you are happy with them, all without re-compiling.
This can save you many hours implementing a new design.

To edit property, simply find the view through the layout hierarchy and edit it's properties on the right hand side panel:

<div display="block">
  <video src="https://lookaside.internalfb.com/intern/pixelcloudnew/asset/?id=585510272692142" controls="1" preload="auto" width="100%"></video>
</div>

### Target mode
Enable Target Mode by clicking on the crosshairs icon. Now, you can touch any view on the device and Layout Inspector will jump to the correct position within your layout hierarchy.

<div display="block">
  <video src="https://lookaside.internalfb.com/intern/pixelcloudnew/asset/?id=232484772246926" controls="1" preload="auto" width="100%"></video>
</div>

### Accessibility Mode

Enable Accessibility Mode by clicking on the accessibility icon. This will show accessibility view hierarchy next to the normal hierarchy.
This shows the accessibility view hierarchy next to the normal hierarchy.
In the hierarchy, the currently accessibility-focused view is highlighted in green and any accessibility-focusable elements have a green icon next to their name.
When accessibility mode is enabled, the sidebar will show special properties that are used by accessibility services to determine their functionality.
This includes things like content-description, clickable, focusable, and long-clickable among others.

![Accessibility in Flipper](/images/debugging-flipper-accessibility.png)

### Talkback

The accessibility mode sidebar also includes a panel with properties derived specifically to show Talkback's interpretation of a view (with logic ported over from Google's Talkback source).
While generally accurate, this is not guaranteed to be accurate for all situations. It is always better to turn Talkback on for verification.



## Sections Plugin

The plugin provides a view into sections tree generations and lifecycle events, that render content on the screen.
With Sections Plugin you can:
- Track Sections lifecycle events
- Show a stack trace for each state update that occurs in the section hierarchy
- Visualize the Sections tree hierarchies
- Inspect Render Sections hierarchies.
- Inspect Sections generated and applied changesets

Once you open the plugin it will starting listening for all sections events that are triggered by the Sections.
This might slow down the interaction with the app at first but it should be smooth after the first event has been tracked.

The plugin rationale is to collect a collection of events that come with an attached stack trace.
Every event can hold different information that is rendered on screen through either the Tree Data port view or the left Sidebar.

![Sections in Flipper](/images/debugging-flipper-sections.png)


## Enable Debugging Logs

Currently there are 3 places where we can change the boolean value to see more useful logs in logcat:
1. [SectionsDebug](pathname:///javadoc/com/facebook/litho/widget/SectionsDebug.html) -
turning on the `ENABLED` flag will enable the logs with the changesets that were calculated and expose which items were inserted, removed, updated or moved in your list.
This can help answer questions like *Why was my item re-rendered when nothing changed?*.

2. [ComponentTree](pathname:///javadoc/com/facebook/litho/ComponentTree.html) -
   turning on the `DEBUG_LOGS` flag will enable the logs from `ComponentTree` class responsible for calculating, resolving, measuring and flattening the layout.
   This can help answer questions like *Why the UI is not updating?* (possible Layout calculation issue), *Why my component is not visible, but it is showing up in the Flipper?* (Possible Mounting problem where `mount` wasn’t called for the layout result)

3. [AnimationsDebug](pathname:///javadoc/com/facebook/litho/AnimationsDebug.html) -
   turning on the `ENABLED` flag will enable the logs related to the Animations and Transitions.








