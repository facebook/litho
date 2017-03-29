---
docid: widgets-recycler
title: Recycler
layout: docs
permalink: /docs/widgets-recycler.html
---
The `Recycler` component renders an Android [SwipeRefreshLayout](https://developer.android.com/reference/android/support/v4/widget/SwipeRefreshLayout.html) with a  [RecyclerView](https://developer.android.com/reference/android/support/v7/widget/RecyclerView.html) as child. Since this component needs a bit more insight to be used correctly, we have a specific guide for it: TODO LINK

`@Prop` 	| Optional | Default | Notes 
---	| ---  | --- | ---
binder	| [ ]      |   | Refer to TODO LINK
refreshHandler	| [x]      |   | `EventHandler` to catch onRefresh events.
hasFixedSize	| [x]      | true | 
clipToPadding	| [x]      |   | 
scrollBarStyle	| [x]      | View.SCROLLBARS\_INSIDE\_OVERLAY  | 
itemDecoration	| [x]      |   | 
recyclerViewId	| [x]      | View.NO\_ID  | Id to be set to the underlying RecyclerView.
itemAnimator	| [x]      | NoUpdateItemAnimator | This is a standard DefaultItemAnimator with disabled change animations.
recyclerEventsController	| [x]      |   | A controller to trigger events from outside the component hierarchy such as: scroll to top or scroll to position.
onScrollListener	| [x]      |   | 