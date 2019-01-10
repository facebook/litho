---
docid: recycling
title: Recycling
layout: docs
permalink: /docs/recycling
---

Device screens typically refresh at a rate of 60 frames per second. To provide smooth performances, an app needs to be able to render changes to our UI continuously every 16ms. Failing to respect this time constraint leads to dropped frames and poor user experience.
As UIs become increasingly sophisticated, it gets harder to complete all the rendering work within this time frame. This proves to be especially challenging with dynamic scrolling surfaces, as new pieces of UI are constantly being introduced on screen.   
Android solves this problem this with [RecyclerView](https://developer.android.com/guide/topics/ui/layout/recyclerview.html), a dynamic UI container that is able to display elements from large data sets by creating only enough views to fill the screen and then recycling and reusing them as the UI scrolls.

<video loop autoplay class="video">
  <source type="video/mp4" src="/static/videos/recycler_view.mp4"></source>
  <p>Your browser does not support the video element.</p>
</video>


RecyclerView supports the idea of displaying heterogeneous content. To do so, it keeps views in different pools depending on their type.
While this concept works pretty well in simple cases, it can prove to be problematic for UIs with many different view types.
In a scenario with many view types, there is a bigger chance that the view coming in the viewport following a scrolling event is a view that the RecyclerView is displaying for the first time.
If that happens, RecyclerView has to allocate a new view. The allocation will happen in the same 16ms slot in which RecyclerView also has to bind, measure and layout the newly visible view.


<video loop autoplay class="video">
  <source type="video/mp4" src="/static/videos/multiple_view_types.mp4"></source>
  <p>Your browser does not support the video element.</p>
</video>


## Incremental recycling with Litho

We wanted to have a more scalable and efficient recycling system for Litho and at the same time we wanted to remove the complexity of view types from our API.  
In Litho the representation of a layout is completely disjointed from the Views and Drawables that will be used to render that layout on screen. This means that by the time we need to put a new View of the RecyclerView on screen, we already know what the content of that item will be and exactly its position relative to the rest of the UI.  
This allows Litho to completely move away from the concept of View types. Rather than re-using the entire View that represents an item in the RecyclerView we can incrementally use building blocks such at `Text` or `Image` while the RecyclerView scrolls.  
This is not possible with traditional Android Views since the layout computation operates on the complete view tree and by the time we know the positions of all Views in a row everything has already been instatiated.

<video loop autoplay class="video">
  <source type="video/mp4" src="/static/videos/incremental_recycling.mp4"></source>
  <p>Your browser does not support the video element.</p>
</video>

Being able to recycle individual primitive items as `Text` increases greatly the memory efficiency of an App as now you can recycle any piece of text in your list for any other piece of text.  
On top of that, since we compute the layout ahead of time, we know exactly at which point a new items needs to become visible, this means that rather than binding and drawing a big view tree in one frame, we can use each frame to introduce a much lower number of primitive items on screen.
