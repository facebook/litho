---
id: common-props
title: Common Props
---

This page contains a detailed API reference for the Litho's component common props. It assumes you’re familiar with fundamental Litho concepts, such as LayoutSpecs, Props, and State. If you’re not, read them first.

## Props

* [`AccessibilityRole`](#accessibilityrole)
* [`AccessibilityRoleDescription`](#accessibilityroledescription)
* [`AlignSelf`](#alignself)
* [`Alpha`](#alpha)
* [`AspectRatio`](#aspectratio)
* [`Background`](#background)
* [`Border`](#border)
* [`ClickHandler`](#clickhandler)
* [`Clickable`](#clickable)
* [`ClipChildren`](#clipchildren)
* [`ClipToOutline`](#cliptooutline)
* [`ContentDescription`](#contentdescription)
* [`DispatchPopulateAccessibilityEventHandler`](#dispatchpopulateaccessibilityeventhandler)
* [`DuplicateParentState`](#duplicateparentstate)
* [`EnabledState`](#enabledstate)
* [`Flex`](#flex)
* [`FlexBasisPercent`](#flexbasispercent)
* [`FlexBasisPx`](#flexbasispx)
* [`FlexGrow`](#flexgrow)
* [`FlexShrink`](#flexshrink)
* [`FocusChangeHandler`](#focuschangehandler)
* [`FocusState`](#focusstate)
* [`FocusedHandler`](#focusedhandler)
* [`Foreground`](#foreground)
* [`FullImpressionHandler`](#fullimpressionhandler)
* [`HeightPercent`](#heightpercent)
* [`HeightPx`](#heightpx)
* [`ImportantForAccessibility`](#importantforaccessibility)
* [`InterceptTouchHandler`](#intercepttouchhandler)
* [`InvisibleHandler`](#invisiblehandler)
* [`IsReferenceBaseline`](#isreferencebaseline)
* [`LayoutDirection`](#layoutdirection)
* [`LongClickHandler`](#longclickhandler)
* [`MarginAutos`](#marginautos)
* [`MarginPercents`](#marginpercents)
* [`Margins`](#margins)
* [`MaxHeightPercent`](#maxheightpercent)
* [`MaxHeightPx`](#maxheightpx)
* [`MaxWidthPercent`](#maxwidthpercent)
* [`MaxWidthPx`](#maxwidthpx)
* [`MinHeightPercent`](#minheightpercent)
* [`MinHeightPx`](#minheightpx)
* [`MinWidthPercent`](#minwidthpercent)
* [`MinWidthPx`](#minwidthpx)
* [`OnInitializeAccessibilityEventHandler`](#oninitializeaccessibilityeventhandler)
* [`OnInitializeAccessibilityNodeInfoHandler`](#oninitializeaccessibilitynodeinfohandler)
* [`OnPopulateAccessibilityEventHandler`](#onpopulateaccessibilityeventhandler)
* [`OnRequestSendAccessibilityEventHandler`](#onrequestsendaccessibilityeventhandler)
* [`OutlineProvider`](#outlineprovider)
* [`PaddingPercents`](#paddingpercents)
* [`Paddings`](#paddings)
* [`PerformAccessibilityActionHandler`](#performaccessibilityactionhandler)
* [`PositionPercents`](#positionpercents)
* [`PositionType`](#positiontype)
* [`Positions`](#positions)
* [`Rotation`](#rotation)
* [`RotationX`](#rotationx)
* [`RotationY`](#rotationy)
* [`Scale`](#scale)
* [`SelectedState`](#selectedstate)
* [`SendAccessibilityEventHandler`](#sendaccessibilityeventhandler)
* [`SendAccessibilityEventUncheckedHandler`](#sendaccessibilityeventuncheckedhandler)
* [`ShadowElevation`](#shadowelevation)
* [`StateListAnimator`](#statelistanimator)
* [`StateListAnimatorRes`](#statelistanimatorres)
* [`TouchExpansions`](#touchexpansions)
* [`TouchHandler`](#touchhandler)
* [`TransitionKey`](#transitionkey)
* [`TransitionKeyType`](#transitionkeytype)
* [`UnfocusedHandler`](#unfocusedhandler)
* [`UseHeightAsBaseline`](#useheightasbaseline)
* [`ViewTag`](#viewtag)
* [`ViewTags`](#viewtags)
* [`VisibilityChangedHandler`](#visibilitychangedhandler)
* [`VisibleHandler`](#visiblehandler)
* [`VisibleHeightRatio`](#visibleheightratio)
* [`VisibleWidthRatio`](#visiblewidthratio)
* [`WidthPercent`](#widthpercent)
* [`WidthPx`](#widthpx)

## Reference

### AccessibilityRole

 {...}

### AccessibilityRoleDescription

 {...}

### AlignSelf

 {...}

### Alpha

 {...}

### AspectRatio

 {...}

### Background

Sets the background of the component; pass a
[ComparableDrawable](/javadoc/com/facebook/litho/drawable/ComparableDrawable.html) to make
subsequent mounting more efficient.

```
MyComponent.create(c)
  .background(new ComparableGradientDrawable())
```

Use the utility methods to set a background color or use an android resource id.

* `Component#backgroundAttr(@AttrRes int)`
* `Component#backgroundColor(@ColorInt int)`
* `Component#backgroundRes(@DrawableRes int)`
* `Component#background(Drawable)` _(deprecated)_

### Border

 Sets a border on the component.

 ```
MyComponent.create(c)
  .border(
    Border.create(c)
      .color(YogaEdge.LEFT, Color.RED)
      .color(YogaEdge.TOP, 0xFFFFFF00)
      .color(YogaEdge.RIGHT, 0xFFFFFFFF)
      .color(YogaEdge.BOTTOM, 0xFFFF00FF)
      .widthDip(YogaEdge.ALL, 4)
      .build()
  )
 ```

 See: [YogaEdge](/javadoc/com/facebook/yoga/YogaEdge.html), [Border](/javadoc/com/facebook/litho/Border.Builder.html)

### ClickHandler

Sets a click handler on the component.

```
MyComponent.create(c)
  .clickHandler(RootComponent.onSomeEvent(c))
```

See: [Event Handling](events-touch-handling) docs for more info.

### Clickable

Defines whether this component reacts to click events. The default value is inherited from its
Android View.

```
MyComponent.create(c)
  .clickable(true)
```

### ClipChildren

Defines whether a children of given component are limited to draw inside of its bounds or not. The
default value of this property is `true`.

```
MyComponent.create(c)
  .clipChildren(true)
```

### ClipToOutline

 {...}

### ContentDescription

 {...}

### DispatchPopulateAccessibilityEventHandler

 {...}

### DuplicateParentState

 {...}

### EnabledState

 {...}

### Flex

 {...}

### FlexBasisPercent

 {...}

### FlexBasisPx

 {...}

### FlexGrow

 {...}

### FlexShrink

 {...}

### FocusChangeHandler

 {...}

### FocusState

 {...}

### FocusedHandler

 {...}

### Foreground

 {...}

### FullImpressionHandler

 {...}

### HeightPercent

 {...}

### HeightPx

 {...}

### ImportantForAccessibility

 {...}

### InterceptTouchHandler

 {...}

### InvisibleHandler

 {...}

### IsReferenceBaseline

 {...}

### LayoutDirection

 {...}

### LongClickHandler

 {...}

### MarginAutos

 {...}

### MarginPercents

 {...}

### Margins

 {...}

### MaxHeightPercent

 {...}

### MaxHeightPx

 {...}

### MaxWidthPercent

 {...}

### MaxWidthPx

 {...}

### MinHeightPercent

 {...}

### MinHeightPx

 {...}

### MinWidthPercent

 {...}

### MinWidthPx

 {...}

### OnInitializeAccessibilityEventHandler

 {...}

### OnInitializeAccessibilityNodeInfoHandler

 {...}

### OnPopulateAccessibilityEventHandler

 {...}

### OnRequestSendAccessibilityEventHandler

 {...}

### OutlineProvider

 {...}

### PaddingPercents

 {...}

### Paddings

 {...}

### PerformAccessibilityActionHandler

 {...}

### PositionPercents

 {...}

### PositionType

 {...}

### Positions

 {...}

### Rotation

 {...}

### RotationX

 {...}

### RotationY

 {...}

### Scale

 {...}

### SelectedState

 {...}

### SendAccessibilityEventHandler

 {...}

### SendAccessibilityEventUncheckedHandler

 {...}

### ShadowElevation

 {...}

### StateListAnimator

 {...}

### StateListAnimatorRes

 {...}

### TouchExpansions

 {...}

### TouchHandler

 {...}

### TransitionKey

 {...}

### TransitionKeyType

 {...}

### UnfocusedHandler

 {...}

### UseHeightAsBaseline

 {...}

### ViewTag

 {...}

### ViewTags

 {...}

### VisibilityChangedHandler

 {...}

### VisibleHandler

 {...}

### VisibleHeightRatio

 {...}

### VisibleWidthRatio

 {...}

### WidthPercent

 {...}

### WidthPx

 {...}
