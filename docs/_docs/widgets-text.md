---
docid: widgets-text
title: Text
layout: docs
permalink: /docs/widgets-text.html
---
The `Text` component renders an Android [TextView](https://developer.android.com/reference/android/widget/TextView.html).

`@Prop` 			| Optional | Default | Notes 
---	| ---  | --- | ---
accessibleClickableSpans | [x]      | false | If a `ClickableSpan` is given as a text `@Prop`, this flag makes the `Text` component use the clickable span information for accessibily.
ellipsize 		| [x]      |         |
extraSpacing	 	| [x]      |         |
glyphWarming		| [x]      | false | Schedules a [Layout](https://developer.android.com/reference/android/text/Layout.html) to be drawn in the background. This warms up the Glyph cache for that Layout.
highlightColor 	| [x]      |         |
isSingleLine 		| [x]      |         |
linkColor 		| [x]      |         |
maxLines 			| [x]      | Integer.MAX_VALUE |
maxEms				| [x]      | 	 |
maxWidth			| [x]      | Integer.MAX_VALUE |
minLines 			| [x]      | Integer.MIN_VALUE |
minEms 			| [x]      | |
minWidth			| [x]      |  |
shadowColor 		| [x]      | Color.GRAY |
shadowDx 			| [x]      |         |
shadowDy 			| [x]      |         |
shadowRadius 		| [x]      |         |
shouldIncludeFontPadding | [x]      |  true |
spacingMultiplier | [x]      |         |
text 				| [ ]      |         |
textAlignment 	| [x]      | ALIGN_NORMAL |
textColor 		| [x]      |         |
textColorStateList | [x]      |         |
textSize 			| [x]      | 13px |
textStyle 		| [x]      |         |
typeface 			| [x]      | Typeface.DEFAULT |
verticalGravity 	| [x]      | TOP |