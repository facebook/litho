---
docid: common-props
title: Common Props
layout: docs
permalink: /docs/common_props
---

This page contains a detailed API reference for the Litho's component common props. It assumes you’re familiar with fundamental Litho concepts, such as LayoutSpecs, Props, and State. If you’re not, read them first.

## Props

* [`AccessibilityRole`](#null__accessibilityrole)
* [`AccessibilityRoleDescription`](#null__accessibilityroledescription)
* [`AlignSelf`](#null__alignself)
* [`Alpha`](#null__alpha)
* [`AspectRatio`](#null__aspectratio)
* [`Background`](#null__background)
* [`Border`](#null__border)
* [`ClickHandler`](#null__clickhandler)
* [`Clickable`](#null__clickable)
* [`ClipChildren`](#null__clipchildren)
* [`ClipToOutline`](#null__cliptooutline)
* [`ContentDescription`](#null__contentdescription)
* [`DispatchPopulateAccessibilityEventHandler`](#null__dispatchpopulateaccessibilityeventhandler)
* [`DuplicateParentState`](#null__duplicateparentstate)
* [`EnabledState`](#null__enabledstate)
* [`Flex`](#null__flex)
* [`FlexBasisPercent`](#null__flexbasispercent)
* [`FlexBasisPx`](#null__flexbasispx)
* [`FlexGrow`](#null__flexgrow)
* [`FlexShrink`](#null__flexshrink)
* [`FocusChangeHandler`](#null__focuschangehandler)
* [`FocusState`](#null__focusstate)
* [`FocusedHandler`](#null__focusedhandler)
* [`Foreground`](#null__foreground)
* [`FullImpressionHandler`](#null__fullimpressionhandler)
* [`HeightPercent`](#null__heightpercent)
* [`HeightPx`](#null__heightpx)
* [`ImportantForAccessibility`](#null__importantforaccessibility)
* [`InterceptTouchHandler`](#null__intercepttouchhandler)
* [`InvisibleHandler`](#null__invisiblehandler)
* [`IsReferenceBaseline`](#null__isreferencebaseline)
* [`LayoutDirection`](#null__layoutdirection)
* [`LongClickHandler`](#null__longclickhandler)
* [`MarginAutos`](#null__marginautos)
* [`MarginPercents`](#null__marginpercents)
* [`Margins`](#null__margins)
* [`MaxHeightPercent`](#null__maxheightpercent)
* [`MaxHeightPx`](#null__maxheightpx)
* [`MaxWidthPercent`](#null__maxwidthpercent)
* [`MaxWidthPx`](#null__maxwidthpx)
* [`MinHeightPercent`](#null__minheightpercent)
* [`MinHeightPx`](#null__minheightpx)
* [`MinWidthPercent`](#null__minwidthpercent)
* [`MinWidthPx`](#null__minwidthpx)
* [`OnInitializeAccessibilityEventHandler`](#null__oninitializeaccessibilityeventhandler)
* [`OnInitializeAccessibilityNodeInfoHandler`](#null__oninitializeaccessibilitynodeinfohandler)
* [`OnPopulateAccessibilityEventHandler`](#null__onpopulateaccessibilityeventhandler)
* [`OnRequestSendAccessibilityEventHandler`](#null__onrequestsendaccessibilityeventhandler)
* [`OutlineProvider`](#null__outlineprovider)
* [`PaddingPercents`](#null__paddingpercents)
* [`Paddings`](#null__paddings)
* [`PerformAccessibilityActionHandler`](#null__performaccessibilityactionhandler)
* [`PositionPercents`](#null__positionpercents)
* [`PositionType`](#null__positiontype)
* [`Positions`](#null__positions)
* [`Rotation`](#null__rotation)
* [`RotationX`](#null__rotationx)
* [`RotationY`](#null__rotationy)
* [`Scale`](#null__scale)
* [`SelectedState`](#null__selectedstate)
* [`SendAccessibilityEventHandler`](#null__sendaccessibilityeventhandler)
* [`SendAccessibilityEventUncheckedHandler`](#null__sendaccessibilityeventuncheckedhandler)
* [`ShadowElevation`](#null__shadowelevation)
* [`StateListAnimator`](#null__statelistanimator)
* [`StateListAnimatorRes`](#null__statelistanimatorres)
* [`TouchExpansions`](#null__touchexpansions)
* [`TouchHandler`](#null__touchhandler)
* [`TransitionKey`](#null__transitionkey)
* [`TransitionKeyType`](#null__transitionkeytype)
* [`UnfocusedHandler`](#null__unfocusedhandler)
* [`UseHeightAsBaseline`](#null__useheightasbaseline)
* [`ViewTag`](#null__viewtag)
* [`ViewTags`](#null__viewtags)
* [`VisibilityChangedHandler`](#null__visibilitychangedhandler)
* [`VisibleHandler`](#null__visiblehandler)
* [`VisibleHeightRatio`](#null__visibleheightratio)
* [`VisibleWidthRatio`](#null__visiblewidthratio)
* [`WidthPercent`](#null__widthpercent)
* [`WidthPx`](#null__widthpx)

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
[ComparableDrawable](/javadoc/com/facebook/litho/drawable/ComparableDrawable.html){:target="_blank"} to make
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

 See: [YogaEdge](/javadoc/com/facebook/yoga/YogaEdge.html){:target="_blank"}, [Border](/javadoc/com/facebook/litho/Border.Builder.html){:target="_blank"}

### ClickHandler

Sets a click handler on the component.

```
MyComponent.create(c)
  .clickHandler(RootComponent.onSomeEvent(c))
```

See: [Event Handling](/docs/events-touch-handling) docs for more info.

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
