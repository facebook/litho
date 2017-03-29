---
docid: motivation
title: Motivation
layout: docs
permalink: /docs/motivation
---

Building a list interface on Android is fairly simple. Just create a layout for
the items, hook it up to a *RecyclerView* adapter, and you're done. Most apps
are a bit more complicated than that, though.

If your adapter has more than a few view types, you'll have to think about more
robust ways to recycle views. If you have too many view types, *RecyclerView*
will be constantly inflating new views every time a new type is displayed,
which is likely to cause frame drops while scrolling.

You can minimise the number of view types by recycling the same view instances
for multiple variations within items in *RecyclerView* but this usually results
in a solution that will get increasingly more prone to bugs as you add more
features to your product.

If the list items are complex, chances are that you'll have to optimize your
layouts. For simpler cases, you can get good performance by simply avoiding
some gotchas from Android's stock layouts but this is not always the case. A
common approach is to implement custom views especially tailored for your use
cases. Custom views are great for UI efficiency but tend to slow you down due
to the added complexity and higher maintenance cost.

You can also optimize complex items in *RecyclerView* by breaking them down
into separate items in the adapter to better spread the time spent on layout
and drawing per frame while scrolling. This approach can work well in some
cases but it might still require investment in custom views and usually leads
to an explosion in view types in the adapter, leading to the problems we
described above.

Litho was primarily built to encapsulate the complexity of implementing
efficient *RecyclerViews*. With Litho, there are no view types and you build
user interfaces that can seamlessly compute layout ahead of time in a
background thread and render much flatter view hierarchies automatically. And
they get all of these fancy features for free behind a much simpler programming
model!
